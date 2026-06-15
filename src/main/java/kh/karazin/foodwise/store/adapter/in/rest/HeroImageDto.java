package kh.karazin.foodwise.store.adapter.in.rest;

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
