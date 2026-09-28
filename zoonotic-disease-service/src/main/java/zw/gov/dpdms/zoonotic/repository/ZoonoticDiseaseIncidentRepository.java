package zw.gov.dpdms.zoonotic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import zw.gov.dpdms.zoonotic.model.ZoonoticDiseaseIncident;
import zw.gov.dpdms.zoonotic.model.IncidentStatus;

import java.util.List;

public interface ZoonoticDiseaseIncidentRepository
        extends JpaRepository<ZoonoticDiseaseIncident, Long>, JpaSpecificationExecutor<ZoonoticDiseaseIncident> {

    // Used by recorders — their own ward only.
    List<ZoonoticDiseaseIncident> findByWard(String ward);

    // Used by the dashboard/report/map — approved-only, ever.
    List<ZoonoticDiseaseIncident> findByStatus(IncidentStatus status);

    List<ZoonoticDiseaseIncident> findByWardAndStatus(String ward, IncidentStatus status);
}
