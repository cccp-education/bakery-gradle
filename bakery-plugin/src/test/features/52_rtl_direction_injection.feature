@cucumber @bakery @i18n @rtl-deploy
Feature: RTL direction injection into translated article frontmatter

  The RtlDirectionInjector adds JBake language directive into the
  asciidocAttributes of a translated article frontmatter. Arabic and Urdu
  are RTL languages and get `:lang: rtl` injected; LTR languages get any
  pre-existing `:lang: rtl` removed so a source RTL article translated to
  a LTR language drops RTL layout.

  Background:
    Given a pivot frontmatter with no language directive

  Scenario: Inject Arabic RTL directive
    When the RTL direction injector injects language "ar"
    Then the frontmatter asciidocAttributes should contain "jbake-lang" with value "ar"
    And the frontmatter asciidocAttributes should contain "lang" with value "rtl"

  Scenario: Inject Urdu RTL directive
    When the RTL direction injector injects language "ur"
    Then the frontmatter asciidocAttributes should contain "jbake-lang" with value "ur"
    And the frontmatter asciidocAttributes should contain "lang" with value "rtl"

  Scenario: Inject French LTR directive removes any RTL marker
    When the RTL direction injector injects language "fr"
    Then the frontmatter asciidocAttributes should contain "jbake-lang" with value "fr"
    And the frontmatter asciidocAttributes should not contain "lang"

  Scenario: Inject English LTR directive
    When the RTL direction injector injects language "en"
    Then the frontmatter asciidocAttributes should contain "jbake-lang" with value "en"
    And the frontmatter asciidocAttributes should not contain "lang"

  Scenario: Inject Chinese LTR directive
    When the RTL direction injector injects language "zh"
    Then the frontmatter asciidocAttributes should contain "jbake-lang" with value "zh"
    And the frontmatter asciidocAttributes should not contain "lang"

  Scenario: Unknown language is a no-op
    When the RTL direction injector injects language "xx"
    Then the frontmatter should be unchanged

  Scenario: Idempotent injection for Arabic
    When the RTL direction injector injects language "ar" twice
    Then the frontmatter asciidocAttributes should contain "jbake-lang" with value "ar"
    And the frontmatter asciidocAttributes should contain "lang" with value "rtl"

  Scenario: Translating RTL source to LTR target drops RTL layout
    Given a pivot frontmatter with "lang" set to "rtl" and "jbake-lang" set to "ar"
    When the RTL direction injector injects language "fr"
    Then the frontmatter asciidocAttributes should contain "jbake-lang" with value "fr"
    And the frontmatter asciidocAttributes should not contain "lang"

  Scenario: Re-injecting overwrites previous jbake-lang value
    Given a pivot frontmatter with "jbake-lang" set to "fr"
    When the RTL direction injector injects language "ar"
    Then the frontmatter asciidocAttributes should contain "jbake-lang" with value "ar"
    And the frontmatter asciidocAttributes should contain "lang" with value "rtl"