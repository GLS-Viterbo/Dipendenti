package it.gls.dipendenti.shift.exception;

public class OverlappingWithAbsenceException extends RuntimeException {
    public OverlappingWithAbsenceException(String message) {
        super(message);
    }
    public OverlappingWithAbsenceException() {super("Shift is overlapping with an absence");}
}
