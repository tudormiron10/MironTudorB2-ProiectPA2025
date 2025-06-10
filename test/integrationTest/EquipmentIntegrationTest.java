package com.UAIC.ISMA.integration;

import com.UAIC.ISMA.dto.EquipmentDTO;
import com.UAIC.ISMA.entity.Equipment;
import com.UAIC.ISMA.entity.Laboratory;
import com.UAIC.ISMA.entity.enums.AvailabilityStatus;
import com.UAIC.ISMA.mapper.EquipmentMapper;
import com.UAIC.ISMA.repository.EquipmentRepository;
import com.UAIC.ISMA.repository.LaboratoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;


import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(authorities = "ADMIN")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EquipmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    private Laboratory testLab;

    @BeforeAll
    void setup() {
        laboratoryRepository.deleteAll();
        testLab = new Laboratory();
        testLab.setLabName("Test Lab");
        testLab.setLocation("Building A");
        laboratoryRepository.save(testLab);
    }

    @AfterEach
    void tearDown() {
        equipmentRepository.deleteAll();
    }

    @Test
    void shouldReturnAllEquipment() throws Exception {
        Equipment eq = new Equipment();
        eq.setName("Microscop");
        eq.setInventoryNumber("INV001");
        eq.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        eq.setLaboratory(testLab);
        equipmentRepository.save(eq);

        mockMvc.perform(get("/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Microscop"));
    }

    @Test
    void shouldCreateNewEquipment() throws Exception {
        EquipmentDTO dto = new EquipmentDTO();
        dto.setName("Osciloscop");
        dto.setInventoryNumber("INV002");
        dto.setAvailabilityStatus(AvailabilityStatus.MAINTENANCE);
        dto.setLaboratoryId(testLab.getId());
        dto.setAcquisitionDate(LocalDateTime.now());

        mockMvc.perform(post("/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Osciloscop"));
    }

    @Test
    void shouldUpdateEquipment() throws Exception {
        Equipment eq = new Equipment();
        eq.setName("Test Eq");
        eq.setInventoryNumber("INV003");
        eq.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        eq.setLaboratory(testLab);
        eq.setAcquisitionDate(LocalDateTime.now());
        Equipment saved = equipmentRepository.save(eq);

        EquipmentDTO updateDto = EquipmentMapper.convertToDTO(saved);
        updateDto.setName("Updated Eq");

        mockMvc.perform(put("/equipment/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Eq"));
    }

    @Test
    void shouldDeleteEquipment() throws Exception {
        Equipment eq = new Equipment();
        eq.setName("To Delete");
        eq.setInventoryNumber("INV004");
        eq.setAvailabilityStatus(AvailabilityStatus.IN_USE);
        eq.setLaboratory(testLab);
        Equipment saved = equipmentRepository.save(eq);

        mockMvc.perform(delete("/equipment/" + saved.getId()))
                .andExpect(status().isNoContent());

        assertFalse(equipmentRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void shouldSearchByNameAndStatus() throws Exception {
        Equipment eq = new Equipment();
        eq.setName("Scanner");
        eq.setInventoryNumber("INV005");
        eq.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        eq.setLaboratory(testLab);
        equipmentRepository.save(eq);

        mockMvc.perform(get("/equipment/search")
                        .param("name", "Scan")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Scanner"));
    }

    @Test
    void shouldNotUpdateNonExistingEquipment() throws Exception {
        EquipmentDTO updateDto = new EquipmentDTO();
        updateDto.setName("NonExistent");
        updateDto.setInventoryNumber("INV999");
        updateDto.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        updateDto.setLaboratoryId(testLab.getId());
        updateDto.setAcquisitionDate(LocalDateTime.now());

        mockMvc.perform(put("/equipment/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Equipment not found with ID: 999999"));
    }

    @Test
    void shouldNotDeleteNonExistingEquipment() throws Exception {
        mockMvc.perform(delete("/equipment/999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Equipment not found with ID: 999999"));
    }

    @Test
    void shouldFailToCreateInvalidEquipment() throws Exception {
        EquipmentDTO invalidDto = new EquipmentDTO();

        mockMvc.perform(post("/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("name: Name is required"),
                        org.hamcrest.Matchers.containsString("inventoryNumber: Inventory number is required")
                )));
    }

    @Test
    void shouldFailToUpdateWithInvalidData() throws Exception {
        Equipment eq = new Equipment();
        eq.setName("Valid");
        eq.setInventoryNumber("INVVALID");
        eq.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        eq.setLaboratory(testLab);
        eq.setAcquisitionDate(LocalDateTime.now());
        Equipment saved = equipmentRepository.save(eq);

        EquipmentDTO invalidDto = new EquipmentDTO();
        invalidDto.setId(saved.getId());
        invalidDto.setLaboratoryId(testLab.getId());

        mockMvc.perform(put("/equipment/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.allOf(
                org.hamcrest.Matchers.containsString("name: Name is required"),
                org.hamcrest.Matchers.containsString("inventoryNumber: Inventory number is required"),
                org.hamcrest.Matchers.containsString("availabilityStatus: Availability status must be specified")
        )));

    }

    @Test
    void shouldReturnEmptySearchResultForUnknownCriteria() throws Exception {
        mockMvc.perform(get("/equipment/search")
                        .param("name", "DoesNotExist")
                        .param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

}
