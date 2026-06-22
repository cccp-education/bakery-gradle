@pipeline-contracts @contracts @unit-contracts
Feature: Pipeline Contracts — Conventional commits and release notes

  As a bakery developer
  I want release notes generation contracts validated end-to-end
  So that conventional commits, config, parser, renderer and generator behave consistently

  Scenario: ConventionalCommit holds all fields including scope
    Given a repository with conventional commits
    When a ConventionalCommit of type "feat" with scope "api" and message "add login endpoint" is created
    Then the commit type is "feat"
    And the commit scope is "api"
    And the commit message is "add login endpoint"
    And the commit hash is not empty
    And the commit date is not empty

  Scenario: ConventionalCommit supports absent scope
    Given a repository with conventional commits
    When a ConventionalCommit of type "fix" with no scope and message "fix null pointer" is created
    Then the commit type is "fix"
    And the commit scope is absent
    And the commit message is "fix null pointer"

  Scenario: ReleaseNotesConfig exposes default categories and renderer type
    When a default ReleaseNotesConfig is created
    Then the config has 7 categories
    And the category "feat" maps to "Nouveautés"
    And the category "fix" maps to "Corrections"
    And the default renderer type is "asciidoc"

  Scenario: ReleaseNotesConfig honors fromTag and toTag
    When a ReleaseNotesConfig is created with fromTag "v1.0.0" and toTag "v1.1.0"
    Then the fromTag is "v1.0.0"
    And the toTag is "v1.1.0"

  Scenario: ReleaseNotesConfig honors renderer type override
    When a ReleaseNotesConfig is created with rendererType "json"
    Then the renderer type is "json"

  Scenario: GitLogParser returns the requested commit count
    When a GitLogParser implementation returns 3 commits between "v1.0.0" and "v1.1.0"
    Then the parser returns 3 commits
    And the first commit type is "feat"

  Scenario: GitLogParser detects version from project directory
    When a GitLogParser implementation detects version "1.2.3" from project directory
    Then the detected version is "1.2.3"

  Scenario: ReleaseNotesRenderer declares its format
    When a ReleaseNotesRenderer implementation declares format "markdown"
    Then the renderer format is "markdown"

  Scenario: ReleaseNotesRenderer renders commits for a version
    When a ReleaseNotesRenderer renders 2 commits for version "1.1.0"
    Then the rendered string contains "Release 1.1.0"
    And the rendered string contains the first commit message

  Scenario: ReleaseNotesRenderer writes rendered output to a file
    When a ReleaseNotesRenderer writes 1 commit to a file
    Then the file exists
    And the file contains the commit message

  Scenario: ReleaseNotesGenerator produces a release notes file
    When a ReleaseNotesGenerator generates release notes for version "1.0.0"
    Then the output file exists
    And the output file contains "Release 1.0.0"
    And the output file contains all commit messages
