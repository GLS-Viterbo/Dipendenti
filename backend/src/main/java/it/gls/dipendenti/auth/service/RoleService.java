package it.gls.dipendenti.auth.service;

import it.gls.dipendenti.auth.exception.DuplicateRoleFoundException;
import it.gls.dipendenti.auth.exception.RoleNotFoundException;
import it.gls.dipendenti.auth.model.Role;
import it.gls.dipendenti.auth.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Create a new role
     * @param role role to create
     * @return created role
     */
    public Role createRole(Role role) {
        if (role.name() == null || role.name().isBlank()) {
            throw new IllegalArgumentException("Role name cannot be empty");
        }

        // Check if role name already exists
        roleRepository.findByName(role.name()).ifPresent(existingRole -> {
            throw new DuplicateRoleFoundException("Role with name '" + role.name() + "' already exists");
        });

        return roleRepository.save(role);
    }

    /**
     * Get role by id
     * @param id role id
     * @return role
     */
    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role with id " + id + " not found"));
    }

    /**
     * Get role by name
     * @param name role name
     * @return role
     */
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new RoleNotFoundException("Role with name '" + name + "' not found"));
    }

    /**
     * Get all roles
     * @return list of all roles
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Get all roles for a specific user
     * @param userId user id
     * @return list of roles
     */
    public List<Role> getRolesByUserId(Long userId) {
        return roleRepository.findByUserId(userId);
    }

    /**
     * Update role
     * @param role role with updated attributes
     */
    public void updateRole(Role role) {
        if (role.id() == null) {
            throw new IllegalArgumentException("Role id cannot be null");
        }

        if (role.name() == null || role.name().isBlank()) {
            throw new IllegalArgumentException("Role name cannot be empty");
        }

        // Check if role exists
        getRoleById(role.id());

        // Check if new name conflicts with existing role
        roleRepository.findByName(role.name()).ifPresent(existingRole -> {
            if (!existingRole.id().equals(role.id())) {
                throw new DuplicateRoleFoundException("Role with name '" + role.name() + "' already exists");
            }
        });

        boolean updated = roleRepository.update(role);
        if (!updated) {
            throw new RoleNotFoundException("Failed to update role with id " + role.id());
        }
    }

    /**
     * Delete role
     * @param id role id
     */
    public void deleteRole(Long id) {
        getRoleById(id);

        boolean deleted = roleRepository.delete(id);
        if (!deleted) {
            throw new IllegalStateException("Failed to delete role with id " + id);
        }
    }

    /**
     * Get total count of roles
     * @return total count
     */
    public Long getTotalCount() {
        return roleRepository.count();
    }
}
