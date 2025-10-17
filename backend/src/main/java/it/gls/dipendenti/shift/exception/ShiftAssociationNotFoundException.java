package it.gls.dipendenti.shift.exception;

public class ShiftAssociationNotFoundException extends RuntimeException {
    public ShiftAssociationNotFoundException(String message) {
        super(message);
    }
    public ShiftAssociationNotFoundException() {super("Association not found"); }
}
