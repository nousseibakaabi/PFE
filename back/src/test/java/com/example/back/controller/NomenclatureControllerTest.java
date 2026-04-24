package com.example.back.controller;

import com.example.back.entity.ZoneGeographique;
import com.example.back.entity.ZoneType;
import com.example.back.entity.Structure;
import com.example.back.payload.request.NomenclatureRequest;
import com.example.back.payload.request.StructureRequest;
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
import java.util.Optional;

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

    @Test
    void remainingZoneEndpoints_areCovered() {
        ZoneGeographique zone = new ZoneGeographique();
        zone.setId(1L);
        zone.setCode("Z1");
        zone.setName("North");
        zone.setType(ZoneType.CUSTOM_ZONE);

        when(zoneGeographiqueRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(zoneGeographiqueRepository.findByType(ZoneType.TUNISIAN_ZONE)).thenReturn(List.of());
        when(zoneGeographiqueRepository.findByType(ZoneType.CUSTOM_ZONE)).thenReturn(List.of(zone));
        when(zoneGeographiqueRepository.existsByCode("Z2")).thenReturn(false);
        when(zoneGeographiqueRepository.existsByName("East")).thenReturn(false);
        when(zoneGeographiqueRepository.save(zone)).thenReturn(zone);
        when(conventionRepository.countByZoneId(1L)).thenReturn(0L);

        NomenclatureRequest update = new NomenclatureRequest();
        update.setCode("Z2");
        update.setName("East");
        update.setDescription("desc");

        assertThat(controller.getZoneById(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.updateZone(1L, update).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getTunisianGovernoratesList().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getZonesByType("custom_zone").getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteZone(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void remainingStructureEndpoints_areCovered() {
        Structure structure = new Structure();
        structure.setId(2L);
        structure.setCode("S1");
        structure.setName("Client");

        when(structureRepository.findAll()).thenReturn(List.of(structure));
        when(structureRepository.findById(2L)).thenReturn(Optional.of(structure));
        when(structureRepository.existsByCode("S2")).thenReturn(false);
        when(structureRepository.existsByName("Client 2")).thenReturn(false);
        when(structureRepository.save(org.mockito.ArgumentMatchers.any(Structure.class))).thenReturn(structure);
        when(structureRepository.findByZoneGeographiqueId(3L)).thenReturn(List.of(structure));
        when(structureRepository.existsById(2L)).thenReturn(true);
        when(conventionRepository.countByStructureResponsableIdOrStructureBeneficielId(2L)).thenReturn(0L);
        when(applicationRepository.count()).thenReturn(1L);
        when(zoneGeographiqueRepository.count()).thenReturn(1L);
        when(structureRepository.findByTypeStructureNot("Client")).thenReturn(List.of(structure));
        when(structureRepository.findByTypeStructure("Client")).thenReturn(List.of(structure));
        when(conventionRepository.count()).thenReturn(0L);
        when(conventionRepository.findByEtat("EN_COURS")).thenReturn(List.of());
        when(conventionRepository.findAll()).thenReturn(List.of());
        when(factureRepository.count()).thenReturn(0L);
        when(factureRepository.findByStatutPaiement("PAYE")).thenReturn(List.of());
        when(factureRepository.findByStatutPaiement("NON_PAYE")).thenReturn(List.of());
        when(factureRepository.findFacturesEnRetard(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        StructureRequest request = new StructureRequest();
        request.setCode("S2");
        request.setName("Client 2");

        assertThat(controller.getAllStructures().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getStructureById(2L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.createStructure(request).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.updateStructure(2L, request).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getStructuresByZone(3L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deleteStructure(2L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getStats().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getCommercialStats().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getStructuresResponsable().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.getStructuresBeneficiel().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.generateClientCode("Client Name").getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
