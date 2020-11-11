package it.acsoftware.hyperiot.base.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnedResource;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;
import it.acsoftware.hyperiot.base.validation.NotNullOnPersist;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author Aristide Cittadino Model class for HyperIoTAbstractEntity. This class
 * implements HyperIoTBaseEntity methods and maps primary key of entity
 * in the database.
 */
@MappedSuperclass
@Embeddable
public abstract class HyperIoTAbstractEntity extends HyperIoTAbstractResource
        implements HyperIoTBaseEntity {

    /**
     * long id, indicates primary key of entity
     */
    private long id;

    /**
     * Version identifier for optimistic locking
     */
    private int entityVersion = 1;

    /**
     * Auto filled: create date
     */
    private Date entityCreateDate;

    /**
     * Auto filled: update date
     */
    private Date entityModifyDate;

    private long[] categoryIds;

    private long[] tagIds;

    /**
     * Gets the primary key of entity
     */
    @Id
    @GeneratedValue
    @NotNullOnPersist
    public long getId() {
        return id;
    }

    /**
     * Sets the primary key of entity
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return get date of entity creation
     */
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entity_create_date")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Date getEntityCreateDate() {
        return entityCreateDate;
    }

    /**
     * @param createDate
     */
    public void setEntityCreateDate(Date createDate) {
        this.entityCreateDate = createDate;
    }

    /**
     * @return the date od the entity update
     */
    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entity_modify_date")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Date getEntityModifyDate() {
        return entityModifyDate;
    }

    /**
     * @param modifyDate
     */
    public void setEntityModifyDate(Date modifyDate) {
        this.entityModifyDate = modifyDate;
    }

    /**
     * @return Version management for optimistic locking
     */
    @Version
    @NotNull
    @Column(name="entity_version",columnDefinition = "INTEGER default 1")
    public int getEntityVersion() {
        return entityVersion;
    }

    /**
     * @param version
     */
    public void setEntityVersion(int version) {
        this.entityVersion = version;
    }

    /**
     * Not persistend on database used to reference categories with the
     * HyperIoTAssetCategoryManager
     *
     * @return categoryIds
     */
    @Transient
    public long[] getCategoryIds() {
        return categoryIds;
    }

    /**
     * @param categoryIds category Ids
     */
    public void setCategoryIds(long[] categoryIds) {
        this.categoryIds = categoryIds;
    }

    /**
     * Not persisted on database used to reference tags with the
     * HyperIoTAssetTagManager
     *
     * @return categoryIds
     */
    @Transient
    public long[] getTagIds() {
        return tagIds;
    }

    /**
     * @param tagIds tagIds
     */
    public void setTagIds(long[] tagIds) {
        this.tagIds = tagIds;
    }

    /*@Override
    @Transient
    @JsonIgnore
    public HyperIoTBaseEntity getParent() {
        return null;
    }*/

    @Override
    @Transient
    @JsonIgnore
    public String getSystemApiClassName() {
        String className = this.getClass().getName();
        return className.replace(".model.", ".api.") + "SystemApi";
    }

}