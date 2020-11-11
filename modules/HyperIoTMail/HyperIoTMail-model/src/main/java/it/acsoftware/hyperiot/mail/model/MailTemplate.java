package it.acsoftware.hyperiot.mail.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import org.hibernate.validator.constraints.Length;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.model.HyperIoTAbstractEntity;
import it.acsoftware.hyperiot.base.validation.NoMalitiusCode;

/**
 * 
 * @author Aristide Cittadino Model class for Mail of HyperIoT platform. This
 *         class is used to map Mail with the database.
 *
 */

@Entity
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = "name") })
public class MailTemplate extends HyperIoTAbstractEntity implements HyperIoTProtectedEntity {

	private String name;
	private String description;
	private String content;

	@NotNullOnPersist
	@NoMalitiusCode
	@NotEmpty
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@NotNullOnPersist
	@NoMalitiusCode
	@NotEmpty
	@Length(max = 3000)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Column(columnDefinition = "TEXT")
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}