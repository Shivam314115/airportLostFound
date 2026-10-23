BEGIN;

SET ROLE airport_owner;


-- 1. Require a verified match and verified identity before claim approval.

CREATE OR REPLACE FUNCTION app.fn_validate_claim_approval()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_match_status VARCHAR(25);
BEGIN
    IF NEW.claim_status = 'approved'
       AND OLD.claim_status IS DISTINCT FROM NEW.claim_status
    THEN
        SELECT m.verification_status
        INTO v_match_status
        FROM app.item_match AS m
        WHERE m.match_id = NEW.match_id;

        IF v_match_status IS DISTINCT FROM 'verified' THEN
            RAISE EXCEPTION
                'A claim cannot be approved until its match is verified.'
                USING ERRCODE = '23514';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM app.identity_verification AS iv
            WHERE iv.claim_id = NEW.claim_id
              AND iv.verification_result = 'verified'
              AND iv.verified_by_staff_id IS NOT NULL
              AND iv.verified_at IS NOT NULL
        ) THEN
            RAISE EXCEPTION
                'A claim cannot be approved until identity is verified.'
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;


-- 2. Require an approved claim and verified identity before completing a return.

CREATE OR REPLACE FUNCTION app.fn_validate_completed_return()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_claim_status VARCHAR(25);
    v_match_status VARCHAR(25);
BEGIN
    IF NEW.return_status <> 'completed' THEN
        RETURN NEW;
    END IF;

    SELECT
        c.claim_status,
        m.verification_status
    INTO
        v_claim_status,
        v_match_status
    FROM app.claim AS c
    JOIN app.item_match AS m
        ON m.match_id = c.match_id
    WHERE c.claim_id = NEW.claim_id;

    IF v_claim_status IS DISTINCT FROM 'approved' THEN
        RAISE EXCEPTION
            'A return cannot be completed before the claim is approved.'
            USING ERRCODE = '23514';
    END IF;

    IF v_match_status IS DISTINCT FROM 'verified' THEN
        RAISE EXCEPTION
            'A return requires a verified item match.'
            USING ERRCODE = '23514';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM app.identity_verification AS iv
        WHERE iv.claim_id = NEW.claim_id
          AND iv.verification_result = 'verified'
          AND iv.verified_by_staff_id IS NOT NULL
          AND iv.verified_at IS NOT NULL
    ) THEN
        RAISE EXCEPTION
            'A return requires verified passenger identity.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;


-- 3. Require a completed return and released storage before marking an item returned.

CREATE OR REPLACE FUNCTION app.fn_validate_found_item_returned()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.item_status = 'returned'
       AND OLD.item_status IS DISTINCT FROM NEW.item_status
    THEN
        IF EXISTS (
            SELECT 1
            FROM app.storage_assignment AS sa
            WHERE sa.found_item_id = NEW.found_item_id
              AND sa.released_at IS NULL
        ) THEN
            RAISE EXCEPTION
                'Release the active storage assignment before returning the item.'
                USING ERRCODE = '23514';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM app.item_match AS m
            JOIN app.claim AS c
                ON c.match_id = m.match_id
            JOIN app.return_delivery AS r
                ON r.claim_id = c.claim_id
            JOIN app.identity_verification AS iv
                ON iv.claim_id = c.claim_id
            WHERE m.found_item_id = NEW.found_item_id
              AND m.verification_status = 'verified'
              AND c.claim_status = 'approved'
              AND r.return_status = 'completed'
              AND iv.verification_result = 'verified'
              AND iv.verified_by_staff_id IS NOT NULL
              AND iv.verified_at IS NOT NULL
        ) THEN
            RAISE EXCEPTION
                'An item can only be returned after a valid completed return.'
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;


-- 4. Validate active storage assignments.

CREATE OR REPLACE FUNCTION app.fn_validate_storage_assignment()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_item_status VARCHAR(25);
    v_slot_status VARCHAR(20);
BEGIN
    -- Released assignments are historical records, not active assignments.
    IF NEW.released_at IS NOT NULL THEN
        RETURN NEW;
    END IF;

    SELECT f.item_status
    INTO v_item_status
    FROM app.found_item AS f
    WHERE f.found_item_id = NEW.found_item_id;

    IF v_item_status IS DISTINCT FROM 'stored' THEN
        RAISE EXCEPTION
            'Only a stored found item can receive an active storage assignment.'
            USING ERRCODE = '23514';
    END IF;

    SELECT ss.slot_status
    INTO v_slot_status
    FROM app.storage_slot AS ss
    WHERE ss.storage_id = NEW.storage_id
      AND ss.slot_number = NEW.slot_number;

    IF v_slot_status IS DISTINCT FROM 'available' THEN
        RAISE EXCEPTION
            'The selected storage slot is unavailable or does not exist.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;


-- 5. Prevent identity verification from being downgraded after approval or return.

CREATE OR REPLACE FUNCTION app.fn_prevent_identity_downgrade()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.verification_result IS DISTINCT FROM 'verified'
       AND EXISTS (
           SELECT 1
           FROM app.claim AS c
           LEFT JOIN app.return_delivery AS r
               ON r.claim_id = c.claim_id
           WHERE c.claim_id = NEW.claim_id
             AND (
                 c.claim_status = 'approved'
                 OR r.return_status = 'completed'
             )
       )
    THEN
        RAISE EXCEPTION
            'Identity verification cannot be downgraded after claim approval or return completion.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;


-- Install the triggers.
-- DROP IF EXISTS makes trigger replacement safe when rerunning this file.

DROP TRIGGER IF EXISTS trg_claim_approval_requires_verification
    ON app.claim;

CREATE TRIGGER trg_claim_approval_requires_verification
BEFORE UPDATE OF claim_status
ON app.claim
FOR EACH ROW
EXECUTE FUNCTION app.fn_validate_claim_approval();


DROP TRIGGER IF EXISTS trg_completed_return_requires_approval
    ON app.return_delivery;

CREATE TRIGGER trg_completed_return_requires_approval
BEFORE INSERT OR UPDATE
ON app.return_delivery
FOR EACH ROW
EXECUTE FUNCTION app.fn_validate_completed_return();


DROP TRIGGER IF EXISTS trg_found_item_return_requires_completed_return
    ON app.found_item;

CREATE TRIGGER trg_found_item_return_requires_completed_return
BEFORE UPDATE OF item_status
ON app.found_item
FOR EACH ROW
EXECUTE FUNCTION app.fn_validate_found_item_returned();


DROP TRIGGER IF EXISTS trg_storage_assignment_requires_available_slot
    ON app.storage_assignment;

CREATE TRIGGER trg_storage_assignment_requires_available_slot
BEFORE INSERT OR UPDATE
ON app.storage_assignment
FOR EACH ROW
EXECUTE FUNCTION app.fn_validate_storage_assignment();


DROP TRIGGER IF EXISTS trg_identity_verification_cannot_be_downgraded
    ON app.identity_verification;

CREATE TRIGGER trg_identity_verification_cannot_be_downgraded
BEFORE UPDATE OF verification_result
ON app.identity_verification
FOR EACH ROW
EXECUTE FUNCTION app.fn_prevent_identity_downgrade();


-- Trigger functions should not be directly executable by arbitrary users.
REVOKE ALL ON FUNCTION app.fn_validate_claim_approval() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.fn_validate_completed_return() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.fn_validate_found_item_returned() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.fn_validate_storage_assignment() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.fn_prevent_identity_downgrade() FROM PUBLIC;

GRANT EXECUTE ON FUNCTION app.fn_validate_claim_approval()
    TO airport_app;
GRANT EXECUTE ON FUNCTION app.fn_validate_completed_return()
    TO airport_app;
GRANT EXECUTE ON FUNCTION app.fn_validate_found_item_returned()
    TO airport_app;
GRANT EXECUTE ON FUNCTION app.fn_validate_storage_assignment()
    TO airport_app;
GRANT EXECUTE ON FUNCTION app.fn_prevent_identity_downgrade()
    TO airport_app;

COMMIT;

RESET ROLE;
