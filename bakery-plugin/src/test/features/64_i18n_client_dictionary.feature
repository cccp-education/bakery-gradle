@cucumber @bakery @i18n @i18n-client @js-client
Feature: i18n client dictionary translation — delta idempotent et economie d'encre

  The `translateI18nClient` task fills the client-side i18n dictionaries
  (`var DICT` chrome + `TALARIA.I18N.extend` patch) from the `fr` reference
  floor. Only the keys missing from a target language are sent to the
  translation service — a complete dictionary is a strict no-op (Ink Economy
  Law). A failed translation keeps the key missing and leaves the document
  byte-identical.

  After the translation, the development source (`maquette/js/`) is propagated
  byte-identically to the site publication copy (`jbake/assets/js/`, decision
  S-039). The publication copy is never written in dry-run.

  The fixture `talaria-i18n-client` owns a chrome dictionary (fr floor,
  complete en, incomplete fa) and an additive patch (empty de block). A
  label-aware fake translator marks translated content with `[lang]` — no real
  LLM is called.

  The tree may also ship the structured catalogue (`TALARIA.I18N.CATALOG`,
  `i18n-content.js`), whose values are nested and unquoted. It reuses the flat
  4-space block indentation, so it must never be mistaken for a flat dictionary
  owner nor rewritten by the flat writer. Its coverage (missing languages,
  formations, fields) is reported, not translated.

  Background:
    Given a talaria i18n client fixture with chrome and patch dictionaries

  Scenario: A complete target language is a strict no-op
    When the i18n client task translates the dictionaries from fr to "en"
    Then the i18n client task should report "0" missing keys
    And no translation request should have been sent

  Scenario: Only the missing keys of the target language are sent to the model
    When the i18n client task translates the dictionaries from fr to "fa"
    Then the i18n client task should report "1" missing keys
    And the translation service should have received exactly "1" request
    And the translation request for the key "nav.cart" should be sourced from "Panier"

  Scenario: A language block with no key is filled entirely from the floor
    When the i18n client task translates the dictionaries from fr to "de"
    Then the i18n client task should report "2" missing keys
    And the translation service should have received exactly "2" requests

  Scenario: The translated dictionary holds the translated values
    When the i18n client task translates the dictionaries from fr to "fa"
    Then the i18n client dictionary "fa" key "nav.cart" should be "«fa» Panier"

  Scenario: A failed translation keeps the key missing and the document untouched
    Given the i18n client translation service always fails
    When the i18n client task translates the dictionaries from fr to "fa"
    Then the translation service should have received exactly "1" request
    And the source dictionary should be byte-identical to the fixture

  Scenario: A re-run on a translated dictionary is idempotent
    Given the i18n client task has already translated the dictionaries from fr to "fa"
    When the i18n client task translates the dictionaries from fr to "fa" again
    Then the i18n client task should report "0" missing keys
    And the translation service should have received exactly "1" request

  Scenario: The dry-run reports the delta without writing anything
    Given the i18n client task runs in dry-run mode
    When the i18n client task translates the dictionaries from fr to "fa"
    Then the i18n client task should report "1" missing keys
    And no translation request should have been sent
    And the source dictionary should be byte-identical to the fixture

  Scenario: The development dictionary is propagated to the publication copy
    Given a stale jbake publication copy of the chrome dictionary
    When the i18n client task translates the dictionaries from fr to "fa"
    Then the jbake publication copy should be byte-identical to the maquette source

  Scenario: The propagation is a no-op on an already aligned publication copy
    Given an aligned jbake publication copy of the chrome dictionary
    When the i18n client task translates the dictionaries from fr to "fa"
    Then the jbake publication copy should be byte-identical to the maquette source

  Scenario: The dry-run never writes the publication copy
    Given a stale jbake publication copy of the chrome dictionary
    And the i18n client task runs in dry-run mode
    When the i18n client task translates the dictionaries from fr to "fa"
    Then the jbake publication copy should still hold the stale content

  Scenario: The structured catalogue never captures the flat keys
    Given a structured catalogue owning the same languages as the chrome dictionary
    When the i18n client task translates the dictionaries from fr to "fa"
    Then the flat key "nav.cart" should be written to the chrome dictionary
    And the structured catalogue should not own the flat key "nav.cart"

  Scenario: The catalogue coverage reports a missing language
    Given a structured catalogue owning a complete en block
    When the i18n client task translates the dictionaries from fr to "es"
    Then the catalogue coverage should report the missing language "es"

  Scenario: The catalogue coverage reports a formation and its fields
    Given a structured catalogue owning a partial en block
    When the i18n client task translates the dictionaries from fr to "en"
    Then the catalogue coverage should report the missing formation "en" "cda"
    And the catalogue coverage should report the missing field "en" "fpa" "modules"

  Scenario: A complete catalogue coverage is a no-op
    Given a structured catalogue owning a complete en block
    When the i18n client task translates the dictionaries from fr to "en"
    Then the catalogue coverage should have no gap

  Scenario: The structured catalogue is completed with its missing formation and fields
    Given a structured catalogue owning a partial en block
    When the i18n client task translates the dictionaries from fr to "en"
    Then the catalogue "en" should cover every reference literal
    And the catalogue literal "cda.title" of "en" should be "«en» Concepteur"
    And the catalogue literal "fpa.title" of "en" should be "Trainer"

  Scenario: An absent catalogue language is created by cloning the reference
    Given a structured catalogue owning a partial en block
    When the i18n client task translates the dictionaries from fr to "es"
    Then the catalogue "es" should cover every reference literal
    And the catalogue literal "fpa.title" of "es" should be "«es» Formateur"

  Scenario: The structured catalogue translation is idempotent
    Given a structured catalogue owning a partial en block
    And the i18n client task has already translated the dictionaries from fr to "en"
    When the i18n client task translates the dictionaries from fr to "en" again
    Then the catalogue "en" should cover every reference literal
