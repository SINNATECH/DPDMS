HAZARD SERVICES -> ALERT-SERVICE WIRING PATCH
=============================================

Each of the five hazard services needs the same three files. The folder
layout in this patch mirrors your projects exactly, so you can copy each
service's folder over the matching project.

For every service (flood, drought, fire, zoonotic-disease, mining-accident):

  NEW      .../<pkg>/alert/AlertNotifier.java
  NEW      .../<pkg>/config/AlertClientConfig.java
  REPLACED .../<pkg>/controller/<Hazard>IncidentController.java
           (the only change: approve() now calls alertNotifier after approving)

HOW TO APPLY
  1. Copy each service folder in this patch over your existing project
     folder (say "yes" to merge / replace when Windows asks).
  2. In Eclipse: select all five projects -> press F5 (Refresh).
  3. Restart each hazard service.

NOTHING ELSE CHANGES: no pom.xml edits, no application.yml edits, and the
existing unit tests are unaffected.

OPTIONAL: if you change ALERT_INTERNAL_KEY, set the SAME value in alert-service
and in all five hazard services. The default matches everywhere, so for local
development you don't need to set it at all.
