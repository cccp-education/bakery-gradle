@cucumber @bakery @i18n @templates
Feature: i18n template translation — swap de copies completes et convergence

  The `translateTemplates` task produces the fully translated copy of each
  Thymeleaf template that the CI swaps in place of `jbake/` (jbake-core:2.7.0
  installs no MessageResolver, so `#{key}` bundles would never resolve).

  Only the visible text is sent to the model: a bare text node mixed with inline
  markup is translated, tag attributes and `<script>`/`<style>` bodies are not.
  A template already translated is preserved; a template byte-identical to the
  French reference — or a legacy variant whose structural drift kept it French —
  is scheduled (Ink Economy Law).

  Background:
    Given a template translation fixture with a French reference
    And a template translating service that prefixes the target language

  Scenario: A bare text node mixed with a span is translated
    When I translate the template fixture into "en"
    Then the translated template contains "EN:Développeur"
    And the translated template contains "EN:spécialisé en Ingénierie Pédagogique"
    And the translated template preserves the class "display-5 fw-bold mb-5"

  Scenario: Tag attributes and script bodies are never translated
    When I translate the template fixture into "en"
    Then the translated template preserves the attribute "data-lang="
    And the translated template preserves the script body

  Scenario: The contact form placeholder is translated
    Given a contact form fixture with a French placeholder
    When I translate the contact form into "en"
    Then the translated contact form contains "EN:Nom"
    And the translated contact form contains "EN:Votre message"
    And the translated contact form preserves the attribute "name="

  Scenario: Structural attributes are never translated
    Given a contact form fixture with a French placeholder
    When I translate the contact form into "en"
    Then the translated contact form preserves the attribute "data-lang="
    And the translated contact form preserves the attribute "class="

  Scenario: A preserved variant repairs only its still-French attributes
    Given a translated variant whose placeholder is still French
    When I repair the variant attributes into "en"
    Then the repaired variant contains "EN:Nom"
    And the repaired variant preserves the already translated "Your message"

  Scenario: A converged variant is a strict no-op on the second repair
    Given a translated variant whose placeholder is still French
    When I repair the variant attributes into "en"
    And I repair the variant attributes into "en" again
    Then the repaired variant is unchanged by the second repair

  Scenario: An already translated variant has nothing to repair
    Given a translated variant whose placeholder is already translated
    When I repair the variant attributes into "en"
    Then the repaired variant reports nothing pending

  Scenario: A preserved variant repairs only its still-French visible text
    Given a translated variant whose visible text is still French
    When I repair the variant text into "en"
    Then the repaired variant contains "EN:Envoyer le Message"
    And the repaired variant preserves the already translated "Send the Message"

  Scenario: A converged variant is a strict no-op on the second text repair
    Given a translated variant whose visible text is still French
    When I repair the variant text into "en"
    And I repair the variant text into "en" again
    Then the repaired variant is unchanged by the second repair

  Scenario: A variant with neutral visible text reports nothing to repair
    Given a translated variant whose visible text is neutral
    When I repair the variant text into "en"
    Then the repaired variant reports nothing pending

  Scenario: A variant with fully translated text reports nothing to repair
    Given a translated variant whose visible text is already translated
    When I repair the variant text into "en"
    Then the repaired variant reports nothing pending

  Scenario: The language switcher block is never sent to the model
    Given a template carrying the generated language switcher block
    When I translate the template fixture into "en"
    Then the translated template preserves the language name "English"
    And the translated template preserves the language name "Français"

  Scenario: A template without a switcher block is translated normally
    When I translate the template fixture into "en"
    Then the translated template contains "EN:Développeur"

  Scenario: A missing template is scheduled for a new language
    Given a template variant with only the translated hero
    When I plan the templates for "de"
    Then the template plan contains "blog.thyme"
    And the template plan does not contain "hero.thyme"

  Scenario: A legacy variant is fully regenerated when forced
    Given a template variant with only the translated hero
    When I plan the templates for "de" forced
    Then the template plan contains "hero.thyme"

  Scenario: A complete variant is a strict no-op
    Given a template variant with every template translated
    When I plan the templates for "de"
    Then the template plan is empty

  Scenario: The html lang attribute of a translated template declares the target language
    When I align the html lang of the fixture into "de"
    Then the aligned template declares the html lang "de"

  Scenario: The html lang alignment never touches hreflang alternates
    When I align the html lang of the fixture into "de"
    Then the aligned template preserves the attribute "hreflang="

  Scenario: Re-running the html lang alignment is a strict no-op
    When I align the html lang of the fixture into "fr"
    Then the aligned template declares the html lang "fr"
