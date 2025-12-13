package HobbyList.example.HobbyList.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import HobbyList.example.HobbyList.dto.MilestoneDto;
import HobbyList.example.HobbyList.model.Milestone;
import HobbyList.example.HobbyList.model.Photo;
import HobbyList.example.HobbyList.repository.MilestoneRepository;

@ExtendWith(MockitoExtension.class)
public class MilestoneServiceTest {

    @Mock
    private MilestoneRepository milestoneRepository;

    @InjectMocks
    private MilestoneService milestoneService;

    private Milestone milestone;
    private Milestone parentMilestone;

    @BeforeEach
    void setUp() {
        milestone = new Milestone();
        milestone.setId(1L);
        milestone.setTask("Test Task");
        milestone.setCompleted(false);
        milestone.setCompletionRate(0.0);
        milestone.setSubMilestones(new ArrayList<>());
        milestone.setTaggedPhotos(new ArrayList<>());

        parentMilestone = new Milestone();
        parentMilestone.setId(2L);
        parentMilestone.setTask("Parent Task");
        parentMilestone.setSubMilestones(new ArrayList<>(Arrays.asList(milestone)));
        milestone.setParent(parentMilestone);
    }

    //////////////////
    // toDto Tests //
    //////////////////

    @Test
    void toDto_ShouldConvertMilestoneToDto() {
        MilestoneDto dto = milestoneService.toDto(milestone);

        assertNotNull(dto);
        assertEquals(milestone.getId(), dto.id());
        assertEquals(milestone.getTask(), dto.task());
        assertEquals(parentMilestone.getId(), dto.parentId());
    }

    @Test
    void toDto_ShouldHandleNullLists() {
        milestone.setSubMilestones(Collections.emptyList());
        milestone.setTaggedPhotos(null);
        MilestoneDto dto = milestoneService.toDto(milestone);
        assertNotNull(dto);
        assertTrue(dto.getSubMilestoneIds().isEmpty());
    }

    /////////////////////////////////
    // markMilestoneComplete Tests //
    /////////////////////////////////

    @Test
    void markMilestoneComplete_ShouldMarkCompleteAndUpdateParents() {
        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        milestoneService.markMilestoneComplete(1L);

        verify(milestoneRepository).findById(1L);
        assertTrue(milestone.isCompleted());
        assertEquals(1.0, milestone.getCompletionRate());
        assertEquals("complete", milestone.getManualState());

        // verify parent update was called
        // Since updateParentsCompletion calls save on parent, we can verify that
        verify(milestoneRepository, atLeastOnce()).save(parentMilestone);
    }

    @Test
    void markMilestoneComplete_ShouldReturnIfNotFound() {
        when(milestoneRepository.findById(99L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> milestoneService.markMilestoneComplete(99L));
        verify(milestoneRepository, never()).save(any(Milestone.class));
    }

    @Test
    void markMilestoneComplete_ShouldMarkChildrenResultingInRecursion() {
        Milestone child = new Milestone();
        child.setId(3L);
        child.setSubMilestones(new ArrayList<>());
        milestone.getSubMilestones().add(child);
        child.setParent(milestone);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        milestoneService.markMilestoneComplete(1L);

        assertTrue(child.isCompleted());
        assertEquals(1.0, child.getCompletionRate());
        verify(milestoneRepository, atLeastOnce()).save(child);
    }

    ///////////////////////////////////
    // markMilestoneIncomplete Tests //
    ///////////////////////////////////

    @Test
    void markMilestoneIncomplete_ShouldMarkIncompleteAndRecalculate() {
        milestone.setCompleted(true);
        milestone.setCompletionRate(1.0);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        milestoneService.markMilestoneIncomplete(1L);

        assertFalse(milestone.isCompleted());
        assertEquals("incomplete", milestone.getManualState());
        // Since no children, completion rate should be 0.0
        assertEquals(0.0, milestone.getCompletionRate());

        verify(milestoneRepository, atLeastOnce()).save(parentMilestone);
    }

    @Test
    void markMilestoneIncomplete_ShouldCalculateRateFromChildren() {
        Milestone child = new Milestone();
        child.setCompletionRate(0.5);
        milestone.getSubMilestones().add(child);

        when(milestoneRepository.findById(1L)).thenReturn(Optional.of(milestone));

        milestoneService.markMilestoneIncomplete(1L);

        // 1 child with 0.5 rate -> avg 0.5
        assertEquals(0.5, milestone.getCompletionRate());
    }

    ///////////////////////////////////
    // updateParentsCompletion Tests //
    ///////////////////////////////////

    @Test
    void updateParentsCompletion_ShouldHandleNullParent() {
        assertDoesNotThrow(() -> milestoneService.updateParentsCompletion(null));
    }

    @Test
    void updateParentsCompletion_ShouldUpdateParentBasedOnChildren() {
        // parent has one child (milestone) which is 0.0 complete
        milestoneService.updateParentsCompletion(parentMilestone);

        assertEquals(0.0, parentMilestone.getCompletionRate());
        assertFalse(parentMilestone.isCompleted());
    }

    @Test
    void updateParentsCompletion_ShouldRespectManualStateComplete() {
        parentMilestone.setManualState("complete");
        // even if child is incomplete, if manual state is complete, it checks if avg <
        // 1.0
        // if avg < 1.0, it sets manual state to "none" and uses avg

        milestone.setCompletionRate(0.5);

        milestoneService.updateParentsCompletion(parentMilestone);

        assertEquals("none", parentMilestone.getManualState());
        assertEquals(0.5, parentMilestone.getCompletionRate());
    }

    @Test
    void updateParentsCompletion_ShouldRespectManualStateIncomplete() {
        parentMilestone.setManualState("incomplete");
        milestone.setCompletionRate(1.0); // Child is fully complete

        milestoneService.updateParentsCompletion(parentMilestone);

        assertFalse(parentMilestone.isCompleted());
        assertEquals(1.0, parentMilestone.getCompletionRate());
        // Logic says: if incomplete, setCompleted(false), setRate(avgRate).
    }
}
