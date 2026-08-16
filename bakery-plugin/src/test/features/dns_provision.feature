@bakery @dns
Feature: DNS provisioning — idempotent reconciliation (BKY-DNS-5)

  The `DnsProvisioner` reconciles the desired records declared in the
  `dns:` section of site.yml against the live zone through a `DnsProvider`.
  It creates missing records, updates drifting ones, deletes orphans only
  when the purge flag is set, and is a no-op when the zone already matches
  the desired state. An absent dns section means no provisioning at all.
  A value drift is reconciled as create + delete (DnsDiff semantics, S198).

  Scenario: No-op when no dns section is declared
    Given a site configuration without a dns section
    Then no DNS reconciliation is performed
    And the desired records are empty

  Scenario: Create a missing www CNAME record
    Given a zone without records
    And desired records
      | type  | name | value                  | ttl  |
      | CNAME | www  | pages-content.github.io. | 3600 |
    When I reconcile the desired records in dry run
    Then the plan has 1 create
    And the created record is CNAME "www" pointing to "pages-content.github.io."

  Scenario: Create 4 missing apex A records
    Given a zone without records
    And desired records
      | type | name | value           | ttl  |
      | A    | @    | 185.199.108.153 | 3600 |
      | A    | @    | 185.199.109.153 | 3600 |
      | A    | @    | 185.199.110.153 | 3600 |
      | A    | @    | 185.199.111.153 | 3600 |
    When I reconcile the desired records in dry run
    Then the plan has 4 creates
    And all creates target apex A records

  Scenario: Update an existing record when its TTL drifts
    Given a zone with an existing record
      | type | name | value           | ttl |
      | A    | @    | 185.199.108.153 | 60  |
    And desired records
      | type | name | value           | ttl  |
      | A    | @    | 185.199.108.153 | 3600 |
    When I reconcile the desired records in dry run
    Then the plan has 1 update
    And the update keeps the existing record id

  Scenario: Replace a record whose value changed
    Given a zone with an existing record
      | type | name | value           | ttl  |
      | A    | @    | 185.199.108.153 | 3600 |
    And desired records
      | type | name | value           | ttl  |
      | A    | @    | 185.199.109.153 | 3600 |
    When I reconcile the desired records in dry run
    Then the plan has 1 create and 1 delete

  Scenario: Delete an orphan record only with the purge flag
    Given a zone with an existing record
      | type | name | value         | ttl  |
      | TXT  | _spf | v=spf1 -all  | 3600 |
    And desired records
      | type | name | value                  | ttl  |
      | CNAME | www | pages-content.github.io. | 3600 |
    When I reconcile the desired records in dry run
    Then the plan has 1 delete
    And the delete is skipped in dry run
    When I reconcile and apply the desired records with delete allowed
    Then the orphan record is removed from the zone

  Scenario: Idempotence — a second run after applying is a no-op
    Given a zone without records
    And desired records
      | type | name | value                  | ttl  |
      | CNAME | www | pages-content.github.io. | 3600 |
    When I reconcile and apply the desired records with delete allowed
    Then the plan has 1 create
    And the created record is CNAME "www" pointing to "pages-content.github.io."
    When I reconcile the desired records again in dry run
    Then the plan is empty
    And the reconciliation is a no-op

  Scenario: Verify mapping — the zone matches the desired records after a full reconciliation
    Given a zone without records
    And desired records
      | type | name | value           | ttl  |
      | A    | @    | 185.199.108.153 | 3600 |
      | A    | @    | 185.199.109.153 | 3600 |
      | A    | @    | 185.199.110.153 | 3600 |
      | A    | @    | 185.199.111.153 | 3600 |
      | CNAME | www | pages-content.github.io. | 3600 |
    When I reconcile and apply the desired records with delete allowed
    Then the zone matches the desired records exactly