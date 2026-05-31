package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.client.HousingServiceClient;
import bf.backend.employee_service.dto.response.HouseInfo;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.OnboardingApplicationRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.repository.VisaStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrEmployeeServiceImplTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private OnboardingApplicationRepository onboardingApplicationRepository;
    @Mock private VisaStatusRepository visaStatusRepository;
    @Mock private PersonalDocumentRepository personalDocumentRepository;
    @Mock private HousingServiceClient housingServiceClient;

    @InjectMocks
    private HrEmployeeServiceImpl service;

    private Employee employee1;
    private Employee employee2;

    @BeforeEach
    void setUp() {
        employee1 = buildEmployee("10", 1L, "Alice", "Smith");
        employee2 = buildEmployee("20", 2L, "Bob", "Jones");
    }

    // ── listEmployees ────────────────────────────────────────────────────────

    @Test
    void listEmployees_returnsPagedResults() {
        List<Employee> employees = List.of(employee1, employee2);
        Page<Employee> page = new PageImpl<>(employees,
                PageRequest.of(0, 10, Sort.by("id").ascending()), 25L);

        when(employeeRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = service.listEmployees(0);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(25L);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.page()).isEqualTo(0);
    }

    @Test
    void listEmployees_secondPageReturnsCorrectOffset() {
        List<Employee> employees = List.of(employee2);
        Page<Employee> page = new PageImpl<>(employees,
                PageRequest.of(1, 10, Sort.by("id").ascending()), 11L);

        when(employeeRepository.findAll(any(Pageable.class))).thenReturn(page);

        var result = service.listEmployees(1);

        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void listEmployees_emptyPageReturnsEmptyContent() {
        Page<Employee> emptyPage = new PageImpl<>(List.of(),
                PageRequest.of(0, 10, Sort.by("id").ascending()), 0L);

        when(employeeRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        var result = service.listEmployees(0);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0L);
    }

    // ── searchEmployees ───────────────────────────────────────────────────────

    @Test
    void searchEmployees_returnsMatchingEmployees() {
        when(employeeRepository.searchByName(eq("alice"))).thenReturn(List.of(employee1));

        var result = service.searchEmployees("alice");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("Alice");
    }

    @Test
    void searchEmployees_returnsEmptyListWhenNoMatch() {
        when(employeeRepository.searchByName(eq("zzz"))).thenReturn(List.of());

        var result = service.searchEmployees("zzz");

        assertThat(result).isEmpty();
    }

    // ── getApplicationTrackingItems ───────────────────────────────────────────

    @Test
    void getApplicationTrackingItems_combinesOnboardingAndStemOptItems() {
        OnboardingApplication pendingApp = buildOnboardingApp("100", employee1, ApplicationStatus.PENDING);
        VisaStatus stemOptVisa = buildStemOptVisa("200", employee2);

        when(onboardingApplicationRepository.findByStatus(ApplicationStatus.PENDING))
                .thenReturn(List.of(pendingApp));
        when(employeeRepository.findById("10")).thenReturn(Optional.of(employee1));
        when(visaStatusRepository.findByVisaTypeAndActiveFlagTrue(VisaType.F1_OPT_STEM))
                .thenReturn(List.of(stemOptVisa));
        when(personalDocumentRepository.existsByEmployeeIdAndApplicationTypeAndDocumentType(
                "20", "VISA_STEM_OPT", DocumentType.OPT_EAD)).thenReturn(true);
        when(employeeRepository.findById("20")).thenReturn(Optional.of(employee2));

        var result = service.getApplicationTrackingItems();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).applicationType()).isEqualTo("ONBOARDING");
        assertThat(result.get(0).status()).isEqualTo("PENDING");
        assertThat(result.get(1).applicationType()).isEqualTo("STEM_OPT_REVIEW");
        assertThat(result.get(1).status()).isEqualTo("AWAITING_REVIEW");
    }

    @Test
    void getApplicationTrackingItems_returnsEmptyWhenNothingPending() {
        when(onboardingApplicationRepository.findByStatus(ApplicationStatus.PENDING))
                .thenReturn(List.of());
        when(visaStatusRepository.findByVisaTypeAndActiveFlagTrue(VisaType.F1_OPT_STEM))
                .thenReturn(List.of());

        var result = service.getApplicationTrackingItems();

        assertThat(result).isEmpty();
    }

    @Test
    void getApplicationTrackingItems_includesCorrectEmployeeNames() {
        OnboardingApplication pendingApp = buildOnboardingApp("100", employee1, ApplicationStatus.PENDING);

        when(onboardingApplicationRepository.findByStatus(ApplicationStatus.PENDING))
                .thenReturn(List.of(pendingApp));
        when(employeeRepository.findById("10")).thenReturn(Optional.of(employee1));
        when(visaStatusRepository.findByVisaTypeAndActiveFlagTrue(VisaType.F1_OPT_STEM))
                .thenReturn(List.of());

        var result = service.getApplicationTrackingItems();

        assertThat(result.get(0).employeeName()).isEqualTo("Alice Smith");
        assertThat(result.get(0).employeeId()).isEqualTo("10");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @Test
    void getHousingSummary_returnsAllHousesWithResidentCountsAndResidents() {
        employee1.setHouseId(1L);
        employee1.setCellPhone("111-111-1111");
        employee2.setHouseId(1L);
        employee2.setPreferredName("Bobby");
        employee2.setCellPhone("222-222-2222");

        HouseInfo house1 = new HouseInfo(
                1L, "123 Main St", 4, 100L, "Landlord One",
                "landlord1@example.com", "555-111-2222");
        HouseInfo house2 = new HouseInfo(
                2L, "456 Oak Ave", 2, 200L, "Landlord Two",
                "landlord2@example.com", "555-333-4444");

        when(housingServiceClient.getAllHouses()).thenReturn(List.of(house1, house2));
        when(employeeRepository.findByHouseIdIsNotNull()).thenReturn(List.of(employee1, employee2));

        var result = service.getHousingSummary();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).houseId()).isEqualTo(1L);
        assertThat(result.get(0).currentOccupants()).isEqualTo(2);
        assertThat(result.get(0).availableSpots()).isEqualTo(2);
        assertThat(result.get(0).landlordEmail()).isEqualTo("landlord1@example.com");
        assertThat(result.get(0).landlordPhone()).isEqualTo("555-111-2222");
        assertThat(result.get(0).residents()).hasSize(2);
        assertThat(result.get(0).residents().get(1).preferredName()).isEqualTo("Bobby");
        assertThat(result.get(1).houseId()).isEqualTo(2L);
        assertThat(result.get(1).currentOccupants()).isZero();
        assertThat(result.get(1).availableSpots()).isEqualTo(2);
        assertThat(result.get(1).residents()).isEmpty();
    }

    private Employee buildEmployee(String id, Long userId, String firstName, String lastName) {
        Employee e = new Employee();
        e.setId(id);
        e.setUserId(userId);
        e.setFirstName(firstName);
        e.setLastName(lastName);
        e.setEmail(firstName.toLowerCase() + "@example.com");
        e.setCitizenshipStatus(CitizenshipStatus.CITIZEN);
        return e;
    }

    private OnboardingApplication buildOnboardingApp(String id, Employee emp, ApplicationStatus status) {
        OnboardingApplication app = new OnboardingApplication();
        app.setId(id);
        app.setEmployeeId(emp.getId());
        app.setStatus(status);
        app.setUpdatedAt(LocalDateTime.now());
        return app;
    }

    private VisaStatus buildStemOptVisa(String id, Employee emp) {
        VisaStatus vs = new VisaStatus();
        vs.setId(id);
        vs.setEmployeeId(emp.getId());
        vs.setVisaType(VisaType.F1_OPT_STEM);
        vs.setActiveFlag(true);
        return vs;
    }
}
