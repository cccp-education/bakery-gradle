@bakery @seo @locale
Feature: Localised SEO head of the variant pages (BKY-OUT-1)

  The `localizeSeoHead` task repairs the locale-blind SEO fields of every
  variant head: `canonical` and `og:url` carry the `{lang}/` prefix, `og:locale`
  is the variant locale, and `og:image` reads the variant card directory when it
  exists (reference fallback otherwise). Deterministic, idempotent, no LLM.

  Scenario: A variant declares its own canonical and og url
    Given a locale head fixture for the language "en"
    When the head is localised for "en" with og images for "en"
    Then the head canonical is prefixed with "en/"
    And the head og url is prefixed with "en/"
    And the head declares og locale "en_US"

  Scenario: A variant reads its own og images
    Given a locale head fixture for the language "en"
    When the head is localised for "en" with og images for "en"
    Then the head og image reads "og-images/en/"
    And the head og image does not read "og-images/fr/"

  Scenario: A language without cards uses the reference cards
    Given a locale head fixture for the language "de"
    When the head is localised for "de" with og images for "fr"
    Then the head og image reads "og-images/fr/"
    And the head declares og locale "de_DE"

  Scenario: The reference language keeps an auto-referent canonical
    Given a locale head fixture for the language "fr"
    When the head is localised for "fr" with og images for "fr"
    Then the head canonical is not prefixed
    And the head declares og locale "fr_FR"

  Scenario: Localising twice is a no-op
    Given a locale head fixture for the language "en"
    When the head is localised for "en" with og images for "en"
    And the head is localised for "en" with og images for "en"
    Then the head is unchanged

  Scenario: A head without SEO fields is untouched
    Given a head without canonical, og url, og locale and og image
    When the head is localised for "en" with og images for "en"
    Then the head is unchanged
