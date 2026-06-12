package kh.karazin.foodwise.store.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryIconDto {
    private String key;
    private String iconName;
}
