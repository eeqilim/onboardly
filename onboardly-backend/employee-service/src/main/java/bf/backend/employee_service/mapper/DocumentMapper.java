package bf.backend.employee_service.mapper;

import bf.backend.employee_service.dto.request.DocumentUploadMetadata;
import bf.backend.employee_service.dto.response.DocumentResponse;
import bf.backend.employee_service.entity.Employee;
import bf.backend.employee_service.entity.PersonalDocument;

public final class DocumentMapper {

    private DocumentMapper() {}

    public static DocumentResponse toResponse(PersonalDocument d) {
        return new DocumentResponse(
                d.getId(),
                d.getDocumentType(),
                d.getS3Key(),
                d.getTitle(),
                d.getComment(),
                d.getApplicationType(),
                d.getCreatedAt(),
                d.getLastModificationDate()
        );
    }

    public static PersonalDocument toEntity(DocumentUploadMetadata meta, String s3Key, Employee employee) {
        PersonalDocument doc = new PersonalDocument();
        doc.setEmployeeId(employee.getId());
        doc.setDocumentType(meta.documentType());
        doc.setS3Key(s3Key);
        doc.setTitle(meta.title());
        doc.setComment(meta.comment());
        doc.setApplicationType(meta.applicationType());
        return doc;
    }
}
