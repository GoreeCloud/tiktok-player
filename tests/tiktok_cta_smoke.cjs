const fs = require('fs');
const vm = require('vm');
const assert = require('node:assert/strict');
const script = fs.readFileSync('app/src/main/assets/clean_browsing.js', 'utf8');
function element(text, attrs = {}) {
  let hidden = false;
  return {
    nodeType: 1, textContent: text,
    getAttribute(name) { return attrs[name] || null; },
    hasAttribute(name) { return name === 'data-goree-hidden' && hidden; },
    setAttribute(name) { if (name === 'data-goree-hidden') hidden = true; },
    style: { setProperty(name, value) { if (name === 'display' && value === 'none') hidden = true; } },
    isHidden() { return hidden; }
  };
}
function environment(hostname) {
  const controls = [
    element('Open App'), element('  Get   coins '),
    element('', {'aria-label':'OPEN APP'}), element('', { title:'Get Coins' }),
    element('Open in app'), element('Open App settings'),
    element('How to get coins'), element('Normal creator video')
  ];
  let callback, observed = 0;
  const ctx = {
    location: {hostname},
    document: {
      body: {}, documentElement: {},
      querySelectorAll(selector) {
        return selector.includes('button, a, [role="button"]') ? controls : [];
      }
    },
    window: {innerHeight: 800, requestAnimationFrame(fn) {fn();}},
    MutationObserver: class {
      constructor(cb) {callback = cb;}
      observe(node, options) {
        observed++;
        assert.equal(options.characterData, true);
      }
    }
  };
  vm.runInNewContext(script, ctx);
  return { controls, ctx, trigger() { if (callback) callback(); }, observed() {return observed;} };
}
const tiktok = environment('www.tiktok.com');
for (let i=0; i<5; i++) assert.ok(tiktok.controls[i].isHidden(), 'Missing CTA '+i);
for (let i=5; i<tiktok.controls.length; i++)
  assert.ok(!tiktok.controls[i].isHidden(), 'Unrelated control hidden '+i);
tiktok.controls.push(element('Get coins'));
tiktok.trigger();
assert.ok(tiktok.controls.at(-1).isHidden(), 'New SPA CTA must be suppressed');
vm.runInNewContext(script, tiktok.ctx);
assert.equal(tiktok.observed(), 1, 'Script should install once');
const external = environment('example.org');
assert.equal(external.observed(), 0);
assert.ok(external.controls.every(control=>!control.isHidden()));
console.log('TikTok exact CTA hide/SPA changes and other-site safety passed.');
