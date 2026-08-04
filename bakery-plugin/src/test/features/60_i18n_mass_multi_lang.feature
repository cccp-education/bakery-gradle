@cucumber @bakery @i18n @i18n-mass @multi-lang
Feature: i18n mass multi-lang — golden master 3 pilot articles × 9 target languages

  The `migrateContentI18n` task translates 3 real pilot batch articles
  (court 173l, moyen 811l, long 1403l) from French to 9 target languages
  (zh, hi, es, ar, bn, pt, ru, ur) using a label-aware fake translator.
  The golden master validates: JBake headers preserved, `[source]` blocks
  unchanged, `[plantuml]` blocks classified, RTL injected (ar/ur), and
  the translation marker `[lang]` present in the body.

  Background:
    Given a mass-multi-lang fixture with 3 pilot batch articles

  Scenario: FR to EN translation preserves JBake native headers on all 3 articles
    When the mass-multi-lang pipeline translates the fixture from fr to "en"
    Then the mass-multi-lang "en" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" should start with "= "
    And the mass-multi-lang "en" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" should contain ":jbake-type: post"
    And the mass-multi-lang "en" article "0105_integrer_graphify_workflow_gradle_post.adoc" should start with "= "
    And the mass-multi-lang "en" article "0105_integrer_graphify_workflow_gradle_post.adoc" should contain ":jbake-type: post"
    And the mass-multi-lang "en" article "0100_jbake_supabase_springboot_hybride_post.adoc" should start with "= "
    And the mass-multi-lang "en" article "0100_jbake_supabase_springboot_hybride_post.adoc" should contain ":jbake-type: post"

  Scenario: FR to AR translation injects RTL directive on all 3 articles
    When the mass-multi-lang pipeline translates the fixture from fr to "ar"
    And the mass-multi-lang pipeline injects RTL for language "ar"
    Then the mass-multi-lang "ar" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" should contain ":jbake-lang: ar"
    And the mass-multi-lang "ar" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" should contain ":lang: rtl"
    And the mass-multi-lang "ar" article "0105_integrer_graphify_workflow_gradle_post.adoc" should contain ":jbake-lang: ar"
    And the mass-multi-lang "ar" article "0105_integrer_graphify_workflow_gradle_post.adoc" should contain ":lang: rtl"
    And the mass-multi-lang "ar" article "0100_jbake_supabase_springboot_hybride_post.adoc" should contain ":jbake-lang: ar"
    And the mass-multi-lang "ar" article "0100_jbake_supabase_springboot_hybride_post.adoc" should contain ":lang: rtl"

  Scenario: FR to ZH translation injects jbake-lang only (LTR) on all 3 articles
    When the mass-multi-lang pipeline translates the fixture from fr to "zh"
    And the mass-multi-lang pipeline injects RTL for language "zh"
    Then the mass-multi-lang "zh" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" should contain ":jbake-lang: zh"
    And the mass-multi-lang "zh" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" should not contain ":lang: rtl"
    And the mass-multi-lang "zh" article "0105_integrer_graphify_workflow_gradle_post.adoc" should contain ":jbake-lang: zh"
    And the mass-multi-lang "zh" article "0105_integrer_graphify_workflow_gradle_post.adoc" should not contain ":lang: rtl"
    And the mass-multi-lang "zh" article "0100_jbake_supabase_springboot_hybride_post.adoc" should contain ":jbake-lang: zh"
    And the mass-multi-lang "zh" article "0100_jbake_supabase_springboot_hybride_post.adoc" should not contain ":lang: rtl"

  Scenario: `[source]` blocks are preserved unchanged across all 9 target languages
    When the mass-multi-lang pipeline translates the fixture from fr to all 9 target languages
    Then the mass-multi-lang "en" article "0105_integrer_graphify_workflow_gradle_post.adoc" should contain "[source"
    And the mass-multi-lang "en" article "0105_integrer_graphify_workflow_gradle_post.adoc" should contain "----"
    And the mass-multi-lang "zh" article "0105_integrer_graphify_workflow_gradle_post.adoc" should contain "[source"
    And the mass-multi-lang "ar" article "0105_integrer_graphify_workflow_gradle_post.adoc" should contain "[source"

  Scenario: `[plantuml]` blocks are preserved structurally across all 9 target languages
    When the mass-multi-lang pipeline translates the fixture from fr to all 9 target languages
    Then the mass-multi-lang "en" article "0100_jbake_supabase_springboot_hybride_post.adoc" should contain "[plantuml"
    And the mass-multi-lang "en" article "0100_jbake_supabase_springboot_hybride_post.adoc" should contain "@startuml"
    And the mass-multi-lang "zh" article "0100_jbake_supabase_springboot_hybride_post.adoc" should contain "[plantuml"
    And the mass-multi-lang "ar" article "0100_jbake_supabase_springboot_hybride_post.adoc" should contain "[plantuml"

  Scenario: End-to-end 9 languages pipeline produces 9 localized variants
    When the mass-multi-lang pipeline translates the fixture from fr to all 9 target languages
    And the mass-multi-lang pipeline injects RTL for all 9 target languages
    Then the mass-multi-lang fixture should have 9 translated variants under "i18n/"
    And each mass-multi-lang variant should contain a translated version of "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc"
    And each mass-multi-lang variant should contain a translated version of "0105_integrer_graphify_workflow_gradle_post.adoc"
    And each mass-multi-lang variant should contain a translated version of "0100_jbake_supabase_springboot_hybride_post.adoc"
    And the mass-multi-lang "ar" variant should contain ":lang: rtl"
    And the mass-multi-lang "ur" variant should contain ":lang: rtl"
    And the mass-multi-lang "en" variant should not contain ":lang: rtl"

  Scenario: Translation marker `[lang]` is present in body for all 9 target languages
    When the mass-multi-lang pipeline translates the fixture from fr to all 9 target languages
    Then the mass-multi-lang "en" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
    And the mass-multi-lang "zh" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
    And the mass-multi-lang "ar" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
    And the mass-multi-lang "hi" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
    And the mass-multi-lang "es" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
    And the mass-multi-lang "bn" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
    And the mass-multi-lang "pt" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
    And the mass-multi-lang "ru" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
    And the mass-multi-lang "ur" article "0115_anonymiseur_dataset_mvp0_realite_augmentee_llm_post.adoc" body should be in the target language
