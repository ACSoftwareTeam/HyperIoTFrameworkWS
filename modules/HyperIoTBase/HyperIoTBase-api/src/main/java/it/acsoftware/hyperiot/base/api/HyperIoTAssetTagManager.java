package it.acsoftware.hyperiot.base.api;

public interface HyperIoTAssetTagManager {
    public void addAssetTag(String resourceName, long resourceId, long tagId);

    public void addAssetTags(String resourceName, long resourceId, long[] tagsId);

    public void removeAssetTag(String resourceName, long resourceId, long tagId);

    public void removeAssetTags(String resourceName, long resourceId, long[] tagsId);
}
