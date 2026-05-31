package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.client.HousingServiceClient;
import bf.backend.employee_service.dto.response.ApplicationTrackingItem;
import bf.backend.employee_service.dto.response.EmployeeSummaryResponse;
import bf.backend.employee_service.dto.response.HouseInfo;
import bf.backend.employee_service.dto.response.HouseOccupancyResponse;
import bf.backend.employee_service.dto.response.HrHouseResidentResponse;
import bf.backend.employee_service.dto.response.HrHouseSummaryResponse;
import bf.backend.employee_service.dto.response.PageResponse;
import bf.backend.employee_service.entity.*;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.exception.ValidationException;
import bf.backend.employee_service.mapper.EmployeeMapper;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.OnboardingApplicationRepository;
import bf.backend.employee_service.repository.PersonalDocumentRepository;
import bf.backend.employee_service.repository.VisaStatusRepository;
import bf.backend.employee_service.service.HrEmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrEmployeeServiceImpl implements HrEmployeeService {

    private static final int PAGE_SIZE = 10;
    private static final String STEM_OPT_APP_TYPE = "VISA_STEM_OPT";

    private final EmployeeRepository employeeRepository;
    private final OnboardingApplicationRepository onboardingApplicationRepository;
    private final VisaStatusRepository visaStatusRepository;
    private final PersonalDocumentRepository personalDocumentRepository;
    private final HousingServiceClient housingServiceClient;

    @Override
    public PageResponse<EmployeeSummaryResponse> listEmployees(int page) {
        Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").ascending());
        Page<EmployeeSummaryResponse> summaries = employeeRepository.findAll(pageable).map(e -> {
            VisaStatus activeVisa = visaStatusRepository
                    .findByEmployeeIdAndActiveFlagTrue(e.getId()).orElse(null);
            return EmployeeMapper.toSummary(e, activeVisa);
        });
        return PageResponse.of(summaries);
    }

    @Override
    public List<EmployeeSummaryResponse> searchEmployees(String query) {
        return employeeRepository.searchByName(query)
                .stream()
                .map(e -> {
                    VisaStatus activeVisa = visaStatusRepository
                            .findByEmployeeIdAndActiveFlagTrue(e.getId()).orElse(null);
                    return EmployeeMapper.toSummary(e, activeVisa);
                })
                .toList();
    }

    @Override
    public List<EmployeeSummaryResponse> getResidentsByHouseId(Long houseId) {
        return employeeRepository.findByHouseId(houseId)
                .stream()
                .map(e -> {
                    VisaStatus activeVisa = visaStatusRepository
                            .findByEmployeeIdAndActiveFlagTrue(e.getId()).orElse(null);
                    return EmployeeMapper.toSummary(e, activeVisa);
                })
                .toList();
    }

    @Override
    public List<ApplicationTrackingItem> getApplicationTrackingItems() {
        List<ApplicationTrackingItem> items = new ArrayList<>();

        onboardingApplicationRepository.findByStatus(ApplicationStatus.PENDING)
                .forEach(app -> {
                    Employee emp = employeeRepository.findById(app.getEmployeeId()).orElse(null);
                    if (emp == null) return;
                    items.add(new ApplicationTrackingItem(
                            app.getId(),
                            emp.getId(),
                            emp.getFirstName() + " " + emp.getLastName(),
                            "ONBOARDING",
                            "PENDING",
                            app.getUpdatedAt()
                    ));
                });

        List<VisaStatus> stemOptVisas = visaStatusRepository
                .findByVisaTypeAndActiveFlagTrue(VisaType.F1_OPT_STEM);
        stemOptVisas.stream()
                .filter(vs -> personalDocumentRepository.existsByEmployeeIdAndApplicationTypeAndDocumentType(
                        vs.getEmployeeId(), STEM_OPT_APP_TYPE, DocumentType.OPT_EAD))
                .forEach(vs -> {
                    Employee emp = employeeRepository.findById(vs.getEmployeeId()).orElse(null);
                    if (emp == null) return;
                    items.add(new ApplicationTrackingItem(
                            vs.getId(),
                            emp.getId(),
                            emp.getFirstName() + " " + emp.getLastName(),
                            "STEM_OPT_REVIEW",
                            "AWAITING_REVIEW",
                            vs.getLastModificationDate()
                    ));
                });

        return items;
    }

    @Override
    public EmployeeSummaryResponse assignHouse(String employeeId, Long houseId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        HouseInfo house = housingServiceClient.getHouseById(houseId);

        long currentOccupants = employeeRepository.findByHouseId(houseId).stream()
                .filter(e -> !e.getId().equals(employeeId))
                .count();

        if (currentOccupants >= house.maxOccupant()) {
            throw new ValidationException(
                    "House " + houseId + " is at full capacity ("
                            + house.maxOccupant() + "/" + house.maxOccupant() + ")");
        }

        employee.setHouseId(houseId);
        Employee saved = employeeRepository.save(employee);
        VisaStatus activeVisa = visaStatusRepository
                .findByEmployeeIdAndActiveFlagTrue(saved.getId()).orElse(null);
        return EmployeeMapper.toSummary(saved, activeVisa);
    }

    @Override
    public EmployeeSummaryResponse removeHouse(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));

        employee.setHouseId(null);
        Employee saved = employeeRepository.save(employee);
        VisaStatus activeVisa = visaStatusRepository
                .findByEmployeeIdAndActiveFlagTrue(saved.getId()).orElse(null);
        return EmployeeMapper.toSummary(saved, activeVisa);
    }

    @Override
    public List<HouseOccupancyResponse> getAvailableHouses() {
        List<HouseInfo> allHouses = housingServiceClient.getAllHouses();

        Map<Long, Long> occupancyByHouseId = employeeRepository.findByHouseIdIsNotNull()
                .stream()
                .collect(Collectors.groupingBy(Employee::getHouseId, Collectors.counting()));

        return allHouses.stream()
                .map(house -> {
                    int current = occupancyByHouseId.getOrDefault(house.id(), 0L).intValue();
                    int maxOccupant = house.maxOccupant() == null ? 0 : house.maxOccupant();
                    return new HouseOccupancyResponse(
                            house.id(),
                            house.address(),
                            house.maxOccupant(),
                            current,
                            Math.max(maxOccupant - current, 0),
                            house.landlordId(),
                            house.landlordName(),
                            house.landlordEmail(),
                            house.landlordPhone()
                    );
                })
                .filter(h -> h.availableSpots() > 0)
                .toList();
    }

    @Override
    public List<HrHouseSummaryResponse> getHousingSummary() {
        List<HouseInfo> allHouses = housingServiceClient.getAllHouses();

        Map<Long, List<Employee>> residentsByHouseId = employeeRepository.findByHouseIdIsNotNull()
                .stream()
                .collect(Collectors.groupingBy(Employee::getHouseId));

        return allHouses.stream()
                .map(house -> {
                    List<Employee> residents = residentsByHouseId.getOrDefault(house.id(), List.of());
                    int currentOccupants = residents.size();
                    int maxOccupant = house.maxOccupant() == null ? 0 : house.maxOccupant();

                    return new HrHouseSummaryResponse(
                            house.id(),
                            house.address(),
                            house.maxOccupant(),
                            currentOccupants,
                            Math.max(maxOccupant - currentOccupants, 0),
                            house.landlordId(),
                            house.landlordName(),
                            house.landlordEmail(),
                            house.landlordPhone(),
                            residents.stream()
                                    .map(this::toHrHouseResidentResponse)
                                    .toList()
                    );
                })
                .toList();
    }

    private HrHouseResidentResponse toHrHouseResidentResponse(Employee employee) {
        return new HrHouseResidentResponse(
                employee.getId(),
                employee.getUserId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPreferredName(),
                employee.getEmail(),
                employee.getCellPhone()
        );
    }
}
