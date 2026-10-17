(function () {
  'use strict';
  if (window.__goreeCleanBrowsingInstalled) return;
  var domain = location.hostname.toLowerCase();
  if (domain !== 'tiktok.com' && !domain.endsWith('.tiktok.com')) return;
  window.__goreeCleanBrowsingInstalled = true;

  var marker = 'data-goree-hidden';
  var adSelectors = [
    '[data-e2e="ad-card"]',
    '[data-e2e="ad-slot"]',
    '[data-e2e="sponsored-video"]',
    '[data-e2e="sponsored-card"]',
    '[data-ad-slot]',
    '[data-testid="ad-slot"]'
  ].join(',');
  var adLabelSelectors = [
    '[data-e2e="sponsored-label"]',
    '[data-e2e="ad-label"]',
    '[aria-label="Sponsored"]',
    '[aria-label="Advertisement"]'
  ].join(',');
  var dialogs = [
    '[role="dialog"]',
    '[aria-modal="true"]',
    '[data-e2e*="popup"]',
    '[data-e2e*="modal"]',
    '[class*="Modal"]',
    '[class*="modal"]'
  ].join(',');

  function hide(element) {
    if (!element || element.nodeType !== 1 || element.hasAttribute(marker)) return;
    element.setAttribute(marker, '1');
    element.style.setProperty('display', 'none', 'important');
  }

  function closeLitePromotions() {
    document.querySelectorAll(dialogs).forEach(function (dialog) {
      var copy = (dialog.textContent || '').slice(0, 3000);
      if (!/tiktok[\s\u00a0-]*lite/i.test(copy)) return;
      // Never remove the document, video feed, or generic page-level wrapper.
      var target = dialog;
      var parent = dialog.parentElement;
      if (parent && parent !== document.body) {
        var style = window.getComputedStyle(parent);
        if ((style.position === 'fixed' || style.position === 'absolute')
            && parent.clientHeight >= window.innerHeight * 0.55) target = parent;
      }
      hide(target);
    });

    // A few banners are not dialogs: suppress only a matching small promo region.
    document.querySelectorAll('a[href*="tiktoklite"], a[href*="tiktok-lite"], a[href*="lite.tiktok"]').forEach(function (link) {
      var parent = link.closest('[data-e2e*="banner"], [data-e2e*="popup"], aside');
      if (parent && parent.clientHeight < window.innerHeight * 0.75) hide(parent);
    });
  }

  // Hide only exact app-launch / coin purchase controls on TikTok pages.
  function cleanCtaName(value) {
    return (value || '').replace(/\s+/g, ' ').trim().toLowerCase();
  }
  function hideAppAndCoinButtons() {
    document.querySelectorAll('button, a, [role="button"]').forEach(function (button) {
      if (!button || button.nodeType !== 1) return;
      var name = cleanCtaName(button.textContent);
      var aria = cleanCtaName(button.getAttribute('aria-label'));
      var title = cleanCtaName(button.getAttribute('title'));
      var match = function (label) {
        return label === 'open app' || label === 'open in app' ||
            label === 'get coins';
      };
      if (match(aria) || match(title) || (name.length < 90 && match(name)))
        hide(button);
    });
  }

  function removeAds() {
    document.querySelectorAll(adSelectors).forEach(hide);
    document.querySelectorAll(adLabelSelectors).forEach(function (label) {
      var card = label.closest('[data-e2e="recommend-list-item-container"], [data-e2e="video-feed-item"], [data-e2e="feed-item"]');
      hide(card || label);
    });
  }

  var queued = false;
  function refresh() {
    queued = false;
    try { closeLitePromotions(); removeAds(); hideAppAndCoinButtons(); } catch (ignored) {}
  }
  function requestRefresh() {
    if (queued) return;
    queued = true;
    window.requestAnimationFrame(refresh);
  }

  // TikTok navigates via SPA transitions; a single onPageFinished pass is insufficient.
  var observer = new MutationObserver(requestRefresh);
  observer.observe(document.documentElement, { childList: true, subtree: true, characterData: true });
  requestRefresh();
})();
