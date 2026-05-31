package bf.backend.housing_service.repository;

import bf.backend.housing_service.entity.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HouseRepository extends JpaRepository<House, Long> {

    List<House> findByLandlordId(Long landlordId);
}