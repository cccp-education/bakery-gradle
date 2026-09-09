@cucumber @bakery @i18n @i18n-mass @source-path
Feature: migrateContentI18n source path resolution

  The `migrateContentI18n` task resolves the `contentI18nSource` path against
  the content root derived from `bake.srcPath`. A path may be given either
  relative to the content root (`blog/2026`) or already prefixed with the
  content root (`content/blog/2026`). Both must resolve to the same directory
  — the prefix is never applied twice.

  Background:
    Given a content root "content" in the project dir

  Scenario: source path relative to the content root resolves inside the content root
    When the source path "blog/2026" is resolved
    Then the resolved path equals the project dir joined with "content/blog/2026"

  Scenario: source path prefixed with the content root does not double the prefix
    When the source path "content/blog/2026" is resolved
    Then the resolved path equals the project dir joined with "content/blog/2026"

  Scenario: source path equal to the content root resolves to the content root
    When the source path "content" is resolved
    Then the resolved path equals the project dir joined with "content"

  Scenario: an absolute source path is used as-is
    When the source path "/tmp/external/blog" is resolved
    Then the resolved path equals "/tmp/external/blog"

  Scenario: a path relative to the content root is preserved when srcPath differs
    Given a content root "jbake" in the project dir
    When the source path "content/blog" is resolved
    Then the resolved path equals the project dir joined with "jbake/content/blog"