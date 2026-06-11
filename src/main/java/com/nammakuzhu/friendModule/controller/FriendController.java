package com.nammakuzhu.friendModule.controller;

import com.nammakuzhu.friendModule.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        return friendService.searchUsers(keyword);
    }

    @PostMapping("/request/send/{receiverId}")
    public ResponseEntity<?> sendFriendRequest(@PathVariable Long receiverId) {
        return friendService.sendFriendRequest(receiverId);
    }

    @GetMapping("/requests/received")
    public ResponseEntity<?> getReceivedRequests() {
        return friendService.getReceivedRequests();
    }

    @PostMapping("/request/accept/{requestId}")
    public ResponseEntity<?> acceptRequest(@PathVariable Long requestId) {
        return friendService.acceptRequest(requestId);
    }

    @PostMapping("/request/reject/{requestId}")
    public ResponseEntity<?> rejectRequest(@PathVariable Long requestId) {
        return friendService.rejectRequest(requestId);
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFriendsList() {
        return friendService.getFriendsList();
    }

    @DeleteMapping("/delete/{requestId}")
    public ResponseEntity<?> deleteFriend(@PathVariable Long requestId) {
        return friendService.deleteFriend(requestId);
    }

}