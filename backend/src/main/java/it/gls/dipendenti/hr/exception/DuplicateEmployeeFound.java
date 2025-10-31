package it.gls.dipendenti.hr.exception;

public class DuplicateEmployeeFound extends RuntimeException {
    public DuplicateEmployeeFound(String message) {
        super(message);
    }
}
