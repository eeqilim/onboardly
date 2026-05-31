package bf.backend.employee_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationDocumentMetadataRequest {

    @NotNull(message = "Application ID is required")
    private Integer applicationId;

    @NotBlank(message = "Document type is required")
    private String type;

    @NotNull(message = "isRequired is required")
    private Integer isRequired;

    @NotBlank(message = "Path is required")
    private String path;

    private String sourceDocumentId;

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    @NotBlank(message = "Title is required")
    private String title;

    private String description;
}
