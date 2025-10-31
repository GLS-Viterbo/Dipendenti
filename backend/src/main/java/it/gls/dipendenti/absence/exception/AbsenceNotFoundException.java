package it.gls.dipendenti.absence.exception;

public class AbsenceNotFoundException extends RuntimeException {
    public AbsenceNotFoundException() {
        super("Absence not found");
    }

    public AbsenceNotFoundException(String message) {
        super(message);
    }
}