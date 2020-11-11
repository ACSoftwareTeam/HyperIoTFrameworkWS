package it.acsoftware.hyperiot.base.service;

import java.util.logging.Logger;

import it.acsoftware.hyperiot.base.api.HyperIoTBaseApi;
import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;

/**
 * @author Aristide Cittadino Implementation class of HyperIoTBaseApi. It is
 * used to implement methods in order to interact with the system layer.
 */
public abstract class HyperIoTBaseServiceImpl implements HyperIoTBaseApi {
    protected Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    /**
     * @return The current SystemService
     */
    protected abstract HyperIoTBaseSystemApi getSystemService();

}
