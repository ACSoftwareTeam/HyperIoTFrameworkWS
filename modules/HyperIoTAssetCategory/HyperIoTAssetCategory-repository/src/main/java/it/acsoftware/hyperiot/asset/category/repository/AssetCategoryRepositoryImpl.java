package it.acsoftware.hyperiot.asset.category.repository;

import it.acsoftware.hyperiot.asset.category.api.AssetCategoryRepository;
import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.asset.category.model.AssetCategoryResource;
import it.acsoftware.hyperiot.base.api.HyperIoTAssetCategoryManager;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import org.apache.aries.jpa.template.JpaTemplate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.Query;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the AssetCategory. This
 * class is used to interact with the persistence layer.
 */
@Component(service = {AssetCategoryRepository.class,
        HyperIoTAssetCategoryManager.class}, immediate = true)
public class AssetCategoryRepositoryImpl extends HyperIoTBaseRepositoryImpl<AssetCategory>
        implements AssetCategoryRepository, HyperIoTAssetCategoryManager {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Constructor for a AssetCategoryRepositoryImpl
     */
    public AssetCategoryRepositoryImpl() {
        super(AssetCategory.class);
    }

    /**
     * @return The current jpaTemplate
     */
    @Override
    protected JpaTemplate getJpa() {
        getLog().log(Level.FINEST, "invoking getJpa, returning: {0}", jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Override
    @Reference(target = "(osgi.unit.name=hyperiot-assetCategory-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        getLog().log(Level.FINEST, "invoking setJpa, setting: {0}", jpa);
        this.jpa = jpa;
    }

    /**
     * Find the assect cagegory resource from the cateogry id
     */
    @Override
    public AssetCategoryResource findAssetCategoryResource(String resourceName, long resourceId,
                                                           long categoryId) {
        return jpa.txExpr(entityManager -> {
            Query q = entityManager.createQuery(
                    "from AssetCategoryResource res where res.category.id=:categoryId and res.resourceName = :resourceName and res.resourceId = :resourceId",
                    AssetCategoryResource.class);
            q.setParameter("categoryId", categoryId);
            q.setParameter("resourceName", resourceName);
            q.setParameter("resourceId", resourceId);
            return (AssetCategoryResource) q.getSingleResult();
        });
    }

    /**
     * Add category to a resources
     */
    @Override
    public void addAssetCategory(String resourceName, long resourceId, long categoryId) {
        getLog().log(Level.FINEST,
                "invoking addAssetCategory for resource:  {0} - {1}", new Object[]{resourceName, resourceId});
        AssetCategory category = this.find(categoryId, null);
        AssetCategoryResource acr = new AssetCategoryResource();
        acr.setCategory(category);
        acr.setResourceId(resourceId);
        acr.setResourceName(resourceName);
        category.getResources().add(acr);
        this.update(category);
    }

    /**
     * Add multiple categories to a resource
     */
    @Override
    public void addAssetCategories(String resourceName, long resourceId, long[] categoriesId) {
        getLog().log(Level.FINEST,
                "invoking addAssetCategories for resource: {0} - {1}", new Object[]{resourceName, resourceId});
        for (int i = 0; i < categoriesId.length; i++) {
            AssetCategory category = this.find(categoriesId[i], null);
            AssetCategoryResource acr = new AssetCategoryResource();
            acr.setCategory(category);
            acr.setResourceId(resourceId);
            acr.setResourceName(resourceName);
            category.getResources().add(acr);
            this.update(category);
        }
    }

    /**
     * Remove a category from a resource
     */
    @Override
    public void removeAssetCategory(String resourceName, long resourceId, long categoryId) {
        getLog().log(Level.FINEST,
                "invoking removeAssetCategory for resource: {0} - {1}", new Object[]{resourceName, resourceId});
        AssetCategory category = this.find(categoryId, null);
        AssetCategoryResource acr = this.findAssetCategoryResource(resourceName, resourceId,
                categoryId);
        category.getResources().remove(acr);
        this.update(category);
    }

    /**
     * removes list of categories from a resource
     */
    @Override
    public void removeAssetCategories(String resourceName, long resourceId, long[] categoriesId) {
        getLog().log(Level.FINEST,
                "invoking removeAssetCategories for resource: {0} - {1}", new Object[]{resourceName, resourceId});
        for (int i = 0; i < categoriesId.length; i++) {
            AssetCategory category = this.find(categoriesId[i], null);
            AssetCategoryResource acr = this.findAssetCategoryResource(resourceName, resourceId,
                    category.getId());
            category.getResources().remove(acr);
            this.update(category);
        }
    }

}
