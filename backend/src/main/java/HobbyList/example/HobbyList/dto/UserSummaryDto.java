package HobbyList.example.HobbyList.dto;

import java.util.List;

public record UserSummaryDto(
        long id,
        String displayName,
        String profileUrl,
        List<String> hobbies,
        String relationship) {

    public long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public String getRelationship() {
        return relationship;
    }
}
