package zw.gov.dpdms.drought.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import zw.gov.dpdms.drought.model.DroughtIncident;
import zw.gov.dpdms.drought.model.IncidentStatus;

import java.util.List;

public interface DroughtIncidentRepository
        extends JpaRepository<DroughtIncident, Long>, JpaSpecificationExecutor<DroughtIncident> {

    // Used by recorders — their own ward only.
    List<DroughtIncident> findByWard(String ward);

    // Used by the dashboard/report/map — approved-only, ever.
    List<DroughtIncident> findByStatus(IncidentStatus status);

    List<DroughtIncident> findByWardAndStatus(String ward, IncidentStatus status);
}
