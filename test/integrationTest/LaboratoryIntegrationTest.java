package com.UAIC.ISMA.integration;

import com.UAIC.ISMA.dto.LaboratoryDTO;
import com.UAIC.ISMA.entity.Laboratory;
import com.UAIC.ISMA.mapper.LaboratoryMapper;
import com.UAIC.ISMA.repository.LaboratoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(authorities = "ADMIN")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LaboratoryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @BeforeEach
    void setup() {
        laboratoryRepository.deleteAll();
        laboratoryRepository.save(new Laboratory(1L, "Physics Lab", "Advanced Physics Lab", "Building A"));
        laboratoryRepository.save(new Laboratory(2L, "Chemistry Lab", "Organic Chemistry", "Building B"));
        laboratoryRepository.save(new Laboratory(3L, "Math Lab", "Algebra focus", "Building A"));
    }

    @AfterEach
    void tearDown() {
        laboratoryRepository.deleteAll();
    }

    @Test
    void shouldReturnAllLaboratories() throws Exception {
        Laboratory lab = new Laboratory();
        lab.setLabName("Physics Lab");
        lab.setLocation("Building B");
        laboratoryRepository.save(lab);

        mockMvc.perform(get("/laboratories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].labName").value("Physics Lab"));
    }

    @Test
    void shouldCreateNewLaboratory() throws Exception {
        LaboratoryDTO dto = new LaboratoryDTO();
        dto.setLabName("Chemistry Lab");
        dto.setLocation("Building C");

        mockMvc.perform(post("/laboratories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.labName").value("Chemistry Lab"));
    }

    @Test
    void shouldUpdateLaboratory() throws Exception {
        Laboratory lab = new Laboratory();
        lab.setLabName("Old Name");
        lab.setLocation("Building D");
        Laboratory saved = laboratoryRepository.save(lab);

        LaboratoryDTO updateDto = LaboratoryMapper.convertToDTO(saved);
        updateDto.setLabName("Updated Lab");

        mockMvc.perform(put("/laboratories/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labName").value("Updated Lab"));
    }

    @Test
    void shouldDeleteLaboratory() throws Exception {
        Laboratory lab = new Laboratory();
        lab.setLabName("To Delete");
        lab.setLocation("Building E");
        Laboratory saved = laboratoryRepository.save(lab);

        mockMvc.perform(delete("/laboratories/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertFalse(laboratoryRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void shouldNotUpdateNonExistingLaboratory() throws Exception {
        LaboratoryDTO updateDto = new LaboratoryDTO();
        updateDto.setLabName("Ghost Lab");
        updateDto.setLocation("Nowhere");

        mockMvc.perform(put("/laboratories/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Laboratory not found with id 999999"));
    }

    @Test
    void shouldNotDeleteNonExistingLaboratory() throws Exception {
        mockMvc.perform(delete("/laboratories/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Laboratory not found with id 999999"));
    }

    @Test
    void shouldFailToCreateInvalidLaboratory() throws Exception {
        LaboratoryDTO invalidDto = new LaboratoryDTO();

        mockMvc.perform(post("/laboratories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("labName: Name must not be blank"),
                        org.hamcrest.Matchers.containsString("location: Location must not be blank")
                )));
    }

    @Test
    void shouldFailToUpdateWithInvalidData() throws Exception {
        Laboratory lab = new Laboratory();
        lab.setLabName("Initial Lab");
        lab.setLocation("Building F");
        Laboratory saved = laboratoryRepository.save(lab);

        LaboratoryDTO invalidDto = new LaboratoryDTO();
        invalidDto.setId(saved.getId());

        mockMvc.perform(put("/laboratories/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("labName: Name must not be blank"),
                        org.hamcrest.Matchers.containsString("location: Location must not be blank")
                )));
    }

    @Test
    void shouldReturnLabsByName() throws Exception {
        mockMvc.perform(get("/laboratories/search")
                        .param("name", "Physics")
                        .param("location", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].labName").value("Physics Lab"));
    }

    @Test
    void shouldReturnLabsByLocation() throws Exception {
        mockMvc.perform(get("/laboratories/search")
                        .param("location", "Building B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].labName").value("Chemistry Lab"));
    }

    @Test
    void shouldReturnLabsByNameAndLocation() throws Exception {
        mockMvc.perform(get("/laboratories/search")
                        .param("name", "Math")
                        .param("location", "Building A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].labName").value("Math Lab"));
    }

    @Test
    void shouldReturnEmptyIfNoMatch() throws Exception {
        mockMvc.perform(get("/laboratories/search")
                        .param("name", "Bio")
                        .param("location", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void shouldFailOnInvalidLocation() throws Exception {
        mockMvc.perform(get("/laboratories/search")
                        .param("location", "@invalid*"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid location format")));
    }

    @Test
    void shouldFailOnBlankName() throws Exception {
        mockMvc.perform(get("/laboratories/search")
                        .param("name", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid lab name")));
    }

}
