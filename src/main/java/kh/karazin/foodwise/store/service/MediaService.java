package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseErrorCode;
import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.config.GcsStorageService;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Service for managing store media (hero images, category icons, etc.).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final GcsStorageService gcsStorageService;
    private final StoreRepository storeRepository;

    /**
     * Upload a hero image for a store.
     */
    @Transactional
    public String uploadHeroImage(UUID storeId, MultipartFile file) {
        StoreEntity store = storeRepository.findById(storeId)
                .orElseThrow(() -> FoodWiseException.errorWithDescription(
                        FoodWiseErrorCode.ENTITY_NOT_FOUND, "Store not found: " + storeId));

        try {
            // Delete old hero image if exists
            if (store.getHeroImageUrl() != null) {
                gcsStorageService.deleteFile(store.getHeroImageUrl());
            }

            String url = gcsStorageService.uploadFile(file, "stores/" + storeId + "/hero");
            store.setHeroImageUrl(url);
            storeRepository.save(store);
            log.info("Uploaded hero image for store {}: {}", storeId, url);
            return url;
        } catch (IOException e) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.FILE_UPLOAD_FAILED, "Failed to upload hero image: " + e.getMessage());
        }
    }

    /**
     * Upload a category icon.
     */
    public String uploadCategoryIcon(MultipartFile file) {
        try {
            String url = gcsStorageService.uploadFile(file, "categories/icons");
            log.info("Uploaded category icon: {}", url);
            return url;
        } catch (IOException e) {
            throw FoodWiseException.errorWithDescription(
                    FoodWiseErrorCode.FILE_UPLOAD_FAILED, "Failed to upload category icon: " + e.getMessage());
        }
    }
}
