@cucumber @bakery @i18n
Feature: Internationalisation — Langues multiples pour le site genere — BKY-I18N

  Le plugin bakery injecte `site.language={code}` dans `jbake.properties`
  a partir de la cascade ConfigResolver. Les 10 langues supportees sont :
  fr, en, zh, hi, es, ar, bn, pt, ru, ur.

  Background:
    Given a new Bakery project
    And does not have 'site.yml' for site configuration

  @default-language
  Scenario: Langue par defaut — fr quand rien n'est specifie
    When I am executing the task 'generateSite'
    Then the build should succeed
    Then the file 'site/jbake.properties' should contain 'site.language=fr'

  @en-explicit
  Scenario: Langue anglaise explicite via site.yml
    Given the site configuration contains language 'en'
    When I am executing the task 'generateSite'
    Then the build should succeed
    Then the file 'site/jbake.properties' should contain 'site.language=en'

  @zh-scaffold
  Scenario: Langue chinoise (zh) via site.yml — injection dans jbake.properties
    Given the site configuration contains language 'zh'
    When I am executing the task 'generateSite'
    Then the build should succeed
    Then the file 'site/jbake.properties' should contain 'site.language=zh'

  @ar-scaffold
  Scenario: Langue arabe (ar) — langue RTL
    Given the site configuration contains language 'ar'
    When I am executing the task 'generateSite'
    Then the build should succeed
    Then the file 'site/jbake.properties' should contain 'site.language=ar'

  @hi-scaffold
  Scenario: Langue hindi (hi) — ecriture devanagari
    Given the site configuration contains language 'hi'
    When I am executing the task 'generateSite'
    Then the build should succeed
    Then the file 'site/jbake.properties' should contain 'site.language=hi'

  @fallback-unsupported
  Scenario: Fallback fr quand langue non supportee spécifiee
    Given the site configuration contains language 'de'
    When I am executing the task 'generateSite'
    Then the build should succeed
    Then the file 'site/jbake.properties' should contain 'site.language=fr'

  @dsl-cascade
  Scenario: Surcharge DSL — language defini dans bakery DSL
    Given 'build.gradle.kts' file use 'site.yml' as the config path in the DSL
    And the bakery DSL defines language 'es'
    When I am executing the task 'generateSite'
    Then the build should succeed
    Then the file 'site/jbake.properties' should contain 'site.language=es'

  @scaffold-ia-intention-lang
  Scenario: Scaffolding IA avec intention en portugais (pt)
    Given a new Bakery project with scaffold intention configured with description "Site de tecnologia" and siteType "blog" and lang "pt"
    When I am executing the task 'tasks'
    Then the build should succeed
    And task "generateSiteFromIntention" should be registered

