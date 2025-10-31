package it.gls.dipendenti.hr.exception;

public class DeadlineNotFoundException extends RuntimeException {
    public DeadlineNotFoundException(String message) {
        super(message);
    }
    public DeadlineNotFoundException() { super("Cannot find deadline with given id"); }
}
