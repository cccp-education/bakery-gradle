@cucumber @bakery @search
Feature: Pagefind search index
  In order to ship a per-language search
  As a site consumer
  I want the Pagefind bundle to cover every staged language

  Scenario: A bundle with one independent index per language is valid
    Given a Pagefind entry with indexes for "fr" and "en"
    When the index is verified against the staged languages "fr" and "en"
    Then no search index violation is reported

  Scenario: A staged language without an index is reported
    Given a Pagefind entry with an index for "fr"
    When the index is verified against the staged languages "fr" and "en"
    Then the search index reports "missing-language" for "en"

  Scenario: An index that indexed zero pages is reported
    Given a Pagefind entry with a zero-page index for "fr"
    When the index is verified against the staged language "fr"
    Then the search index reports "empty-language" for "fr"

  Scenario: A variant bundle path reaches the shared root
    When the bundle script is resolved from "../"
    Then the bundle script path is "../pagefind/pagefind.js"
