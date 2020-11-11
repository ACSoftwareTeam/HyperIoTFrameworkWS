package it.acsoftware.hyperiot.base.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class GenericExceptionMapperProvider implements ExceptionMapper<Throwable> {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    class ErrorMessage {
        int status;
        String message;
        String developerMessage;

        public ErrorMessage(int status, String message) {
            this.status = status;
            String[] messages = message.split("\\|");
            this.message = messages[0];
            if (messages.length > 1) {
                this.developerMessage = messages[1];
            }
        }

        public int getStatus() {
            return this.status;
        }

        public String getMessage() {
            return this.message;
        }

        public String getDeveloperMessage() {
            return this.developerMessage;
        }
    }

    public Response toResponse(Throwable ex) {
        log.log(Level.SEVERE, ex.getMessage(), ex);
        ErrorMessage errorMessage = new ErrorMessage(500,
                "An internal error has occurred|" + ex.getMessage());
        return Response.serverError().entity(errorMessage).type(MediaType.APPLICATION_JSON).build();

    }

}
