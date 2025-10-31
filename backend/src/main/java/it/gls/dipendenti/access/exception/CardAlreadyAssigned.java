package it.gls.dipendenti.access.exception;

public class CardAlreadyAssigned extends RuntimeException {
    public CardAlreadyAssigned(Long id) {
        super("Card with id %s is already assigned to an employee");
    }
}
