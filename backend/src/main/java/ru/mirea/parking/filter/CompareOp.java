package ru.mirea.parking.filter;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

public enum CompareOp {
    EQ, GT, GTE, LT, LTE;

    public <T extends Comparable<? super T>> Predicate apply(CriteriaBuilder cb, Expression<T> field, T value) {
        return switch (this) {
            case EQ -> cb.equal(field, value);
            case GT -> cb.greaterThan(field, value);
            case GTE -> cb.greaterThanOrEqualTo(field, value);
            case LT -> cb.lessThan(field, value);
            case LTE -> cb.lessThanOrEqualTo(field, value);
        };
    }
}
