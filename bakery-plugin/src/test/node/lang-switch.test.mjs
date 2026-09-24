import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);

const MODULE_PATH = fileURLToPath(
    new URL('../../main/resources/site/assets/js/lang-switch.js', import.meta.url),
);
const VECTORS_PATH = fileURLToPath(
    new URL('../../main/resources/bakery/langswitch/lang-switch-path-vectors.json', import.meta.url),
);

const langSwitch = require(MODULE_PATH);
const vectors = JSON.parse(readFileSync(VECTORS_PATH, 'utf8'));

/**
 * A DOM-free element double: records href/class/attribute mutations and click
 * listeners, mirroring the browser surface the adapter touches.
 */
function fakeElement(lang) {
    const attrs = new Map([['data-lang', lang]]);
    const classes = new Set();
    const listeners = new Map();
    return {
        lang,
        classList: {
            add: (name) => classes.add(name),
            remove: (name) => classes.delete(name),
            contains: (name) => classes.has(name),
        },
        getAttribute: (name) => (attrs.has(name) ? attrs.get(name) : null),
        setAttribute: (name, value) => attrs.set(name, String(value)),
        addEventListener: (type, handler) => {
            if (!listeners.has(type)) listeners.set(type, []);
            listeners.get(type).push(handler);
        },
        dispatch: (type, event) => (listeners.get(type) || []).forEach((handler) => handler(event)),
        classes,
        attrs,
    };
}

function fakeDocument(langs) {
    const options = langs.map(fakeElement);
    return {
        options,
        querySelectorAll: () => options,
    };
}

test('every shared vector resolves to its expected same-page url', () => {
    assert.ok(vectors.vectors.length >= 10, 'the shared fixture must stay populated');
    for (const vector of vectors.vectors) {
        const existing = vector.existingPages;
        const pageExists = existing ? (candidate) => existing.includes(candidate) : () => true;
        const actual = langSwitch.resolveLangPath(
            vector.currentPageUri,
            vector.currentLang,
            vector.targetLang,
            vector.defaultLang,
            pageExists,
        );
        assert.equal(
            actual,
            vector.expected,
            `vector '${vector.name}' diverged between the JS host and the shared spec`,
        );
    }
});

test('the resolver preserves a nested page instead of falling back to home', () => {
    assert.equal(langSwitch.resolveLangPath('blog/foo.html', 'fr', 'en', 'fr'), '../en/blog/foo.html');
    assert.notEqual(langSwitch.resolveLangPath('blog/foo.html', 'fr', 'en', 'fr'), 'en/index.html');
});

test('the resolver degrades to the language index when the target page is missing', () => {
    assert.equal(
        langSwitch.resolveLangPath('blog/foo.html', 'fr', 'en', 'fr', () => false),
        '../en/index.html',
    );
});

test('the tree adapter rewrites every data-lang href page-aware and marks the active one', () => {
    const document = fakeDocument(['fr', 'en']);

    const controller = langSwitch.attachLangSwitch({
        mode: 'tree',
        currentPageUri: 'en/blog/foo.html',
        currentLang: 'en',
        defaultLang: 'fr',
        document,
    });

    assert.ok(controller, 'attachLangSwitch returns a controller in tree mode');
    assert.equal(document.options[0].getAttribute('href'), '../../blog/foo.html');
    assert.equal(document.options[1].getAttribute('href'), 'foo.html');
    assert.equal(document.options[1].classes.has('active'), true);
    assert.equal(document.options[0].classes.has('active'), false);
});

test('the tree adapter never hardcodes the language index', () => {
    const document = fakeDocument(['fr', 'en']);

    langSwitch.attachLangSwitch({
        mode: 'tree',
        currentPageUri: 'blog/foo.html',
        currentLang: 'fr',
        defaultLang: 'fr',
        document,
    });

    assert.equal(document.options[1].getAttribute('href'), '../en/blog/foo.html');
    for (const option of document.options) {
        assert.notEqual(option.getAttribute('href'), 'en/index.html');
    }
});

test('the dictionary adapter relocalises in place without navigating', () => {
    const document = fakeDocument(['fr', 'en']);
    const switched = [];

    langSwitch.attachLangSwitch({
        mode: 'dictionary',
        currentPageUri: 'index.html',
        currentLang: 'fr',
        defaultLang: 'fr',
        document,
        onSwitch: (lang) => switched.push(lang),
    });

    let prevented = false;
    document.options[1].dispatch('click', {
        preventDefault: () => {
            prevented = true;
        },
    });

    assert.equal(prevented, true, 'dictionary mode must prevent the default navigation');
    assert.deepEqual(switched, ['en']);
});

test('the adapter is a no-op without a document or without options', () => {
    assert.equal(langSwitch.attachLangSwitch({ mode: 'tree' }), null);
    assert.equal(
        langSwitch.attachLangSwitch({ mode: 'tree', document: { querySelectorAll: () => [] } }),
        null,
    );
});
