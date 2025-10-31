package it.gls.dipendenti.access.exception;

public class LogNotFoundException extends RuntimeException {
    public LogNotFoundException(String message) {
        super(message);
    }
    public LogNotFoundException() {super("Log not found");}
}
