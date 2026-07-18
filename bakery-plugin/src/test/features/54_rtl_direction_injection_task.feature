@cucumber @bakery @i18n @rtl-deploy
Feature: RTL direction injection task on translated content

  The `injectRtlDirection` task walks `content-i18n/{lang}/**/*.adoc` (the
  persistent output of `migrateContentI18n` non-dry-run — NOT `build/` which
  is wiped by `clean`, per the Ink Economy Law) and applies the
  RtlDirectionInjector to each article frontmatter. Arabic and Urdu get
  `:jbake-lang: {lang}` + `:lang: rtl`; LTR languages get `:jbake-lang: {lang}`
  and any pre-existing `:lang: rtl` is dropped.

  Scenario: Inject Arabic RTL directive into translated article
    Given a translated "arabic" article with no language directive
    When the RTL injection task processes language "ar"
    Then the article should contain ":jbake-lang: ar"
    And the article should contain ":lang: rtl"

  Scenario: Inject French LTR directive into translated article
    Given a translated "french" article with no language directive
    When the RTL injection task processes language "fr"
    Then the article should contain ":jbake-lang: fr"
    And the article should not contain ":lang: rtl"

  Scenario: Inject Urdu RTL directive into translated article
    Given a translated "urdu" article with no language directive
    When the RTL injection task processes language "ur"
    Then the article should contain ":jbake-lang: ur"
    And the article should contain ":lang: rtl"

  Scenario: RTL injection is idempotent on second invocation
    Given a translated "arabic" article with no language directive
    When the RTL injection task processes language "ar" twice
    Then the article should contain ":jbake-lang: ar"
    And the article should contain ":lang: rtl"
    And the article should contain ":jbake-lang: ar" exactly once

  Scenario: Translating RTL source to LTR target drops RTL layout
    Given a translated "arabic" article with ":lang: rtl" and ":jbake-lang: ar"
    When the RTL injection task processes language "fr"
    Then the article should contain ":jbake-lang: fr"
    And the article should not contain ":lang: rtl"

  Scenario: LTR injection preserves jbake header attributes
    Given a translated "french" article with jbake header attributes
    When the RTL injection task processes language "fr"
    Then the article should contain ":jbake-title: Article de test"
    And the article should contain ":jbake-type: post"
    And the article should contain ":jbake-status: published"

  Scenario: Injection preserves article body content
    Given a translated "arabic" article with body content
    When the RTL injection task processes language "ar"
    Then the article should contain "== Introduction"
    And the article should contain "Ceci est un paragraphe de test."