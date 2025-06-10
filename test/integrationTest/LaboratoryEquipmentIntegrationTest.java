
package com.UAIC.ISMA.integration;

import com.UAIC.ISMA.entity.Equipment;
import com.UAIC.ISMA.entity.Laboratory;
import com.UAIC.ISMA.entity.enums.AvailabilityStatus;
import com.UAIC.ISMA.repository.EquipmentRepository;
import com.UAIC.ISMA.repository.LaboratoryRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(authorities = "ADMIN")
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LaboratoryEquipmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private Laboratory testLab;

    @BeforeEach
    void setUp() {
        equipmentRepository.deleteAll();
        laboratoryRepository.deleteAll();

        testLab = new Laboratory();
        testLab.setLabName("Physics Lab");
        testLab.setLocation("Building A");
        testLab.setDescription("Physics experiments lab");
        testLab = laboratoryRepository.save(testLab);
    }

    @Test
    void shouldCreateLabWithMultipleEquipment() throws Exception {
        Equipment eq1 = new Equipment("Microscope", "INV001", LocalDateTime.now(), AvailabilityStatus.AVAILABLE, testLab);
        Equipment eq2 = new Equipment("Spectrometer", "INV002", LocalDateTime.now(), AvailabilityStatus.IN_USE, testLab);
        equipmentRepository.saveAll(List.of(eq1, eq2));

        mockMvc.perform(get("/laboratories/" + testLab.getId() + "/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Microscope"))
                .andExpect(jsonPath("$[1].name").value("Spectrometer"));
    }

    @Test
    void deletingLabShouldCascadeDeleteEquipment() {
        Equipment eq = new Equipment("Old Tool", "INV999", LocalDateTime.now(), AvailabilityStatus.MAINTENANCE, testLab);
        equipmentRepository.save(eq);

        laboratoryRepository.deleteById(testLab.getId());

        assertFalse(equipmentRepository.findAll().stream()
                .anyMatch(e -> "INV999".equals(e.getInventoryNumber())));
    }

    @Test
    void equipmentShouldBeFilteredByLabId() throws Exception {
        Laboratory otherLab = new Laboratory("Other Lab", "Floor 2", "Chemistry lab");
        otherLab = laboratoryRepository.save(otherLab);

        equipmentRepository.save(new Equipment("Tool A", "INV-A", LocalDateTime.now(), AvailabilityStatus.AVAILABLE, testLab));
        equipmentRepository.save(new Equipment("Tool B", "INV-B", LocalDateTime.now(), AvailabilityStatus.AVAILABLE, otherLab));

        mockMvc.perform(get("/laboratories/" + testLab.getId() + "/equipment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Tool A"));
    }
}
