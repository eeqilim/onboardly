package bf.backend.housing_service.service;

import bf.backend.housing_service.dto.LandlordRequest;
import bf.backend.housing_service.dto.LandlordResponse;
import bf.backend.housing_service.entity.Landlord;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.LandlordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LandlordService {

    private final LandlordRepository landlordRepository;

    public LandlordService(LandlordRepository landlordRepository) {
        this.landlordRepository = landlordRepository;
    }

    public LandlordResponse createLandlord(LandlordRequest request) {
        Landlord landlord = Landlord.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .cellPhone(request.getCellPhone())
                .build();

        Landlord savedLandlord = landlordRepository.save(landlord);
        return mapToLandlordResponse(savedLandlord);
    }

    public List<LandlordResponse> getAllLandlords() {
        return landlordRepository.findAll()
                .stream()
                .map(this::mapToLandlordResponse)
                .toList();
    }

    public LandlordResponse getLandlordById(Long id) {
        Landlord landlord = landlordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with id: " + id));

        return mapToLandlordResponse(landlord);
    }

    public LandlordResponse updateLandlord(Long id, LandlordRequest request) {
        Landlord landlord = landlordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with id: " + id));

        landlord.setFirstName(request.getFirstName());
        landlord.setLastName(request.getLastName());
        landlord.setEmail(request.getEmail());
        landlord.setCellPhone(request.getCellPhone());

        Landlord updatedLandlord = landlordRepository.save(landlord);
        return mapToLandlordResponse(updatedLandlord);
    }

    public void deleteLandlord(Long id) {
        if (!landlordRepository.existsById(id)) {
            throw new ResourceNotFoundException("Landlord not found with id: " + id);
        }

        landlordRepository.deleteById(id);
    }

    private LandlordResponse mapToLandlordResponse(Landlord landlord) {
        return LandlordResponse.builder()
                .id(landlord.getId())
                .firstName(landlord.getFirstName())
                .lastName(landlord.getLastName())
                .email(landlord.getEmail())
                .cellPhone(landlord.getCellPhone())
                .createDate(landlord.getCreateDate())
                .lastModificationDate(landlord.getLastModificationDate())
                .build();
    }
}