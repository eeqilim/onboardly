package bf.backend.employee_service.service;

import bf.backend.employee_service.dto.response.HouseDetailResponse;
import bf.backend.employee_service.dto.response.HouseResidentResponse;

import java.util.List;

public interface EmployeeHousingService {

    List<HouseResidentResponse> getMyHouseResidents();

    HouseDetailResponse getMyHouseDetail();
}