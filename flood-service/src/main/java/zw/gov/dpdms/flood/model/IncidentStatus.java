package zw.gov.dpdms.flood.model;

/**
 * The approval workflow state machine shared by every hazard service.
 *
 * PENDING --(supervisor approves)--> APPROVED
 * PENDING --(supervisor rejects)---> REJECTED
 * PENDING --(supervisor requests corrections)--> CORRECTIONS_REQUESTED
 * CORRECTIONS_REQUESTED --(recorder resubmits)--> PENDING
 *
 * Only APPROVED records may appear on the dashboard, the map, or in
 * generated reports. See FloodIncidentService for the guarded
 * transition logic — never set this field directly from a controller.
 */
public enum IncidentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CORRECTIONS_REQUESTED
}
