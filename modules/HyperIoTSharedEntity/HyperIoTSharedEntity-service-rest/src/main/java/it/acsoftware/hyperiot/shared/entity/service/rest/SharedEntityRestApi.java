package  it.acsoftware.hyperiot.shared.entity.service.rest;

import java.util.HashMap;
import java.util.logging.Level;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;

import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseEntityRestApi ;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi ;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntityApi;

import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;


/**
 * 
 * @author Aristide Cittadino SharedEntity rest service class. Registered with DOSGi CXF
 * 
 */
@SwaggerDefinition(basePath = "/sharedentity", info = @Info(description = "HyperIoT SharedEntity API", version = "2.0.0", title = "hyperiot SharedEntity", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")),securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
		@ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/sharedentity", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = SharedEntityRestApi.class, property = { 
	    "service.exported.interfaces=it.acsoftware.hyperiot.shared.entity.service.rest.SharedEntityRestApi",
		"service.exported.configs=org.apache.cxf.rs","org.apache.cxf.rs.address=/sharedentity",
		"service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
		"service.exported.intents=swagger","service.exported.intents=exceptionmapper"
		 }, immediate = true)
@Path("")
public class SharedEntityRestApi extends HyperIoTBaseEntityRestApi<SharedEntity>  {
	private SharedEntityApi entityService ;

	/**
	 * Simple service for checking module status
	 * 
	 * @return HyperIoT Role Module work!
	 */
	@GET
	@Path("/module/status")
	@ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
	public Response checkModuleWorking() {
		log.log(Level.FINE, "In Rest Service GET /hyperiot/sharedentity/module/status");
		return Response.ok("SharedEntity Module works!").build();
	}

	/**
	 * @Return the current entityService
	 */
	@Override
	protected HyperIoTBaseEntityApi<SharedEntity> getEntityService() {
		log.log(Level.FINEST, "invoking getEntityService, returning: {}" , this.entityService);
		return entityService;
	}

	/**
	 * 
	 * @param entityService: Injecting entityService 
	 */
	@Reference(service = SharedEntityApi.class)
	protected void setEntityService(SharedEntityApi entityService) {
		log.log(Level.FINEST, "invoking setEntityService, setting: {}" , this.entityService);
		this.entityService = entityService;
	}

	/**
	 * 
	 * @param entityService: Unsetting current entityService
	 */
	protected void unsetEntityService(SharedEntityApi entityService) {
		log.log(Level.FINEST, "invoking unsetEntityService, setting: {}" , entityService);
		this.entityService = entityService;
	}

	/**
	 * Service saves a new SharedEntity
	 * 
	 * @param entity SharedEntity object to store in database
	 * @return the SharedEntity saved
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/sharedentity", notes = "Service for adding a new sharedentity entity", httpMethod = "POST", produces = "application/json", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response saveSharedEntity(
		@ApiParam(value = "SharedEntity entity which must be saved ", required = true) SharedEntity entity) {
		log.log(Level.FINE, "In Rest Service POST /hyperiot/sharedentity \n Body: {}" , entity);
		return this.save(entity);
	}

	/**
	 * Service finds all available sharedentity
	 * 
	 * @return list of all available sharedentity
	 */
	@GET
	@Path("/all")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/sharedentity/all", notes = "Service for finding all sharedentity entities", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAllSharedEntity() {
		log.log(Level.FINE, "In Rest Service GET /hyperiot/sharedentity/");
		return this.findAll();
	}

	/**
	 * Service finds all available sharedentity
	 *
	 * @return list of all available sharedentity
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/sharedentity", notes = "Service for finding all sharedentity entities", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal error") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findAllSharedEntityPaginated(@QueryParam("delta") Integer delta,@QueryParam("page") Integer page) {
		log.log(Level.FINE, "In Rest Service GET /hyperiot/sharedentity/");
		return this.findAll(delta,page);
	}

	/**
	 * Service deletes a SharedEntity
	 *
	 * @param sharedEntity the SharedEntity object will deleted
	 * @return 200 OK if it has been deleted
	 */
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/sharedentity", notes = "Service for deleting a sharedentity entity", httpMethod = "DELETE", consumes = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
	@JsonView(HyperIoTJSONView.Public.class)
	public Response deleteSharedEntity(
			@ApiParam(value = "The sharedentity which must be deleted", required = true) SharedEntity sharedEntity) {
		log.log(Level.FINE, "In Rest Service DELETE /hyperiot/sharedentity/");
		try {
			String entityResourceName = sharedEntity.getEntityResourceName();
			long entityId = sharedEntity.getEntityId();
			long userId = sharedEntity.getUserId();

			this.log.log(Level.FINER,
					"Invoking Remove entity from rest service for {0} with primary key: (entityResourceName: {1}, entityId: {2}, userId: {3})",
					new Object[]{this.getEntityService().getEntityType().getSimpleName(), entityResourceName, entityId, userId});

			this.entityService.removeByPK(entityResourceName, entityId, userId, this.getHyperIoTContext());
			return Response.ok().build();
		} catch (Throwable var4) {
			return this.handleException(var4);
		}
	}

	/**
	 * Service finds an existing SharedEntity by its primary key
	 *
	 * @param sharedEntity The SharedEntity to be found
	 * @return  SharedEntity if found
	 */
	@GET
	@Path("/findByPK")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/sharedentity/findByPK",
			notes = "Service for finding sharedentity", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findByPK(@ApiParam(value = "SharedEntity entity which must find ", required = true) SharedEntity sharedEntity) {
		log.log(Level.FINE, "In Rest Service GET /hyperiot/sharedentity/findByPK {0}" , sharedEntity);
		try {
			String entityResourceName = sharedEntity.getEntityResourceName();
			long entityId = sharedEntity.getEntityId();
			long userId = sharedEntity.getUserId();

			return Response.ok(entityService.findByPK(entityResourceName, entityId, userId, null, this.getHyperIoTContext())).build();
		} catch (Throwable t) {
			return this.handleException(t);
		}
	}

	/**
	 * Service finds all available SharedEntities by given entityId and entityResourceName
	 * @param entityId The entity id
	 * @param entityResourceName The resource name of the entity
	 * @return list of all available SharedEntities by given entityId and entityResourceName
	 */
	@GET
	@Path("/findByEntity")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/sharedentity/findByEntity",
			notes = "Service for finding SharedEntity objects", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findByEntity(@QueryParam("entityResourceName") String entityResourceName, @QueryParam("entityId") long entityId) {
		log.log(Level.FINE, "In Rest Service GET /hyperiot/sharedentity/findByEntity?entityResourceName={0}&entityId={1}",
				new Object[]{entityResourceName, entityId});
		try {
			return Response.ok(entityService.findByEntity(entityResourceName, entityId, (HashMap<String, Object>)null, this.getHyperIoTContext())).build();
		} catch (Throwable t) {
			return this.handleException(t);
		}
	}

	/**
	 * Service finds all available SharedEntity objects by given userId
	 * @param userId The id by which SharedEntity object will by retrieved
	 * @return list of all available SharedEntity objects by given userId
	 */
	@GET
	@Path("/findByUser/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/sharedentity/findByUser/{userId}",
			notes = "Service for finding sharedentity", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response findByUser(@PathParam("userId") long userId) {
		log.log(Level.FINE, "In Rest Service GET /hyperiot/sharedentity/findByUser/{0}" , userId);
		try {
			return Response.ok(entityService.findByUser(userId, (HashMap<String, Object>)null, this.getHyperIoTContext())).build();
		} catch (Throwable t) {
			return this.handleException(t);
		}
	}

	/**
	 * Service finds users shared by the entity
	 *
	 * @param entityResourceName The resource name of the entity
	 * @param entityId The id of the entity
	 * @return  list of users shared by the entity with given entityId and given entityResourceName
	 */
	@GET
	@Path("/getUsers")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/sharedentity/getUsers",
			notes = "Service for finding sharing users by entity", httpMethod = "GET", produces = "application/json",authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Entity not found") })
	@JsonView(HyperIoTJSONView.Public.class)
	public Response getUsers(@QueryParam("entityResourceName") String entityResourceName, @QueryParam("entityId") long entityId) {
		log.log(Level.FINE,
				"In Rest Service GET /hyperiot/sharedentity/getUsers?entityResourceName={0}&entityId={1}" ,
				new Object[]{entityResourceName, entityId});
		try {
			return Response.ok(entityService.getSharingUsers(entityResourceName, entityId, this.getHyperIoTContext())).build();
		} catch (Throwable t) {
			return this.handleException(t);
		}
	}
	
}
