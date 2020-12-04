package it.acsoftware.hyperiot.query.util.filter;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTQueryFilter;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class HyperIoTQueryFilterBuilder {
    /**
     * Creates a filter from an hashmap using and condition for default
     *
     * @param filter
     * @return
     */
    public static HyperIoTQueryFilter createFromMapWithOrCondition(HashMap<String, Object> filter) {
        return createFromMap(filter, false);
    }

    /**
     *
     * @param filter
     * @return
     */
    public static HyperIoTQueryFilter createFromMapWithAndCondition(HashMap<String, Object> filter) {
        return createFromMap(filter, true);
    }

    /**
     *
     * @param filter
     * @param inAnd
     * @return
     */
    private static HyperIoTQueryFilter createFromMap(HashMap<String, Object> filter, boolean inAnd) {
        HyperIoTQueryFilter finalFilter = null;
        if (filter != null && filter.size() > 0) {
            Iterator<String> it = filter.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                HyperIoTQueryEqualsFilter eqFilter = new HyperIoTQueryEqualsFilter(key, filter.get(key));
                if (finalFilter == null)
                    finalFilter = eqFilter;
                else if (inAnd)
                    finalFilter = finalFilter.and(eqFilter);
                else
                    finalFilter = finalFilter.or(eqFilter);
            }
        }
        return finalFilter;
    }

    /**
     *
     * @param entityDef
     * @param criteriaBuilder
     * @param filter
     * @param <T>
     * @return
     */
    public static <T> Predicate createPredicateByQueryFilter(Root<T> entityDef, CriteriaBuilder criteriaBuilder, HyperIoTQueryFilter filter) {
        if (filter instanceof HyperIoTQueryEqualsFilter) {
            return createEqualPredicate(entityDef, criteriaBuilder, (HyperIoTQueryEqualsFilter) filter);
        }
        if (filter instanceof HyperIoTQueryInFilter) {
            return createInPredicate(entityDef, criteriaBuilder, (HyperIoTQueryInFilter) filter);
        }
        if (filter instanceof HyperIoTQueryFilterAndCondition) {
            return createAndPredicate(entityDef, criteriaBuilder, (HyperIoTQueryFilterAndCondition) filter);
        }
        if (filter instanceof HyperIoTQueryFilterOrCondition) {
            return createOrPredicate(entityDef, criteriaBuilder, (HyperIoTQueryFilterOrCondition) filter);
        }
        return null;
    }

    /**
     *
     * @param entityDef
     * @param criteriaBuilder
     * @param filter
     * @param <T>
     * @return
     */
    private static <T> Predicate createAndPredicate(Root<T> entityDef, CriteriaBuilder criteriaBuilder, HyperIoTQueryFilterAndCondition filter) {
        HyperIoTQueryFilter left = filter.leftOperand();
        HyperIoTQueryFilter right = filter.rightOperand();

        Predicate leftPredicate = createPredicateByQueryFilter(entityDef, criteriaBuilder, left);
        Predicate rightPredicate = createPredicateByQueryFilter(entityDef, criteriaBuilder, right);
        Predicate andPredicate = criteriaBuilder.and(leftPredicate, rightPredicate);

        if (filter.isNot()) {
            return andPredicate.not();
        }
        return andPredicate;
    }

    /**
     *
     * @param entityDef
     * @param criteriaBuilder
     * @param filter
     * @param <T>
     * @return
     */
    private static <T> Predicate createEqualPredicate(Root<T> entityDef, CriteriaBuilder criteriaBuilder, HyperIoTQueryEqualsFilter filter) {
        String name = filter.getName();
        Object value = filter.getValue();

        Path<?> p = getPath(entityDef, name);

        if (filter.isNot()) {
            return criteriaBuilder.notEqual(p, value);
        }
        return criteriaBuilder.equal(p, value);
    }

    /**
     *
     * @param entityDef
     * @param criteriaBuilder
     * @param filter
     * @param <C>
     * @param <T>
     * @return
     */
    private static <C extends Collection<?>, T> Predicate createInPredicate(Root<T> entityDef, CriteriaBuilder criteriaBuilder, HyperIoTQueryInFilter<C> filter) {
        String name = filter.getName();
        C value = filter.getValues();

        Path<?> p = getPath(entityDef, name);

        if (filter.isNot()) {
            return criteriaBuilder.not(p.in(value));
        }
        return p.in(value);
    }

    /**
     *
     * @param entityDef
     * @param criteriaBuilder
     * @param filter
     * @param <T>
     * @return
     */
    private static <T> Predicate createOrPredicate(Root<T> entityDef, CriteriaBuilder criteriaBuilder, HyperIoTQueryFilterOrCondition filter) {
        HyperIoTQueryFilter left = filter.leftOperand();
        HyperIoTQueryFilter right = filter.rightOperand();

        Predicate leftPredicate = createPredicateByQueryFilter(entityDef, criteriaBuilder, left);
        Predicate rightPredicate = createPredicateByQueryFilter(entityDef, criteriaBuilder, right);
        Predicate orPredicate = criteriaBuilder.or(leftPredicate, rightPredicate);

        if (filter.isNot()) {
            return orPredicate.not();
        }
        return orPredicate;
    }

    /**
     *
     * @param entityDef
     * @param name
     * @param <T>
     * @return
     */
    private static <T> Path<?> getPath(Root<T> entityDef, String name) {
        String[] dottedRelationships = name.split("\\.");
        Path<?> p = entityDef.get(dottedRelationships[0]);
        for (int i = 1; i < dottedRelationships.length; i++) {
            p = p.get(dottedRelationships[i]);
        }
        return p;
    }

}
