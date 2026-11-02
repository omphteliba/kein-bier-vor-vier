import BeerCore
import SwiftUI

@main
struct KeinBierWatchApp: App {
    var body: some Scene {
        WindowGroup {
            WatchContentView()
        }
    }
}

/// Looking into a full glass: foam across the top with the saying, beer below
/// with the flag, the place, the brewery of the day and the next round.
struct WatchContentView: View {
    @StateObject private var location = UserLocation()
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { context in
            let status = BeerClock.status(at: context.date, edition: Lang.edition)
            let current = status.openNow[0]
            let next = status.next
            let nextSpot = next.spots[0]
            GeometryReader { g in
                VStack(spacing: 2) {
                    Spacer().frame(height: g.size.height * 0.16)
                    Text(Lang.nowIn(current.edition).uppercased())
                        .font(.aleo(11, .bold))
                        .tracking(1)
                        .foregroundStyle(Color.ink)
                    Spacer().frame(height: g.size.height * 0.09)
                    FlagCoaster(flag: current.spot.flag, size: 36)
                    Text(current.place(german: Lang.isGerman))
                        .font(.aleo(21, .black))
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                    Text(([current.spot.country(german: Lang.isGerman)] + sinceAndDistance(current, from: location.point))
                        .joined(separator: " · "))
                        .font(.system(size: 10))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Text(current.brewery?.name ?? String(localized: "no_local_beer"))
                        .font(.aleo(13, .bold))
                        .lineLimit(1)
                        .padding(.top, 4)
                    Text("→ \(nextSpot.flag) \(nextSpot.place(german: Lang.isGerman, day: nextSpot.epochDay(at: next.at))) · \(formatCountdown(next.countdown(from: context.date)))")
                        .font(.system(size: 10))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    Spacer(minLength: 0)
                }
                .foregroundStyle(Color.inkDeep)
                .padding(.horizontal, 20)
                .frame(width: g.size.width, height: g.size.height)
                .background {
                    TimelineView(.animation(minimumInterval: 1.0 / 15, paused: reduceMotion)) { anim in
                        BeerGlass(
                            foamAt: g.size.height * 0.27,
                            top: .foamWhite,
                            foamScale: 1.3,
                            phase: reduceMotion ? 0.3 : anim.date.timeIntervalSinceReferenceDate / 9
                        )
                    }
                }
            }
            .ignoresSafeArea()
        }
        .onAppear { location.start() }
    }
}
