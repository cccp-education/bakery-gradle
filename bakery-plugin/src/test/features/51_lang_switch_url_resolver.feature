Feature: Language Switcher URL Resolver
  As a site visitor
  I want the language switcher to point at the translation of the page I am on
  So that I stay on the same page when I change language, instead of being sent home

  Background:
    Given a lang-switch fixture site with 2 languages "fr" and "en"
    And the default language is "fr"

  Scenario: FR root page links to the EN tree, page-aware
    When I inject the language switcher into the FR site
    Then the menu in "site/templates/menu.thyme" should contain a page-aware link to language "en"
    And the menu in "site/templates/menu.thyme" should not contain "en/index.html" anywhere in lang-switcher links
    And the active language should be "fr"

  Scenario: EN subdir page links back to FR root, page-aware
    When I inject the language switcher into the EN site
    Then the menu in "site/en/templates/menu.thyme" should contain a page-aware link to language "fr"
    And the menu in "site/en/templates/menu.thyme" should not contain "en/index.html" for language "en"
    And the active language should be "en"

  Scenario: EN subdir page links to another non-default language, page-aware
    Given a lang-switch fixture site with 3 languages "fr", "en", and "ar"
    When I inject the language switcher into the EN site
    Then the menu in "site/en/templates/menu.thyme" should contain a page-aware link to language "ar"

  Scenario: No self-loop on EN page
    When I inject the language switcher into the EN site
    Then the menu in "site/en/templates/menu.thyme" should not contain "en/index.html" anywhere in lang-switcher links
    And the lang-option for language "en" should point at the current page

  Scenario: Active class is set on current language
    When I inject the language switcher into the FR site
    Then the lang-option for language "fr" should have class "active"
    And the lang-option for language "en" should not have class "active"

  Scenario: A materialized i18n variant receives the page-aware switcher
    Given a lang-switch fixture site with 2 languages "fr" and "en" materialized under i18n
    And the default language is "fr"
    When I inject the language switcher into the EN site
    Then the menu in "site/i18n/en/templates/menu.thyme" should contain a page-aware link to language "fr"
    And the menu in "site/i18n/en/templates/menu.thyme" should not contain "en/index.html" anywhere in lang-switcher links
