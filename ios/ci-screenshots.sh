#!/bin/bash
# Boots an iPhone and an Apple Watch simulator, installs the freshly built apps
# and takes screenshots in German and English. Used by .github/workflows/ios.yml.
set -euo pipefail
mkdir -p screenshots
KIRN="49.79,7.46"   # a place for the distance

device() {  # $1 = runtime match (iOS|watchOS), $2 = name prefix
  xcrun simctl list devices available -j | python3 -c "
import json, sys
devices = json.load(sys.stdin)['devices']
print(next(d['udid'] for runtime, ds in sorted(devices.items(), reverse=True) if '$1' in runtime
           for d in ds if d['name'].startswith('$2')))"
}

shoot() {  # $1 = udid, $2 = app path, $3 = bundle id, $4 = name
  xcrun simctl boot "$1" || true
  xcrun simctl bootstatus "$1" -b
  xcrun simctl status_bar "$1" override --time 16:41 || true
  xcrun simctl install "$1" "$2"
  xcrun simctl privacy "$1" grant location "$3" || true
  xcrun simctl location "$1" set "$KIRN" || true
  for lang in de en; do
    xcrun simctl terminate "$1" "$3" || true
    xcrun simctl launch "$1" "$3" -AppleLanguages "($lang)" -AppleLocale "${lang}_DE"
    sleep 12
    xcrun simctl io "$1" screenshot "screenshots/$4-$lang.png"
  done
}

shoot "$(device iOS iPhone)" build/Build/Products/Debug-iphonesimulator/KeinBier.app de.keinbiervorvier.ios iphone
shoot "$(device watchOS 'Apple Watch')" build/Build/Products/Debug-watchsimulator/KeinBierWatch.app de.keinbiervorvier.watch watch
ls -la screenshots
