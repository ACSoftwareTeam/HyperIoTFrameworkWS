package it.acsoftware.hyperiot.authentication.service;

import it.acsoftware.hyperiot.authentication.api.AuthenticationSystemApi;
import it.acsoftware.hyperiot.base.api.HyperIoTAuthenticationProvider;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;
import it.acsoftware.hyperiot.base.security.rest.HyperIoTAuthenticationFilter;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.huser.model.HUser;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtProducer;
import org.apache.cxf.rs.security.jose.jwt.JwtClaims;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.container.ContainerRequestFilter;
import java.util.*;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the interace
 * AuthenticationSystemApi for user login. Service classes differ from
 * System Service classes because the first checks permissions the
 * latter not. It's always up to the user to check the permission in the
 * service class.
 */

@Component(service = AuthenticationSystemApi.class, immediate = true)
public final class AuthenticationSystemServiceImpl extends HyperIoTBaseSystemServiceImpl
        implements AuthenticationSystemApi {

    private HyperIoTAuthenticationFilter authFilter;

    public HyperIoTAuthenticationFilter getAuthFilter() {
        return authFilter;
    }

    @Reference(target = "(org.apache.cxf.dosgi.IntentName=jwtAuthFilter)")
    public void setAuthFilter(ContainerRequestFilter authFilter) {
        this.authFilter = (HyperIoTAuthenticationFilter) authFilter;
    }

    /**
     * Method that verifies user credentials against database
     *
     * @param username
     * @param password Valid JWT Token or password
     * @return
     */
    @Override
    public HyperIoTAuthenticable login(String username, String password) {
        log.log(Level.FINE,
                "Invoking login with all auth providers");
        List<HyperIoTAuthenticationProvider> authenticationProviders = this.getAuthenticationProviders(null);
        return this.login(username, password, authenticationProviders);
    }

    /**
     * Method that verifies user credentials against database using specific auth provider filter
     *
     * @param username
     * @param password           valid JWT Token or password
     * @param authProviderFilter
     * @return Authenticable
     */
    @Override
    public HyperIoTAuthenticable login(String username, String password, String authProviderFilter) {
        log.log(Level.FINE,
                "Invoking login with auth providers which match filter: {0}" , authProviderFilter);
        List<HyperIoTAuthenticationProvider> authenticationProviders = this.getAuthenticationProviders(authProviderFilter);
        return this.login(username, password, authenticationProviders);
    }

    /**
     * Method tries to login with JWT and then tries with Auth Provider
     *
     * @param username
     * @param password                JWT Token or Password
     * @param authenticationProviders
     * @return Authenticable
     */
    private HyperIoTAuthenticable login(String username, String password, List<HyperIoTAuthenticationProvider> authenticationProviders) {
        if (authenticationProviders != null) {
            //Trying to authneticate on all providers
            for (int i = 0; i < authenticationProviders.size(); i++) {
                //password can be JWT Token
                HyperIoTAuthenticable user = this.loginWithJWTToken(username, password, authenticationProviders.get(i));
                //try JWT Login
                if (user == null) {
                    //classic login by auth provider
                    user = authenticationProviders.get(i).login(username, password);
                }
                if (user != null) {
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * @param username
     * @param jwt
     * @param provider
     * @return Authenticable
     */
    private HyperIoTAuthenticable loginWithJWTToken(String username, String jwt, HyperIoTAuthenticationProvider provider) {
        log.log(Level.FINE,
                "Checking if user attempted login with JWT Token");
        try {
            JwtToken token = authFilter.getJwtToken(jwt);
            boolean validIssuer = Arrays.asList(provider.validIssuers()).contains(token.getClaims().getIssuer());
            //checking wheater is the right user
            if (token.getClaims().getSubject().equals(username) && validIssuer) {
                HyperIoTAuthenticable authenticable = provider.findByUsername(username);
                return authenticable;
            }
            return null;
        } catch (Exception e) {
            log.log(Level.FINE, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Checks wherever the user can access the requested topic
     */
    @Override
    public boolean userCanAccessTopic(String username, String topic) {
        return true;
    }

    /**
     * @param filter OSGi filter for auth provider
     * @return
     */
    private List<HyperIoTAuthenticationProvider> getAuthenticationProviders(String filter) {
        log.log(Level.FINE,
                "Invoking getAuthenticationProviders for searching for AuthenticationProviders");
        List<HyperIoTAuthenticationProvider> providers = new ArrayList<>();
        try {
            Collection<ServiceReference<HyperIoTAuthenticationProvider>> references = HyperIoTUtil
                    .getBundleContext(this)
                    .getServiceReferences(HyperIoTAuthenticationProvider.class, filter);
            if (references != null && references.size() > 0) {
                Iterator<ServiceReference<HyperIoTAuthenticationProvider>> it = references.iterator();
                while (it.hasNext()) {
                    providers.add(HyperIoTUtil.getBundleContext(this).getService(it.next()));
                }
                return providers;
            }
        } catch (InvalidSyntaxException e) {
            log.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param user user from which token must be generated
     * @return JWT Token
     */
    @Override
    public String generateToken(HyperIoTAuthenticable user) {
        Calendar c = new GregorianCalendar();
        c.setTime(new Date());
        JwtClaims jwtClaims = new JwtClaims();
        jwtClaims.setSubject(user.getScreenName());
        //Enforcing only HUSers has the righe Isseuer, for rest services
        if (user instanceof HUser)
            jwtClaims.setIssuer(HyperIoTUser.class.getName());
        else
            jwtClaims.setIssuer(user.getClass().getName());
        jwtClaims.setClaim("loggedEntityId", user.getId());
        jwtClaims.setIssuedAt(c.getTimeInMillis() / 1000L);
        c.add(Calendar.HOUR, 8);
        jwtClaims.setExpiryTime(c.getTimeInMillis());
        JwtToken token = new JwtToken(jwtClaims);
        return this.generateToken(token);
    }

    /**
     * @param token JWTToken Object
     * @return
     */
    @Override
    public String generateToken(JwtToken token) {
        Properties signingProperties = HyperIoTSecurityUtil.getJwtProperties(HyperIoTUtil.getBundleContext(this));

        JwsHeaders jwsHeaders = new JwsHeaders(signingProperties);
        JwsSignatureProvider sigProvider = JwsUtils.loadSignatureProvider(signingProperties, jwsHeaders);

        JoseJwtProducer producer = new JoseJwtProducer();
        producer.setSignatureProvider(sigProvider);

        String tokenStr = producer.processJwt(token);
        return tokenStr;
    }


}
