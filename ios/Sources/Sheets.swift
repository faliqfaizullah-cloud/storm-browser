import SwiftUI
import WebKit
import PhotosUI

func hostOf(_ u: String) -> String {
    (URL(string: u)?.host ?? u).replacingOccurrences(of: "www.", with: "")
}

// MARK: - shared pieces

struct Panel<Content: View>: View {
    let p: Palette
    let content: Content
    init(_ p: Palette, @ViewBuilder _ c: () -> Content) {
        self.p = p
        self.content = c()
    }
    var body: some View {
        VStack(spacing: 0) { content }.boxed(p.ink, p.line)
    }
}

struct Row: View {
    let p: Palette
    let n: Int
    let name: String
    var state: String? = nil
    var last = false
    let action: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Button(action: action) {
                HStack(spacing: 0) {
                    LText(s: n2(n), size: 12).frame(width: 46)
                    DText(s: name, size: 24).lineLimit(1)
                    Spacer()
                    if let s = state {
                        LText(s: s, size: 13)
                            .padding(.horizontal, 12).padding(.vertical, 6)
                            .boxed(p.ink, p.line)
                            .padding(.trailing, 12)
                    } else {
                        Image(systemName: "arrow.up.right").font(.system(size: 16, weight: .black))
                            .foregroundColor(p.ink).padding(.trailing, 14)
                    }
                }
                .frame(height: 60)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if !last { p.ink.frame(height: p.line) }
        }
    }
}

struct SheetView: View {
    @ObservedObject var store = Store.shared
    @State var kind: ActiveSheet
    let b: Browser
    @Binding var sheet: ActiveSheet?

    init(start: ActiveSheet, b: Browser, sheet: Binding<ActiveSheet?>) {
        _kind = State(initialValue: start)
        self.b = b
        _sheet = sheet
    }

    var body: some View {
        let p = store.palette
        ZStack {
            p.paper.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 0) {
                HStack {
                    DText(s: "Storm", size: 26)
                    Spacer()
                    Button(action: { sheet = nil }) {
                        LText(s: "Done", size: 13, color: p.paper).padding(.horizontal, 16).padding(.vertical, 9).background(p.ink)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.horizontal, 16)
                .padding(.top, 10)
                content()
            }
        }
        .preferredColorScheme(store.theme == 1 ? ColorScheme.dark : ColorScheme.light)
    }

    @ViewBuilder
    private func content() -> some View {
        switch kind {
        case .menu: MenuView(b: b, sheet: $sheet, kind: $kind)
        case .tabs: TabsView(b: b, sheet: $sheet)
        case .bookmarks: BookmarksView(b: b, sheet: $sheet)
        case .history: HistoryView(b: b, sheet: $sheet)
        case .notes: NotesView(b: b, sheet: $sheet)
        case .settings: SettingsView()
        case .games: GamesWeb(onClose: { sheet = nil }, theme: store.theme, accent: gamesAccent())
        }
    }

    private func gamesAccent() -> String {
        let h = Palette.accents[store.accent % Palette.accents.count]
        return String(format: "#%06X", store.theme == 2 ? softHex(h) : h)
    }
}

// MARK: - menu

struct MenuView: View {
    @ObservedObject var store = Store.shared
    let b: Browser
    @Binding var sheet: ActiveSheet?
    @Binding var kind: ActiveSheet

    private func closeThen(_ f: @escaping () -> Void) {
        sheet = nil
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { f() }
    }

    var body: some View {
        let p = store.palette
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                DText(s: "Menu", size: 80)
                Panel(p) {
                    Group {
                        Row(p: p, n: 1, name: "New tab") {
                            closeThen { b.newTab(); NotificationCenter.default.post(name: .stormFocus, object: nil) }
                        }
                        Row(p: p, n: 2, name: "Private tab") {
                            closeThen { b.newTab(priv: true); NotificationCenter.default.post(name: .stormFocus, object: nil) }
                        }
                        Row(p: p, n: 3, name: "Tabs") { kind = .tabs }
                        Row(p: p, n: 4, name: "Bookmarks") { kind = .bookmarks }
                        Row(p: p, n: 5, name: "History") { kind = .history }
                        Row(p: p, n: 6, name: "Notes") { kind = .notes }
                    }
                    Group {
                        Row(p: p, n: 7, name: "Games", state: "Offline") { kind = .games }
                        Row(p: p, n: 8, name: "Add bookmark") {
                            let t = b.tab
                            if !t.showHome && !t.priv { store.addBookmark(title: t.title, url: t.url); UINotificationFeedbackGenerator().notificationOccurred(.success) }
                        }
                        Row(p: p, n: 9, name: "Ad block", state: store.ads ? "On" : "Off") { store.ads.toggle(); b.applyAds() }
                        Row(p: p, n: 10, name: "Settings") { kind = .settings }
                        Row(p: p, n: 11, name: "Clear data", last: true) {
                            WKWebsiteDataStore.default().removeData(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(), modifiedSince: Date(timeIntervalSince1970: 0)) { }
                            store.history = []
                            UINotificationFeedbackGenerator().notificationOccurred(.success)
                        }
                    }
                }
                if !b.tab.showHome, let u = URL(string: b.tab.url) {
                    ShareLink(item: u) {
                        HStack { LText(s: "Share this page", size: 14, color: p.paper); Spacer(); Image(systemName: "square.and.arrow.up").foregroundColor(p.paper) }
                            .padding(16).background(p.ink)
                    }
                }
            }
            .padding(16)
        }
    }
}

// MARK: - tabs

struct TabCard: View {
    @ObservedObject var t: TabState
    let p: Palette
    let num: Int
    let active: Bool
    let onOpen: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 0) {
                LText(s: n2(num), size: 12, color: active ? p.onAcc : p.paper).padding(.leading, 10)
                Spacer()
                Button(action: onClose) {
                    Text("✕").font(p.L(13)).foregroundColor(active ? p.onAcc : p.paper).frame(width: 36, height: 30)
                }
                .buttonStyle(.plain)
            }
            .frame(height: 30)
            .background(active ? p.acc : p.ink)
            VStack(alignment: .leading, spacing: 4) {
                DText(s: t.showHome ? "New tab" : hostOf(t.url), size: 22).lineLimit(2)
                Spacer()
                LText(s: t.showHome ? "" : t.title, size: 11, color: p.ink.opacity(0.7)).lineLimit(2)
            }
            .padding(10)
            .frame(maxWidth: .infinity, minHeight: 130, alignment: .leading)
            .contentShape(Rectangle())
            .onTapGesture(perform: onOpen)
        }
        .boxed(p.ink, p.line)
    }
}

struct TabsView: View {
    @ObservedObject var b: Browser
    @ObservedObject var store = Store.shared
    @Binding var sheet: ActiveSheet?
    @State private var showPriv = false

    var body: some View {
        let p = store.palette
        let list = b.tabs.indices.filter { b.tabs[$0].priv == showPriv }
        let normal = b.tabs.filter { !$0.priv }.count
        let priv = b.tabs.count - normal
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .bottom) {
                    DText(s: showPriv ? "Private" : "Tabs", size: 72)
                    Spacer()
                    DText(s: n2(list.count), size: 72, color: p.acc)
                }
                HStack(spacing: 0) {
                    seg(p, "Tabs \(normal)", !showPriv) { showPriv = false }
                    p.ink.frame(width: p.line)
                    seg(p, "Private \(priv)", showPriv) { showPriv = true }
                }
                .boxed(p.ink, p.line)
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 10), GridItem(.flexible(), spacing: 10)], spacing: 10) {
                    ForEach(list, id: \.self) { i in
                        TabCard(t: b.tabs[i], p: p, num: i + 1, active: i == b.cur,
                                onOpen: { b.cur = i; sheet = nil },
                                onClose: { b.close(i) })
                    }
                }
                Button(action: {
                    sheet = nil
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) {
                        b.newTab(priv: showPriv)
                        NotificationCenter.default.post(name: .stormFocus, object: nil)
                    }
                }) {
                    DText(s: "+ New tab", size: 24, color: showPriv ? Color.white : p.onAcc)
                        .frame(maxWidth: .infinity, minHeight: 56)
                        .background(showPriv ? Color(hex: 0x8E6BFF) : p.acc)
                }
                .buttonStyle(.plain)
            }
            .padding(16)
        }
    }

    private func seg(_ p: Palette, _ t: String, _ on: Bool, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            LText(s: t, size: 14, color: on ? p.paper : p.ink).frame(maxWidth: .infinity, minHeight: 44).background(on ? p.ink : Color.clear)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - bookmarks & history

struct BookmarksView: View {
    @ObservedObject var store = Store.shared
    let b: Browser
    @Binding var sheet: ActiveSheet?

    var body: some View {
        let p = store.palette
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                DText(s: "Bookmarks", size: 60)
                if !b.tab.showHome && !b.tab.priv {
                    Button(action: { store.addBookmark(title: b.tab.title, url: b.tab.url) }) {
                        LText(s: "+ Add this page", size: 16, color: p.paper).frame(maxWidth: .infinity, minHeight: 52).background(p.ink)
                    }
                    .buttonStyle(.plain)
                }
                if store.bookmarks.isEmpty {
                    DText(s: "Nothing\nsaved yet", size: 36, color: p.ink.opacity(0.35))
                } else {
                    Panel(p) {
                        ForEach(Array(store.bookmarks.enumerated()), id: \.element.id) { i, bm in
                            VStack(spacing: 0) {
                                HStack(spacing: 0) {
                                    LText(s: n2(i + 1), size: 12).frame(width: 44)
                                    VStack(alignment: .leading, spacing: 4) {
                                        DText(s: bm.title, size: 20).lineLimit(1)
                                        LText(s: hostOf(bm.url), size: 11, color: p.ink.opacity(0.6)).lineLimit(1)
                                    }
                                    Spacer()
                                    Button(action: { store.bookmarks.removeAll { $0.id == bm.id } }) {
                                        Text("✕").font(p.L(14)).foregroundColor(p.paper).frame(width: 52, height: 64).background(p.ink)
                                    }
                                    .buttonStyle(.plain)
                                }
                                .frame(height: 64)
                                .contentShape(Rectangle())
                                .onTapGesture { b.tab.load(bm.url); sheet = nil }
                                if i < store.bookmarks.count - 1 { p.ink.frame(height: p.line) }
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }
}

struct HistoryView: View {
    @ObservedObject var store = Store.shared
    let b: Browser
    @Binding var sheet: ActiveSheet?
    @State private var q = ""

    var body: some View {
        let p = store.palette
        let items = store.history.filter { q.isEmpty || $0.title.lowercased().contains(q.lowercased()) || $0.url.lowercased().contains(q.lowercased()) }
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                DText(s: "History", size: 60)
                HStack(spacing: 0) {
                    TextField("Search history", text: $q)
                        .font(p.L(17)).foregroundColor(p.ink).padding(12)
                        .textInputAutocapitalization(.never).autocorrectionDisabled(true)
                    Button(action: { store.history = [] }) {
                        LText(s: "Clear", size: 14, color: p.paper).frame(width: 84, height: 46).background(p.ink)
                    }
                    .buttonStyle(.plain)
                }
                .boxed(p.ink, p.line)
                if items.isEmpty {
                    DText(s: "Nothing\nhere yet", size: 36, color: p.ink.opacity(0.35))
                } else {
                    Panel(p) {
                        ForEach(Array(items.prefix(150).enumerated()), id: \.element.id) { i, h in
                            VStack(spacing: 0) {
                                HStack(spacing: 0) {
                                    LText(s: n2(i + 1), size: 12).frame(width: 44)
                                    VStack(alignment: .leading, spacing: 4) {
                                        DText(s: h.title, size: 19).lineLimit(1)
                                        LText(s: hostOf(h.url), size: 11, color: p.ink.opacity(0.6)).lineLimit(1)
                                    }
                                    Spacer()
                                }
                                .padding(.vertical, 10)
                                .contentShape(Rectangle())
                                .onTapGesture { b.tab.load(h.url); sheet = nil }
                                if i < min(items.count, 150) - 1 { p.ink.frame(height: p.line) }
                            }
                        }
                    }
                }
            }
            .padding(16)
        }
    }
}

// MARK: - settings

struct SettingsView: View {
    @ObservedObject var store = Store.shared

    var body: some View {
        let p = store.palette
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                DText(s: "Settings", size: 64)
                LText(s: "Theme", size: 12).padding(.top, 8)
                HStack(spacing: 0) {
                    themeBtn(p, "Light", 0)
                    p.ink.frame(width: p.line)
                    themeBtn(p, "OLED", 1)
                    p.ink.frame(width: p.line)
                    themeBtn(p, "Minimal", 2)
                }
                .boxed(p.ink, p.line)
                LText(s: "Accent colour", size: 12).padding(.top, 8)
                HStack(spacing: 6) {
                    ForEach(0..<Palette.accents.count, id: \.self) { i in
                        let h = Palette.accents[i]
                        Button(action: { store.accent = i }) {
                            Text(store.accent == i ? "✓" : "").font(.system(size: 20, weight: .heavy)).foregroundColor(Color(hex: 0x111111))
                                .frame(maxWidth: .infinity, minHeight: 46)
                                .background(Color(hex: p.mini ? softHex(h) : h))
                                .boxed(p.ink, store.accent == i ? 3 : p.line)
                        }
                        .buttonStyle(.plain)
                    }
                }
                LText(s: "Browsing", size: 12).padding(.top, 8)
                Panel(p) {
                    Row(p: p, n: 1, name: "Ad block", state: store.ads ? "On" : "Off") { store.ads.toggle() }
                    Row(p: p, n: 2, name: "Search engine", state: Store.engines[store.engine % Store.engines.count].0, last: true) {
                        store.engine = (store.engine + 1) % Store.engines.count
                    }
                }
                LText(s: "Ad block applies to pages you open next. Reload a page to apply it.", size: 11, color: p.ink.opacity(0.6))
            }
            .padding(16)
        }
    }

    private func themeBtn(_ p: Palette, _ t: String, _ v: Int) -> some View {
        Button(action: { store.theme = v }) {
            LText(s: t, size: 14, color: store.theme == v ? p.paper : p.ink)
                .frame(maxWidth: .infinity, minHeight: 46)
                .background(store.theme == v ? p.ink : Color.clear)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - notes (pictures + links)

func noteColors(_ p: Palette) -> [(Color, Color)] {
    let m = p.mini
    func c(_ h: UInt32) -> Color { Color(hex: m ? softHex(h) : h) }
    let dark = Color(hex: 0x111111)
    return [(c(0xC8F23C), dark), (c(0xFFC928), dark), (c(0xFF5500), dark),
            (c(0x4A4DFF), m ? Color(hex: 0x1C1C1E) : Color.white), (c(0xFF4FA3), dark), (Color.white, dark)]
}

func linksIn(_ text: String) -> [URL] {
    guard let d = try? NSDataDetector(types: NSTextCheckingResult.CheckingType.link.rawValue) else { return [] }
    let r = NSRange(text.startIndex..., in: text)
    var out: [URL] = []
    for m in d.matches(in: text, options: [], range: r) {
        if let u = m.url, (u.scheme ?? "").hasPrefix("http"), !out.contains(u) { out.append(u) }
    }
    return Array(out.prefix(8))
}

struct NotesView: View {
    @ObservedObject var store = Store.shared
    let b: Browser
    @Binding var sheet: ActiveSheet?
    @State private var editing: UUID?

    var body: some View {
        if let id = editing {
            NoteEditor(id: id, onBack: { editing = nil }, openURL: { u in
                sheet = nil
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.45) { b.newTab(url: u.absoluteString) }
            })
        } else {
            list()
        }
    }

    private func list() -> some View {
        let p = store.palette
        let colors = noteColors(p)
        let sorted = store.notes.sorted { ($0.pin ? 1 : 0, $0.ts) > ($1.pin ? 1 : 0, $1.ts) }
        return ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .bottom) {
                    DText(s: "Notes", size: 72)
                    Spacer()
                    DText(s: n2(store.notes.count), size: 72, color: p.acc)
                }
                HStack(spacing: 0) {
                    Button(action: {
                        let n = Note()
                        store.notes.insert(n, at: 0)
                        editing = n.id
                    }) {
                        LText(s: "+ New note", size: 15, color: p.paper).frame(maxWidth: .infinity, minHeight: 52).background(p.ink)
                    }
                    .buttonStyle(.plain)
                    p.ink.frame(width: p.line)
                    Button(action: {
                        let t = b.tab
                        if !t.showHome {
                            let n = Note(title: t.title, body: t.url)
                            store.notes.insert(n, at: 0)
                        }
                    }) {
                        LText(s: "+ This page", size: 15).frame(maxWidth: .infinity, minHeight: 52)
                    }
                    .buttonStyle(.plain)
                }
                .boxed(p.ink, p.line)
                if sorted.isEmpty {
                    DText(s: "No notes\nyet", size: 40, color: p.ink.opacity(0.35))
                }
                ForEach(sorted) { n in
                    let c = colors[n.color % colors.count]
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            LText(s: Date(timeIntervalSince1970: n.ts).formatted(date: .abbreviated, time: .shortened), size: 11, color: c.1)
                            Spacer()
                            if !n.images.isEmpty { LText(s: "\(n.images.count) pic  ", size: 11, color: c.1) }
                            if n.pin { LText(s: "★ Pinned", size: 11, color: c.1) }
                        }
                        DText(s: n.title.isEmpty ? "Untitled" : n.title, size: 28, color: c.1).lineLimit(1)
                        if !n.body.isEmpty {
                            Text(n.body).font(p.L(14)).foregroundColor(c.1.opacity(0.85)).lineLimit(2)
                        }
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(c.0)
                    .boxed(n.color % colors.count == 5 ? p.ink : Color.clear, p.line)
                    .contentShape(Rectangle())
                    .onTapGesture { editing = n.id }
                }
            }
            .padding(16)
        }
    }
}

struct NoteEditor: View {
    @ObservedObject var store = Store.shared
    let id: UUID
    let onBack: () -> Void
    let openURL: (URL) -> Void
    @State private var picks: [PhotosPickerItem] = []
    @State private var viewing: String?

    private var idx: Int? { store.notes.firstIndex(where: { $0.id == id }) }

    var body: some View {
        if let i = idx { editor(i) } else { Color.clear.onAppear { onBack() } }
    }

    private func imageURL(_ name: String) -> URL { Store.docs().appendingPathComponent(name) }

    private func finish(_ i: Int) {
        let n = store.notes[i]
        if n.title.trimmingCharacters(in: .whitespaces).isEmpty && n.body.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && n.images.isEmpty {
            store.notes.remove(at: i)
        }
        onBack()
    }

    private func editor(_ i: Int) -> some View {
        let p = store.palette
        let colors = noteColors(p)
        let c = colors[store.notes[i].color % colors.count]
        let links = linksIn(store.notes[i].body)
        return ZStack {
            c.0.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    LText(s: Date(timeIntervalSince1970: store.notes[i].ts).formatted(date: .abbreviated, time: .shortened), size: 11, color: c.1)
                    Spacer()
                    Button(action: { finish(i) }) {
                        LText(s: "Done", size: 13, color: c.0).padding(.horizontal, 16).padding(.vertical, 9).background(c.1)
                    }
                    .buttonStyle(.plain)
                }
                TextField("Title", text: $store.notes[i].title)
                    .font(p.D(40)).foregroundColor(c.1)
                    .onChange(of: store.notes[i].title) { _ in store.notes[i].ts = Date().timeIntervalSince1970 }
                c.1.frame(height: p.line)
                TextEditor(text: $store.notes[i].body)
                    .scrollContentBackground(.hidden)
                    .font(p.L(18)).foregroundColor(c.1)
                    .frame(minHeight: 120)
                if !links.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(links, id: \.self) { u in
                                Button(action: { openURL(u) }) {
                                    LText(s: "→ " + hostOf(u.absoluteString) + u.path, size: 12, color: c.1)
                                        .lineLimit(1).padding(.horizontal, 12).padding(.vertical, 9)
                                        .boxed(c.1, p.line)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                if !store.notes[i].images.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(store.notes[i].images, id: \.self) { name in
                                ZStack(alignment: .topTrailing) {
                                    Image(uiImage: UIImage(contentsOfFile: imageURL(name).path) ?? UIImage())
                                        .resizable().scaledToFill().frame(width: 92, height: 92).clipped()
                                        .onTapGesture { viewing = name }
                                    Button(action: {
                                        try? FileManager.default.removeItem(at: imageURL(name))
                                        store.notes[i].images.removeAll { $0 == name }
                                    }) {
                                        Text("✕").font(.system(size: 11, weight: .heavy)).foregroundColor(Color.white)
                                            .frame(width: 24, height: 24).background(Color.black.opacity(0.75))
                                    }
                                    .buttonStyle(.plain)
                                }
                                .boxed(c.1, p.line)
                            }
                        }
                    }
                }
                HStack(spacing: 0) {
                    bar(c, store.notes[i].pin ? "Unpin" : "Pin") { store.notes[i].pin.toggle() }
                    c.1.frame(width: p.line)
                    bar(c, "Colour") { store.notes[i].color = (store.notes[i].color + 1) % colors.count }
                    c.1.frame(width: p.line)
                    PhotosPicker(selection: $picks, maxSelectionCount: 8, matching: .images) {
                        LText(s: "Photo", size: 13, color: c.1).frame(maxWidth: .infinity, minHeight: 50)
                    }
                    c.1.frame(width: p.line)
                    bar(c, "Delete") {
                        for name in store.notes[i].images { try? FileManager.default.removeItem(at: imageURL(name)) }
                        store.notes.remove(at: i)
                        onBack()
                    }
                }
                .boxed(c.1, p.line)
            }
            .padding(16)
        }
        .onChange(of: picks) { items in
            Task {
                for it in items {
                    if let data = try? await it.loadTransferable(type: Data.self), let img = UIImage(data: data),
                       let jpg = img.jpegData(compressionQuality: 0.85) {
                        let name = "note_\(id.uuidString)_\(Int(Date().timeIntervalSince1970 * 1000))_\(Int.random(in: 0..<9999)).jpg"
                        try? jpg.write(to: imageURL(name))
                        if let j = store.notes.firstIndex(where: { $0.id == id }) { store.notes[j].images.append(name) }
                    }
                }
                picks = []
            }
        }
        .fullScreenCover(item: Binding(get: { viewing.map { ViewedImage(name: $0) } }, set: { viewing = $0?.name })) { v in
            ZStack {
                Color.black.ignoresSafeArea()
                Image(uiImage: UIImage(contentsOfFile: imageURL(v.name).path) ?? UIImage()).resizable().scaledToFit()
            }
            .onTapGesture { viewing = nil }
        }
    }

    private func bar(_ c: (Color, Color), _ t: String, _ action: @escaping () -> Void) -> some View {
        Button(action: action) {
            LText(s: t, size: 13, color: c.1).frame(maxWidth: .infinity, minHeight: 50)
        }
        .buttonStyle(.plain)
    }
}

struct ViewedImage: Identifiable {
    let name: String
    var id: String { name }
}

// MARK: - offline games

struct GamesWeb: UIViewRepresentable {
    let onClose: () -> Void
    let theme: Int
    let accent: String

    func makeCoordinator() -> Coordinator { Coordinator(onClose) }

    func makeUIView(context: Context) -> WKWebView {
        let cfg = WKWebViewConfiguration()
        cfg.userContentController.add(context.coordinator, name: "StormGames")
        let w = WKWebView(frame: .zero, configuration: cfg)
        w.isOpaque = false
        w.backgroundColor = UIColor.clear
        w.scrollView.bounces = false
        w.scrollView.isScrollEnabled = false
        if let u = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "Games"),
           var comps = URLComponents(url: u, resolvingAgainstBaseURL: false) {
            comps.queryItems = [URLQueryItem(name: "oled", value: theme == 1 ? "1" : "0"),
                                URLQueryItem(name: "mini", value: theme == 2 ? "1" : "0"),
                                URLQueryItem(name: "acc", value: accent)]
            if let full = comps.url { w.loadFileURL(full, allowingReadAccessTo: u.deletingLastPathComponent()) }
        }
        return w
    }

    func updateUIView(_ uiView: WKWebView, context: Context) {}

    final class Coordinator: NSObject, WKScriptMessageHandler {
        let onClose: () -> Void
        init(_ c: @escaping () -> Void) { onClose = c }
        func userContentController(_ c: WKUserContentController, didReceive m: WKScriptMessage) {
            guard let d = m.body as? [String: Any], let a = d["action"] as? String else { return }
            DispatchQueue.main.async {
                if a == "close" { self.onClose() }
                else if a == "tick" { UIImpactFeedbackGenerator(style: .light).impactOccurred() }
            }
        }
    }
}
