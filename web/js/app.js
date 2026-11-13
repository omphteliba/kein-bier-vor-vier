// The web app: same screen as the phone app, without widget and watch.
import {
  EDITIONS, editionForLanguage, status, rounds, loadSpots, breweriesFrom, breweryOn, place, placeOf,
  cityName, countryName, locationOn, distanceKm, formatDistance, formatCountdown, dayAt,
} from './clock.js';

const TEXT = {
  de: {
    title: 'Irgendwo ist immer Vier',
    tagline: { vier: 'Frei nach dem Spruch „Kein Bier vor Vier“.', five: 'Irgendwo ist immer fünf.' },
    nowIn: { vier: 'Gerade 16 Uhr in', five: 'Gerade 17 Uhr in' },
    since: (m) => `seit ${m} min`,
    howFar: 'Wie weit?',
    locating: 'Suche …',
    noLocation: 'Standort nicht verfügbar',
    youAreThere: 'Du bist da! 🍻',
    showOnMap: 'Auf der Karte zeigen',
    alsoNow: 'Auch gerade Bierzeit in',
    nextRound: 'Nächste Runde',
    breweries: 'Brauereien vor Ort',
    selectionNote: 'Eine kleine Auswahl – ohne Anspruch auf Vollständigkeit.',
    count: (i, n) => `${i} von ${n}`,
    previous: 'Vorherige Brauerei',
    next: 'Nächste Brauerei',
    untappd: 'Untappd ↗',
    website: 'Website ↗',
    map: 'Karte ↗',
    moreNearby: 'Mehr Brauereien in der Nähe',
    moreNearbyText: (p) => `Google Maps zeigt dir Brauereien rund um ${p}.`,
    noLocalBeer: 'Kein lokales Bier',
    bringYourOwn: 'Selbst mitbringen 🎒',
    comingUp: 'Als Nächstes',
    install: '＋  Zum Startbildschirm hinzufügen',
    installIos: 'So geht’s auf dem iPhone: Tippe auf Teilen (□↑) – in Safari steckt es oft hinter „…“ – und dann auf „Zum Home-Bildschirm“. Falls der Eintrag fehlt, in der Liste ganz nach unten scrollen.',
    installOther: 'Öffne das Menü deines Browsers (⋮ oder ≡) und wähle „App installieren“ oder „Zum Startbildschirm hinzufügen“. Fehlt der Eintrag, klappt es mit Chrome, Edge oder Samsung Internet.',
    responsibly: 'Bitte verantwortungsvoll genießen 🍻',
    privacy: 'Datenschutz',
    settings: 'Einstellungen',
    whichSaying: 'Welcher Spruch?',
    editionAuto: 'Automatisch (nach Sprache)',
    editionVier: 'Kein Bier vor Vier (16 Uhr)',
    editionFive: "5 O'Clock Somewhere (17 Uhr)",
    language: 'Sprache',
    langAuto: 'Automatisch (Browser)',
    close: 'Schließen',
  },
  en: {
    title: "5 O'Clock Somewhere",
    tagline: { vier: 'Somewhere it’s always four.', five: "It's five o'clock somewhere." },
    nowIn: { vier: "It's 4 p.m. in", five: "It's 5 p.m. in" },
    since: (m) => `for ${m} min`,
    howFar: 'How far?',
    locating: 'Locating …',
    noLocation: 'Location unavailable',
    youAreThere: "You're there! 🍻",
    showOnMap: 'Show on map',
    alsoNow: "Also beer o'clock right now in",
    nextRound: 'Next round',
    breweries: 'Breweries nearby',
    selectionNote: 'A small selection – no claim to completeness.',
    count: (i, n) => `${i} of ${n}`,
    previous: 'Previous brewery',
    next: 'Next brewery',
    untappd: 'Untappd ↗',
    website: 'Website ↗',
    map: 'Map ↗',
    moreNearby: 'More breweries nearby',
    moreNearbyText: (p) => `Google Maps shows you breweries around ${p}.`,
    noLocalBeer: 'No local beer',
    bringYourOwn: 'Bring your own 🎒',
    comingUp: 'Coming up',
    install: '＋  Add to home screen',
    installIos: 'On iPhone: tap Share (□↑) – in Safari it often hides behind “…” – then “Add to Home Screen”. If it isn’t there, scroll down the list.',
    installOther: 'Open your browser’s menu (⋮ or ≡) and choose “Install app” or “Add to home screen”. If it isn’t there, Chrome, Edge or Samsung Internet can do it.',
    responsibly: 'Please enjoy responsibly 🍻',
    privacy: 'Privacy',
    settings: 'Settings',
    whichSaying: 'Which saying?',
    editionAuto: 'Automatic (by language)',
    editionVier: 'Kein Bier vor Vier (4 p.m.)',
    editionFive: "5 O'Clock Somewhere (5 p.m.)",
    language: 'Language',
    langAuto: 'Automatic (browser)',
    close: 'Close',
  },
};

const UPCOMING_MS = 12 * 3600_000;
const $ = (id) => document.getElementById(id);

// ---- Settings, kept in this browser only ----
const store = {
  get(key) { try { return localStorage.getItem(key); } catch { return null; } },
  set(key, value) { try { value == null ? localStorage.removeItem(key) : localStorage.setItem(key, value); } catch { /* private mode */ } },
};
const browserLang = () => ((navigator.language || 'en').slice(0, 2) === 'de' ? 'de' : 'en');
const lang = () => { const l = store.get('lang'); return l === 'de' || l === 'en' ? l : browserLang(); };
const edition = () => EDITIONS[store.get('edition')] ?? editionForLanguage(lang());

// ---- Installing ----
// iPads report themselves as Macs, but Macs have no touch screen.
const isIos = () => /iPhone|iPad|iPod/.test(navigator.userAgent) ||
  (/Macintosh/.test(navigator.userAgent) && navigator.maxTouchPoints > 1);
const isInstalled = () => matchMedia('(display-mode: standalone)').matches || navigator.standalone === true;

// ---- Links, the same as in the apps ----
const mapsSearch = (terms) => 'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(terms.join(', '));
const mapsUrl = (spot, brewery) =>
  mapsSearch([brewery?.name, placeOf(spot, brewery, 'en'), countryName(spot.country, 'en')].filter(Boolean));
const nearbyUrl = (spot, placeName) => mapsSearch(['brewery', placeName, countryName(spot.country, 'en')]);
const untappdUrl = (brewery) => 'https://untappd.com/search?type=brewery&q=' + encodeURIComponent(brewery.name);

// ---- Small DOM helpers ----
function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v == null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const c of children.flat()) if (c != null) el.append(c);
  return el;
}
const link = (href, attrs, ...children) => h('a', { href, target: '_blank', rel: 'noopener', ...attrs }, ...children);
const flag = (code, cls = 'flag-inline') => h('img', { class: cls, src: `flags/${code.toLowerCase()}.svg`, alt: '', width: 20, height: 15 });

// ---- State ----
let spots = [];
let here = null; // {lat, lon} once the visitor asked for distances
let locating = false;
let carouselKey = '';
let installPrompt = null;

// ---- Rendering ----
function render() {
  const now = Date.now();
  const L = lang();
  const T = TEXT[L];
  const ed = edition();
  document.documentElement.lang = L;
  document.title = T.title;

  $('title').textContent = T.title;
  $('tagline').textContent = T.tagline[ed.id];
  $('settings-label').textContent = T.settings;
  $('open-settings').title = T.settings;
  $('dial').innerHTML = dialSvg(ed.hour - 12, new Date(now));

  const s = status(now, ed, spots);
  renderGlass(s, ed, L, T);
  renderNext(s, L, T);
  $('breweries-title').textContent = T.breweries;
  $('selection-note').textContent = T.selectionNote;
  renderCarousel(s, L, T);
  $('upcoming-title').textContent = T.comingUp;
  renderUpcoming(now, ed, L, T);

  $('install').textContent = T.install;
  $('install-hint').textContent = isIos() ? T.installIos : T.installOther;
  $('responsibly').textContent = T.responsibly;
  $('privacy-link').textContent = T.privacy;
}

function renderGlass(s, ed, L, T) {
  const cur = s.openNow[0];
  const spot = cur.spot;
  const brewery = breweryOn(spot, cur.day);
  const placeName = place(spot, cur.day, L);
  const country = countryName(spot.country, L);

  let distancePill;
  if (here) {
    const text = formatDistance(distanceKm(here, locationOn(spot, cur.day)), L) ?? T.youAreThere;
    distancePill = link(mapsUrl(spot, brewery), { class: 'pill', 'aria-label': `${text}, ${T.showOnMap}` }, h('span', { 'aria-hidden': 'true' }, '📍'), text);
  } else {
    distancePill = h('button', { class: 'pill', type: 'button', onclick: locate, disabled: locating },
      h('span', { 'aria-hidden': 'true' }, '📍'), locating ? T.locating : T.howFar);
  }

  const alsoNow = s.openNow.slice(1).filter((o) => o.minute === cur.minute);
  const glass = $('glass');
  glass.replaceChildren(
    h('div', { class: 'glass-top' },
      h('p', { class: 'saying', id: 'saying' }, h('span', { 'aria-hidden': 'true' }, '🍺 '), T.nowIn[ed.id]),
      h('div', { class: 'coaster' }, h('img', { src: `flags/${spot.country.toLowerCase()}.svg`, alt: country, width: 54, height: 40 })),
    ),
    h('div', { class: 'glass-beer' },
      foamSvg(),
      bubbles(),
      h('h2', { class: 'place' }, link(mapsUrl(spot, brewery), { title: T.showOnMap }, placeName)),
      h('p', { class: 'country' }, country),
      h('div', { class: 'pills' },
        h('span', { class: 'pill' }, h('span', { 'aria-hidden': 'true' }, '⏱'), T.since(cur.minutesIn)),
        distancePill,
      ),
      alsoNow.length ? [
        h('p', { class: 'also' }, T.alsoNow),
        h('div', { class: 'chips' }, alsoNow.map((o) => {
          const b = breweryOn(o.spot, o.day);
          return link(mapsUrl(o.spot, b), { class: 'chip' }, flag(o.spot.country), place(o.spot, o.day, L));
        })),
      ] : null,
    ),
  );
}

function renderNext(s, L, T) {
  const next = s.next;
  const since = s.openNow[0].minutesIn * 60_000;
  const progress = Math.min(1, since / Math.max(1, since + next.countdown));
  const first = next.spots[0];
  const names = next.spots.map((x) => [flag(x.country), ' ', place(x, dayAt(x, next.at), L)]);
  const joined = names.flatMap((n, i) => (i ? [', ', ...n] : n));
  $('next').replaceWith(link(mapsUrl(first, breweryOn(first, dayAt(first, next.at))), { class: 'card next', id: 'next' },
    h('div', { class: 'next-row' },
      h('div', { class: 'next-where' }, h('p', { class: 'label' }, T.nextRound), h('p', { class: 'next-names' }, joined)),
      h('p', { class: 'countdown' }, formatCountdown(next.countdown)),
    ),
    h('div', { class: 'progress', role: 'progressbar', 'aria-valuemin': 0, 'aria-valuemax': 100, 'aria-valuenow': Math.round(progress * 100) },
      h('div', { style: `width:${(progress * 100).toFixed(1)}%` })),
  ));
}

/** The breweries of the places where it's beer o'clock right now, then Google Maps for more. */
function carouselPages(s, L) {
  const first = s.openNow[0];
  const pages = s.openNow.filter((o) => o.minute === first.minute)
    .flatMap((o) => breweriesFrom(o.spot, o.day).map((b) => ({ spot: o.spot, brewery: b })));
  if (!pages.length) return [];
  return [...pages, { spot: first.spot, nearby: place(first.spot, first.day, L) }];
}

function renderCarousel(s, L, T) {
  const pages = carouselPages(s, L);
  // Re-render only when the round changes, so swiping isn't reset every minute.
  const key = L + '|' + pages.map((p) => p.brewery?.name ?? p.nearby).join('|');
  if (key === carouselKey) return;
  carouselKey = key;

  const box = $('carousel');
  if (!pages.length) {
    box.replaceChildren(h('div', { class: 'card brewery' },
      h('div', { class: 'brewery-head' }, h('div', { class: 'tile', 'aria-hidden': 'true' }, '🎒'),
        h('div', {}, h('p', { class: 'brewery-name' }, T.noLocalBeer), h('p', { class: 'soft' }, T.bringYourOwn)))));
    return;
  }

  const list = h('ul', { class: 'carousel', tabindex: 0, 'aria-label': T.breweries },
    pages.map((p) => h('li', {}, p.brewery ? breweryCard(p, L, T) : nearbyCard(p, T))));
  const count = h('span', { class: 'carousel-count', 'aria-live': 'polite' });
  const prev = h('button', { type: 'button', 'aria-label': T.previous, onclick: () => go(-1) }, '‹');
  const next = h('button', { type: 'button', 'aria-label': T.next, onclick: () => go(1) }, '›');
  // While a button scroll is still animating, count from where it is heading.
  let target = null;
  const current = () => {
    const w = Math.max(1, list.clientWidth);
    if (target != null && Math.abs(list.scrollLeft - target * w) > 2) return target;
    target = null;
    return Math.round(list.scrollLeft / w);
  };
  const update = () => {
    const i = current();
    count.textContent = T.count(i + 1, pages.length);
    prev.disabled = i === 0;
    next.disabled = i >= pages.length - 1;
  };
  function go(step) {
    target = Math.min(pages.length - 1, Math.max(0, current() + step));
    update();
    list.scrollTo({ left: target * list.clientWidth, behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth' });
  }
  list.addEventListener('scroll', () => requestAnimationFrame(update), { passive: true });
  list.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowRight') { go(1); e.preventDefault(); }
    if (e.key === 'ArrowLeft') { go(-1); e.preventDefault(); }
  });
  box.replaceChildren(h('div', { class: 'carousel-wrap' }, list, h('div', { class: 'carousel-nav' }, prev, count, next)));
  update();
}

function breweryCard({ spot, brewery }, L, T) {
  const town = placeOf(spot, brewery, L);
  return h('div', { class: 'card brewery' },
    h('div', { class: 'brewery-head' },
      h('div', { class: 'tile', 'aria-hidden': 'true' }, '🍺'),
      h('div', {},
        h('p', { class: 'brewery-town' }, flag(spot.country), ' ', town),
        h('h3', { class: 'brewery-name' }, brewery.name))),
    h('div', { class: 'brewery-links' },
      link(untappdUrl(brewery), { 'aria-label': `${brewery.name} – Untappd` }, T.untappd),
      brewery.website ? link(brewery.website, { 'aria-label': `${brewery.name} – Website` }, T.website) : null,
      link(mapsUrl(spot, brewery), { 'aria-label': `${brewery.name} – ${T.map.replace(' ↗', '')}` }, T.map)));
}

function nearbyCard({ spot, nearby }, T) {
  return link(nearbyUrl(spot, nearby), { class: 'card brewery nearby' },
    h('div', { class: 'tile', 'aria-hidden': 'true' }, '🗺'),
    h('div', {}, h('h3', { class: 'brewery-name' }, T.moreNearby), h('p', {}, T.moreNearbyText(nearby))));
}

function renderUpcoming(now, ed, L, T) {
  const fmt = new Intl.DateTimeFormat(L === 'de' ? 'de-DE' : 'en-US', { hour: 'numeric', minute: '2-digit' });
  // The very next round already has its own card.
  const items = rounds(now, now + UPCOMING_MS, ed, spots).slice(1).map((r) => {
    const first = r.spots[0];
    const names = r.spots.map((x) => [flag(x.country), ' ', place(x, dayAt(x, r.at), L)]).flatMap((n, i) => (i ? [', ', ...n] : n));
    return h('li', {}, link(mapsUrl(first, breweryOn(first, dayAt(first, r.at))), {},
      h('span', { class: 'time' }, fmt.format(r.at)), h('span', { class: 'where' }, names)));
  });
  $('upcoming').replaceChildren(...items);
}

// ---- Drawings ----
/** The brand mark: a tiny dial where every number is [digit], showing the real time. */
function dialSvg(digit, date) {
  const c = 50, r = 47;
  const nums = Array.from({ length: 12 }, (_, i) => {
    const a = (i * 30 * Math.PI) / 180;
    return `<text x="${(c + r * 0.72 * Math.sin(a)).toFixed(1)}" y="${(c - r * 0.72 * Math.cos(a)).toFixed(1)}">${digit}</text>`;
  }).join('');
  const minutes = date.getMinutes() + date.getSeconds() / 60;
  const hours = (date.getHours() % 12) + minutes / 60;
  return `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">
    <defs><linearGradient id="dialBeer" x1="0" y1="50" x2="0" y2="100" gradientUnits="userSpaceOnUse">
      <stop offset="0" stop-color="#F6B400"/><stop offset="1" stop-color="#D98300"/></linearGradient>
      <clipPath id="dialLow"><rect x="0" y="50" width="100" height="50"/></clipPath></defs>
    <circle cx="50" cy="50" r="${r}" fill="#FDF6E3"/>
    <circle cx="50" cy="50" r="${r}" fill="url(#dialBeer)" clip-path="url(#dialLow)"/>
    <rect x="3" y="47" width="94" height="6" fill="#fff"/>
    <circle cx="50" cy="50" r="${r - 2}" fill="none" stroke="#595540" stroke-width="4"/>
    <g font-family="system-ui, sans-serif" font-weight="700" font-size="15" fill="#595540" text-anchor="middle" dominant-baseline="central">${nums}</g>
    <line x1="50" y1="50" x2="50" y2="${50 - r * 0.42}" stroke="#595540" stroke-width="5" stroke-linecap="round" transform="rotate(${hours * 30} 50 50)"/>
    <line x1="50" y1="50" x2="50" y2="${50 - r * 0.62}" stroke="#595540" stroke-width="3.6" stroke-linecap="round" transform="rotate(${minutes * 6} 50 50)"/>
    <circle cx="50" cy="50" r="4" fill="#C62828"/></svg>`;
}

/** A soft band of foam: gently wavy on top and bottom, a little out of phase. */
function foamSvg() {
  const W = 600, mid = 18, thick = 9, amp = 4, wave = 240;
  const edge = (x, side, phase) => mid + side * thick + amp * Math.sin((x / wave) * 2 * Math.PI + phase);
  let d = `M0,${edge(0, -1, 0).toFixed(1)}`;
  for (let x = 6; x <= W; x += 6) d += ` L${x},${edge(x, -1, 0).toFixed(1)}`;
  for (let x = W; x >= 0; x -= 6) d += ` L${x},${edge(x, 1, 2.2).toFixed(1)}`;
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('class', 'foam');
  svg.setAttribute('viewBox', `0 0 ${W} 36`);
  svg.setAttribute('preserveAspectRatio', 'none');
  svg.setAttribute('aria-hidden', 'true');
  svg.innerHTML = `<path d="${d} Z" fill="#FFFCF2"/>` +
    [[0.1, 0.2], [0.27, -0.25], [0.46, 0.3], [0.64, -0.1], [0.83, 0.25], [0.95, -0.2]]
      .map(([fx, fy]) => `<circle cx="${fx * W}" cy="${mid + fy * thick * 2}" r="1.8" fill="#F3E7C9"/>`).join('');
  return svg;
}

/** Small bubbles drifting up through the beer (standing still with reduced motion). */
function bubbles() {
  const box = h('div', { class: 'bubbles', 'aria-hidden': 'true' });
  [[8, 0, 9, 6], [17, 5, 6, 4], [29, 2, 7.5, 8], [44, 7, 4.5, 5], [58, 3, 6.5, 7], [71, 8, 9, 4], [83, 1, 5, 6], [93, 6, 7, 5]]
    .forEach(([x, delay, dur, size]) => box.append(h('span', {
      style: `left:${x}%;width:${size}px;height:${size}px;animation-duration:${dur}s;animation-delay:-${delay}s`,
    })));
  requestAnimationFrame(() => box.style.setProperty('--h', `-${box.clientHeight + 20}px`));
  return box;
}

// ---- Location: only when asked, and it never leaves the browser ----
function locate() {
  if (!navigator.geolocation) return;
  locating = true;
  render();
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      here = { lat: pos.coords.latitude, lon: pos.coords.longitude };
      locating = false;
      render();
    },
    () => {
      locating = false;
      render();
      const pill = document.querySelector('.glass .pills button');
      if (pill) pill.lastChild.textContent = TEXT[lang()].noLocation;
    },
    { maximumAge: 30 * 60_000, timeout: 20_000 },
  );
}

async function locateIfAllowed() {
  try {
    const p = await navigator.permissions?.query({ name: 'geolocation' });
    if (p?.state === 'granted') locate();
  } catch { /* not supported */ }
}

// ---- Settings dialog ----
function setUpSettings() {
  const dialog = $('settings');
  const form = dialog.querySelector('form');
  const fill = () => {
    const T = TEXT[lang()];
    $('settings-title').textContent = T.settings;
    $('saying-legend').textContent = T.whichSaying;
    $('edition-auto').textContent = T.editionAuto;
    $('edition-vier').textContent = T.editionVier;
    $('edition-five').textContent = T.editionFive;
    $('language-legend').textContent = T.language;
    $('lang-auto').textContent = T.langAuto;
    $('close-settings').textContent = T.close;
    form.edition.value = store.get('edition') ?? 'auto';
    form.lang.value = store.get('lang') ?? 'auto';
  };
  $('open-settings').addEventListener('click', () => { fill(); dialog.showModal(); });
  dialog.addEventListener('click', (e) => { if (e.target === dialog) dialog.close(); });
  form.addEventListener('change', () => {
    store.set('edition', form.edition.value === 'auto' ? null : form.edition.value);
    store.set('lang', form.lang.value === 'auto' ? null : form.lang.value);
    fill();
    render();
    // Title, icon and manifest follow the language; they're set before the page loads.
    const m = document.querySelector('link[rel=manifest]');
    if (m) m.href = `manifest-${lang()}.webmanifest`;
  });
}

// ---- Start ----
function tick() {
  render();
  // Again at the start of the next minute, so the countdown stays exact.
  clearTimeout(tick.timer);
  tick.timer = setTimeout(tick, 60_000 - (Date.now() % 60_000) + 50);
}

async function start() {
  const res = await fetch('spots.json');
  spots = loadSpots(await res.json());
  setUpSettings();
  tick();
  locateIfAllowed();
  document.addEventListener('visibilitychange', () => { if (!document.hidden) tick(); });

  // Chrome, Edge and Samsung Internet offer their own install dialog; Safari,
  // Firefox and in-app browsers don't, so the button explains the way there.
  $('install').hidden = isInstalled();
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    installPrompt = e;
  });
  window.addEventListener('appinstalled', () => {
    $('install').hidden = true;
    $('install-hint').hidden = true;
  });
  $('install').addEventListener('click', async () => {
    if (!installPrompt) {
      const hint = $('install-hint');
      hint.hidden = false;
      hint.scrollIntoView({ behavior: 'smooth', block: 'center' });
      return;
    }
    installPrompt.prompt();
    const { outcome } = await installPrompt.userChoice;
    installPrompt = null;
    if (outcome === 'accepted') $('install').hidden = true;
  });
  if ('serviceWorker' in navigator && location.protocol === 'https:') {
    navigator.serviceWorker.register('sw.js').catch(() => {});
  }
}

start();
