package it.acsoftware.hyperiot.asset.tag.service.rest;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.asset.tag.api.AssetTagApi;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino AssetTag rest service class. Registered with DOSGi
 * CXF
 */
@SwaggerDefinition(basePath = "/assets/tags", info = @Info(description = "HyperIoT AssetTag API", version = "2.0.0", title = "HyperIoT AssetTag", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")), securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
        @ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/assets/tags", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = AssetTagRestApi.class, property = {
        "service.exported.interfaces=it.acsoftware.hyperiot.asset.tag.service.rest.AssetTagRestApi",
        "service.exported.configs=org.apache.cxf.rs", "org.apache.cxf.rs.address=/assets/tags",
        "service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
        "service.exported.intents=exceptionmapper", "service.exported.intents=swagger"}, immediate = true)
@Path("")
public class AssetTagRestApi extends HyperIoTBaseEntityRestApi<AssetTag> {
    private AssetTagApi entityService;

    /**
     * Simple service for checking module status
     *
     * @return HyperIoT AssetTag Module work!
     */
    @GET
    @Path("/module/status")
    @ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
    public Response checkModuleWorking() {
        getLog().log(Level.FINE, "In Rest Service GET /hyperiot/assets/tags/module/status");
        return Response.ok("AssetTag Module works!").build();
    }

    /**
     * @Return the current entityService
     */
    @Override
    protected HyperIoTBaseEntityApi<AssetTag> getEntityService() {
        getLog().log(Level.FINEST, "invoking getEntityService, returning: {0}" , this.entityService);
        return entityService;
    }

    /**
     * @param entityService: Injecting entityService
     */
    @Reference(service = AssetTagApi.class)
    protected void setEntityService(AssetTagApi entityService) {
        getLog().log(Level.FINEST, "invoking setEntityService, setting: {0}" , this.entityService);
        this.entityService = entityService;
    }

    /**
     * Service finds an existing AssetTag
     *
     * @param id id from which AssetTag object will retrieved
     * @return AssetTag if found
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/assets/tags/{id}", notes = "Service for finding assettag", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response findAssetTag(
            @ApiParam(value = "id from which AssetTag object will retrieve", required = true) @PathParam("id") long id) {
        getLog().log(Level.FINE, "In Rest Service GET /hyperiot/assets/tags/{0}" , id);
        return this.find(id);
    }

    /**
     * Service saves a new AssetTag
     *
     * @param entity AssetTag object to store in database
     * @return the AssetTag saved
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/assets/tags", notes = "Service for adding a new assettag entity", httpMethod = "POST", produces = "application/json", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response saveAssetTag(
            @ApiParam(value = "AssetTag entity which must be saved ", required = true) AssetTag entity) {
        getLog().log(Level.FINE, "In Rest Service POST /hyperiot/assets/tags/ \n Body: {0}", entity);
        return this.save(entity);
    }

    /**
     * Service updates a AssetTag
     *
     * @param entity AssetTag object to update in database
     * @return the AssetTag updated
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/assets/tags", notes = "Service for updating a assettag entity", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Invalid ID supplied")})
    public Response updateAssetTag(
            @ApiParam(value = "AssetTag entity which must be updated ", required = true) AssetTag entity) {
        getLog().log(Level.FINE, "In Rest Service PUT /hyperiot/assets/tags/ \n Body: {0}" , entity);
        return this.update(entity);
    }

    /**
     * Service deletes a AssetTag
     *
     * @param id id from which AssetTag object will deleted
     * @return 200 OK if it has been deleted
     */
    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/assets/tags/{id}", notes = "Service for deleting a assettag entity", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found")})
    public Response deleteAssetTag(
            @ApiParam(value = "The assettag id which must be deleted", required = true) @PathParam("id") long id) {
        getLog().log(Level.FINE, "In Rest Service DELETE /hyperiot/assets/tags/{0}" , id);
        return this.remove(id);
    }

    /**
     * Service finds all available AssetTag
     *
     * @return list of all available AssetTag
     */
    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/assets/tags/all", notes = "Service for finding all assettag entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllAssetTag() {
        getLog().log(Level.FINE, "In Rest Service GET /hyperiot/assets/tags/all");
        return this.findAll();
    }

    /**
     * Service finds all available AssetTag
     *
     * @return list of all available AssetTag
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/assets/tags", notes = "Service for finding all assettag entities", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 500, message = "Internal error")})
    public Response findAllAssetTagPaginated(@QueryParam("delta") Integer delta, @QueryParam("page") Integer page) {
        getLog().log(Level.FINE, "In Rest Service GET /hyperiot/assets/tags/");
        return this.findAll(delta, page);
    }

}
