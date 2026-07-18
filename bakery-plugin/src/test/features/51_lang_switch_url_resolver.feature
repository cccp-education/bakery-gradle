Feature: Language Switcher URL Resolver
  As a site visitor
  I want the language switcher to point to the correct language URL
  So that I can navigate between languages without self-loops or broken links

  Background:
    Given a lang-switch fixture site with 2 languages "fr" and "en"
    And the default language is "fr"

  Scenario: FR root page links to EN subdir
    When I inject the language switcher into the FR site
    Then the menu in "site/templates/menu.thyme" should contain a link to "en/index.html" for language "en"
    And the menu in "site/templates/menu.thyme" should not contain a self-loop for language "fr"
    And the active language should be "fr"

  Scenario: EN subdir page links back to FR root
    When I inject the language switcher into the EN site
    Then the menu in "site/en/templates/menu.thyme" should contain a link to "../index.html" for language "fr"
    And the menu in "site/en/templates/menu.thyme" should not contain "en/index.html" for language "en"
    And the active language should be "en"

  Scenario: EN subdir page links to another non-default language
    Given a lang-switch fixture site with 3 languages "fr", "en", and "ar"
    When I inject the language switcher into the EN site
    Then the menu in "site/en/templates/menu.thyme" should contain a link to "../ar/index.html" for language "ar"

  Scenario: No self-loop on EN page
    When I inject the language switcher into the EN site
    Then the menu in "site/en/templates/menu.thyme" should not contain "en/index.html" anywhere in lang-switcher links
    And the link for language "en" should resolve to "index.html"

  Scenario: Active class is set on current language
    When I inject the language switcher into the FR site
    Then the lang-option for language "fr" should have class "active"
    And the lang-option for language "en" should not have class "active"