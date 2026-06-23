@cucumber @bakery @a11y
Feature: Accessibility Audit Task — BKY-A11Y-1

  As a bakery developer
  I want an accessibility audit task that scans the baked HTML
  So that I can verify WCAG/RGAA compliance in the Gradle pipeline

  Background:
    Given a new Bakery project with site configured but without IA

  @happy-path
  Scenario: accessibilityAudit task is registered in full pipeline
    When I list the tasks in group "audit"
    Then the task "accessibilityAudit" should be registered

  @happy-path
  Scenario: accessibilityAudit produces JSON report for compliant HTML
    Given the baked directory contains "index.html" with inline contrast "#000000 on #FFFFFF"
    When I am executing the a11y task "accessibilityAudit"
    Then the build should succeed
    And the report "build/reports/accessibility-audit.json" should contain "\"compliant\": true"

  @failure-path
  Scenario: accessibilityAudit reports non-compliance for low contrast
    Given the baked directory contains "index.html" with inline contrast "#FFA500 on #FFFFFF"
    When I am executing the a11y task "accessibilityAudit"
    Then the build should succeed
    And the report "build/reports/accessibility-audit.json" should contain "\"compliant\": false"

  @error-path
  Scenario: accessibilityAudit warns when no HTML files found
    Given the baked directory is empty
    When I am executing the a11y task "accessibilityAudit"
    Then the build should succeed
    And the build output should contain "Aucun fichier HTML trouvé"
