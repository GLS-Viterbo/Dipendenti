package it.gls.dipendenti.hr.controller;

import it.gls.dipendenti.hr.exception.ContractNotFoundException;
import it.gls.dipendenti.hr.exception.DuplicateGroupException;
import it.gls.dipendenti.hr.exception.GroupNotFoundException;
import it.gls.dipendenti.hr.model.Employee;
import it.gls.dipendenti.hr.model.Group;
import it.gls.dipendenti.hr.service.GroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import it.gls.dipendenti.util.ErrorResponse;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ResponseEntity<List<Group>> getGroups() {
        return ResponseEntity.ok(groupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getGroupById(id));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<Employee>> getGroupMembers(@PathVariable Long id) {
        return ResponseEntity.ok(groupService.getGroupMembers(id));
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(@RequestBody Group group) {
        Group newGroup = groupService.createGroup(group);
        return ResponseEntity.status(HttpStatus.CREATED).body(newGroup);
    }
    @PostMapping("/{groupId}/members/{employeeId}")
    public ResponseEntity<Void> addEmployeeToGroup(@PathVariable Long groupId, @PathVariable Long employeeId) {
        groupService.addEmployeeToGroup(employeeId, groupId);
        return ResponseEntity.status(HttpStatus.OK).build();

    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateGroup(@PathVariable Long id, @RequestBody Group group) {
        groupService.updateGroup(new Group(
                id,
                group.name(),
                group.deleted()
        ));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{groupId}/members/{employeeId}")
    public ResponseEntity<Void> removeFromGroup(@PathVariable Long groupId, @PathVariable Long employeeId) {
        groupService.removeEmployeeFromGroup(employeeId, groupId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();

    }

    @ExceptionHandler(GroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleContractNotFound(GroupNotFoundException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Group not found", HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

    }

    @ExceptionHandler(DuplicateGroupException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateGroup(DuplicateGroupException ex) {
        ErrorResponse errorResponse = new ErrorResponse("Group with same name already exists", HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }






}
