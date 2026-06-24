@bakery @tree
Feature: Site tree — Content wrapper over PivotModel (BKY-TREE-3)

  `Content` wraps a `PivotArticle` (the AsciiDoc AST built in S157-163) and
  exposes it as the leaf of the editorial tree. `Article` carries an optional
  `Content` — an article can exist without content (scaffold skeleton).
  `Content` exposes `blocs()`, `inlineTexts()` (all inline recursively), and
  `translatableSegments()` (filtered via `TextTranslatableClassifier`).
  `PivotModel` is never modified.

  Scenario: An article without content has a null content
    Given an article "formations/ab-partition" without content
    Then the article content is null

  Scenario: An article with content exposes its pivot AST
    Given an article "formations/ab-partition" with a 3-block content
    Then the content has 3 blocks
    And the first block is a heading

  Scenario: inlineTexts collects inline segments from paragraphs
    Given an article "formations/ab-partition" with a 2-segment paragraph
    Then the content has 2 inline segments

  Scenario: translatableSegments filters non-translatable segments
    Given an article "formations/ab-partition" with a mixed translatable and technical paragraph
    Then the content has 1 translatable segment

  Scenario: An article with a nested table exposes all inline segments
    Given an article "formations/ab-partition" with a 2-column 1-row table
    Then the content has 4 inline segments

  Scenario: An empty content has no blocks and no segments
    Given an article "formations/ab-partition" with an empty content
    Then the content has 0 blocks
    And the content has 0 inline segments
    And the content has 0 translatable segments