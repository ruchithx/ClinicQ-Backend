package com.dispensary.dms.prescription;

/**
 * Pure transition table for prescription status (spec section 10.4). No Spring, no I/O.
 */
public final class PrescriptionStateMachine {

    public enum Action {
        START, DISPENSE, CANCEL
    }

    public enum Kind {
        CHANGED, NO_OP, INVALID
    }

    public record Transition(Kind kind, PrescriptionStatus target) {
        public boolean isValid() {
            return kind != Kind.INVALID;
        }

        public boolean isChange() {
            return kind == Kind.CHANGED;
        }
    }

    public Transition evaluate(PrescriptionStatus from, Action action) {
        return switch (action) {
            case START -> switch (from) {
                case PENDING -> changed(PrescriptionStatus.IN_PROGRESS);
                case IN_PROGRESS -> noop(from);
                default -> invalid();
            };
            case DISPENSE -> switch (from) {
                case PENDING, IN_PROGRESS -> changed(PrescriptionStatus.DISPENSED);
                default -> invalid();
            };
            case CANCEL -> switch (from) {
                case PENDING, IN_PROGRESS -> changed(PrescriptionStatus.CANCELLED);
                case CANCELLED -> noop(from);
                default -> invalid();
            };
        };
    }

    private Transition changed(PrescriptionStatus target) {
        return new Transition(Kind.CHANGED, target);
    }

    private Transition noop(PrescriptionStatus from) {
        return new Transition(Kind.NO_OP, from);
    }

    private Transition invalid() {
        return new Transition(Kind.INVALID, null);
    }
}
