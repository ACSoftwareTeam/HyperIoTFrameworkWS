package  it.acsoftware.hyperiot.hbase.connector.service.rest;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation;
import it.acsoftware.hyperiot.base.api.HyperIoTBaseApi;
import it.acsoftware.hyperiot.base.model.HyperIoTJSONView;
import it.acsoftware.hyperiot.base.security.rest.LoggedIn;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorApi;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseTimelineColumnFamily;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;


/**
 * 
 * @author Aristide Cittadino HBaseConnector rest service class. Registered with DOSGi CXF
 * 
 */
@SwaggerDefinition(basePath = "/hbaseconnectors", info = @Info(description = "HyperIoT HBaseConnector API", version = "2.0.0", title = "hyperiot HBaseConnector", contact = @Contact(name = "ACSoftware.it", email = "users@acsoftware.it")),securityDefinition = @SecurityDefinition(apiKeyAuthDefinitions = {
		@ApiKeyAuthDefinition(key = "jwt-auth", name = "AUTHORIZATION", in = ApiKeyLocation.HEADER)}))
@Api(value = "/hbaseconnectors", produces = "application/json")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = HBaseConnectorRestApi.class, property = { 
	    "service.exported.interfaces=it.acsoftware.hyperiot.hbase.connector.service.rest.HBaseConnectorRestApi",
		"service.exported.configs=org.apache.cxf.rs","org.apache.cxf.rs.address=/hbaseconnectors",
		"service.exported.intents=jackson", "service.exported.intents=jwtAuthFilter",
		"service.exported.intents=swagger","service.exported.intents=exceptionmapper"
		 }, immediate = true)
@Path("")
public class HBaseConnectorRestApi extends  HyperIoTBaseRestApi  {
	private HBaseConnectorApi  service;

	private final String PACKET_IDS_REGEX = "\\d+(,\\d+)*";

	/**
	 * Check if there's an active connection to the database.
	 * @return Response object
	 */
	@GET
	@Path("/checkConnection")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/hbaseconnector/checkConnection", notes = "Check HBase client connection", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"),
			@ApiResponse(code = 500, message = "Internal server error")
	})
	public Response checkConnection() {
		log.log(Level.FINE, "In REST Service GET /hyperiot/hbase/checkConnection");
		try {
			service.checkConnection(this.getHyperIoTContext());
			return Response.ok("Connection OK").build();
		} catch (Throwable e) {
			return handleException(e);
		}
	}

	/**
	 * Simple service for checking module status
	 * 
	 * @return HyperIoT Role Module work!
	 */
	@GET
	@Path("/module/status")
	@ApiOperation(value = "/module/status", notes = "Simple service for checking module status", httpMethod = "GET")
	public Response checkModuleWorking() {
		log.log(Level.FINE, "In Rest Service GET /hyperiot/hbaseconnector/module/status");
		return Response.ok("HBaseConnector Module works!").build();
	}

	/**
	 * Service creates table
	 * //@param tableName Table name
	 * //@param columnFamilies Column families
	 * @return Response object
	 */
	@POST
	@Path("/createTable")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/hbaseconnector/createTable", notes = "Service for creating new table", httpMethod = "POST", consumes = "application/x-www-form-urlencoded", produces = "application/json", authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error")})
	@JsonView(HyperIoTJSONView.Public.class)
	public Response createTable(
			@FormParam("tableName") String tableName,
			@FormParam("columnFamilies") String columnFamilies) {
		log.log(Level.FINE, "In Rest Service POST /hyperiot/hbaseconnector/createTable");
		log.log(Level.FINE, "\t tableName: {0}", tableName);
		log.log(Level.FINE, "\t columnFamilies: {0}", columnFamilies);
		try {
			List<String> columnFamilyList = Arrays.asList(columnFamilies.trim().split(","));
			service.createTable(this.getHyperIoTContext(), tableName, columnFamilyList);
			return Response.ok("Table created").build();
		} catch (Throwable e) {
			return handleException(e);
		}
	}

	/**
	 * Service deletes row from table
	 * @param tableName Table name
	 * @param rowKey Row key
	 * @return Response object
	 */
	@DELETE
	@Path("/deleteData")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/hbaseconnector/deleteData", notes = "Service for delete data", httpMethod = "DELETE", consumes = "application/x-www-form-urlencoded", produces = "application/json", authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error")})
	@JsonView(HyperIoTJSONView.Public.class)
	public Response deleteData(
			@FormParam(value = "tableName") String tableName,
			@FormParam(value = "rowKey") String rowKey) {
		log.log(Level.FINE, "In Rest Service DELETE hyperiot/hbaseconnector/deleteData");
		log.log(Level.FINE, "\t tableName: {0}", tableName);
		log.log(Level.FINE, "\t rowKey: {0}", rowKey);
		try {
			service.deleteData(this.getHyperIoTContext(), tableName, rowKey);
			return Response.ok().entity("Row deleted").build();
		} catch (Throwable e) {
			return handleException(e);
		}
	}

    /**
     * Service disables table
     * @param tableName Table name to disable
     * @return Response object
     */
    @PUT
	@Path("/disableTable/{tableName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hbaseconnector/disableTable/{tableName}", notes = "Service for disabling table", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response disableTable(
    		@ApiParam(value = "Table name which must be disabled", required = true)
			@PathParam("tableName") String tableName) {
		log.log(Level.FINE, "In REST Service PUT /hyperiot/hbaseconnector/disableTable/{0}", tableName);
        try {
            service.disableTable(this.getHyperIoTContext(), tableName);
            return Response.ok().entity("Table disabled").build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Service drops table
     *
     * @param tableName Table to drop
     * @return Response object
     */
    @DELETE
    @Path("/dropTable/{tableName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hbaseconnector/dropTable/{tableName}", notes = "Service for deleting a table", httpMethod = "DELETE", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"),
            @ApiResponse(code = 404, message = "Entity not found"),
			@ApiResponse(code = 500, message = "Internal error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response dropTable(
            @ApiParam(value = "The table name which must be dropped", required = true)
			@PathParam("tableName") String tableName) {
        log.log(Level.FINE, "In Rest Service DELETE /hyperiot/hbaseconnector/dropTable/{0}", tableName);
        try {
            service.dropTable(this.getHyperIoTContext(), tableName);
            return Response.ok().entity("Table dropped").build();
        } catch (Throwable e) {
            return handleException(e);
        }

    }

    /**
     * Service enables table
     *
     * @param tableName Table name to enable
     * @return Response object
     */
    @PUT
	@Path("/enableTable/{tableName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @LoggedIn
    @ApiOperation(value = "/hyperiot/hbaseconnector/enableTable/{tableName}", notes = "Service for enabling table", httpMethod = "PUT", consumes = "application/json", authorizations = @Authorization("jwt-auth"))
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
            @ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
            @ApiResponse(code = 500, message = "Internal Error")})
    @JsonView(HyperIoTJSONView.Public.class)
    public Response enableTable(
            @ApiParam(value = "Table name which must be enabled ", required = true)
			@PathParam("tableName") String tableName) {
        log.log(Level.FINE, "In Rest Service PUT hyperiot/hbaseconnector/enableTable/{0}", tableName);
        try {
            service.enableTable(this.getHyperIoTContext(), tableName);
            return Response.ok().entity("Table enabled").build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

	/**
	 * @return the current service class
	 */
	@SuppressWarnings("unused")
	protected HyperIoTBaseApi getService() {
		log.log(Level.FINEST, "invoking getService, returning: " + this.service);
		return service;
	}


	/**
	 * Service inserts data into table
	 * @param tableName Table name
	 * @param rowKey Row key
	 * @param columnFamily Column Family
	 * @param column Column
	 * @param cellValue Value to insert
	 * @return Response object
	 */
	@PUT
	@Path("/insertData")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/hbaseconnector/insertData", notes = "Service for insert data", httpMethod = "PUT", consumes = "application/x-www-form-urlencoded", produces = "application/json", authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error")})
	@JsonView(HyperIoTJSONView.Public.class)
	public Response insertData(
			@FormParam(value = "tableName") String tableName,
			@FormParam(value = "rowKey") String rowKey,
			@FormParam(value = "columnFamily") String columnFamily,
			@FormParam(value = "column") String column,
			@FormParam(value = "cellValue") String cellValue) {
		log.log(Level.FINE, "In Rest Service PUT hyperiot/hbaseconnector/insertData");
		log.log(Level.FINE, "\t tableName: {0}", tableName);
		log.log(Level.FINE, "\t rowKey: {0}", rowKey);
		log.log(Level.FINE, "\t columnFamily: {0}", columnFamily);
		log.log(Level.FINE, "\t column: {0}", column);
		log.log(Level.FINE, "\t cellValue: {0}", cellValue);
		try {
			service.insertData(this.getHyperIoTContext(), tableName, rowKey, columnFamily, column, cellValue);
			return Response.ok().entity("Value inserted").build();
		} catch (Throwable e) {
			return handleException(e);
		}
	}

	/**
	 * Service scans and returns data from table
	 * @param tableName Table name
	 * @param columnFamily Column Family
	 * @param column Column
	 * @param rowKeyLowerBound Row key lower bound
	 * @param rowKeyUpperBound Row key upper bound
	 * @return Response object
	 */
	@GET
	@Path("/scanAvroHPackets/{tableName}/{columnFamily}/{column}/{rowKeyLowerBound}/{rowKeyUpperBound}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/hbaseconnector/scanAvroHPackets/{tableName}/{columnFamily}/{column}/{rowKeyLowerBound}/{rowKeyUpperBound}", notes = "Service for scan data", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error")})
	@JsonView(HyperIoTJSONView.Public.class)
	public Response scanAvroHPackets(
			@ApiParam(value = "Table name which retrieve hpackets from", required = true) @PathParam("tableName") String tableName,
			@ApiParam(value = "HBase table column family", required = true) @PathParam("columnFamily") String columnFamily,
			@ApiParam(value = "Column inside a column family", required = true) @PathParam("column") long column,
			@ApiParam(value = "HBase row key lower bound", required = true) @PathParam("rowKeyLowerBound") long rowKeyLowerBound,
			@ApiParam(value = "HBase row key upper bound", required = true) @PathParam("rowKeyUpperBound") long rowKeyUpperBound) {
		log.log(Level.FINE, "In Rest Service GET hyperiot/hbaseconnector/scanAvroHPackets/{0}/{1}/{2}/{3}/{4}",
				new Object[] {tableName, columnFamily, column, rowKeyLowerBound, rowKeyUpperBound});
		try {
			if(rowKeyLowerBound > rowKeyUpperBound)
				throw new IllegalArgumentException("startTime must be prior or equal to endTime");
			return Response.ok(service.scanAvroHPackets(this.getHyperIoTContext(), tableName, columnFamily, column, rowKeyLowerBound, rowKeyUpperBound)).build();
		} catch (IOException | IllegalArgumentException e) {
			return handleException(e);
		}
	}

	@GET
	@Path("/scanHProject/{hProjectId}/{hPacketIds}/{rowKeyLowerBound}/{rowKeyUpperBound}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/hbaseconnector/scanHProject/{hProjectId}/{hPacketIds}/{rowKeyLowerBound}/{rowKeyUpperBound}", notes = "Service for scan HProject data", httpMethod = "GET", produces = "application/json", authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error")})
	@JsonView(HyperIoTJSONView.Public.class)
	public Response scanHProject(
			@ApiParam(value = "HProject ID from retrieve HPackets in Avro format", required = true) @PathParam("hProjectId") long hProjectId,
			@ApiParam(value = "HPacket list, containing comma separated ID", required = true) @PathParam("hPacketIds") String hPacketIds,
			@ApiParam(value = "HBase row key lower bound", required = true) @PathParam("rowKeyLowerBound") long rowKeyLowerBound,
			@ApiParam(value = "HBase row key upper bound", required = true) @PathParam("rowKeyUpperBound") long rowKeyUpperBound) {
		log.log(Level.FINE, "In Rest Service GET hyperiot/hbaseconnector/scanHProject/{0}/{1}/{2}/{3}",
				new Object[] {hProjectId, hPacketIds, rowKeyLowerBound, rowKeyUpperBound});
		try {
			if(rowKeyLowerBound > rowKeyUpperBound)
				throw new IllegalArgumentException("startTime must be prior or equal to endTime");
			if(!Pattern.matches(PACKET_IDS_REGEX, hPacketIds))
				throw new IllegalArgumentException("wrong packetIds parameter");
			List<String> packetIdsList = new ArrayList<>(new HashSet<>(Arrays.asList(hPacketIds.trim().split(",")))); // remove duplicates
			return Response.ok(service.scanHProject(this.getHyperIoTContext(), hProjectId, packetIdsList, rowKeyLowerBound, rowKeyUpperBound)).build();
		} catch (IOException | IllegalArgumentException e) {
			return handleException(e);
		}
	}

	/**
	 * Service count HPacket event number from timeline table
	 * @param tableName Table name
	 * @param packetIds Packet IDs
	 * @param startTime Timeline start time
	 * @param endTime Timeline end time
	 * @return Response object
	 */
	@GET
	@Path("/timeline/event/count/{tableName}/{packetIds}/{startTime}/{endTime}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/hbaseconnector/timeline/event/count/{tableName}/{packetIds}/{startTime}/{endTime}",
			notes = "Service for count data and get it back", httpMethod = "GET", produces = "application/json",
			authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error")})
	@JsonView(HyperIoTJSONView.Public.class)
	public Response timelineEventCount(
			@ApiParam(value = "Table name which count hpacket event number from", required = true) @PathParam("tableName") String tableName,
			@ApiParam(value = "HPacket list, containing comma separated ID", required = true) @PathParam("packetIds") String packetIds,
			@ApiParam(value = "Scanning start time", required = true) @PathParam("startTime") long startTime,
			@ApiParam(value = "Scanning end time", required = true) @PathParam("endTime") long endTime) {
		log.log(Level.FINE, "In Rest Service GET hyperiot/hbaseconnector/timeline/event/count/{0}/{1}/{2}/{3}",
				new Object[] {tableName, packetIds, startTime, endTime});
		try {
			if(startTime > endTime)
				throw new IllegalArgumentException("startTime must be prior or equal to endTime");
			if(!Pattern.matches(PACKET_IDS_REGEX, packetIds))
				throw new IllegalArgumentException("wrong packetIds parameter");
			List<String> packetIdsList = new ArrayList<>(new HashSet<>(Arrays.asList(packetIds.trim().split(",")))); // remove duplicates
			return Response.ok(service.timelineEventCount(this.getHyperIoTContext(), tableName, packetIdsList, startTime, endTime)).build();
		} catch (IOException | IllegalArgumentException | ParseException e) {
			return handleException(e);
		}
	}

	/**
	 * Service scans and returns data from timeline table
	 * @param tableName Table name
	 * @param packetIds Packet IDs
	 * @param step Step, which determines output
	 * @param granularity Search granularity
	 * @param startTime Timeline start time
	 * @param endTime Timeline end time
	 * @return Response object
	 */
	@GET
	@Path("/timeline/scan/{tableName}/{packetIds}/{step}/{granularity}/{startTime}/{endTime}/{timezone}")
	@Produces(MediaType.APPLICATION_JSON)
	@LoggedIn
	@ApiOperation(value = "/hyperiot/hbaseconnector/timeline/scan/{tableName}/{packetIds}/{step}/{granularity}/{startTime}/{endTime}/{timezone}",
			notes = "Service for scan data and get it back for timeline queries", httpMethod = "GET", produces = "application/json",
			authorizations = @Authorization("jwt-auth"))
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Successful operation"),
			@ApiResponse(code = 403, message = "Not authorized"), @ApiResponse(code = 422, message = "Not validated"),
			@ApiResponse(code = 500, message = "Internal error")})
	@JsonView(HyperIoTJSONView.Public.class)
	public Response timelineScan(
			@ApiParam(value = "Table name which count hpacket event number from", required = true) @PathParam("tableName") String tableName,
			@ApiParam(value = "HPacket list, containing comma separated ID", required = true) @PathParam("packetIds") String packetIds,
			@ApiParam(value = "Scanning step", required = true) @PathParam("step") String step,
			@ApiParam(value = "Scanning granularity", required = true) @PathParam("granularity") String granularity,
			@ApiParam(value = "Scanning start time", required = true) @PathParam("startTime") long startTime,
			@ApiParam(value = "Scanning end time", required = true) @PathParam("endTime") long endTime,
			@ApiParam(value = "Timezone Timezone of client which has invoked the method, i.e. Europe/Rome", required = true) @PathParam("timezone") String timezone) {
		log.log(Level.FINE, "In Rest Service GET hyperiot/hbaseconnector/timeline/scan/{0}/{1}/{2}/{3}/{4}/{5}",
				new Object[] {tableName, packetIds, step, granularity, startTime, endTime});
		try {
			HBaseTimelineColumnFamily convertedStep = HBaseTimelineColumnFamily.valueOf(step.toUpperCase());
			HBaseTimelineColumnFamily convertedGranularity = HBaseTimelineColumnFamily.valueOf(granularity.toUpperCase());
			if (convertedStep.getOrder() > convertedGranularity.getOrder())
				throw new IllegalArgumentException("step must be prior or equal to granularity");
			if(startTime > endTime)
				throw new IllegalArgumentException("startTime must be prior or equal to endTime");
			if(!Pattern.matches(PACKET_IDS_REGEX, packetIds))
				throw new IllegalArgumentException("wrong packetIds parameter");
			List<String> packetIdsList = new ArrayList<>(new HashSet<>(Arrays.asList(packetIds.trim().split(",")))); // remove duplicates
			return Response.ok(service.timelineScan(this.getHyperIoTContext(), tableName, packetIdsList,
					convertedStep, convertedGranularity, startTime, endTime, timezone)).build();
		} catch (IOException | IllegalArgumentException | ParseException e) {
			return handleException(e);
		}
	}

	/**
	 * 
	 * @param service: Injecting service class
	 */
	@Reference(service = HBaseConnectorApi.class)
	protected void setService(HBaseConnectorApi service) {
		log.log(Level.FINEST, "invoking setService, setting: " + service);
		this.service = service;
	}
	
}
