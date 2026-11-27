import SwiftUI
import WebKit

struct Bookmark: Codable, Identifiable, Hashable {
    var id = UUID()
    var title: String
    var url: String
}

struct HistoryItem: Codable, Identifiable, Hashable {
    var id = UUID()
    var title: String
    var url: String
    var ts: Double
}

struct Note: Codable, Identifiable, Hashable {
    var id = UUID()
    var title = ""
    var body = ""
    var color = 0
    var pin = false
    var ts: Double = Date().timeIntervalSince1970
    var images: [String] = []
}

final class Store: ObservableObject {
    static let shared = Store()
    static let engines: [(String, String)] = [
        ("DuckDuckGo", "https://duckduckgo.com/?q="), ("Google", "https://www.google.com/search?q="),
        ("Brave", "https://search.brave.com/search?q="), ("Bing", "https://www.bing.com/search?q=")]

    @Published var bookmarks: [Bookmark] { didSet { Store.save(bookmarks, "bm") } }
    @Published var history: [HistoryItem] { didSet { Store.save(history, "hist") } }
    @Published var notes: [Note] { didSet { Store.save(notes, "notes") } }
    @Published var theme: Int { didSet { UserDefaults.standard.set(theme, forKey: "theme") } }
    @Published var accent: Int { didSet { UserDefaults.standard.set(accent, forKey: "acc") } }
    @Published var ads: Bool { didSet { UserDefaults.standard.set(ads, forKey: "ads") } }
    @Published var engine: Int { didSet { UserDefaults.standard.set(engine, forKey: "se") } }

    private init() {
        let d = UserDefaults.standard
        bookmarks = Store.load("bm") ?? []
        history = Store.load("hist") ?? []
        notes = Store.load("notes") ?? []
        theme = d.integer(forKey: "theme")
        accent = d.integer(forKey: "acc")
        ads = (d.object(forKey: "ads") as? Bool) ?? true
        engine = d.integer(forKey: "se")
    }

    var palette: Palette { Palette(theme: theme, accentIndex: accent) }

    static func load<T: Decodable>(_ key: String) -> T? {
        guard let d = UserDefaults.standard.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(T.self, from: d)
    }

    static func save<T: Encodable>(_ value: T, _ key: String) {
        if let d = try? JSONEncoder().encode(value) { UserDefaults.standard.set(d, forKey: key) }
    }

    static func docs() -> URL { FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0] }

    func addHistory(title: String, url: String) {
        history.removeAll { $0.url == url }
        history.insert(HistoryItem(title: title, url: url, ts: Date().timeIntervalSince1970), at: 0)
        if history.count > 300 { history.removeLast(history.count - 300) }
    }

    func addBookmark(title: String, url: String) {
        guard !url.isEmpty else { return }
        bookmarks.removeAll { $0.url == url }
        bookmarks.insert(Bookmark(title: title, url: url), at: 0)
    }
}
