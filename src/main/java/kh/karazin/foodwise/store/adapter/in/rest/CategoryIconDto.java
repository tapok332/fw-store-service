package kh.karazin.foodwise.store.adapter.in.rest;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryIconDto {
    private String key;
    private String iconName;
}
