@cucumber @bakery @lang-switch @same-page
Feature: Same-page language switch end to end

  A visitor changing language must land on the translation of the page they
  were reading, never on the home page of the target language (BKY-LANG-NAV,
  decision S2 + hybrid). The single rule `LangSwitchPath` is projected on two
  hosts: the injected shared menu (`injectLangSwitch`, NAV-2) and the ex-nihilo
  model menu (NAV-4). This feature proves both hosts resolve the *same* nested
  page at build time, against the same shared vectors (anti split-brain, D3).

  The fixture bakes every language tree from its own root (FR at the site root,
  EN under `en/`), so `content.uri` is the page path within the tree and
  `content.rootpath` ascends to that tree root — exactly the model the NAV-2
  expression assumes.

  Background:
    Given a same-page fixture site with a French page "blog/foo.html" and its English twin

  Scenario: The injected FR menu keeps the nested page when switching to English
    When the same-page switcher is injected into both language trees
    Then the "fr" tree link for page "blog/foo.html" to language "en" resolves to "../en/blog/foo.html"
    And the "fr" tree link for page "blog/foo.html" to language "en" does not fall back to home

  Scenario: The injected EN menu returns to the French translation of the same page
    When the same-page switcher is injected into both language trees
    Then the "en" tree link for page "en/blog/foo.html" to language "fr" resolves to "../../blog/foo.html"
    And the "en" tree link for page "en/blog/foo.html" to language "fr" does not fall back to home

  Scenario: No self-loop: the active language points at the current file
    When the same-page switcher is injected into both language trees
    Then the "en" tree self link for page "en/blog/foo.html" resolves to the file name "foo.html"
    And the active language of the "en" tree is "en"

  Scenario: Every injected link matches the shared same-page rule
    When the same-page switcher is injected into both language trees
    Then every link of the "en" tree for page "en/blog/foo.html" matches the shared same-page rule
    And every link of the "fr" tree for page "blog/foo.html" matches the shared same-page rule

  Scenario: The ex-nihilo model menu resolves the same page as the injected host
    When the same-page switcher is injected into both language trees
    Then the shipped model menu for page "en/blog/foo.html" and language "en" resolves language "fr" to "../../blog/foo.html"
    And the shipped model menu for page "blog/foo.html" and language "fr" resolves language "en" to "../en/blog/foo.html"
    And the shipped model menu for page "en/blog/foo.html" and language "en" matches the shared same-page rule

  Scenario: The two hosts agree on every language of a nested page
    When the same-page switcher is injected into both language trees
    Then the injected "en" host and the shipped model menu agree for page "en/blog/foo.html"
    And the injected "fr" host and the shipped model menu agree for page "blog/foo.html"
