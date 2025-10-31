package it.gls.dipendenti.hr.exception;

public class DuplicateContractException extends RuntimeException {
    public DuplicateContractException() {
        super("Employee already has a contract");
    }
}
