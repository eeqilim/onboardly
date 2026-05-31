package bf.backend.employee_service.service;

import bf.backend.employee_service.dto.response.ApplicationTrackingItem;
import bf.backend.employee_service.dto.response.EmployeeSummaryResponse;
import bf.backend.employee_service.dto.response.HouseOccupancyResponse;
import bf.backend.employee_service.dto.response.HrHouseSummaryResponse;
import bf.backend.employee_service.dto.response.PageResponse;

import java.util.List;

public interface HrEmployeeService {

    PageResponse<EmployeeSummaryResponse> listEmployees(int page);

    List<EmployeeSummaryResponse> searchEmployees(String query);

    List<ApplicationTrackingItem> getApplicationTrackingItems();

    List<EmployeeSummaryResponse> getResidentsByHouseId(Long houseId);

    EmployeeSummaryResponse assignHouse(String employeeId, Long houseId);

    EmployeeSummaryResponse removeHouse(String employeeId);

    List<HouseOccupancyResponse> getAvailableHouses();

    List<HrHouseSummaryResponse> getHousingSummary();
}
