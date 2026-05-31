package bf.backend.housing_service.service;

import bf.backend.housing_service.dto.FacilityRequest;
import bf.backend.housing_service.dto.FacilityResponse;
import bf.backend.housing_service.entity.Facility;
import bf.backend.housing_service.entity.House;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.FacilityRepository;
import bf.backend.housing_service.repository.HouseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FacilityService {

    private final FacilityRepository facilityRepository;
    private final HouseRepository houseRepository;

    public FacilityService(FacilityRepository facilityRepository,
                           HouseRepository houseRepository) {
        this.facilityRepository = facilityRepository;
        this.houseRepository = houseRepository;
    }

    public FacilityResponse createFacility(FacilityRequest request) {
        House house = getHouseOrThrow(request.getHouseId());

        Facility facility = Facility.builder()
                .house(house)
                .type(request.getType())
                .description(request.getDescription())
                .quantity(request.getQuantity())
                .build();

        Facility savedFacility = facilityRepository.save(facility);
        return mapToFacilityResponse(savedFacility);
    }

    public List<FacilityResponse> getAllFacilities() {
        return facilityRepository.findAll()
                .stream()
                .map(this::mapToFacilityResponse)
                .toList();
    }

    public FacilityResponse getFacilityById(Long id) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + id));

        return mapToFacilityResponse(facility);
    }

    public List<FacilityResponse> getFacilitiesByHouseId(Long houseId) {
        if (!houseRepository.existsById(houseId)) {
            throw new ResourceNotFoundException("House not found with id: " + houseId);
        }

        return facilityRepository.findByHouse_Id(houseId)
                .stream()
                .map(this::mapToFacilityResponse)
                .toList();
    }

    public FacilityResponse updateFacility(Long id, FacilityRequest request) {
        Facility facility = facilityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Facility not found with id: " + id));

        House house = getHouseOrThrow(request.getHouseId());

        facility.setHouse(house);
        facility.setType(request.getType());
        facility.setDescription(request.getDescription());
        facility.setQuantity(request.getQuantity());

        Facility updatedFacility = facilityRepository.save(facility);
        return mapToFacilityResponse(updatedFacility);
    }

    public void deleteFacility(Long id) {
        if (!facilityRepository.existsById(id)) {
            throw new ResourceNotFoundException("Facility not found with id: " + id);
        }

        facilityRepository.deleteById(id);
    }

    private FacilityResponse mapToFacilityResponse(Facility facility) {
        return FacilityResponse.builder()
                .id(facility.getId())
                .houseId(facility.getHouse().getId())
                .type(facility.getType())
                .description(facility.getDescription())
                .quantity(facility.getQuantity())
                .build();
    }

    private House getHouseOrThrow(Long houseId) {
        return houseRepository.findById(houseId)
                .orElseThrow(() -> new ResourceNotFoundException("House not found with id: " + houseId));
    }
}
