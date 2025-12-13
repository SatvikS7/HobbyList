package HobbyList.example.HobbyList.dto;

import java.util.List;

public record ProfileDto(
        long id,
        String displayName,
        String profileUrl,
        String description,
        List<String> hobbies,
        Boolean isPrivate,
        int followersCount,
        int followingCount,
        boolean isFollowing,
        boolean isFollowRequested,
        List<MilestoneDto> milestones,
        List<PhotoDto> photos) {
    

    public long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public Boolean getIsPrivate() {
        return isPrivate;
    }

    public int getFollowersCount() {
        return followersCount;
    }

    public int getFollowingCount() {
        return followingCount;
    }

    public boolean getIsFollowing() {
        return isFollowing;
    }

    public boolean getIsFollowRequested() {
        return isFollowRequested;
    }

    public List<MilestoneDto> getMilestones() {
        return milestones;
    }

    public List<PhotoDto> getPhotos() {
        return photos;
    }
}
