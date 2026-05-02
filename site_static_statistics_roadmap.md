# Feuille de route - Statistiques de surveillance pour site web statique
## Stack Google + Outils freemium sélectionnés

## Phase 1: Analyse des besoins et préparation (Semaine 1)

### 1.1 Métriques à surveiller
- **Trafic web** : visiteurs uniques, pages vues, sessions, taux de rebond
- **Performance** : temps de chargement, Core Web Vitals, scores Lighthouse
- **Disponibilité** : uptime, temps de réponse
- **Comportement utilisateur** : heatmaps, parcours, interactions
- **Erreurs techniques** : erreurs JavaScript, exceptions, bugs

### 1.2 Stack technologique retenue
- **Google Analytics 4** : analytics principal (gratuit)
- **Google PageSpeed Insights** : audit de performance (gratuit)
- **GTmetrix** : monitoring continu de performance (freemium)
- **UptimeRobot** : surveillance de disponibilité (freemium)
- **Hotjar** : heatmaps et comportement utilisateur (freemium)
- **Sentry** : monitoring des erreurs JavaScript (freemium)

## Phase 2: Implémentation Google Analytics 4 (Semaine 2)

### 2.1 Configuration GA4
1. **Création du compte Google Analytics**
    - Créer une propriété GA4
    - Configurer les données démographiques
    - Activer les signaux Google (remarketing)

2. **Installation du code de suivi**
```html
<!-- Google tag (gtag.js) -->
<script async src="https://www.googletagmanager.com/gtag/js?id=G-XXXXXXXXXX"></script>
<script>
  window.dataLayer = window.dataLayer || [];
  function gtag(){dataLayer.push(arguments);}
  gtag('js', new Date());
  gtag('config', 'G-XXXXXXXXXX', {
    anonymize_ip: true,
    allow_google_signals: false // RGPD
  });
</script>
```

### 2.2 Configuration des événements personnalisés GA4
```javascript
// Téléchargement de fichiers
gtag('event', 'file_download', {
  file_name: 'document.pdf',
  file_extension: 'pdf'
});

// Clics liens externes
gtag('event', 'click', {
  event_category: 'outbound',
  event_label: url
});

// Scroll profondeur
gtag('event', 'scroll', {
  percent_scrolled: 90
});
```

### 2.3 Configuration Google Search Console
- Vérifier la propriété du site
- Soumettre le sitemap
- Lier avec Google Analytics

## Phase 3: Monitoring de performance (Semaine 3)

### 3.1 Google PageSpeed Insights - Tests manuels
1. **Configuration des tests réguliers**
    - Tester les pages principales hebdomadairement
    - Documenter les scores Core Web Vitals
    - Identifier les optimisations prioritaires

2. **Métriques à surveiller** :
    - **LCP** (Largest Contentful Paint) : < 2.5s
    - **FID** (First Input Delay) : < 100ms
    - **CLS** (Cumulative Layout Shift) : < 0.1

### 3.2 GTmetrix - Monitoring automatisé
1. **Configuration compte GTmetrix**
    - Plan gratuit : 3 crédits/mois, 1 site
    - Configurer les tests de Vancouver + Londres
    - Fréquence : tests hebdomadaires

2. **Alertes GTmetrix** :
    - Score PageSpeed < 80%
    - Temps de chargement > 3 secondes
    - Notifications par email

### 3.3 UptimeRobot - Surveillance disponibilité
1. **Configuration des monitors**
    - Monitor HTTP/HTTPS principal (intervalle 5 min)
    - Monitors pour pages clés (intervalle 5 min)
    - Plan gratuit : 50 monitors

2. **Alertes configurées** :
    - Email + SMS en cas de panne
    - Webhook Slack (optionnel)
    - Rapport mensuel de disponibilité

## Phase 4: Comportement utilisateur et erreurs (Semaine 4)

### 4.1 Hotjar - Analyse comportementale
1. **Installation du code Hotjar**
```html
<!-- Hotjar Tracking Code -->
<script>
    (function(h,o,t,j,a,r){
        h.hj=h.hj||function(){(h.hj.q=h.hj.q||[]).push(arguments)};
        h._hjSettings={hjid:XXXXXX,hjsv:6};
        a=o.getElementsByTagName('head')[0];
        r=o.createElement('script');r.async=1;
        r.src=t+h._hjSettings.hjid+j+h._hjSettings.hjsv;
        a.appendChild(r);
    })(window,document,'https://static.hotjar.com/c/hotjar-','.js?sv=');
</script>
```

2. **Configuration Hotjar (Plan gratuit)**
    - 35 sessions/jour enregistrées
    - Heatmaps illimitées
    - 3 funnels
    - Configurer les pages prioritaires pour l'enregistrement

### 4.2 Sentry - Monitoring des erreurs
1. **Installation Sentry JavaScript**
```html
<script src="https://browser.sentry-cdn.com/7.x.x/bundle.min.js"></script>
<script>
  Sentry.init({
    dsn: "YOUR_DSN_HERE",
    environment: "production",
    sampleRate: 1.0,
  });
</script>
```

2. **Configuration Sentry (Plan gratuit)**
    - 5 000 erreurs/mois
    - 1 projet
    - Alertes email configurées
    - Intégration Slack pour les erreurs critiques

## Phase 5: Tableaux de bord et reporting (Semaine 5)

### 5.1 Google Analytics 4 - Rapports personnalisés
1. **Tableaux de bord GA4** :
    - Vue d'ensemble du trafic
    - Rapport d'acquisition
    - Rapport de rétention
    - Événements personnalisés

2. **Google Data Studio (Looker Studio)**
    - Connecter GA4, Search Console
    - Dashboard unifié avec métriques clés
    - Rapports automatisés mensuels

### 5.2 Alertes et notifications centralisées
1. **Google Analytics Intelligence**
    - Alertes personnalisées sur anomalies
    - Notifications hebdomadaires par email

2. **Slack/Discord Webhooks** :
    - UptimeRobot → Canal #monitoring
    - Sentry → Canal #bugs
    - GTmetrix → Canal #performance

### 5.3 KPIs de monitoring
- **Trafic** : Visiteurs uniques mensuels (+/- 10%)
- **Performance** : Score PageSpeed > 85
- **Disponibilité** : Uptime > 99.5%
- **Erreurs** : < 1% des sessions avec erreurs JS
- **UX** : Taux de rebond < 60%

## Phase 6: Optimisation continue (En continu)

### 6.1 Routine de monitoring
- **Quotidien** : Vérification alertes Sentry et UptimeRobot
- **Hebdomadaire** : Analyse GA4 + GTmetrix
- **Mensuel** : Révision Hotjar + optimisations performance
- **Trimestriel** : Audit complet et révision des objectifs

### 6.2 Actions d'optimisation basées sur les données
- **GA4** → Améliorer contenus pages à fort rebond
- **PageSpeed** → Optimiser images, CSS, JS critiques
- **Hotjar** → UX/UI des zones problématiques
- **Sentry** → Correction bugs JavaScript prioritaires

## Limites des plans gratuits/freemium

| Outil | Plan gratuit | Limitations |
|-------|--------------|-------------|
| **Google Analytics 4** | Illimité | Aucune limite significative |
| **Google PageSpeed** | Illimité | Tests manuels uniquement |
| **GTmetrix** | 3 crédits/mois | 1 site, tests limités |
| **UptimeRobot** | 50 monitors | Vérification 5min, 2 mois historique |
| **Hotjar** | 35 sessions/jour | Enregistrements limités |
| **Sentry** | 5K erreurs/mois | 1 projet, historique 30 jours |

## Checklist de mise en œuvre

**Phase 2 - GA4** :
- [ ] Créer compte Google Analytics 4
- [ ] Installer le code de suivi sur toutes les pages
- [ ] Configurer événements personnalisés
- [ ] Lier Google Search Console

**Phase 3 - Performance** :
- [ ] Configurer GTmetrix monitoring
- [ ] Créer monitors UptimeRobot
- [ ] Établir baseline PageSpeed Insights
- [ ] Configurer alertes email

**Phase 4 - UX/Erreurs** :
- [ ] Installer Hotjar sur pages prioritaires
- [ ] Configurer Sentry pour JS errors
- [ ] Tester l'envoi d'erreurs
- [ ] Configurer alertes critiques

**Phase 5 - Reporting** :
- [ ] Créer dashboard Looker Studio
- [ ] Configurer rapports automatisés
- [ ] Intégrer webhooks Slack/Discord
- [ ] Tester toutes les alertes

## Ressources Google officielles

- [Google Analytics 4 Setup Guide](https://support.google.com/analytics/answer/9304153)
- [PageSpeed Insights Documentation](https://developers.google.com/speed/docs/insights/v5/get-started)
- [Search Console Help](https://support.google.com/webmasters)
- [Looker Studio GA4 Connector](https://support.google.com/datastudio/answer/9707050)

---

**Temps total d'implémentation** : 5 semaines
**Coût mensuel** : 0€ (versions gratuites/freemium)
**Maintenance** : 2-3h/semaine