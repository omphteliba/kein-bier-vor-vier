import BeerCore
import SwiftUI
import WidgetKit

/// One moment of the widget: the beer o'clock status at `date`.
struct BeerEntry: TimelineEntry {
    let date: Date
    let status: BeerStatus
}

/// Widgets and complications switch exactly when a round starts: the timeline
/// has one entry per round for the next 24 hours, then asks again.
struct BeerTimelineProvider: TimelineProvider {
    func placeholder(in context: Context) -> BeerEntry { entry(at: .now) }

    func getSnapshot(in context: Context, completion: @escaping (BeerEntry) -> Void) {
        completion(entry(at: .now))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<BeerEntry>) -> Void) {
        let now = Date.now
        let rounds = BeerClock.rounds(from: now, until: now.addingTimeInterval(24 * 3600), edition: Lang.edition)
        let entries = [entry(at: now)] + rounds.map { entry(at: $0.at) }
        completion(Timeline(entries: entries, policy: .atEnd))
    }

    private func entry(at date: Date) -> BeerEntry {
        BeerEntry(date: date, status: BeerClock.status(at: date, edition: Lang.edition))
    }
}
