package zw.gov.dpdms.fire.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import zw.gov.dpdms.fire.model.FireIncident;
import zw.gov.dpdms.fire.model.IncidentStatus;

import java.util.List;

public interface FireIncidentRepository
        extends JpaRepository<FireIncident, Long>, JpaSpecificationExecutor<FireIncident> {

    // Used by recorders — their own ward only.
    List<FireIncident> findByWard(String ward);

    // Used by the dashboard/report/map — approved-only, ever.
    List<FireIncident> findByStatus(IncidentStatus status);

    List<FireIncident> findByWardAndStatus(String ward, IncidentStatus status);
}
