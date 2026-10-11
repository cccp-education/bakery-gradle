@template-tree
Feature: Template tree — the templates branch of the translation tree

  bakery owns the templates branch: every reference template × language has a
  status (missing / untranslated / translated) — the per-node view the unified
  translation tree needs (GBL-010 G-1). Only `VariantDeployability`'s missing half
  existed before.

  Scenario: A variant template differing from the reference is translated
    Given a reference template "footer.thyme" with the text "Bonjour"
    And a variant "en" template "footer.thyme" with the text "Hello"
    When the template tree is audited for "en"
    Then the template tree has 1 translated templates

  Scenario: A variant template identical to the reference is untranslated
    Given a reference template "header.thyme" with the text "Menu"
    And a variant "de" template "header.thyme" with the text "Menu"
    When the template tree is audited for "de"
    Then the template tree has 1 untranslated templates

  Scenario: A missing variant template is reported
    Given a reference template "archive.thyme" with the text "Archives"
    And a variant "ja" with no "archive.thyme" template
    When the template tree is audited for "ja"
    Then the template tree has 1 missing templates
