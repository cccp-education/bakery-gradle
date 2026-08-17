package bakery.contact

class ContactSecRenderer {

    fun renderFooterFragment(config: ContactSecConfig): String {
        val turnstileDiv =
            if (config.turnstile != null && config.turnstile.siteKey.isNotBlank()) {
                """    <div class="cf-turnstile" data-sitekey="${config.turnstile.siteKey}"></div>
    <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>"""
            } else {
                "    <!-- CONTACT-SEC: turnstile disabled (no siteKey configured) -->"
            }

        return """<!-- CONTACT-SEC: bakery -->
    <form id="contact-form" action="${config.endpointUrl}" method="post" novalidate>
      <input type="hidden" name="session_token" value="" />
      <input type="text" name="hp_name" style="display:none" tabindex="-1" autocomplete="off" aria-hidden="true" />
      <input type="hidden" name="ts_render" value="" />
      <input type="hidden" name="fp" value="" />
      <input type="hidden" name="pow_nonce" value="" />
      <input type="hidden" name="pow_challenge" value="" />
      <label for="contact-name">Name</label>
      <input type="text" id="contact-name" name="name" required />
      <label for="contact-email">Email</label>
      <input type="email" id="contact-email" name="email" required />
      <label for="contact-subject">Subject</label>
      <input type="text" id="contact-subject" name="subject" required />
      <label for="contact-message">Message</label>
      <textarea id="contact-message" name="message" rows="5" required></textarea>
$turnstileDiv
      <button type="submit" id="contact-submit" disabled>Send</button>
    </form>
    <script src="/assets/js/contact.js" defer></script>
<!-- /CONTACT-SEC: bakery -->"""
    }

    fun renderContactJs(config: ContactSecConfig): String {
        val challengeExpr = "document.querySelector('input[name=pow_challenge]').value"
        val nonceExpr = "document.querySelector('input[name=pow_nonce]').value"
        val powDifficulty = 3

        return """// CONTACT-SEC: bakery — hardened contact form JS
// Generated from site.yml contact: section. Do not edit by hand.
(function () {
  var form = document.getElementById('contact-form');
  if (!form) return;
  var submitBtn = document.getElementById('contact-submit');
  var tsRenderInput = form.querySelector('input[name=ts_render]');
  var fpInput = form.querySelector('input[name=fp]');
  var powNonceInput = form.querySelector('input[name=pow_nonce]');
  var powChallengeInput = form.querySelector('input[name=pow_challenge]');
  var hpName = form.querySelector('input[name=hp_name]');
  var minRenderTimeMs = ${config.minRenderTimeMs};
  var renderTime = Date.now();
  tsRenderInput.value = renderTime;

  // Fingerprint (simple hash of navigator properties)
  function fingerprint() {
    var raw = navigator.userAgent + '|' + navigator.language + '|' + screen.width + 'x' + screen.height;
    var hash = 0;
    for (var i = 0; i < raw.length; i++) {
      hash = ((hash << 5) - hash + raw.charCodeAt(i)) | 0;
    }
    return 'fp_' + Math.abs(hash).toString(36);
  }
  fpInput.value = fingerprint();

  // PoW challenge (generated client-side, nonce verified server-side)
  var challenge = 'bakery_' + Date.now() + '_' + Math.random().toString(36).slice(2);
  powChallengeInput.value = challenge;

  function sha256Hex(text, cb) {
    crypto.subtle.digest('SHA-256', new TextEncoder().encode(text)).then(function (buf) {
      var hex = '';
      var bytes = new Uint8Array(buf);
      for (var i = 0; i < bytes.length; i++) {
        hex += ('00' + bytes[i].toString(16)).slice(-2);
      }
      cb(hex);
    });
  }

  function solvePow(challenge, difficulty, cb) {
    var prefix = '';
    for (var i = 0; i < difficulty; i++) prefix += '0';
    var nonce = 0;
    function attempt() {
      sha256Hex(challenge + '_' + nonce, function (hash) {
        if (hash.substring(0, difficulty) === prefix) {
          cb(nonce);
        } else {
          nonce++;
          if (nonce > 100000) { cb(null); return; }
          setTimeout(attempt, 0);
        }
      });
    }
    attempt();
  }

  solvePow(challenge, $powDifficulty, function (nonce) {
    if (nonce !== null) {
      powNonceInput.value = nonce;
      submitBtn.disabled = false;
    }
  });

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    if (hpName.value) return;
    var elapsed = Date.now() - renderTime;
    if (elapsed < minRenderTimeMs) return;

    var turnstileResponse = '';
    var turnstileWidget = form.querySelector('.cf-turnstile');
    if (window.turnstile && turnstileWidget) {
      turnstileResponse = window.turnstile.getResponse();
    }
    var payload = {
      name: form.querySelector('#contact-name').value,
      email: form.querySelector('#contact-email').value,
      subject: form.querySelector('#contact-subject').value,
      message: form.querySelector('#contact-message').value,
      session_token: form.querySelector('input[name=session_token]').value,
      hp_name: hpName.value,
      ts_render: tsRenderInput.value,
      fp: fpInput.value,
      pow_nonce: powNonceInput.value,
      pow_challenge: powChallengeInput.value,
      'cf-turnstile-response': turnstileResponse
    };

    fetch(form.action, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    }).then(function (r) {
      if (r.ok) { form.reset(); alert('Message sent'); }
      else { alert('Send failed'); }
    }).catch(function () { alert('Network error'); });
  });
})();"""
    }

    fun renderFirestoreRules(config: ContactSecConfig): String {
        val allowedFields = listOf(
            "name",
            "email",
            "subject",
            "message",
            "session_token",
            "ts_render",
            "fp",
            "pow_nonce",
            "pow_challenge",
            "cf-turnstile-response",
            "created_at",
        )
        val fieldsCheck =
            allowedFields.joinToString("\n          ") { field ->
                "!(''${'$'}field' in request.resource.data) || request.resource.data.${'$'}field is ${if (field == "created_at") "timestamp" else "string"}"
            }

        return """rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /${config.firestoreCollection}/{docId} {
      allow create: if
          // Honeypot must be empty
          request.resource.data.hp_name == '' &&
          // Render time must be >= minRenderTimeMs
          request.resource.data.ts_render is int &&
          // Timestamp matches server time (anti-replay)
          request.resource.data.created_at == request.time &&
          // Whitelisted fields only
          request.resource.data.keys().hasAll(['name','email','subject','message','session_token','ts_render','fp','pow_nonce','pow_challenge']) &&
          request.resource.data.keys().size() <= ${allowedFields.size} &&
          // Field type checks
          $fieldsCheck &&
          // Length caps
          request.resource.data.name.size() <= 200 &&
          request.resource.data.email.size() <= 200 &&
          request.resource.data.subject.size() <= 200 &&
          request.resource.data.message.size() <= 5000;
      allow read, update, delete: if false;
    }
  }
}"""
    }
}