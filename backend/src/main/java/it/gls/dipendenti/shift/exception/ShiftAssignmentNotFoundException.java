package it.gls.dipendenti.shift.exception;

public class ShiftAssignmentNotFoundException extends RuntimeException {
    public ShiftAssignmentNotFoundException(String message) {
        super(message);
    }
    public ShiftAssignmentNotFoundException() {super("Shift assignment not found");}
}
