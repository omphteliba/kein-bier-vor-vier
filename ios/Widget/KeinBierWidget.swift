import BeerCore
import SwiftUI
import WidgetKit

/// Home screen widget: where it is beer o'clock right now and where it's next.
/// The timeline switches exactly when a round starts (see BeerTimelineProvider).
@main
struct KeinBierWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "KeinBierWidget", provider: BeerTimelineProvider()) { entry in
            WidgetView(entry: entry)
                .containerBackground(for: .widget) {
                    GeometryReader { g in BeerGlass(foamAt: g.size.height * 0.48) }
                }
        }
        .configurationDisplayName("app_name")
        .description("widget_description")
        .supportedFamilies([.systemSmall, .systemMedium])
        .contentMarginsDisabled()
    }
}

private struct WidgetView: View {
    let entry: BeerEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let current = entry.status.openNow[0]
        let next = entry.status.next
        let nextSpot = next.spots[0]
        VStack(spacing: 4) {
            Text(Lang.nowIn(current.edition).uppercased())
                .font(.aleo(family == .systemSmall ? 11 : 13, .black))
                .tracking(1)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(current.spot.flag).font(.system(size: family == .systemSmall ? 30 : 36))
            Spacer(minLength: 10)
            Text(current.place(german: Lang.isGerman))
                .font(.aleo(family == .systemSmall ? 18 : 22, .black))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            if family != .systemSmall, let brewery = current.brewery {
                Text(brewery.name).font(.subheadline).lineLimit(1)
            }
            Text(Lang.format(
                "widget_next",
                shortTime(next.at),
                "\(nextSpot.flag) \(nextSpot.place(german: Lang.isGerman, day: nextSpot.epochDay(at: next.at)))"
            ))
            .font(.caption2)
            .lineLimit(1)
            .minimumScaleFactor(0.7)
        }
        .foregroundStyle(Color.ink)
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
