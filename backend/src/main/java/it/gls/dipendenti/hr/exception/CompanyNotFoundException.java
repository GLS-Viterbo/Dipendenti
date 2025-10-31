package it.gls.dipendenti.hr.exception;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(String message) {
        super(message);
    }
    public CompanyNotFoundException() {super("Cannot find company with given id"); }
}
