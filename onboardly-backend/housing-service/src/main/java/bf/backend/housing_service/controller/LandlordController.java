package bf.backend.housing_service.controller;

import bf.backend.housing_service.dto.LandlordRequest;
import bf.backend.housing_service.dto.LandlordResponse;
import bf.backend.housing_service.service.LandlordService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/housing/landlords")
public class LandlordController {

    private final LandlordService landlordService;

    public LandlordController(LandlordService landlordService) {
        this.landlordService = landlordService;
    }

    @PostMapping
    @PreAuthorize("hasRole('HR')")
    public LandlordResponse createLandlord(@Valid @RequestBody LandlordRequest request) {
        return landlordService.createLandlord(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('HR')")
    public List<LandlordResponse> getAllLandlords() {
        return landlordService.getAllLandlords();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public LandlordResponse getLandlordById(@PathVariable Long id) {
        return landlordService.getLandlordById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public LandlordResponse updateLandlord(@PathVariable Long id,
                                           @Valid @RequestBody LandlordRequest request) {
        return landlordService.updateLandlord(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HR')")
    public String deleteLandlord(@PathVariable Long id) {
        landlordService.deleteLandlord(id);
        return "Landlord deleted successfully";
    }
}
