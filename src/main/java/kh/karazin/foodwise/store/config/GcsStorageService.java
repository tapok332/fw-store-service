package kh.karazin.foodwise.store.config;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Google Cloud Storage service for file uploads.
 */
@Slf4j
@Service
public class GcsStorageService {

    private final String bucketName;
    private final Storage storage;

    public GcsStorageService(@Value("${gcp.storage.bucket}") String bucketName) {
        this.bucketName = bucketName;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Upload a file to GCS and return its public URL.
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String originalName = file.getOriginalFilename();
        String safeName = (originalName != null)
                ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "unnamed";
        String fileName = folder + "/" + UUID.randomUUID() + "-" + safeName;
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        storage.create(blobInfo, file.getBytes());
        String url = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
        log.info("Uploaded file to GCS: {}", url);
        return url;
    }

    /**
     * Delete a file from GCS by its URL.
     */
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            String prefix = String.format("https://storage.googleapis.com/%s/", bucketName);
            if (fileUrl.startsWith(prefix)) {
                String objectName = fileUrl.substring(prefix.length());
                storage.delete(BlobId.of(bucketName, objectName));
                log.info("Deleted file from GCS: {}", fileUrl);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from GCS: {}", fileUrl, e);
        }
    }
}
