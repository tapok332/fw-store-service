package kh.karazin.foodwise.store.application.usecase;

import kh.karazin.foodwise.common.exception.FoodWiseException;
import kh.karazin.foodwise.store.application.port.out.MenuSectionRepository;
import kh.karazin.foodwise.store.application.port.out.StoreRepository;
import kh.karazin.foodwise.store.domain.MenuSection;
import kh.karazin.foodwise.store.domain.MenuSectionId;
import kh.karazin.foodwise.store.domain.StoreId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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

    private MenuSectionService sectionService;

    @BeforeEach
    void setUp() {
        sectionService = new MenuSectionService(sectionRepository, storeRepository);
    }

    @Test
    void createSection_persistsSectionLinkedToStore_whenStoreExists() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.existsById(new StoreId(storeId))).thenReturn(true);
        when(sectionRepository.save(any(MenuSection.class))).thenAnswer(inv -> {
            MenuSection s = inv.getArgument(0);
            return new MenuSection(new MenuSectionId(UUID.randomUUID()), s.storeId(), s.title(), s.sortOrder());
        });

        MenuSection result = sectionService.createSection(new StoreId(storeId), "Breakfast", 1);

        assertThat(result.id()).isNotNull();
        assertThat(result.title()).isEqualTo("Breakfast");
        assertThat(result.sortOrder()).isEqualTo(1);
        assertThat(result.storeId()).isEqualTo(new StoreId(storeId));

        ArgumentCaptor<MenuSection> captor = ArgumentCaptor.forClass(MenuSection.class);
        verify(sectionRepository).save(captor.capture());
        assertThat(captor.getValue().storeId()).isEqualTo(new StoreId(storeId));
        assertThat(captor.getValue().title()).isEqualTo("Breakfast");
    }

    @Test
    void createSection_throwsEntityNotFound_whenStoreDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.existsById(new StoreId(storeId))).thenReturn(false);

        assertThatThrownBy(() -> sectionService.createSection(new StoreId(storeId), "Lunch", 2))
                .isInstanceOf(FoodWiseException.class)
                .extracting("errorDetails.httpStatus")
                .isEqualTo(HttpStatus.NOT_FOUND);

        verify(sectionRepository, never()).save(any());
    }
}
