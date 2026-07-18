@cucumber @bakery @i18n @i18n-deploy @i18n-deploy-e2e
Feature: i18n deploy end-to-end — 10 languages pipeline

  The `migrateContentI18n` task translates AsciiDoc articles from a French
  source into the 10 supported languages (ar, bn, en, es, hi, pt, ru, ur, zh)
  using a label-aware fake translator (no LLM call in CI). After translation
  the `injectRtlDirection` task injects `:jbake-lang:` + `:lang: rtl` into
  RTL variants (ar, ur) and the `injectLangSwitch` task injects the language
  switcher fragment into `menu.thyme` for each variant.

  The fixture `cheroliv-com-i18n-deploy` contains 3 French articles covering
  the 3 PlantUml strategies: PreserveTechnical (class diagram, no labels),
  TranslateLabels (use case diagram with human labels) and BorrowVocabulary
  (REAC/AFNOR/DC/RNCP borrowed terms).

  Background:
    Given a cheroliv-com-i18n-deploy fixture with 3 French articles and 10 supported languages

  Scenario: FR to EN translation preserves JBake native headers and translates body
    When the deploy pipeline translates the fixture from fr to "en"
    And the deploy pipeline injects RTL for language "en"
    Then the translated "en" article "introduction-pivot.adoc" should start with "= "
    And the translated "en" article "introduction-pivot.adoc" should contain ":jbake-type: post"
    And the translated "en" article "introduction-pivot.adoc" should not contain ":lang: rtl"
    And the translated "en" article "introduction-pivot.adoc" should contain ":jbake-lang: en"
    And the translated "en" article "introduction-pivot.adoc" should contain "This is a paragraph in French"

  Scenario: FR to AR translation injects RTL directive and jbake-lang
    When the deploy pipeline translates the fixture from fr to "ar"
    And the deploy pipeline injects RTL for language "ar"
    Then the translated "ar" article "introduction-pivot.adoc" should contain ":jbake-lang: ar"
    And the translated "ar" article "introduction-pivot.adoc" should contain ":lang: rtl"
    And the translated "ar" article "introduction-pivot.adoc" should start with "= "

  Scenario: FR to ZH translation injects jbake-lang only (LTR)
    When the deploy pipeline translates the fixture from fr to "zh"
    And the deploy pipeline injects RTL for language "zh"
    Then the translated "zh" article "introduction-pivot.adoc" should contain ":jbake-lang: zh"
    And the translated "zh" article "introduction-pivot.adoc" should not contain ":lang: rtl"

  Scenario: PlantUml PreserveTechnical leaves a class diagram intact
    When the deploy pipeline translates the fixture from fr to "en"
    Then the translated "en" article "introduction-pivot.adoc" should contain "@startuml"
    And the translated "en" article "introduction-pivot.adoc" should contain "class UserService"
    And the translated "en" article "introduction-pivot.adoc" should contain "UserService --> AuthService"

  Scenario: PlantUml TranslateLabels translates human labels and keeps structure
    When the deploy pipeline translates the fixture from fr to "en"
    Then the translated "en" article "diagramme-labels.adoc" should contain "@startuml"
    And the translated "en" article "diagramme-labels.adoc" should contain "@enduml"
    And the translated "en" article "diagramme-labels.adoc" should not contain "Utilisateur"
    And the translated "en" article "diagramme-labels.adoc" should not contain "Administrateur"

  Scenario: PlantUml BorrowVocabulary preserves REAC and AFNOR verbatim
    When the deploy pipeline translates the fixture from fr to "en"
    Then the translated "en" article "vocabulaire-metier.adoc" should contain "REAC"
    And the translated "en" article "vocabulaire-metier.adoc" should contain "AFNOR"
    And the translated "en" article "vocabulaire-metier.adoc" should contain "RNCP"

  Scenario: End-to-end 10 languages pipeline produces 10 localized variants
    When the deploy pipeline translates the fixture from fr to all supported languages
    And the deploy pipeline injects RTL for all supported languages
    And the language switcher is injected into the FR menu
    Then the fixture should have 9 translated variants under "i18n/"
    And each variant should contain a translated version of "introduction-pivot.adoc"
    And the "ar" variant should contain ":lang: rtl"
    And the "ur" variant should contain ":lang: rtl"
    And the "en" variant should not contain ":lang: rtl"
    And the FR menu should contain a link to "en/index.html" for language "en"
    And the FR menu should contain a link to "ar/index.html" for language "ar"