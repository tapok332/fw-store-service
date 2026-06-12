package kh.karazin.foodwise.store.repository;

import kh.karazin.foodwise.store.entity.MenuSectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for menu sections.
 */
public interface MenuSectionRepository extends JpaRepository<MenuSectionEntity, UUID> {
}
