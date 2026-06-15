package kh.karazin.foodwise.store.adapter.out.storage;

import kh.karazin.foodwise.store.application.port.out.FileStoragePort;
import kh.karazin.foodwise.store.config.GcsStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Outbound adapter implementing {@link FileStoragePort} on top of the
 * Google Cloud Storage infrastructure bean.
 */
@Component
@RequiredArgsConstructor
class GcsFileStorageAdapter implements FileStoragePort {

    private final GcsStorageService gcsStorageService;

    @Override
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        return gcsStorageService.uploadFile(file, folder);
    }

    @Override
    public void deleteFile(String fileUrl) {
        gcsStorageService.deleteFile(fileUrl);
    }
}
