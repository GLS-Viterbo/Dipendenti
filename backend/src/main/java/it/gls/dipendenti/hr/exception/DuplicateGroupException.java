package it.gls.dipendenti.hr.exception;

public class DuplicateGroupException extends RuntimeException {
    public DuplicateGroupException(String message) {
        super(message);
    }

    public DuplicateGroupException() {
        super("Group already exists");
    }
}