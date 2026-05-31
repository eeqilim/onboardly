package bf.backend.employee_service.service;

import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

public interface FileStorageService {

    String upload(MultipartFile file, String keyPrefix);

    byte[] download(String key);

    String generatePresignedDownloadUrl(String key, Duration ttl);

    void delete(String key);
}
