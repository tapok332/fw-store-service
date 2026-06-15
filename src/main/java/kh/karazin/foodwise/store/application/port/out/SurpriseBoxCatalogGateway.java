package kh.karazin.foodwise.store.application.port.out;

import kh.karazin.foodwise.store.domain.CatalogItem;

import java.util.Optional;

/** Outbound port to surprisebox-service for the unified internal item lookup. */
public interface SurpriseBoxCatalogGateway {

    Optional<CatalogItem> findBoxAsItem(java.util.UUID boxId);
}
