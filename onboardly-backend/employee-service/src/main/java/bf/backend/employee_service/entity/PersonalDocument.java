package bf.backend.employee_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "personal_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonalDocument {

    @Id
    private String id;

    @Indexed
    private String employeeId;

    private DocumentType documentType;
    private String s3Key;
    private String title;
    private String comment;
    private String applicationType;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime lastModificationDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonalDocument other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "PersonalDocument{id=" + id
                + ", employeeId=" + employeeId
                + ", documentType=" + documentType
                + ", s3Key='" + s3Key + '\''
                + ", title='" + title + '\''
                + ", applicationType='" + applicationType + '\''
                + '}';
    }
}
