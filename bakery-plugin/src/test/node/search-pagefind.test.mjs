import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';

const require = createRequire(import.meta.url);

const MODULE_PATH = fileURLToPath(
    new URL('../../main/resources/site/assets/js/search-pagefind.js', import.meta.url),
);

const search = require(MODULE_PATH);

function fakeElement(tag) {
    const attributes = new Map();
    return {
        tag,
        children: [],
        attributes,
        className: '',
        textContent: '',
        innerHTML: '',
        appendChild(child) {
            this.children.push(child);
            return child;
        },
        setAttribute(name, value) {
            attributes.set(name, String(value));
        },
        getAttribute(name) {
            return attributes.has(name) ? attributes.get(name) : null;
        },
        addEventListener() {},
    };
}

function fakeDocument(elementsById) {
    return {
        getElementById: (id) => elementsById[id] || null,
        createElement: (tag) => fakeElement(tag),
    };
}

function fakePagefind(pages) {
    return {
        search: async () => ({
            results: pages.map((page) => ({ data: async () => page })),
        }),
    };
}

function resultPage(index) {
    return {
        url: `/blog/post-${index}.html`,
        excerpt: `Excerpt <mark>${index}</mark>`,
        meta: { title: `Post ${index}` },
    };
}

function flush() {
    return new Promise((resolve) => setTimeout(resolve, 10));
}

test('a query is trimmed and an absent query is empty', () => {
    assert.equal(search.normaliseQuery(null), '');
    assert.equal(search.normaliseQuery('  pagefind  '), 'pagefind');
});

test('the bundle path is read from the form, then falls back to the default', () => {
    const form = fakeElement('form');
    form.setAttribute('data-pagefind-script', '  ../pagefind/pagefind.js  ');
    assert.equal(search.bundlePath(form), '../pagefind/pagefind.js');
    assert.equal(search.bundlePath(fakeElement('form')), './' + search.DEFAULT_SCRIPT);
});

test('a bare bundle path is normalised to a dynamic import specifier', () => {
    const form = fakeElement('form');
    form.setAttribute('data-pagefind-script', 'pagefind/pagefind.js');
    assert.equal(search.bundlePath(form), './pagefind/pagefind.js');
    assert.equal(search.importSpecifier('/pagefind/pagefind.js'), '/pagefind/pagefind.js');
    assert.equal(search.importSpecifier('https://cdn.example/pagefind/pagefind.js'), 'https://cdn.example/pagefind/pagefind.js');
});

test('the import URL is resolved against the document, not the script', () => {
    assert.equal(
        search.resolveImportUrl('pagefind/pagefind.js', 'https://cheroliv.com/'),
        'https://cheroliv.com/pagefind/pagefind.js',
    );
    assert.equal(
        search.resolveImportUrl('../pagefind/pagefind.js', 'https://cheroliv.com/en/'),
        'https://cheroliv.com/pagefind/pagefind.js',
    );
    assert.equal(
        search.resolveImportUrl('../../pagefind/pagefind.js', 'https://cheroliv.com/en/blog/post.html'),
        'https://cheroliv.com/pagefind/pagefind.js',
    );
});

test('the empty label is read from the result container, then falls back', () => {
    const container = fakeElement('div');
    container.setAttribute('data-empty-label', 'Aucun résultat.');
    assert.equal(search.emptyLabel(container), 'Aucun résultat.');
    assert.equal(search.emptyLabel(fakeElement('div'), 'No results.'), 'No results.');
});

test('an empty query clears the container without calling Pagefind', async () => {
    const container = fakeElement('div');
    const pagefind = {
        search: async () => assert.fail('Pagefind must not be called for an empty query'),
    };

    const rendered = await search.runSearch({
        pagefind,
        query: '   ',
        container,
        document: fakeDocument({}),
    });

    assert.deepEqual(rendered, []);
    assert.equal(container.textContent, '');
    assert.equal(container.children.length, 0);
});

test('no result renders the translated empty label', async () => {
    const container = fakeElement('div');
    container.setAttribute('data-empty-label', 'Aucun résultat.');

    const rendered = await search.runSearch({
        pagefind: fakePagefind([]),
        query: 'nothing',
        container,
        document: fakeDocument({}),
    });

    assert.deepEqual(rendered, []);
    assert.equal(container.textContent, 'Aucun résultat.');
});

test('results are rendered as links with their title and marked excerpt', async () => {
    const container = fakeElement('div');
    const document = fakeDocument({});

    const rendered = await search.runSearch({
        pagefind: fakePagefind([resultPage(1), resultPage(2)]),
        query: 'pagefind',
        container,
        document,
    });

    assert.equal(rendered.length, 2);
    assert.equal(container.children.length, 2);
    assert.equal(container.children[0].attributes.get('href'), '/blog/post-1.html');
    assert.equal(container.children[0].children[0].textContent, 'Post 1');
    assert.equal(container.children[0].children[1].innerHTML, 'Excerpt <mark>1</mark>');
});

test('the result count is capped to keep the modal lightweight', async () => {
    const container = fakeElement('div');
    const pages = Array.from({ length: 15 }, (_, index) => resultPage(index));

    const rendered = await search.runSearch({
        pagefind: fakePagefind(pages),
        query: 'pagefind',
        container,
        document: fakeDocument({}),
        maxResults: 3,
    });

    assert.equal(rendered.length, 3);
    assert.equal(container.children.length, 3);
});

test('attach is inert when the page does not carry the search surface', () => {
    assert.equal(search.attach({ document: fakeDocument({}) }), false);
});

test('attach wires the form submit to the Pagefind loader', async () => {
    const modal = fakeElement('div');
    const form = fakeElement('form');
    const container = fakeElement('div');
    container.setAttribute('data-empty-label', 'Aucun résultat.');
    const input = { value: 'pagefind' };

    form.setAttribute('data-pagefind-script', '../pagefind/pagefind.js');
    form.querySelector = (selector) => (selector === 'input[type="search"]' ? input : null);
    form.addEventListener = (type, handler) => {
        if (type === 'submit') form.submitHandler = handler;
    };

    let loadedPath = null;
    const attached = search.attach({
        document: fakeDocument({
            searchModal: modal,
            'pagefind-search-form': form,
            'pagefind-results': container,
        }),
        loadPagefind: async (path) => {
            loadedPath = path;
            return fakePagefind([resultPage(1)]);
        },
    });

    assert.equal(attached, true);
    assert.equal(typeof form.submitHandler, 'function');

    form.submitHandler({ preventDefault() {} });
    await flush();

    assert.equal(loadedPath, '../pagefind/pagefind.js');
    assert.equal(container.children.length, 1);
});
