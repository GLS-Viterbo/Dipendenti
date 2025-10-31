package it.gls.dipendenti.shift.exception;

public class DuplicateShiftNameException extends RuntimeException {
    public DuplicateShiftNameException(String message) {
        super(message);
    }
    public DuplicateShiftNameException() {super("There is already a shift with this name");}
}
