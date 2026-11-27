import SwiftUI
import WebKit

enum ActiveSheet: String, Identifiable {
    case menu, tabs, bookmarks, history, notes, settings, games
    var id: String { rawValue }
}

extension Notification.Name {
    static let stormFocus = Notification.Name("stormFocusAddress")
}

struct BrowserView: View {
    @StateObject private var b = Browser()
    @ObservedObject var store = Store.shared
    @State private var sheet: ActiveSheet?

    var body: some View {
        let p = store.palette
        ZStack {
            p.paper.ignoresSafeArea()
            TabHost(tab: b.tab, b: b, sheet: $sheet)
        }
        .preferredColorScheme(store.theme == 1 ? ColorScheme.dark : ColorScheme.light)
        .fullScreenCover(item: $sheet) { s in
            SheetView(start: s, b: b, sheet: $sheet)
        }
    }
}

struct ProgressLine: View {
    let value: Double
    let color: Color
    var body: some View {
        GeometryReader { g in
            Rectangle().fill(color).frame(width: g.size.width * CGFloat(value), height: 3)
        }
        .frame(height: 3)
    }
}

struct TabHost: View {
    @ObservedObject var tab: TabState
    @ObservedObject var b: Browser
    @ObservedObject var store = Store.shared
    @Binding var sheet: ActiveSheet?

    var body: some View {
        let p = store.palette
        VStack(spacing: 0) {
            ZStack(alignment: .top) {
                if tab.showHome {
                    HomeView(tab: tab, b: b, sheet: $sheet)
                } else {
                    WebContainer(tab: tab).id(tab.id)
                    if tab.loading {
                        ProgressLine(value: tab.progress, color: tab.priv ? Color(hex: 0x8E6BFF) : p.acc)
                    }
                }
            }
            Dock(tab: tab, b: b, sheet: $sheet)
        }
    }
}

struct NavCell: View {
    let p: Palette
    let icon: String
    let label: String
    let filled: Bool
    let dim: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon).font(.system(size: 20, weight: .bold))
                LText(s: label, size: 10, color: filled ? p.paper : p.ink)
            }
            .foregroundColor(filled ? p.paper : p.ink)
            .frame(maxWidth: .infinity, minHeight: 60)
            .background(filled ? p.ink : Color.clear)
            .opacity(dim ? 0.3 : 1)
        }
        .buttonStyle(.plain)
    }
}

struct Dock: View {
    @ObservedObject var tab: TabState
    @ObservedObject var b: Browser
    @ObservedObject var store = Store.shared
    @Binding var sheet: ActiveSheet?
    @State private var editing = false
    @State private var text = ""
    @FocusState private var focus: Bool

    private var host: String {
        (URL(string: tab.url)?.host ?? "").replacingOccurrences(of: "www.", with: "")
    }

    var body: some View {
        let p = store.palette
        VStack(spacing: 0) {
            p.ink.frame(height: p.line)
            HStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 3) {
                    LText(s: tab.priv ? "Private · address" : (tab.url.hasPrefix("https") ? "Secure · address" : "Address"),
                          size: 10, color: p.ink.opacity(0.6))
                    if editing {
                        TextField("Type or paste", text: $text)
                            .font(p.L(18))
                            .foregroundColor(p.ink)
                            .focused($focus)
                            .keyboardType(.webSearch)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled(true)
                            .submitLabel(.go)
                            .onSubmit { go() }
                    } else {
                        Text(tab.showHome || host.isEmpty ? "Search or enter URL" : host)
                            .font(p.L(18))
                            .foregroundColor(p.ink.opacity(tab.showHome ? 0.5 : 1))
                            .lineLimit(1)
                    }
                }
                .padding(.horizontal, 14)
                .frame(maxWidth: .infinity, minHeight: 58, alignment: .leading)
                .contentShape(Rectangle())
                .onTapGesture { startEdit() }
                Button(action: reloadOrGo) {
                    Text(editing ? "GO" : (tab.loading ? "✕" : "↻"))
                        .font(p.L(editing ? 16 : 22))
                        .foregroundColor(p.paper)
                        .frame(width: 72, height: 58)
                        .background(tab.priv ? Color(hex: 0x8E6BFF) : p.ink)
                }
                .buttonStyle(.plain)
            }
            p.ink.frame(height: p.line)
            if !editing { navRow(p) }
        }
        .background(p.paper)
        .onChange(of: focus) { f in if !f { editing = false } }
        .onReceive(NotificationCenter.default.publisher(for: .stormFocus)) { _ in startEdit() }
    }

    private func startEdit() {
        text = tab.showHome ? "" : tab.url
        editing = true
        DispatchQueue.main.async { focus = true }
    }

    private func go() {
        let t = text
        editing = false
        focus = false
        tab.load(t)
    }

    private func reloadOrGo() {
        if editing { go() }
        else if tab.loading { tab.web?.stopLoading() }
        else { tab.web?.reload() }
    }

    @ViewBuilder
    private func navRow(_ p: Palette) -> some View {
        HStack(spacing: 0) {
            NavCell(p: p, icon: "chevron.left", label: "Back", filled: false, dim: tab.showHome) { tab.goBack() }
            p.ink.frame(width: p.line)
            NavCell(p: p, icon: "chevron.right", label: "Fwd", filled: false, dim: !tab.canFwd || tab.showHome) { tab.web?.goForward() }
            p.ink.frame(width: p.line)
            NavCell(p: p, icon: "house", label: "Home", filled: tab.showHome, dim: false) { tab.showHome = true }
            p.ink.frame(width: p.line)
            NavCell(p: p, icon: "square.on.square", label: "Tabs \(b.tabs.count)", filled: false, dim: false) { sheet = .tabs }
            p.ink.frame(width: p.line)
            NavCell(p: p, icon: "line.3.horizontal", label: "Menu", filled: false, dim: false) { sheet = .menu }
        }
        .frame(height: 62)
    }
}

// MARK: - Home

func cardFill(_ i: Int, _ p: Palette) -> (Color, Color) {
    let m = p.mini
    func c(_ h: UInt32) -> Color { Color(hex: m ? softHex(h) : h) }
    let dark = Color(hex: 0x111111)
    switch i {
    case 0: return (c(0xC8F23C), dark)
    case 1: return (c(0x4A4DFF), m ? Color(hex: 0x1C1C1E) : Color.white)
    case 2: return (p.ink, p.paper)
    case 3: return (m ? c(0xFFC928) : Color.white, dark)
    case 4: return (c(0xFF5500), dark)
    default: return (p.paper, p.ink)
    }
}

struct HomeView: View {
    @ObservedObject var tab: TabState
    @ObservedObject var b: Browser
    @ObservedObject var store = Store.shared
    @Binding var sheet: ActiveSheet?
    let quick: [(String, String)] = [
        ("Google", "https://www.google.com"), ("YouTube", "https://m.youtube.com"), ("Wikipedia", "https://en.m.wikipedia.org"),
        ("GitHub", "https://github.com"), ("Brave", "https://search.brave.com"), ("Reddit", "https://www.reddit.com")]

    var body: some View {
        let p = store.palette
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    DText(s: "Storm", size: 26)
                    Spacer()
                    Button(action: { sheet = .menu }) {
                        LText(s: "Menu", size: 13, color: p.paper).padding(.horizontal, 16).padding(.vertical, 9).background(p.ink)
                    }
                    .buttonStyle(.plain)
                }
                DText(s: tab.priv ? "Private\nSession" : "Quick\nAccess", size: 66)
                banner(p)
                hero(p)
                HStack {
                    LText(s: "Shortcuts", size: 12)
                    Spacer()
                    LText(s: n2(quick.count), size: 12)
                }
                .padding(.top, 8)
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 8), GridItem(.flexible(), spacing: 8)], spacing: 8) {
                    ForEach(0..<quick.count, id: \.self) { i in card(i, p) }
                }
                infoGrid(p)
                cell(p, "Mini games · works offline", "Play now", "Play") { sheet = .games }
                LText(s: "No trackers · No clutter", size: 10, color: p.ink.opacity(0.6))
                    .frame(maxWidth: .infinity)
                    .padding(.top, 8)
            }
            .padding(.horizontal, 16)
            .padding(.top, 10)
            .padding(.bottom, 24)
        }
    }

    private func banner(_ p: Palette) -> some View {
        let on = store.ads
        return HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                LText(s: "Ad block", size: 11, color: on ? p.paper : p.ink)
                DText(s: on ? "Protected" : "Exposed", size: 34, color: on ? p.paper : p.ink)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            DText(s: on ? "On" : "Off", size: 38, color: on ? p.ink : p.paper)
                .frame(width: 96, height: 58)
                .background(on ? p.paper : p.ink)
                .padding(.trailing, 12)
        }
        .background(on ? p.ink : Color.clear)
        .boxed(p.ink, p.line)
        .contentShape(Rectangle())
        .onTapGesture {
            store.ads.toggle()
            b.applyAds()
        }
    }

    private func hero(_ p: Palette) -> some View {
        ZStack(alignment: .topLeading) {
            p.acc
            Image(systemName: "arrow.up.right")
                .font(.system(size: 120, weight: .black))
                .foregroundColor(Color.white)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                .offset(x: 24, y: 28)
            VStack(alignment: .leading) {
                HStack {
                    Spacer()
                    DText(s: "01", size: 30, color: p.onAcc)
                }
                Spacer()
                DText(s: "Search", size: 76, color: p.onAcc)
                LText(s: "Tap to type an address", size: 12, color: p.onAcc)
            }
            .padding(16)
        }
        .frame(height: 220)
        .clipped()
        .contentShape(Rectangle())
        .onTapGesture { NotificationCenter.default.post(name: .stormFocus, object: nil) }
    }

    private func card(_ i: Int, _ p: Palette) -> some View {
        let colors = cardFill(i, p)
        let fill = colors.0
        let fg = colors.1
        let stroke = (i == 5 || (i == 3 && !p.mini)) ? p.ink : Color.clear
        return ZStack(alignment: .topLeading) {
            fill
            VStack(alignment: .leading) {
                HStack {
                    LText(s: n2(i + 2), size: 12, color: fg)
                    Spacer()
                    Image(systemName: "arrow.up.right").font(.system(size: 14, weight: .black)).foregroundColor(fg)
                }
                Spacer()
                DText(s: quick[i].0, size: 28, color: fg)
            }
            .padding(12)
        }
        .frame(height: 118)
        .boxed(stroke, p.line)
        .contentShape(Rectangle())
        .onTapGesture { tab.load(quick[i].1) }
    }

    private func infoGrid(_ p: Palette) -> some View {
        let items: [(String, Int, () -> Void)] = [
            ("Tabs", b.tabs.count, { sheet = .tabs }),
            ("Saved", store.bookmarks.count, { sheet = .bookmarks }),
            ("History", store.history.count, { sheet = .history }),
            ("Notes", store.notes.count, { sheet = .notes })]
        return HStack(spacing: 0) {
            ForEach(0..<items.count, id: \.self) { i in
                VStack(alignment: .leading, spacing: 4) {
                    LText(s: items[i].0, size: 10)
                    DText(s: n2(items[i].1), size: 28)
                }
                .padding(12)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
                .onTapGesture { items[i].2() }
                if i < items.count - 1 { p.ink.frame(width: p.line) }
            }
        }
        .boxed(p.ink, p.line)
    }

    private func cell(_ p: Palette, _ label: String, _ title: String, _ glyph: String, _ action: @escaping () -> Void) -> some View {
        HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                LText(s: label, size: 10)
                DText(s: title, size: 26)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            p.ink.frame(width: p.line)
            DText(s: glyph, size: 20, color: p.paper)
                .frame(width: 72)
                .frame(maxHeight: .infinity)
                .background(p.ink)
        }
        .frame(height: 64)
        .boxed(p.ink, p.line)
        .contentShape(Rectangle())
        .onTapGesture(perform: action)
    }
}
