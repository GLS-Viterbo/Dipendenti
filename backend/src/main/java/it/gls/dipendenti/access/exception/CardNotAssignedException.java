package it.gls.dipendenti.access.exception;

public class CardNotAssignedException extends RuntimeException {
    public CardNotAssignedException(Long id) {
        super("Card with id %s is not assigned to an employee");
    }
    public CardNotAssignedException() {
        super("Card not assigned");
    }
}
