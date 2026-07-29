-- Optimistic locking for the food_post reservation lifecycle.
--
-- Without a version column two concurrent POST /api/food/{id}/reserve requests
-- both read status = 'AVAILABLE', both pass the in-entity guard, and both write
-- status = 'RESERVED'. The second write silently overwrites the first, so two
-- takers each believe they hold the reservation and the giver receives two
-- food.claimed notifications. Hibernate's @Version turns the second write into
-- an OptimisticLockingFailureException, which the API surfaces as 409.
--
-- Existing rows start at version 0; NOT NULL keeps Hibernate from treating a
-- pre-existing row as transient.
ALTER TABLE food_post
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
