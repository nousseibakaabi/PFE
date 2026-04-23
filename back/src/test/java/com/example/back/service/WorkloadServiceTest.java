package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.repository.*;
import com.example.back.payload.response.MailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkloadServiceTest {

    @Mock
    private WorkloadRepository workloadRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;


    @Mock
    private HistoryService historyService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WorkloadService workloadService;

    private User testChef;
    private User testAdmin;
    private User testSystemUser;
    private Application testApplication;
    private Workload testWorkload;
    private Convention testConvention;

    @BeforeEach
    void setUp() {
        // Setup test chef
        testChef = new User();
        testChef.setId(1L);
        testChef.setUsername("chef");
        testChef.setEmail("chef@example.com");
        testChef.setFirstName("Chef");
        testChef.setLastName("Projet");
        testChef.setPhone("+21612345678");
        testChef.setNotifMode("email");

        Role chefRole = new Role(ERole.ROLE_CHEF_PROJET);
        Set<Role> roles = new HashSet<>();
        roles.add(chefRole);
        testChef.setRoles(roles);

        // Setup test admin
        testAdmin = new User();
        testAdmin.setId(99L);
        testAdmin.setUsername("admin");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("User");
        Role adminRole = new Role(ERole.ROLE_ADMIN);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        testAdmin.setRoles(adminRoles);

        // Setup test system user
        testSystemUser = new User();
        testSystemUser.setId(100L);
        testSystemUser.setUsername("system");
        testSystemUser.setEmail("system@example.com");
        testSystemUser.setFirstName("System");
        testSystemUser.setLastName("User");
        testSystemUser.setRoles(adminRoles);

        // Setup test convention
        testConvention = new Convention();
        testConvention.setId(1L);
        testConvention.setMontantTTC(BigDecimal.valueOf(100000));
        testConvention.setArchived(false);

        // Setup test application
        testApplication = new Application();
        testApplication.setId(1L);
        testApplication.setCode("APP-2024-001");
        testApplication.setName("Test Application");
        testApplication.setDateDebut(LocalDate.now());
        testApplication.setDateFin(LocalDate.now().plusMonths(6));
        testApplication.setConventions(Arrays.asList(testConvention));

        // Setup test workload
        testWorkload = new Workload();
        testWorkload.setId(1L);
        testWorkload.setChefDeProjet(testChef);
        testWorkload.setCurrentWorkloadScore(30.0);
        testWorkload.setCurrentApplicationsCount(2);
        testWorkload.setTotalApplicationsValue(200000.0);
        testWorkload.setTotalApplicationsDuration(180L);
        testWorkload.setLastCalculatedAt(LocalDateTime.now());
    }

    private void setupAuthenticationForUser(User user) {
        when(authentication.getName()).thenReturn(user.getUsername());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    private void setupSystemUser() {
        when(userRepository.findByUsername("system")).thenReturn(Optional.of(testSystemUser));
    }

    @Test
    void initializeWorkload_NewChef_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(workloadRepository.findByChefDeProjet(testChef)).thenReturn(Optional.empty());
        when(applicationRepository.findByChefDeProjet(testChef)).thenReturn(Arrays.asList(testApplication));
        when(workloadRepository.save(any(Workload.class))).thenAnswer(invocation -> {
            Workload saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // When
        Workload result = workloadService.initializeWorkload(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getChefDeProjet()).isEqualTo(testChef);
        verify(workloadRepository).save(any(Workload.class));
    }

    @Test
    void initializeWorkload_ExistingChef_Updates() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(workloadRepository.findByChefDeProjet(testChef)).thenReturn(Optional.of(testWorkload));
        when(applicationRepository.findByChefDeProjet(testChef)).thenReturn(Arrays.asList(testApplication));
        when(workloadRepository.save(any(Workload.class))).thenReturn(testWorkload);

        // When
        Workload result = workloadService.initializeWorkload(1L);

        // Then
        assertThat(result).isNotNull();
        verify(workloadRepository).save(any(Workload.class));
    }

    @Test
    void initializeWorkload_UserNotChef_ThrowsException() {
        // Given
        User nonChef = new User();
        nonChef.setId(2L);
        nonChef.setUsername("user");

        when(userRepository.findById(2L)).thenReturn(Optional.of(nonChef));

        // When & Then
        assertThatThrownBy(() -> workloadService.initializeWorkload(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User is not a Chef de Projet");
    }

    @Test
    void checkAssignment_WithinThresholds_ReturnsCanAssign() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(workloadRepository.findByChefDeProjet(testChef)).thenReturn(Optional.of(testWorkload));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.findByChefDeProjet(testChef)).thenReturn(Arrays.asList(testApplication));
        when(workloadRepository.save(any(Workload.class))).thenReturn(testWorkload);

        // When
        WorkloadService.AssignmentCheck result = workloadService.checkAssignment(1L, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isCanAssign()).isTrue();
    }


    @Test
    void assignApplication_Success() {
        // Given
        setupAuthenticationForUser(testAdmin);
        setupSystemUser();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(workloadRepository.findByChefDeProjet(testChef)).thenReturn(Optional.of(testWorkload));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.findByChefDeProjet(testChef)).thenReturn(Arrays.asList(testApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(testApplication);
        when(workloadRepository.save(any(Workload.class))).thenReturn(testWorkload);

        MailResponse mockResponse = new MailResponse();
        mockResponse.setId(1L);
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        doNothing().when(historyService).logApplicationAssignChef(any(), any(), any(), any());

        // When
        WorkloadService.AssignmentResult result = workloadService.assignApplication(1L, 1L, false);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isBlocked()).isFalse();
        verify(applicationRepository).save(any(Application.class));
    }


    @Test
    void getWorkloadDashboard_Success() {
        // Given
        // No authentication needed for this test - don't call setupAuthentication
        List<User> allChefs = Arrays.asList(testChef);
        List<Workload> allWorkloads = Arrays.asList(testWorkload);

        when(userRepository.findAll()).thenReturn(allChefs);
        when(userRepository.findById(testChef.getId())).thenReturn(Optional.of(testChef));
        when(workloadRepository.findByChefDeProjet(testChef)).thenReturn(Optional.of(testWorkload));
        when(applicationRepository.findByChefDeProjet(testChef)).thenReturn(Arrays.asList(testApplication));
        when(workloadRepository.save(any(Workload.class))).thenReturn(testWorkload);
        when(workloadRepository.findAllOrderedByWorkload()).thenReturn(allWorkloads);

        // When
        WorkloadService.WorkloadDashboard result = workloadService.getWorkloadDashboard();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalChefs()).isEqualTo(1);
        assertThat(result.getWorkloads()).hasSize(1);
    }




    @Test
    void assignApplication_Blocked_Forced_Success() {
        // Given
        setupAuthenticationForUser(testAdmin);
        setupSystemUser();

        // Create a workload that will definitely exceed 75%
        Workload highWorkload = new Workload();
        highWorkload.setId(2L);
        highWorkload.setChefDeProjet(testChef);
        // Set max values to force >75% projection
        highWorkload.setCurrentWorkloadScore(75.0);
        highWorkload.setCurrentApplicationsCount(5);
        highWorkload.setTotalApplicationsValue(4000000.0);
        highWorkload.setTotalApplicationsDuration(600L);

        // Set up the application to have values that will push the workload over
        // Create a test application with higher values
        Application largeApplication = new Application();
        largeApplication.setId(2L);
        largeApplication.setCode("APP-2024-002");
        largeApplication.setName("Large Test Application");
        largeApplication.setDateDebut(LocalDate.now());
        largeApplication.setDateFin(LocalDate.now().plusMonths(12)); // 365 days
        largeApplication.setConventions(Arrays.asList(testConvention));

        // Add convention with larger value
        Convention largeConvention = new Convention();
        largeConvention.setId(2L);
        largeConvention.setMontantTTC(BigDecimal.valueOf(1500000)); // 1.5M
        largeConvention.setArchived(false);
        largeApplication.setConventions(Arrays.asList(largeConvention));

        List<Application> multipleApps = Arrays.asList(
                testApplication, testApplication, testApplication, testApplication, testApplication
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testChef));
        when(workloadRepository.findByChefDeProjet(testChef)).thenReturn(Optional.of(highWorkload));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(largeApplication));
        when(applicationRepository.findByChefDeProjet(testChef)).thenReturn(multipleApps);
        when(applicationRepository.save(any(Application.class))).thenReturn(largeApplication);
        when(workloadRepository.save(any(Workload.class))).thenReturn(highWorkload);

        MailResponse mockResponse = new MailResponse();
        mockResponse.setId(1L);
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        doNothing().when(historyService).logApplicationAssignChef(any(), any(), any(), any());

        // When - forced
        WorkloadService.AssignmentResult result = workloadService.assignApplication(1L, 1L, true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isForced()).isTrue();
        assertThat(result.isBlocked()).isFalse();
        verify(applicationRepository).save(any(Application.class));
    }




}