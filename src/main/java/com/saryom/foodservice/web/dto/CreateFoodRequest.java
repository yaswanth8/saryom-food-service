package com.saryom.foodservice.web.dto;

import com.saryom.foodservice.domain.DietaryType;
import com.saryom.foodservice.domain.FoodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateFoodRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 4000) String description,
        @NotBlank @Size(max = 80) String quantity,
        @NotNull FoodType foodType,
        DietaryType dietary,
        @Size(max = 120) String locationText,
        Double lat,
        Double lng,
        Instant bestBefore,
        @Size(max = 1000) String imageUrl) {
}
