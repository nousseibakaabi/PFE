package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.response.HistoryResponse;
import com.example.back.repository.HistoryRepository;
import com.example.back.service.mapper.HistoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private HistoryRepository historyRepository;


    @Mock
    private HistoryMapper historyMapper;


    @InjectMocks
    private HistoryService historyService;

    private User testUser;
    private User testAdmin;
    private Application testApplication;
    private Convention testConvention;
    private Facture testFacture;
    private History testHistory;

    @BeforeEach
    void setUp() {
        // Setup test user - ONLY DATA, NO STUBBINGS
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");

        Role userRole = new Role(ERole.ROLE_CHEF_PROJET);
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);

        // Setup test admin
        testAdmin = new User();
        testAdmin.setId(2L);
        testAdmin.setUsername("admin");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("User");
        Role adminRole = new Role(ERole.ROLE_ADMIN);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        testAdmin.setRoles(adminRoles);

        // Setup test application
        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setCode("APP-2024-001");
        testApplication.setName("Test Application");
        testApplication.setDescription("Test Description");
        testApplication.setClientName("Test Client");
        testApplication.setStatus("EN_COURS");
        testApplication.setChefDeProjet(testUser);
        testApplication.setCreatedBy(testAdmin);

        // Setup test convention
        testConvention = new Convention();
        testConvention.setId(1L);
        testConvention.setReferenceConvention("CONV-2024-001");
        testConvention.setLibelle("Test Convention");
        testConvention.setMontantHT(BigDecimal.valueOf(10000));
        testConvention.setTva(BigDecimal.valueOf(19));
        testConvention.setMontantTTC(BigDecimal.valueOf(11900));
        testConvention.setNbUsers(10L);
        testConvention.setEtat("PLANIFIE");
        testConvention.setApplication(testApplication);
        testConvention.setCreatedBy(testUser);
        testConvention.setDateDebut(LocalDate.now()); // Add this to prevent NPE
        testConvention.setDateFin(LocalDate.now().plusMonths(6)); // Add this to prevent NPE

        // Setup test facture
        testFacture = new Facture();
        testFacture.setId(1L);
        testFacture.setNumeroFacture("FACT-2024-001");
        testFacture.setMontantTTC(BigDecimal.valueOf(11900));
        testFacture.setStatutPaiement("NON_PAYE");
        testFacture.setDateEcheance(LocalDate.now().plusDays(30));
        testFacture.setConvention(testConvention);

        // Setup test history
        testHistory = new History();
        testHistory.setId(1L);
        testHistory.setActionType("CREATE");
        testHistory.setEntityType("APPLICATION");
        testHistory.setEntityId(1L);
        testHistory.setEntityCode("APP-2024-001");
        testHistory.setEntityName("Test Application");
        testHistory.setUser(testAdmin);
        testHistory.setDescription("Test description");
        testHistory.setTimestamp(LocalDateTime.now());

    }


    // ==================== USER HISTORY TESTS ====================


    @Test
    void getAllHistory_Success() {
        // Given
        List<History> histories = Arrays.asList(testHistory);
        HistoryResponse response = new HistoryResponse();

        when(historyRepository.findAll()).thenReturn(histories);
        when(historyMapper.toResponse(testHistory)).thenReturn(response);

        // When
        List<HistoryResponse> result = historyService.getAllHistory();

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getRecentHistory_Success() {
        // Given
        List<History> histories = Arrays.asList(testHistory);
        HistoryResponse response = new HistoryResponse();

        when(historyRepository.findRecent(any(PageRequest.class))).thenReturn(histories);
        when(historyMapper.toResponse(testHistory)).thenReturn(response);

        // When
        List<HistoryResponse> result = historyService.getRecentHistory(10);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getHistoryByUser_Success() {
        // Given
        List<History> histories = Arrays.asList(testHistory);
        HistoryResponse response = new HistoryResponse();

        when(historyRepository.findByUserIdOrderByTimestampDesc(1L)).thenReturn(histories);
        when(historyMapper.toResponse(testHistory)).thenReturn(response);

        // When
        List<HistoryResponse> result = historyService.getHistoryByUser(1L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getHistoryByEntity_Success() {
        // Given
        List<History> histories = Arrays.asList(testHistory);
        HistoryResponse response = new HistoryResponse();

        when(historyRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("APPLICATION", 1L))
                .thenReturn(histories);
        when(historyMapper.toResponse(testHistory)).thenReturn(response);

        // When
        List<HistoryResponse> result = historyService.getHistoryByEntity("APPLICATION", 1L);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getHistoryByDate_Success() {
        // Given
        LocalDate today = LocalDate.now();
        List<History> histories = Arrays.asList(testHistory);
        HistoryResponse response = new HistoryResponse();

        when(historyRepository.findByDate(today)).thenReturn(histories);
        when(historyMapper.toResponse(testHistory)).thenReturn(response);

        // When
        List<HistoryResponse> result = historyService.getHistoryByDate(today);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void searchHistory_Success() {
        // Given
        List<History> histories = Arrays.asList(testHistory);
        HistoryResponse response = new HistoryResponse();

        when(historyRepository.searchHistory(any(), any(), any(), any())).thenReturn(histories);
        when(historyMapper.toResponse(testHistory)).thenReturn(response);

        // When
        List<HistoryResponse> result = historyService.searchHistory(
                "APPLICATION", "CREATE", 1L, LocalDate.now());

        // Then
        assertThat(result).hasSize(1);
    }
}