package it.acsoftware.hyperiot.authentication.model.principal;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;

public class HyperIoTRolePrincipal extends RolePrincipal {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public HyperIoTRolePrincipal(String name) {
		super(name);
	}

}
