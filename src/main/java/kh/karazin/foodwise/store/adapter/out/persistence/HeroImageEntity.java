package kh.karazin.foodwise.store.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hero_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeroImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String url;

    private String icon;
}
