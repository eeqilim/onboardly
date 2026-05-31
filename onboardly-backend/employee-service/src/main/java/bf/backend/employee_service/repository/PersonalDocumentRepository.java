package bf.backend.employee_service.repository;

import bf.backend.employee_service.entity.DocumentType;
import bf.backend.employee_service.entity.PersonalDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonalDocumentRepository extends MongoRepository<PersonalDocument, String> {

    List<PersonalDocument> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);

    List<PersonalDocument> findByEmployeeIdAndApplicationTypeOrderByCreatedAtDesc(
            String employeeId, String applicationType);

    boolean existsByEmployeeIdAndApplicationTypeAndDocumentType(
            String employeeId, String applicationType, DocumentType documentType);
}
