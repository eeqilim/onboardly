package bf.backend.housing_service.service;

import bf.backend.housing_service.dto.FacilityReportDetailRequest;
import bf.backend.housing_service.dto.FacilityReportDetailResponse;
import bf.backend.housing_service.entity.Facility;
import bf.backend.housing_service.entity.FacilityReport;
import bf.backend.housing_service.entity.FacilityReportDetail;
import bf.backend.housing_service.entity.House;
import bf.backend.housing_service.entity.Landlord;
import bf.backend.housing_service.entity.ReportStatus;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.FacilityReportDetailRepository;
import bf.backend.housing_service.repository.FacilityReportRepository;
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
class FacilityReportDetailServiceTest {

    @Mock
    private FacilityReportDetailRepository detailRepository;

    @Mock
    private FacilityReportRepository reportRepository;

    @InjectMocks
    private FacilityReportDetailService detailService;

    @Test
    void createDetail_setsReportRelationAndReturnsResponse() {
        FacilityReport report = report(3L);
        FacilityReportDetailRequest request = new FacilityReportDetailRequest();
        request.setFacilityReportId(report.getId());
        request.setEmployeeId(100L);
        request.setComment("Please fix soon");

        FacilityReportDetail savedDetail = FacilityReportDetail.builder()
                .id(4L)
                .facilityReport(report)
                .employeeId(request.getEmployeeId())
                .comment(request.getComment())
                .build();

        when(reportRepository.findById(report.getId())).thenReturn(Optional.of(report));
        when(detailRepository.save(any(FacilityReportDetail.class))).thenReturn(savedDetail);

        FacilityReportDetailResponse response = detailService.createDetail(request);

        assertThat(response.getId()).isEqualTo(4L);
        assertThat(response.getFacilityReportId()).isEqualTo(3L);
        assertThat(response.getComment()).isEqualTo("Please fix soon");

        ArgumentCaptor<FacilityReportDetail> detailCaptor = ArgumentCaptor.forClass(FacilityReportDetail.class);
        verify(detailRepository).save(detailCaptor.capture());
        assertThat(detailCaptor.getValue().getFacilityReport()).isSameAs(report);
    }

    @Test
    void getDetailsByReportId_returnsMappedDetails() {
        FacilityReport report = report(3L);
        FacilityReportDetail detail = detail(4L, report);

        when(reportRepository.existsById(report.getId())).thenReturn(true);
        when(detailRepository.findByFacilityReport_Id(report.getId())).thenReturn(List.of(detail));

        List<FacilityReportDetailResponse> response = detailService.getDetailsByReportId(report.getId());

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getFacilityReportId()).isEqualTo(3L);
    }

    @Test
    void updateDetail_updatesFieldsAndReport() {
        FacilityReport oldReport = report(3L);
        FacilityReport newReport = report(5L);
        FacilityReportDetail detail = detail(4L, oldReport);
        FacilityReportDetailRequest request = new FacilityReportDetailRequest();
        request.setFacilityReportId(newReport.getId());
        request.setEmployeeId(200L);
        request.setComment("Updated comment");

        when(detailRepository.findById(detail.getId())).thenReturn(Optional.of(detail));
        when(reportRepository.findById(newReport.getId())).thenReturn(Optional.of(newReport));
        when(detailRepository.save(detail)).thenReturn(detail);

        FacilityReportDetailResponse response = detailService.updateDetail(detail.getId(), request);

        assertThat(response.getFacilityReportId()).isEqualTo(5L);
        assertThat(response.getEmployeeId()).isEqualTo(200L);
        assertThat(response.getComment()).isEqualTo("Updated comment");
        verify(detailRepository).save(detail);
    }

    @Test
    void updateDetail_throwsWhenDetailDoesNotExist() {
        FacilityReportDetailRequest request = new FacilityReportDetailRequest();
        request.setFacilityReportId(3L);
        request.setEmployeeId(100L);
        request.setComment("Updated comment");

        when(detailRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> detailService.updateDetail(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Facility report detail not found");
    }

    @Test
    void deleteDetail_deletesWhenFound() {
        when(detailRepository.existsById(4L)).thenReturn(true);

        detailService.deleteDetail(4L);

        verify(detailRepository).deleteById(4L);
    }

    private FacilityReportDetail detail(Long id, FacilityReport report) {
        return FacilityReportDetail.builder()
                .id(id)
                .facilityReport(report)
                .employeeId(100L)
                .comment("Please fix soon")
                .build();
    }

    private FacilityReport report(Long id) {
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
        Facility facility = Facility.builder()
                .id(2L)
                .house(house)
                .type("Chair")
                .quantity(4)
                .build();

        return FacilityReport.builder()
                .id(id)
                .facility(facility)
                .employeeId(100L)
                .title("Broken chair")
                .description("One chair leg is loose")
                .status(ReportStatus.OPEN)
                .build();
    }
}
