@cucumber @bakery @pivot
Feature: Generation de YAML pivot depuis AsciiDoc — generatePivotYaml

  La tâche `generatePivotYaml` transforme un fichier AsciiDoc en YAML pivot
  (séparation structure/contenu éditorial). Elle est indépendante du pipeline
  JBake et n'exige pas de site.yml — disponible en mode scaffold only.

  Session 161 — CLI migré vers options natives `@Option` :
  ```
  ./gradlew generatePivotYaml --input=article.adoc --output=article.pivot.yaml
  ```
  Si `--output` est omis, la convention dérive `{stem}.pivot.yaml`.

  Scenario: generatePivotYaml task is registered without site.yml
    Given a new Bakery project without site.yml
    When I execute generatePivotYaml with input "sample.adoc" and output "sample.pivot.yaml"
    Then the build should fail with missing input "sample.adoc"

  Scenario: generatePivotYaml produces YAML from AsciiDoc input
    Given a new Bakery project without site.yml
    And an AsciiDoc input file "article.adoc" with frontmatter and heading
    When I execute generatePivotYaml with input "article.adoc" and output "pivot.yaml"
    Then the pivot build should succeed
    And the output file "pivot.yaml" should contain "article:"
    And the output file "pivot.yaml" should contain "type: heading"

  Scenario: generatePivotYaml derives default output path when --output omitted
    Given a new Bakery project without site.yml
    And an AsciiDoc input file "article.adoc" with frontmatter and heading
    When I execute generatePivotYaml with only input "article.adoc"
    Then the pivot build should succeed
    And the output file "article.pivot.yaml" should contain "article:"

  Scenario: generatePivotYaml fails when --input option is missing
    Given a new Bakery project without site.yml
    When I execute generatePivotYaml without any option
    Then the build should fail requiring option "--input"