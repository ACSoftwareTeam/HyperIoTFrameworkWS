package it.acsoftware.hyperiot.base.api.entity;

public interface HyperIoTQueryFilter {
    boolean isNot();

    HyperIoTQueryFilter and(HyperIoTQueryFilter filter);

    HyperIoTQueryFilter or(HyperIoTQueryFilter filter);

    HyperIoTQueryFilter not();
}
