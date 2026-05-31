package bf.backend.housing_service.service;

import bf.backend.housing_service.dto.HouseDetailResponse;
import bf.backend.housing_service.dto.HouseRequest;
import bf.backend.housing_service.dto.HouseResponse;
import bf.backend.housing_service.entity.Facility;
import bf.backend.housing_service.entity.FacilityReport;
import bf.backend.housing_service.entity.House;
import bf.backend.housing_service.entity.Landlord;
import bf.backend.housing_service.entity.ReportStatus;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.FacilityReportRepository;
import bf.backend.housing_service.repository.FacilityRepository;
import bf.backend.housing_service.repository.HouseRepository;
import bf.backend.housing_service.repository.LandlordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseServiceTest {

    @Mock
    private HouseRepository houseRepository;

    @Mock
    private LandlordRepository landlordRepository;

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private FacilityReportRepository facilityReportRepository;

    @InjectMocks
    private HouseService houseService;

    @Test
    void createHouse_setsLandlordAndReturnsResponse() {
        Landlord landlord = landlord(10L);
        HouseRequest request = new HouseRequest();
        request.setAddress("123 Main St");
        request.setMaxOccupant(4);
        request.setLandlordId(landlord.getId());

        House savedHouse = House.builder()
                .id(1L)
                .address(request.getAddress())
                .maxOccupant(request.getMaxOccupant())
                .landlord(landlord)
                .build();

        when(landlordRepository.findById(landlord.getId())).thenReturn(Optional.of(landlord));
        when(houseRepository.save(any(House.class))).thenReturn(savedHouse);

        HouseResponse response = houseService.createHouse(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAddress()).isEqualTo("123 Main St");
        assertThat(response.getLandlordId()).isEqualTo(10L);
        assertThat(response.getLandlordName()).isEqualTo("John Doe");

        ArgumentCaptor<House> houseCaptor = ArgumentCaptor.forClass(House.class);
        verify(houseRepository).save(houseCaptor.capture());
        assertThat(houseCaptor.getValue().getLandlord()).isSameAs(landlord);
    }

    @Test
    void getHouseDetails_returnsFacilitiesAndRecentReports() {
        Landlord landlord = landlord(10L);
        House house = house(1L, landlord);
        Facility facility = facility(2L, house);
        FacilityReport report = facilityReport(3L, facility);

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(facilityRepository.findByHouse_Id(house.getId())).thenReturn(List.of(facility));
        when(facilityReportRepository.findTop5ByFacility_House_IdOrderByCreateDateDesc(house.getId()))
                .thenReturn(List.of(report));

        HouseDetailResponse response = houseService.getHouseDetails(house.getId());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFacilities()).hasSize(1);
        assertThat(response.getFacilities().get(0).getHouseId()).isEqualTo(1L);
        assertThat(response.getRecentReports()).hasSize(1);
        assertThat(response.getRecentReports().get(0).getFacilityId()).isEqualTo(2L);
    }

    @Test
    void getFacilityReportsByHouseId_returnsPagedReports() {
        Landlord landlord = landlord(10L);
        House house = house(1L, landlord);
        Facility facility = facility(2L, house);
        FacilityReport report = facilityReport(3L, facility);

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(facilityReportRepository.findByFacility_House_Id(any(Long.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        Page<?> response = houseService.getFacilityReportsByHouseId(house.getId(), 0, 5);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    void getHouseById_throwsWhenHouseDoesNotExist() {
        when(houseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> houseService.getHouseById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("House not found");
    }

    @Test
    void createHouse_throwsWhenLandlordDoesNotExist() {
        HouseRequest request = new HouseRequest();
        request.setAddress("123 Main St");
        request.setMaxOccupant(4);
        request.setLandlordId(99L);

        when(landlordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> houseService.createHouse(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Landlord not found");
    }

    @Test
    void updateHouse_updatesFieldsAndLandlord() {
        Landlord oldLandlord = landlord(10L);
        Landlord newLandlord = landlord(11L);
        House house = house(1L, oldLandlord);
        HouseRequest request = new HouseRequest();
        request.setAddress("456 Oak Ave");
        request.setMaxOccupant(6);
        request.setLandlordId(newLandlord.getId());

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(landlordRepository.findById(newLandlord.getId())).thenReturn(Optional.of(newLandlord));
        when(houseRepository.save(house)).thenReturn(house);

        HouseResponse response = houseService.updateHouse(house.getId(), request);

        assertThat(response.getAddress()).isEqualTo("456 Oak Ave");
        assertThat(response.getMaxOccupant()).isEqualTo(6);
        assertThat(response.getLandlordId()).isEqualTo(11L);
        verify(houseRepository).save(house);
    }

    @Test
    void deleteHouse_deletesWhenFound() {
        when(houseRepository.existsById(1L)).thenReturn(true);

        houseService.deleteHouse(1L);

        verify(houseRepository).deleteById(1L);
    }

    private Landlord landlord(Long id) {
        return Landlord.builder()
                .id(id)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .cellPhone("1234567890")
                .build();
    }

    private House house(Long id, Landlord landlord) {
        return House.builder()
                .id(id)
                .address("123 Main St")
                .maxOccupant(4)
                .landlord(landlord)
                .build();
    }

    private Facility facility(Long id, House house) {
        return Facility.builder()
                .id(id)
                .house(house)
                .type("Bed")
                .description("Queen bed")
                .quantity(2)
                .build();
    }

    private FacilityReport facilityReport(Long id, Facility facility) {
        return FacilityReport.builder()
                .id(id)
                .facility(facility)
                .employeeId(100L)
                .title("Broken bed")
                .description("Bed frame is broken")
                .status(ReportStatus.OPEN)
                .build();
    }
}
