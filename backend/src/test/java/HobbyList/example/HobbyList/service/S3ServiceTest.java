package HobbyList.example.HobbyList.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
public class S3ServiceTest {

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private S3Service s3Service;

    /////////////////////////////
    // generateUploadUrl Tests //
    /////////////////////////////

    @Test
    void generateUploadUrl_ShouldReturnUrl() throws Exception {
        PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/upload"));
        when(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = s3Service.generateUploadUrl("bucket", "key", "image/png");

        assertEquals("https://example.com/upload", url);

        ArgumentCaptor<PutObjectPresignRequest> captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        verify(s3Presigner).presignPutObject(captor.capture());

        assertEquals("bucket", captor.getValue().putObjectRequest().bucket());
        assertEquals("key", captor.getValue().putObjectRequest().key());
        assertEquals("image/png", captor.getValue().putObjectRequest().contentType());
    }

    ///////////////////////////////
    // generateDownloadUrl Tests //
    ///////////////////////////////

    @Test
    void generateDownloadUrl_ShouldReturnUrl() throws Exception {
        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/download"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);

        String url = s3Service.generateDownloadUrl("bucket", "key");

        assertEquals("https://example.com/download", url);

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());

        assertEquals("bucket", captor.getValue().getObjectRequest().bucket());
        assertEquals("key", captor.getValue().getObjectRequest().key());
    }
}
