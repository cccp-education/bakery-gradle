@cucumber @bakery @conv-1-7
Feature: Save Configuration — BKY-CONV-1.7

  As a bakery developer
  I want the collectSiteConfig task to save push credentials into site.yml
  So that the site configuration persists across builds

  Background:
    Given a new Bakery project with fully configured site for collectSiteConfig

  Scenario: Save push credentials into site.yml via collectSiteConfig
    When I execute task "collectSiteConfig" with github username "cucumber-user" and repository "https://github.com/cucumber/repo.git" and token "cucumber-token"
    Then the file "site.yml" should contain "cucumber-user"
    And the file "site.yml" should contain "https://github.com/cucumber/repo.git"

  Scenario: Preserve existing bake configuration when saving push credentials
    When I execute task "collectSiteConfig" with github username "preserve-user" and repository "https://github.com/preserve/repo.git" and token "preserve-token"
    Then the file "site.yml" should contain "srcPath"
    And the file "site.yml" should contain "build/bake"
    And the file "site.yml" should contain "preserve-user"