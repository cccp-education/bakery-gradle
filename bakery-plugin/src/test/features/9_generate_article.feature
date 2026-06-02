#noinspection CucumberUndefinedStep
@cucumber @bakery @article
Feature: Generation d'article assistée IA — generateArticle

  La tâche `generateArticle` utilise le service LlmService configuré
  via le DSL `bakery { ia { ... } }` pour générer un article AsciiDoc
  et l'injecter dans `content/blog/YYYY/MM/`.

  Quand ia.enabled = false (par défaut), la tâche est enregistrée
  mais n'a pas de LlmService injecté — elle échoue avec un message clair.

  Scenario: generateArticle task is registered without IA config
    Given a new Bakery project with site configured but without IA
    When I check for task "generateArticle"
    Then task "generateArticle" should be registered
    And task "generateArticle" should be in group "generate"

  Scenario: generateArticle fails gracefully without IA config
    Given a new Bakery project with site configured but without IA
    When I execute task "generateArticle" with topic "Test"
    Then the build should fail
    And the output should contain "LlmService"
