package com.management.web.service.dto;

import com.management.web.domain.GiftcardProduct;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GiftcardResponseDto {

    private Long id;
    private String name;
    private Long price;
    private String image;

    public GiftcardResponseDto(GiftcardProduct entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.price = entity.getPrice();
        this.image = entity.getImage();
    }
}
