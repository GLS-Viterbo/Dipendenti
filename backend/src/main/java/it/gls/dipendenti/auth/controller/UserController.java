package it.gls.dipendenti.auth.controller;

import it.gls.dipendenti.auth.exception.DuplicateUserFoundException;
import it.gls.dipendenti.auth.exception.RoleNotFoundException;
import it.gls.dipendenti.auth.exception.UserNotFoundException;
import it.gls.dipendenti.hr.exception.*;
import it.gls.dipendenti.auth.model.Role;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.auth.service.UserService;
import it.gls.dipendenti.util.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        User createdUser = userService.createUser(
                new User(null, request.username(), null, request.email(),
                        request.companyId(), request.active() != null ? request.active() : true),
                request.password()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(createdUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(toUserResponse(user));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(toUserResponse(user));
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @RequestParam(value = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        List<User> users = activeOnly ? userService.getAllActiveUsers() : userService.getAllUsers();
        return ResponseEntity.ok(users.stream().map(this::toUserResponse).toList());
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<UserResponse>> getUsersByCompanyId(@PathVariable Long companyId) {
        List<User> users = userService.getUsersByCompanyId(companyId);
        return ResponseEntity.ok(users.stream().map(this::toUserResponse).toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        userService.updateUser(new User(
                id,
                request.username(),
                null, // Password hash managed separately
                request.email(),
                request.companyId(),
                request.active()
        ));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> updatePassword(@PathVariable Long id, @RequestBody PasswordUpdateRequest request) {
        userService.updatePassword(id, request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(@PathVariable Long id) {
        userService.activateUser(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> assignRole(@PathVariable Long userId, @PathVariable Long roleId) {
        userService.assignRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    public ResponseEntity<Void> removeRole(@PathVariable Long userId, @PathVariable Long roleId) {
        userService.removeRole(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/roles")
    public ResponseEntity<List<Role>> getUserRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserRoles(userId));
    }

    @GetMapping("/count")
    public ResponseEntity<UserCountResponse> getTotalCount() {
        Long count = userService.getTotalCount();
        return ResponseEntity.ok(new UserCountResponse(count));
    }

    @GetMapping("/count/active")
    public ResponseEntity<UserCountResponse> getActiveCount() {
        Long count = userService.getActiveCount();
        return ResponseEntity.ok(new UserCountResponse(count));
    }

    // Helper method to convert User to UserResponse (without password hash)
    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.id(),
                user.username(),
                user.email(),
                user.companyId(),
                user.active()
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateUserFoundException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateUser(DuplicateUserFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCompanyNotFound(CompanyNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRoleNotFound(RoleNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
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

    // DTOs
    public record CreateUserRequest(
            String username,
            String password,
            String email,
            Long companyId,
            Boolean active
    ) {}

    public record UpdateUserRequest(
            String username,
            String email,
            Long companyId,
            boolean active
    ) {}

    public record PasswordUpdateRequest(String newPassword) {}

    public record UserResponse(
            Long id,
            String username,
            String email,
            Long companyId,
            boolean active
    ) {}

    public record UserCountResponse(Long count) {}
}
