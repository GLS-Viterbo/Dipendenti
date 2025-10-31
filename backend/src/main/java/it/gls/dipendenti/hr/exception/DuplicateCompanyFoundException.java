package it.gls.dipendenti.hr.exception;

public class DuplicateCompanyFoundException extends RuntimeException {
    public DuplicateCompanyFoundException(String message) {
        super(message);
    }
}
