// Run with: node --test web/test
// Mirrors core/src/test (BeerClockTest, BreweryOfTheDayTest, GeoPointTest).
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
  EDITIONS, editionForLanguage, status, rounds, nextBeerHourStart, loadSpots, breweryOn, breweriesFrom,
  place, distanceKm, formatDistance, formatCountdown, epochDay, countryName, dayAt,
} from '../js/clock.js';

const ALL = loadSpots(JSON.parse(readFileSync(new URL('../spots.json', import.meta.url), 'utf-8')));
const t = (iso) => Date.parse(iso);
const MIN = 60_000;
const spot = (city, zone, extra = {}) => ({ city, cityEn: city, zone, country: 'XX', lat: 0, lon: 0, breweries: [], ...extra });
const berlin = spot('Berlin', 'Europe/Berlin');
const kathmandu = spot('Kathmandu', 'Asia/Kathmandu');
const mumbai = spot('Mumbai', 'Asia/Kolkata');

test('every zone in the data exists', () => {
  for (const s of ALL) assert.doesNotThrow(() => new Intl.DateTimeFormat('en', { timeZone: s.zone }), s.zone);
});

test('somewhere it is always beer o\'clock, in both editions', () => {
  for (const edition of Object.values(EDITIONS)) {
    // A year, across both daylight saving switches. All offsets are whole quarter
    // hours, so one sample inside every quarter hour finds any gap.
    for (let now = t('2026-01-01T00:07:00Z'); now < t('2027-01-01T00:00:00Z'); now += 15 * MIN) {
      const s = status(now, edition, ALL);
      assert.ok(s.openNow.length > 0, `nothing open at ${new Date(now).toISOString()} (${edition.id})`);
      assert.ok(s.next.countdown > 0);
    }
  }
});

test('five o\'clock edition starts an hour later', () => {
  const now = t('2026-11-02T00:00:00Z');
  assert.equal(nextBeerHourStart(now, berlin, EDITIONS.five) - nextBeerHourStart(now, berlin, EDITIONS.vier), 60 * MIN);
});

test('edition follows the language', () => {
  assert.equal(editionForLanguage('de'), EDITIONS.vier);
  assert.equal(editionForLanguage('en'), EDITIONS.five);
  assert.equal(editionForLanguage('fr'), EDITIONS.five);
});

test('open spots are those in the 16 o\'clock hour, newest first', () => {
  // 10:20 UTC = 16:05 Kathmandu (+5:45), 15:50 Mumbai (+5:30)
  const s = status(t('2026-11-02T10:20:00Z'), EDITIONS.vier, [berlin, kathmandu, mumbai]);
  assert.deepEqual(s.openNow.map((o) => o.spot), [kathmandu]);
  assert.equal(s.openNow[0].minutesIn, 5);
  assert.deepEqual(s.next.spots, [mumbai]);
  assert.equal(s.next.countdown, 10 * MIN);
});

test('next round rolls over to tomorrow once 16 o\'clock has passed', () => {
  assert.equal(nextBeerHourStart(t('2026-11-02T15:00:00Z'), berlin), t('2026-11-03T15:00:00Z'));
});

test('daylight saving time is respected', () => {
  assert.equal(nextBeerHourStart(t('2026-07-01T00:00:00Z'), berlin), t('2026-07-01T14:00:00Z'));
  assert.equal(nextBeerHourStart(t('2026-12-01T00:00:00Z'), berlin), t('2026-12-01T15:00:00Z'));
  // The night of the switch itself.
  assert.equal(nextBeerHourStart(t('2026-03-29T00:30:00Z'), berlin), t('2026-03-29T14:00:00Z'));
  assert.equal(nextBeerHourStart(t('2026-10-25T00:30:00Z'), berlin), t('2026-10-25T15:00:00Z'));
});

test('rounds lists every start in order and matches the open spot', () => {
  const from = t('2026-11-02T00:00:00Z');
  const list = rounds(from, from + 24 * 60 * MIN, EDITIONS.vier, ALL);
  assert.ok(list.length > 24);
  for (let i = 1; i < list.length; i++) assert.ok(list[i].at > list[i - 1].at);
  for (const round of list) {
    const open = status(round.at, EDITIONS.vier, ALL).openNow[0];
    assert.equal(open.minutesIn, 0);
    assert.ok(round.spots.includes(open.spot));
  }
});

test('spots reaching 16 o\'clock at the same moment are grouped', () => {
  const lagos = spot('Lagos', 'Africa/Lagos');
  const s = status(t('2026-12-01T12:00:00Z'), EDITIONS.vier, [berlin, lagos, kathmandu]);
  assert.deepEqual(new Set(s.next.spots), new Set([berlin, lagos]));
});

test('spots on the same clock take turns being first, day by day', () => {
  const at = (iso) => status(t(iso), EDITIONS.vier, ALL).openNow[0].spot.country;
  const firsts = new Set(['2026-12-01', '2026-12-02', '2026-12-03', '2026-12-04'].map((d) => at(`${d}T15:10:00Z`)));
  assert.ok(firsts.size > 1, 'always the same country first');
});

test('breweries take turns day by day and come round again', () => {
  const s = ALL.find((x) => x.breweries.length > 2);
  const day = epochDay({ year: 2026, month: 11, day: 2 });
  const n = s.breweries.length;
  assert.notEqual(breweryOn(s, day), breweryOn(s, day + 1));
  assert.equal(breweryOn(s, day), breweryOn(s, day + n));
  const from = breweriesFrom(s, day);
  assert.equal(from[0], breweryOn(s, day));
  assert.equal(new Set(from).size, n);
});

test('the place is the town of the brewery of the day', () => {
  const s = ALL.find((x) => x.breweries.some((b) => b.town === 'Kirn'));
  const i = s.breweries.findIndex((b) => b.town === 'Kirn');
  const n = s.breweries.length;
  const day = epochDay({ year: 2026, month: 11, day: 2 });
  const kirnDay = day + ((i - (day % n)) + n) % n;
  assert.equal(place(s, kirnDay, 'de'), 'Kirn');
  assert.equal(dayAt(s, Date.UTC(2026, 10, 2, 12)), day);
});

test('picks the same as the Android app', () => {
  // Phone app (English) on 2026-11-12 at 16:06 UTC: Mundolsheim with Mogwaï first,
  // also Brussels, Lagos and Ingolstadt; next round Dublin and Leça do Balio in 54 min.
  const now = t('2026-11-12T16:06:00Z');
  const s = status(now, EDITIONS.five, ALL);
  const names = s.openNow.map((o) => place(o.spot, o.day, 'en'));
  assert.deepEqual(names, ['Mundolsheim', 'Brussels', 'Lagos', 'Ingolstadt']);
  assert.equal(breweryOn(s.openNow[0].spot, s.openNow[0].day).name, 'Mogwaï Beer Company');
  assert.deepEqual(s.next.spots.map((x) => place(x, dayAt(x, s.next.at), 'en')).slice(0, 2), ['Dublin', 'Leça do Balio']);
  assert.equal(formatCountdown(s.next.countdown), '54 min');
});

test('distances and their text', () => {
  const berlinPt = { lat: 52.52, lon: 13.405 };
  const paris = { lat: 48.8566, lon: 2.3522 };
  const km = distanceKm(berlinPt, paris);
  assert.ok(km > 870 && km < 890, String(km));
  assert.equal(formatDistance(km, 'de'), '880 km');
  assert.equal(formatDistance(7623, 'de'), '7.600 km');
  assert.equal(formatDistance(7623, 'en'), '7,600 km');
  assert.equal(formatDistance(20, 'en'), null);
});

test('countdown text', () => {
  assert.equal(formatCountdown(1), '1 min');
  assert.equal(formatCountdown(12 * MIN), '12 min');
  assert.equal(formatCountdown(65 * MIN), '1 h 5 min');
});

test('country names follow the language', () => {
  assert.equal(countryName('DE', 'de'), 'Deutschland');
  assert.equal(countryName('DE', 'en'), 'Germany');
});
