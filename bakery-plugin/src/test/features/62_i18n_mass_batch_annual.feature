@cucumber @bakery @i18n @i18n-mass @batch-annual
Feature: i18n mass batch annual — delta inter-lots et idempotence

  The `migrateContentI18n` task supports a batch-by-year strategy where
  each invocation targets a single year subdirectory (`blog/{year}`).
  The global checksum store (`.bakery-checksums.properties` per lang dir)
  preserves articles already translated by previous lots — the delta
  inter-lots is the Ink Economy Law applied across lot boundaries.

  The fixture `cheroliv-com-mass-batch` contains 2 annual lots (2025 and
  2026), each with 2 short French articles. A label-aware fake translator
  marks translated content with `[lang]` — no real LLM is called.

  Background:
    Given a mass-batch fixture with 2 annual lots and 4 French articles

  Scenario: Fresh lot 2025 translates 2 articles and preserves none
    When the mass-batch task migrates lot "2025" from fr to "en"
    Then the mass-batch task should report "2" files translated for language "en"
    And the mass-batch task should report "0" files preserved for language "en"

  Scenario: Subsequent lot 2026 translates 2 new articles and preserves lot 2025
    Given the mass-batch task has already migrated lot "2025" from fr to "en"
    When the mass-batch task migrates lot "2026" from fr to "en"
    Then the mass-batch task should report "2" files translated for language "en"
    And the mass-batch task should report "2" files preserved for language "en"

  Scenario: Re-running lot 2026 translates nothing (idempotence intra-lot)
    Given the mass-batch task has already migrated lot "2025" from fr to "en"
    And the mass-batch task has already migrated lot "2026" from fr to "en"
    When the mass-batch task migrates lot "2026" from fr to "en" again
    Then the mass-batch task should report "0" files translated for language "en"
    And the mass-batch task should report "4" files preserved for language "en"

  Scenario: Re-running lot 2025 after lot 2026 translates nothing (idempotence inter-lots)
    Given the mass-batch task has already migrated lot "2025" from fr to "en"
    And the mass-batch task has already migrated lot "2026" from fr to "en"
    When the mass-batch task migrates lot "2025" from fr to "en" again
    Then the mass-batch task should report "0" files translated for language "en"
    And the mass-batch task should report "4" files preserved for language "en"

  Scenario: Translated articles contain the language marker
    Given the mass-batch task has already migrated lot "2025" from fr to "en"
    Then the mass-batch "en" article "article-2025-a.adoc" should contain "[en]"
    And the mass-batch "en" article "article-2025-b.adoc" should contain "[en]"

  Scenario: JBake native headers are preserved on translated articles
    Given the mass-batch task has already migrated lot "2025" from fr to "en"
    Then the mass-batch "en" article "article-2025-a.adoc" should contain ":jbake-type: post"
    And the mass-batch "en" article "article-2025-a.adoc" should contain ":jbake-status: published"