package com.nammakuzhu.friendModule.service;

import com.nammakuzhu.authModule.dto.ApiResponse;
import com.nammakuzhu.authModule.entity.AuthEntity;
import com.nammakuzhu.authModule.repository.AuthRepository;
import com.nammakuzhu.friendModule.dto.FriendRequestResponse;
import com.nammakuzhu.friendModule.dto.FriendSearchResponse;
import com.nammakuzhu.friendModule.entity.FriendRequestEntity;
import com.nammakuzhu.friendModule.entity.FriendRequestStatus;
import com.nammakuzhu.friendModule.repository.FriendRequestRepository;
import com.nammakuzhu.profileModule.entity.ProfileEntity;
import com.nammakuzhu.profileModule.repository.ProfileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FriendService {

    private final AuthRepository authRepository;
    private final ProfileRepository profileRepository;
    private final FriendRequestRepository friendRequestRepository;

    public FriendService(AuthRepository authRepository,
                         ProfileRepository profileRepository,
                         FriendRequestRepository friendRequestRepository) {
        this.authRepository = authRepository;
        this.profileRepository = profileRepository;
        this.friendRequestRepository = friendRequestRepository;
    }

    private AuthEntity getLoggedInUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return authRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    public ResponseEntity<?> searchUsers(String keyword) {
        AuthEntity loggedInUser = getLoggedInUser();

        List<ProfileEntity> profiles = profileRepository
                .findByFullNameContainingIgnoreCaseOrUser_MobileNumberContaining(keyword, keyword);

        List<FriendSearchResponse> responseList = new ArrayList<>();

        for (ProfileEntity profile : profiles) {
            AuthEntity user = profile.getUser();

            if (user.getId().equals(loggedInUser.getId())) {
                continue;
            }

            String requestStatus = "NONE";

            if (friendRequestRepository.existsBySenderAndReceiver(loggedInUser, user)) {
                requestStatus = "REQUEST_SENT";
            } else if (friendRequestRepository.existsBySenderAndReceiver(user, loggedInUser)) {
                requestStatus = "REQUEST_RECEIVED";
            }

            responseList.add(new FriendSearchResponse(
                    user.getId(),
                    profile.getFullName(),
                    user.getMobileNumber(),
                    profile.getDistrict(),
                    profile.getProfileImageUrl(),
                    requestStatus
            ));
        }

        return ResponseEntity.ok(new ApiResponse(true, "Users fetched successfully", responseList));
    }

    public ResponseEntity<?> sendFriendRequest(Long receiverId) {
        AuthEntity sender = getLoggedInUser();

        AuthEntity receiver = authRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (sender.getId().equals(receiver.getId())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "You cannot send request to yourself", null));
        }

        Optional<FriendRequestEntity> existingRequest =
                friendRequestRepository.findBySenderAndReceiver(sender, receiver);

        if (existingRequest.isPresent()) {
            FriendRequestEntity request = existingRequest.get();

            if (request.getStatus() == FriendRequestStatus.PENDING) {
                return ResponseEntity.status(409)
                        .body(new ApiResponse(false, "Friend request already sent", null));
            }

            if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
                return ResponseEntity.status(409)
                        .body(new ApiResponse(false, "You are already friends", null));
            }
        }

        Optional<FriendRequestEntity> reverseRequest =
                friendRequestRepository.findBySenderAndReceiver(receiver, sender);

        if (reverseRequest.isPresent()) {
            FriendRequestEntity request = reverseRequest.get();

            if (request.getStatus() == FriendRequestStatus.PENDING) {
                request.setStatus(FriendRequestStatus.ACCEPTED);
                friendRequestRepository.save(request);

                return ResponseEntity.ok(
                        new ApiResponse(true, "Friend request accepted automatically", null)
                );
            }

            if (request.getStatus() == FriendRequestStatus.ACCEPTED) {
                return ResponseEntity.status(409)
                        .body(new ApiResponse(false, "You are already friends", null));
            }
        }

        FriendRequestEntity request = new FriendRequestEntity();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());

        friendRequestRepository.save(request);

        return ResponseEntity.ok(
                new ApiResponse(true, "Friend request sent successfully", null)
        );
    }

    public ResponseEntity<?> getReceivedRequests() {
        AuthEntity loggedInUser = getLoggedInUser();

        List<FriendRequestEntity> requests =
                friendRequestRepository.findByReceiverAndStatus(
                        loggedInUser,
                        FriendRequestStatus.PENDING
                );

        List<FriendRequestResponse> responseList = new ArrayList<>();

        for (FriendRequestEntity request : requests) {
            AuthEntity sender = request.getSender();

            ProfileEntity profile = profileRepository.findByUser(sender)
                    .orElse(null);

            responseList.add(new FriendRequestResponse(
                    request.getId(),
                    sender.getId(),
                    profile != null ? profile.getFullName() : sender.getName(),
                    sender.getMobileNumber(),
                    profile != null ? profile.getDistrict() : null,
                    profile != null ? profile.getProfileImageUrl() : null,
                    request.getStatus().name()
            ));
        }

        return ResponseEntity.ok(
                new ApiResponse(true, "Received requests fetched successfully", responseList)
        );
    }

    public ResponseEntity<?> acceptRequest(Long requestId) {
        AuthEntity loggedInUser = getLoggedInUser();

        FriendRequestEntity request = friendRequestRepository
                .findByIdAndReceiver(requestId, loggedInUser)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "This request is already processed", null));
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        return ResponseEntity.ok(
                new ApiResponse(true, "Friend request accepted successfully", null)
        );
    }

    public ResponseEntity<?> rejectRequest(Long requestId) {
        AuthEntity loggedInUser = getLoggedInUser();

        FriendRequestEntity request = friendRequestRepository
                .findByIdAndReceiver(requestId, loggedInUser)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (request.getStatus() != FriendRequestStatus.PENDING) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "This request is already processed", null));
        }

        request.setStatus(FriendRequestStatus.REJECTED);
        friendRequestRepository.save(request);

        return ResponseEntity.ok(
                new ApiResponse(true, "Friend request rejected successfully", null)
        );
    }

    public ResponseEntity<?> getFriendsList() {
        AuthEntity loggedInUser = getLoggedInUser();

        List<FriendRequestEntity> sentAccepted =
                friendRequestRepository.findBySenderAndStatus(
                        loggedInUser,
                        FriendRequestStatus.ACCEPTED
                );

        List<FriendRequestEntity> receivedAccepted =
                friendRequestRepository.findByReceiverAndStatus(
                        loggedInUser,
                        FriendRequestStatus.ACCEPTED
                );

        List<FriendRequestResponse> responseList = new ArrayList<>();

        for (FriendRequestEntity request : sentAccepted) {
            AuthEntity friend = request.getReceiver();

            ProfileEntity profile = profileRepository.findByUser(friend)
                    .orElse(null);

            responseList.add(new FriendRequestResponse(
                    request.getId(),
                    friend.getId(),
                    profile != null ? profile.getFullName() : friend.getName(),
                    friend.getMobileNumber(),
                    profile != null ? profile.getDistrict() : null,
                    profile != null ? profile.getProfileImageUrl() : null,
                    request.getStatus().name()
            ));
        }

        for (FriendRequestEntity request : receivedAccepted) {
            AuthEntity friend = request.getSender();

            ProfileEntity profile = profileRepository.findByUser(friend)
                    .orElse(null);

            responseList.add(new FriendRequestResponse(
                    request.getId(),
                    friend.getId(),
                    profile != null ? profile.getFullName() : friend.getName(),
                    friend.getMobileNumber(),
                    profile != null ? profile.getDistrict() : null,
                    profile != null ? profile.getProfileImageUrl() : null,
                    request.getStatus().name()
            ));
        }

        return ResponseEntity.ok(
                new ApiResponse(true, "Friends list fetched successfully", responseList)
        );
    }

    public ResponseEntity<?> deleteFriend(Long requestId) {
        AuthEntity loggedInUser = getLoggedInUser();

        FriendRequestEntity request = friendRequestRepository
                .findById(requestId)
                .orElseThrow(() -> new RuntimeException("Friend not found"));

        boolean isSender = request.getSender().getId().equals(loggedInUser.getId());
        boolean isReceiver = request.getReceiver().getId().equals(loggedInUser.getId());

        if (!isSender && !isReceiver) {
            return ResponseEntity.status(403)
                    .body(new ApiResponse(false, "You are not allowed to delete this friend", null));
        }

        if (request.getStatus() != FriendRequestStatus.ACCEPTED) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse(false, "This user is not in your friends list", null));
        }

        friendRequestRepository.delete(request);

        return ResponseEntity.ok(
                new ApiResponse(true, "Friend removed successfully", null)
        );
    }

}