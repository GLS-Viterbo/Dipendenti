package it.gls.dipendenti.hr.exception;

public class ContractNotFoundException extends RuntimeException {
    public ContractNotFoundException() {
        super("Contract not found");
    }
}
