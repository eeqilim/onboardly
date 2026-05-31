package bf.backend.employee_service.service;

import bf.backend.employee_service.dto.request.ReviewApplicationRequest;
import bf.backend.employee_service.dto.response.StemOptProgressResponse;
import bf.backend.employee_service.dto.response.VisaSummaryResponse;
import bf.backend.employee_service.dto.response.VisaStatusResponse;
import bf.backend.employee_service.entity.StemOptStep;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface VisaStatusService {

    VisaStatusResponse getMyActiveVisa();

    List<VisaStatusResponse> getMyVisaHistory();

    VisaStatusResponse uploadStemOptDocument(StemOptStep step, MultipartFile file,
                                             LocalDate eadStartDate, LocalDate eadEndDate);

    StemOptProgressResponse getMyStemOptProgress();

    StemOptProgressResponse getStemOptProgress(String employeeId);

    List<VisaSummaryResponse> listAllVisaStatuses();

    VisaStatusResponse reviewStemOptApplication(String visaStatusId, ReviewApplicationRequest req);
}
