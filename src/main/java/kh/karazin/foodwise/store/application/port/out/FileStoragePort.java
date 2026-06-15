package kh.karazin.foodwise.store.application.port.out;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/** Outbound port for object storage of media files. */
public interface FileStoragePort {

    String uploadFile(MultipartFile file, String folder) throws IOException;

    void deleteFile(String fileUrl);
}
