import XCTest
@testable import BeerCore

/// The same checks as the Kotlin tests in core/, so both apps behave alike.
final class BeerCoreTests: XCTestCase {
    private func spot(_ zone: String) -> BeerSpot { BeerSpots.all.first { $0.zone == zone }! }
    private func date(_ iso: String) -> Date { ISO8601DateFormatter().date(from: iso)! }

    func testDataLoads() {
        XCTAssertEqual(BeerSpots.all.count, 54)
        for spot in BeerSpots.all {
            XCTAssertNotNil(TimeZone(identifier: spot.zone), spot.zone)
            XCTAssertLessThanOrEqual(spot.shortName(german: true).count, 7, spot.city)
            XCTAssertLessThanOrEqual(spot.shortName(german: false).count, 7, spot.city)
        }
    }

    func testFlagsAndNames() {
        let germany = spot("Europe/Berlin")
        XCTAssertEqual(germany.flag, "🇩🇪")
        XCTAssertEqual(germany.country(german: true), "Deutschland")
        XCTAssertEqual(germany.country(german: false), "Germany")
        XCTAssertEqual(germany.city(german: false), "Munich")
        XCTAssertEqual(spot("Asia/Tokyo").shortName(german: false), "Tokyo")
    }

    func testSomewhereItIsAlwaysBeerOClock() {
        for edition in Edition.allCases {
            var t = date("2026-01-01T00:00:00Z")
            let end = t.addingTimeInterval(366 * 86_400)
            while t < end {
                XCTAssertFalse(BeerClock.status(at: t, edition: edition).openNow.isEmpty, "\(t) \(edition)")
                t = t.addingTimeInterval(7 * 60 * 7) // every 49 minutes keeps the test quick
            }
        }
    }

    func testOpenSpotsAndNextRound() {
        let spots = [spot("Europe/Berlin"), spot("Asia/Kathmandu"), spot("Asia/Kolkata")]
        // 10:20 UTC = 16:05 Kathmandu (+5:45), 15:50 Mumbai (+5:30)
        let status = BeerClock.status(at: date("2026-11-02T10:20:00Z"), spots: spots)
        XCTAssertEqual(status.openNow.map(\.spot.zone), ["Asia/Kathmandu"])
        XCTAssertEqual(status.openNow.first?.minutesIn, 5)
        XCTAssertEqual(status.next.spots.map(\.zone), ["Asia/Kolkata"])
        XCTAssertEqual(status.next.countdown(from: date("2026-11-02T10:20:00Z")), 600)
    }

    func testDaylightSavingTime() {
        let berlin = spot("Europe/Berlin")
        XCTAssertEqual(BeerClock.nextBeerHourStart(after: date("2026-07-01T00:00:00Z"), spot: berlin), date("2026-07-01T14:00:00Z"))
        XCTAssertEqual(BeerClock.nextBeerHourStart(after: date("2026-12-01T00:00:00Z"), spot: berlin), date("2026-12-01T15:00:00Z"))
        XCTAssertEqual(BeerClock.nextBeerHourStart(after: date("2026-11-02T15:00:00Z"), spot: berlin), date("2026-11-03T15:00:00Z"))
    }

    func testCentralEuropeTakesTurns() {
        let base = date("2026-12-01T15:30:00Z") // 16:30 in Berlin, Paris, Brussels and Lagos
        let sameTime = BeerClock.status(at: base).openNow.map(\.spot.country)
        XCTAssertEqual(Set(sameTime), ["DE", "FR", "BE", "NG"])
        let firsts = (0..<sameTime.count).map {
            BeerClock.status(at: base.addingTimeInterval(Double($0) * 86_400)).openNow[0].spot.country
        }
        XCTAssertEqual(Set(firsts), Set(sameTime))
    }

    func testBreweryOfTheDayMatchesKotlin() {
        let germany = spot("Europe/Berlin")
        // 2026-11-02 is epoch day 20759; LocalDate.toEpochDay() agrees.
        XCTAssertEqual(germany.epochDay(at: date("2026-11-02T12:00:00Z")), 20_759)
        let day = 20_759
        XCTAssertEqual(germany.breweries(fromDay: day).first, germany.brewery(onDay: day))
        XCTAssertEqual(Set(germany.breweries(fromDay: day)), Set(germany.breweries))
        XCTAssertNotEqual(germany.brewery(onDay: day), germany.brewery(onDay: day + 1))
    }

    func testRounds() {
        let from = date("2026-11-02T00:00:00Z")
        let rounds = BeerClock.rounds(from: from, until: from.addingTimeInterval(86_400))
        XCTAssertGreaterThan(rounds.count, 24)
        XCTAssertEqual(rounds.map(\.at), rounds.map(\.at).sorted())
    }

    func testFormatting() {
        XCTAssertEqual(formatCountdown(600), "10 min")
        XCTAssertEqual(formatCountdown(3_900), "1 h 5 min")
        XCTAssertNil(formatDistance(12, german: true))
        XCTAssertEqual(formatDistance(483, german: true), "480 km")
        XCTAssertEqual(formatDistance(7_563, german: true), "7.600 km")
        XCTAssertEqual(formatDistance(7_563, german: false), "7,600 km")
        XCTAssertEqual(GeoPoint(lat: 0, lon: 0).distanceKm(to: GeoPoint(lat: 0, lon: 90)), 10_007.5, accuracy: 1)
    }
}
