import BeerCore
import SwiftUI

// The beer glass colours, the same as on Android (common/.../Palette.kt).
extension Color {
    static let cream = Color(red: 0xFD / 255, green: 0xF6 / 255, blue: 0xE3 / 255)
    static let foam = Color(red: 0xF3 / 255, green: 0xE7 / 255, blue: 0xC9 / 255)
    static let foamWhite = Color(red: 0xFF / 255, green: 0xFC / 255, blue: 0xF2 / 255)
    static let amber = Color(red: 0xF6 / 255, green: 0xB4 / 255, blue: 0x00 / 255)
    static let darkAmber = Color(red: 0xD9 / 255, green: 0x83 / 255, blue: 0x00 / 255)
    static let beerRed = Color(red: 0xC6 / 255, green: 0x28 / 255, blue: 0x28 / 255)
    /// Text: warm grey, 90 % neutral grey with 10 % golden yellow.
    static let ink = Color(red: 0x59 / 255, green: 0x55 / 255, blue: 0x40 / 255)
    /// Secondary text, 5.3:1 on cream.
    static let inkSoft = Color(red: 0x6B / 255, green: 0x67 / 255, blue: 0x56 / 255)
    /// Small text on the beer, 5.6:1 on amber.
    static let inkDeep = Color(red: 0x45 / 255, green: 0x41 / 255, blue: 0x2B / 255)
}

extension Font {
    /// Aleo, the soft slab serif (OFL), bundled in every target.
    static func aleo(_ size: CGFloat, _ weight: Font.Weight = .bold, relativeTo style: Font.TextStyle = .body) -> Font {
        .custom("Aleo", size: size, relativeTo: style).weight(weight)
    }
}

/// Language and saying for this device.
enum Lang {
    static var isGerman: Bool { Locale.current.language.languageCode?.identifier == "de" }
    static var edition: Edition { Edition.forLanguage(Locale.current.language.languageCode?.identifier) }

    static func nowIn(_ edition: Edition) -> String {
        edition == .keinBierVorVier ? String(localized: "now_in_vier") : String(localized: "now_in_five")
    }

    /// A localized format string filled in, e.g. "seit 25 min".
    static func format(_ key: String, _ args: CVarArg...) -> String {
        String(format: NSLocalizedString(key, comment: ""), arguments: args)
    }
}

/// Times in the user's own clock, e.g. "18:00".
func shortTime(_ date: Date) -> String {
    date.formatted(date: .omitted, time: .shortened)
}
