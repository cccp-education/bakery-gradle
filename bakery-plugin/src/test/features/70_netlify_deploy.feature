@netlify
Feature: Netlify deployment target
  The baked payload is target-neutral: the same tree is shipped to Netlify, an
  unchanged digest is skipped before any network call, and GitHub Pages
  artifacts never travel to another provider.

  Scenario: an unchanged payload is skipped
    Given a netlify baked site with a home page
    And a previously deployed netlify digest for the same site
    When I plan a netlify deployment for site "site-123"
    Then the netlify plan is skipped
    And the netlify plan does not upload

  Scenario: a payload whose digest belongs to another site is deployed
    Given a netlify baked site with a home page
    And a previously deployed netlify digest for another site
    When I plan a netlify deployment for site "site-123"
    Then the netlify plan is deployed

  Scenario: a dry run never uploads
    Given a netlify baked site with a home page
    When I plan a netlify dry run for site "site-123"
    Then the netlify plan is deployed
    And the netlify plan does not upload

  Scenario: GitHub Pages artifacts are not shipped to Netlify
    Given a netlify baked site with a home page
    And a netlify CNAME file
    And a netlify editor backup file
    When I collect the netlify payload
    Then the netlify payload contains "index.html"
    And the netlify payload does not contain "CNAME"
    And the netlify payload does not contain "old.bak"

  Scenario: language variants and the search index are shipped untouched
    Given a netlify baked site with a home page
    And a netlify language variant "en"
    And a netlify search index
    When I collect the netlify payload
    Then the netlify payload contains "en/index.html"
    And the netlify payload contains "pagefind/pagefind.js"

  Scenario: a missing site id fails the plan
    Given a netlify baked site with a home page
    When I plan a netlify deployment for site ""
    Then the netlify plan fails with "siteId"
