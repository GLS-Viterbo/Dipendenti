package it.gls.dipendenti.shift.exception;

public class OverlappingShiftException extends RuntimeException {
    public OverlappingShiftException(String message) {
        super(message);
    }
    public OverlappingShiftException() {super("Overlapping shift found");}
}
