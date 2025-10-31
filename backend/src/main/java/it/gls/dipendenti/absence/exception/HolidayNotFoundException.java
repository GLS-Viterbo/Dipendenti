package it.gls.dipendenti.absence.exception;

public class HolidayNotFoundException extends RuntimeException {
    public HolidayNotFoundException(String message) {
        super(message);
    }

    public HolidayNotFoundException() {
        super("Holiday not found");
    }
}

