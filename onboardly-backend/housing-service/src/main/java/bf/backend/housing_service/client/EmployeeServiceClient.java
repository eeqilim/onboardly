package bf.backend.housing_service.client;

import bf.backend.housing_service.config.FeignConfig;
import bf.backend.housing_service.dto.EmployeeNameResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "employee-service",
        configuration = FeignConfig.class
)
public interface EmployeeServiceClient {

    @GetMapping("/employee/users/{userId}/name")
    EmployeeNameResponse getEmployeeNameByUserId(@PathVariable("userId") Long userId);
}