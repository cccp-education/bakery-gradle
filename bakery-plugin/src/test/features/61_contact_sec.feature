@bakery @contact
Feature: Contact form security — scaffold hardened contact form (BKY-CONTACT-SEC-8)

  The `scaffoldContactSec` task generates a hardened contact form
  (Turnstile, PoW, honeypot, rate limit, session token) into footer.thyme
  for each configured language, produces a hardened contact.js, and
  generates firestore.rules enforcing create-only + whitelist + caps.
  It is a no-op when the `contact:` section is absent or `enabled = false`.

  Background:
    Given a contact-sec fixture site with 2 languages "fr" and "en"
    And a contact config with endpoint "https://script.example.com/exec" and siteKey "0xTESTKEY"

  Scenario: Scaffold FR footer with hardened form
    When I run scaffoldContactSec
    Then the FR footer.thyme contains a contact form
    And the FR footer.thyme contains a honeypot field
    And the FR footer.thyme contains a session_token field
    And the FR footer.thyme contains a pow_nonce field
    And the FR footer.thyme contains a Turnstile div with sitekey "0xTESTKEY"

  Scenario: Scaffold EN footer with hardened form
    When I run scaffoldContactSec
    Then the EN footer.thyme contains a contact form
    And the EN footer.thyme contains a honeypot field

  Scenario: No-op without contact config
    Given a contact-sec fixture site without a contact config
    When I run scaffoldContactSec
    Then the build succeeds
    And no contact form is scaffolded

  Scenario: Firestore rules are generated and conform
    When I run scaffoldContactSec
    Then firestore.rules are generated at "site/firestore.rules"
    And the firestore rules allow create only
    And the firestore rules enforce honeypot empty
    And the firestore rules enforce created_at equals request.time
    And the firestore rules enforce whitelist of allowed fields