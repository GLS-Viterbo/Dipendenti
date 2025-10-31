package it.gls.dipendenti.auth.exception;

public class DuplicateUserFoundException extends RuntimeException {
    public DuplicateUserFoundException(String message) {
        super(message);
    }
}
