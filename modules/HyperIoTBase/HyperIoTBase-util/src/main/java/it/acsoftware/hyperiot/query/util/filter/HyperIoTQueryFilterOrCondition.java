package it.acsoftware.hyperiot.query.util.filter;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTQueryFilter;

public class HyperIoTQueryFilterOrCondition implements HyperIoTQueryFilter {
    private HyperIoTQueryFilter left;
    private HyperIoTQueryFilter right;
    private boolean not;

    public HyperIoTQueryFilterOrCondition(HyperIoTQueryFilter left, HyperIoTQueryFilter right) {
        this.left = left;
        this.right = right;
    }

    public HyperIoTQueryFilter leftOperand() {
        return left;
    }

    public HyperIoTQueryFilter rightOperand() {
        return right;
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
        this.not = !this.not;
        return this;
    }
}
