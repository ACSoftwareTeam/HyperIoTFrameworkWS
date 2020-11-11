package it.acsoftware.hyperiot.mail.service.rest;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.mail.api.MailApi;
import it.acsoftware.hyperiot.mail.model.MailTemplate;
import it.acsoftware.hyperiot.mail.util.MailConstants;
import it.acsoftware.hyperiot.mail.util.MailUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Aristide Cittadino Mail rest service class. Registered with DOSGi CXF
 */
@SwaggerDefinition(basePath = "/mail/templates", info = @Info(description = "HyperIoT Mail API", version = "2.0.0", title = "HyperIoT Mail Templates", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/mail/templates", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = MailRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.mail.service.rest.MailRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/mail/templates",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"}, immediate = true)
@Path("")
public class MailRestApi extends HyperIoTBaseEntityRestApi<MailTemplate> {
    private Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    private MailApi service;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Mail Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET", authorizations = @Authorization("jwt-auth"))
    @LoggedIn
    public Response checkModuleWorking() {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/mail/templates/module/status");
        return Response.ok("Mail Module works!").build();
    }

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Mail Module work!
     */
    @GET
    @Path("/test/send")
    @ApiOperation(value = "/test/send", notes = "Simple service for checking mail service status", httpMethod = "GET", authorizations = @Authorization("jwt-auth"))
    @LoggedIn
    public Response testEmailSend() {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/mail/templates/module/status");
        List<String> recipients = MailUtil.getTestRecipients();
        try {
            HashMap<String, Object> params = new HashMap<>();
            params.put("title", "Test Title!");
            String mailBody = this.service.generateTextFromTemplate(MailConstants.MAIL_TEMPLATE_TEST, params);
            this.service.sendMail(MailUtil.getUsername(), recipients, null, null, "MAIL TEST", mailBody, null);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            return Response.ok("Error while sending mail, please check logs: {0}", e.getMessage()).build();
        }
        return Response.ok("Mail Module works!").build();

    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<MailTemplate> getEntityService() {
        log.log(Level.FINEST, "invoking getEntityService, returning: {0}", this.service);
        return service;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = MailApi.class)
    protected void setEntityService(MailApi entityService) {
        log.log(Level.FINEST, "invoking setEntityService, setting: {0}", this.service);
        this.service = entityService;
    }

    /**
     * Service finds an existing Mail
     *
     * @param id id from which Mail object will retrieved
     * @return Mail if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/mail/templates/{id}", notes = "Service for finding mail template", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response findMail(@PathParam("id") long id) {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/mail/templates/{0}", id);
        return this.find(id);
    }

    /**
     * Service saves a new Mail
     *
     * @param entity Mail object to store in database
     * @return the Mail saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/mail/templates", notes = "Service for adding a new mail template entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response saveMail(
            @ApiParam(value = "Mail entity which must be saved ", required = true) MailTemplate entity) {
        log.log(Level.FINE, "In Rest Service POST /hyperiot/mail/templates/ \n Body: {0}", entity);
        return this.save(entity);
    }

    /**
     * Service updates a Mail
     *
     * @param entity Mail object to update in database
     * @return the Mail updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/mail/templates", notes = "Service for updating a mail template entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    public Response updateMail(
            @ApiParam(value = "Mail entity which must be updated ", required = true) MailTemplate entity) {
        log.log(Level.FINE, "In Rest Service PUT /hyperiot/mail/templates/ \n Body: {0}", entity);
        return this.update(entity);
    }

    /**
     * Service deletes a Mail
     *
     * @param id id from which Mail object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/mail/templates/{id}", notes = "Service for deleting a mail template entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response deleteMail(
            @ApiParam(value = "The mail id which must be deleted", required = true) @PathParam("id") long id) {
        log.log(Level.FINE, "In Rest Service DELETE /hyperiot/mail/templates/{0}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available mail
     *
     * @return list of all available mail
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/mail/templates/all", notes = "Service for finding all mail templates entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllMail() {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/mail/templates/all");
        return this.findAll();
    }

    /**
     * Service finds all available mail
     *
     * @return list of all available mail
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/mail/templates", notes = "Service for finding all mail templates entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllMailPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        log.log(Level.FINE, "In Rest Service GET /hyperiot/mail/templates/");
        return this.findAll(delta, page);
    }

}
