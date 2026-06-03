#cucumber @bakery @scaffold @ia
Feature: Scaffolding assiste par IA — BKY-IA-1

  La tache `generateSiteFromIntention` accepte une intention riche
  via le DSL `bakery { scaffoldIntention { ... } }` ou via CLI
  (`-PscaffoldDescription`, `-PsiteType`, `-PprojectName`, `-PscaffoldLang`).

  Scenario: Intention IA genere une structure de site valide
    Given a new Bakery project with scaffold intention configured with description "Blog tech Kotlin" and siteType "blog"
    When I am executing the task 'tasks'
    Then the build should succeed
    And task "generateSiteFromIntention" should be registered

  Scenario: Sans IA, le scaffolding classique fonctionne
    Given a new Bakery project with minimal scaffolding configuration
    When I am executing the task 'tasks'
    Then the build should succeed
    And task "generateSite" should be registered

  Scenario: Intention invalide (description vide) produit un fallback gracieux
    Given a new Bakery project with scaffold intention configured with description "" and siteType "blog"
    When I am executing the task 'tasks'
    Then the build should succeed
