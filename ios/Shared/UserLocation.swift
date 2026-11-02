import BeerCore
import CoreLocation
import SwiftUI

/// The device's rough position for distances. Asks once; city level is plenty.
@MainActor
final class UserLocation: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published private(set) var point: GeoPoint?
    private let manager = CLLocationManager()

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyReduced
    }

    func start() {
        switch manager.authorizationStatus {
        case .notDetermined: manager.requestWhenInUseAuthorization()
        case .denied, .restricted: break
        default: manager.requestLocation()
        }
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        if status == .authorizedWhenInUse || status == .authorizedAlways {
            manager.requestLocation()
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let last = locations.last else { return }
        let point = GeoPoint(lat: last.coordinate.latitude, lon: last.coordinate.longitude)
        Task { @MainActor in self.point = point }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // No position, no distance - everything else works without it.
    }
}

/// "seit 25 min · 2.300 km" style line for the place of `open`.
func sinceAndDistance(_ open: OpenSpot, from user: GeoPoint?) -> [String] {
    let german = Lang.isGerman
    var parts = [Lang.format("since_minutes", open.minutesIn)]
    if let user {
        let km = user.distanceKm(to: open.spot.location(onDay: open.day))
        parts.append(formatDistance(km, german: german) ?? String(localized: "you_are_there"))
    }
    return parts
}
