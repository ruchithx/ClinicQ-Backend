package com.dispensary.dms.queue;

import java.util.Set;

/**
 * Pure transition table for queue-entry status (spec section 10.4). No Spring, no I/O.
 */
public final class QueueEntryStateMachine {

    public enum Action {
        CALL, COMPLETE, SKIP, REQUEUE, CANCEL
    }

    public enum Kind {
        CHANGED, NO_OP, INVALID
    }

    public record Transition(Kind kind, QueueEntryStatus target) {
        public boolean isValid() {
            return kind != Kind.INVALID;
        }

        public boolean isChange() {
            return kind == Kind.CHANGED;
        }
    }

    public Transition evaluate(QueueEntryStatus from, Action action) {
        QueueEntryStatus target = targetOf(action);
        if (from == target) {
            return new Transition(Kind.NO_OP, from);
        }
        if (allowedSources(action).contains(from)) {
            return new Transition(Kind.CHANGED, target);
        }
        return new Transition(Kind.INVALID, null);
    }

    private QueueEntryStatus targetOf(Action action) {
        return switch (action) {
            case CALL -> QueueEntryStatus.SERVING;
            case COMPLETE -> QueueEntryStatus.COMPLETED;
            case SKIP -> QueueEntryStatus.SKIPPED;
            case REQUEUE -> QueueEntryStatus.WAITING;
            case CANCEL -> QueueEntryStatus.CANCELLED;
        };
    }

    private Set<QueueEntryStatus> allowedSources(Action action) {
        return switch (action) {
            case CALL -> Set.of(QueueEntryStatus.WAITING);
            case COMPLETE -> Set.of(QueueEntryStatus.SERVING);
            case SKIP -> Set.of(QueueEntryStatus.SERVING);
            case REQUEUE -> Set.of(QueueEntryStatus.SKIPPED);
            case CANCEL -> Set.of(QueueEntryStatus.WAITING);
        };
    }
}
