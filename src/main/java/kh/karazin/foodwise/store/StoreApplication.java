package kh.karazin.foodwise.store;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"kh.karazin.foodwise.store", "kh.karazin.foodwise.common"})
@EntityScan(basePackages = {"kh.karazin.foodwise.store", "kh.karazin.foodwise.common"})
@EnableJpaRepositories(basePackages = {"kh.karazin.foodwise.store", "kh.karazin.foodwise.common"})
@ConfigurationPropertiesScan("kh.karazin.foodwise.store.config")
@EnableScheduling
@EnableCaching
public class StoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }
}
