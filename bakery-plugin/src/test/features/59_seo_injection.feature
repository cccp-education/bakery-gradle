@bakery @seo
Feature: SEO injection — canonical, hreflang, OG, JSON-LD, 404, sitemap (BKY-SEO-7)

  The `injectSeo` task injects SEO tags (canonical, hreflang, Open Graph,
  Twitter Card, JSON-LD) into header.thyme for each configured language,
  generates a sitemap.xml with hreflang xhtml:link alternates, and is a
  no-op when the `seo:` section is absent from site.yml.

  Background:
    Given a seo fixture site with 2 languages "fr" and "en"
    And a seo config with siteName "Example" and websiteUrl "https://example.com"

  Scenario: Home FR page has canonical and hreflang
    When I run injectSeo
    Then the FR header.thyme contains a canonical link
    And the FR header.thyme contains hreflang "fr"
    And the FR header.thyme contains hreflang "en"
    And the FR header.thyme contains hreflang "x-default"

  Scenario: Home EN page has canonical and hreflang
    When I run injectSeo
    Then the EN header.thyme contains a canonical link
    And the EN header.thyme contains hreflang "fr"
    And the EN header.thyme contains hreflang "en"

  Scenario: OG image defaults to defaultOgImage on home page
    When I run injectSeo
    Then the FR header.thyme contains og:image with the default image
    And the FR header.thyme contains og:site_name "Example"

  Scenario: JSON-LD @graph with WebSite is rendered
    When I run injectSeo
    Then the FR header.thyme contains a JSON-LD script
    And the JSON-LD contains a WebSite node with name "Example"

  Scenario: Meta description is rendered
    When I run injectSeo
    Then the FR header.thyme contains a meta description tag

  Scenario: Sitemap contains root url and hreflang alternates
    When I run injectSeo
    Then a sitemap.xml is generated at "site/sitemap.xml"
    And the sitemap contains a root url with loc "https://example.com/"
    And the sitemap contains hreflang "fr" pointing to "https://example.com/"
    And the sitemap contains hreflang "en" pointing to "https://example.com/en/"
    And the sitemap contains hreflang "x-default" pointing to "https://example.com/"