package bf.backend.housing_service.controller;

import bf.backend.housing_service.dto.FacilityRequest;
import bf.backend.housing_service.dto.FacilityResponse;
import bf.backend.housing_service.service.FacilityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/housing/facilities")
public class FacilityController {

    private final FacilityService facilityService;

    public FacilityController(FacilityService facilityService) {
        this.facilityService = facilityService;
    }

    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public FacilityResponse createFacility(@Valid @RequestBody FacilityRequest request) {
        return facilityService.createFacility(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public List<FacilityResponse> getAllFacilities() {
        return facilityService.getAllFacilities();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public FacilityResponse getFacilityById(@PathVariable Long id) {
        return facilityService.getFacilityById(id);
    }

    @GetMapping("/house/{houseId}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public List<FacilityResponse> getFacilitiesByHouseId(@PathVariable Long houseId) {
        return facilityService.getFacilitiesByHouseId(houseId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public FacilityResponse updateFacility(@PathVariable Long id,
                                           @Valid @RequestBody FacilityRequest request) {
        return facilityService.updateFacility(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public String deleteFacility(@PathVariable Long id) {
        facilityService.deleteFacility(id);
        return "Facility deleted successfully";
    }
}
