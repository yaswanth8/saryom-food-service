package com.saryom.foodservice.domain;

/**
 * Lifecycle of a shared food post.
 *
 * <pre>
 *   AVAILABLE --reserve--> RESERVED --complete--> COMPLETED
 *       |                     |
 *       |                     +----release-------> AVAILABLE
 *       +--cancel--> CANCELLED  (also reachable from RESERVED)
 * </pre>
 *
 * Expiry is derived from {@code bestBefore} rather than being a stored status,
 * so no scheduler is needed — browse simply excludes posts past their time.
 */
public enum FoodStatus {
    AVAILABLE,
    RESERVED,
    COMPLETED,
    CANCELLED
}
