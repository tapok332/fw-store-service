package kh.karazin.foodwise.store.application.port.in;

import kh.karazin.foodwise.store.domain.StoreId;
import org.springframework.web.multipart.MultipartFile;

/** Store media (hero image / category icon) uploads. */
public interface MediaUseCase {

    String uploadHeroImage(StoreId storeId, MultipartFile file);

    String uploadCategoryIcon(MultipartFile file);
}
