package it.gls.dipendenti.hr.exception;

public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(String message) {
        super(message);
    }

    public EmployeeNotFoundException() {
        super("Employee not found");
    }
}
