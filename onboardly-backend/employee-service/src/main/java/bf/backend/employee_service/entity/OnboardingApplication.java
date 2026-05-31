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
import java.util.ArrayList;
import java.util.List;

@Document(collection = "onboarding_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingApplication {

    @Id
    private String id;

    @Indexed
    private String employeeId;

    private Integer applicationWorkflowId;

    private ApplicationStatus status = ApplicationStatus.NOT_STARTED;
    private String hrFeedback;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;

    private List<ApplicationComment> comments = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OnboardingApplication other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

    @Override
    public String toString() {
        return "OnboardingApplication{id=" + id
                + ", employeeId=" + employeeId
                + ", status=" + status
                + ", submittedAt=" + submittedAt
                + ", reviewedAt=" + reviewedAt
                + '}';
    }
}
