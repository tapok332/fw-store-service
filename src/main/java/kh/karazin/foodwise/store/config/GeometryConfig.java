package kh.karazin.foodwise.store.config;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for JTS GeometryFactory bean.
 */
@Configuration
public class GeometryConfig {

    /**
     * GeometryFactory with SRID 4326 (WGS 84) for PostGIS geography.
     */
    @Bean
    public GeometryFactory geometryFactory() {
        return new GeometryFactory(new PrecisionModel(), 4326);
    }
}
