import Foundation

/// A position on Earth in decimal degrees.
public struct GeoPoint: Codable, Hashable, Sendable {
    public let lat: Double
    public let lon: Double

    public init(lat: Double, lon: Double) {
        self.lat = lat
        self.lon = lon
    }

    /// Great-circle distance (haversine), good to well under 1 % for our purposes.
    public func distanceKm(to other: GeoPoint) -> Double {
        let radians = Double.pi / 180
        let dLat = (other.lat - lat) * radians
        let dLon = (other.lon - lon) * radians
        let a = pow(sin(dLat / 2), 2) + cos(lat * radians) * cos(other.lat * radians) * pow(sin(dLon / 2), 2)
        return 2 * 6371.0088 * asin(sqrt(a))
    }
}

/// A brewery, big or small, by name only. `town` and the coordinates say where
/// it is when that isn't the spot's own city.
public struct Brewery: Codable, Hashable, Sendable {
    public let name: String
    public let town: String?
    public let townEn: String?
    public let lat: Double?
    public let lon: Double?
    public let website: String?

    public var location: GeoPoint? {
        guard let lat, let lon else { return nil }
        return GeoPoint(lat: lat, lon: lon)
    }

    public func town(german: Bool) -> String? { german ? town : (townEn ?? town) }
}

/// A place where the beer hour can start, with a small selection of breweries
/// that take turns day by day. Names are German, with English ones alongside.
public struct BeerSpot: Codable, Hashable, Sendable, Identifiable {
    public let city: String
    public let cityEn: String?
    public let short: String?
    public let shortEn: String?
    public let zone: String
    public let country: String
    public let lat: Double
    public let lon: Double
    public let breweries: [Brewery]

    public var id: String { zone }
    public var timeZone: TimeZone { TimeZone(identifier: zone)! }
    public var location: GeoPoint { GeoPoint(lat: lat, lon: lon) }

    /// The country's flag emoji, built from regional indicator symbols.
    public var flag: String {
        country.uppercased().unicodeScalars
            .compactMap { UnicodeScalar(0x1F1E6 - 0x41 + $0.value) }
            .map(String.init)
            .joined()
    }

    public func city(german: Bool) -> String { german ? city : (cityEn ?? city) }

    /// Short enough for a small complication.
    public func shortName(german: Bool) -> String {
        if german { return short ?? city }
        if let cityEn { return shortEn ?? cityEn }
        return short ?? city
    }

    public func country(german: Bool) -> String {
        Locale(identifier: german ? "de" : "en").localizedString(forRegionCode: country) ?? country
    }

    /// The brewery of the day: the list takes turns, one per local day.
    public func brewery(onDay day: Int) -> Brewery? {
        breweries.isEmpty ? nil : breweries[floorMod(day, breweries.count)]
    }

    /// All breweries here, starting with the one of the day.
    public func breweries(fromDay day: Int) -> [Brewery] {
        guard !breweries.isEmpty else { return [] }
        let first = floorMod(day, breweries.count)
        return breweries.indices.map { breweries[($0 + first) % breweries.count] }
    }

    /// Where to say it's beer o'clock: the town of the day's brewery if it has
    /// one (same time zone, so just as true), otherwise the city.
    public func place(german: Bool, day: Int) -> String { place(of: brewery(onDay: day), german: german) }

    public func place(of brewery: Brewery?, german: Bool) -> String {
        brewery?.town(german: german) ?? city(german: german)
    }

    /// Where `place` is, for distances.
    public func location(onDay day: Int) -> GeoPoint { brewery(onDay: day)?.location ?? location }

    /// The local date here at `instant`, as days since 1970-01-01 - the same
    /// number Java's `LocalDate.toEpochDay()` gives, so both apps agree.
    public func epochDay(at instant: Date) -> Int {
        var local = Calendar(identifier: .gregorian)
        local.timeZone = timeZone
        let date = local.dateComponents([.year, .month, .day], from: instant)
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC")!
        return Int((utc.date(from: date)!.timeIntervalSince1970 / 86_400).rounded(.down))
    }
}

func floorMod(_ a: Int, _ n: Int) -> Int { ((a % n) + n) % n }

/// Which saying the app follows, and so which hour counts as beer o'clock.
public enum Edition: Int, CaseIterable, Sendable {
    /// German: "Kein Bier vor Vier" - no beer before 4 p.m.
    case keinBierVorVier = 16
    /// English: "It's five o'clock somewhere".
    case fiveOClockSomewhere = 17

    public var hour: Int { rawValue }

    /// German speakers get their saying, everyone else the English one.
    public static func forLanguage(_ code: String?) -> Edition { code == "de" ? .keinBierVorVier : .fiveOClockSomewhere }
}

/// Every place, from data/spots.json.
public enum BeerSpots {
    public static let all: [BeerSpot] = {
        struct File: Decodable { let spots: [BeerSpot] }
        let url = Bundle.module.url(forResource: "spots", withExtension: "json")!
        // swiftlint:disable:next force_try
        return try! JSONDecoder().decode(File.self, from: Data(contentsOf: url)).spots
    }()
}
