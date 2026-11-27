# Storm for iOS

SwiftUI + WebKit build of Storm Browser. Needs macOS + Xcode 15 to build (GitHub's macOS runner does it for you).

The workflow `.github/workflows/build.yml` builds an **unsigned** `Storm-iOS.ipa`. Unsigned IPAs must be signed on install —
use AltStore, Sideloadly, SideStore (free Apple ID, 7-day certificate) or TrollStore. To publish on the App Store or TestFlight
you need a paid Apple Developer account and a signed build.

Local build:

    brew install xcodegen
    cd ios && xcodegen generate && open Storm.xcodeproj
