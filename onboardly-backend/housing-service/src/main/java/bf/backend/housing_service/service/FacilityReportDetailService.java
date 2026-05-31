package bf.backend.housing_service.service;

import bf.backend.housing_service.client.EmployeeServiceClient;
import bf.backend.housing_service.dto.EmployeeNameResponse;
import bf.backend.housing_service.dto.FacilityReportDetailRequest;
import bf.backend.housing_service.dto.FacilityReportDetailResponse;
import bf.backend.housing_service.entity.FacilityReport;
import bf.backend.housing_service.entity.FacilityReportDetail;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.FacilityReportDetailRepository;
import bf.backend.housing_service.repository.FacilityReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacilityReportDetailService {

    private final FacilityReportDetailRepository detailRepository;
    private final FacilityReportRepository reportRepository;
    private final EmployeeServiceClient employeeServiceClient;


    public FacilityReportDetailService(FacilityReportDetailRepository detailRepository,
                                       FacilityReportRepository reportRepository,
                                       EmployeeServiceClient employeeServiceClient) {
        this.detailRepository = detailRepository;
        this.reportRepository = reportRepository;
        this.employeeServiceClient = employeeServiceClient;
    }

    public FacilityReportDetailResponse createDetail(FacilityReportDetailRequest request) {
        FacilityReport report = getReportOrThrow(request.getFacilityReportId());

        FacilityReportDetail detail = FacilityReportDetail.builder()
                .facilityReport(report)
                .employeeId(request.getEmployeeId())
                .comment(request.getComment())
                .build();

        FacilityReportDetail savedDetail = detailRepository.save(detail);
        return mapToResponse(savedDetail);
    }

    public List<FacilityReportDetailResponse> getAllDetails() {
        return detailRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public FacilityReportDetailResponse getDetailById(Long id) {
        FacilityReportDetail detail = detailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility report detail not found with id: " + id));

        return mapToResponse(detail);
    }

    public List<FacilityReportDetailResponse> getDetailsByReportId(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new ResourceNotFoundException("Facility report not found with id: " + reportId);
        }

        return detailRepository.findByFacilityReport_Id(reportId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<FacilityReportDetailResponse> getDetailsByEmployeeId(Long employeeId) {
        return detailRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public FacilityReportDetailResponse updateDetail(Long id, FacilityReportDetailRequest request) {
        FacilityReportDetail detail = detailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility report detail not found with id: " + id));

        FacilityReport report = getReportOrThrow(request.getFacilityReportId());

        detail.setFacilityReport(report);
        detail.setEmployeeId(request.getEmployeeId());
        detail.setComment(request.getComment());

        FacilityReportDetail updatedDetail = detailRepository.save(detail);
        return mapToResponse(updatedDetail);
    }

    public void deleteDetail(Long id) {
        if (!detailRepository.existsById(id)) {
            throw new ResourceNotFoundException("Facility report detail not found with id: " + id);
        }

        detailRepository.deleteById(id);
    }

    private FacilityReportDetailResponse mapToResponse(FacilityReportDetail detail) {
        String commenterName = getEmployeeName(detail.getEmployeeId());

        return FacilityReportDetailResponse.builder()
                .id(detail.getId())
                .facilityReportId(detail.getFacilityReport().getId())
                .employeeId(detail.getEmployeeId())
                .comment(detail.getComment())
                .createDate(detail.getCreateDate())
                .lastModificationDate(detail.getLastModificationDate())
                .commenterName(commenterName)
                .build();
    }

    private FacilityReport getReportOrThrow(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility report not found with id: " + reportId));
    }

    private String getEmployeeName(Long employeeId) {
        try {
            EmployeeNameResponse employee = employeeServiceClient.getEmployeeNameByUserId(employeeId);

            if (employee == null) {
                return "Employee #" + employeeId;
            }

            if (employee.getPreferredName() != null && !employee.getPreferredName().isBlank()) {
                return employee.getPreferredName();
            }

            String firstName = employee.getFirstName() == null ? "" : employee.getFirstName();
            String lastName = employee.getLastName() == null ? "" : employee.getLastName();
            String fullName = (firstName + " " + lastName).trim();

            return fullName.isBlank() ? "Employee #" + employeeId : fullName;
        } catch (Exception e) {
            return "Employee #" + employeeId;
        }
    }
}
