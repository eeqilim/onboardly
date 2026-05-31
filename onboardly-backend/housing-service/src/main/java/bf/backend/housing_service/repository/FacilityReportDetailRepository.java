package bf.backend.housing_service.repository;

import bf.backend.housing_service.entity.FacilityReportDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityReportDetailRepository extends JpaRepository<FacilityReportDetail, Long> {

    List<FacilityReportDetail> findByFacilityReport_Id(Long facilityReportId);

    List<FacilityReportDetail> findByFacilityReport_IdOrderByCreateDateAsc(Long facilityReportId);

    List<FacilityReportDetail> findByEmployeeId(Long employeeId);
}
