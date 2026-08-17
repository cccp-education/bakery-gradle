// Turnstile placeholder for CI (no real Cloudflare account needed)
// Replaced at deploy time by the real Turnstile API script.
window.turnstile = {
  render: function () { return 0; },
  getResponse: function () { return 'DUMMY.TOKEN'; },
  reset: function () {},
  remove: function () {}
};