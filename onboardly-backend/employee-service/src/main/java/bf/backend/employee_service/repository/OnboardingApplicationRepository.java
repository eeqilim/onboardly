package bf.backend.employee_service.repository;

import bf.backend.employee_service.entity.ApplicationStatus;
import bf.backend.employee_service.entity.OnboardingApplication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OnboardingApplicationRepository extends MongoRepository<OnboardingApplication, String> {

    Optional<OnboardingApplication> findFirstByEmployeeIdOrderByCreatedAtDesc(String employeeId);

    List<OnboardingApplication> findByStatus(ApplicationStatus status);
}
