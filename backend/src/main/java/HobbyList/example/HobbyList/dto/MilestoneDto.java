package HobbyList.example.HobbyList.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public record MilestoneDto(
        Long id,
        String task,
        OffsetDateTime dueDate,
        Boolean completed,
        Double completionRate,
        Long parentId,
        List<MilestoneDto> subMilestones,
        List<Long> taggedPhotoIds,
        String hobbyTag,
        String manualState) {

    public MilestoneDto(String task) {
        this(null, task, null, false, null, null, null, new ArrayList<>(), null, null);
    }

    public MilestoneDto(String task, Boolean completed) {
        this(null, task, null, completed, null, null, null, new ArrayList<>(), null, null);
    }

    public List<Long> getSubMilestoneIds() {
        List<Long> subMilestoneIds = new ArrayList<>();
        if (subMilestones != null) {
            for (MilestoneDto dto : subMilestones) {
                if (dto.id() != null) {
                    subMilestoneIds.add(dto.id());
                }
            }
        }
        return subMilestoneIds;
    }
}
