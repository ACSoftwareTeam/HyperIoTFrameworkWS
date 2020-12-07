package it.acsoftware.hyperiot.permission.service.rest;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import it.acsoftware.hyperiot.permission.actions.HyperIoTPermissionAction;
import it.acsoftware.hyperiot.permission.api.PermissionApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Permission rest service class. Registered with
 * DOSGi CXF.
 */
@SwaggerDefinition(basePath = "/permissions", info = @Info(description = "HyperIoT Permission API", version = "2.0.0", title = "HyperIoT Permission", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/permissions", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = PermissionRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/permissions",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=swagger", "service.exported.intents=exceptionmapper"}, immediate = true)
@Path("")
public class PermissionRestApi extends HyperIoTBaseEntityRestApi<Permission> {
    private PermissionApi entityService;

    /**
     * @return the current entityService
     */
    @Override
    public HyperIoTBaseEntityApi<Permission> getEntityService() {
        getLog().log(Level.FINEST, "invoking getEntityService, returning: " + this.entityService);
        return entityService;
    }

    /**
     * @param entityService Injecting entityService
     */
    @Reference(service = PermissionApi.class)
    protected void setEntityService(PermissionApi entityService) {
        getLog().log(Level.FINEST, "invoking setEntityService, setting: {0}", entityService);
        this.entityService = entityService;
    }

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT Permission Module works!
     */
    @GET
    @Path("/module/status")
    @LoggedIn
    @ApiOperation(value = "/hyperiot/permissions/module/status", notes = "Simple service for checking module status", httpMethod = "GET", authorizations = @Authorization("jwt-auth"))
    public Response sayHi() {
        getLog().log(Level.FINE, "In Rest Service GET /hyperiot/permissions/module/status");
        return Response.ok("HyperIoT Permission Module works!").build();
    }

    /**
     * Service finds an existing permission
     *
     * @param id id from which permission object will retrieve
     * @return Permission if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/permissions/{id}", notes = "Find Permission", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response findPermission(
            @ApiParam(value = "id from which permission object will retrieve", required = true) @PathParam("id") long id) {
        getLog().log(Level.FINE, "In Rest Service GET /hyperiot/permissions/{0}", id);
        return this.find(id);
    }

    /**
     * Service saves a new permission
     *
     * @param p Permission object to store in database
     * @return Permission saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/permissions", notes = "Save Permission", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response savePermission(
            @ApiParam(value = "Permission object to store in database", required = true) Permission p) {
        getLog().log(Level.FINE, "In Rest Service POST /hyperiot/permissions \n Body: {0}", p);
        return this.save(p);
    }

    /**
     * Service updates a permission
     *
     * @param p Permission object to update in database
     * @return Permission updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/permissions", notes = "Update Permission", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    public Response updatePermission(
            @ApiParam(value = "Permission object to update in database", required = true) Permission p) {
        getLog().log(Level.FINE, "In Rest Service PUT /hyperiot/permissions \n Body: {0}", p);
        return this.update(p);
    }

    /**
     * Service deletes an existing permission
     *
     * @param id id from which permission object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/permissions/{id}", notes = "Delete Permission", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response deletePermission(
            @ApiParam(value = "id from which permission object will deleted", required = true) @PathParam("id") long id) {
        getLog().log(Level.FINE, "In Rest Service DELETE /hyperiot/permissions/{0}", id);
        return this.remove(id);
    }

    /**
     * Service finds all available permissions
     *
     * @return List of all available permissions
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/permissions/all", notes = "Find All Permission", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllPermission() {
        getLog().log(Level.FINE, "In Rest Service GET /hyperiot/permissions");
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
    @ApiOperation(value = "/hyperiot/permissions", notes = "Find All Permission", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllPermissionPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        return this.findAll(delta, page);
    }

    /**
     * Service finds all available permissions
     *
     * @return List of all available permissions
     */
    @GET
    @Path("/actions")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/permissions/actions", notes = "Find All available actions", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllActions() {
        getLog().log(Level.FINE, "In Rest Service GET /hyperiot/permissions");
        List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTActions();
        HyperIoTContext context = this.getHyperIoTContext();
        try {
            if (HyperIoTSecurityUtil.checkPermission(context, Permission.class.getName(), HyperIoTActionsUtil
                    .getHyperIoTAction(Permission.class.getName(), HyperIoTPermissionAction.LIST_ACTIONS))) {
                HashMap<String, List<HyperIoTAction>> actionsMap = new HashMap<>();
                actions.forEach(action -> {
                    if (!actionsMap.containsKey(action.getCategory())) {
                        actionsMap.put(action.getCategory(), new ArrayList<>());
                    }
                    actionsMap.get(action.getCategory()).add(action);
                });
                return Response.ok().entity(actionsMap).build();
            } else
                throw new HyperIoTUnauthorizedException();
        } catch (Throwable t) {
            return this.handleException(t);
        }
    }

}
