package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.ApplicationRequest;
import com.example.back.payload.response.ApplicationResponse;
import com.example.back.repository.*;
import com.example.back.service.mapper.ApplicationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private HistoryService historyService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ApplicationService applicationService;

    private User testAdmin;
    private User testChef;
    private Application testApplication;
    private ApplicationRequest testRequest;
    private ApplicationResponse testResponse;

    @BeforeEach
    void setUp() {
        // Setup test admin
        testAdmin = new User();
        testAdmin.setId(1L);
        testAdmin.setUsername("admin");
        testAdmin.setEmail("admin@example.com");
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("User");
        Role adminRole = new Role(ERole.ROLE_ADMIN);
        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);
        testAdmin.setRoles(adminRoles);

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
        testApplication.setDescription("Test Description");
        testApplication.setClientName("Test Client");
        testApplication.setClientEmail("client@example.com");
        testApplication.setClientPhone("123456789");
        testApplication.setDateDebut(LocalDate.now());
        testApplication.setDateFin(LocalDate.now().plusMonths(6));
        testApplication.setMinUser(5L);
        testApplication.setMaxUser(20L);
        testApplication.setStatus("PLANIFIE");
        testApplication.setCreatedBy(testAdmin);
        testApplication.setChefDeProjet(testChef);
        testApplication.setCreatedAt(LocalDateTime.now());
        testApplication.setUpdatedAt(LocalDateTime.now());

        // Setup test request
        testRequest = new ApplicationRequest();
        testRequest.setCode("APP-2024-001");
        testRequest.setName("Test Application");
        testRequest.setDescription("Test Description");
        testRequest.setClientName("Test Client");
        testRequest.setClientEmail("client@example.com");
        testRequest.setClientPhone("123456789");
        testRequest.setDateDebut(LocalDate.now());
        testRequest.setDateFin(LocalDate.now().plusMonths(6));
        testRequest.setMinUser(5L);
        testRequest.setMaxUser(20L);
        testRequest.setChefDeProjetId(2L);
        testRequest.setStatus("PLANIFIE");

        // Setup test response
        testResponse = new ApplicationResponse();
        testResponse.setId(1L);
        testResponse.setCode("APP-2024-001");
        testResponse.setName("Test Application");
    }

    private void mockAuthentication(User user, String role) {
        when(authentication.getName()).thenReturn(user.getUsername());
        when(authentication.getAuthorities()).thenAnswer(invocation -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(() -> role);
            return authorities;
        });
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }



    @Test
    void updateApplication_Success_AsChefDeProjet_OwnApplication() {
        // Given
        // Create application owned by the chef
        Application chefApplication = new Application();
        chefApplication.setId(2L);
        chefApplication.setCode("APP-2024-002");
        chefApplication.setName("Chef Application");
        chefApplication.setChefDeProjet(testChef);
        chefApplication.setCreatedBy(testChef);

        mockAuthentication(testChef, "ROLE_CHEF_PROJET");
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(testChef));
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(chefApplication));
        when(applicationRepository.save(any(Application.class))).thenReturn(chefApplication);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(testResponse);

        // When
        ApplicationResponse result = applicationService.updateApplication(2L, testRequest);

        // Then
        assertThat(result).isNotNull();
        verify(applicationRepository).save(any(Application.class));
    }


    @Test
    void updateApplication_AsChefDeProjet_TryingToChangeChef_ThrowsException() {
        // Given
        ApplicationRequest requestWithDifferentChef = new ApplicationRequest();
        requestWithDifferentChef.setChefDeProjetId(5L);
        requestWithDifferentChef.setCode(testRequest.getCode());
        requestWithDifferentChef.setName(testRequest.getName());

        Application chefApplication = new Application();
        chefApplication.setId(2L);
        chefApplication.setCode("APP-2024-002");
        chefApplication.setName("Chef Application");
        chefApplication.setChefDeProjet(testChef);
        chefApplication.setCreatedBy(testChef);

        mockAuthentication(testChef, "ROLE_CHEF_PROJET");
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(testChef));
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(chefApplication));

        // When & Then
        assertThatThrownBy(() -> applicationService.updateApplication(2L, requestWithDifferentChef))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied: You cannot change the chef de projet assignment");
    }

    @Test
    void updateApplication_ApplicationNotFound_ThrowsException() {
        // Given
        mockAuthentication(testAdmin, "ROLE_ADMIN");
        when(applicationRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> applicationService.updateApplication(1L, testRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Application not found");
    }

    // ==================== DELETE APPLICATION TESTS ====================

    @Test
    void deleteApplication_Success_AsAdmin() {
        // Given
        mockAuthentication(testAdmin, "ROLE_ADMIN");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testAdmin));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.hasConventions(1L)).thenReturn(false);
        doNothing().when(applicationRepository).delete(testApplication);

        // When
        applicationService.deleteApplication(1L);

        // Then
        verify(applicationRepository).delete(testApplication);
        verify(historyService).logApplicationDelete(any(Application.class), eq(testAdmin));
    }

    @Test
    void deleteApplication_Success_AsChefDeProjet_OwnApplication() {
        // Given
        Application chefApplication = new Application();
        chefApplication.setId(2L);
        chefApplication.setCode("APP-2024-002");
        chefApplication.setName("Chef Application");
        chefApplication.setChefDeProjet(testChef);
        chefApplication.setCreatedBy(testChef);

        mockAuthentication(testChef, "ROLE_CHEF_PROJET");
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(testChef));
        when(applicationRepository.findById(2L)).thenReturn(Optional.of(chefApplication));
        when(applicationRepository.hasConventions(2L)).thenReturn(false);
        doNothing().when(applicationRepository).delete(chefApplication);

        // When
        applicationService.deleteApplication(2L);

        // Then
        verify(applicationRepository).delete(chefApplication);
        verify(historyService).logApplicationDelete(any(Application.class), eq(testChef));
    }


    @Test
    void deleteApplication_HasConventions_ThrowsException() {
        // Given
        mockAuthentication(testAdmin, "ROLE_ADMIN");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationRepository.hasConventions(1L)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> applicationService.deleteApplication(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot delete application that has conventions");

        verify(applicationRepository, never()).delete(any(Application.class));
    }

    // ==================== GET APPLICATION TESTS ====================

    @Test
    void getApplicationById_Success_AsAdmin() {
        // Given
        mockAuthentication(testAdmin, "ROLE_ADMIN");
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationMapper.toResponse(testApplication)).thenReturn(testResponse);

        // When
        ApplicationResponse result = applicationService.getApplicationById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getApplicationById_AsChefDeProjet_OwnApplication() {
        // Given
        mockAuthentication(testChef, "ROLE_CHEF_PROJET");
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(testChef));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));
        when(applicationMapper.toResponse(testApplication)).thenReturn(testResponse);

        // When
        ApplicationResponse result = applicationService.getApplicationById(1L);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    void getApplicationById_AsChefDeProjet_NotOwnApplication_ThrowsException() {
        // Given
        User otherChef = new User();
        otherChef.setId(3L);
        otherChef.setUsername("otherchef");
        Role chefRole = new Role(ERole.ROLE_CHEF_PROJET);
        Set<Role> chefRoles = new HashSet<>();
        chefRoles.add(chefRole);
        otherChef.setRoles(chefRoles);

        mockAuthentication(otherChef, "ROLE_CHEF_PROJET");
        when(userRepository.findByUsername("otherchef")).thenReturn(Optional.of(otherChef));
        when(applicationRepository.findById(1L)).thenReturn(Optional.of(testApplication));

        // When & Then
        assertThatThrownBy(() -> applicationService.getApplicationById(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Access denied: You can only view your own applications");
    }

    // ==================== GET ALL APPLICATIONS TESTS ====================

    @Test
    void getAllApplications_AsAdmin() {
        // Given
        List<Application> applications = Collections.singletonList(testApplication);

        mockAuthentication(testAdmin, "ROLE_ADMIN");
        when(applicationRepository.findByArchivedFalse()).thenReturn(applications);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(testResponse);

        // When
        List<ApplicationResponse> result = applicationService.getAllApplications();

        // Then
        assertThat(result).hasSize(1);
        verify(applicationRepository).findByArchivedFalse();
    }

    @Test
    void getAllApplications_AsChefDeProjet() {
        // Given
        List<Application> applications = Collections.singletonList(testApplication);

        mockAuthentication(testChef, "ROLE_CHEF_PROJET");
        when(userRepository.findByUsername("chef")).thenReturn(Optional.of(testChef));
        when(applicationRepository.findByChefDeProjetAndArchivedFalse(testChef))
                .thenReturn(applications);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(testResponse);

        // When
        List<ApplicationResponse> result = applicationService.getAllApplications();

        // Then
        assertThat(result).hasSize(1);
        verify(applicationRepository).findByChefDeProjetAndArchivedFalse(testChef);
    }




    // ==================== OTHER TESTS ====================

    @Test
    void getUnassignedApplications_Success() {
        // Given
        List<Application> unassignedApps = Collections.singletonList(testApplication);

        when(applicationRepository.findByChefDeProjetIsNull()).thenReturn(unassignedApps);
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(testResponse);

        // When
        List<ApplicationResponse> result = applicationService.getUnassignedApplications();

        // Then
        assertThat(result).hasSize(1);
        verify(applicationRepository).findByChefDeProjetIsNull();
    }

    @Test
    void generateSuggestedApplicationCode_FirstOfYear() {
        // Given
        when(applicationRepository.findUsedSequencesByYear(String.valueOf(LocalDate.now().getYear())))
                .thenReturn(null);

        // When
        String result = applicationService.generateSuggestedApplicationCode();

        // Then
        assertThat(result).startsWith("APP-" + LocalDate.now().getYear() + "-001");
        verify(applicationRepository).findUsedSequencesByYear(anyString());
    }

    @Test
    void generateSuggestedApplicationCode_WithExistingSequences() {
        // Given
        List<Integer> usedSequences = Arrays.asList(1, 2, 3);
        when(applicationRepository.findUsedSequencesByYear(String.valueOf(LocalDate.now().getYear())))
                .thenReturn(usedSequences);

        // When
        String result = applicationService.generateSuggestedApplicationCode();

        // Then
        assertThat(result).startsWith("APP-" + LocalDate.now().getYear() + "-004");
        verify(applicationRepository).findUsedSequencesByYear(anyString());
    }

}