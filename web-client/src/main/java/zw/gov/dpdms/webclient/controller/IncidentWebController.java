package zw.gov.dpdms.webclient.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import zw.gov.dpdms.webclient.service.GatewayClient;
import zw.gov.dpdms.webclient.util.FieldDef;
import zw.gov.dpdms.webclient.util.HazardConfig;
import zw.gov.dpdms.webclient.util.SessionKeys;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class IncidentWebController {

    private final GatewayClient gatewayClient;

    @GetMapping("/incidents")
    public String list(@RequestParam(required = false) String hazard,
                        HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        String sessionHazard = (String) session.getAttribute(SessionKeys.HAZARD);
        String role = (String) session.getAttribute(SessionKeys.ROLE);

        // RECORDER/SUPERVISOR are locked to their own hazard; NATIONAL/ADMIN
        // have none on their token and pick one via the dropdown instead.
        String effectiveHazard = sessionHazard != null ? sessionHazard
                : (hazard != null ? hazard : "FLOOD");

        String hazardPath = HazardConfig.API_PATH.get(effectiveHazard);
        boolean onlyApproved = "NATIONAL".equals(role);

        try {
            List<Map<String, Object>> incidents =
                    gatewayClient.list(hazardPath, token(session), onlyApproved);
            model.addAttribute("incidents", incidents);
        } catch (HttpStatusCodeException ex) {
            model.addAttribute("error", gatewayClient.extractErrorMessage(ex));
            model.addAttribute("incidents", List.of());
        }

        model.addAttribute("effectiveHazard", effectiveHazard);
        model.addAttribute("hazardOptions", HazardConfig.DISPLAY_NAME);
        model.addAttribute("canPickHazard", sessionHazard == null);
        model.addAttribute("canCreate", "RECORDER".equals(role) || "ADMIN".equals(role));
        model.addAttribute("canReview", "SUPERVISOR".equals(role) || "ADMIN".equals(role));
        addIdentityToModel(session, model);
        return "incidents";
    }

    @GetMapping("/incidents/new")
    public String newForm(HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        String hazard = (String) session.getAttribute(SessionKeys.HAZARD);
        if (hazard == null) {
            model.addAttribute("error", "Only a ward recorder can create an incident.");
            return "redirect:/incidents";
        }

        model.addAttribute("hazard", hazard);
        model.addAttribute("hazardLabel", HazardConfig.DISPLAY_NAME.get(hazard));
        model.addAttribute("fields", HazardConfig.FIELDS.get(hazard));
        addIdentityToModel(session, model);
        return "incident-form";
    }

    @PostMapping("/incidents")
    public String create(@RequestParam Map<String, String> formParams,
                          HttpSession session, Model model) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        String hazard = (String) session.getAttribute(SessionKeys.HAZARD);
        String hazardPath = HazardConfig.API_PATH.get(hazard);
        List<FieldDef> fields = HazardConfig.FIELDS.get(hazard);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("district", formParams.get("district"));
        body.put("province", formParams.get("province"));
        body.put("occurredAt", formParams.get("occurredAt"));
        body.put("severity", formParams.get("severity"));
        body.put("latitude", parseDouble(formParams.get("latitude")));
        body.put("longitude", parseDouble(formParams.get("longitude")));

        for (FieldDef field : fields) {
            String raw = formParams.get(field.name());
            body.put(field.name(), switch (field.type()) {
                case "integer" -> parseInteger(raw);
                case "decimal" -> parseDouble(raw);
                case "checkbox" -> raw != null; // present in form params only when checked
                default -> raw;
            });
        }

        try {
            gatewayClient.create(hazardPath, token(session), body);
            return "redirect:/incidents";
        } catch (HttpStatusCodeException ex) {
            model.addAttribute("error", gatewayClient.extractErrorMessage(ex));
            model.addAttribute("hazard", hazard);
            model.addAttribute("hazardLabel", HazardConfig.DISPLAY_NAME.get(hazard));
            model.addAttribute("fields", fields);
            addIdentityToModel(session, model);
            return "incident-form";
        }
    }

    @PostMapping("/incidents/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam String hazard, HttpSession session) {
        return doReview(session, hazard, id, "approve", null);
    }

    @PostMapping("/incidents/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam String hazard,
                          @RequestParam String comment, HttpSession session) {
        return doReview(session, hazard, id, "reject", comment);
    }

    @PostMapping("/incidents/{id}/request-corrections")
    public String requestCorrections(@PathVariable Long id, @RequestParam String hazard,
                                      @RequestParam String comment, HttpSession session) {
        return doReview(session, hazard, id, "request-corrections", comment);
    }

    private String doReview(HttpSession session, String hazard, Long id, String action, String comment) {
        String redirect = requireLogin(session);
        if (redirect != null) return redirect;

        String hazardPath = HazardConfig.API_PATH.get(hazard);
        try {
            switch (action) {
                case "approve" -> gatewayClient.approve(hazardPath, token(session), id);
                case "reject" -> gatewayClient.reject(hazardPath, token(session), id, comment);
                case "request-corrections" -> gatewayClient.requestCorrections(hazardPath, token(session), id, comment);
            }
        } catch (HttpStatusCodeException ignored) {
            // Deliberately swallowed for this MVP — the list page reloads either way;
            // a fuller build would flash the error message back to the user here.
        }
        return "redirect:/incidents?hazard=" + hazard;
    }

    private void addIdentityToModel(HttpSession session, Model model) {
        model.addAttribute("username", session.getAttribute(SessionKeys.USERNAME));
        model.addAttribute("role", session.getAttribute(SessionKeys.ROLE));
    }

    private String requireLogin(HttpSession session) {
        return session.getAttribute(SessionKeys.TOKEN) == null ? "redirect:/login" : null;
    }

    private String token(HttpSession session) {
        return (String) session.getAttribute(SessionKeys.TOKEN);
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Double.valueOf(raw);
    }

    private Integer parseInteger(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return Integer.valueOf(raw);
    }
}
