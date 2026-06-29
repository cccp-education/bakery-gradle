@bakery @tree @page-assets
Feature: Page assets — AssetRef and PageAssets (BKY-OUTPUT-3)

  `AssetRef` is a rich asset descriptor with `path`, optional `integrity` hash,
  `async` and `defer` flags for JS loading strategy. `PageAssets` groups CSS
  and JS `AssetRef` lists and supports merge inheritance along the site tree.

  Scenario: An asset ref with path only
    Given an asset ref "style.css"
    Then the asset path is "style.css"
    And the asset has no integrity hash
    And the asset is not async
    And the asset is not deferred

  Scenario: An asset ref with integrity hash
    Given an asset ref "app.js" with integrity "sha256-abc123"
    Then the asset path is "app.js"
    And the asset integrity is "sha256-abc123"

  Scenario: A JS asset ref with defer flag
    Given an asset ref "analytics.js" with defer
    Then the asset path is "analytics.js"
    And the asset is deferred

  Scenario: A JS asset ref with async flag
    Given an asset ref "widget.js" with async
    Then the asset path is "widget.js"
    And the asset is async

  Scenario: Page assets with CSS only
    Given page assets with css "theme.css" "print.css"
    Then the page assets have 2 css entries
    And the page assets have no js entries

  Scenario: Page assets with JS and CSS
    Given page assets with css "style.css" and js "app.js" "analytics.js"
    Then the page assets have 1 css entries
    And the page assets have 2 js entries

  Scenario: An output config carries page assets
    Given an output config with page assets css "article.css" and js "defer.js" with defer
    Then the output config has 1 css assets
    And the output config has 1 js assets
    And the js asset is deferred

  Scenario: Merge page assets: child css overrides parent css
    Given parent page assets with css "parent.css"
    And child page assets with css "child.css"
    When I merge child into parent
    Then the merged css is "child.css"
    And the merged js is null

  Scenario: Merge page assets: child inherits parent js
    Given parent page assets with js "analytics.js"
    And child page assets with css "article.css"
    When I merge child into parent
    Then the merged css is "article.css"
    And the merged js is "analytics.js"
