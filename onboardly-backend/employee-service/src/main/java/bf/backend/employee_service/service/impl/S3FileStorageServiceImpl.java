package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.s3.bucket-name}")
    private String bucketName;

    @Override
    public String upload(MultipartFile file, String keyPrefix) {
        String sanitized = sanitizeFilename(file.getOriginalFilename());
        String key = buildKey(keyPrefix, sanitized);
        try {
            PutObjectRequest req = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(Objects.requireNonNullElse(file.getContentType(), "application/octet-stream"))
                    .build();
            s3Client.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage(), e);
        } catch (SdkException e) {
            throw new RuntimeException("S3 upload failed for key '" + key + "': " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] download(String key) {
        try {
            GetObjectRequest req = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            return s3Client.getObjectAsBytes(req).asByteArray();
        } catch (SdkException e) {
            throw new RuntimeException("S3 download failed for key '" + key + "': " + e.getMessage(), e);
        }
    }

    @Override
    public String generatePresignedDownloadUrl(String key, Duration ttl) {
        try {
            GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                    .signatureDuration(ttl)
                    .getObjectRequest(r -> r.bucket(bucketName).key(key))
                    .build();
            return s3Presigner.presignGetObject(presignReq).url().toString();
        } catch (SdkException e) {
            throw new RuntimeException(
                    "Failed to generate presigned URL for key '" + key + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            DeleteObjectRequest req = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(req);
        } catch (SdkException e) {
            throw new RuntimeException("S3 delete failed for key '" + key + "': " + e.getMessage(), e);
        }
    }

    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) return "file";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String buildKey(String keyPrefix, String filename) {
        String prefix = keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
        return prefix + UUID.randomUUID() + "-" + filename;
    }
}
