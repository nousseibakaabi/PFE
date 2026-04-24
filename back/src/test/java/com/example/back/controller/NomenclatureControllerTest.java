package com.example.back.controller;

import com.example.back.entity.ZoneGeographique;
import com.example.back.payload.request.NomenclatureRequest;
import com.example.back.repository.ApplicationRepository;
import com.example.back.repository.ConventionRepository;
import com.example.back.repository.FactureRepository;
import com.example.back.repository.StructureRepository;
import com.example.back.repository.ZoneGeographiqueRepository;
import com.example.back.service.EntitySyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NomenclatureControllerTest {

    @Mock
    private EntitySyncService entitySyncService;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ZoneGeographiqueRepository zoneGeographiqueRepository;
    @Mock
    private StructureRepository structureRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private ConventionRepository conventionRepository;

    @InjectMocks
    private NomenclatureController controller;

    @Test
    void getAllZones_returnsRepositoryContent() {
        ZoneGeographique zone = new ZoneGeographique();
        zone.setId(1L);
        zone.setName("North");

        when(zoneGeographiqueRepository.findAll()).thenReturn(List.of(zone));

        ResponseEntity<?> response = controller.getAllZones();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", true)
                .containsEntry("count", 1);
    }

    @Test
    void createZone_whenCodeExists_returnsBadRequest() {
        NomenclatureRequest request = new NomenclatureRequest();
        request.setCode("ZONE-1");
        request.setName("North");

        when(zoneGeographiqueRepository.existsByCode("ZONE-1")).thenReturn(true);

        ResponseEntity<?> response = controller.createZone(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((Map<String, Object>) response.getBody())
                .containsEntry("success", false)
                .containsEntry("message", "Zone with this code already exists");
    }
}
