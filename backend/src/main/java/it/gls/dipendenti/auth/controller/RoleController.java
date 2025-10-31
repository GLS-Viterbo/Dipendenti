package it.gls.dipendenti.auth.controller;

import it.gls.dipendenti.auth.exception.DuplicateRoleFoundException;
import it.gls.dipendenti.auth.exception.RoleNotFoundException;
import it.gls.dipendenti.auth.model.Role;
import it.gls.dipendenti.auth.service.RoleService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        Role createdRole = roleService.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Role> getRoleByName(@PathVariable String name) {
        Role role = roleService.getRoleByName(name);
        return ResponseEntity.ok(role);
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Role>> getRolesByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(roleService.getRolesByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateRole(@PathVariable Long id, @RequestBody Role role) {
        roleService.updateRole(new Role(id, role.name()));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<RoleCountResponse> getTotalCount() {
        Long count = roleService.getTotalCount();
        return ResponseEntity.ok(new RoleCountResponse(count));
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(RoleNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateRoleFoundException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRole(DuplicateRoleFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    public record RoleCountResponse(Long count) {}
}
