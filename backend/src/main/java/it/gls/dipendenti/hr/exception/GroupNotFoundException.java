package it.gls.dipendenti.hr.exception;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(String message) {
        super(message);
    }

    public GroupNotFoundException() {
        super("Group not found");
    }
}