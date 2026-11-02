import BeerCore
import Foundation

/// Outgoing links: Apple Maps, Untappd (linking only, no API) and websites.
enum Links {
    /// The brewery (or the city, without one) in Maps. English names find the most.
    static func map(_ spot: BeerSpot, _ brewery: Brewery?) -> URL {
        let query = [brewery?.name, spot.place(of: brewery, german: false), spot.country(german: false)]
            .compactMap { $0 }
            .joined(separator: ", ")
        return url("https://maps.apple.com/?q=", query)
    }

    /// Maps' own list of breweries around a place - it knows far more than we do.
    static func breweriesNearby(_ spot: BeerSpot, day: Int) -> URL {
        let point = spot.location(onDay: day)
        return URL(string: "https://maps.apple.com/?q=Brewery&sll=\(point.lat),\(point.lon)&z=12")!
    }

    /// The brewery on Untappd, which keeps track of its current beers.
    static func untappd(_ brewery: Brewery) -> URL {
        url("https://untappd.com/search?type=brewery&q=", brewery.name)
    }

    private static func url(_ base: String, _ query: String) -> URL {
        URL(string: base + (query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? ""))!
    }
}
