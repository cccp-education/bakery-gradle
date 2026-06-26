@bakery @i18n @i18n-real @migration
Feature: Real i18n migration of 3 sites on copies — BKY-I18N-REAL

  Validate that i18n migration produces a deterministic result, frozen by
  golden master, and that idempotence (Ink Economy Law) is respected on
  the 3 real sites (magic-stick, cccp-education, cheroliv-com).

  Background:
    Given a post-migration snapshot loader is available

  @magic-stick-golden-master
  Scenario: Magic-stick migration matches the post-migration golden master
    Given a copy of pre-migration fixture "magic-stick"
    When the fixture is migrated with languages "fr,en" and defaultLanguage "fr"
    And english translations from "i18n-fixtures/magic-stick/translations_en.properties" are applied
    Then the migrated templates should match the golden master for "magic-stick"
    And messages_fr.properties should match the golden master for "magic-stick"
    And messages_en.properties should match the golden master for "magic-stick"

  @cccp-education-golden-master
  Scenario: cccp-education migration matches the post-migration golden master
    Given a copy of pre-migration fixture "cccp-education"
    When the fixture is migrated with languages "fr,en" and defaultLanguage "fr"
    And english translations from "i18n-fixtures/cccp-education/translations_en.properties" are applied
    Then the migrated templates should match the golden master for "cccp-education"
    And messages_fr.properties should match the golden master for "cccp-education"
    And messages_en.properties should match the golden master for "cccp-education"

  @cheroliv-com-golden-master
  Scenario: cheroliv-com migration matches the post-migration golden master
    Given a copy of pre-migration fixture "cheroliv-com"
    When the fixture is migrated with languages "fr,en" and defaultLanguage "fr"
    And english translations from "i18n-fixtures/cheroliv-com/translations_en.properties" are applied
    Then the migrated templates should match the golden master for "cheroliv-com"
    And messages_fr.properties should match the golden master for "cheroliv-com"
    And messages_en.properties should match the golden master for "cheroliv-com"

  @magic-stick-idempotence
  Scenario: Re-migrating magic-stick already migrated produces zero changes
    Given a copy of pre-migration fixture "magic-stick"
    When the fixture is migrated with languages "fr,en" and defaultLanguage "fr"
    And english translations from "i18n-fixtures/magic-stick/translations_en.properties" are applied
    And the already migrated fixture is migrated again
    Then the second migration should extract 0 keys
    And the second migration should modify 0 templates
    And the templates should be unchanged after re-migration

  @cccp-education-idempotence
  Scenario: Re-migrating cccp-education already migrated produces zero changes
    Given a copy of pre-migration fixture "cccp-education"
    When the fixture is migrated with languages "fr,en" and defaultLanguage "fr"
    And english translations from "i18n-fixtures/cccp-education/translations_en.properties" are applied
    And the already migrated fixture is migrated again
    Then the second migration should extract 0 keys
    And the second migration should modify 0 templates
    And the templates should be unchanged after re-migration

  @cheroliv-com-idempotence
  Scenario: Re-migrating cheroliv-com already migrated produces zero changes
    Given a copy of pre-migration fixture "cheroliv-com"
    When the fixture is migrated with languages "fr,en" and defaultLanguage "fr"
    And english translations from "i18n-fixtures/cheroliv-com/translations_en.properties" are applied
    And the already migrated fixture is migrated again
    Then the second migration should extract 0 keys
    And the second migration should modify 0 templates
    And the templates should be unchanged after re-migration