package it.gls.dipendenti.absence.exception;

public class DuplicateHolidayException extends RuntimeException {
    public DuplicateHolidayException(String message) {
        super(message);
    }

    public DuplicateHolidayException() {
        super("A holiday already exists for this date");
    }
}