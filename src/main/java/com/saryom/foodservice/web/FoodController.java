package com.saryom.foodservice.web;

import com.saryom.foodservice.auth.CurrentUser;
import com.saryom.foodservice.domain.FoodSort;
import com.saryom.foodservice.domain.FoodType;
import com.saryom.foodservice.service.FoodService;
import com.saryom.foodservice.web.dto.CreateFoodRequest;
import com.saryom.foodservice.web.dto.FoodCardResponse;
import com.saryom.foodservice.web.dto.FoodDetailResponse;
import com.saryom.foodservice.web.dto.UpdateFoodRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/food")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping
    public Page<FoodCardResponse> browse(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) FoodType foodType,
            @RequestParam(defaultValue = "NEWEST") FoodSort sort,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radiusMiles,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return foodService.browse(q, foodType, sort, lat, lng, radiusMiles, page, size);
    }

    @GetMapping("/mine")
    public List<FoodDetailResponse> mine() {
        return foodService.mine(CurrentUser.requireUid());
    }

    @GetMapping("/reserved")
    public List<FoodDetailResponse> reserved() {
        return foodService.reservedByMe(CurrentUser.requireUid());
    }

    @GetMapping("/{id}")
    public FoodDetailResponse detail(@PathVariable UUID id) {
        return foodService.getDetail(id, CurrentUser.uid());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodDetailResponse create(@Valid @RequestBody CreateFoodRequest request) {
        return foodService.create(CurrentUser.requireUid(), request);
    }

    @PatchMapping("/{id}")
    public FoodDetailResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateFoodRequest request) {
        return foodService.update(id, CurrentUser.requireUid(), request);
    }

    @PostMapping("/{id}/reserve")
    public FoodDetailResponse reserve(@PathVariable UUID id) {
        return foodService.reserve(id, CurrentUser.requireUid());
    }

    @PostMapping("/{id}/release")
    public FoodDetailResponse release(@PathVariable UUID id) {
        return foodService.release(id, CurrentUser.requireUid());
    }

    @PostMapping("/{id}/complete")
    public FoodDetailResponse complete(@PathVariable UUID id) {
        return foodService.complete(id, CurrentUser.requireUid());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        foodService.cancel(id, CurrentUser.requireUid());
    }
}
