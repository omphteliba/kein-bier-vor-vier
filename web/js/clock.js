// The beer clock for the web: a port of core/BeerClock.kt and core/BeerSpots.kt.
// Plain JavaScript without dependencies, so it runs in the browser and in
// Node's test runner alike. Times are milliseconds since the epoch (UTC);
// the browser's Intl API knows every time zone and its daylight saving rules.

export const EDITIONS = {
  vier: { id: 'vier', hour: 16 }, // German: "Kein Bier vor Vier"
  five: { id: 'five', hour: 17 }, // English: "It's five o'clock somewhere"
};

/** German speakers get their saying, everyone else the English one. */
export const editionForLanguage = (lang) => (lang === 'de' ? EDITIONS.vier : EDITIONS.five);

const MINUTE = 60_000;
const DAY = 86_400_000;

const floorMod = (a, n) => ((a % n) + n) % n;

const partsFormats = new Map();
function partsFormat(zone) {
  let f = partsFormats.get(zone);
  if (!f) {
    f = new Intl.DateTimeFormat('en-US', {
      timeZone: zone, hourCycle: 'h23',
      year: 'numeric', month: 'numeric', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric',
    });
    partsFormats.set(zone, f);
  }
  return f;
}

const QUARTER = 15 * MINUTE;
const offsets = new Map();

/**
 * How far [zone] is ahead of UTC at [t], in milliseconds. Asking Intl is slow,
 * so offsets are cached per quarter hour: every daylight saving switch in the
 * world happens on a quarter hour (UTC), so the offset can't change within one.
 */
function offsetAt(t, zone) {
  const quarter = Math.floor(t / QUARTER);
  const key = zone + quarter;
  let offset = offsets.get(key);
  if (offset === undefined) {
    const start = quarter * QUARTER;
    const p = {};
    for (const { type, value } of partsFormat(zone).formatToParts(start)) {
      if (type !== 'literal') p[type] = Number(value);
    }
    offset = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second) - start;
    if (offsets.size > 100_000) offsets.clear();
    offsets.set(key, offset);
  }
  return offset;
}

/** The wall clock in [zone] at [t]: {year, month, day, hour, minute, second}. */
export function localParts(t, zone) {
  const d = new Date(t + offsetAt(t, zone));
  return {
    year: d.getUTCFullYear(), month: d.getUTCMonth() + 1, day: d.getUTCDate(),
    hour: d.getUTCHours(), minute: d.getUTCMinutes(), second: d.getUTCSeconds(),
  };
}

/** Days since 1970-01-01 of a local date, like LocalDate.toEpochDay(). */
export const epochDay = ({ year, month, day }) => Math.floor(Date.UTC(year, month - 1, day) / DAY);

/** The instant when the wall clock in [zone] shows the given local time. */
export function zonedTime(year, month, day, hour, minute, zone) {
  const wall = Date.UTC(year, month - 1, day, hour, minute);
  let t = wall - offsetAt(wall, zone);
  const corrected = wall - offsetAt(t, zone);
  if (corrected !== t) t = corrected; // the guess crossed a daylight saving switch
  return t;
}

/** The first start of the beer hour in [spot]'s zone strictly after [now]. */
export function nextBeerHourStart(now, spot, edition = EDITIONS.vier) {
  const local = localParts(now, spot.zone);
  let candidate = zonedTime(local.year, local.month, local.day, edition.hour, 0, spot.zone);
  if (candidate <= now) {
    const tomorrow = new Date(Date.UTC(local.year, local.month - 1, local.day + 1));
    candidate = zonedTime(tomorrow.getUTCFullYear(), tomorrow.getUTCMonth() + 1, tomorrow.getUTCDate(), edition.hour, 0, spot.zone);
  }
  return candidate;
}

/**
 * Spots whose beer hour starts at the same moment (Germany, France and
 * Belgium, say) take turns being first, one day each.
 */
function takingTurns(list, day) {
  return list.map((_, i) => list[floorMod(i + day, list.length)]);
}

/** The local date in [spot]'s zone at [t], as an epoch day. */
export const dayAt = (spot, t) => epochDay(localParts(t, spot.zone));

/**
 * Where it's beer o'clock right now and where the next round starts.
 * openNow: [{spot, day, hour, minute, minutesIn}], most recently started first.
 * next: {at, countdown (ms), spots}.
 */
export function status(now, edition, spots) {
  if (!spots.length) throw new Error('Need at least one spot');

  const open = [];
  for (const spot of spots) {
    const p = localParts(now, spot.zone);
    if (p.hour === edition.hour) {
      open.push({ spot, day: epochDay(p), hour: p.hour, minute: p.minute, minutesIn: p.minute });
    }
  }
  // Group by local time, earliest local time (= most recent start) first.
  const byTime = new Map();
  for (const o of open.sort((a, b) => a.minute - b.minute)) {
    const k = o.minute;
    if (!byTime.has(k)) byTime.set(k, []);
    byTime.get(k).push(o);
  }
  const openNow = [...byTime.values()].flatMap((same) => takingTurns(same, same[0].day));

  const byStart = new Map();
  for (const spot of spots) {
    const at = nextBeerHourStart(now, spot, edition);
    if (!byStart.has(at)) byStart.set(at, []);
    byStart.get(at).push(spot);
  }
  const at = Math.min(...byStart.keys());
  const same = byStart.get(at);
  const next = { at, countdown: at - now, spots: takingTurns(same, dayAt(same[0], at)) };
  return { openNow, next };
}

/** All rounds starting after [from] up to and including [until], in order. */
export function rounds(from, until, edition, spots) {
  const result = [];
  let t = from;
  for (;;) {
    const { next } = status(t, edition, spots);
    if (next.at > until) return result;
    result.push({ ...next, countdown: next.at - from });
    t = next.at;
  }
}

// ---- Spots and breweries --------------------------------------------------

/** The spots from data/spots.json, with English names filled in. */
export function loadSpots(json) {
  return json.spots.map((s) => ({
    city: s.city,
    cityEn: s.cityEn ?? s.city,
    zone: s.zone,
    country: s.country,
    lat: s.lat,
    lon: s.lon,
    breweries: s.breweries.map((b) => ({
      name: b.name,
      town: b.town ?? null,
      townEn: b.townEn ?? b.town ?? null,
      lat: b.lat ?? null,
      lon: b.lon ?? null,
      website: b.website ?? null,
    })),
  }));
}

/** The brewery of the day: the list takes turns, one per local day. */
export const breweryOn = (spot, day) =>
  spot.breweries.length ? spot.breweries[floorMod(day, spot.breweries.length)] : null;

/** All breweries here, starting with the one of the day. */
export function breweriesFrom(spot, day) {
  const n = spot.breweries.length;
  const first = floorMod(day, n || 1);
  return spot.breweries.map((_, i) => spot.breweries[(first + i) % n]);
}

export const cityName = (spot, lang) => (lang === 'de' ? spot.city : spot.cityEn);

/** The town of [brewery], or the spot's city. */
export function placeOf(spot, brewery, lang) {
  if (brewery?.town) return lang === 'de' ? brewery.town : brewery.townEn;
  return cityName(spot, lang);
}

/** Where to say it's beer o'clock on [day]: the town of the day's brewery, or the city. */
export const place = (spot, day, lang) => placeOf(spot, breweryOn(spot, day), lang);

/** Where [place] is, for distances. */
export function locationOn(spot, day) {
  const b = breweryOn(spot, day);
  return b?.lat != null ? { lat: b.lat, lon: b.lon } : { lat: spot.lat, lon: spot.lon };
}

const countryNames = new Map();
export function countryName(code, lang) {
  const key = lang === 'de' ? 'de' : 'en';
  if (!countryNames.has(key)) countryNames.set(key, new Intl.DisplayNames([key], { type: 'region' }));
  return countryNames.get(key).of(code);
}

// ---- Distances and text -----------------------------------------------------

const EARTH_RADIUS_KM = 6371.0088;
const rad = (d) => (d * Math.PI) / 180;

/** Great-circle distance (haversine). */
export function distanceKm(a, b) {
  const dLat = rad(b.lat - a.lat);
  const dLon = rad(b.lon - a.lon);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLon / 2) ** 2;
  return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(h));
}

/** Below this distance you are basically there. */
export const THERE_KM = 50;

/** "7.600 km" / "7,600 km", rounded like the apps; null when you're there. */
export function formatDistance(km, lang) {
  if (km < THERE_KM) return null;
  const rounded = km < 1000 ? Math.round(km / 10) * 10 : Math.round(km / 100) * 100;
  return `${rounded.toLocaleString(lang === 'de' ? 'de-DE' : 'en-US')} km`;
}

/** "12 min" or "1 h 5 min", rounded up so it never says 0 before the round starts. */
export function formatCountdown(ms) {
  const minutes = Math.ceil(ms / MINUTE);
  return minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
}
