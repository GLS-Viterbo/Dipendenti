package it.gls.dipendenti.shift.exception;

public class ShiftNotFoundException extends RuntimeException {
    public ShiftNotFoundException(String message) {
        super(message);
    }
    public ShiftNotFoundException() {
        super("Shift not found");
    }
}
