package com.saryom.foodservice.web;

import com.saryom.foodservice.auth.FirebaseAuthenticationFilter;
import com.saryom.foodservice.auth.RestAuthenticationEntryPoint;
import com.saryom.foodservice.auth.StubTokenVerifier;
import com.saryom.foodservice.config.SecurityConfig;
import com.saryom.foodservice.error.ApiExceptionHandler;
import com.saryom.foodservice.error.NotFoundException;
import com.saryom.foodservice.service.FoodService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
@ActiveProfiles("dev")
@Import({SecurityConfig.class, FirebaseAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class, StubTokenVerifier.class, ApiExceptionHandler.class})
class FoodControllerTest {

    private static final String TOKEN = "Bearer dev:alice";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FoodService foodService;

    @Test
    void browseIsPublic() throws Exception {
        when(foodService.browse(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/food"))
                .andExpect(status().isOk());
    }

    @Test
    void mineRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/food/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detailMaps404() throws Exception {
        UUID id = UUID.randomUUID();
        when(foodService.getDetail(eq(id), any())).thenThrow(new NotFoundException("No food post " + id));

        mvc.perform(get("/api/food/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        String body = """
                {"title":"Bread","quantity":"2 loaves","foodType":"BAKERY"}
                """;
        mvc.perform(post("/api/food").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRejectsInvalidBody() throws Exception {
        String body = """
                {"title":"","quantity":"2 loaves","foodType":"BAKERY"}
                """;
        mvc.perform(post("/api/food")
                        .header("Authorization", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"));
    }
}
