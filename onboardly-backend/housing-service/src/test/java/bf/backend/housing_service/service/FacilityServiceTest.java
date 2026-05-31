package bf.backend.housing_service.service;

import bf.backend.housing_service.dto.FacilityRequest;
import bf.backend.housing_service.dto.FacilityResponse;
import bf.backend.housing_service.entity.Facility;
import bf.backend.housing_service.entity.House;
import bf.backend.housing_service.entity.Landlord;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.FacilityRepository;
import bf.backend.housing_service.repository.HouseRepository;
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
class FacilityServiceTest {

    @Mock
    private FacilityRepository facilityRepository;

    @Mock
    private HouseRepository houseRepository;

    @InjectMocks
    private FacilityService facilityService;

    @Test
    void createFacility_setsHouseRelationAndReturnsResponse() {
        House house = house(1L);
        FacilityRequest request = new FacilityRequest();
        request.setHouseId(house.getId());
        request.setType("Table");
        request.setDescription("Office table");
        request.setQuantity(3);

        Facility savedFacility = Facility.builder()
                .id(2L)
                .house(house)
                .type(request.getType())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .build();

        when(houseRepository.findById(house.getId())).thenReturn(Optional.of(house));
        when(facilityRepository.save(any(Facility.class))).thenReturn(savedFacility);

        FacilityResponse response = facilityService.createFacility(request);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getHouseId()).isEqualTo(1L);
        assertThat(response.getType()).isEqualTo("Table");

        ArgumentCaptor<Facility> facilityCaptor = ArgumentCaptor.forClass(Facility.class);
        verify(facilityRepository).save(facilityCaptor.capture());
        assertThat(facilityCaptor.getValue().getHouse()).isSameAs(house);
    }

    @Test
    void getFacilitiesByHouseId_returnsFacilitiesForHouse() {
        House house = house(1L);
        Facility facility = Facility.builder()
                .id(2L)
                .house(house)
                .type("Chair")
                .quantity(4)
                .build();

        when(houseRepository.existsById(house.getId())).thenReturn(true);
        when(facilityRepository.findByHouse_Id(house.getId())).thenReturn(List.of(facility));

        List<FacilityResponse> response = facilityService.getFacilitiesByHouseId(house.getId());

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getHouseId()).isEqualTo(1L);
    }

    @Test
    void createFacility_throwsWhenHouseDoesNotExist() {
        FacilityRequest request = new FacilityRequest();
        request.setHouseId(99L);
        request.setType("Bed");
        request.setQuantity(1);

        when(houseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> facilityService.createFacility(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("House not found");
    }

    @Test
    void updateFacility_updatesFieldsAndHouse() {
        House oldHouse = house(1L);
        House newHouse = house(3L);
        Facility facility = Facility.builder()
                .id(2L)
                .house(oldHouse)
                .type("Bed")
                .description("Queen bed")
                .quantity(2)
                .build();
        FacilityRequest request = new FacilityRequest();
        request.setHouseId(newHouse.getId());
        request.setType("Table");
        request.setDescription("Office table");
        request.setQuantity(1);

        when(facilityRepository.findById(facility.getId())).thenReturn(Optional.of(facility));
        when(houseRepository.findById(newHouse.getId())).thenReturn(Optional.of(newHouse));
        when(facilityRepository.save(facility)).thenReturn(facility);

        FacilityResponse response = facilityService.updateFacility(facility.getId(), request);

        assertThat(response.getHouseId()).isEqualTo(3L);
        assertThat(response.getType()).isEqualTo("Table");
        assertThat(response.getQuantity()).isEqualTo(1);
        verify(facilityRepository).save(facility);
    }

    @Test
    void deleteFacility_deletesWhenFound() {
        when(facilityRepository.existsById(2L)).thenReturn(true);

        facilityService.deleteFacility(2L);

        verify(facilityRepository).deleteById(2L);
    }

    private House house(Long id) {
        Landlord landlord = Landlord.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .cellPhone("1234567890")
                .build();

        return House.builder()
                .id(id)
                .address("123 Main St")
                .maxOccupant(4)
                .landlord(landlord)
                .build();
    }
}
