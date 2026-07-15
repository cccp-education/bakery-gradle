@cucumber @bakery @pivot
Feature: AsciiDoc parser supports JBake native format

  The AsciiDoc parser must handle both pivot format (title=.../~~~~~~)
  and JBake native AsciiDoc format (= Title / @Author / :jbake-*: attributes).
  cheroliv.com blog articles use the JBake native format.

  Scenario: JBake native header is parsed into frontmatter
    Given a JBake native AsciiDoc document with title and jbake attributes
    When the parser parses the document
    Then the frontmatter title should be the document title
    And the frontmatter date should be the jbake date
    And the frontmatter type should be the jbake type
    And the frontmatter status should be the jbake status

  Scenario: JBake native body blocks are parsed correctly
    Given a JBake native AsciiDoc document with heading and source block
    When the parser parses the document
    Then the first block should be a heading at level 2
    And the second block should be a paragraph
    And the third block should be a source block with language "bash"

  Scenario: JBake native roundtrip preserves structure
    Given a JBake native AsciiDoc document with heading and paragraph
    When the parser parses and the renderer renders the article
    Then the reparsed article should have the same frontmatter title
    And the reparsed article should have the same number of blocks