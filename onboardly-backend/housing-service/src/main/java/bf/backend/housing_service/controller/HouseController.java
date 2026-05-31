package bf.backend.housing_service.controller;

import bf.backend.housing_service.dto.FacilityReportResponse;
import bf.backend.housing_service.dto.HouseDetailResponse;
import bf.backend.housing_service.dto.HouseRequest;
import bf.backend.housing_service.dto.HouseResponse;
import bf.backend.housing_service.service.HouseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/housing/houses")
public class HouseController {

    private final HouseService houseService;

    public HouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public HouseResponse createHouse(@Valid @RequestBody HouseRequest request) {
        return houseService.createHouse(request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public List<HouseResponse> getAllHouses() {
        return houseService.getAllHouses();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public HouseResponse getHouseById(@PathVariable Long id) {
        return houseService.getHouseById(id);
    }

    @GetMapping("/{id}/details")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public HouseDetailResponse getHouseDetails(@PathVariable Long id) {
        return houseService.getHouseDetails(id);
    }

    @GetMapping("/{houseId}/reports")
    @PreAuthorize("hasAnyRole('HR','EMPLOYEE')")
    public Page<FacilityReportResponse> getFacilityReportsByHouseId(@PathVariable Long houseId,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "5") int size) {
        return houseService.getFacilityReportsByHouseId(houseId, page, size);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public HouseResponse updateHouse(@PathVariable Long id,
                                     @Valid @RequestBody HouseRequest request) {
        return houseService.updateHouse(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public String deleteHouse(@PathVariable Long id) {
        houseService.deleteHouse(id);
        return "House deleted successfully";
    }
}
