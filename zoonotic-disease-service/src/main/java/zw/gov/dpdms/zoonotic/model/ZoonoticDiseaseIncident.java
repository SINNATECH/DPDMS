package zw.gov.dpdms.zoonotic.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single zoonoticDisease incident record.
 *
 * Fields split into three groups, matching the assignment brief:
 *  1. Shared incident metadata (identical shape across all five hazards —
 *     keep this block byte-for-byte consistent when you clone this class).
 *  2. GPS coordinates (mandatory on every hazard record).
 *  3. ZoonoticDisease-specific indicators (the five required fields for this hazard).
 */
@Entity
@Table(name = "zoonotic_disease_incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoonoticDiseaseIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---- Shared incident metadata ----
    @Column(nullable = false)
    private String ward;

    @Column(nullable = false)
    private String district;

    @Column(nullable = false)
    private String province;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private String reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    // ---- GPS coordinates (mandatory for every hazard) ----
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // ---- Zoonotic disease-specific indicators (exactly the five required) ----
    @Column(nullable = false)
    private String pathogenName;

    @Column(nullable = false)
    private String animalSpeciesAffected;

    @Column(nullable = false)
    private Integer confirmedHumanCases;

    @Column(nullable = false)
    private Integer confirmedAnimalCases;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventClassification classification;

    // ---- Approval workflow audit ----
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) {
            status = IncidentStatus.PENDING;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
