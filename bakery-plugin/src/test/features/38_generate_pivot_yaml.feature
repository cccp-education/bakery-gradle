@cucumber @bakery @pivot
Feature: Generation de YAML pivot depuis AsciiDoc — generatePivotYaml

  La tâche `generatePivotYaml` transforme un fichier AsciiDoc en YAML pivot
  (séparation structure/contenu éditorial). Elle est indépendante du pipeline
  JBake et n'exige pas de site.yml — disponible en mode scaffold only.

  Usage:
  ```
  ./gradlew generatePivotYaml -Pinput=article.adoc -Poutput=article.pivot.yaml
  ```

  Scenario: generatePivotYaml task is registered without site.yml
    Given a new Bakery project without site.yml
    When I execute generatePivotYaml with input "sample.adoc" and output "sample.pivot.yaml"
    Then the build should fail with missing input "sample.adoc"

  Scenario: generatePivotYaml produces YAML from AsciiDoc input
    Given a new Bakery project without site.yml
    And an AsciiDoc input file "article.adoc" with frontmatter and heading
    When I execute generatePivotYaml with input "article.adoc" and output "pivot.yaml"
    Then the build should succeed
    And the output file "pivot.yaml" should contain "article:"
    And the output file "pivot.yaml" should contain "type: heading"