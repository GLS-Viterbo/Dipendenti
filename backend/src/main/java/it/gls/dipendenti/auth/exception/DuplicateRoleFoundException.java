package it.gls.dipendenti.auth.exception;

public class DuplicateRoleFoundException extends RuntimeException {
    public DuplicateRoleFoundException(String message) {
        super(message);
    }
}