package bf.backend.housing_service.service;

import bf.backend.housing_service.client.EmployeeServiceClient;
import bf.backend.housing_service.dto.EmployeeNameResponse;
import bf.backend.housing_service.dto.FacilityReportDetailResponse;
import bf.backend.housing_service.dto.FacilityReportRequest;
import bf.backend.housing_service.dto.FacilityReportResponse;
import bf.backend.housing_service.dto.FacilityReportWithDetailsResponse;
import bf.backend.housing_service.entity.Facility;
import bf.backend.housing_service.entity.FacilityReport;
import bf.backend.housing_service.entity.FacilityReportDetail;
import bf.backend.housing_service.entity.ReportStatus;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.FacilityReportDetailRepository;
import bf.backend.housing_service.repository.FacilityRepository;
import bf.backend.housing_service.repository.FacilityReportRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacilityReportService {

    private final FacilityReportRepository facilityReportRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityReportDetailRepository detailRepository;
    private final EmployeeServiceClient employeeServiceClient;

    public FacilityReportService(FacilityReportRepository facilityReportRepository,
                                 FacilityRepository facilityRepository,
                                 FacilityReportDetailRepository detailRepository,
                                 EmployeeServiceClient employeeServiceClient) {
        this.facilityReportRepository = facilityReportRepository;
        this.facilityRepository = facilityRepository;
        this.detailRepository = detailRepository;
        this.employeeServiceClient = employeeServiceClient;
    }

    public FacilityReportResponse createFacilityReport(FacilityReportRequest request) {
        Facility facility = getFacilityOrThrow(request.getFacilityId());

        FacilityReport report = FacilityReport.builder()
                .facility(facility)
                .employeeId(request.getEmployeeId())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ReportStatus.OPEN)
                .build();

        FacilityReport savedReport = facilityReportRepository.save(report);
        return mapToFacilityReportResponse(savedReport);
    }

    public List<FacilityReportResponse> getAllFacilityReports() {
        return facilityReportRepository.findAll()
                .stream()
                .map(this::mapToFacilityReportResponse)
                .toList();
    }

    public FacilityReportResponse getFacilityReportById(Long id) {
        FacilityReport report = facilityReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility report not found with id: " + id));

        return mapToFacilityReportResponse(report);
    }

    public FacilityReportWithDetailsResponse getFacilityReportWithDetails(Long id) {
        FacilityReport report = getReportOrThrow(id);

        List<FacilityReportDetailResponse> comments = detailRepository
                .findByFacilityReport_IdOrderByCreateDateAsc(id)
                .stream()
                .map(this::mapToDetailResponse)
                .toList();

        String createdByName = getEmployeeName(report.getEmployeeId());

        return FacilityReportWithDetailsResponse.builder()
                .id(report.getId())
                .facilityId(report.getFacility().getId())
                .employeeId(report.getEmployeeId())
                .title(report.getTitle())
                .description(report.getDescription())
                .status(report.getStatus())
                .createDate(report.getCreateDate())
                .lastModificationDate(report.getLastModificationDate())
                .createdByName(createdByName)
                .comments(comments)
                .build();
    }

    public List<FacilityReportResponse> getFacilityReportsByFacilityId(Long facilityId) {
        if (!facilityRepository.existsById(facilityId)) {
            throw new ResourceNotFoundException("Facility not found with id: " + facilityId);
        }

        return facilityReportRepository.findByFacility_Id(facilityId)
                .stream()
                .map(this::mapToFacilityReportResponse)
                .toList();
    }

    public List<FacilityReportResponse> getFacilityReportsByEmployeeId(Long employeeId) {
        return facilityReportRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::mapToFacilityReportResponse)
                .toList();
    }

    public List<FacilityReportResponse> getFacilityReportsByStatus(ReportStatus status) {
        return facilityReportRepository.findByStatus(status)
                .stream()
                .map(this::mapToFacilityReportResponse)
                .toList();
    }

    public FacilityReportResponse updateFacilityReportStatus(Long id, ReportStatus status) {
        FacilityReport report = facilityReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility report not found with id: " + id));

        report.setStatus(status);

        FacilityReport updatedReport = facilityReportRepository.save(report);
        return mapToFacilityReportResponse(updatedReport);
    }

    public FacilityReportResponse updateFacilityReport(Long id, FacilityReportRequest request) {
        FacilityReport report = facilityReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility report not found with id: " + id));

        Facility facility = getFacilityOrThrow(request.getFacilityId());

        report.setFacility(facility);
        report.setEmployeeId(request.getEmployeeId());
        report.setTitle(request.getTitle());
        report.setDescription(request.getDescription());

        FacilityReport updatedReport = facilityReportRepository.save(report);
        return mapToFacilityReportResponse(updatedReport);
    }

    public void deleteFacilityReport(Long id) {
        if (!facilityReportRepository.existsById(id)) {
            throw new ResourceNotFoundException("Facility report not found with id: " + id);
        }

        facilityReportRepository.deleteById(id);
    }

    private FacilityReportResponse mapToFacilityReportResponse(FacilityReport report) {
        String createdByName = getEmployeeName(report.getEmployeeId());

        return FacilityReportResponse.builder()
                .id(report.getId())
                .facilityId(report.getFacility().getId())
                .employeeId(report.getEmployeeId())
                .title(report.getTitle())
                .description(report.getDescription())
                .status(report.getStatus())
                .createDate(report.getCreateDate())
                .lastModificationDate(report.getLastModificationDate())
                .createdByName(createdByName)
                .build();
    }

    private Facility getFacilityOrThrow(Long facilityId) {
        return facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + facilityId));
    }

    private FacilityReport getReportOrThrow(Long reportId) {
        return facilityReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Facility report not found with id: " + reportId));
    }

    private FacilityReportDetailResponse mapToDetailResponse(FacilityReportDetail detail) {
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
