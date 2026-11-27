import Foundation
import WebKit

/// Ad / tracker blocking: Safari content-blocker rules + a start-of-page script (YouTube ad removal).
enum AdBlock {
    static let hosts: [String] = [
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "googletagservices.com",
        "adservice.google.com",
        "2mdn.net",
        "google-analytics.com",
        "googletagmanager.com",
        "adtrafficquality.google",
        "fundingchoicesmessages.google.com",
        "moatads.com",
        "adnxs.com",
        "adsrvr.org",
        "criteo.com",
        "criteo.net",
        "taboola.com",
        "outbrain.com",
        "scorecardresearch.com",
        "quantserve.com",
        "quantcount.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "smartadserver.com",
        "amazon-adsystem.com",
        "advertising.com",
        "adform.net",
        "bidswitch.net",
        "contextweb.com",
        "lijit.com",
        "sovrn.com",
        "indexww.com",
        "yieldmo.com",
        "teads.tv",
        "sharethrough.com",
        "33across.com",
        "media.net",
        "revcontent.com",
        "mgid.com",
        "zedo.com",
        "adcolony.com",
        "applovin.com",
        "unityads.unity3d.com",
        "mopub.com",
        "inmobi.com",
        "vungle.com",
        "chartboost.com",
        "ironsrc.com",
        "popads.net",
        "propellerads.com",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.com",
        "adsterra.com",
        "clickadu.com",
        "onclickads.net",
        "adskeeper.com",
        "hotjar.com",
        "mixpanel.com",
        "ads.youtube.com",
        "ads.tiktok.com",
        "analytics.tiktok.com",
        "ads.twitter.com",
        "static.ads-twitter.com",
        "analytics.twitter.com",
        "ads.linkedin.com",
        "px.ads.linkedin.com",
        "snap.licdn.com",
        "ads.pinterest.com",
        "ct.pinterest.com",
        "an.facebook.com",
        "pixel.facebook.com",
        "adsafeprotected.com",
        "doubleverify.com",
        "serving-sys.com",
        "flashtalking.com",
        "innovid.com",
        "spotxchange.com",
        "springserve.com",
        "tremorhub.com",
        "tapad.com",
        "bluekai.com",
        "krxd.net",
        "demdex.net",
        "rlcdn.com",
        "agkn.com",
        "adition.com",
        "adroll.com",
        "bat.bing.com",
        "clarity.ms",
        "an.yandex.ru",
        "everesttech.net",
        "mathtag.com",
        "w55c.net",
        "bttrack.com",
        "bidr.io",
        "adgrx.com",
        "sitescout.com",
        "stickyadstv.com",
        "freewheel.tv",
        "fwmrm.net",
        "btloader.com",
        "pubmine.com",
        "ezoic.net",
        "adsymptotic.com",
        "smaato.net",
        "admob.com",
        "rfihub.com",
        "turn.com",
        "yieldlab.net"
    ]
    static let patterns: [String] = [
        "/pagead/",
        "/adsbygoogle",
        "/api/stats/ads",
        "/ptracking",
        "/pcs/activeview",
        "/gampad/ads",
        "/get_midroll_",
        "adformat=",
        "/doubleclick/",
        "googleads.",
        "pubads.",
        "/prebid",
        "/adview?",
        "/adserver/",
        "/adserve",
        "/ad-delivery",
        "/advertising/",
        "/adframe",
        "/adsense/",
        "/ads/ga-audiences",
        "/ad_status.js",
        "/ads.js",
        "/adblock.js"
    ]
    static let cosmeticSelectors = "ins.adsbygoogle,.adsbygoogle,[id^=\"google_ads_\"],[id^=\"div-gpt-ad\"],[id*=\"google_ads_iframe\"],iframe[src*=\"doubleclick.net\"],iframe[src*=\"googlesyndication.com\"],iframe[id^=\"aswift\"],[class*=\"ad-slot\"],[class*=\"adslot\"],[class*=\"ad-banner\"],[class*=\"adbanner\"],[class*=\"ad-container\"],[class*=\"ads-container\"],[class*=\"advert\"],[id*=\"advert\"],[id^=\"ad-\"],[id^=\"ads-\"],[class^=\"ad-\"],[class^=\"ads-\"],a[href*=\"doubleclick.net\"],ytd-ad-slot-renderer,ytd-in-feed-ad-layout-renderer,ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-promoted-video-renderer,ytd-banner-promo-renderer,ytd-statement-banner-renderer,ytd-video-masthead-ad-v3-renderer,ytd-primetime-promo-renderer,#masthead-ad,#player-ads,.video-ads,.ytp-ad-module,.ytp-ad-overlay-container,.ytp-ad-image-overlay,.ytp-ad-player-overlay,ytm-promoted-sparkles-web-renderer,ytm-promoted-video-renderer,ytm-companion-ad-renderer,ytm-ad-slot-renderer,ytm-in-feed-ad-layout-renderer,ytm-display-ad-renderer,ad-slot-renderer,ytm-statement-banner-renderer,ytm-mealbar-promo-renderer,.ytm-ad-slot"

    static let script: String = #"""
(function(){if(window.__stormAds)return;window.__stormAds=1;
var css="ins.adsbygoogle,.adsbygoogle,[id^=\"google_ads_\"],[id^=\"div-gpt-ad\"],[id*=\"google_ads_iframe\"],iframe[src*=\"doubleclick.net\"],iframe[src*=\"googlesyndication.com\"],iframe[id^=\"aswift\"],[class*=\"ad-slot\"],[class*=\"adslot\"],[class*=\"ad-banner\"],[class*=\"adbanner\"],[class*=\"ad-container\"],[class*=\"ads-container\"],[class*=\"advert\"],[id*=\"advert\"],[id^=\"ad-\"],[id^=\"ads-\"],[class^=\"ad-\"],[class^=\"ads-\"],a[href*=\"doubleclick.net\"],ytd-ad-slot-renderer,ytd-in-feed-ad-layout-renderer,ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-promoted-video-renderer,ytd-banner-promo-renderer,ytd-statement-banner-renderer,ytd-video-masthead-ad-v3-renderer,ytd-primetime-promo-renderer,#masthead-ad,#player-ads,.video-ads,.ytp-ad-module,.ytp-ad-overlay-container,.ytp-ad-image-overlay,.ytp-ad-player-overlay,ytm-promoted-sparkles-web-renderer,ytm-promoted-video-renderer,ytm-companion-ad-renderer,ytm-ad-slot-renderer,ytm-in-feed-ad-layout-renderer,ytm-display-ad-renderer,ad-slot-renderer,ytm-statement-banner-renderer,ytm-mealbar-promo-renderer,.ytm-ad-slot{display:none!important;}";
function put(){var r=document.head||document.documentElement;if(!r){setTimeout(put,10);return;}var s=document.createElement('style');s.textContent=css;r.appendChild(s);}put();
if(!/(^|\.)youtube(-nocookie)?\.com$/.test(location.hostname))return;
function strip(o){if(!o||typeof o!=='object')return o;['adPlacements','playerAds','adSlots'].forEach(function(k){if(k in o){try{delete o[k];}catch(e){o[k]=[];}}});return o;}
var jp=JSON.parse;JSON.parse=function(){var r=jp.apply(this,arguments);try{strip(r);if(r&&r.playerResponse)strip(r.playerResponse);}catch(e){}return r;};
try{var rj=Response.prototype.json;Response.prototype.json=function(){return rj.apply(this,arguments).then(function(d){try{strip(d);if(d&&d.playerResponse)strip(d.playerResponse);}catch(e){}return d;});};}catch(e){}
try{var ipr;Object.defineProperty(window,'ytInitialPlayerResponse',{configurable:true,get:function(){return ipr;},set:function(v){try{strip(v);}catch(e){}ipr=v;}});}catch(e){}
setInterval(function(){try{var p=document.querySelector('.html5-video-player');var ad=p&&p.classList.contains('ad-showing');var v=document.querySelector('video.html5-main-video')||document.querySelector('video');
if(ad&&v){if(!v.__sa){v.__sa=1;v.__m=v.muted;v.__r=v.playbackRate;}v.muted=true;if(isFinite(v.duration)&&v.duration>0&&v.currentTime<v.duration-0.1)v.currentTime=v.duration;try{v.playbackRate=16;}catch(e){}
var b=document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button');if(b)b.click();}
else if(v&&v.__sa){v.__sa=0;v.muted=v.__m;try{v.playbackRate=v.__r||1;}catch(e){}}
var c=document.querySelector('.ytp-ad-overlay-close-button');if(c)c.click();}catch(e){}},250);
})();
"""#

    static var blockList: WKContentRuleList?
    static var cssList: WKContentRuleList?
    static var onReady: (() -> Void)?

    static func esc(_ s: String) -> String {
        var o = ""
        for ch in s { if ".?+*()[]{}|\\^$".contains(ch) { o.append("\\") }; o.append(ch) }
        return o
    }

    static func blockJSON() -> String {
        var rules: [[String: Any]] = []
        for h in hosts {
            rules.append(["trigger": ["url-filter": "^https?://[^/]*" + esc(h) + "[:/]"], "action": ["type": "block"]])
        }
        for p in patterns {
            rules.append(["trigger": ["url-filter": esc(p)], "action": ["type": "block"]])
        }
        let data = (try? JSONSerialization.data(withJSONObject: rules, options: [])) ?? Data("[]".utf8)
        return String(data: data, encoding: .utf8) ?? "[]"
    }

    static func cssJSON() -> String {
        let rules: [[String: Any]] = [["trigger": ["url-filter": ".*"], "action": ["type": "css-display-none", "selector": cosmeticSelectors]]]
        let data = (try? JSONSerialization.data(withJSONObject: rules, options: [])) ?? Data("[]".utf8)
        return String(data: data, encoding: .utf8) ?? "[]"
    }

    /// Compile the rule lists once at launch.
    static func prepare() {
        let store = WKContentRuleListStore.default()
        store?.compileContentRuleList(forIdentifier: "storm-block", encodedContentRuleList: blockJSON()) { list, _ in
            DispatchQueue.main.async { blockList = list; onReady?() }
        }
        store?.compileContentRuleList(forIdentifier: "storm-css", encodedContentRuleList: cssJSON()) { list, _ in
            DispatchQueue.main.async { cssList = list; onReady?() }
        }
    }

    static func attach(to ucc: WKUserContentController) {
        detach(from: ucc)
        guard Store.shared.ads else { return }
        if let l = blockList { ucc.add(l) }
        if let l = cssList { ucc.add(l) }
        ucc.addUserScript(WKUserScript(source: script, injectionTime: .atDocumentStart, forMainFrameOnly: false))
    }

    static func detach(from ucc: WKUserContentController) {
        ucc.removeAllContentRuleLists()
        ucc.removeAllUserScripts()
    }
}
