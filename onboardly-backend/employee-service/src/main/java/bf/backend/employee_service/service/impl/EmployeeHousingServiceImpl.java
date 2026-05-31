package bf.backend.employee_service.service.impl;

import bf.backend.employee_service.client.HousingServiceClient;
import bf.backend.employee_service.dto.response.HouseResidentResponse;
import bf.backend.employee_service.dto.response.HouseDetailResponse;
import bf.backend.employee_service.dto.response.HouseInfo;
import bf.backend.employee_service.entity.Employee;
import bf.backend.employee_service.entity.VisaStatus;
import bf.backend.employee_service.exception.ResourceNotFoundException;
import bf.backend.employee_service.mapper.EmployeeMapper;
import bf.backend.employee_service.repository.EmployeeRepository;
import bf.backend.employee_service.repository.VisaStatusRepository;
import bf.backend.employee_service.service.EmployeeHousingService;
import bf.backend.employee_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeHousingServiceImpl implements EmployeeHousingService {

    private final EmployeeRepository employeeRepository;
    private final VisaStatusRepository visaStatusRepository;
    private final HousingServiceClient housingServiceClient;

    @Override
    public List<HouseResidentResponse> getMyHouseResidents() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Employee currentEmployee = employeeRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        Long houseId = currentEmployee.getHouseId();

        if (houseId == null) {
            return List.of();
        }

        return employeeRepository.findByHouseId(houseId)
                .stream()
//                .filter(employee -> !employee.getId().equals(currentEmployee.getId()))
                .map(this::toHouseResidentResponse)
                .toList();
    }

    @Override
    public HouseDetailResponse getMyHouseDetail() {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        Employee currentEmployee = employeeRepository.findByUserId(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee profile not found"));

        Long houseId = currentEmployee.getHouseId();

        if (houseId == null) {
            return HouseDetailResponse.builder()
                    .houseId(null)
                    .address(null)
                    .residents(List.of())
                    .build();
        }

        HouseInfo house = housingServiceClient.getHouseById(houseId);

        List<HouseResidentResponse> residents = employeeRepository.findByHouseId(houseId)
                .stream()
                .filter(employee -> !employee.getId().equals(currentEmployee.getId()))
                .map(this::toHouseResidentResponse)
                .toList();

        return HouseDetailResponse.builder()
                .houseId(houseId)
                .address(house == null ? null : house.address())
                .residents(residents)
                .build();
    }

    private HouseResidentResponse toHouseResidentResponse(Employee employee) {
        return HouseResidentResponse.builder()
                .employeeId(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .preferredName(employee.getPreferredName())
                .cellPhone(employee.getCellPhone())
                .build();
    }
}