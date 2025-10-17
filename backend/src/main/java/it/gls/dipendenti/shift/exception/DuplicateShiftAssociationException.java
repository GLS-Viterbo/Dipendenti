package it.gls.dipendenti.shift.exception;

public class DuplicateShiftAssociationException extends RuntimeException {
    public DuplicateShiftAssociationException(String message) {
        super(message);
    }
    public DuplicateShiftAssociationException() {
        super("The shift is already associated with this employee on this day");
    }
}
