package zw.gov.dpdms.mining.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import zw.gov.dpdms.mining.model.MiningAccidentIncident;
import zw.gov.dpdms.mining.model.IncidentStatus;

import java.util.List;

public interface MiningAccidentIncidentRepository
        extends JpaRepository<MiningAccidentIncident, Long>, JpaSpecificationExecutor<MiningAccidentIncident> {

    // Used by recorders — their own ward only.
    List<MiningAccidentIncident> findByWard(String ward);

    // Used by the dashboard/report/map — approved-only, ever.
    List<MiningAccidentIncident> findByStatus(IncidentStatus status);

    List<MiningAccidentIncident> findByWardAndStatus(String ward, IncidentStatus status);
}
