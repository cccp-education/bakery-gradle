@cucumber @bakery @pivot @i18n
Feature: JBake native format translation preserves header structure

  The content translation service must detect JBake native AsciiDoc
  format (= Title / @Author / :jbake-*: attributes) and use the
  JbakeNativeRenderer to preserve the header structure while translating
  the body. Pivot format articles keep using the AsciiDocRenderer.

  Scenario: JBake native article is translated with native renderer
    Given a JBake native AsciiDoc article with header and body
    When the content translation service translates it from fr to en
    Then the translated article should use JBake native format
    And the translated header should start with the document title
    And the translated body should be in the target language

  Scenario: Pivot format article still uses pivot renderer
    Given a pivot format AsciiDoc article with title and body
    When the content translation service translates it from fr to en
    Then the translated article should use pivot format
    And the translated header should contain the tilde separator

  Scenario: JBake native roundtrip through translation preserves structure
    Given a JBake native AsciiDoc article with tags and summary attributes
    When the content translation service translates it from fr to en
    Then the translated article should preserve jbake tags attribute
    And the translated article should preserve summary attribute