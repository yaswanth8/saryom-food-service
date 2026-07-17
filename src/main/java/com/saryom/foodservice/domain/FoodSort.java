package com.saryom.foodservice.domain;

import org.springframework.data.domain.Sort;

/** Ordering options for the food feed. */
public enum FoodSort {
    NEWEST,
    NEAREST,
    EXPIRING_SOON;

    /** Sort applied by the database for the non-geo browse path. */
    public Sort toSort() {
        return switch (this) {
            // NEAREST needs per-request coordinates, so it can only be applied in
            // the geo path; fall back to newest for a straight DB query.
            case NEWEST, NEAREST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case EXPIRING_SOON -> Sort.by(Sort.Direction.ASC, "bestBefore");
        };
    }
}
