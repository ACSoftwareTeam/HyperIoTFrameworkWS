package it.acsoftware.hyperiot.base.service;

import java.util.logging.Logger;

public class HyperIoTAbstractService {
    private final Logger log = Logger.getLogger(this.getClass().getName());

    /**
     * @return the default logger for the class
     */
    protected Logger getLog() {
        return this.log;
    }
}
