package kh.karazin.foodwise.store.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeroImageDto {
    private int id;
    private String key;
    private String url;
    private String icon;
}
