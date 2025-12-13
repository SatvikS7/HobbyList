package HobbyList.example.HobbyList.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import HobbyList.example.HobbyList.model.User;
import HobbyList.example.HobbyList.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class HobbyServiceTest {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    HobbyService hobbyService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
    }

    //////////////////////////
    // AddHobbyToUser Tests //
    //////////////////////////

    // ----------------------------------------------
    // 1. null hobby -> ERROR
    // ----------------------------------------------
    @Test
    void addHobbyToUser_ShouldThrow_WhenHobbyIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> hobbyService.addHobbyToUser(user, null));
    }

    // ----------------------------------------------
    // 2. blank hobby -> ERROR
    // ----------------------------------------------
    @Test
    void addHobbyToUser_ShouldThrow_WhenHobbyIsBlank() {
        assertThrows(
                IllegalArgumentException.class,
                () -> hobbyService.addHobbyToUser(user, "   "));
    }

    // ----------------------------------------------
    // 3. valid single hobby -> SUCCESS
    // ----------------------------------------------
    @Test
    void addHobbyToUser_ShouldAddHobby_WhenHobbyIsValid() {
        hobbyService.addHobbyToUser(user, "hobby");
        assertEquals(1, user.getHobbies().size());
        assertTrue(user.getHobbies().contains("hobby"));

        hobbyService.addHobbyToUser(user, "hobby");
        assertEquals(1, user.getHobbies().size());
        assertTrue(user.getHobbies().contains("hobby"));
    }

    // ----------------------------------------------
    // 4. valid multiple hobbies -> SUCCESS
    // ----------------------------------------------
    @Test
    void addHobbyToUser_ShouldAddHobbies_WhenHobbiesAreValid() {
        hobbyService.addHobbyToUser(user, "hobby");
        hobbyService.addHobbyToUser(user, "hobby2");
        assertEquals(2, user.getHobbies().size());
        assertTrue(user.getHobbies().contains("hobby2"));
        assertTrue(user.getHobbies().contains("hobby"));

        hobbyService.addHobbyToUser(user, "Hobby");
        assertEquals(2, user.getHobbies().size());
        assertTrue(user.getHobbies().contains("hobby"));
        assertTrue(user.getHobbies().contains("hobby2"));
    }

    // ----------------------------------------------
    // 5. duplicate valid hobbies with different casing -> SUCCESS
    // ----------------------------------------------
    @Test
    void addHobbyToUser_ShouldAddHobby_WhenHobbyIsDuplicate() {
        hobbyService.addHobbyToUser(user, "hobby");
        hobbyService.addHobbyToUser(user, "    HOBBY      ");
        assertEquals(1, user.getHobbies().size());
        assertTrue(user.getHobbies().contains("hobby"));
    }
}
