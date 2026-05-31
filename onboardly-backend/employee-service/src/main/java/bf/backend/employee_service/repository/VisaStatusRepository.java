package bf.backend.employee_service.repository;

import bf.backend.employee_service.entity.VisaStatus;
import bf.backend.employee_service.entity.VisaType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisaStatusRepository extends MongoRepository<VisaStatus, String> {

    Optional<VisaStatus> findByEmployeeIdAndActiveFlagTrue(String employeeId);

    List<VisaStatus> findByEmployeeIdOrderByLastModificationDateDesc(String employeeId);

    List<VisaStatus> findAllByActiveFlagTrue();

    List<VisaStatus> findByVisaTypeAndActiveFlagTrue(VisaType visaType);

    List<VisaStatus> findByEmployeeId(String employeeId);
}
