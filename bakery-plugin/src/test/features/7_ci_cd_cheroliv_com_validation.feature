@cucumber @bakery @personal
Feature: CI/CD cheroliv.com — Validation end-to-end sans exposition de credentials

  Ce fichier simule exactement le workflow du pipeline CI/CD (deploy-cheroliv-com.yml)
  mais avec des credentials vides dans site.yml pour ne jamais exposer les secrets.
  Le tag @personal garantit que ces scenarios ne s'executent QUE dans mon environnement
  ou ils ont acces a la source jbake reelle dans office/sites/cheroliv.com/jbake/.

  Ces scenarios sont exclus par defaut (voir CucumberTestRunner). Pour les lancer :
    ./gradlew cucumberTest -Dcucumber.filter.tags="@personal"

  Scénario 1 — bake sans credentials (le coeur du build)
  Scénario 2 — verification des tasks enregistrees
  Scénario 3 — validation du SEO (Articles connexes)


  Scenario: Le build bake de cheroliv.com reussit avec des credentials vides
    Given the cheroliv.com jbake source in the engine-style project
    And a "site.yml" with bake config and empty credentials
    When I run the "bake" task for cheroliv.com
    Then the build should succeed
    And the baked output should contain "index.html"
    And the baked output should contain "blog/2026/0119_benchmark_dgx_spark_vs_cloud_abonnement_llm_post.html"

  Scenario: La tache deploySite est enregistree pour cheroliv.com
    Given the cheroliv.com jbake source in the engine-style project
    And a "site.yml" with bake config and empty credentials
    When I run the "tasks" task for cheroliv.com
    Then the task "deploySite" should be listed in the output

  Scenario: Les articles generes contiennent le bloc SEO 'Articles connexes'
    Given the cheroliv.com jbake source in the engine-style project
    And a "site.yml" with bake config and empty credentials
    When I run the "bake" task for cheroliv.com
    Then the baked article "blog/2026/0119_benchmark_dgx_spark_vs_cloud_abonnement_llm_post.html" should contain "Articles connexes"
    And the baked article "blog/2025/0080_kitty_term_post.html" should contain "Mis à jour"
