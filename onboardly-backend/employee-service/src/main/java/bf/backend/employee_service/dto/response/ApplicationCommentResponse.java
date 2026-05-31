package bf.backend.employee_service.dto.response;

import java.time.LocalDateTime;

public record ApplicationCommentResponse(
        String id,
        Long authorId,
        LocalDateTime createdAt,
        String content
) {}
