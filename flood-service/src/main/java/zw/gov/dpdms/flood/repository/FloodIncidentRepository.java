package zw.gov.dpdms.flood.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import zw.gov.dpdms.flood.model.FloodIncident;
import zw.gov.dpdms.flood.model.IncidentStatus;

import java.util.List;

public interface FloodIncidentRepository
        extends JpaRepository<FloodIncident, Long>, JpaSpecificationExecutor<FloodIncident> {

    // Used by recorders — their own ward only.
    List<FloodIncident> findByWard(String ward);

    // Used by the dashboard/report/map — approved-only, ever.
    List<FloodIncident> findByStatus(IncidentStatus status);

    List<FloodIncident> findByWardAndStatus(String ward, IncidentStatus status);
}
