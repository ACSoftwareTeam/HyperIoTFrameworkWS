package it.acsoftware.hyperiot.authentication.model;

import com.fasterxml.jackson.annotation.JsonView;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Aristide Cittadino Model class for JWT Token
 */
public class JWTLoginResponse {
    /**
     * Serialized and encrypted string for JWT Token
     */
    private String token;
    /**
     * User info
     */
    private HyperIoTAuthenticable authenticable;

    /**
     * HashMap which stores resourcenames and profiles, so permissions
     */
    private HashMap<String, JWTProfile> profile;


    public JWTLoginResponse(String token, HyperIoTAuthenticable authenticable) {
        super();
        this.token = token;
        this.profile = new HashMap<>();
        this.authenticable = authenticable;
    }

    /**
     * @return the encoded Token
     */
    public String getToken() {
        return token;
    }

    public HashMap<String, JWTProfile> getProfile() {
        return profile;
    }

    public HyperIoTAuthenticable getAuthenticable() {
        return authenticable;
    }

    public void setAuthenticable(HyperIoTAuthenticable authenticable) {
        this.authenticable = authenticable;
    }

}
