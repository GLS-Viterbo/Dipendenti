package it.gls.dipendenti.shift.exception;

public class InvalidShiftException extends RuntimeException {
    public InvalidShiftException(String message) {
        super(message);
    }

    public InvalidShiftException() {
        super("shift is invalid");
    }
}
