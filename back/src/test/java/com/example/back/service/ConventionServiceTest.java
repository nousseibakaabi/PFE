package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.ConventionRequest;
import com.example.back.payload.request.RenewalRequestDTO;
import com.example.back.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConventionServiceTest {

    @Mock
    private ConventionRepository conventionRepository;

    @Mock
    private FactureRepository factureRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private StructureRepository structureRepository;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ConventionService conventionService;

    private User testUser;
    private User testChef;
    private Application testApplication;
    private Structure testStructure;
    private Convention testConvention;
    private ConventionRequest testRequest;
    private Facture testFacture;

    @BeforeEach
    void setUp() {
        // Setup test user - ONLY DATA, NO STUBBINGS
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        Role commercialRole = new Role(ERole.ROLE_COMMERCIAL_METIER);
        Set<Role> roles = new HashSet<>();
        roles.add(commercialRole);
        testUser.setRoles(roles);

        // Setup test chef
        testChef = new User();
        testChef.setId(2L);
        testChef.setUsername("chef");
        testChef.setEmail("chef@example.com");
        testChef.setFirstName("Chef");
        testChef.setLastName("Projet");
        Role chefRole = new Role(ERole.ROLE_CHEF_PROJET);
        Set<Role> chefRoles = new HashSet<>();
        chefRoles.add(chefRole);
        testChef.setRoles(chefRoles);

        // Setup test application
        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setCode("APP-2024-001");
        testApplication.setName("Test Application");
        testApplication.setClientName("Test Client");
        testApplication.setMinUser(5L);
        testApplication.setMaxUser(20L);
        testApplication.setChefDeProjet(testChef);
        testApplication.setStatus("TERMINE");

        // Add admin user to the user list for find all calls
        User adminUser = new User();
        adminUser.setId(99L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        Role adminRole = new Role(ERole.ROLE_ADMIN);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        adminUser.setRoles(adminRoles);

        // Setup test structure
        testStructure = new Structure();
        testStructure.setId(1L);
        testStructure.setCode("STR-001");
        testStructure.setName("Test Structure");

        // Setup test convention
        testConvention = new Convention();
        testConvention.setId(1L);
        testConvention.setReferenceConvention("CONV-2024-001");
        testConvention.setReferenceERP("ERP-001");
        testConvention.setLibelle("Test Convention");
        testConvention.setDateDebut(LocalDate.now());
        testConvention.setDateFin(LocalDate.now().plusMonths(6));
        testConvention.setDateSignature(LocalDate.now());
        testConvention.setPeriodicite("MENSUEL");
        testConvention.setMontantHT(BigDecimal.valueOf(10000));
        testConvention.setTva(BigDecimal.valueOf(19));
        testConvention.setMontantTTC(BigDecimal.valueOf(11900));
        testConvention.setNbUsers(10L);
        testConvention.setEtat("PLANIFIE");
        testConvention.setApplication(testApplication);
        testConvention.setStructureResponsable(testStructure);
        testConvention.setStructureBeneficiel(testStructure);
        testConvention.setCreatedBy(testUser);
        testConvention.setCreatedAt(LocalDateTime.now());
        testConvention.setUpdatedAt(LocalDateTime.now());

        // Setup test request
        testRequest = new ConventionRequest();
        testRequest.setReferenceConvention("CONV-2024-001");
        testRequest.setReferenceERP("ERP-001");
        testRequest.setLibelle("Test Convention");
        testRequest.setDateDebut(LocalDate.now());
        testRequest.setDateFin(LocalDate.now().plusMonths(6));
        testRequest.setDateSignature(LocalDate.now());
        testRequest.setPeriodicite("MENSUEL");
        testRequest.setMontantHT(BigDecimal.valueOf(10000));
        testRequest.setTva(BigDecimal.valueOf(19));
        testRequest.setNbUsers(10L);
        testRequest.setApplicationId(1L);
        testRequest.setStructureResponsableId(1L);
        testRequest.setStructureBeneficielId(1L);

        // Setup test facture
        testFacture = new Facture();
        testFacture.setId(1L);
        testFacture.setNumeroFacture("FACT-2024-CONV-2024-001-001");
        testFacture.setMontantTTC(BigDecimal.valueOf(11900));
        testFacture.setStatutPaiement("NON_PAYE");
        testFacture.setDateEcheance(LocalDate.now().plusMonths(1));
        testFacture.setConvention(testConvention);

        // DO NOT stub authentication here - it will be stubbed in individual tests
    }

    private void setupAuthentication(User user, String role) {
        when(authentication.getName()).thenReturn(user.getUsername());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    // ==================== BASIC CALCULATION TESTS ====================

    @Test
    void calculateTTC_Success() {
        // Given
        BigDecimal montantHT = BigDecimal.valueOf(10000);
        BigDecimal tva = BigDecimal.valueOf(19);

        // When
        BigDecimal result = conventionService.calculateTTC(montantHT, tva);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(11900));
    }

    @Test
    void calculateTTC_WithNullTva_UsesDefault() {
        // Given
        BigDecimal montantHT = BigDecimal.valueOf(10000);

        // When
        BigDecimal result = conventionService.calculateTTC(montantHT, null);

        // Then
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(11900));
    }

    @Test
    void determineNbUsers_WithValidSelection() {
        // Given
        Long selectedUsers = 10L;

        // When
        Long result = conventionService.determineNbUsers(selectedUsers, testApplication);

        // Then
        assertThat(result).isEqualTo(10L);
    }

    @Test
    void determineNbUsers_SelectionBelowMin_UsesMin() {
        // Given
        Long selectedUsers = 3L;

        // When
        Long result = conventionService.determineNbUsers(selectedUsers, testApplication);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    void determineNbUsers_NoSelection_UsesMin() {
        // Given
        Long selectedUsers = null;

        // When
        Long result = conventionService.determineNbUsers(selectedUsers, testApplication);

        // Then
        assertThat(result).isEqualTo(5L);
    }

    @Test
    void determineNbUsers_SelectionAboveMax_UsesSelected() {
        // Given
        Long selectedUsers = 25L;

        // When
        Long result = conventionService.determineNbUsers(selectedUsers, testApplication);

        // Then
        assertThat(result).isEqualTo(25L);
    }



    // ==================== UPDATE CONVENTION TESTS ====================

    @Test
    void updateConventionWithFinancials_Success() {
        // Given
        ConventionRequest updateRequest = new ConventionRequest();
        updateRequest.setReferenceConvention("CONV-2024-001-UPDATED");
        updateRequest.setReferenceERP("ERP-001-UPDATED");
        updateRequest.setLibelle("Updated Convention");
        updateRequest.setDateDebut(LocalDate.now().plusMonths(1));
        updateRequest.setDateFin(LocalDate.now().plusMonths(7));
        updateRequest.setDateSignature(LocalDate.now());
        updateRequest.setPeriodicite("TRIMESTRIEL");
        updateRequest.setMontantHT(BigDecimal.valueOf(15000));
        updateRequest.setTva(BigDecimal.valueOf(19));
        updateRequest.setNbUsers(15L);
        updateRequest.setApplicationId(1L);
        updateRequest.setStructureResponsableId(1L);
        updateRequest.setStructureBeneficielId(1L);

        setupAuthentication(testUser, "ROLE_COMMERCIAL_METIER");
        when(conventionRepository.findById(1L)).thenReturn(Optional.of(testConvention));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(structureRepository.findById(1L)).thenReturn(Optional.of(testStructure));
        when(conventionRepository.save(any(Convention.class))).thenReturn(testConvention);
        when(factureRepository.findByConventionIdOrderByDateFacturationAsc(1L)).thenReturn(new ArrayList<>());
        doNothing().when(applicationService).updateApplicationDatesFromConvention(anyLong(), any(), any());
        when(factureRepository.save(any(Facture.class))).thenReturn(testFacture);

        // When
        Convention result = conventionService.updateConventionWithFinancials(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        verify(conventionRepository).save(any(Convention.class));
    }



    // ==================== STATUS UPDATE TESTS ====================

    @Test
    void updateConventionStatusRealTime_Success() {
        // Given
        when(conventionRepository.findById(1L)).thenReturn(Optional.of(testConvention));
        when(conventionRepository.save(any(Convention.class))).thenReturn(testConvention);

        // When
        conventionService.updateConventionStatusRealTime(1L);

        // Then
        verify(conventionRepository).findById(1L);
    }

    @Test
    void updateConventionStatusRealTime_ConventionNotFound() {
        // Given
        when(conventionRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        conventionService.updateConventionStatusRealTime(1L);

        // Then - no exception thrown, just log warning
        verify(conventionRepository, never()).save(any(Convention.class));
    }

    // ==================== REFERENCE GENERATION TESTS ====================

    @Test
    void generateSuggestedReference_FirstOfYear() {
        // Given
        when(conventionRepository.findUsedSequencesByYear(anyString())).thenReturn(null);

        // When
        String result = conventionService.generateSuggestedReference();

        // Then
        assertThat(result).startsWith("CONV-" + LocalDate.now().getYear() + "-001");
    }

    @Test
    void generateSuggestedReference_WithExistingSequences() {
        // Given
        List<Integer> usedSequences = Arrays.asList(1, 2, 3);
        when(conventionRepository.findUsedSequencesByYear(anyString())).thenReturn(usedSequences);

        // When
        String result = conventionService.generateSuggestedReference();

        // Then
        assertThat(result).startsWith("CONV-" + LocalDate.now().getYear() + "-004");
    }

    // ==================== API HELPER TESTS ====================

    @Test
    void calculateTTCResponse_Success() {
        // Given
        BigDecimal montantHT = BigDecimal.valueOf(10000);
        BigDecimal tva = BigDecimal.valueOf(19);

        // When
        Map<String, Object> result = conventionService.calculateTTCResponse(montantHT, tva);

        // Then
        assertThat(result).containsKey("montantHT");
        assertThat(result).containsKey("tva");
        assertThat(result).containsKey("montantTTC");
        assertThat(((BigDecimal) result.get("montantTTC")).compareTo(BigDecimal.valueOf(11900))).isEqualTo(0);
    }

    @Test
    void determineNbUsersResponse_Success() {
        // Given
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When
        Map<String, Object> result = conventionService.determineNbUsersResponse(1L, 10L);

        // Then
        assertThat(result).containsKey("nbUsers");
        assertThat(result).containsKey("minUser");
        assertThat(result).containsKey("maxUser");
        assertThat(result).containsKey("selectedUsers");
        assertThat(result).containsKey("appliedRule");
        assertThat(result.get("nbUsers")).isEqualTo(10L);
    }

    // ==================== RENEWAL TESTS ====================


    @Test
    void renewConvention_NotTerminated_ThrowsException() {
        // Given
        testConvention.setEtat("EN COURS");
        RenewalRequestDTO renewalRequest = new RenewalRequestDTO();

        when(conventionRepository.findById(1L)).thenReturn(Optional.of(testConvention));

        // When & Then
        assertThatThrownBy(() -> conventionService.renewConvention(1L, renewalRequest, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only terminated conventions can be renewed");
    }

    @Test
    void renewConvention_Archived_ThrowsException() {
        // Given
        testConvention.setEtat("TERMINE");
        testConvention.setArchived(true);
        RenewalRequestDTO renewalRequest = new RenewalRequestDTO();

        when(conventionRepository.findById(1L)).thenReturn(Optional.of(testConvention));

        // When & Then
        assertThatThrownBy(() -> conventionService.renewConvention(1L, renewalRequest, testUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Archived conventions cannot be renewed");
    }

    // ==================== SYNC TESTS ====================

    @Test
    void syncAllApplicationDates_Success() {
        // Given
        doNothing().when(applicationService).syncApplicationDatesWithAllConventions(1L);

        // When
        conventionService.syncAllApplicationDates(1L);

        // Then
        verify(applicationService).syncApplicationDatesWithAllConventions(1L);
    }





}