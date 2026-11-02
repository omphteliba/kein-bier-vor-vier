import BeerCore
import SwiftUI

/// The phone screen, in the same beer-glass look as the Android app.
struct ContentView: View {
    @StateObject private var location = UserLocation()

    var body: some View {
        TimelineView(.periodic(from: .now, by: 30)) { context in
            let status = BeerClock.status(at: context.date, edition: Lang.edition)
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Header()
                    GlassCard(status: status, user: location.point)
                    NextRoundCard(status: status, now: context.date)
                    BreweriesSection(status: status)
                    Upcoming(now: context.date)
                    Text("drink_responsibly")
                        .font(.footnote)
                        .foregroundStyle(Color.inkSoft)
                        .frame(maxWidth: .infinity)
                        .padding(.top, 8)
                }
                .padding(20)
            }
            .background(Color.cream)
            .foregroundStyle(Color.ink)
        }
        .onAppear { location.start() }
    }
}

private struct Header: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("app_name").font(.aleo(26, .black, relativeTo: .title))
            Text(LocalizedStringKey(Lang.edition == .keinBierVorVier ? "tagline_vier" : "tagline_five"))
                .font(.subheadline)
                .foregroundStyle(Color.inkSoft)
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

/// The hero: cream glass with the saying and the flag, foam, the place in the beer.
private struct GlassCard: View {
    let status: BeerStatus
    let user: GeoPoint?
    @State private var foamAt: CGFloat = 170
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let current = status.openNow[0]
        let alsoNow = status.openNow.dropFirst().filter { $0.hour == current.hour && $0.minute == current.minute }
        VStack(spacing: 0) {
            VStack(spacing: 12) {
                Text(Lang.nowIn(current.edition).uppercased())
                    .font(.aleo(15, .black))
                    .tracking(1.5)
                FlagCoaster(flag: current.spot.flag)
            }
            .padding(.top, 22)
            .padding(.bottom, 20)
            .frame(maxWidth: .infinity, minHeight: 170)
            .background(GeometryReader { g in Color.clear.preference(key: TopHeight.self, value: g.size.height) })

            Link(destination: Links.map(current.spot, current.brewery)) {
                VStack(spacing: 4) {
                    Text(current.place(german: Lang.isGerman))
                        .font(.aleo(40, .black, relativeTo: .largeTitle))
                        .multilineTextAlignment(.center)
                    Text(current.spot.country(german: Lang.isGerman).uppercased())
                        .font(.subheadline.weight(.semibold))
                        .tracking(2)
                        .foregroundStyle(Color.inkDeep)
                    HStack(spacing: 8) {
                        ForEach(sinceAndDistance(current, from: user), id: \.self) { Pill(text: $0) }
                    }
                    .padding(.top, 10)
                }
                .foregroundStyle(Color.ink)
                .frame(maxWidth: .infinity)
            }
            .padding(.top, 22)
            .padding(.horizontal, 20)

            if !alsoNow.isEmpty {
                Text("also_now").font(.caption.weight(.semibold)).foregroundStyle(Color.inkDeep).padding(.top, 14)
                FlowChips(items: Array(alsoNow))
                    .padding(.top, 6)
            }
            Spacer().frame(height: 24)
        }
        .background {
            TimelineView(.animation(minimumInterval: 1.0 / 20, paused: reduceMotion)) { context in
                BeerGlass(foamAt: foamAt, phase: reduceMotion ? 0.3 : context.date.timeIntervalSinceReferenceDate / 9)
            }
        }
        .onPreferenceChange(TopHeight.self) { foamAt = $0 }
        .clipShape(RoundedRectangle(cornerRadius: 28))
        .shadow(color: Color.darkAmber.opacity(0.35), radius: 10, y: 4)
    }
}

private struct TopHeight: PreferenceKey {
    static let defaultValue: CGFloat = 170
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = nextValue() }
}

private struct Pill: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.subheadline.weight(.medium))
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(Capsule().fill(.white.opacity(0.4)))
    }
}

/// The other places on the same clock, as tappable chips.
private struct FlowChips: View {
    let items: [OpenSpot]
    var body: some View {
        HStack(spacing: 6) {
            ForEach(items, id: \.spot.zone) { open in
                Link(destination: Links.map(open.spot, open.brewery)) {
                    Pill(text: "\(open.spot.flag) \(open.place(german: Lang.isGerman))")
                        .foregroundStyle(Color.ink)
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
}

/// Where it's next, how long until then, and how much of this round is gone.
private struct NextRoundCard: View {
    let status: BeerStatus
    let now: Date

    var body: some View {
        let next = status.next
        let since = Double(status.openNow[0].minutesIn * 60)
        let left = max(next.countdown(from: now), 0)
        let first = next.spots[0]
        Link(destination: Links.map(first, first.brewery(onDay: first.epochDay(at: next.at)))) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .center) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(String(localized: "next_round").uppercased())
                            .font(.caption.weight(.semibold))
                            .tracking(1.5)
                            .foregroundStyle(Color.inkSoft)
                        Text(next.spots.map { "\($0.flag) \($0.place(german: Lang.isGerman, day: $0.epochDay(at: next.at)))" }
                            .joined(separator: ", "))
                            .font(.aleo(20, .bold, relativeTo: .title3))
                            .lineLimit(1)
                    }
                    Spacer(minLength: 12)
                    Text(formatCountdown(left)).font(.aleo(28, .black, relativeTo: .title))
                }
                ProgressView(value: since, total: max(since + left, 1))
                    .tint(Color.amber)
            }
            .foregroundStyle(Color.ink)
            .padding(20)
            .background(RoundedRectangle(cornerRadius: 28).fill(Color.foam))
        }
    }
}

/// Swipe through the breweries of the places where it's beer o'clock now.
private struct BreweriesSection: View {
    let status: BeerStatus

    private enum Page: Hashable {
        case brewery(BeerSpot, Brewery)
        case nearby(BeerSpot, Int)
    }

    private var pages: [Page] {
        let first = status.openNow[0]
        let breweries = status.openNow
            .filter { $0.hour == first.hour && $0.minute == first.minute }
            .flatMap { open in open.spot.breweries(fromDay: open.day).map { Page.brewery(open.spot, $0) } }
        return breweries.isEmpty ? [] : breweries + [.nearby(first.spot, first.day)]
    }

    @State private var selection = 0

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("breweries_here").font(.aleo(20, .black, relativeTo: .title3)).accessibilityAddTraits(.isHeader)
            Text("selection_note").font(.footnote).foregroundStyle(Color.inkSoft)
            if pages.isEmpty {
                card {
                    VStack(alignment: .leading) {
                        Text("no_local_beer").font(.aleo(20))
                        Text("bring_your_own").foregroundStyle(Color.inkSoft)
                    }
                }
            } else {
                TabView(selection: $selection) {
                    ForEach(Array(pages.enumerated()), id: \.offset) { index, page in
                        pageView(page).tag(index).padding(.horizontal, 2)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .frame(height: 150)
                Text(Lang.format("brewery_count", min(selection, pages.count - 1) + 1, pages.count))
                    .font(.caption)
                    .foregroundStyle(Color.inkSoft)
                    .frame(maxWidth: .infinity)
            }
        }
        .onChange(of: pages.count) { selection = 0 }
    }

    @ViewBuilder
    private func pageView(_ page: Page) -> some View {
        switch page {
        case let .brewery(spot, brewery):
            card {
                VStack(alignment: .leading, spacing: 6) {
                    Text("\(spot.flag) \(spot.place(of: brewery, german: Lang.isGerman).uppercased())")
                        .font(.caption.weight(.semibold))
                        .tracking(1.5)
                        .foregroundStyle(Color.inkSoft)
                    Text(brewery.name).font(.aleo(22, .bold, relativeTo: .title2)).lineLimit(2)
                    Spacer(minLength: 0)
                    HStack(spacing: 16) {
                        Link("on_untappd", destination: Links.untappd(brewery))
                        if let site = brewery.website, let url = URL(string: site) {
                            Link("brewery_website", destination: url)
                        }
                        Link("on_map", destination: Links.map(spot, brewery))
                    }
                    .font(.subheadline.weight(.bold))
                    .tint(Color.darkAmber)
                }
            }
        case let .nearby(spot, day):
            Link(destination: Links.breweriesNearby(spot, day: day)) {
                card {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("more_nearby").font(.aleo(22, .bold, relativeTo: .title2))
                        Text(Lang.format("more_nearby_text", spot.place(german: Lang.isGerman, day: day)))
                            .font(.subheadline)
                    }
                }
                .foregroundStyle(Color.ink)
            }
        }
    }

    private func card<Content: View>(@ViewBuilder _ content: () -> Content) -> some View {
        content()
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .padding(16)
            .background(RoundedRectangle(cornerRadius: 28).fill(Color.foam))
    }
}

/// The rounds of the next 12 hours, with your own clock times.
private struct Upcoming: View {
    let now: Date

    var body: some View {
        let rounds = BeerClock.rounds(from: now, until: now.addingTimeInterval(12 * 3600), edition: Lang.edition)
        VStack(alignment: .leading, spacing: 10) {
            Text("coming_up").font(.aleo(20, .black, relativeTo: .title3)).accessibilityAddTraits(.isHeader)
            ForEach(rounds.dropFirst(), id: \.at) { round in
                let first = round.spots[0]
                Link(destination: Links.map(first, first.brewery(onDay: first.epochDay(at: round.at)))) {
                    HStack(spacing: 14) {
                        Text(shortTime(round.at))
                            .font(.subheadline.monospacedDigit().weight(.medium))
                            .frame(minWidth: 64)
                            .padding(.vertical, 6)
                            .background(Capsule().fill(Color.foam))
                        Text(round.spots.map { "\($0.flag) \($0.place(german: Lang.isGerman, day: $0.epochDay(at: round.at)))" }
                            .joined(separator: ", "))
                            .lineLimit(1)
                        Spacer(minLength: 0)
                    }
                    .foregroundStyle(Color.ink)
                    .frame(minHeight: 44)
                }
            }
        }
    }
}
