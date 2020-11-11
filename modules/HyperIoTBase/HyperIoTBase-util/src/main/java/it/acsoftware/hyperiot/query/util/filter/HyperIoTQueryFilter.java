package it.acsoftware.hyperiot.query.util.filter;

public interface HyperIoTQueryFilter {
    boolean isNot();

    HyperIoTQueryFilter and(HyperIoTQueryFilter filter);

    HyperIoTQueryFilter or(HyperIoTQueryFilter filter);

    HyperIoTQueryFilter not();
}
