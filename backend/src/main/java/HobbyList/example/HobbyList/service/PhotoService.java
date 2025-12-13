package HobbyList.example.HobbyList.service;

import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import HobbyList.example.HobbyList.model.Milestone;
import HobbyList.example.HobbyList.model.Photo;
import HobbyList.example.HobbyList.dto.PhotoDto;
import HobbyList.example.HobbyList.repository.PhotoRepository;

@Service
public class PhotoService {
    private final PhotoRepository photoRepository;
    private final S3Service s3Service;

    public PhotoService(PhotoRepository photoRepository, S3Service s3Service) {
        this.photoRepository = photoRepository;
        this.s3Service = s3Service;
    }
    
    public Photo getPhotoById(Long id) {
        if (id == null) {
            return null;
        }
        return photoRepository.findById(id).orElse(null);
    }

    public Photo savePhoto(Photo photo) {
        if (photo == null) {
            return null;
        }
        return photoRepository.save(photo); 
    }

    public void deletePhoto(Long id) {
        if (id == null) {
            return;
        }
        photoRepository.deleteById(id);
    }

    public PhotoDto toDto(Photo photo, String imageUrl) {
        //System.out.println("Image URL: " + imageUrl);
        //System.out.println("Image URL Substring: " + imageUrl.substring(imageUrl.indexOf("photos/")));
        String preSignedUrl = s3Service.generateDownloadUrl("hobbylist-photos", imageUrl.substring(imageUrl.indexOf("photos/")));
        return new PhotoDto(
            photo.getId(),
            photo.getTopic(),
            preSignedUrl,
            photo.getDescription(),
            photo.getUploadDate(),
            photo.getTaggedMilestones() != null ? photo.getTaggedMilestones().stream().map(Milestone::getId).collect(Collectors.toList()) : null
        );
    }
}
