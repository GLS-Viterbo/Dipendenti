package it.gls.dipendenti.hr.service;

import it.gls.dipendenti.hr.exception.DuplicateGroupException;
import it.gls.dipendenti.hr.exception.EmployeeNotFoundException;
import it.gls.dipendenti.hr.exception.GroupNotFoundException;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.model.Group;
import it.gls.dipendenti.hr.repository.EmployeeRepository;
import it.gls.dipendenti.hr.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final EmployeeRepository employeeRepository;

    public GroupService(GroupRepository groupRepository, EmployeeRepository employeeRepository) {
        this.groupRepository = groupRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Creates a new group
     * @param group the group to create
     * @return created group with generated id
     */
    @Transactional
    public Group createGroup(Group group) {
        validateGroupName(group.name());

        // Check for duplicate name
        if (groupExists(group.name())) {
            throw new DuplicateGroupException("Group with name '%s' already exists".formatted(group.name()));
        }

        Group groupToSave = new Group(null, group.name(), false);
        return groupRepository.save(groupToSave);
    }

    /**
     * Updates a group's name
     * @param groupId the id of the group to update
     * @param newName the new name
     */
    @Transactional
    public void updateGroup(Group group) {
        if (group.id() == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        validateGroupName(group.name());

        Group existingGroup = groupRepository.findById(group.id())
                .orElseThrow(() -> new GroupNotFoundException("Group with ID %d not found".formatted(group.id())));

        // Check if new name is already taken by another group
        List<Group> groupsWithSameName = groupRepository.findAll().stream()
                .filter(g -> g.name().equalsIgnoreCase(group.name()) && !g.id().equals(group.id()))
                .toList();

        if (!groupsWithSameName.isEmpty()) {
            throw new DuplicateGroupException("Group with name '%s' already exists".formatted(group.name()));
        }

        Group updatedGroup = new Group(existingGroup.id(), group.name(), group.deleted());
        groupRepository.update(updatedGroup);
    }

    /**
     * Soft deletes a group
     * @param groupId the id of the group to delete
     */
    @Transactional
    public void deleteGroup(Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        if (!groupRepository.delete(groupId)) {
            throw new GroupNotFoundException("Group with ID %d not found".formatted(groupId));
        }

        // Removing all employees from Group
        groupRepository.findMemberIds(groupId)
                .forEach(id -> groupRepository.removeMember(id, groupId));

    }

    /**
     * Adds an employee to a group
     * @param employeeId the employee to add
     * @param groupId the group
     */
    @Transactional
    public void addEmployeeToGroup(Long employeeId, Long groupId) {
        if (employeeId == null || groupId == null) {
            throw new IllegalArgumentException("Employee ID and Group ID cannot be null");
        }

        // Verify employee exists and is not deleted
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee with ID %d not found".formatted(employeeId)));

        if (employee.deleted()) {
            throw new IllegalArgumentException("Cannot add deleted employee to group");
        }

        // Verify group exists and is not deleted
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID %d not found".formatted(groupId)));

        if (group.deleted()) {
            throw new IllegalArgumentException("Cannot add employee to deleted group");
        }

        // Check if already member
        if (groupRepository.isMember(employeeId, groupId)) {
            throw new IllegalArgumentException("Employee is already a member of this group");
        }

        groupRepository.addMember(employeeId, groupId);
    }

    /**
     * Removes an employee from a group
     * @param employeeId the employee to remove
     * @param groupId the group
     */
    @Transactional
    public void removeEmployeeFromGroup(Long employeeId, Long groupId) {
        if (employeeId == null || groupId == null) {
            throw new IllegalArgumentException("Employee ID and Group ID cannot be null");
        }

        if (!groupRepository.isMember(employeeId, groupId)) {
            throw new IllegalArgumentException("Employee is not a member of this group");
        }

        if (!groupRepository.removeMember(employeeId, groupId)) {
            throw new IllegalArgumentException("Failed to remove employee from group");
        }
    }

    /**
     * Gets all active groups
     * @return list of groups
     */
    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    /**
     * Gets a group by ID
     * @param groupId the group ID
     * @return the group
     */
    public Group getGroupById(Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        return groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID %d not found".formatted(groupId)));
    }

    /**
     * Gets all members of a group
     * @param groupId the group ID
     * @return list of employees in the group
     */
    public List<Employee> getGroupMembers(Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }

        // Verify group exists
        groupRepository.findById(groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group with ID %d not found".formatted(groupId)));

        List<Long> memberIds = groupRepository.findMemberIds(groupId);

        return memberIds.stream()
                .map(employeeRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Gets all groups an employee belongs to
     * @param employeeId the employee ID
     * @return list of groups
     */
    public List<Group> getEmployeeGroups(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID cannot be null");
        }

        // Verify employee exists
        employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EmployeeNotFoundException("Employee with ID %d not found".formatted(employeeId)));

        List<Long> groupIds = groupRepository.findGroupIds(employeeId);

        return groupIds.stream()
                .map(groupRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Checks if an employee is member of a group
     * @param employeeId the employee ID
     * @param groupId the group ID
     * @return true if member
     */
    public boolean isEmployeeMemberOfGroup(Long employeeId, Long groupId) {
        if (employeeId == null || groupId == null) {
            return false;
        }
        return groupRepository.isMember(employeeId, groupId);
    }


    /**
     * Checks if a name is blank or greater than 50 chars
     * @param name input name
     */
    private void validateGroupName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Group name cannot be null or empty");
        }

        if (name.length() > 50) {
            throw new IllegalArgumentException("Group name cannot exceed 50 characters");
        }
    }

    private boolean groupExists(String name) {
        return groupRepository.findAll().stream()
                .anyMatch(g -> g.name().equalsIgnoreCase(name));
    }

}
