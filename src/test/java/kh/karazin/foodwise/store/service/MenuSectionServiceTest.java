package kh.karazin.foodwise.store.service;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.dto.MenuSectionCreateRequest;
import kh.karazin.foodwise.store.dto.MenuSectionDto;
import kh.karazin.foodwise.store.entity.MenuSectionEntity;
import kh.karazin.foodwise.store.entity.StoreEntity;
import kh.karazin.foodwise.store.repository.MenuSectionRepository;
import kh.karazin.foodwise.store.repository.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuSectionServiceTest {

    @Mock private MenuSectionRepository sectionRepository;
    @Mock private StoreRepository storeRepository;

    @InjectMocks
    private MenuSectionService sectionService;

    @Test
    void createSection_persistsSectionLinkedToStore_whenStoreExists() {
        UUID storeId = UUID.randomUUID();
        StoreEntity store = StoreEntity.builder().id(storeId).name("Bakery").build();
        var request = new MenuSectionCreateRequest("Breakfast", 1);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(sectionRepository.save(any(MenuSectionEntity.class))).thenAnswer(inv -> {
            MenuSectionEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        MenuSectionDto result = sectionService.createSection(storeId, request);

        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("Breakfast");
        assertThat(result.sortOrder()).isEqualTo(1);
        assertThat(result.storeId()).isEqualTo(storeId);

        ArgumentCaptor<MenuSectionEntity> captor = ArgumentCaptor.forClass(MenuSectionEntity.class);
        verify(sectionRepository).save(captor.capture());
        assertThat(captor.getValue().getStore().getId()).isEqualTo(storeId);
        assertThat(captor.getValue().getTitle()).isEqualTo("Breakfast");
        assertThat(captor.getValue().getSortOrder()).isEqualTo(1);
    }

    @Test
    void createSection_throwsEntityNotFound_whenStoreDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        var request = new MenuSectionCreateRequest("Lunch", 2);
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectionService.createSection(storeId, request))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(sectionRepository, never()).save(any());
    }
}
