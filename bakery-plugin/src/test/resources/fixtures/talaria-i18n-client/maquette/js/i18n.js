/* ------------------------------------------------------------------ *
 * talaria-i18n-client — BDD fixture — chrome dictionary (`var DICT`)
 *
 * Reference floor `fr` owns nav.home + nav.cart. The `en` block is complete,
 * the `fa` block is missing nav.cart, the `de` block (patch file) is empty.
 * ------------------------------------------------------------------ */
window.TALARIA = window.TALARIA || {};

(function (ns) {
  var DICT = {
    fr: {
      "nav.home": "Accueil",
      "nav.cart": "Panier"
    },
    en: {
      "nav.home": "Home",
      "nav.cart": "Cart"
    },
    fa: {
      "nav.home": "خانه"
    }
  };
})(window.TALARIA);
