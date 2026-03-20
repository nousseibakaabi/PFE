package com.example.back.service;

import com.example.back.entity.*;
import com.example.back.payload.request.RequestActionDTO;
import com.example.back.payload.response.MailResponse;
import com.example.back.payload.response.RequestResponse;
import com.example.back.repository.*;
import com.example.back.service.mapper.RequestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ConventionRepository conventionRepository;

    @Mock
    private WorkloadService workloadService;

    @Mock
    private MailService mailService;

    @Mock
    private RequestMapper requestMapper;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private RequestService requestService;

    private User testUser;
    private User testChef;
    private User testAdmin;
    private Application testApplication;
    private Convention testConvention;
    private Request testRequest;
    private RequestActionDTO testActionDTO;

    @BeforeEach
    void setUp() {
        // Setup test user (commercial)
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("commercial");
        testUser.setEmail("commercial@example.com");
        testUser.setFirstName("Commercial");
        testUser.setLastName("User");
        Role commercialRole = new Role(ERole.ROLE_COMMERCIAL_METIER);
        Set<Role> commercialRoles = new HashSet<>();
        commercialRoles.add(commercialRole);
        testUser.setRoles(commercialRoles);

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

        // Setup test admin
        testAdmin = new User();
        testAdmin.setId(3L);
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
        testApplication.setChefDeProjet(testChef);
        testApplication.setCreatedBy(testChef);

        // Setup test convention
        testConvention = new Convention();
        testConvention.setId(1L);
        testConvention.setReferenceConvention("CONV-2024-001");
        testConvention.setApplication(testApplication);
        testConvention.setEtat("TERMINE");

        // Setup test request
        testRequest = new Request();
        testRequest.setId(1L);
        testRequest.setRequestType("RENEWAL_ACCEPTANCE");
        testRequest.setStatus("PENDING");
        testRequest.setRequester(testAdmin);
        testRequest.setTargetUser(testChef);
        testRequest.setApplication(testApplication);
        testRequest.setConvention(testConvention);
        testRequest.setReason("Test reason");
        testRequest.setCreatedAt(LocalDateTime.now());

        // Setup test action DTO
        testActionDTO = new RequestActionDTO();
        testActionDTO.setRequestId(1L);
        testActionDTO.setAction("APPROVE");
        testActionDTO.setReason("Approved");
    }

    private MailResponse createMockMailResponse() {
        MailResponse response = new MailResponse();
        response.setId(1L);
        return response;
    }

    // ==================== SCENARIO 1 TESTS ====================

    @Test
    void sendRenewalNotificationToAdmin_Success() throws IOException {
        // Given
        MailResponse mockResponse = createMockMailResponse();
        when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);


        // When
        requestService.sendRenewalNotificationToAdmin(testApplication, testConvention, testAdmin);

        // Then
        verify(mailService).sendMail(any(), any(), isNull());
    }

    @Test
    void createRenewalAcceptanceRequest_Success() {
        // Given
        MailResponse mockResponse = createMockMailResponse();
        when(requestRepository.save(any(Request.class))).thenReturn(testRequest);
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // No need to mock userRepository.findAll() - admin is passed as parameter

        // When
        Request result = requestService.createRenewalAcceptanceRequest(testConvention, testChef, testAdmin);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestType()).isEqualTo("RENEWAL_ACCEPTANCE");
        verify(requestRepository).save(any(Request.class));
        try {
            verify(mailService).sendMail(any(), any(), isNull());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void sendReassignmentNotificationToChef_Success() {
        // Given
        MailResponse mockResponse = createMockMailResponse();
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // No need to mock userRepository.findAll() - admin is passed as parameter

        // When
        requestService.sendReassignmentNotificationToChef(testChef, testApplication, testConvention, testAdmin);

        // Then
        try {
            verify(mailService).sendMail(any(), any(), isNull());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




    // ==================== SCENARIO 2 TESTS ====================

    @Test
    void sendRenewalNotificationToChef_Success() {
        // Given
        MailResponse mockResponse = createMockMailResponse();
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Mock userRepository.findAll for getAdmin()
        when(userRepository.findAll()).thenReturn(Arrays.asList(testAdmin, testChef, testUser));

        // When
        requestService.sendRenewalNotificationToChef(testChef, testApplication, testConvention);

        // Then
        try {
            verify(mailService).sendMail(any(), any(), isNull());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void createReassignmentSuggestion_Success() {
        // Given
        MailResponse mockResponse = createMockMailResponse();

        // Create a request with the correct type
        Request reassignmentRequest = new Request();
        reassignmentRequest.setId(2L);
        reassignmentRequest.setRequestType("REASSIGNMENT_SUGGESTION");
        reassignmentRequest.setRequester(testChef);
        reassignmentRequest.setTargetUser(testAdmin);
        reassignmentRequest.setApplication(testApplication);
        reassignmentRequest.setConvention(testConvention);

        when(requestRepository.save(any(Request.class))).thenReturn(reassignmentRequest);
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        when(userRepository.findAll()).thenReturn(Arrays.asList(testAdmin, testChef, testUser));

        // When
        Request result = requestService.createReassignmentSuggestion(
                testConvention, testChef, testAdmin, "Too busy", "Consider other chef");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestType()).isEqualTo("REASSIGNMENT_SUGGESTION");
        verify(requestRepository).save(any(Request.class));
    }

    // ==================== REQUEST PROCESSING TESTS ====================

    @Test
    void processRequest_RenewalAcceptance_Approve_Success() {
        // Given
        MailResponse mockResponse = createMockMailResponse();
        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(Request.class))).thenReturn(testRequest);
        when(requestMapper.toResponse(any(Request.class))).thenReturn(new RequestResponse());
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Mock userRepository.findAll for getAdmin()
        when(userRepository.findAll()).thenReturn(Arrays.asList(testAdmin, testChef, testUser));

        // When
        RequestResponse result = requestService.processRequest(testActionDTO, testChef);

        // Then
        assertThat(result).isNotNull();
        verify(requestRepository).save(any(Request.class));
    }

    @Test
    void processRequest_RenewalAcceptance_Deny_Success() {
        // Given
        MailResponse mockResponse = createMockMailResponse();
        testActionDTO.setAction("DENY");
        testActionDTO.setReason("Cannot continue");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));
        when(requestRepository.save(any(Request.class))).thenReturn(testRequest);
        when(requestMapper.toResponse(any(Request.class))).thenReturn(new RequestResponse());
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Mock userRepository.findAll for getAdmin()
        when(userRepository.findAll()).thenReturn(Arrays.asList(testAdmin, testChef, testUser));

        // When
        RequestResponse result = requestService.processRequest(testActionDTO, testChef);

        // Then
        assertThat(result).isNotNull();
        verify(requestRepository).save(any(Request.class));
    }

    @Test
    void processRequest_RenewalAcceptance_Deny_NoReason_ThrowsException() {
        // Given
        testActionDTO.setAction("DENY");
        testActionDTO.setReason(null);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        assertThatThrownBy(() -> requestService.processRequest(testActionDTO, testChef))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Reason is required when denying");
    }

    @Test
    void processRequest_RenewalAcceptance_WrongUser_ThrowsException() {
        // Given
        User wrongUser = new User();
        wrongUser.setId(999L);

        when(requestRepository.findById(1L)).thenReturn(Optional.of(testRequest));

        // When & Then
        assertThatThrownBy(() -> requestService.processRequest(testActionDTO, wrongUser))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You can only respond to your own requests");
    }



    @Test
    void processRequest_ReassignmentSuggestion_NotAdmin_ThrowsException() {
        // Given
        Request suggestionRequest = new Request();
        suggestionRequest.setRequestType("REASSIGNMENT_SUGGESTION");

        when(requestRepository.findById(1L)).thenReturn(Optional.of(suggestionRequest));

        // When & Then
        assertThatThrownBy(() -> requestService.processRequest(testActionDTO, testChef))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Only admin can process reassignment suggestions");
    }

    // ==================== REQUEST RETRIEVAL TESTS ====================

    @Test
    void getUserRequests_AsAdmin() {
        // Given
        List<Request> requests = Arrays.asList(testRequest);
        RequestResponse response = new RequestResponse();

        when(requestRepository.findUserRequests(testAdmin)).thenReturn(requests);
        when(requestMapper.toResponse(testRequest)).thenReturn(response);

        // When
        List<RequestResponse> result = requestService.getUserRequests(testAdmin);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getUserRequests_AsChef() {
        // Given
        List<Request> requests = Arrays.asList(testRequest);
        RequestResponse response = new RequestResponse();

        when(requestRepository.findUserRequests(testChef)).thenReturn(requests);
        when(requestMapper.toResponse(testRequest)).thenReturn(response);

        // When
        List<RequestResponse> result = requestService.getUserRequests(testChef);

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getRequestsByStatus_Success() {
        // Given
        List<Request> requests = Arrays.asList(testRequest);
        RequestResponse response = new RequestResponse();

        when(requestRepository.findByStatus("PENDING")).thenReturn(requests);
        when(requestMapper.toResponse(testRequest)).thenReturn(response);

        // When
        List<RequestResponse> result = requestService.getRequestsByStatus("PENDING");

        // Then
        assertThat(result).hasSize(1);
    }

    // ==================== REASSIGNMENT REQUEST FROM CHEF TESTS ====================

    @Test
    void createReassignmentRequestFromChef_Success() {
        // Given
        MailResponse mockResponse = createMockMailResponse();
        testApplication.setCreatedBy(testChef);

        // Create a request with the correct type
        Request reassignmentRequest = new Request();
        reassignmentRequest.setId(3L);
        reassignmentRequest.setRequestType("REASSIGNMENT_REQUEST_FROM_CHEF");
        reassignmentRequest.setRequester(testChef);
        reassignmentRequest.setTargetUser(testAdmin);
        reassignmentRequest.setApplication(testApplication);
        reassignmentRequest.setConvention(testConvention);
        reassignmentRequest.setRecommendedChef(testAdmin);

        when(requestRepository.save(any(Request.class))).thenReturn(reassignmentRequest);
        try {
            when(mailService.sendMail(any(), any(), isNull())).thenReturn(mockResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        when(userRepository.findAll()).thenReturn(Arrays.asList(testAdmin, testChef, testUser));

        // When
        Request result = requestService.createReassignmentRequestFromChef(
                testApplication, testConvention, testChef, testAdmin, "Too busy", "Recommend other chef");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getRequestType()).isEqualTo("REASSIGNMENT_REQUEST_FROM_CHEF");
        verify(requestRepository).save(any(Request.class));
    }

    @Test
    void createReassignmentRequestFromChef_NotCreator_ThrowsException() {
        // Given
        User otherChef = new User();
        otherChef.setId(99L);
        testApplication.setCreatedBy(testChef);

        // When & Then
        assertThatThrownBy(() -> requestService.createReassignmentRequestFromChef(
                testApplication, testConvention, otherChef, testAdmin, "Reason", null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("You are not the creator of this application");
    }


}