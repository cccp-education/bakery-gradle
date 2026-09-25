@cucumber @bakery @i18n @variant-bake
Feature: Deployable i18n variant bake

  A site whose translations live under `i18n/{lang}/` (materialised from a
  frozen bundle, or LLM-translated) obtains a deployable `{lang}/` tree by
  baking every variant with the same JBake engine as the reference
  (BKY-LANG-NAV-8). A variant missing a reference template is skipped, never
  baked: one missing template aborts the whole JBake render (S-049).

  Background:
    Given a variant bake fixture site with languages "en" and "de"

  Scenario: A complete variant is scheduled and baked into its language directory
    When the variant bake is planned for languages "fr,en,de" with reference "fr"
    Then the bake plan is "en,de"
    When the variants are baked
    Then the "en" variant tree contains a baked "index.html"
    And the "de" variant tree contains a baked "index.html"

  Scenario: An undeployable variant is skipped, never baked
    Given the "de" variant is missing the reference template "menu.thyme"
    When the variant bake is planned for languages "fr,en,de" with reference "fr"
    Then the bake plan is "en"
    When the variants are baked
    Then the "en" variant tree contains a baked "index.html"
    And the "de" variant tree is absent

  Scenario: The reference language is never baked as a variant
    When the variant bake is planned for languages "fr,en,de" with reference "fr"
    Then the bake plan is "en,de"
    And the bake plan does not contain "fr"

  Scenario: The bake root carries the shared assets, never the variant tree
    When the variant bake is planned for languages "fr,en,de" with reference "fr"
    When the variants are baked
    Then the assembled "en" bake root carried the shared assets

  Scenario: The throwaway assembled roots are removed after the bake
    When the variant bake is planned for languages "fr,en,de" with reference "fr"
    When the variants are baked
    Then no assembled bake root remains

  Scenario: A partial corpus bakes its templates with an empty content shell
    Given the "de" variant has no content tree
    When the variant bake is planned for languages "fr,en,de" with reference "fr"
    Then the bake plan is "en,de"
    When the variants are baked
    Then the "de" variant content tree is empty
