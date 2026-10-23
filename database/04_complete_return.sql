BEGIN;

DO $$
DECLARE
    v_claim_id BIGINT;
    v_match_id BIGINT;
    v_lost_report_id BIGINT;
    v_found_item_id BIGINT;
    v_passenger_id BIGINT;
    v_staff_id BIGINT;
    v_verification_id BIGINT;
    v_storage_id BIGINT;
    v_slot_number INTEGER;
    v_return_id BIGINT;
    v_rows INTEGER;
    v_now TIMESTAMPTZ := CURRENT_TIMESTAMP;
BEGIN
    /*
     * 1. Locate and lock the eligible claim, match, report,
     *    and found-item records.
     */
    SELECT
        c.claim_id,
        m.match_id,
        lr.lost_report_id,
        f.found_item_id,
        p.passenger_id,
        s.staff_id
    INTO
        v_claim_id,
        v_match_id,
        v_lost_report_id,
        v_found_item_id,
        v_passenger_id,
        v_staff_id
    FROM app.claim AS c
    JOIN app.item_match AS m
        ON m.match_id = c.match_id
    JOIN app.lost_report AS lr
        ON lr.lost_report_id = m.lost_report_id
    JOIN app.passenger AS p
        ON p.passenger_id = lr.passenger_id
    JOIN app.item AS lost_item
        ON lost_item.item_id = lr.item_id
    JOIN app.found_item AS f
        ON f.found_item_id = m.found_item_id
    JOIN app.item AS found_item
        ON found_item.item_id = f.item_id
    JOIN app.staff AS s
        ON s.email = 'aarav.officer@example.test'
       AND s.is_active = TRUE
       AND s.staff_role IN (
           'lost_found_officer',
           'administrator'
       )
    WHERE p.email = 'riya.patel@example.test'
      AND lost_item.description =
          'Passenger description: black charging case with a small scratch.'
      AND found_item.description =
          'Found item description: black charging case with a small scratch; recovered near Gate 4.'
      AND m.verification_status = 'verified'
      AND c.claim_status = 'submitted'
      AND f.item_status = 'stored'
    FOR UPDATE OF c, m, lr, f;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No eligible submitted claim was found. Check the existing workflow states.';
    END IF;


    /*
     * 2. Lock the pending identity-verification record.
     */
    SELECT iv.verification_id
    INTO v_verification_id
    FROM app.identity_verification AS iv
    WHERE iv.claim_id = v_claim_id
      AND iv.verification_method = 'in_person_document_check'
      AND iv.verification_result = 'pending'
      AND iv.verified_by_staff_id IS NULL
      AND iv.verified_at IS NULL
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'The expected pending identity-verification record was not found.';
    END IF;


    /*
     * 3. Locate and lock the active storage assignment.
     */
    SELECT
        sa.storage_id,
        sa.slot_number
    INTO
        v_storage_id,
        v_slot_number
    FROM app.storage_assignment AS sa
    JOIN app.storage_slot AS ss
        ON ss.storage_id = sa.storage_id
       AND ss.slot_number = sa.slot_number
    WHERE sa.found_item_id = v_found_item_id
      AND sa.released_at IS NULL
      AND ss.slot_status = 'available'
    FOR UPDATE OF sa, ss;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'No active storage assignment was found for the item.';
    END IF;


    /*
     * 4. Simulate successful identity verification.
     */
    UPDATE app.identity_verification
    SET
        verification_result = 'verified',
        verified_by_staff_id = v_staff_id,
        verified_at = v_now,
        verification_notes =
            'Demonstration only: identity verification simulated for DBMS practice.'
    WHERE verification_id = v_verification_id
      AND verification_result = 'pending'
      AND verified_by_staff_id IS NULL
      AND verified_at IS NULL;

    GET DIAGNOSTICS v_rows = ROW_COUNT;

    IF v_rows <> 1 THEN
        RAISE EXCEPTION
            'Expected to verify one identity record, but updated %.', v_rows;
    END IF;


    /*
     * 5. Approve the claim.
     */
    UPDATE app.claim
    SET
        claim_status = 'approved',
        reviewed_by_staff_id = v_staff_id,
        reviewed_at = v_now,
        decision_notes =
            'Demonstration approval after simulated identity verification.'
    WHERE claim_id = v_claim_id
      AND claim_status = 'submitted';

    GET DIAGNOSTICS v_rows = ROW_COUNT;

    IF v_rows <> 1 THEN
        RAISE EXCEPTION
            'Expected to approve one claim, but updated %.', v_rows;
    END IF;


    /*
     * 6. Record the completed counter handover.
     */
    INSERT INTO app.return_delivery (
        claim_id,
        return_method,
        return_status,
        handled_by_staff_id,
        delivery_notes,
        created_at,
        completed_at
    )
    VALUES (
        v_claim_id,
        'counter_handover',
        'completed',
        v_staff_id,
        'Demonstration handover after simulated identity verification.',
        v_now,
        v_now
    )
    RETURNING return_id INTO v_return_id;


    /*
     * 7. Release the active storage assignment.
     */
    UPDATE app.storage_assignment
    SET released_at = v_now
    WHERE found_item_id = v_found_item_id
      AND released_at IS NULL;

    GET DIAGNOSTICS v_rows = ROW_COUNT;

    IF v_rows <> 1 THEN
        RAISE EXCEPTION
            'Expected to release one storage assignment, but updated %.', v_rows;
    END IF;


    /*
     * 8. Mark the found item as returned.
     */
    UPDATE app.found_item
    SET
        item_status = 'returned',
        updated_at = v_now
    WHERE found_item_id = v_found_item_id
      AND item_status = 'stored';

    GET DIAGNOSTICS v_rows = ROW_COUNT;

    IF v_rows <> 1 THEN
        RAISE EXCEPTION
            'Expected to update one found item, but updated %.', v_rows;
    END IF;


    /*
     * 9. Resolve the lost report.
     */
    UPDATE app.lost_report
    SET
        report_status = 'resolved',
        updated_at = v_now
    WHERE lost_report_id = v_lost_report_id
      AND report_status IN ('open', 'candidate_found');

    GET DIAGNOSTICS v_rows = ROW_COUNT;

    IF v_rows <> 1 THEN
        RAISE EXCEPTION
            'Expected to resolve one lost report, but updated %.', v_rows;
    END IF;


    /*
     * 10. Notify the passenger that the demonstration return is complete.
     */
    INSERT INTO app.notification (
        passenger_id,
        notification_type,
        subject,
        message
    )
    VALUES (
        v_passenger_id,
        'return',
        'Item return completed',
        'The demonstration workflow has recorded the item as returned.'
    );


    /*
     * 11. Write an audit record.
     */
    INSERT INTO app.audit_log (
        actor_staff_id,
        action_type,
        entity_name,
        entity_id,
        details,
        occurred_at
    )
    VALUES (
        v_staff_id,
        'ITEM_RETURNED',
        'return_delivery',
        v_return_id,
        jsonb_build_object(
            'claim_id', v_claim_id,
            'match_id', v_match_id,
            'found_item_id', v_found_item_id,
            'storage_id', v_storage_id,
            'slot_number', v_slot_number,
            'demonstration', TRUE
        ),
        v_now
    );

    RAISE NOTICE
        'Demonstration return recorded. Claim ID: %, Return ID: %, Released slot: %/%',
        v_claim_id,
        v_return_id,
        v_storage_id,
        v_slot_number;
END;
$$;

COMMIT;
