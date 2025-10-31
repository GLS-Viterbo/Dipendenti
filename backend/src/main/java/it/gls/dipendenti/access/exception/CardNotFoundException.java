package it.gls.dipendenti.access.exception;

public class CardNotFoundException extends RuntimeException {
    public CardNotFoundException() {
        super("Card not found");
    }
}
