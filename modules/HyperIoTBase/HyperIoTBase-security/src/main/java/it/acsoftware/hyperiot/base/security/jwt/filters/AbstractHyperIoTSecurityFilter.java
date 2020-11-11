package it.acsoftware.hyperiot.base.security.jwt.filters;

import it.acsoftware.hyperiot.authentication.model.context.HyperIoTContextFactory;
import it.acsoftware.hyperiot.base.api.HyperIoTJwtContext;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jaxrs.AbstractJwtAuthenticationFilter;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwt.JoseJwtConsumer;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.apache.cxf.rs.security.jose.jwt.JwtUtils;
import org.apache.cxf.security.SecurityContext;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Aristide Cittadino This Class is a copy of
 * AbstractJwtAuthenticationFilter. The copy is needed because the
 * original class use @PreMatching annotation which prevents method
 * access control.
 */
@Priority(Priorities.AUTHENTICATION)
public abstract class AbstractHyperIoTSecurityFilter extends JoseJwtConsumer
    implements ContainerRequestFilter {
    protected static final Logger LOG = LogUtils
        .getL7dLogger(AbstractJwtAuthenticationFilter.class);

    private String roleClaim;
    private boolean validateAudience = true;

    /**
     * @param requestContext
     * @throws IOException
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String encodedJwtToken = getEncodedJwtToken(requestContext);
        JwtToken token = super.getJwtToken(encodedJwtToken);

        SecurityContext securityContext = configureSecurityContext(token);
        if (securityContext != null) {
            JAXRSUtils.getCurrentMessage().put(SecurityContext.class, securityContext);
            requestContext.setSecurityContext((HyperIoTJwtContext) securityContext);
        }
    }

    /**
     * @param requestContext
     * @return
     */
    protected abstract String getEncodedJwtToken(ContainerRequestContext requestContext);

    /**
     * @param jwt
     * @return
     */
    protected SecurityContext configureSecurityContext(JwtToken jwt) {
        Message m = JAXRSUtils.getCurrentMessage();
        boolean enableUnsignedJwt = MessageUtils.getContextualBoolean(m,
            JoseConstants.ENABLE_UNSIGNED_JWT_PRINCIPAL, false);

        // The token must be signed/verified with a public key to set up the security
        // context,
        // unless we directly configure otherwise
        if (jwt.getClaims().getSubject() != null
            && (isVerifiedWithAPublicKey(jwt) || enableUnsignedJwt)) {
            return HyperIoTContextFactory.createJwtContext(jwt, roleClaim);
        }
        return null;
    }

    /**
     * @param tokenStr
     * @return
     */
    public HyperIoTJwtContext doApplicationFilter(String tokenStr) {
        JwtToken token = super.getJwtToken(tokenStr);
        return (HyperIoTJwtContext) configureSecurityContext(token);
    }

    /**
     * @param jwt
     * @return
     */
    private boolean isVerifiedWithAPublicKey(JwtToken jwt) {
        if (isJwsRequired()) {
            String alg = (String) jwt.getJwsHeader(JoseConstants.HEADER_ALGORITHM);
            SignatureAlgorithm sigAlg = SignatureAlgorithm.getAlgorithm(alg);
            return SignatureAlgorithm.isPublicKeyAlgorithm(sigAlg);
        }

        return false;
    }

    /**
     * @param jwt
     */
    @Override
    protected void validateToken(JwtToken jwt) {
        JwtUtils.validateTokenClaims(jwt.getClaims(), getTtl(), getClockOffset(),
            isValidateAudience());
    }

    /**
     * @return
     */
    public String getRoleClaim() {
        return roleClaim;
    }

    /**
     * @param roleClaim
     */
    public void setRoleClaim(String roleClaim) {
        this.roleClaim = roleClaim;
    }

    /**
     * @return
     */
    public boolean isValidateAudience() {
        return validateAudience;
    }

    /**
     * @param validateAudience
     */
    public void setValidateAudience(boolean validateAudience) {
        this.validateAudience = validateAudience;
    }
}
