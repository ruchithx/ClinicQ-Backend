package com.dispensary.dms.queue;

import com.dispensary.dms.queue.QueueEntryStateMachine.Action;
import com.dispensary.dms.queue.QueueEntryStateMachine.Kind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** BT-020: exhaustive queue-entry transition table. */
class QueueEntryStateMachineTest {

    private final QueueEntryStateMachine sm = new QueueEntryStateMachine();

    @Test
    void waitingCallMovesToServing() {
        assertThat(sm.evaluate(QueueEntryStatus.WAITING, Action.CALL).target())
                .isEqualTo(QueueEntryStatus.SERVING);
    }

    @Test
    void servingCompleteAndSkip() {
        assertThat(sm.evaluate(QueueEntryStatus.SERVING, Action.COMPLETE).target())
                .isEqualTo(QueueEntryStatus.COMPLETED);
        assertThat(sm.evaluate(QueueEntryStatus.SERVING, Action.SKIP).target())
                .isEqualTo(QueueEntryStatus.SKIPPED);
    }

    @Test
    void skippedRequeueReturnsToWaiting() {
        assertThat(sm.evaluate(QueueEntryStatus.SKIPPED, Action.REQUEUE).target())
                .isEqualTo(QueueEntryStatus.WAITING);
    }

    @Test
    void waitingCancel() {
        assertThat(sm.evaluate(QueueEntryStatus.WAITING, Action.CANCEL).target())
                .isEqualTo(QueueEntryStatus.CANCELLED);
    }

    @Test
    void sameStateIsNoOp() {
        assertThat(sm.evaluate(QueueEntryStatus.SERVING, Action.CALL).kind()).isEqualTo(Kind.NO_OP);
        assertThat(sm.evaluate(QueueEntryStatus.COMPLETED, Action.COMPLETE).kind()).isEqualTo(Kind.NO_OP);
        assertThat(sm.evaluate(QueueEntryStatus.WAITING, Action.REQUEUE).kind()).isEqualTo(Kind.NO_OP);
    }

    @Test
    void invalidTransitions() {
        assertThat(sm.evaluate(QueueEntryStatus.COMPLETED, Action.CALL).kind()).isEqualTo(Kind.INVALID);
        assertThat(sm.evaluate(QueueEntryStatus.WAITING, Action.COMPLETE).kind()).isEqualTo(Kind.INVALID);
        assertThat(sm.evaluate(QueueEntryStatus.CANCELLED, Action.REQUEUE).kind()).isEqualTo(Kind.INVALID);
    }
}
