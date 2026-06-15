package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.in.MediaUseCase;
import kh.karazin.foodwise.store.application.port.out.FileStoragePort;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.domain.Store;
import kh.karazin.foodwise.store.domain.StoreId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Store media (hero image / category icon) uploads.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class MediaService implements MediaUseCase {

    private final FileStoragePort fileStorage;
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public String uploadHeroImage(StoreId storeId, MultipartFile file) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId.value()));

        try {
            if (store.heroImageUrl() != null) {
                fileStorage.deleteFile(store.heroImageUrl());
            }

            String url = fileStorage.uploadFile(file, "stores/" + storeId.value() + "/hero");
            storeRepository.save(store.withHeroImage(url));
            log.info("Uploaded hero image for store {}: {}", storeId.value(), url);
            return url;
        } catch (IOException e) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.FILE_UPLOAD_FAILED, "Failed to upload hero image: " + e.getMessage());
        }
    }

    @Override
    public String uploadCategoryIcon(MultipartFile file) {
        try {
            String url = fileStorage.uploadFile(file, "categories/icons");
            log.info("Uploaded category icon: {}", url);
            return url;
        } catch (IOException e) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.FILE_UPLOAD_FAILED, "Failed to upload category icon: " + e.getMessage());
        }
    }
}
