package bf.backend.housing_service.repository;

import bf.backend.housing_service.entity.FacilityReport;
import bf.backend.housing_service.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FacilityReportRepository extends JpaRepository<FacilityReport, Long> {

    List<FacilityReport> findByFacility_Id(Long facilityId);

    List<FacilityReport> findTop5ByFacility_House_IdOrderByCreateDateDesc(Long houseId);

    Page<FacilityReport> findByFacility_House_Id(Long houseId, Pageable pageable);

    List<FacilityReport> findByEmployeeId(Long employeeId);

    List<FacilityReport> findByStatus(ReportStatus status);
}
