import Foundation

/// A spot where the beer hour is running, on its local `day`.
public struct OpenSpot: Hashable, Sendable {
    public let spot: BeerSpot
    public let day: Int
    public let hour: Int
    public let minute: Int
    public let edition: Edition

    /// Minutes since the beer hour started locally.
    public var minutesIn: Int { hour * 60 + minute - edition.hour * 60 }

    /// Today's brewery here.
    public var brewery: Brewery? { spot.brewery(onDay: day) }

    public func place(german: Bool) -> String { spot.place(german: german, day: day) }
}

/// The next moment some spots reach the beer hour, and which ones.
public struct NextRound: Hashable, Sendable {
    public let at: Date
    public let spots: [BeerSpot]

    public func countdown(from now: Date) -> TimeInterval { at.timeIntervalSince(now) }
}

public struct BeerStatus: Hashable, Sendable {
    /// Spots inside the beer hour right now, most recently started first.
    public let openNow: [OpenSpot]
    public let next: NextRound
}

public enum BeerClock {
    public static func status(
        at now: Date,
        edition: Edition = .keinBierVorVier,
        spots: [BeerSpot] = BeerSpots.all
    ) -> BeerStatus {
        precondition(!spots.isEmpty, "Need at least one spot")

        let open: [OpenSpot] = spots.compactMap { spot in
            let time = calendar(for: spot).dateComponents([.hour, .minute], from: now)
            guard time.hour == edition.hour, let hour = time.hour, let minute = time.minute else { return nil }
            return OpenSpot(spot: spot, day: spot.epochDay(at: now), hour: hour, minute: minute, edition: edition)
        }
        let byTime = Dictionary(grouping: open) { $0.hour * 60 + $0.minute }
        let openNow = byTime.keys.sorted().flatMap { key in
            let sameTime = byTime[key]!
            return sameTime.takingTurns(day: sameTime[0].day)
        }

        let starts = spots.map { (spot: $0, at: nextBeerHourStart(after: now, spot: $0, edition: edition)) }
        let nextAt = starts.map(\.at).min()!
        let sameStart = starts.filter { $0.at == nextAt }.map(\.spot)
        let next = NextRound(at: nextAt, spots: sameStart.takingTurns(day: sameStart[0].epochDay(at: nextAt)))
        return BeerStatus(openNow: openNow, next: next)
    }

    /// All rounds starting after `from` up to and including `until`, in order.
    public static func rounds(
        from: Date,
        until: Date,
        edition: Edition = .keinBierVorVier,
        spots: [BeerSpot] = BeerSpots.all
    ) -> [NextRound] {
        var result: [NextRound] = []
        var t = from
        while true {
            let next = status(at: t, edition: edition, spots: spots).next
            if next.at > until { return result }
            result.append(next)
            t = next.at
        }
    }

    /// The first start of the beer hour in `spot`'s zone strictly after `now`.
    public static func nextBeerHourStart(after now: Date, spot: BeerSpot, edition: Edition = .keinBierVorVier) -> Date {
        let cal = calendar(for: spot)
        var start = cal.dateComponents([.year, .month, .day], from: now)
        start.hour = edition.hour
        start.minute = 0
        start.second = 0
        let today = cal.date(from: start)!
        return today > now ? today : cal.date(byAdding: .day, value: 1, to: today)!
    }

    private static func calendar(for spot: BeerSpot) -> Calendar {
        var cal = Calendar(identifier: .gregorian)
        cal.timeZone = spot.timeZone
        return cal
    }
}

extension Array {
    /// Spots whose beer hour starts at the same moment (Germany, France and
    /// Belgium, say) take turns being first, one day each.
    func takingTurns(day: Int) -> [Element] {
        indices.map { self[floorMod($0 + day, count)] }
    }
}

/// "12 min" or "1 h 5 min", rounded up so it never says 0 before the round starts.
public func formatCountdown(_ interval: TimeInterval) -> String {
    let minutes = (Int(interval.rounded(.down)) + 59) / 60
    return minutes < 60 ? "\(minutes) min" : "\(minutes / 60) h \(minutes % 60) min"
}

/// "7.600 km" (or "7,600 km" in English): rounded to 100 km above 1000 km and
/// to 10 km below. Nil when you're already there (under 50 km).
public func formatDistance(_ km: Double, german: Bool) -> String? {
    guard km >= 50 else { return nil }
    let rounded = km < 1000 ? (km / 10).rounded() * 10 : (km / 100).rounded() * 100
    let formatter = NumberFormatter()
    formatter.locale = Locale(identifier: german ? "de_DE" : "en_US")
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    return "\(formatter.string(from: NSNumber(value: rounded))!) km"
}
