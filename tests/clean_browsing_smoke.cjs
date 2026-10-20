const fs = require('fs');
const vm = require('vm');
const assert = require('assert');
const script = fs.readFileSync('app/src/main/assets/clean_browsing.js', 'utf8');
function fake(text, parent) {
  const attributes = new Set();
  const styles = {};
  return {
    nodeType: 1, textContent: text, parentElement: parent || null,
    clientHeight: 120, style: { setProperty(k, v) { styles[k] = v; } },
    hasAttribute(k) { return attributes.has(k); },
    setAttribute(k) { attributes.add(k); },
    isHidden() { return attributes.has('data-goree-hidden') && styles.display === 'none'; },
    closest() { return null; }
  };
}
const lite = fake('Try TikTok Lite — install to watch more');
const upsell = fake('Get the full app experience. Open TikTok. Not now');
const signin = fake('Log in to TikTok. Continue with Google');
const sponsored = fake('Paid placement');
const label = fake('Sponsored');
label.closest = () => sponsored;
const ordinary = fake('Normal creator video');
const body = {};
const doc = {
  body, documentElement: {},
  querySelectorAll(sel) {
    if (sel.includes('[role="dialog"]')) return [lite, upsell, signin];
    if (sel.includes('[data-e2e="ad-card"]')) return [ordinary];
    if (sel.includes('[data-e2e="sponsored-label"]')) return [label];
    return [];
  }
};
let observed = 0;
const context = {
  window: {
    innerHeight: 800, getComputedStyle() { return { position: 'relative' }; },
    requestAnimationFrame(fn) { fn(); }
  },
  document: doc, location: { hostname: 'www.tiktok.com' },
  MutationObserver: class {
    constructor(callback) { this.callback = callback; }
    observe() { observed++; }
  }
};
vm.runInNewContext(script, context);
assert.ok(lite.isHidden(), 'TikTok Lite promo should be hidden');
assert.ok(upsell.isHidden(), 'Identifiable full-app popup should be hidden');
assert.ok(!signin.isHidden(), 'Normal login dialog must remain visible');
assert.ok(sponsored.isHidden(), 'sponsored card should be hidden');
assert.ok(ordinary.isHidden(), 'explicit ad slot should be hidden');
assert.strictEqual(observed, 1);
vm.runInNewContext(script, context);
assert.strictEqual(observed, 1, 'repeat injection must be idempotent');
console.log('Clean browsing mock DOM: Lite promotion + ad slots hidden, observer installed once.');
