package it.acsoftware.hyperiot.query.util.filter;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTQueryFilter;

import java.util.Collection;

public class HyperIoTQueryInFilter<T extends Collection<?>> implements HyperIoTQueryFilter {
    private String name;
    private T values;
    private boolean not;

    public HyperIoTQueryInFilter(String name, T values) {
        super();
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public T getValues() {
        return values;
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
