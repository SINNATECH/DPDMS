package zw.gov.dpdms.alert.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import zw.gov.dpdms.alert.model.AlertLog;

import java.util.List;

public interface AlertLogRepository extends JpaRepository<AlertLog, Long> {

    List<AlertLog> findTop200ByOrderByCreatedAtDesc();

    List<AlertLog> findTop200ByHazardTypeOrderByCreatedAtDesc(String hazardType);
}
