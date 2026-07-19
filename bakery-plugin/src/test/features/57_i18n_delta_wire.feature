@cucumber @bakery @i18n @i18n-delta-wire
Feature: i18n delta wire — SubtreeI18nPlanner cabled in MigrateContentI18nTask

  The `migrateContentI18n` task now uses `I18nDeltaApplier` to compute a
  delta between source checksums and stored checksums from a previous run.
  Only modified or new articles are translated; untouched articles are
  preserved (Ink Economy Law per leaf).

  The fixture `cheroliv-com-i18n-deploy` contains 3 French articles covering
  the 3 PlantUml strategies.

  Background:
    Given a cheroliv-com-i18n-deploy fixture with 3 French articles

  Scenario: Fresh migration translates all articles
    When the delta-wire task migrates content from fr to "en" for the first time
    Then the delta-wire task should report "3" files translated for language "en"
    And the delta-wire task should report "0" files preserved for language "en"

  Scenario: Second run with unchanged source translates nothing
    Given the delta-wire task has already migrated content from fr to "en"
    When the delta-wire task migrates content from fr to "en" again
    Then the delta-wire task should report "0" files translated for language "en"
    And the delta-wire task should report "3" files preserved for language "en"

  Scenario: Second run with one modified article translates only that one
    Given the delta-wire task has already migrated content from fr to "en"
    And the source article "introduction-pivot.adoc" is modified
    When the delta-wire task migrates content from fr to "en" again
    Then the delta-wire task should report "1" files translated for language "en"
    And the delta-wire task should report "2" files preserved for language "en"
