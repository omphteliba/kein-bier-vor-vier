import BeerCore
import SwiftUI
import WidgetKit

/// Complications: the flag and the place where it's beer o'clock right now,
/// switching exactly when the next round starts.
@main
struct BeerComplication: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "BeerComplication", provider: BeerTimelineProvider()) { entry in
            ComplicationView(entry: entry)
                .containerBackground(for: .widget) { Color.clear }
        }
        .configurationDisplayName("app_name")
        .description("widget_description")
        .supportedFamilies([.accessoryCircular, .accessoryRectangular, .accessoryInline, .accessoryCorner])
    }
}

private struct ComplicationView: View {
    let entry: BeerEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        let current = entry.status.openNow[0]
        let next = entry.status.next
        let nextSpot = next.spots[0]
        let place = current.place(german: Lang.isGerman)
        switch family {
        case .accessoryCircular:
            ZStack {
                AccessoryWidgetBackground()
                VStack(spacing: 0) {
                    Text(current.spot.flag).font(.system(size: 20))
                    Text(current.spot.shortName(german: Lang.isGerman)).font(.system(size: 10, weight: .semibold))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
            }
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 1) {
                Text("\(current.spot.flag) \(place)").font(.headline).lineLimit(1)
                Text(current.brewery?.name ?? String(localized: "no_local_beer")).font(.caption).lineLimit(1)
                HStack(spacing: 3) {
                    Text("→ \(nextSpot.flag)")
                    Text(next.at, style: .relative)
                }
                .font(.caption2)
                .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        case .accessoryCorner:
            Text(current.spot.flag)
                .font(.system(size: 22))
                .widgetLabel { Text(place) }
        default:
            Text("\(current.spot.flag) \(place)")
        }
    }
}
