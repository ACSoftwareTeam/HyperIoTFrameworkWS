package it.acsoftware.hyperiot.mail.api;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseRepository;

import it.acsoftware.hyperiot.mail.model.MailTemplate;

/**
 * 
 * @author Aristide Cittadino Interface component for Mail Repository.
 *         It is used for CRUD operations,
 *         and to interact with the persistence layer.
 *
 */
public interface MailRepository extends HyperIoTBaseRepository<MailTemplate> {
	public MailTemplate findByName(String name);
}
