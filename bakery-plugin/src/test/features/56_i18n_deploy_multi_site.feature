@cucumber @bakery @i18n @i18n-deploy @i18n-deploy-multi-site
Feature: i18n deploy multi-site — 8 sites generalization

  The i18n deploy pipeline generalizes to multiple sites. Each site has its
  own copy of French articles. The pipeline translates each site from French
  into the 9 non-French supported languages, injects RTL directives for
  ar/ur, and injects the language switcher into the menu of each variant.

  A label-aware fake TranslationService stands in for the LLM so the scenario
  runs in CI without network or metered cost (Ink Economy Law). The fake
  applies a FR→EN dictionary and prefixes other languages with [lang].

  The fixture reuses the 3 articles from `cheroliv-com-i18n-deploy` (covering
  the 3 PlantUml strategies) and copies them under 3 site directories
  (cheroliv.com, cccp.education, magic-stick) to simulate the 8-site
  generalization with a representative 3-site sample.

  Background:
    Given a multi-site i18n deploy fixture with 3 sites and 3 French articles each

  Scenario: Dry-run translation leaves all 3 sites untouched
    When the multi-site pipeline dry-runs translation for all 3 sites
    Then each site should still have only French content
    And each site should have no translated adoc file under "i18n/"

  Scenario: Multi-site translation produces 9 variants per site
    When the multi-site pipeline translates all 3 sites from fr to all supported languages
    Then each site should have 9 translated variants under "i18n/"
    And each variant of each site should contain a translated version of "introduction-pivot.adoc"

  Scenario: RTL injection applies to ar and ur variants of every site
    When the multi-site pipeline translates all 3 sites from fr to all supported languages
    And the multi-site pipeline injects RTL for all supported languages on every site
    Then the "ar" variant of each site should contain ":lang: rtl"
    And the "ur" variant of each site should contain ":lang: rtl"
    And the "en" variant of each site should not contain ":lang: rtl"

  Scenario: JBake native headers preserved across all sites and languages
    When the multi-site pipeline translates all 3 sites from fr to "en"
    And the multi-site pipeline injects RTL for all supported languages on every site
    Then each site "en" article "introduction-pivot.adoc" should start with "= "
    And each site "en" article "introduction-pivot.adoc" should contain ":jbake-type: post"
    And each site "en" article "introduction-pivot.adoc" should contain ":jbake-lang: en"

  Scenario: PlantUml PreserveTechnical leaves class diagram intact on every site
    When the multi-site pipeline translates all 3 sites from fr to "en"
    Then each site "en" article "introduction-pivot.adoc" should contain "@startuml"
    And each site "en" article "introduction-pivot.adoc" should contain "class UserService"
    And each site "en" article "introduction-pivot.adoc" should not contain "[en] class UserService"

  Scenario: PlantUml BorrowVocabulary preserves REAC and AFNOR on every site
    When the multi-site pipeline translates all 3 sites from fr to "en"
    Then each site "en" article "vocabulaire-metier.adoc" should contain "REAC"
    And each site "en" article "vocabulaire-metier.adoc" should contain "AFNOR"
    And each site "en" article "vocabulaire-metier.adoc" should contain "RNCP"

  Scenario: SiteTranslationPlan computes missing languages per site
    Given a SiteTranslationPlan for site "cheroliv.com" with source "fr" and targets "en,ar,zh,es"
    When existing languages "en,zh" are already translated
    Then the plan missing languages should be "ar,es"
    And the plan should not be complete
    And the plan rtlTargets should be "ar"