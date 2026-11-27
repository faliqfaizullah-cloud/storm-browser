import SwiftUI
import WebKit

/// One browser tab: owns its WKWebView and publishes the bits the UI needs.
final class TabState: NSObject, ObservableObject, Identifiable, WKNavigationDelegate, WKUIDelegate {
    let id = UUID()
    let priv: Bool
    @Published var url = ""
    @Published var title = "New tab"
    @Published var canBack = false
    @Published var canFwd = false
    @Published var loading = false
    @Published var progress: Double = 0
    @Published var showHome = true

    var web: WKWebView?
    weak var browser: Browser?
    private var obs: [NSKeyValueObservation] = []

    init(priv: Bool) {
        self.priv = priv
        super.init()
    }

    func ensureWeb() -> WKWebView {
        if let w = web { return w }
        let cfg = WKWebViewConfiguration()
        cfg.websiteDataStore = priv ? WKWebsiteDataStore.nonPersistent() : WKWebsiteDataStore.default()
        cfg.allowsInlineMediaPlayback = true
        cfg.mediaTypesRequiringUserActionForPlayback = []
        AdBlock.attach(to: cfg.userContentController)
        let w = WKWebView(frame: .zero, configuration: cfg)
        w.navigationDelegate = self
        w.uiDelegate = self
        w.allowsBackForwardNavigationGestures = true
        w.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
        obs = [
            w.observe(\.url, options: [.new]) { [weak self] wv, _ in
                DispatchQueue.main.async { self?.url = wv.url?.absoluteString ?? "" }
            },
            w.observe(\.title, options: [.new]) { [weak self] wv, _ in
                DispatchQueue.main.async { if let t = wv.title, !t.isEmpty { self?.title = t } }
            },
            w.observe(\.canGoBack, options: [.new]) { [weak self] wv, _ in
                DispatchQueue.main.async { self?.canBack = wv.canGoBack }
            },
            w.observe(\.canGoForward, options: [.new]) { [weak self] wv, _ in
                DispatchQueue.main.async { self?.canFwd = wv.canGoForward }
            },
            w.observe(\.estimatedProgress, options: [.new]) { [weak self] wv, _ in
                DispatchQueue.main.async { self?.progress = wv.estimatedProgress }
            },
            w.observe(\.isLoading, options: [.new]) { [weak self] wv, _ in
                DispatchQueue.main.async { self?.loading = wv.isLoading }
            }
        ]
        web = w
        return w
    }

    static func fix(_ s: String) -> URL? {
        if s.contains("://") { return URL(string: s) }
        if s.contains(".") && !s.contains(" ") { return URL(string: "https://" + s) }
        let q = s.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? s
        return URL(string: Store.engines[Store.shared.engine % Store.engines.count].1 + q)
    }

    func load(_ text: String) {
        let t = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !t.isEmpty, let u = TabState.fix(t) else { return }
        showHome = false
        ensureWeb().load(URLRequest(url: u))
    }

    func goBack() {
        if let w = web, w.canGoBack { w.goBack() } else { showHome = true }
    }

    // MARK: WKNavigationDelegate / WKUIDelegate
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        let u = webView.url?.absoluteString ?? ""
        let t = webView.title ?? u
        if !priv && !u.isEmpty { Store.shared.addHistory(title: t, url: u) }
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let u = navigationAction.request.url, let s = u.scheme?.lowercased(),
           !["http", "https", "about", "file", "data", "blob"].contains(s) {
            UIApplication.shared.open(u)
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if let u = navigationAction.request.url {
            DispatchQueue.main.async { self.browser?.newTab(url: u.absoluteString, priv: self.priv) }
        }
        return nil
    }
}

final class Browser: ObservableObject {
    @Published var tabs: [TabState] = []
    @Published var cur = 0

    var tab: TabState { tabs[min(max(cur, 0), tabs.count - 1)] }

    init() {
        newTab()
        AdBlock.onReady = { [weak self] in self?.applyAds() }
    }

    func newTab(url: String? = nil, priv: Bool = false) {
        let t = TabState(priv: priv)
        t.browser = self
        tabs.append(t)
        cur = tabs.count - 1
        if let u = url { t.load(u) }
    }

    func close(_ i: Int) {
        guard tabs.indices.contains(i) else { return }
        tabs[i].web?.stopLoading()
        tabs.remove(at: i)
        if tabs.isEmpty { newTab() } else { cur = min(cur, tabs.count - 1) }
    }

    func applyAds() {
        for t in tabs { if let w = t.web { AdBlock.attach(to: w.configuration.userContentController) } }
    }
}

/// Hosts a tab's WKWebView inside SwiftUI (the web view lives as long as the tab).
struct WebContainer: UIViewRepresentable {
    @ObservedObject var tab: TabState

    func makeUIView(context: Context) -> UIView { UIView() }

    func updateUIView(_ ui: UIView, context: Context) {
        let w = tab.ensureWeb()
        if w.superview !== ui {
            ui.subviews.forEach { $0.removeFromSuperview() }
            w.frame = ui.bounds
            w.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            ui.addSubview(w)
        }
    }
}
