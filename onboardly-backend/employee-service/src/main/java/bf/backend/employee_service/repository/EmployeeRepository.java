package bf.backend.employee_service.repository;

import bf.backend.employee_service.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends MongoRepository<Employee, String> {

    Optional<Employee> findByUserId(Long userId);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("{ '$or': [ { 'firstName': { '$regex': ?0, '$options': 'i' } }, { 'lastName': { '$regex': ?0, '$options': 'i' } }, { 'preferredName': { '$regex': ?0, '$options': 'i' } } ] }")
    List<Employee> searchByName(String query);

    List<Employee> findByHouseId(Long houseId);

    List<Employee> findByHouseIdIsNotNull();

    Page<Employee> findAll(Pageable pageable);
}
