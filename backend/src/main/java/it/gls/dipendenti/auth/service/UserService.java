package it.gls.dipendenti.auth.service;

import it.gls.dipendenti.hr.exception.CompanyNotFoundException;
import it.gls.dipendenti.auth.exception.DuplicateUserFoundException;
import it.gls.dipendenti.auth.exception.RoleNotFoundException;
import it.gls.dipendenti.auth.exception.UserNotFoundException;
import it.gls.dipendenti.auth.model.Role;
import it.gls.dipendenti.auth.model.User;
import it.gls.dipendenti.hr.repository.CompanyRepository;
import it.gls.dipendenti.auth.repository.RoleRepository;
import it.gls.dipendenti.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       CompanyRepository companyRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Create a new user
     * @param user user to create
     * @param rawPassword plain text password
     * @return created user
     */
    public User createUser(User user, String rawPassword) {
        if (user.username() == null || user.username().isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        // Check if username already exists
        userRepository.findByUsername(user.username()).ifPresent(existingUser -> {
            throw new DuplicateUserFoundException("User with username '" + user.username() + "' already exists");
        });

        // Validate company if provided
        if (user.companyId() != null) {
            companyRepository.findById(user.companyId())
                    .orElseThrow(() -> new CompanyNotFoundException("Company with id " + user.companyId() + " not found"));
        }

        // Hash password
        String hashedPassword = passwordEncoder.encode(rawPassword);

        User userToSave = new User(
                null,
                user.username(),
                hashedPassword,
                user.email(),
                user.companyId(),
                user.active()
        );

        return userRepository.save(userToSave);
    }

    /**
     * Get user by id
     * @param id user id
     * @return user
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
    }

    /**
     * Get user by username
     * @param username username
     * @return user
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User with username '" + username + "' not found"));
    }

    /**
     * Get all users
     * @return list of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get all active users
     * @return list of active users
     */
    public List<User> getAllActiveUsers() {
        return userRepository.findAllActive();
    }

    /**
     * Get all users by company
     * @param companyId company id
     * @return list of users
     */
    public List<User> getUsersByCompanyId(Long companyId) {
        return userRepository.findByCompanyId(companyId);
    }

    /**
     * Update user
     * @param user user with updated attributes
     */
    public void updateUser(User user) {
        if (user.id() == null) {
            throw new IllegalArgumentException("User id cannot be null");
        }

        if (user.username() == null || user.username().isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        // Check if user exists
        User existingUser = getUserById(user.id());

        // Check if new username conflicts with existing user
        userRepository.findByUsername(user.username()).ifPresent(u -> {
            if (!u.id().equals(user.id())) {
                throw new DuplicateUserFoundException("User with username '" + user.username() + "' already exists");
            }
        });


        // Validate company if provided
        if (user.companyId() != null) {
            companyRepository.findById(user.companyId())
                    .orElseThrow(() -> new CompanyNotFoundException("Company with id " + user.companyId() + " not found"));
        }

        // Keep existing password hash if not changing password
        User userToUpdate = new User(
                user.id(),
                user.username(),
                existingUser.passwordHash(), // Keep existing password
                user.email(),
                user.companyId(),
                user.active()
        );

        boolean updated = userRepository.update(userToUpdate);
        if (!updated) {
            throw new UserNotFoundException("Failed to update user with id " + user.id());
        }
    }

    /**
     * Update user password
     * @param userId user id
     * @param newPassword new plain text password
     */
    public void updatePassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        getUserById(userId); // Check if user exists

        String hashedPassword = passwordEncoder.encode(newPassword);
        boolean updated = userRepository.updatePassword(userId, hashedPassword);

        if (!updated) {
            throw new UserNotFoundException("Failed to update password for user with id " + userId);
        }
    }

    /**
     * Deactivate user
     * @param id user id
     */
    public void deactivateUser(Long id) {
        getUserById(id);

        boolean deactivated = userRepository.deactivate(id);
        if (!deactivated) {
            throw new IllegalStateException("Failed to deactivate user with id " + id);
        }
    }

    /**
     * Activate user
     * @param id user id
     */
    public void activateUser(Long id) {
        getUserById(id);

        boolean activated = userRepository.activate(id);
        if (!activated) {
            throw new IllegalStateException("Failed to activate user with id " + id);
        }
    }

    /**
     * Delete user
     * @param id user id
     */
    public void deleteUser(Long id) {
        getUserById(id);

        // Remove all role assignments first
        userRepository.removeAllRoles(id);

        boolean deleted = userRepository.delete(id);
        if (!deleted) {
            throw new IllegalStateException("Failed to delete user with id " + id);
        }
    }

    /**
     * Assign role to user
     * @param userId user id
     * @param roleId role id
     */
    public void assignRole(Long userId, Long roleId) {
        getUserById(userId);
        roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role with id " + roleId + " not found"));

        userRepository.assignRole(userId, roleId);
    }

    /**
     * Remove role from user
     * @param userId user id
     * @param roleId role id
     */
    public void removeRole(Long userId, Long roleId) {
        getUserById(userId);

        boolean removed = userRepository.removeRole(userId, roleId);
        if (!removed) {
            throw new IllegalStateException("User does not have this role or role removal failed");
        }
    }

    /**
     * Get user roles
     * @param userId user id
     * @return list of roles
     */
    public List<Role> getUserRoles(Long userId) {
        getUserById(userId);
        return roleRepository.findByUserId(userId);
    }

    /**
     * Get total count of users
     * @return total count
     */
    public Long getTotalCount() {
        return userRepository.count();
    }

    /**
     * Get count of active users
     * @return active count
     */
    public Long getActiveCount() {
        return userRepository.countActive();
    }
}
