package com.dispensary.dms.prescription;

import com.dispensary.dms.prescription.PrescriptionStateMachine.Action;
import com.dispensary.dms.prescription.PrescriptionStateMachine.Kind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** BT-012: exhaustive prescription transition table. */
class PrescriptionStateMachineTest {

    private final PrescriptionStateMachine sm = new PrescriptionStateMachine();

    @Test
    void pendingStartMovesToInProgress() {
        var t = sm.evaluate(PrescriptionStatus.PENDING, Action.START);
        assertThat(t.kind()).isEqualTo(Kind.CHANGED);
        assertThat(t.target()).isEqualTo(PrescriptionStatus.IN_PROGRESS);
    }

    @Test
    void pendingAndInProgressDispenseMovesToDispensed() {
        assertThat(sm.evaluate(PrescriptionStatus.PENDING, Action.DISPENSE).target())
                .isEqualTo(PrescriptionStatus.DISPENSED);
        assertThat(sm.evaluate(PrescriptionStatus.IN_PROGRESS, Action.DISPENSE).target())
                .isEqualTo(PrescriptionStatus.DISPENSED);
    }

    @Test
    void cancelFromPendingOrInProgress() {
        assertThat(sm.evaluate(PrescriptionStatus.PENDING, Action.CANCEL).target())
                .isEqualTo(PrescriptionStatus.CANCELLED);
        assertThat(sm.evaluate(PrescriptionStatus.IN_PROGRESS, Action.CANCEL).target())
                .isEqualTo(PrescriptionStatus.CANCELLED);
    }

    @Test
    void repeatStartOnInProgressIsNoOp() {
        assertThat(sm.evaluate(PrescriptionStatus.IN_PROGRESS, Action.START).kind()).isEqualTo(Kind.NO_OP);
    }

    @Test
    void repeatCancelOnCancelledIsNoOp() {
        assertThat(sm.evaluate(PrescriptionStatus.CANCELLED, Action.CANCEL).kind()).isEqualTo(Kind.NO_OP);
    }

    @Test
    void terminalDispensedRejectsEverything() {
        assertThat(sm.evaluate(PrescriptionStatus.DISPENSED, Action.START).kind()).isEqualTo(Kind.INVALID);
        assertThat(sm.evaluate(PrescriptionStatus.DISPENSED, Action.DISPENSE).kind()).isEqualTo(Kind.INVALID);
        assertThat(sm.evaluate(PrescriptionStatus.DISPENSED, Action.CANCEL).kind()).isEqualTo(Kind.INVALID);
    }

    @Test
    void cancelledRejectsStartAndDispense() {
        assertThat(sm.evaluate(PrescriptionStatus.CANCELLED, Action.START).kind()).isEqualTo(Kind.INVALID);
        assertThat(sm.evaluate(PrescriptionStatus.CANCELLED, Action.DISPENSE).kind()).isEqualTo(Kind.INVALID);
    }
}
