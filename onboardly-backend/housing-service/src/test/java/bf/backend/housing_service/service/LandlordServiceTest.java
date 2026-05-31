package bf.backend.housing_service.service;

import bf.backend.housing_service.dto.LandlordRequest;
import bf.backend.housing_service.dto.LandlordResponse;
import bf.backend.housing_service.entity.Landlord;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.LandlordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class LandlordServiceTest {

    @Mock
    private LandlordRepository landlordRepository;

    @InjectMocks
    private LandlordService landlordService;

    @Test
    void createLandlord_returnsSavedLandlord() {
        LandlordRequest request = new LandlordRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setCellPhone("1234567890");

        Landlord savedLandlord = landlord(1L);
        when(landlordRepository.save(any(Landlord.class))).thenReturn(savedLandlord);

        LandlordResponse response = landlordService.createLandlord(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getAllLandlords_returnsMappedLandlords() {
        when(landlordRepository.findAll()).thenReturn(List.of(landlord(1L)));

        List<LandlordResponse> response = landlordService.getAllLandlords();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getLastName()).isEqualTo("Doe");
    }

    @Test
    void updateLandlord_updatesFields() {
        Landlord landlord = landlord(1L);
        LandlordRequest request = new LandlordRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setEmail("jane@example.com");
        request.setCellPhone("9876543210");

        when(landlordRepository.findById(landlord.getId())).thenReturn(Optional.of(landlord));
        when(landlordRepository.save(landlord)).thenReturn(landlord);

        LandlordResponse response = landlordService.updateLandlord(landlord.getId(), request);

        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo("Smith");
        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        verify(landlordRepository).save(landlord);
    }

    @Test
    void deleteLandlord_deletesWhenFound() {
        when(landlordRepository.existsById(1L)).thenReturn(true);

        landlordService.deleteLandlord(1L);

        verify(landlordRepository).deleteById(1L);
    }

    @Test
    void getLandlordById_throwsWhenMissing() {
        when(landlordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> landlordService.getLandlordById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Landlord not found");
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
}
