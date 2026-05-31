package bf.backend.housing_service.service;

import bf.backend.housing_service.dto.FacilityReportResponse;
import bf.backend.housing_service.dto.FacilityResponse;
import bf.backend.housing_service.dto.HouseDetailResponse;
import bf.backend.housing_service.dto.HouseRequest;
import bf.backend.housing_service.dto.HouseResponse;
import bf.backend.housing_service.entity.Facility;
import bf.backend.housing_service.entity.FacilityReport;
import bf.backend.housing_service.entity.House;
import bf.backend.housing_service.entity.Landlord;
import bf.backend.housing_service.exception.ResourceNotFoundException;
import bf.backend.housing_service.repository.FacilityReportRepository;
import bf.backend.housing_service.repository.FacilityRepository;
import bf.backend.housing_service.repository.HouseRepository;
import bf.backend.housing_service.repository.LandlordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HouseService {

    private final HouseRepository houseRepository;
    private final LandlordRepository landlordRepository;
    private final FacilityRepository facilityRepository;
    private final FacilityReportRepository facilityReportRepository;

    public HouseService(HouseRepository houseRepository,
                        LandlordRepository landlordRepository,
                        FacilityRepository facilityRepository,
                        FacilityReportRepository facilityReportRepository) {
        this.houseRepository = houseRepository;
        this.landlordRepository = landlordRepository;
        this.facilityRepository = facilityRepository;
        this.facilityReportRepository = facilityReportRepository;
    }

    public HouseResponse  createHouse(HouseRequest request) {
        House house = new House();
        house.setAddress(request.getAddress());
        house.setMaxOccupant(request.getMaxOccupant());

        if (request.getLandlordId() != null) {
            Landlord landlord = landlordRepository.findById(request.getLandlordId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Landlord not found with id: " + request.getLandlordId()
                    ));

            house.setLandlord(landlord);
        }

        House savedHouse = houseRepository.save(house);
        return mapToHouseResponse(savedHouse);
    }

    public List<HouseResponse> getAllHouses() {
        return houseRepository.findAll()
                .stream()
                .map(this::mapToHouseResponse)
                .toList();
    }

    public HouseResponse getHouseById(Long id) {
        House house = houseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House not found with id: " + id));

        return mapToHouseResponse(house);
    }

    public HouseDetailResponse getHouseDetails(Long id) {
        House house = getHouseOrThrow(id);

        List<FacilityResponse> facilities = facilityRepository.findByHouse_Id(id)
                .stream()
                .map(this::mapToFacilityResponse)
                .toList();

        List<FacilityReportResponse> recentReports = facilityReportRepository
                .findTop5ByFacility_House_IdOrderByCreateDateDesc(id)
                .stream()
                .map(this::mapToFacilityReportResponse)
                .toList();

        return mapToHouseDetailResponse(house, facilities, recentReports);
    }

    public Page<FacilityReportResponse> getFacilityReportsByHouseId(Long houseId, int page, int size) {
        getHouseOrThrow(houseId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 3), 5),
                Sort.by(Sort.Direction.DESC, "createDate")
        );

        return facilityReportRepository.findByFacility_House_Id(houseId, pageable)
                .map(this::mapToFacilityReportResponse);
    }

    public HouseResponse updateHouse(Long id, HouseRequest request) {
        House house = houseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House not found with id: " + id));

        Landlord landlord = landlordRepository.findById(request.getLandlordId())
                .orElseThrow(() -> new ResourceNotFoundException("Landlord not found with id: " + request.getLandlordId()));

        house.setAddress(request.getAddress());
        house.setMaxOccupant(request.getMaxOccupant());
        house.setLandlord(landlord);

        House updatedHouse = houseRepository.save(house);
        return mapToHouseResponse(updatedHouse);
    }

    public void deleteHouse(Long id) {
        if (!houseRepository.existsById(id)) {
            throw new ResourceNotFoundException("House not found with id: " + id);
        }

        houseRepository.deleteById(id);
    }

    private HouseResponse mapToHouseResponse(House house) {
        String landlordName = null;
        Long landlordId = null;
        String landlordEmail = null;
        String landlordPhone = null;

        if (house.getLandlord() != null) {
            landlordId = house.getLandlord().getId();
            landlordName = house.getLandlord().getFirstName() + " " + house.getLandlord().getLastName();
            landlordEmail = house.getLandlord().getEmail();
            landlordPhone = house.getLandlord().getCellPhone();
        }

        return HouseResponse.builder()
                .id(house.getId())
                .address(house.getAddress())
                .maxOccupant(house.getMaxOccupant())
                .landlordId(landlordId)
                .landlordName(landlordName)
                .landlordEmail(landlordEmail)
                .landlordPhone(landlordPhone)
                .createDate(house.getCreateDate())
                .lastModificationDate(house.getLastModificationDate())
                .build();
    }

    private HouseDetailResponse mapToHouseDetailResponse(House house,
                                                         List<FacilityResponse> facilities,
                                                         List<FacilityReportResponse> recentReports) {
        String landlordName = null;
        Long landlordId = null;
        String landlordEmail = null;
        String landlordPhone = null;

        if (house.getLandlord() != null) {
            landlordId = house.getLandlord().getId();
            landlordName = house.getLandlord().getFirstName() + " " + house.getLandlord().getLastName();
            landlordEmail = house.getLandlord().getEmail();
            landlordPhone = house.getLandlord().getCellPhone();
        }

        return HouseDetailResponse.builder()
                .id(house.getId())
                .address(house.getAddress())
                .maxOccupant(house.getMaxOccupant())
                .landlordId(landlordId)
                .landlordName(landlordName)
                .landlordEmail(landlordEmail)
                .landlordPhone(landlordPhone)
                .facilities(facilities)
                .recentReports(recentReports)
                .createDate(house.getCreateDate())
                .lastModificationDate(house.getLastModificationDate())
                .build();
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

    private FacilityReportResponse mapToFacilityReportResponse(FacilityReport report) {
        return FacilityReportResponse.builder()
                .id(report.getId())
                .facilityId(report.getFacility().getId())
                .employeeId(report.getEmployeeId())
                .title(report.getTitle())
                .description(report.getDescription())
                .status(report.getStatus())
                .createDate(report.getCreateDate())
                .lastModificationDate(report.getLastModificationDate())
                .build();
    }

    private House getHouseOrThrow(Long id) {
        return houseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House not found with id: " + id));
    }
}
