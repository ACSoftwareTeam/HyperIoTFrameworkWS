package it.acsoftware.hyperiot.huser.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.huser.api.HUserApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.model.HUserPasswordReset;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino HUser rest service class. Registered with DOSGi
 * CXF
 */
@SwaggerDefinition(basePath = "/husers", info = @Info(description = "HyperIoT HUser API", version = "2.0.0", title = "HyperIoT HUser", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/husers", produces = "application/json")
@Component(service = HUserRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.huser.service.rest.HUserRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/husers",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=exceptionmapper", "service.exported.intents=swagger"}, immediate = true)
@Path("")
public class HUserRestApi extends HyperIoTBaseEntityRestApi<HUser> {
    private HUserApi entityService;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT HUser Module works!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/hyperiot/huser/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/huser/module/status: ");
        return Response.ok("HyperIoT HUser Module works!").build();
    }

    /**
     * @return the current entityService
     */
    @Override
    public HUserApi getEntityService() {
        log.log(Level.FINEST, "invoking getEntityService, returning: {0}", this.entityService);
        return entityService;
    }

    /**
     * @param entityService Injecting entityService
     */
    @Reference(service = HUserApi.class)
    protected void setEntityService(HUserApi entityService) {
        log.log(Level.FINEST, "invoking setEntityService, setting: {0}", this.entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing user
     *
     * @param id id from which user object will retrieve
     * @return User if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/husers/{id}", notes = "Find User", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findHUser(
            @ApiParam(value = "id from which user object will retrieve", required = true) @PathParam("id") long id) {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/husers/{0}", id);
        return this.find(id);
    }

    /**
     * Service saves a new user
     *
     * @param h HUser object to store in database
     * @return User saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/husers", notes = "Save User", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response saveHUser(@ApiParam(value = "HUser object to store in database", required = true) HUser h) {
        log.log(Level.FINE, "In Rest Service POST /hyperiot/husers/ \n Body: {0}", h);
        return this.save(h);
    }

    /**
     * Register a new user
     *
     * @param h HUser object to store in database
     * @return User saved
     */
    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/husers/register", notes = "Save User", httpMethod = "POST", produces = "application/json", consumes = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 422, message = "Not validated"), @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response register(@ApiParam(value = "HUser object to store in database", required = true) HUser h) {
        log.log(Level.FINE, "In Rest Service POST /hyperiot/husers/register \n Body: {0}", h);
        try {
            // forcing active false
            h.setActive(false);
            h.setAdmin(false);
            h.setActivateCode(UUID.randomUUID().toString());
            this.entityService.registerUser(h, this.getHyperIoTContext());
            return Response.ok().entity(h).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Activate a new user
     *
     * @param email Email of the activating HUser
     * @param code  Code of the activating HUser
     * @return User saved
     */
    @POST
    @Path("/activate")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/husers/activate", notes = "Activate User", httpMethod = "POST", produces = "application/json", consumes = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 422, message = "Not validated"), @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response activate(
            @ApiParam(value = "Email of the activating user", required = true) @QueryParam("email") String email,
            @ApiParam(value = "Code of the activating user", required = true) @QueryParam("code") String code) {
        log.log(Level.FINE,
                "In Rest Service POST /hyperiot/husers/activate with code: {0}", new Object[]{code, email});
        try {
            this.entityService.activateUser(email, code);
            return Response.ok().build();
        } catch (Throwable t) {
            return handleException(t);
        }
    }

    /**
     * Reset user password request
     *
     * @param email Email of the HUser
     * @return User saved
     */
    @POST
    @Path("/resetPasswordRequest")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/husers/resetPasswordRequest", notes = "Reset User Password", httpMethod = "POST", produces = "application/json", consumes = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 404, message = "Entity not found"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response resetPasswordRequest(
            @ApiParam(value = "Email of the user", required = true) @QueryParam("email") String email) {
        log.log(Level.FINE, "In Rest Service POST /hyperiot/husers/resetPassword?email={0}", email);
        try {
            this.entityService.passwordResetRequest(email);
            return Response.ok().build();
        } catch (Throwable t) {
            return handleException(t);
        }
    }

    /**
     * Reset user password
     *
     * @param pwdReset Code for resetting user password
     * @return User saved
     */
    @POST
    @Path("/resetPassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "/hyperiot/husers/resetPassword", notes = "Change User Password", httpMethod = "POST", produces = "application/json", consumes = "application/json")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 404, message = "Entity not found"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response resetPassword(
            @ApiParam(value = "Code for resetting user password", required = true) HUserPasswordReset pwdReset) {
        log.log(Level.FINE, "In Rest Service POST /hyperiot/husers/changePassword?email={0}", pwdReset.getEmail()
                + "  with code:" + pwdReset.getResetCode());
        try {
            if (pwdReset.getPassword() == null || pwdReset.getPasswordConfirm() == null) {
                throw new HyperIoTRuntimeException("it.acsoftware.hyperiot.error.huser.password.reset.not.null");
            }
            this.entityService.resetPassword(pwdReset.getEmail(), pwdReset.getResetCode(), pwdReset.getPassword(),
                    pwdReset.getPasswordConfirm());
            return Response.ok().build();
        } catch (Throwable t) {
            return handleException(t);
        }

    }

    /**
     * Service updates a user
     *
     * @param h HUser object to update in database
     * @return User updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/husers", notes = "Update User", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateHUser(@ApiParam(value = "HUser object to update in database", required = true) HUser h) {
        log.log(Level.FINE, "In Rest Service PUT /hyperiot/husers/ \n Body: {0}", h);
        return this.update(h);
    }

    /**
     * Service updates a user
     *
     * @param hUser HUser object to update in database
     * @return User updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/husers/account", notes = "Update User Account", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @Path("/account")
    @JsonView(HyperIoTJSONView.Public.class)
    public Response updateAccountInfo(
            @ApiParam(value = "HUser object to update in database", required = true) HUser hUser) {
        log.log(Level.FINE, "In Rest Service PUT /hyperiot/husers/ \n Body: {0}", hUser);
        try {
            return Response.ok().entity(this.entityService.updateAccountInfo(this.getHyperIoTContext(), hUser)).build();
        } catch (Throwable e) {
            return this.handleException(e);
        }
    }

    /**
     * Service changes HUser password
     *
     * @param userId          id from which user object will retrieve
     * @param oldPassword     Old HUser password
     * @param newPassword     New HUser password
     * @param passwordConfirm New HUser password confirm
     * @return User updated
     */
    @PUT
    @Path("/password")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes("application/x-www-form-urlencoded")
    @LoggedIn
    @ApiOperation(value = "/hyperiot/husers/password", notes = "Update User", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response changeHUserPassword(
            @ApiParam(value = "HUser id which must be updated ", required = true) @FormParam("userId") long userId,
            @ApiParam(value = "Old HUser Password", required = true) @FormParam("oldPassword") String oldPassword,
            @ApiParam(value = "New HUser Password", required = true) @FormParam("newPassword") String newPassword,
            @ApiParam(value = "New HUser Password confirm", required = true) @FormParam("passwordConfirm") String passwordConfirm) {
        log.log(Level.FINE, "In Rest Service PUT /hyperiot/husers/password \n Body: {0}", userId);
        try {
            return Response.ok(this.entityService.changePassword(this.getHyperIoTContext(), userId, oldPassword,
                    newPassword, passwordConfirm)).build();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

    /**
     * Service deletes an existing user
     *
     * @param id id from which user object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/husers/{id}", notes = "Delete User", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response deleteHUser(
            @ApiParam(value = "id from which user object will deleted", required = true) @PathParam("id") long id) {
        log.log(Level.FINE, "In Rest Service DELETE /hyperiot/husers/{0}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available users
     *
     * @return List of all available users
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/husers/all", notes = "Find all users", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHUser() {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/husers/ ");
        return this.findAll();
    }

    /**
     * Service finds all available users
     *
     * @return List of all available users
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/husers", notes = "Find all users", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response findAllHUserPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/husers/ ");
        return this.findAll(delta, page);
    }

}
