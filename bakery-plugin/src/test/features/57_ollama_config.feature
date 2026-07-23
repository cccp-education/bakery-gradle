@bakery @ollama @i18n-ci
Feature: Ollama Device Keys in site.yml — CI/CD translation rotation (BKY-I18N-CI)

  `SiteConfiguration.ollama` holds the Ollama Cloud Device Keys (SSH ed25519)
  for all 29 instance ports (11437-11465). A single GitHub secret
  `CHEROLIV_COM_CONFIG` carries the entire site.yml including all Device Keys.
  Rotation is between batches: after N articles, switch to next Device Key.

  Security: Device Key private keys are NEVER logged. The `toString()` on
  `OllamaDeviceKey` masks the private key. `maskSecret(DeviceKey)` produces
  `ssh-ed25519***[N chars]`.

  Scenario: A site.yml without ollama section has null ollama config
    Given a site.yml without ollama section
    When I parse the site configuration for ollama
    Then the ollama config is null
    And isConfigured returns false by default

  Scenario: A site.yml with 29 device keys parses correctly
    Given a site.yml with 29 ollama device keys for ports 11437 to 11465
    When I parse the site configuration for ollama
    Then the ollama config is present
    And isConfigured returns true
    And the device keys list has 29 entries
    And the model is "gemma4:31b-cloud"
    And portStart is 11437
    And portEnd is 11465
    And the first device key name is "ollama-11437"
    And the last device key name is "ollama-11465"

  Scenario: Device key toString never leaks the private key
    Given an Ollama device key with private key "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAI-super-secret-data"
    When I call toString on the device key
    Then the output contains "keyName"
    And the output contains "ssh-ed25519***["
    And the output does not contain "super-secret-data"
    And the output does not contain "AAAAC3Nza"

  Scenario: maskSecret on DeviceKey produces safe output for logs
    Given an Ollama device key with private key "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAILongSecret"
    When I mask the private key as a SecretField.DeviceKey
    Then the masked value starts with "ssh-ed25519***"
    And the masked value does not contain "LongSecret"
    And the masked value does not contain "AAAAC3Nza"
