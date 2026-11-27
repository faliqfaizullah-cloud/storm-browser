import SwiftUI

@main
struct StormApp: App {
    init() { AdBlock.prepare() }
    var body: some Scene {
        WindowGroup { BrowserView() }
    }
}
