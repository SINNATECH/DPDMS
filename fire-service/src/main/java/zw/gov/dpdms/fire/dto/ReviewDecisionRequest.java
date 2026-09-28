package zw.gov.dpdms.fire.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewDecisionRequest {

    /** Required for REJECTED and CORRECTIONS_REQUESTED; optional for APPROVED. */
    private String comment;

    public void requireCommentFor(String action) {
        if ((comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("A comment is required when you " + action + " a record");
        }
    }
}
