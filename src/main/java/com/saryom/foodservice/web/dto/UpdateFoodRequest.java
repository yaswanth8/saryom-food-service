package com.saryom.foodservice.web.dto;

import com.saryom.foodservice.domain.DietaryType;
import com.saryom.foodservice.domain.FoodType;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** All fields optional — only the non-null ones are applied. */
public record UpdateFoodRequest(
        @Size(max = 120) String title,
        @Size(max = 4000) String description,
        @Size(max = 80) String quantity,
        FoodType foodType,
        DietaryType dietary,
        @Size(max = 120) String locationText,
        Instant bestBefore,
        @Size(max = 1000) String imageUrl) {
}
