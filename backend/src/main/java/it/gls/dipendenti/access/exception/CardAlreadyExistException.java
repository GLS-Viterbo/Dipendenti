package it.gls.dipendenti.access.exception;

public class CardAlreadyExistException extends RuntimeException {
    public CardAlreadyExistException(String uid) {super("Card with uid %s already exists");}
}
