package bf.backend.employee_service.mapper;

import bf.backend.employee_service.dto.request.VisaInfoRequest;
import bf.backend.employee_service.dto.response.VisaStatusResponse;
import bf.backend.employee_service.entity.Employee;
import bf.backend.employee_service.entity.VisaStatus;

public final class VisaStatusMapper {

    private VisaStatusMapper() {}

    public static VisaStatusResponse toResponse(VisaStatus v) {
        return new VisaStatusResponse(
                v.getId(),
                v.getVisaType(),
                v.getVisaTypeOther(),
                v.getActiveFlag(),
                v.getStartDate(),
                v.getEndDate(),
                v.getLastModificationDate()
        );
    }

    public static VisaStatus toEntity(VisaInfoRequest r, Employee employee) {
        VisaStatus vs = new VisaStatus();
        vs.setEmployeeId(employee.getId());
        vs.setVisaType(r.visaType());
        vs.setVisaTypeOther(r.visaTypeOther());
        vs.setActiveFlag(true);
        vs.setStartDate(r.startDate());
        vs.setEndDate(r.endDate());
        return vs;
    }
}
