package bf.backend.employee_service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "visa_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VisaStatus {

    @Id
    private String id;

    @Indexed
    private String employeeId;

    private VisaType visaType;
    private String visaTypeOther;
    private Boolean activeFlag = true;
    private LocalDate startDate;
    private LocalDate endDate;

    @LastModifiedDate
    private LocalDateTime lastModificationDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisaStatus other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "VisaStatus{id=" + id
                + ", employeeId=" + employeeId
                + ", visaType=" + visaType
                + ", activeFlag=" + activeFlag
                + ", startDate=" + startDate
                + ", endDate=" + endDate
                + '}';
    }
}
