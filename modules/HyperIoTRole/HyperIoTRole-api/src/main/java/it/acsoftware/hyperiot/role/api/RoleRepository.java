package it.acsoftware.hyperiot.role.api;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseRepository;
import it.acsoftware.hyperiot.role.model.Role;

/**
 * 
 * @author Aristide Cittadino Interface component for Role Repository.
 *         RoleRepository is used for CRUD operations,
 *         and to interact with the persistence layer.
 *
 */
public interface RoleRepository extends HyperIoTBaseRepository<Role> {
    /**
     *
     * @param name role Name
     * @return Role
     */
    public Role findByName(String name);
}
