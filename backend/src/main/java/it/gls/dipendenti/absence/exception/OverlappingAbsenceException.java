package it.gls.dipendenti.absence.exception;

public class OverlappingAbsenceException extends RuntimeException {
    public OverlappingAbsenceException() {
        super("Overlapping absence found for this period");
    }

    public OverlappingAbsenceException(String message) {
        super(message);
    }
}