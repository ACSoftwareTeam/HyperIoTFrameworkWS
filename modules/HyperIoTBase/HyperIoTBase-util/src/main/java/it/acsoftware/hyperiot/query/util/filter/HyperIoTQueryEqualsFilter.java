package it.acsoftware.hyperiot.query.util.filter;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTQueryFilter;

public class HyperIoTQueryEqualsFilter implements HyperIoTQueryFilter {
    private String name;
    private Object value;
    private boolean not;

    public HyperIoTQueryEqualsFilter(String name, Object value) {
        super();
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public boolean isNot() {
        return not;
    }

    @Override
    public HyperIoTQueryFilter and(HyperIoTQueryFilter filter) {
        return new HyperIoTQueryFilterAndCondition(this, filter);
    }

    @Override
    public HyperIoTQueryFilter or(HyperIoTQueryFilter filter) {
        return new HyperIoTQueryFilterOrCondition(this, filter);
    }

    @Override
    public HyperIoTQueryFilter not() {
        this.not = true;
        return this;
    }
}
