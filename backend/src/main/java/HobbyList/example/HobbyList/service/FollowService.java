package HobbyList.example.HobbyList.service;

import HobbyList.example.HobbyList.dto.UserSummaryDto;
import HobbyList.example.HobbyList.dto.UserSummaryProjection;
import HobbyList.example.HobbyList.model.FollowRequest;
import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.repository.FollowRequestRepository;
import HobbyList.example.HobbyList.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private final UserRepository userRepository;
    private final FollowRequestRepository followRequestRepository;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    public FollowService(UserRepository userRepository, FollowRequestRepository followRequestRepository,
            S3Service s3Service, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.followRequestRepository = followRequestRepository;
        this.s3Service = s3Service;
        this.objectMapper = objectMapper;
    }

    private String getPresignUrl(String profileUrl) {
        if (profileUrl == null) {
            return null;
        }
        String bucketName = "hobbylist-photos";
        String key = profileUrl.substring(profileUrl.indexOf("profile/"));
        return s3Service.generateDownloadUrl(bucketName, key);
    }

    @Transactional
    public void followUser(User requester, User target) {
        if (requester.getId() == target.getId()) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }

        if (isFollowing(requester, target)) {
            throw new IllegalStateException("You are already following this user.");
        }

        if (target.isPrivate()) {
            // Check if request already exists
            Optional<FollowRequest> existingRequest = followRequestRepository.findByRequesterAndTarget(requester,
                    target);
            if (existingRequest.isPresent()
                    && existingRequest.get().getStatus() == FollowRequest.RequestStatus.PENDING) {
                throw new IllegalStateException("Follow request already pending.");
            }

            // Create follow request
            FollowRequest request = new FollowRequest(requester, target);
            followRequestRepository.save(request);
        } else {
            // Direct follow
            target.getFollowers().add(requester);
            requester.getFollowing().add(target);
            userRepository.save(target);
            userRepository.save(requester);
        }
    }

    @Transactional
    public void unfollowUser(User requester, User target) {

        if (requester.getId() == target.getId()) {
            throw new IllegalArgumentException("You cannot unfollow yourself.");
        }

        if (isFollowing(requester, target)) {
            target.getFollowers().remove(requester);
            requester.getFollowing().remove(target);
            userRepository.save(target);
            userRepository.save(requester);
        } else {
            // Check if there is a pending request to cancel
            Optional<FollowRequest> existingRequest = followRequestRepository.findByRequesterAndTarget(requester,
                    target);
            if (existingRequest.isPresent()
                    && existingRequest.get().getStatus() == FollowRequest.RequestStatus.PENDING) {
                followRequestRepository.delete(existingRequest.get());
            } else {
                throw new IllegalArgumentException("You do not follow nor have requested to follow this user.");
            }
        }
    }

    @Transactional
    public void acceptRequest(User requester, User target) {
        FollowRequest request = followRequestRepository
                .findByRequesterAndTargetAndStatus(requester, target, FollowRequest.RequestStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getStatus() != FollowRequest.RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }
        request.setStatus(FollowRequest.RequestStatus.ACCEPTED);
        followRequestRepository.save(request);

        try {
            target.getFollowers().add(requester);
            requester.getFollowing().add(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to accept follow request", e);
        }

        userRepository.save(target);
        userRepository.save(requester);
    }

    @Transactional
    public void rejectRequest(User requester, User target) {
        FollowRequest request = followRequestRepository
                .findByRequesterAndTargetAndStatus(requester, target, FollowRequest.RequestStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        request.setStatus(FollowRequest.RequestStatus.REJECTED);
        followRequestRepository.save(request);
    }

    public boolean isFollowing(User requester, User target) {
        return target.getFollowers().contains(requester);
    }

    public boolean isFollowRequested(User requester, User target) {
        Optional<FollowRequest> request = followRequestRepository.findByRequesterAndTargetAndStatus(requester, target,
                FollowRequest.RequestStatus.PENDING);
        return request.isPresent();
    }

    public List<UserSummaryDto> getFollowers(User targetUser, User currentUser) {
        return userRepository.findFollowers(targetUser.getId(), currentUser.getId()).stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    public List<UserSummaryDto> getFollowing(User targetUser, User currentUser) {
        return userRepository.findFollowing(targetUser.getId(), currentUser.getId()).stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    public List<UserSummaryDto> getPendingRequests(User target) {
        return userRepository.findPendingRequests(target.getId()).stream()
                .map(this::convertToSummaryDto)
                .collect(Collectors.toList());
    }

    private UserSummaryDto convertToSummaryDto(UserSummaryProjection user) {
        List<String> hobbiesList = new ArrayList<>();

        try {
            if (user.getHobbies() != null) {
                hobbiesList = objectMapper.readValue(
                        user.getHobbies(),
                        new TypeReference<List<String>>() {
                        });
            }
        } catch (Exception e) {
            System.err.println("Error parsing hobbies for user " + user.getId() + ": " + e.getMessage());
        }

        return new UserSummaryDto(
                user.getId(),
                user.getDisplayName(),
                getPresignUrl(user.getProfileUrl()),
                hobbiesList,
                user.getRelationship());
    }
}
