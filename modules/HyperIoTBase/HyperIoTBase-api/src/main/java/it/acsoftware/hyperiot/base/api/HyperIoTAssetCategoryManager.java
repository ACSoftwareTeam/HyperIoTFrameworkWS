package it.acsoftware.hyperiot.base.api;

public interface HyperIoTAssetCategoryManager {
    public void addAssetCategory(String resourceName, long resourceId, long categoryId);

    public void addAssetCategories(String resourceName, long resourceId, long[] categoriesId);

    public void removeAssetCategory(String resourceName, long resourceId, long categoryId);

    public void removeAssetCategories(String resourceName, long resourceId, long[] categoriesId);
}
