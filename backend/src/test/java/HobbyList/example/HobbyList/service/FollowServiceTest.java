package HobbyList.example.HobbyList.service;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import HobbyList.example.HobbyList.repository.FollowRequestRepository;
import HobbyList.example.HobbyList.repository.UserRepository;
import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.dto.UserSummaryDto;
import HobbyList.example.HobbyList.dto.UserSummaryProjection;
import HobbyList.example.HobbyList.model.FollowRequest;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    FollowRequestRepository followRequestRepository;
    @Mock
    S3Service s3Service;
    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    FollowService followService;

    private User requester;
    private User targetPublic;
    private User targetPrivate;

    @BeforeEach
    void setup() {
        requester = new User();
        requester.setId(1L);

        targetPublic = new User();
        targetPublic.setId(2L);
        targetPublic.setPrivate(false);

        targetPrivate = new User();
        targetPrivate.setId(3L);
        targetPrivate.setPrivate(true);
    }

    //////////////////////
    // FollowUser Tests //
    //////////////////////

    // ----------------------------------------------
    // 1. requester follows themselves -> ERROR
    // ----------------------------------------------
    @Test
    void followUser_ShouldThrow_WhenFollowingSelf() {
        assertThrows(
                IllegalArgumentException.class,
                () -> followService.followUser(requester, requester));
    }

    // ----------------------------------------------
    // 2. already following -> ERROR
    // ----------------------------------------------
    @Test
    void followUser_ShouldThrow_WhenAlreadyFollowing() {
        targetPublic.getFollowers().add(requester);
        requester.getFollowing().add(targetPublic);

        assertThrows(
                IllegalStateException.class,
                () -> followService.followUser(requester, targetPublic));
    }

    // ----------------------------------------------
    // 3. private target -> create pending FOLLOW REQUEST
    // ----------------------------------------------
    @Test
    void followUser_ShouldCreatePendingRequest_WhenTargetIsPrivate() {
        when(followRequestRepository.findByRequesterAndTarget(requester, targetPrivate)).thenReturn(Optional.empty());

        targetPrivate.getFollowers().clear();

        followService.followUser(requester, targetPrivate);

        ArgumentCaptor<FollowRequest> captor = ArgumentCaptor.forClass(FollowRequest.class);
        verify(followRequestRepository).save(captor.capture());

        FollowRequest saved = captor.getValue();

        assertEquals(requester, saved.getRequester());
        assertEquals(targetPrivate, saved.getTarget());
        assertEquals(FollowRequest.RequestStatus.PENDING, saved.getStatus());
    }

    // ----------------------------------------------
    // 4. public target -> directly add follower relation
    // ----------------------------------------------
    @Test
    void followUser_ShouldDirectlyFollow_WhenTargetIsPublic() {
        targetPublic.getFollowers().clear();

        followService.followUser(requester, targetPublic);

        assertTrue(targetPublic.getFollowers().contains(requester));
        assertTrue(requester.getFollowing().contains(targetPublic));
    }

    // ----------------------------------------------
    // 5. private target but request already exists -> ERROR
    // ----------------------------------------------
    @Test
    void followUser_ShouldThrow_WhenPendingRequestAlreadyExists() {
        when(followRequestRepository.findByRequesterAndTarget(requester, targetPrivate))
                .thenReturn(Optional.of(new FollowRequest(requester, targetPrivate)));

        targetPrivate.getFollowers().clear();

        assertThrows(
                IllegalStateException.class,
                () -> followService.followUser(requester, targetPrivate));
    }

    ////////////////////////
    // UnfollowUser Tests //
    ////////////////////////

    // ----------------------------------------------
    // 1. requester unfollows themselves -> ERROR
    // ----------------------------------------------
    @Test
    void unfollowUser_ShouldThrow_WhenUnfollowingSelf() {
        assertThrows(
                IllegalArgumentException.class,
                () -> followService.unfollowUser(requester, requester));
    }

    // ----------------------------------------------
    // 2. requester follows target -> remove requester from targets followers
    // ----------------------------------------------
    @Test
    void unfollowUser_ShouldRemoveRequesterFromTargetsFollowers_WhenRequesterFollowsPrivateTarget() {
        requester.getFollowing().clear();
        targetPrivate.getFollowers().clear();

        requester.getFollowing().add(targetPrivate);
        targetPrivate.getFollowers().add(requester);

        followService.unfollowUser(requester, targetPrivate);

        assertTrue(targetPrivate.getFollowers().isEmpty());
        assertTrue(requester.getFollowing().isEmpty());
    }

    // ----------------------------------------------
    // 3. requester has requested to follow private target -> remove request
    // ----------------------------------------------
    @Test
    void unfollowUser_ShouldRemoveRequest_WhenRequesterHasRequestedToFollowPrivateTarget() {
        requester.getFollowing().clear();
        targetPrivate.getFollowers().clear();

        FollowRequest pendingRequest = new FollowRequest(requester, targetPrivate);

        when(followRequestRepository.findByRequesterAndTarget(requester, targetPrivate))
                .thenReturn(Optional.of(pendingRequest));

        followService.unfollowUser(requester, targetPrivate);

        verify(followRequestRepository, times(1)).delete(pendingRequest);
    }

    // ----------------------------------------------
    // 4. requester is not following target -> ERROR
    // ----------------------------------------------
    @Test
    void unfollowUser_ShouldThrow_WhenRequesterIsNotFollowingTarget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> followService.unfollowUser(requester, targetPublic));
    }

    /////////////////////////
    // AcceptRequest Tests //
    /////////////////////////

    // ----------------------------------------------
    // 1. requester has not requsted target -> ERROR
    // ----------------------------------------------
    @Test
    void acceptRequest_ShouldThrow_WhenRequesterHasNotRequestedTarget() {
        when(followRequestRepository.findByRequesterAndTargetAndStatus(requester, targetPrivate,
                FollowRequest.RequestStatus.PENDING))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> followService.acceptRequest(requester, targetPrivate));
    }

    // ----------------------------------------------
    // 2. requester already follows target -> ERROR
    // ----------------------------------------------
    @Test
    void acceptRequest_ShouldThrow_WhenRequesterAlreadyFollowsTarget() {
        requester.getFollowing().clear();
        targetPrivate.getFollowers().clear();

        requester.getFollowing().add(targetPrivate);
        targetPrivate.getFollowers().add(requester);

        FollowRequest acceptedRequest = new FollowRequest(requester, targetPrivate);
        acceptedRequest.setStatus(FollowRequest.RequestStatus.ACCEPTED);

        when(followRequestRepository.findByRequesterAndTargetAndStatus(requester, targetPrivate,
                FollowRequest.RequestStatus.PENDING))
                .thenReturn(Optional.of(acceptedRequest));

        assertThrows(
                IllegalStateException.class,
                () -> followService.acceptRequest(requester, targetPrivate));
    }

    // ----------------------------------------------
    // 3. requester has requested target -> accept request
    // ----------------------------------------------
    @Test
    void acceptRequest_ShouldAcceptRequest_WhenRequesterHasRequestedTarget() {
        requester.getFollowing().clear();
        targetPrivate.getFollowers().clear();

        FollowRequest pendingRequest = new FollowRequest(requester, targetPrivate);
        pendingRequest.setStatus(FollowRequest.RequestStatus.PENDING);

        when(followRequestRepository.findByRequesterAndTargetAndStatus(requester, targetPrivate,
                FollowRequest.RequestStatus.PENDING))
                .thenReturn(Optional.of(pendingRequest));

        followService.acceptRequest(requester, targetPrivate);

        assertTrue(targetPrivate.getFollowers().contains(requester));
        assertTrue(requester.getFollowing().contains(targetPrivate));
        assertEquals(FollowRequest.RequestStatus.ACCEPTED, pendingRequest.getStatus());
    }

    /////////////////////////////////////////////////
    // RejectRequest Tests //
    /////////////////////////////////////////////////

    // ----------------------------------------------
    // 1. requester has not requsted target -> ERROR
    // ----------------------------------------------
    @Test
    void rejectRequest_ShouldThrow_WhenRequesterHasNotRequestedTarget() {
        assertThrows(
                IllegalArgumentException.class,
                () -> followService.rejectRequest(requester, targetPrivate));
    }

    // ----------------------------------------------
    // 2. requester has requested target -> reject request
    // ----------------------------------------------
    @Test
    void rejectRequest_ShouldRejectRequest_WhenRequesterHasRequestedTarget() {
        requester.getFollowing().clear();
        targetPrivate.getFollowers().clear();

        FollowRequest pendingRequest = new FollowRequest(requester, targetPrivate);
        pendingRequest.setStatus(FollowRequest.RequestStatus.PENDING);

        when(followRequestRepository.findByRequesterAndTargetAndStatus(requester, targetPrivate,
                FollowRequest.RequestStatus.PENDING))
                .thenReturn(Optional.of(pendingRequest));

        followService.rejectRequest(requester, targetPrivate);

        assertTrue(targetPrivate.getFollowers().isEmpty());
        assertTrue(requester.getFollowing().isEmpty());
        assertEquals(FollowRequest.RequestStatus.REJECTED, pendingRequest.getStatus());
    }

    //////////////////////////////////////////////////////
    // IsFollowing Tests //
    //////////////////////////////////////////////////////

    // ----------------------------------------------
    // 1. requester is in target followers -> true
    // ----------------------------------------------
    @Test
    void isFollowing_ShouldReturnTrue_WhenRequesterIsInTargetFollowers() {
        targetPublic.getFollowers().add(requester);

        boolean result = followService.isFollowing(requester, targetPublic);

        assertTrue(result);
    }

    // ----------------------------------------------
    // 2. requester is not in target followers -> false
    // ----------------------------------------------
    @Test
    void isFollowing_ShouldReturnFalse_WhenRequesterIsNotInTargetFollowers() {
        targetPublic.getFollowers().clear();
        boolean result = followService.isFollowing(requester, targetPublic);

        assertFalse(result);
    }

    //////////////////////////////////////////////////////
    // IsFollowRequested Tests //
    //////////////////////////////////////////////////////

    @Test
    void isFollowRequested_ShouldReturnTrue_WhenRequestExists() {
        when(followRequestRepository.findByRequesterAndTargetAndStatus(
                requester, targetPrivate, FollowRequest.RequestStatus.PENDING))
                .thenReturn(Optional.of(new FollowRequest(requester, targetPrivate)));

        boolean result = followService.isFollowRequested(requester, targetPrivate);

        assertTrue(result);
    }

    @Test
    void isFollowRequested_ShouldReturnFalse_WhenRequestDoesNotExist() {
        when(followRequestRepository.findByRequesterAndTargetAndStatus(
                requester, targetPrivate, FollowRequest.RequestStatus.PENDING))
                .thenReturn(Optional.empty());

        boolean result = followService.isFollowRequested(requester, targetPrivate);

        assertFalse(result);
    }

    //////////////////////////////////////////////////////
    // GetFollowers Tests //
    //////////////////////////////////////////////////////

    @Test
    void getFollowers_ShouldReturnMappedDtos() {
        UserSummaryProjection proj1 = mock(UserSummaryProjection.class);
        when(proj1.getId()).thenReturn(4L);
        when(proj1.getDisplayName()).thenReturn("pendingUser1");
        when(proj1.getProfileUrl()).thenReturn(null);
        when(proj1.getHobbies()).thenReturn("[]");
        when(proj1.getRelationship()).thenReturn("FOLLOWING");

        UserSummaryProjection proj2 = mock(UserSummaryProjection.class);
        when(proj2.getId()).thenReturn(5L);
        when(proj2.getDisplayName()).thenReturn("pendingUser2");
        when(proj2.getProfileUrl()).thenReturn(null);
        when(proj2.getHobbies()).thenReturn("[]");
        when(proj2.getRelationship()).thenReturn("FOLLOWING");

        when(userRepository.findFollowers(targetPublic.getId(), requester.getId()))
                .thenReturn(List.of(proj1, proj2));

        List<UserSummaryDto> dtos = followService.getFollowers(targetPublic, requester);

        assertEquals(2, dtos.size());
        assertEquals(4L, dtos.get(0).getId());
        assertEquals("pendingUser1", dtos.get(0).getDisplayName());
        assertEquals("FOLLOWING", dtos.get(0).getRelationship());
        assertEquals(null, dtos.get(0).getHobbies());

        assertEquals(5L, dtos.get(1).getId());
        assertEquals("pendingUser2", dtos.get(1).getDisplayName());
        assertEquals("FOLLOWING", dtos.get(1).getRelationship());
        assertEquals(null, dtos.get(1).getHobbies());
    }

    //////////////////////////////////////////////////////
    // GetFollowing Tests //
    //////////////////////////////////////////////////////

    @Test
    void getFollowing_ShouldReturnMappedDtos() {
        UserSummaryProjection proj1 = mock(UserSummaryProjection.class);
        when(proj1.getId()).thenReturn(4L);
        when(proj1.getDisplayName()).thenReturn("pendingUser1");
        when(proj1.getProfileUrl()).thenReturn(null);
        when(proj1.getHobbies()).thenReturn("[]");
        when(proj1.getRelationship()).thenReturn("FOLLOWING");

        UserSummaryProjection proj2 = mock(UserSummaryProjection.class);
        when(proj2.getId()).thenReturn(5L);
        when(proj2.getDisplayName()).thenReturn("pendingUser2");
        when(proj2.getProfileUrl()).thenReturn(null);
        when(proj2.getHobbies()).thenReturn("[]");
        when(proj2.getRelationship()).thenReturn("FOLLOWING");

        when(userRepository.findFollowing(targetPublic.getId(), requester.getId()))
                .thenReturn(List.of(proj1, proj2));

        List<UserSummaryDto> dtos = followService.getFollowing(targetPublic, requester);

        assertEquals(2, dtos.size());
        assertEquals(4L, dtos.get(0).getId());
        assertEquals("pendingUser1", dtos.get(0).getDisplayName());
        assertEquals("FOLLOWING", dtos.get(0).getRelationship());
        assertEquals(null, dtos.get(0).getHobbies());

        assertEquals(5L, dtos.get(1).getId());
        assertEquals("pendingUser2", dtos.get(1).getDisplayName());
        assertEquals("FOLLOWING", dtos.get(1).getRelationship());
        assertEquals(null, dtos.get(1).getHobbies());
    }

    //////////////////////////////////////////////////////
    // GetPendingRequests Tests //
    //////////////////////////////////////////////////////

    @Test
    void getPendingRequests_ShouldReturnMappedDtos() {
        UserSummaryProjection proj1 = mock(UserSummaryProjection.class);
        when(proj1.getId()).thenReturn(4L);
        when(proj1.getDisplayName()).thenReturn("pendingUser1");
        when(proj1.getProfileUrl()).thenReturn(null);
        when(proj1.getHobbies()).thenReturn("[]");
        when(proj1.getRelationship()).thenReturn("REQUESTED");

        UserSummaryProjection proj2 = mock(UserSummaryProjection.class);
        when(proj2.getId()).thenReturn(5L);
        when(proj2.getDisplayName()).thenReturn("pendingUser2");
        when(proj2.getProfileUrl()).thenReturn(null);
        when(proj2.getHobbies()).thenReturn("[]");
        when(proj2.getRelationship()).thenReturn("REQUESTED");

        when(userRepository.findPendingRequests(targetPrivate.getId()))
                .thenReturn(List.of(proj1, proj2));

        List<UserSummaryDto> dtos = followService.getPendingRequests(targetPrivate);

        assertEquals(2, dtos.size());
        assertEquals(4L, dtos.get(0).getId());
        assertEquals("pendingUser1", dtos.get(0).getDisplayName());
        assertEquals("REQUESTED", dtos.get(0).getRelationship());
        assertEquals(null, dtos.get(0).getHobbies());

        assertEquals(5L, dtos.get(1).getId());
        assertEquals("pendingUser2", dtos.get(1).getDisplayName());
        assertEquals("REQUESTED", dtos.get(1).getRelationship());
        assertEquals(null, dtos.get(1).getHobbies());
    }

}
