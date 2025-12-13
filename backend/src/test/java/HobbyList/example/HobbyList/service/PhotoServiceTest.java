package HobbyList.example.HobbyList.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import HobbyList.example.HobbyList.dto.PhotoDto;
import HobbyList.example.HobbyList.model.Photo;
import HobbyList.example.HobbyList.repository.PhotoRepository;

@ExtendWith(MockitoExtension.class)
public class PhotoServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private PhotoService photoService;

    private Photo photo;

    @BeforeEach
    void setUp() {
        photo = new Photo();
        photo.setId(1L);
        photo.setTopic("Test Topic");
        photo.setUploadDate(LocalDateTime.now());
        photo.setTaggedMilestones(new ArrayList<>());
        photo.setDescription("Test Description");
    }

    //////////////////////////
    // getPhotoById Tests //
    //////////////////////////

    @Test
    void getPhotoById_ShouldReturnPhoto_WhenExists() {
        when(photoRepository.findById(1L)).thenReturn(Optional.of(photo));
        Photo result = photoService.getPhotoById(1L);
        assertEquals(photo, result);
    }

    @Test
    void getPhotoById_ShouldReturnNull_WhenNotExists() {
        when(photoRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(photoService.getPhotoById(1L));
    }

    @Test
    void getPhotoById_ShouldReturnNull_WhenIdIsNull() {
        assertNull(photoService.getPhotoById(null));
        verify(photoRepository, never()).findById(any());
    }

    ///////////////////////
    // savePhoto Tests //
    ///////////////////////

    @Test
    void savePhoto_ShouldReturnSavedPhoto() {
        when(photoRepository.save(photo)).thenReturn(photo);
        Photo saved = photoService.savePhoto(photo);
        assertEquals(photo, saved);
    }

    @Test
    void savePhoto_ShouldReturnNull_WhenPhotoIsNull() {
        assertNull(photoService.savePhoto(null));
        verify(photoRepository, never()).save(any());
    }

    /////////////////////////
    // deletePhoto Tests //
    /////////////////////////

    @Test
    void deletePhoto_ShouldDelete_WhenIdIsNotNull() {
        photoService.deletePhoto(1L);
        verify(photoRepository).deleteById(1L);
    }

    @Test
    void deletePhoto_ShouldDoNothing_WhenIdIsNull() {
        photoService.deletePhoto(null);
        verify(photoRepository, never()).deleteById(any());
    }

    ///////////////////
    // toDto Tests //
    ///////////////////

    @Test
    void toDto_ShouldConvertToDto_WhenUrlIsValid() {
        String imageUrl = "https://s3.amazonaws.com/hobbylist-photos.s3/photos/test.jpg";
        when(s3Service.generateDownloadUrl(anyString(), anyString())).thenReturn("presigned-url");

        PhotoDto dto = photoService.toDto(photo, imageUrl);

        assertEquals(photo.getId(), dto.getId());
        assertEquals("presigned-url", dto.getImageUrl());
        assertEquals(photo.getTopic(), dto.getTopic());
        assertEquals(photo.getDescription(), dto.getDescription());

        verify(s3Service).generateDownloadUrl(eq("hobbylist-photos"), eq("photos/test.jpg"));
    }

    @Test
    void toDto_ShouldThrowException_WhenUrlFormatIsUnexpected() {
        // The service does imageUrl.substring(imageUrl.indexOf("photos/"))
        // If "photos/" is missing, indexOf returns -1. substring(-1) throws
        // StringIndexOutOfBoundsException
        String imageUrl = "invalid-url";

        assertThrows(StringIndexOutOfBoundsException.class, () -> photoService.toDto(photo, imageUrl));
    }
}
