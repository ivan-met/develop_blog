package met.ivan.devblog.controller;

import tools.jackson.databind.ObjectMapper;
import met.ivan.devblog.config.AppProperties;
import met.ivan.devblog.config.SecurityConfig;
import met.ivan.devblog.dto.CategoryResponse;
import met.ivan.devblog.dto.CreateCategoryRequest;
import met.ivan.devblog.dto.UpdateCategoryRequest;
import met.ivan.devblog.exception.ConflictException;
import met.ivan.devblog.security.CustomUserDetailsService;
import met.ivan.devblog.security.JwtAuthenticationFilter;
import met.ivan.devblog.security.JwtService;
import met.ivan.devblog.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AdminCategoryController.class, GlobalExceptionHandler.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AppProperties.class})
@ActiveProfiles("test")
@DisplayName("AdminCategoryController")
class AdminCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    private CategoryResponse sampleCategory() {
        return new CategoryResponse(1L, "Java", "java", "Java desc");
    }

    // --- Authorization matrix ---

    @Test
    @DisplayName("POST /api/admin/categories - anonymous returns 401")
    void create_anonymous_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Java", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/admin/categories - USER role returns 403")
    @WithMockUser(roles = "USER")
    void create_userRole_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Java", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/admin/categories - ADMIN role returns 201")
    @WithMockUser(roles = "ADMIN")
    void create_adminRole_returns201() throws Exception {
        when(categoryService.create(any())).thenReturn(sampleCategory());

        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCategoryRequest("Java", "Java desc"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Java"))
                .andExpect(jsonPath("$.slug").value("java"));
    }

    @Test
    @DisplayName("POST /api/admin/categories - missing name returns 400")
    @WithMockUser(roles = "ADMIN")
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/admin/categories/{id} - USER role returns 403")
    @WithMockUser(roles = "USER")
    void update_userRole_returns403() throws Exception {
        mockMvc.perform(put("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequest("Java", null))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/admin/categories/{id} - ADMIN role returns 200")
    @WithMockUser(roles = "ADMIN")
    void update_adminRole_returns200() throws Exception {
        when(categoryService.update(eq(1L), any())).thenReturn(sampleCategory());

        mockMvc.perform(put("/api/admin/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateCategoryRequest("Java", "desc"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/admin/categories/{id} - ADMIN role returns 204")
    @WithMockUser(roles = "ADMIN")
    void delete_adminRole_returns204() throws Exception {
        mockMvc.perform(delete("/api/admin/categories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/admin/categories/{id} - returns 409 when category is referenced")
    @WithMockUser(roles = "ADMIN")
    void delete_referencedCategory_returns409() throws Exception {
        doThrow(new ConflictException("Category is referenced by existing posts"))
                .when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/admin/categories/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Category is referenced by existing posts"));
    }
}
