#cucumber @bakery @article @intention
Feature: Article IA avec intention enrichie — BKY-JB-8

  La tâche `generateArticle` accepte désormais une intention riche
  via le DSL `bakery { articleIntention { ... } }` ou via CLI
  (`-ParticleTon`, `-ParticleAudience`, `-ParticleKeywords`).

  Scenario: Article intention DSL with topic only compiles
    Given a new Bakery project with article intention configured with topic "Kotlin pour Gradle"
    When I am executing the task 'tasks'
    Then the build should succeed

  Scenario: Article intention DSL with full config compiles
    Given a new Bakery project with article intention configured with topic "Kotlin Coroutines" and ton "technique" and audience "developpeur" and keywords "suspend,flow" and lang "en"
    When I am executing the task 'tasks'
    Then the build should succeed

  Scenario: generateArticle task is registered with intention DSL
    Given a new Bakery project with article intention configured with topic "Test article"
    When I check for task "generateArticle"
    Then task "generateArticle" should be registered
    And task "generateArticle" should be in group "generate"