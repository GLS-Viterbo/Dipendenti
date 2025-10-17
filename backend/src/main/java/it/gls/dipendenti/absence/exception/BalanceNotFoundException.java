package it.gls.dipendenti.absence.exception;

public class BalanceNotFoundException extends RuntimeException{

    public BalanceNotFoundException() {
        super("Balance not found for this employee");
    }

    public BalanceNotFoundException(String message) {
        super(message);
    }
}
