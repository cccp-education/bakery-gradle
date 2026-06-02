@cucumber @bakery @jb9-e2e-thymeleaf
Feature: E2E Thymeleaf Component Rendering — BKY-JB-9

  As a bakery developer
  I want the Thymeleaf templates to render correctly through the Gradle plugin pipeline
  So that I can trust the components work end-to-end (config → plugin → template → HTML)

  Background:
    Given a new Bakery project
    And 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And 'settings.gradle.kts' set gradle portal dependencies repository with 'gradlePluginPortal'

  # ─── Google Forms Embed ─────────────────────────────────────────────

  @happy-path @google-forms @rendering
  Scenario: Google Forms embed — Rendered HTML contains iframe with formId when configured
    Given the template context has "googleFormsFormId" = "1ABC-x12345"
    And the template context has "googleFormsWidth" = "640"
    And the template context has "googleFormsHeight" = "800"
    When I render the template "google-forms"
    Then the rendered HTML should contain "1ABC-x12345"
    And the rendered HTML should contain "docs.google.com/forms"
    And the rendered HTML should contain "<iframe"
    And the rendered HTML should not contain "th:if"

  @no-config @google-forms
  Scenario: Google Forms no config — No iframe when googleForms not configured
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'googleFormsFormId'
    And the project should have a file named 'google-forms.thyme' in the site templates directory
    And the file 'site/templates/google-forms.thyme' should contain 'th:if'

  # ─── Analytics Script ───────────────────────────────────────────────

  @happy-path @analytics @rendering
  Scenario: Analytics script — Rendered HTML contains Plausible script when configured
    Given the template context has "analyticsProvider" = "plausible"
    And the template context has "analyticsDomain" = "my-site.com"
    And the template context has "analyticsScriptSrc" = "https://plausible.io/js/script.js"
    When I render the template "analytics-script"
    Then the rendered HTML should contain "data-domain"
    And the rendered HTML should contain "<script"
    And the rendered HTML should not contain "th:if"

  @no-config @analytics
  Scenario: Analytics no config — No script when analytics not configured
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'analyticsProvider'
    And the project should have a file named 'analytics-script.thyme' in the site templates directory
    And the file 'site/templates/analytics-script.thyme' should contain 'th:if'

  # ─── Newsletter Form ────────────────────────────────────────────────

  @happy-path @newsletter @rendering
  Scenario: Newsletter form — Rendered HTML contains newsletter form when enabled
    Given the template context has "newsletterEnabled" = "true"
    And the template context has "newsletterEndpoint" = "https://mailchimp.example.com/subscribe"
    When I render the template "newsletter-form"
    Then the rendered HTML should contain "newsletter-section"
    And the rendered HTML should contain "newsletter-input"
    And the rendered HTML should contain "newsletter-submit"
    And the rendered HTML should not contain "th:if"

  @no-config @newsletter
  Scenario: Newsletter no config — No newsletter form when not configured
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'newsletterEnabled'
    And the project should have a file named 'newsletter-form.thyme' in the site templates directory
    And the file 'site/templates/newsletter-form.thyme' should contain 'th:if'

  # ─── Auth Header ────────────────────────────────────────────────────

  @happy-path @auth @rendering
  Scenario: Auth header — Auth button is rendered when Firebase Auth configured
    Given the template context has "firebaseAuthApiKey" = "AIzaSyTestKey123"
    And the template context has "firebaseAuthDomain" = "my-project.firebaseapp.com"
    When I render the template "auth-header"
    Then the rendered HTML should contain "auth-btn"
    And the rendered HTML should contain "Connexion"
    And the rendered HTML should not contain "th:if"

  @no-config @auth
  Scenario: Auth header no config — No auth button when Firebase Auth not configured
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'firebaseAuthApiKey'
    And the project should have a file named 'auth-header.thyme' in the site templates directory
    And the file 'site/templates/auth-header.thyme' should contain 'th:if'

  # ─── Comments Section ───────────────────────────────────────────────

  @happy-path @comments @rendering
  Scenario: Comments section — Comments section visible when enabled
    Given the template context has "commentsEnabled" = "true"
    And the template context has "commentsCollection" = "blog-comments"
    And the nested context "content" has "uri" = "/blog/my-post"
    When I render the template "comments"
    Then the rendered HTML should contain "comments-section"
    And the rendered HTML should contain "comment-submit"
    And the rendered HTML should not contain "th:if"

  @disabled @comments
  Scenario: Comments disabled — No comments section when disabled
    And does not have 'site.yml' for site configuration
    When I am executing the task 'generateSite'
    Then the file 'site/jbake.properties' should not contain 'commentsEnabled'
    And the project should have a file named 'comments.thyme' in the site templates directory
    And the file 'site/templates/comments.thyme' should contain 'th:if'