package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.dto.request.UpdateAddressRequest;
import bf.backend.employee_service.dto.request.UpdateContactRequest;
import bf.backend.employee_service.dto.request.UpdateNameRequest;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.UnauthorizedActionException;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.OnboardingApplicationRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.repository.VisaStatusRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeProfileServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final String EMPLOYEE_ID = "10";

    @Mock private EmployeeRepository employeeRepository;
    @Mock private VisaStatusRepository visaStatusRepository;
    @Mock private PersonalDocumentRepository personalDocumentRepository;
    @Mock private OnboardingApplicationRepository onboardingApplicationRepository;

    @InjectMocks
    private EmployeeProfileServiceImpl service;

    private Employee employee;

    @BeforeEach
    void setUp() {
        setAuth(USER_ID, "EMPLOYEE");
        employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        employee.setUserId(USER_ID);
        employee.setFirstName("Alice");
        employee.setLastName("Smith");
        employee.setEmail("alice@example.com");
        employee.setCitizenshipStatus(CitizenshipStatus.CITIZEN);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProfile_returnsProfileForCurrentUser() {
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(employee));

        var result = service.getMyProfile();

        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Alice");
    }

    @Test
    void getMyProfile_throwsWhenEmployeeNotFound() {
        when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyProfile())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getProfileById_allowsOwnerAccess() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        var result = service.getProfileById(EMPLOYEE_ID);

        assertThat(result.lastName()).isEqualTo("Smith");
    }

    @Test
    void getProfileById_throwsForDifferentEmployee() {
        Employee other = new Employee();
        other.setId("99");
        other.setUserId(99L);
        other.setFirstName("Bob");
        other.setLastName("Jones");
        other.setEmail("bob@example.com");
        other.setCitizenshipStatus(CitizenshipStatus.CITIZEN);

        when(employeeRepository.findById("99")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getProfileById("99"))
                .isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void getProfileById_hrCanAccessAnyProfile() {
        setAuth(USER_ID, "HR");
        Employee other = new Employee();
        other.setId("99");
        other.setUserId(99L);
        other.setFirstName("Bob");
        other.setLastName("Jones");
        other.setEmail("bob@example.com");
        other.setCitizenshipStatus(CitizenshipStatus.CITIZEN);

        when(employeeRepository.findById("99")).thenReturn(Optional.of(other));

        var result = service.getProfileById("99");

        assertThat(result.firstName()).isEqualTo("Bob");
    }

    @Test
    void getProfileById_throwsWhenEmployeeNotFound() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getProfileById(EMPLOYEE_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateName_updatesAndSaves() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);

        var req = new UpdateNameRequest("Carol", "Smith", null, "C");
        var result = service.updateName(EMPLOYEE_ID, req);

        assertThat(result.firstName()).isEqualTo("Carol");
        verify(employeeRepository).save(employee);
    }

    @Test
    void updateAddress_createsNewAddressWhenNoneMatches() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);

        var req = new UpdateAddressRequest(AddressType.PRIMARY, "123 Main St", null, "City", "CA", "90210");
        service.updateAddress(EMPLOYEE_ID, req);

        assertThat(employee.getAddresses()).hasSize(1);
        assertThat(employee.getAddresses().get(0).getAddressLine1()).isEqualTo("123 Main St");
    }

    @Test
    void updateAddress_updatesExistingMatchingAddress() {
        Address existing = new Address();
        existing.setType(AddressType.PRIMARY);
        existing.setAddressLine1("Old St");
        employee.addAddress(existing);

        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);

        var req = new UpdateAddressRequest(AddressType.PRIMARY, "New Ave", null, "City", "CA", "90210");
        service.updateAddress(EMPLOYEE_ID, req);

        assertThat(employee.getAddresses()).hasSize(1);
        assertThat(employee.getAddresses().get(0).getAddressLine1()).isEqualTo("New Ave");
    }

    @Test
    void updateContact_createsNewContactWhenNoneMatches() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(employee)).thenReturn(employee);

        var req = new UpdateContactRequest(
                ContactType.EMERGENCY, "Dave", "Doe", null,
                "555-0001", null, "dave@example.com", "Friend", null);
        service.updateContact(EMPLOYEE_ID, req);

        assertThat(employee.getContacts()).hasSize(1);
        assertThat(employee.getContacts().get(0).getFirstName()).isEqualTo("Dave");
    }

    private void setAuth(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userId.toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
