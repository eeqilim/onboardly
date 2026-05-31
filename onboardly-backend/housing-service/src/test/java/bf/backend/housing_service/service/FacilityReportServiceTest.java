package bf.backend.housing_service.service;

import bf.backend.housing_service.dto.FacilityReportRequest;
import bf.backend.housing_service.dto.FacilityReportResponse;
import bf.backend.housing_service.dto.FacilityReportWithDetailsResponse;
import bf.backend.housing_service.entity.Facility;
import bf.backend.housing_service.entity.FacilityReport;
import bf.backend.housing_service.entity.FacilityReportDetail;
import bf.backend.housing_service.entity.House;
import bf.backend.housing_service.entity.Landlord;
import bf.backend.housing_service.entity.ReportStatus;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.FacilityReportDetailRepository;
import bf.backend.housing_service.repository.FacilityReportRepository;
import bf.backend.housing_service.repository.FacilityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityReportServiceTest {

    @Mock
    private FacilityReportRepository facilityReportRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private FacilityReportDetailRepository detailRepository;

    @InjectMocks
    private FacilityReportService facilityReportService;

    @Test
    void createFacilityReport_defaultsStatusToOpenAndSetsFacility() {
        Facility facility = facility(2L);
        FacilityReportRequest request = new FacilityReportRequest();
        request.setFacilityId(facility.getId());
        request.setEmployeeId(100L);
        request.setTitle("Broken chair");
        request.setDescription("One chair leg is loose");

        FacilityReport savedReport = FacilityReport.builder()
                .id(3L)
                .facility(facility)
                .employeeId(request.getEmployeeId())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(ReportStatus.OPEN)
                .build();

        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        when(facilityReportRepository.save(any(FacilityReport.class))).thenReturn(savedReport);

        FacilityReportResponse response = facilityReportService.createFacilityReport(request);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getFacilityId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.OPEN);

        ArgumentCaptor<FacilityReport> reportCaptor = ArgumentCaptor.forClass(FacilityReport.class);
        verify(facilityReportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getFacility()).isSameAs(facility);
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    @Test
    void updateFacilityReportStatus_changesStatus() {
        FacilityReport report = report(3L, facility(2L), ReportStatus.OPEN);

        when(facilityReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(facilityReportRepository.save(report)).thenReturn(report);

        FacilityReportResponse response = facilityReportService.updateFacilityReportStatus(
                report.getId(),
                ReportStatus.IN_PROGRESS
        );

        assertThat(response.getStatus()).isEqualTo(ReportStatus.IN_PROGRESS);
        verify(facilityReportRepository).save(report);
    }

    @Test
    void getFacilityReportWithDetails_returnsComments() {
        Facility facility = facility(2L);
        FacilityReport report = report(3L, facility, ReportStatus.OPEN);
        FacilityReportDetail detail = FacilityReportDetail.builder()
                .id(4L)
                .facilityReport(report)
                .employeeId(100L)
                .comment("Please fix soon")
                .build();

        when(facilityReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(detailRepository.findByFacilityReport_IdOrderByCreateDateAsc(report.getId()))
                .thenReturn(List.of(detail));

        FacilityReportWithDetailsResponse response = facilityReportService.getFacilityReportWithDetails(report.getId());

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getComments().get(0).getComment()).isEqualTo("Please fix soon");
    }

    @Test
    void createFacilityReport_throwsWhenFacilityDoesNotExist() {
        FacilityReportRequest request = new FacilityReportRequest();
        request.setFacilityId(99L);
        request.setEmployeeId(100L);
        request.setTitle("Broken chair");
        request.setDescription("One chair leg is loose");

        when(facilityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityReportService.createFacilityReport(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Facility not found");
    }

    @Test
    void getFacilityReportsByFacilityId_returnsMappedReports() {
        Facility facility = facility(2L);
        FacilityReport report = report(3L, facility, ReportStatus.OPEN);

        when(facilityRepository.existsById(facility.getId())).thenReturn(true);
        when(facilityReportRepository.findByFacility_Id(facility.getId())).thenReturn(List.of(report));

        List<FacilityReportResponse> response = facilityReportService.getFacilityReportsByFacilityId(facility.getId());

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getFacilityId()).isEqualTo(2L);
    }

    @Test
    void updateFacilityReport_updatesFieldsAndFacility() {
        Facility oldFacility = facility(2L);
        Facility newFacility = facility(5L);
        FacilityReport report = report(3L, oldFacility, ReportStatus.OPEN);
        FacilityReportRequest request = new FacilityReportRequest();
        request.setFacilityId(newFacility.getId());
        request.setEmployeeId(200L);
        request.setTitle("Broken table");
        request.setDescription("Table top is cracked");

        when(facilityReportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(facilityRepository.findById(newFacility.getId())).thenReturn(Optional.of(newFacility));
        when(facilityReportRepository.save(report)).thenReturn(report);

        FacilityReportResponse response = facilityReportService.updateFacilityReport(report.getId(), request);

        assertThat(response.getFacilityId()).isEqualTo(5L);
        assertThat(response.getEmployeeId()).isEqualTo(200L);
        assertThat(response.getTitle()).isEqualTo("Broken table");
        verify(facilityReportRepository).save(report);
    }

    @Test
    void deleteFacilityReport_deletesWhenFound() {
        when(facilityReportRepository.existsById(3L)).thenReturn(true);

        facilityReportService.deleteFacilityReport(3L);

        verify(facilityReportRepository).deleteById(3L);
    }

    private FacilityReport report(Long id, Facility facility, ReportStatus status) {
        return FacilityReport.builder()
                .id(id)
                .facility(facility)
                .employeeId(100L)
                .title("Broken chair")
                .description("One chair leg is loose")
                .status(status)
                .build();
    }

    private Facility facility(Long id) {
        Landlord landlord = Landlord.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .cellPhone("1234567890")
                .build();
        House house = House.builder()
                .id(1L)
                .address("123 Main St")
                .maxOccupant(4)
                .landlord(landlord)
                .build();

        return Facility.builder()
                .id(id)
                .house(house)
                .type("Chair")
                .quantity(4)
                .build();
    }
}
