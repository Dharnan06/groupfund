package com.nammakuzhu.groupModule.controller;

import com.nammakuzhu.groupModule.dto.CreateGroupRequest;
import com.nammakuzhu.groupModule.dto.InviteMemberRequest;
import com.nammakuzhu.groupModule.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createGroup(@RequestBody CreateGroupRequest request) {
        return groupService.createGroup(request);
    }

    @PostMapping("/invite")
    public ResponseEntity<?> inviteMember(@RequestBody InviteMemberRequest request) {
        return groupService.inviteMember(request);
    }

    @GetMapping("/invitations")
    public ResponseEntity<?> getMyPendingInvitations() {
        return groupService.getMyPendingInvitations();
    }

    @PostMapping("/invitations/accept/{invitationId}")
    public ResponseEntity<?> acceptGroupInvitation(@PathVariable Long invitationId) {
        return groupService.acceptGroupInvitation(invitationId);
    }

    @PostMapping("/invitations/reject/{invitationId}")
    public ResponseEntity<?> rejectGroupInvitation(@PathVariable Long invitationId) {
        return groupService.rejectGroupInvitation(invitationId);
    }

    @GetMapping("/my-groups")
    public ResponseEntity<?> getMyGroups() {
        return groupService.getMyGroups();
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats() {
        return groupService.getDashboardStats();
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<?> getGroupDetails(@PathVariable Long groupId) {
        return groupService.getGroupDetails(groupId);
    }

    @PostMapping("/{groupId}/activation/request")
    public ResponseEntity<?> requestActivation(@PathVariable Long groupId) {
        return groupService.requestActivation(groupId);
    }

    @PostMapping("/{groupId}/activation/approve")
    public ResponseEntity<?> approveActivation(@PathVariable Long groupId) {
        return groupService.approveActivation(groupId);
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<?> deleteGroup(@PathVariable Long groupId) {
        return groupService.deleteGroup(groupId);
    }

}
