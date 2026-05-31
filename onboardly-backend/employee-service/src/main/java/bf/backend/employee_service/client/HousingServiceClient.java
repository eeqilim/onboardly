package bf.backend.employee_service.client;

import bf.backend.employee_service.config.FeignConfig;
import bf.backend.employee_service.dto.response.HouseInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "housing-service", configuration = FeignConfig.class)
public interface HousingServiceClient {

    @GetMapping("/housing/houses")
    List<HouseInfo> getAllHouses();

    @GetMapping("/housing/houses/{id}")
    HouseInfo getHouseById(@PathVariable("id") Long id);
}
