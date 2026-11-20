package com.storm.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.provider.Settings;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.FileProvider;
import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;
import com.wireguard.config.Config;
import androidx.webkit.ScriptHandler;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.security.KeyStore;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {
    static final int ORANGE = 0xFFFF5500, LIME = 0xFFC8F23C, BLUE = 0xFF4A4DFF, DARKTXT = 0xFF111111,
        WHITE = 0xFFFFFFFF, PINK = 0xFFFF4FA3, PURPLE = 0xFF8E6BFF, YELLOW = 0xFFFFC928;
    static final int[] NCOL = {LIME, YELLOW, ORANGE, BLUE, PINK, WHITE};
    static final int[] ACC = {ORANGE, BLUE, LIME, PINK, PURPLE, YELLOW};
    static final String DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    static final String[][] ENGINES = {
        {"DuckDuckGo", "https://duckduckgo.com/?q="}, {"Google", "https://www.google.com/search?q="},
        {"Brave", "https://search.brave.com/search?q="}, {"Bing", "https://www.bing.com/search?q="}};
    static final String[] ADS = {"doubleclick.net", "googlesyndication.com", "googleadservices.com", "googletagservices.com", "adservice.google.com",
        "2mdn.net", "google-analytics.com", "googletagmanager.com", "adtrafficquality.google", "fundingchoicesmessages.google.com",
        "moatads.com", "adnxs.com", "adsrvr.org", "criteo.com", "criteo.net", "taboola.com", "outbrain.com", "scorecardresearch.com",
        "quantserve.com", "quantcount.com", "rubiconproject.com", "pubmatic.com", "openx.net", "casalemedia.com", "smartadserver.com",
        "amazon-adsystem.com", "advertising.com", "adform.net", "bidswitch.net", "contextweb.com", "lijit.com", "sovrn.com",
        "indexww.com", "yieldmo.com", "teads.tv", "sharethrough.com", "33across.com", "media.net", "revcontent.com", "mgid.com",
        "zedo.com", "adcolony.com", "applovin.com", "unityads.unity3d.com", "mopub.com", "inmobi.com", "vungle.com", "chartboost.com",
        "ironsrc.com", "popads.net", "propellerads.com", "exoclick.com", "juicyads.com", "trafficjunky.com", "adsterra.com",
        "clickadu.com", "onclickads.net", "adskeeper.com", "hotjar.com", "mixpanel.com", "ads.youtube.com", "ads.tiktok.com",
        "analytics.tiktok.com", "ads.twitter.com", "static.ads-twitter.com", "analytics.twitter.com", "ads.linkedin.com",
        "px.ads.linkedin.com", "snap.licdn.com", "ads.pinterest.com", "ct.pinterest.com", "an.facebook.com", "pixel.facebook.com",
        "adsafeprotected.com", "doubleverify.com", "serving-sys.com", "flashtalking.com", "innovid.com", "spotxchange.com",
        "springserve.com", "tremorhub.com", "tapad.com", "bluekai.com", "krxd.net", "demdex.net", "rlcdn.com", "agkn.com",
        "adition.com", "adroll.com", "bat.bing.com", "clarity.ms", "an.yandex.ru", "everesttech.net", "mathtag.com", "w55c.net",
        "bttrack.com", "bidr.io", "adgrx.com", "sitescout.com", "stickyadstv.com", "freewheel.tv", "fwmrm.net", "btloader.com",
        "pubmine.com", "ezoic.net", "adsymptotic.com", "smaato.net", "admob.com", "rfihub.com", "turn.com", "yieldlab.net"};
    static final String[] PATS = {"/pagead/", "/adsbygoogle", "/api/stats/ads", "/ptracking", "/pcs/activeview", "/gampad/ads", "/get_midroll_", "adformat=",
        "/doubleclick/", "googleads.", "pubads.", "/prebid", "/adview?", "/adserver/", "/adserve", "/ad-delivery", "/advertising/",
        "/adframe", "/adsense/", "/ads/ga-audiences", "/ad_status.js", "/ads.js", "/adblock.js"};
    static final HashSet<String> ADSET = new HashSet<>();
    static { for (String h : ADS) ADSET.add(h); }
    static final String ADS_CSS = "ins.adsbygoogle,.adsbygoogle,[id^=\"google_ads_\"],[id^=\"div-gpt-ad\"],[id*=\"google_ads_iframe\"],iframe[src*=\"doubleclick.net\"],iframe[src*=\"googlesyndication.com\"],iframe[id^=\"aswift\"],[class*=\"ad-slot\"],[class*=\"adslot\"],[class*=\"ad-banner\"],[class*=\"adbanner\"],[class*=\"ad-container\"],[class*=\"ads-container\"],[class*=\"advert\"],[id*=\"advert\"],[id^=\"ad-\"],[id^=\"ads-\"],[class^=\"ad-\"],[class^=\"ads-\"],a[href*=\"doubleclick.net\"],ytd-ad-slot-renderer,ytd-in-feed-ad-layout-renderer,ytd-display-ad-renderer,ytd-promoted-sparkles-web-renderer,ytd-promoted-video-renderer,ytd-banner-promo-renderer,ytd-statement-banner-renderer,ytd-video-masthead-ad-v3-renderer,ytd-primetime-promo-renderer,#masthead-ad,#player-ads,.video-ads,.ytp-ad-module,.ytp-ad-overlay-container,.ytp-ad-image-overlay,.ytp-ad-player-overlay,ytm-promoted-sparkles-web-renderer,ytm-promoted-video-renderer,ytm-companion-ad-renderer,ytm-ad-slot-renderer,ytm-in-feed-ad-layout-renderer,ytm-display-ad-renderer,ad-slot-renderer,ytm-statement-banner-renderer,ytm-mealbar-promo-renderer,.ytm-ad-slot{display:none!important;}";
    static final String ADS_JS = "(function(){if(window.__stormAds)return;window.__stormAds=1;"
        + "var css=" + JSONObject.quote(ADS_CSS) + ";"
        + "function put(){var r=document.head||document.documentElement;if(!r){setTimeout(put,10);return;}var s=document.createElement('style');s.textContent=css;r.appendChild(s);}put();"
        + "if(!/(^|\\.)youtube(-nocookie)?\\.com$/.test(location.hostname))return;"
        + "function strip(o){if(!o||typeof o!=='object')return o;['adPlacements','playerAds','adSlots'].forEach(function(k){if(k in o){try{delete o[k];}catch(e){o[k]=[];}}});return o;}"
        + "var jp=JSON.parse;JSON.parse=function(){var r=jp.apply(this,arguments);try{strip(r);if(r&&r.playerResponse)strip(r.playerResponse);}catch(e){}return r;};"
        + "try{var rj=Response.prototype.json;Response.prototype.json=function(){return rj.apply(this,arguments).then(function(d){try{strip(d);if(d&&d.playerResponse)strip(d.playerResponse);}catch(e){}return d;});};}catch(e){}"
        + "try{var ipr;Object.defineProperty(window,'ytInitialPlayerResponse',{configurable:true,get:function(){return ipr;},set:function(v){try{strip(v);}catch(e){}ipr=v;}});}catch(e){}"
        + "setInterval(function(){try{var p=document.querySelector('.html5-video-player');var ad=p&&p.classList.contains('ad-showing');var v=document.querySelector('video.html5-main-video')||document.querySelector('video');"
        + "if(ad&&v){if(!v.__sa){v.__sa=1;v.__m=v.muted;v.__r=v.playbackRate;}v.muted=true;if(isFinite(v.duration)&&v.duration>0&&v.currentTime<v.duration-0.1)v.currentTime=v.duration;try{v.playbackRate=16;}catch(e){}"
        + "var b=document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button');if(b)b.click();}"
        + "else if(v&&v.__sa){v.__sa=0;v.muted=v.__m;try{v.playbackRate=v.__r||1;}catch(e){}}"
        + "var c=document.querySelector('.ytp-ad-overlay-close-button');if(c)c.click();}catch(e){}},250);"
        + "})();";
    static final String[][] QUICK = {
        {"Google", "https://www.google.com"}, {"YouTube", "https://m.youtube.com"},
        {"Wikipedia", "https://en.m.wikipedia.org"}, {"GitHub", "https://github.com"},
        {"Brave", "https://search.brave.com"}, {"Reddit", "https://www.reddit.com"}};

    static class Tab {
        boolean min; float bx = -1, by; int wi = -1; ScriptHandler adHandle; WebView web; String title = "New tab", url = ""; boolean priv, desktop; Bitmap thumb;
    }

    static final String MEDIA_JS = "(function(){if(window.__stormMedia)return;window.__stormMedia=1;"
        + "function rep(){var m=document.querySelectorAll('video,audio'),p=false;for(var i=0;i<m.length;i++){if(!m[i].paused&&!m[i].ended&&m[i].readyState>2)p=true;}try{StormMedia.state(p,document.title);}catch(e){}}"
        + "['play','playing','pause','ended','emptied'].forEach(function(ev){document.addEventListener(ev,function(){setTimeout(rep,150);},true);});"
        + "window.__stormCmd=function(c){var m=document.querySelectorAll('video,audio');for(var i=0;i<m.length;i++){if(c=='pause'&&!m[i].paused)m[i].pause();if(c=='play'&&m[i].paused&&m[i].currentTime>0)m[i].play();}};"
        + "%SPOOF%})();";
    static final String SPOOF_JS = "try{Object.defineProperty(document,'hidden',{configurable:true,get:function(){return false;}});Object.defineProperty(document,'visibilityState',{configurable:true,get:function(){return 'visible';}});document.addEventListener('visibilitychange',function(e){e.stopImmediatePropagation();},true);}catch(e){}";
    static MainActivity inst;

    // WebView that keeps running (audio/video) when the app is in the background
    static class SWebView extends WebView {
        static volatile boolean keep = true;
        SWebView(Context c) { super(c); }
        @Override protected void onWindowVisibilityChanged(int v) { if (keep && v != View.VISIBLE) return; super.onWindowVisibilityChanged(v); }
    }

    static class Nav { LinearLayout cell; ImageView ic; TextView lb; }

    int PAPER = 0xFFF0EEE9, INK = 0xFF111111;
    FrameLayout notesPanel;
    View hint;
    long revealAt;
    boolean zoneStart;
    boolean notesOpen, noteEditing, swipeOk, swiping, swipeClose, hapticOn = true;
    float nsx, nsy; long nst;
    String[] curNote; EditText noteTitle, noteBody;
    final Runnable noteSave = () -> saveCurrentNote();
    boolean oled, mediaOn;
    final AtomicInteger blocked = new AtomicInteger();
    String mediaTitle = "Playing";
    SharedPreferences sp;
    FrameLayout wrapper, content, switcher, library, home;
    LinearLayout dock, navRow;
    ProgressBar bar;
    TextView addrLabel, pillText, goText;
    FrameLayout goCell;
    EditText urlField;
    Nav nBack, nFwd, nHome, nTabs, nMenu;
    Ripples ripples;
    ArrayList<Tab> tabs = new ArrayList<>();
    int cur = -1, topInset, botInset, LW;
    boolean blockAds = true, editing, swPriv, imeUp, barsShown = true, rippleOn = true, blurOn = true, autoHide = true;
    float dx0, dy0, dScale = .8f; long dt0;
    Typeface TF_D, TF_L;
    Bitmap homeBg;
    String cua;
    FrameLayout bubbleLayer;
    Bitmap wallBmp; String wallKey;
    GoBackend vpnBackend; Tunnel vpnTunnel; boolean vpnUp, vpnBusy; long vpnSince; String libMode = ""; TextView vpnSessTv;
    final Bitmap[] scBmp = new Bitmap[QUICK.length];
    View blurTarget; float blurLvl;
    final Handler hnd = new Handler(Looper.getMainLooper());
    final Object lock = new Object();
    final ExecutorService io = Executors.newSingleThreadExecutor();
    final Runnable hideRun = () -> {
        if (switcher.getVisibility() != View.VISIBLE && library.getVisibility() != View.VISIBLE) hideBars();
    };
    final Runnable blurDecay = new Runnable() {
        @Override public void run() {
            View t = blurTarget; if (t == null) return;
            blurLvl *= .76f;
            if (blurLvl < 1f) { clearBlur(t); blurTarget = null; blurLvl = 0; return; }
            setBlur(t, blurLvl); hnd.postDelayed(this, 16);
        }
    };
    View customView;
    WebChromeClient.CustomViewCallback customCb;

    // ================= helpers =================
    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    int on(int fill) {
        if (fill == BLUE || fill == PURPLE) return WHITE;
        if (fill == INK) return PAPER;
        if (fill == PAPER) return INK;
        return DARKTXT;
    }

    int ia(int a) { return (a << 24) | (INK & 0xFFFFFF); }
    int pageBg(boolean priv) { return priv && !oled ? INK : PAPER; }
    int pageFg(boolean priv) { return priv && !oled ? PAPER : INK; }
    int[] qfill() { return new int[]{LIME, BLUE, INK, WHITE, ORANGE, PAPER}; }

    String n2(int n) { return String.format(Locale.US, "%02d", n); }

    GradientDrawable box(int fill, int stroke) {
        GradientDrawable g = new GradientDrawable(); g.setColor(fill); if (stroke != 0) g.setStroke(LW, stroke); return g;
    }

    TextView D(String s, float size, int color) {
        TextView t = new TextView(this); t.setText(s.toUpperCase()); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(TF_D); t.setTextScaleX(dScale); t.setIncludeFontPadding(false); t.setLineSpacing(0, .86f);
        t.setLetterSpacing(-.02f); return t;
    }

    TextView L(String s, float size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setTypeface(TF_L); t.setAllCaps(true); t.setIncludeFontPadding(false); t.setLetterSpacing(.04f); return t;
    }

    View vline(int c) { View v = new View(this); v.setBackgroundColor(c); return v; }
    LinearLayout.LayoutParams vlp() { return new LinearLayout.LayoutParams(LW, -1); }
    LinearLayout.LayoutParams hlp() { return new LinearLayout.LayoutParams(-1, LW); }

    void hap(int type) {
        if (!hapticOn || wrapper == null) return;
        try { wrapper.performHapticFeedback(type, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING | HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING); }
        catch (Throwable e) { }
    }
    void tick() { hap(HapticFeedbackConstants.KEYBOARD_TAP); }
    void softTick() { hap(Build.VERSION.SDK_INT >= 23 ? HapticFeedbackConstants.CONTEXT_CLICK : HapticFeedbackConstants.KEYBOARD_TAP); }
    void confirmH() { hap(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.CONFIRM : HapticFeedbackConstants.LONG_PRESS); }
    void rejectH() { hap(Build.VERSION.SDK_INT >= 30 ? HapticFeedbackConstants.REJECT : HapticFeedbackConstants.LONG_PRESS); }

    void press(View v) {
        v.setOnTouchListener((x, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) { tick(); x.animate().setStartDelay(0).scaleX(.96f).scaleY(.96f).setDuration(70).start(); }
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL)
                x.animate().setStartDelay(0).scaleX(1f).scaleY(1f).setDuration(120).start();
            return false;
        });
    }

    void slide(View v, int i) {
        v.setAlpha(0f); v.setTranslationX(-dp(36));
        v.animate().alpha(1f).translationX(0).setStartDelay(Math.min(i, 9) * 55L).setDuration(300)
            .setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    void showOverlay(View v) {
        v.animate().cancel(); v.setVisibility(View.VISIBLE); v.setAlpha(1f);
        v.setTranslationY(getResources().getDisplayMetrics().heightPixels);
        v.animate().setStartDelay(0).translationY(0).setDuration(340).setInterpolator(new DecelerateInterpolator(2.2f)).start();
    }

    void hideOverlay(final View v) {
        v.animate().setStartDelay(0).translationY(getResources().getDisplayMetrics().heightPixels).setDuration(260)
            .setInterpolator(new AccelerateInterpolator(1.4f))
            .withEndAction(() -> { v.setVisibility(View.GONE); v.setTranslationY(0); }).start();
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
    AlertDialog.Builder dlg() { return new AlertDialog.Builder(this, oled ? android.R.style.Theme_Material_Dialog_Alert : android.R.style.Theme_Material_Light_Dialog_Alert); }
    int eng() { return sp.getInt("se", 0); }
    int accentColor() { return ACC[Math.min(ACC.length - 1, Math.max(0, sp.getInt("acc", 0)))]; }

    String hostOf(String u) {
        try { String h = Uri.parse(u).getHost(); if (h == null) return u; return h.startsWith("www.") ? h.substring(4) : h; }
        catch (Exception e) { return u; }
    }

    String fix(String s) {
        s = s.trim();
        if (s.contains("://") || s.startsWith("about:") || s.startsWith("file:")) return s;
        if (s.contains(".") && !s.contains(" ")) return "https://" + s;
        return ENGINES[eng()][1] + Uri.encode(s);
    }

    ArrayList<String[]> read(String k) {
        ArrayList<String[]> l = new ArrayList<>();
        synchronized (lock) {
            try {
                JSONArray a = new JSONArray(sp.getString(k, "[]"));
                for (int i = 0; i < a.length(); i++) {
                    JSONArray e = a.getJSONArray(i); String[] r = new String[e.length()];
                    for (int j = 0; j < r.length; j++) r[j] = e.getString(j);
                    l.add(r);
                }
            } catch (Exception e) { }
        }
        return l;
    }

    void write(String k, ArrayList<String[]> l) {
        synchronized (lock) {
            JSONArray a = new JSONArray();
            for (String[] e : l) { JSONArray x = new JSONArray(); for (String s : e) x.put(s); a.put(x); }
            sp.edit().putString(k, a.toString()).apply();
        }
    }

    void addHistory(final String title, final String url) {
        if (url == null || url.startsWith("about:")) return;
        io.execute(() -> {
            synchronized (lock) {
                ArrayList<String[]> l = read("hist");
                for (int i = l.size() - 1; i >= 0; i--) if (l.get(i)[1].equals(url)) l.remove(i);
                l.add(0, new String[]{title == null ? url : title, url, String.valueOf(System.currentTimeMillis())});
                while (l.size() > 300) l.remove(l.size() - 1);
                write("hist", l);
            }
        });
    }

    void loadFonts() {
        try { TF_D = Typeface.createFromAsset(getAssets(), "fonts/display.ttf"); dScale = 1f; }
        catch (Throwable e) { TF_D = Typeface.create("sans-serif-black", Typeface.NORMAL); dScale = .8f; }
        TF_L = Typeface.create("sans-serif-condensed", Typeface.BOLD);
    }

    // ================= lifecycle =================
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("storm", MODE_PRIVATE);
        blockAds = sp.getBoolean("ads", true); rippleOn = sp.getBoolean("ripple", true);
        blurOn = sp.getBoolean("mblur", true); autoHide = sp.getBoolean("autohide", true); hapticOn = sp.getBoolean("haptic", true);
        blocked.set(sp.getInt("blk_total", 0));
        oled = sp.getBoolean("oled", false);
        PAPER = oled ? 0xFF000000 : 0xFFF0EEE9; INK = oled ? 0xFFF2F2F2 : 0xFF111111;
        SWebView.keep = sp.getBoolean("bgplay", true); inst = this;
        LW = Math.max(2, dp(1)); loadFonts();
        getWindow().setStatusBarColor(Color.TRANSPARENT); getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        topInset = dp(28); botInset = 0;
        wrapper = new FrameLayout(this); wrapper.setBackgroundColor(PAPER);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else {
            int f = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            if (!oled) f |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26 && !oled) f |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            wrapper.setSystemUiVisibility(f);
        }
        content = new FrameLayout(this); content.setBackgroundColor(PAPER);
        wrapper.addView(content, contentLp());
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgressTintList(ColorStateList.valueOf(INK)); bar.setVisibility(View.INVISIBLE);
        wrapper.addView(bar, barLp());
        dock = buildDock();
        wrapper.addView(dock, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        hint = new View(this);
        GradientDrawable hg = new GradientDrawable(); hg.setColor(INK); hg.setCornerRadius(dp(3)); hg.setStroke(LW, PAPER);
        hint.setBackground(hg); hint.setVisibility(View.GONE);
        wrapper.addView(hint, hintLp());
        bubbleLayer = new FrameLayout(this); bubbleLayer.setClipChildren(false);
        wrapper.addView(bubbleLayer, new FrameLayout.LayoutParams(-1, -1));
        library = new FrameLayout(this); library.setVisibility(View.GONE);
        wrapper.addView(library, new FrameLayout.LayoutParams(-1, -1));
        switcher = new FrameLayout(this); switcher.setVisibility(View.GONE);
        wrapper.addView(switcher, new FrameLayout.LayoutParams(-1, -1));
        notesPanel = new FrameLayout(this); notesPanel.setVisibility(View.GONE); notesPanel.setClickable(true);
        wrapper.addView(notesPanel, new FrameLayout.LayoutParams(-1, -1));
        ripples = new Ripples(); wrapper.addView(ripples, new FrameLayout.LayoutParams(-1, -1));
        wrapper.setOnApplyWindowInsetsListener((v, ins) -> {
            boolean ime; int oldTop = topInset;
            if (Build.VERSION.SDK_INT >= 30) {
                topInset = ins.getInsets(WindowInsets.Type.systemBars()).top;
                botInset = Math.max(ins.getInsets(WindowInsets.Type.systemBars()).bottom, ins.getInsets(WindowInsets.Type.ime()).bottom);
                ime = ins.isVisible(WindowInsets.Type.ime());
            } else {
                topInset = ins.getSystemWindowInsetTop(); botInset = ins.getSystemWindowInsetBottom(); ime = botInset > dp(150);
            }
            content.setLayoutParams(contentLp()); bar.setLayoutParams(barLp()); hint.setLayoutParams(hintLp()); dock.setPadding(0, 0, 0, botInset); notesPanel.setPadding(0, 0, 0, botInset);
            if ("ai".equals(libMode)) library.setPadding(0, 0, 0, botInset);
            if (editing && imeUp && !ime) endEdit();
            imeUp = ime;
            if (oldTop != topInset) { Tab ct = cur(); if (ct != null && ct.url.isEmpty()) buildHome(ct.priv, false); }
            return ins;
        });
        setContentView(wrapper);
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) c.setSystemBarsAppearance(oled ? 0 : (WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS),
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        }
        wrapper.requestApplyInsets();
        home = new FrameLayout(this);
        handle(getIntent());
        playOpening();
        cleanUpdates();
        if (sp.getBoolean("autoupd", true) && System.currentTimeMillis() - sp.getLong("lastcheck", 0) > 12 * 3600 * 1000L)
            hnd.postDelayed(() -> checkUpdate(false), 3500);
    }

    FrameLayout.LayoutParams hintLp() {
        FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(dp(48), dp(6), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        l.bottomMargin = botInset + dp(8); return l;
    }

    FrameLayout.LayoutParams contentLp() { FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(-1, -1); l.topMargin = topInset; return l; }
    FrameLayout.LayoutParams barLp() { FrameLayout.LayoutParams l = new FrameLayout.LayoutParams(-1, dp(4), Gravity.TOP); l.topMargin = topInset; return l; }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); handle(i); }

    void handle(Intent i) {
        Uri d = i == null ? null : i.getData();
        if (tabs.isEmpty()) restoreTabs();
        if (d != null) newTab(d.toString(), false);
    }

    void restoreTabs() {
        ArrayList<String[]> l = read("tabs");
        if (l.isEmpty()) { newTab(null, false); return; }
        for (String[] e : l) { Tab t = new Tab(); t.title = e[0]; t.url = e[1]; t.min = e.length > 2 && "1".equals(e[2]); tabs.add(t); }
        cur = Math.min(sp.getInt("tabcur", 0), tabs.size() - 1); ensureVisibleCur(); refreshBubbles();
    }

    void saveTabs() {
        ArrayList<String[]> l = new ArrayList<>(); int ci = 0;
        for (Tab t : tabs) { if (t.priv) continue; if (t == cur()) ci = l.size(); l.add(new String[]{t.title, t.url, t.min ? "1" : "0"}); }
        write("tabs", l); sp.edit().putInt("tabcur", ci).apply();
    }

    @Override protected void onPause() {
        super.onPause(); CookieManager.getInstance().flush(); saveTabs(); sp.edit().putInt("blk_total", blocked.get()).apply();
        if (sp.getBoolean("bgplay", true) && !mediaOn) {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null && am.isMusicActive()) { mediaOn = true; startMedia(mediaTitle, true); }
        }
    }

    @Override protected void onDestroy() {
        for (Tab t : tabs) if (t.web != null) t.web.destroy();
        inst = null; hnd.removeCallbacks(mediaStop);
        try { stopService(new Intent(this, MediaService.class)); } catch (Throwable e) { }
        io.shutdown(); super.onDestroy();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        int a = e.getActionMasked(); float x = e.getRawX(), y = e.getRawY();
        int W = getResources().getDisplayMetrics().widthPixels;
        if (a == MotionEvent.ACTION_DOWN) {
            if (rippleOn) { int[] l = new int[2]; ripples.getLocationOnScreen(l); ripples.spawn(x - l[0], y - l[1]); }
            boolean onB = bubbleHit(x, y);
            if (barsShown && !editing) scheduleHide();
            if (!onB && barsShown && autoHide && !editing && !notesOpen) {
                Tab ct0 = cur();
                boolean ov0 = switcher.getVisibility() == View.VISIBLE || library.getVisibility() == View.VISIBLE || customView != null;
                if (ct0 != null && !ct0.url.isEmpty() && !ov0) { int[] dl = new int[2]; dock.getLocationOnScreen(dl); if (y < dl[1]) hideBars(); }
            }
            nsx = x; nsy = y; nst = e.getEventTime(); swiping = false;
            zoneStart = y > getResources().getDisplayMetrics().heightPixels * .62f;
            Tab t = cur(); boolean onWeb = t != null && !t.url.isEmpty();
            boolean ov = switcher.getVisibility() == View.VISIBLE || library.getVisibility() == View.VISIBLE || customView != null || editing;
            if (notesOpen) { swipeOk = !noteEditing; swipeClose = true; }
            else { swipeOk = !ov && !onB && x > dp(32) && (!onWeb || x < W * .4f); swipeClose = false; }
            if (onB) zoneStart = false;
        } else if (a == MotionEvent.ACTION_MOVE && zoneStart && !barsShown && !editing && !notesOpen) {
            float up = nsy - y;
            Tab ct = cur();
            boolean free = switcher.getVisibility() != View.VISIBLE && library.getVisibility() != View.VISIBLE && customView == null;
            if (free && ct != null && !ct.url.isEmpty() && up > dp(48) && Math.abs(x - nsx) < up * .8f) {
                zoneStart = false; softTick(); showBars();
            }
        }
        if (a == MotionEvent.ACTION_MOVE && swipeOk && !swiping) {
            float dx = x - nsx, dy = y - nsy;
            boolean go = swipeClose ? (-dx > dp(22) && Math.abs(dy) < -dx * .6f) : (dx > dp(22) && Math.abs(dy) < dx * .6f);
            if (go) {
                swiping = true;
                MotionEvent c = MotionEvent.obtain(e); c.setAction(MotionEvent.ACTION_CANCEL); super.dispatchTouchEvent(c); c.recycle();
                if (!swipeClose) prepareNotes();
                softTick();
            } else if (Math.abs(dy) > dp(18)) swipeOk = false;
        }
        if (swiping) {
            float dx = x - nsx;
            if (a == MotionEvent.ACTION_MOVE) {
                notesPanel.setTranslationX(swipeClose ? Math.max(-W, Math.min(0, dx)) : Math.max(-W, Math.min(0, -W + dx)));
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                swiping = false;
                boolean fast = e.getEventTime() - nst < 260 && Math.abs(dx) > dp(60);
                boolean open = swipeClose ? !(dx < -W * .3f || (fast && dx < 0)) : (dx > W * .3f || (fast && dx > 0));
                setNotes(open);
            }
            return true;
        }
        return super.dispatchTouchEvent(e);
    }

    @Override public void onBackPressed() {
        if (customView != null) { hideCustom(); return; }
        if (notesOpen) { if (noteEditing) { saveCurrentNote(); buildNotesList(); } else setNotes(false); return; }
        if (switcher.getVisibility() == View.VISIBLE) { hideOverlay(switcher); return; }
        if (library.getVisibility() == View.VISIBLE) { hideOverlay(library); return; }
        if (editing) { endEdit(); return; }
        Tab t = cur();
        if (t != null && !t.url.isEmpty()) { goBack(); return; }
        if (tabs.size() > 1) { closeTab(cur); return; }
        super.onBackPressed();
    }

    // ================= bottom dock: address cell + tab bar =================
    Nav mkNav(int res, String label, View.OnClickListener l) {
        Nav n = new Nav();
        n.cell = new LinearLayout(this); n.cell.setOrientation(LinearLayout.VERTICAL); n.cell.setGravity(Gravity.CENTER);
        n.ic = new ImageView(this); n.ic.setImageResource(res); n.ic.setColorFilter(INK);
        n.cell.addView(n.ic, new LinearLayout.LayoutParams(dp(24), dp(24)));
        n.lb = L(label, 10, INK); n.lb.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.topMargin = dp(4);
        n.cell.addView(n.lb, lp);
        n.cell.setOnClickListener(l); press(n.cell);
        return n;
    }

    void setNav(Nav n, boolean filled) {
        n.cell.setBackgroundColor(filled ? INK : 0); n.ic.setColorFilter(filled ? PAPER : INK); n.lb.setTextColor(filled ? PAPER : INK);
    }

    LinearLayout buildDock() {
        LinearLayout d = new LinearLayout(this); d.setOrientation(LinearLayout.VERTICAL); d.setBackgroundColor(PAPER);
        d.addView(vline(INK), hlp());
        LinearLayout row = new LinearLayout(this);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL); col.setGravity(Gravity.CENTER_VERTICAL);
        col.setPadding(dp(14), dp(6), dp(10), dp(6)); col.setOnClickListener(v -> startEdit());
        addrLabel = L("Address", 9, ia(0x99)); col.addView(addrLabel);
        FrameLayout slot = new FrameLayout(this);
        pillText = L("", 18, INK); pillText.setSingleLine(true); pillText.setEllipsize(TextUtils.TruncateAt.END);
        pillText.setOnClickListener(v -> startEdit());
        slot.addView(pillText, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL));
        urlField = new EditText(this); urlField.setBackground(null); urlField.setPadding(0, 0, 0, 0); urlField.setSingleLine(true);
        urlField.setTypeface(TF_L); urlField.setTextSize(18); urlField.setTextColor(INK); urlField.setHint("TYPE OR PASTE");
        urlField.setHintTextColor(ia(0x66)); urlField.setSelectAllOnFocus(true); urlField.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlField.setOnEditorActionListener((v, a, e) -> { load(v.getText().toString()); return true; });
        urlField.setOnFocusChangeListener((v, f) -> { if (!f) endEdit(); });
        urlField.setVisibility(View.GONE);
        slot.addView(urlField, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL));
        LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-1, -2); sl.topMargin = dp(3);
        col.addView(slot, sl);
        row.addView(col, new LinearLayout.LayoutParams(0, dp(58), 1f));
        goCell = new FrameLayout(this); goCell.setBackgroundColor(INK);
        goText = L("\u21BB", 20, PAPER); goText.setGravity(Gravity.CENTER);
        goCell.addView(goText, new FrameLayout.LayoutParams(-1, -1));
        goCell.setOnClickListener(v -> {
            if (editing) { load(urlField.getText().toString()); return; }
            Tab t = cur(); if (t == null || t.url.isEmpty() || t.web == null) return;
            if (bar.getVisibility() == View.VISIBLE) t.web.stopLoading(); else t.web.reload();
        });
        press(goCell);
        row.addView(goCell, new LinearLayout.LayoutParams(dp(72), dp(58)));
        d.addView(row);
        d.addView(vline(INK), hlp());

        navRow = new LinearLayout(this);
        nBack = mkNav(R.drawable.ic_back, "Back", v -> goBack());
        nFwd = mkNav(R.drawable.ic_fwd, "Fwd", v -> { Tab t = cur(); if (t != null && t.web != null && t.web.canGoForward()) t.web.goForward(); });
        nHome = mkNav(R.drawable.ic_home, "Home", v -> { Tab t = cur(); if (t != null) { t.url = ""; showCurrent(); } });
        nTabs = mkNav(R.drawable.ic_layers, "Tabs", v -> openSwitcher(false));
        nMenu = mkNav(R.drawable.ic_menu, "Menu", v -> openMenu());
        nTabs.cell.setOnLongClickListener(v -> { hap(HapticFeedbackConstants.LONG_PRESS); minimizeTab(cur()); return true; });
        Nav[] all = {nBack, nFwd, nHome, nTabs, nMenu};
        for (int i = 0; i < all.length; i++) {
            navRow.addView(all[i].cell, new LinearLayout.LayoutParams(0, dp(62), 1f));
            if (i < all.length - 1) navRow.addView(vline(INK), vlp());
        }
        d.addView(navRow);
        return d;
    }

    void startEdit() {
        if (editing) return;
        Tab t = cur(); if (t == null) return;
        editing = true; hnd.removeCallbacks(hideRun); revealDock();
        urlField.setText(t.url); addrLabel.setText("Type or paste");
        pillText.setVisibility(View.GONE); urlField.setVisibility(View.VISIBLE); navRow.setVisibility(View.GONE);
        goText.setText("GO"); goText.setTextSize(16);
        urlField.requestFocus(); urlField.selectAll();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(urlField, InputMethodManager.SHOW_IMPLICIT);
    }

    void endEdit() {
        if (!editing) return;
        editing = false;
        urlField.setVisibility(View.GONE); pillText.setVisibility(View.VISIBLE); navRow.setVisibility(View.VISIBLE);
        goText.setText("\u21BB"); goText.setTextSize(20);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(urlField.getWindowToken(), 0);
        urlField.clearFocus(); Tab t = cur(); setUrlText(t == null ? "" : t.url); scheduleHide();
    }

    void setUrlText(String u) {
        Tab t = cur(); boolean priv = t != null && t.priv;
        if (u == null || u.isEmpty()) { pillText.setText("Search or enter URL"); pillText.setAlpha(.5f); addrLabel.setText(priv ? "Private" : "Address"); }
        else { pillText.setText(hostOf(u)); pillText.setAlpha(1f); addrLabel.setText((priv ? "Private \u00B7 " : "") + (u.startsWith("https") ? "Secure" : "Address")); }
        if (vpnUp) addrLabel.setText(addrLabel.getText() + " \u00B7 VPN");
        goCell.setBackgroundColor(priv ? PURPLE : INK);
        if (!editing) urlField.setText(u);
    }

    // ---- auto-hide dock ----
    void revealDock() {
        if (barsShown) return;
        barsShown = true; revealAt = System.currentTimeMillis(); dock.setVisibility(View.VISIBLE); hint.setVisibility(View.GONE);
        dock.animate().setStartDelay(0).translationY(0).setDuration(260).setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    void hideSoon() { hnd.removeCallbacks(hideRun); hnd.postDelayed(hideRun, 350); }

    void showBars() { hnd.removeCallbacks(hideRun); revealDock(); scheduleHide(); }

    void scheduleHide() {
        hnd.removeCallbacks(hideRun);
        Tab t = cur();
        if (autoHide && t != null && !t.url.isEmpty() && !editing) hnd.postDelayed(hideRun, 3000);
    }

    void hideBars() {
        if (!barsShown || editing) return;
        barsShown = false;
        dock.animate().setStartDelay(0).translationY(dock.getHeight() + dp(4)).setDuration(240).setInterpolator(new AccelerateInterpolator())
            .withEndAction(() -> { if (!barsShown) { dock.setVisibility(View.INVISIBLE); hint.setVisibility(View.VISIBLE); } }).start();
    }

    // ================= shared page parts =================
    View header(int fg, String tag, View.OnClickListener l) {
        LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL);
        TextView b = D("Storm", 24, fg); b.setTextScaleX(dScale * 1.3f);
        r.addView(b, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView t = L(tag, 13, Color.luminance(fg) > .5f ? DARKTXT : WHITE); t.setBackgroundColor(fg); t.setPadding(dp(16), dp(9), dp(16), dp(9));
        t.setOnClickListener(l); press(t); r.addView(t);
        return r;
    }

    LinearLayout.LayoutParams top(int dpv) { LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-1, -2); l.topMargin = dp(dpv); return l; }

    // ================= home =================
    void buildHome(boolean priv, boolean anim) {
        home.removeAllViews();
        final Bitmap wbm = priv ? null : wallBitmap(cur());
        final boolean wall = wbm != null;
        final int fg = wall ? WHITE : pageFg(priv), bgc = wall ? 0xFF111111 : pageBg(priv), acc = priv ? PURPLE : accentColor();
        final int cf = wall ? 0x66000000 : 0;
        home.setBackgroundColor(wall ? 0xFF000000 : bgc);
        if (wall) {
            ImageView wiv = new ImageView(this); wiv.setImageBitmap(wbm); wiv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            home.addView(wiv, new FrameLayout.LayoutParams(-1, -1));
            View dimv = new View(this); dimv.setBackgroundColor(Color.argb(sp.getInt("wall_dim", 35) * 255 / 100, 0, 0, 0));
            home.addView(dimv, new FrameLayout.LayoutParams(-1, -1));
        }
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), dp(10), dp(16), botInset + dp(210));
        int idx = 0;
        View hd = header(fg, "Menu", v -> openMenu()); col.addView(hd); if (anim) slide(hd, idx++);
        TextView title = D(priv ? "Private\nSession" : "Quick\nAccess", 66, fg); col.addView(title, top(10)); if (anim) slide(title, idx++);

        // VPN banner (CONNECTED | ON)
        {
            int vbf = vpnUp ? acc : fg, vbt = vpnUp ? on(acc) : bgc;
            LinearLayout vb = new LinearLayout(this); vb.setBackgroundColor(vbf); vb.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout vl = new LinearLayout(this); vl.setOrientation(LinearLayout.VERTICAL); vl.setPadding(dp(16), dp(12), dp(8), dp(12));
            String vh = vpnHost();
            vl.addView(L(hasVpn() ? "VPN \u00B7 " + (vh.isEmpty() ? "server" : vh) : "VPN \u00B7 tap to set up", 11, vbt));
            vl.addView(D(vpnBusy ? "Working" : (vpnUp ? "Connected" : "VPN off"), 34, vbt), top(4));
            vb.addView(vl, new LinearLayout.LayoutParams(0, -2, 1f));
            vb.addView(vline(vbt), new LinearLayout.LayoutParams(LW, dp(64)));
            FrameLayout vBox = new FrameLayout(this); vBox.setBackgroundColor(vbt);
            TextView vT = D(vpnUp ? "On" : "Off", 40, vbf); vT.setGravity(Gravity.CENTER);
            vBox.addView(vT, new FrameLayout.LayoutParams(-1, -1));
            LinearLayout.LayoutParams vob = new LinearLayout.LayoutParams(dp(100), dp(60)); vob.setMargins(dp(14), 0, dp(14), 0);
            vb.addView(vBox, vob);
            vb.setOnClickListener(v -> vpnToggle());
            vb.setOnLongClickListener(v -> { hap(HapticFeedbackConstants.LONG_PRESS); openVpnScreen(); return true; });
            press(vb);
            col.addView(vb, top(14)); if (anim) slide(vb, idx++);
        }
        // status banner (black block with ON box)
        int bf = fg, bt = bgc;
        LinearLayout ban = new LinearLayout(this); ban.setBackgroundColor(bf); ban.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout bl = new LinearLayout(this); bl.setOrientation(LinearLayout.VERTICAL); bl.setPadding(dp(16), dp(12), dp(8), dp(12));
        bl.addView(L(priv ? "History" : "Ad block \u00B7 " + blocked.get() + " blocked", 11, bt));
        LinearLayout.LayoutParams bt1 = new LinearLayout.LayoutParams(-2, -2); bt1.topMargin = dp(4);
        bl.addView(D(priv ? "Not saved" : (blockAds ? "Protected" : "Exposed"), 38, bt), bt1);
        ban.addView(bl, new LinearLayout.LayoutParams(0, -2, 1f));
        ban.addView(vline(bt), new LinearLayout.LayoutParams(LW, dp(64)));
        FrameLayout onBox = new FrameLayout(this); onBox.setBackgroundColor(bt);
        TextView onT = D(priv ? "Off" : (blockAds ? "On" : "Off"), 40, bf); onT.setGravity(Gravity.CENTER);
        onBox.addView(onT, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams ob = new LinearLayout.LayoutParams(dp(100), dp(60)); ob.setMargins(dp(14), 0, dp(14), 0);
        ban.addView(onBox, ob);
        if (!priv) ban.setOnClickListener(v -> { confirmH(); blockAds = !blockAds; sp.edit().putBoolean("ads", blockAds).apply(); applyAdsAll(); buildHome(false, false); });
        press(ban);
        col.addView(ban, top(14)); if (anim) slide(ban, idx++);

        // hero card
        FrameLayout hero = new FrameLayout(this); hero.setBackgroundColor(acc);
        Bitmap bg = priv ? null : loadBg();
        if (bg != null) {
            ImageView iv = new ImageView(this); iv.setImageBitmap(bg); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            hero.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            View ov = new View(this); ov.setBackgroundColor((acc & 0xFFFFFF) | 0x8C000000); hero.addView(ov, new FrameLayout.LayoutParams(-1, -1));
        }
        int hf = on(acc);
        TextView num = D("01", 30, hf); FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        np.setMargins(0, dp(14), dp(16), 0); hero.addView(num, np);
        ImageView arrow = new ImageView(this); arrow.setImageResource(R.drawable.ic_arrow);
        arrow.setColorFilter((acc == LIME || acc == YELLOW) ? DARKTXT : WHITE);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(dp(130), dp(130), Gravity.BOTTOM | Gravity.END);
        ap.setMargins(0, 0, dp(-14), dp(-14)); hero.addView(arrow, ap);
        LinearLayout ht = new LinearLayout(this); ht.setOrientation(LinearLayout.VERTICAL);
        ht.addView(D("Search", 76, hf)); LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2); hl.topMargin = dp(6);
        ht.addView(L("Tap to type an address", 12, hf), hl);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START); hp.setMargins(dp(16), 0, 0, dp(16));
        hero.addView(ht, hp);
        hero.setOnClickListener(v -> startEdit()); press(hero);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(-1, dp(220)); hlp.topMargin = dp(14);
        col.addView(hero, hlp); if (anim) slide(hero, idx++);
        if (!priv) {
            boolean gin = googleSignedIn();
            LinearLayout acBox = new LinearLayout(this); acBox.setGravity(Gravity.CENTER_VERTICAL); acBox.setBackground(box(cf, fg));
            LinearLayout acT = new LinearLayout(this); acT.setOrientation(LinearLayout.VERTICAL); acT.setPadding(dp(14), dp(10), dp(8), dp(10));
            acT.addView(L("Google account", 10, fg));
            acT.addView(D(gin ? "Signed in" : "Sign in", 26, fg), top(4));
            acBox.addView(acT, new LinearLayout.LayoutParams(0, -2, 1f));
            acBox.addView(vline(fg), new LinearLayout.LayoutParams(LW, dp(64)));
            FrameLayout acG = new FrameLayout(this); acG.setBackgroundColor(fg);
            TextView acGt = D("G", 34, bgc); acGt.setGravity(Gravity.CENTER); acG.addView(acGt, new FrameLayout.LayoutParams(-1, -1));
            acBox.addView(acG, new LinearLayout.LayoutParams(dp(72), dp(64)));
            acBox.setOnClickListener(v -> googleAccount()); press(acBox);
            col.addView(acBox, top(14)); if (anim) slide(acBox, idx++);
        }
        {
            LinearLayout aiB = new LinearLayout(this); aiB.setGravity(Gravity.CENTER_VERTICAL); aiB.setBackground(box(cf, fg));
            LinearLayout aiT = new LinearLayout(this); aiT.setOrientation(LinearLayout.VERTICAL); aiT.setPadding(dp(14), dp(10), dp(8), dp(10));
            aiT.addView(L("AI assistant \u00B7 " + AIS[aiSel()][0], 10, fg));
            aiT.addView(D("Ask anything", 26, fg), top(4));
            aiB.addView(aiT, new LinearLayout.LayoutParams(0, -2, 1f));
            aiB.addView(vline(fg), new LinearLayout.LayoutParams(LW, dp(64)));
            FrameLayout aiG = new FrameLayout(this); aiG.setBackgroundColor(acc);
            TextView aiGt = D("AI", 30, on(acc)); aiGt.setGravity(Gravity.CENTER); aiG.addView(aiGt, new FrameLayout.LayoutParams(-1, -1));
            aiB.addView(aiG, new LinearLayout.LayoutParams(dp(72), dp(64)));
            aiB.setOnClickListener(v -> openAi()); press(aiB);
            col.addView(aiB, top(14)); if (anim) slide(aiB, idx++);
        }

        // shortcuts
        LinearLayout sh = new LinearLayout(this);
        sh.addView(L("Shortcuts", 12, fg), new LinearLayout.LayoutParams(0, -2, 1f)); sh.addView(L(n2(QUICK.length), 12, fg));
        col.addView(sh, top(20));
        LinearLayout row = null;
        for (int i = 0; i < QUICK.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(118)); rp.topMargin = dp(8);
                col.addView(row, rp);
            }
            final String url = QUICK[i][1]; int fill = qfill()[i]; int tc = on(fill);
            int stroke = (fill == PAPER || fill == WHITE) ? INK : (fill == INK && priv && !oled ? PAPER : 0);
            FrameLayout c = new FrameLayout(this); c.setBackground(box(fill, stroke));
            final int si = i;
            Bitmap pic = loadSc(i);
            if (pic != null) {
                ImageView pv = new ImageView(this); pv.setImageBitmap(pic); pv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                c.addView(pv, new FrameLayout.LayoutParams(-1, -1));
                View ov = new View(this);
                ov.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{(fill & 0xFFFFFF) | 0x30000000, (fill & 0xFFFFFF) | 0xCC000000}));
                c.addView(ov, new FrameLayout.LayoutParams(-1, -1));
            }
            TextView cn = L(n2(i + 2), 12, tc); FrameLayout.LayoutParams cnp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
            cnp.setMargins(dp(12), dp(10), 0, 0); c.addView(cn, cnp);
            TextView nm = D(QUICK[i][0], 28, tc); FrameLayout.LayoutParams nmp = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START);
            nmp.setMargins(dp(12), 0, 0, dp(10)); c.addView(nm, nmp);
            ImageView ar = new ImageView(this); ar.setImageResource(R.drawable.ic_arrow); ar.setColorFilter(tc);
            FrameLayout.LayoutParams arp = new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.TOP | Gravity.END); arp.setMargins(0, dp(10), dp(10), 0);
            c.addView(ar, arp);
            ImageView pb2 = new ImageView(this); pb2.setImageResource(R.drawable.ic_image); pb2.setColorFilter(tc);
            pb2.setPadding(dp(5), dp(5), dp(5), dp(5)); pb2.setBackground(box((tc & 0xFFFFFF) | 0x22000000, (tc & 0xFFFFFF) | 0x88000000));
            pb2.setOnClickListener(v -> pickSc(si)); press(pb2);
            FrameLayout.LayoutParams pbp = new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.TOP | Gravity.END); pbp.setMargins(0, dp(8), dp(46), 0);
            c.addView(pb2, pbp);
            c.setOnClickListener(v -> load(url)); press(c);
            c.setOnLongClickListener(v -> { hap(HapticFeedbackConstants.LONG_PRESS); scMenu(si); return true; });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f); if (i % 2 == 1) lp.leftMargin = dp(8);
            row.addView(c, lp); if (anim) slide(c, idx++);
        }

        // data grid
        LinearLayout grid = new LinearLayout(this); grid.setBackground(box(cf, fg));
        String[][] info = {{"Tabs", n2(tabs.size())}, {"Saved", n2(read("bm").size())}, {"History", n2(read("hist").size())}, {"Notes", n2(read("notes").size())}};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            LinearLayout cell = new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL); cell.setPadding(dp(12), dp(10), dp(8), dp(10));
            cell.addView(L(info[i][0], 10, fg)); LinearLayout.LayoutParams vl = new LinearLayout.LayoutParams(-2, -2); vl.topMargin = dp(4);
            cell.addView(D(info[i][1], 28, fg), vl);
            cell.setOnClickListener(v -> { if (k == 0) openSwitcher(false); else if (k == 3) openNotes(); else openLibrary(k == 2); }); press(cell);
            grid.addView(cell, new LinearLayout.LayoutParams(0, -2, 1f));
            if (i < 3) grid.addView(vline(fg), vlp());
        }
        col.addView(grid, top(14)); if (anim) slide(grid, idx++);
        TextView foot = L("No trackers \u00B7 No clutter \u00B7 " + Calendar.getInstance().get(Calendar.YEAR), 10, fg); foot.setAlpha(.6f);
        foot.setGravity(Gravity.CENTER); col.addView(foot, top(18));
        sv.addView(col); attachMotion(sv, col);
        home.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    // ================= tabs =================
    Tab cur() { return cur >= 0 && cur < tabs.size() ? tabs.get(cur) : null; }
    WebView web(Tab t) { if (t.web == null) { t.web = makeWeb(t); if (!t.url.isEmpty()) t.web.loadUrl(t.url); } return t.web; }

    void newTab(String url, boolean priv) {
        Tab t = new Tab(); t.priv = priv; tabs.add(t);
        select(tabs.size() - 1);
        if (url != null) load(url);
        if (switcher.getVisibility() == View.VISIBLE) hideOverlay(switcher);
    }

    void select(int i) {
        Tab p = cur(); if (p != null) snap(p);
        cur = i; showCurrent();
        content.setTranslationX(dp(40)); content.setAlpha(.4f);
        content.animate().setStartDelay(0).translationX(0).alpha(1f).setDuration(240).setInterpolator(new DecelerateInterpolator(2f)).start();
    }

    void showCurrent() {
        Tab t = cur(); content.removeAllViews();
        if (t.url.isEmpty()) { buildHome(t.priv, true); content.addView(home, new FrameLayout.LayoutParams(-1, -1)); setUrlText(""); }
        else { content.addView(web(t), new FrameLayout.LayoutParams(-1, -1)); setUrlText(t.url); }
        content.setBackgroundColor(t.url.isEmpty() ? pageBg(t.priv) : PAPER);
        bar.setVisibility(View.INVISIBLE); refreshUI();
        if (t.url.isEmpty() || !autoHide) showBars(); else hideSoon();
    }

    void refreshUI() {
        Tab t = cur(); if (t == null) return;
        nTabs.lb.setText("Tabs " + countTabs(t.priv));
        nBack.cell.setAlpha(!t.url.isEmpty() ? 1f : .3f);
        nFwd.cell.setAlpha(!t.url.isEmpty() && t.web != null && t.web.canGoForward() ? 1f : .3f);
        setNav(nHome, t.url.isEmpty());
        bar.setProgressTintList(ColorStateList.valueOf(t.priv ? PURPLE : accentColor()));
    }

    int countTabs(boolean priv) { int n = 0; for (Tab x : tabs) if (x.priv == priv) n++; return n; }

    void load(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        Tab t = cur(); String u = fix(raw);
        t.url = u; if (t.web != null) t.web.loadUrl(u); else web(t); showCurrent(); endEdit();
    }

    void goBack() {
        Tab t = cur();
        if (t.web != null && t.web.canGoBack()) t.web.goBack(); else { t.url = ""; showCurrent(); }
    }

    void closeTab(int i) {
        Tab t = tabs.remove(i);
        if (t.web != null) { ViewGroup p = (ViewGroup) t.web.getParent(); if (p != null) p.removeView(t.web); t.web.destroy(); }
        if (tabs.isEmpty()) { cur = -1; newTab(null, false); }
        else { if (i < cur) cur--; if (cur >= tabs.size()) cur = tabs.size() - 1; ensureVisibleCur(); }
        refreshBubbles();
    }

    void snap(Tab t) {
        try {
            if (t.web == null || t.url.isEmpty() || t.web.getWidth() == 0) return;
            Bitmap b = Bitmap.createBitmap(t.web.getWidth() / 3, t.web.getHeight() / 3, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b); c.scale(1 / 3f, 1 / 3f); t.web.draw(c); t.thumb = b;
        } catch (Throwable e) { }
    }

    // ================= tab switcher =================
    void openSwitcher(boolean priv) {
        endEdit(); Tab c = cur(); if (c != null) snap(c);
        swPriv = priv; buildSwitcher(); showOverlay(switcher);
    }

    void buildSwitcher() {
        switcher.removeAllViews();
        final int bgc = pageBg(swPriv), fg = pageFg(swPriv), acc = swPriv ? PURPLE : accentColor();
        switcher.setBackgroundColor(bgc);
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), topInset + dp(10), dp(16), dp(110) + botInset);
        col.addView(header(fg, "Done", v -> hideOverlay(switcher)));
        LinearLayout tr = new LinearLayout(this); tr.setGravity(Gravity.BOTTOM);
        tr.addView(D(swPriv ? "Private" : "Tabs", 72, fg), new LinearLayout.LayoutParams(0, -2, 1f));
        tr.addView(D(n2(countTabs(swPriv)), 72, acc));
        col.addView(tr, top(10));
        LinearLayout seg = new LinearLayout(this); seg.setBackground(box(0, fg));
        for (int m = 0; m < 2; m++) {
            final boolean mode = m == 1; boolean sel = mode == swPriv;
            TextView c = L((mode ? "Private " : "Tabs ") + countTabs(mode), 14, sel ? bgc : fg); c.setGravity(Gravity.CENTER);
            c.setBackgroundColor(sel ? fg : 0); c.setOnClickListener(v -> { swPriv = mode; buildSwitcher(); }); press(c);
            seg.addView(c, new LinearLayout.LayoutParams(0, dp(46), 1f));
            if (m == 0) seg.addView(vline(fg), vlp());
        }
        col.addView(seg, top(14));
        ArrayList<Integer> idx = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).priv == swPriv) idx.add(i);
        if (idx.isEmpty()) {
            TextView e = D(swPriv ? "No private\ntabs" : "No tabs", 40, fg); e.setAlpha(.35f); col.addView(e, top(40));
        }
        for (int k = 0; k < idx.size(); k += 2) {
            LinearLayout row = new LinearLayout(this);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(240)); rp.topMargin = dp(10);
            for (int j = k; j < k + 2; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f); if (j % 2 == 1) lp.leftMargin = dp(10);
                if (j < idx.size()) { View c = tabCard(idx.get(j), j + 1, fg, bgc, acc); row.addView(c, lp); slide(c, j); }
                else row.addView(new View(this), lp);
            }
            col.addView(row, rp);
        }
        sv.addView(col); attachMotion(sv, col);
        switcher.addView(sv, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout bb = new LinearLayout(this); bb.setOrientation(LinearLayout.VERTICAL); bb.setBackgroundColor(bgc);
        bb.addView(vline(fg), hlp());
        LinearLayout br = new LinearLayout(this);
        TextView done = L("Done", 15, fg), nw = L("+ New", 15, on(acc)), all = L("Close all", 15, fg);
        done.setOnClickListener(v -> hideOverlay(switcher));
        nw.setOnClickListener(v -> { newTab(null, swPriv); startEdit(); });
        all.setOnClickListener(v -> {
            for (int i = tabs.size() - 1; i >= 0; i--) if (tabs.get(i).priv == swPriv) closeTab(i);
            if (switcher.getVisibility() == View.VISIBLE) buildSwitcher();
        });
        nw.setBackgroundColor(acc);
        TextView[] cs = {done, nw, all};
        for (int i = 0; i < 3; i++) {
            cs[i].setGravity(Gravity.CENTER); press(cs[i]);
            br.addView(cs[i], new LinearLayout.LayoutParams(0, dp(58), 1f));
            if (i < 2) br.addView(vline(fg), vlp());
        }
        bb.addView(br); bb.setPadding(0, 0, 0, botInset);
        switcher.addView(bb, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
    }

    View tabCard(final int i, int pos, int fg, int bgc, int acc) {
        Tab t = tabs.get(i);
        LinearLayout f = new LinearLayout(this); f.setOrientation(LinearLayout.VERTICAL);
        f.setBackground(box(swPriv && !oled ? 0xFF1E1E1E : (oled ? 0xFF0E0E0E : WHITE), fg));
        boolean sel = i == cur;
        FrameLayout tp = new FrameLayout(this); tp.setBackgroundColor(sel ? acc : fg);
        TextView pn = L(n2(pos), 12, sel ? on(acc) : bgc); FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_VERTICAL | Gravity.START);
        pp.leftMargin = dp(10); tp.addView(pn, pp);
        TextView x = L("\u2715", 13, sel ? on(acc) : bgc); x.setGravity(Gravity.CENTER);
        x.setOnClickListener(v -> { rejectH(); closeTab(i); if (switcher.getVisibility() == View.VISIBLE) buildSwitcher(); });
        tp.addView(x, new FrameLayout.LayoutParams(dp(34), -1, Gravity.END));
        if (!t.url.isEmpty() && !t.min) {
            TextView mn = L("\u2013", 16, sel ? on(acc) : bgc); mn.setGravity(Gravity.CENTER);
            mn.setOnClickListener(v -> { minimizeTab(tabs.get(i)); if (switcher.getVisibility() == View.VISIBLE) buildSwitcher(); });
            FrameLayout.LayoutParams mp = new FrameLayout.LayoutParams(dp(34), -1, Gravity.END); mp.rightMargin = dp(34);
            tp.addView(mn, mp);
        }
        f.addView(tp, new LinearLayout.LayoutParams(-1, dp(32)));
        ImageView iv = new ImageView(this); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (t.thumb != null) iv.setImageBitmap(t.thumb); else iv.setBackgroundColor(oled || swPriv ? 0xFF1A1A1A : 0xFFE4E1DA);
        f.addView(iv, new LinearLayout.LayoutParams(-1, 0, 1f));
        f.addView(vline(fg), hlp());
        TextView title = L((t.min ? "\u2197 floating \u00B7 " : "") + (t.url.isEmpty() ? (t.priv ? "Private tab" : "New tab") : t.title), 12, fg);
        title.setSingleLine(true); title.setEllipsize(TextUtils.TruncateAt.END); title.setPadding(dp(10), dp(10), dp(10), dp(10));
        f.addView(title);
        f.setOnClickListener(v -> { Tab tt = tabs.get(i); hideOverlay(switcher); if (tt.min) restoreTab(tt); else select(i); }); press(f);
        return f;
    }

    // ================= library: bookmarks & history =================
    void openLibrary(boolean hist) { endEdit(); buildLibrary(hist); if (library.getVisibility() != View.VISIBLE) showOverlay(library); }

    void buildLibrary(final boolean hist) {
        library.removeAllViews(); libMode = ""; library.setPadding(0, 0, 0, 0); library.setBackgroundColor(PAPER);
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), topInset + dp(10), dp(16), botInset + dp(40));
        col.addView(header(INK, "Done", v -> hideOverlay(library)));
        col.addView(D(hist ? "History" : "Bookmarks", 60, INK), top(10));
        final LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        if (hist) {
            LinearLayout sb = new LinearLayout(this); sb.setBackground(box(0, INK)); sb.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout sc = new LinearLayout(this); sc.setOrientation(LinearLayout.VERTICAL); sc.setPadding(dp(12), dp(6), dp(8), dp(6));
            sc.addView(L("Search", 9, ia(0x99)));
            final EditText q = new EditText(this); q.setBackground(null); q.setPadding(0, 0, 0, 0); q.setSingleLine(true);
            q.setTypeface(TF_L); q.setTextSize(17); q.setTextColor(INK); q.setHint("TYPE TO FILTER"); q.setHintTextColor(ia(0x66));
            q.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { fillHistory(list, s.toString()); }
                @Override public void afterTextChanged(Editable s) { }
            });
            sc.addView(q, new LinearLayout.LayoutParams(-1, -2));
            sb.addView(sc, new LinearLayout.LayoutParams(0, -2, 1f)); sb.addView(vline(INK), new LinearLayout.LayoutParams(LW, dp(54)));
            TextView clr = L("Clear", 14, PAPER); clr.setBackgroundColor(INK); clr.setGravity(Gravity.CENTER);
            clr.setOnClickListener(v -> dlg().setTitle("Clear history?")
                .setPositiveButton("Clear", (d, w) -> { sp.edit().remove("hist").apply(); fillHistory(list, q.getText().toString()); })
                .setNegativeButton("Cancel", null).show());
            sb.addView(clr, new LinearLayout.LayoutParams(dp(84), dp(54)));
            col.addView(sb, top(14));
        } else {
            final Tab t = cur();
            if (t != null && !t.url.isEmpty() && !t.priv) {
                TextView add = L("+ Add this page", 16, PAPER); add.setBackgroundColor(INK); add.setGravity(Gravity.CENTER);
                add.setOnClickListener(v -> { ArrayList<String[]> l = read("bm"); l.add(0, new String[]{t.title, t.url}); write("bm", l); buildLibrary(false); });
                press(add); col.addView(add, new LinearLayout.LayoutParams(-1, dp(54)) {{ topMargin = dp(14); }});
            }
        }
        col.addView(list, top(14));
        if (hist) fillHistory(list, ""); else fillBookmarks(list);
        sv.addView(col); attachMotion(sv, col);
        library.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    void fillBookmarks(final LinearLayout list) {
        list.removeAllViews();
        final ArrayList<String[]> l = read("bm");
        if (l.isEmpty()) { TextView e = D("Nothing\nsaved yet", 34, INK); e.setAlpha(.35f); list.addView(e); return; }
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackground(box(0, INK));
        for (int i = 0; i < l.size(); i++) {
            final int idx = i; final String[] e = l.get(i);
            LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL);
            TextView num = L(n2(i + 1), 12, INK); num.setGravity(Gravity.CENTER); r.addView(num, new LinearLayout.LayoutParams(dp(44), -2));
            LinearLayout mid = new LinearLayout(this); mid.setOrientation(LinearLayout.VERTICAL); mid.setPadding(0, dp(12), dp(8), dp(12));
            TextView ti = D(e[0], 20, INK); ti.setSingleLine(true); ti.setEllipsize(TextUtils.TruncateAt.END); mid.addView(ti);
            TextView ho = L(hostOf(e[1]), 11, ia(0x99)); ho.setSingleLine(true); LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2); hl.topMargin = dp(4);
            mid.addView(ho, hl); r.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
            TextView x = L("\u2715", 14, PAPER); x.setBackgroundColor(INK); x.setGravity(Gravity.CENTER);
            x.setOnClickListener(v -> { l.remove(idx); write("bm", l); fillBookmarks(list); });
            r.addView(x, new LinearLayout.LayoutParams(dp(52), dp(64)));
            r.setOnClickListener(v -> { hideOverlay(library); load(e[1]); }); press(r);
            p.addView(r); if (i < l.size() - 1) p.addView(vline(INK), hlp());
        }
        list.addView(p);
    }

    void fillHistory(final LinearLayout list, String filter) {
        list.removeAllViews();
        ArrayList<String[]> l = read("hist");
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        long today = c.getTimeInMillis(), yest = today - 86400000L;
        String f = filter.toLowerCase(), last = ""; int n = 0;
        for (final String[] e : l) {
            if (!f.isEmpty() && !(e[0].toLowerCase().contains(f) || e[1].toLowerCase().contains(f))) continue;
            long ts = 0; try { if (e.length > 2) ts = Long.parseLong(e[2]); } catch (Exception x) { }
            String g = ts >= today ? "Today" : ts >= yest ? "Yesterday" : "Earlier";
            if (!g.equals(last)) {
                TextView h = L(g, 12, PAPER); h.setBackgroundColor(INK); h.setPadding(dp(12), dp(7), dp(12), dp(7));
                list.addView(h, top(n == 0 ? 0 : 14)); last = g;
            }
            n++;
            LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setBackground(box(0, INK));
            TextView num = L(n2(n), 12, INK); num.setGravity(Gravity.CENTER); r.addView(num, new LinearLayout.LayoutParams(dp(44), -2));
            LinearLayout mid = new LinearLayout(this); mid.setOrientation(LinearLayout.VERTICAL); mid.setPadding(0, dp(10), dp(10), dp(10));
            TextView ti = D(e[0], 19, INK); ti.setSingleLine(true); ti.setEllipsize(TextUtils.TruncateAt.END); mid.addView(ti);
            TextView ho = L(hostOf(e[1]), 11, ia(0x99)); ho.setSingleLine(true); LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-2, -2); hl.topMargin = dp(3);
            mid.addView(ho, hl); r.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
            r.setOnClickListener(v -> { hideOverlay(library); load(e[1]); });
            r.setOnLongClickListener(v -> {
                ArrayList<String[]> all = read("hist");
                for (int i = all.size() - 1; i >= 0; i--) if (all.get(i)[1].equals(e[1])) all.remove(i);
                write("hist", all); list.removeView(r); return true;
            });
            press(r); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.topMargin = -LW; list.addView(r, rp);
        }
        if (n == 0) { TextView e = D("Nothing\nhere yet", 34, INK); e.setAlpha(.35f); list.addView(e); }
    }

    // ================= menu & settings =================
    LinearLayout panel(int stroke) { LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackground(box(0, stroke)); return p; }

    void addRow(LinearLayout p, boolean last, int num, String name, String state, View.OnClickListener l) {
        LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL);
        TextView n = L(n2(num), 12, INK); n.setGravity(Gravity.CENTER); r.addView(n, new LinearLayout.LayoutParams(dp(44), -2));
        r.addView(D(name, 26, INK), new LinearLayout.LayoutParams(0, -2, 1f));
        if (state != null) {
            boolean onS = state.equals("On");
            TextView s = L(state, 13, onS ? PAPER : INK); s.setGravity(Gravity.CENTER); s.setBackgroundColor(onS ? INK : 0);
            s.setPadding(dp(12), dp(8), dp(12), dp(8));
            if (!onS) s.setBackground(box(0, INK));
            LinearLayout.LayoutParams sl = new LinearLayout.LayoutParams(-2, -2); sl.rightMargin = dp(12); r.addView(s, sl);
        } else {
            ImageView a = new ImageView(this); a.setImageResource(R.drawable.ic_arrow); a.setColorFilter(INK);
            LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(dp(22), dp(22)); al.rightMargin = dp(14); r.addView(a, al);
        }
        r.setOnClickListener(l); press(r);
        p.addView(r, new LinearLayout.LayoutParams(-1, dp(62)));
        if (!last) p.addView(vline(INK), hlp());
    }

    void openMenu() { endEdit(); buildMenu(); if (library.getVisibility() != View.VISIBLE) showOverlay(library); }

    void buildMenu() {
        library.removeAllViews(); libMode = ""; library.setPadding(0, 0, 0, 0); library.setBackgroundColor(PAPER);
        final Tab t = cur();
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), topInset + dp(10), dp(16), botInset + dp(40));
        col.addView(header(INK, "Done", v -> hideOverlay(library)));
        col.addView(D("Menu", 80, INK), top(10));
        LinearLayout p = panel(INK);
        addRow(p, false, 1, "New tab", null, v -> { hideOverlay(library); newTab(null, false); startEdit(); });
        addRow(p, false, 2, "Private tab", null, v -> { hideOverlay(library); newTab(null, true); startEdit(); });
        addRow(p, false, 3, "Notes", null, v -> { hideOverlay(library); openNotes(); });
        addRow(p, false, 4, "VPN", vpnUp ? "On" : "Off", v -> buildVpn());
        addRow(p, false, 5, "AI assistant", null, v -> buildAi());
        addRow(p, false, 6, "Minimize tab", null, v -> { hideOverlay(library); minimizeTab(cur()); });
        addRow(p, false, 7, "Bookmarks", null, v -> { buildLibrary(false); });
        addRow(p, false, 8, "History", null, v -> { buildLibrary(true); });
        addRow(p, false, 9, "Add bookmark", null, v -> {
            if (t.url.isEmpty() || t.priv) { toast("Open a normal page first"); return; }
            ArrayList<String[]> l = read("bm"); l.add(0, new String[]{t.title, t.url}); write("bm", l); toast("Bookmarked");
        });
        addRow(p, false, 10, "Share page", null, v -> {
            if (t.url.isEmpty()) { toast("Open a page first"); return; }
            Intent s = new Intent(Intent.ACTION_SEND); s.setType("text/plain"); s.putExtra(Intent.EXTRA_TEXT, t.url);
            startActivity(Intent.createChooser(s, "Share"));
        });
        addRow(p, false, 11, "Desktop site", t.desktop ? "On" : "Off", v -> {
            t.desktop = !t.desktop;
            if (t.web != null) { t.web.getSettings().setUserAgentString(t.desktop ? DESKTOP_UA : chromeUA()); if (!t.url.isEmpty()) t.web.reload(); }
            buildMenu();
        });
        addRow(p, false, 12, "Ad block", blockAds ? "On" : "Off", v -> { blockAds = !blockAds; sp.edit().putBoolean("ads", blockAds).apply(); applyAdsAll(); buildMenu(); });
        addRow(p, false, 13, "Settings", null, v -> buildSettings());
        addRow(p, true, 14, "Clear data", null, v -> {
            CookieManager.getInstance().removeAllCookies(null); WebStorage.getInstance().deleteAllData();
            for (Tab x : tabs) if (x.web != null) x.web.clearCache(true);
            sp.edit().remove("hist").apply(); toast("Cleared");
        });
        col.addView(p, top(14));
        sv.addView(col); attachMotion(sv, col);
        library.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    TextView sectionLabel(String s) { TextView l = L(s, 12, INK); l.setPadding(0, dp(22), 0, dp(8)); return l; }

    void buildSettings() {
        library.removeAllViews(); libMode = ""; library.setPadding(0, 0, 0, 0); library.setBackgroundColor(PAPER);
        final int acc = accentColor();
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), topInset + dp(10), dp(16), botInset + dp(40));
        col.addView(header(INK, "Back", v -> buildMenu()));
        col.addView(D("Settings", 64, INK), top(10));

        col.addView(sectionLabel("Wallpaper (home page)"));
        final ArrayList<String> wl = wallList(); final int wsel = sp.getInt("wall_sel", -1); final boolean wshuf = sp.getBoolean("wall_shuf", false);
        HorizontalScrollView wscroll = new HorizontalScrollView(this); wscroll.setHorizontalScrollBarEnabled(false);
        LinearLayout wrow = new LinearLayout(this);
        for (int i = 0; i < wl.size(); i++) {
            final int k = i;
            FrameLayout th = new FrameLayout(this);
            ImageView tiv = new ImageView(this); tiv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap tb = decodeImg(new File(getFilesDir(), wl.get(i)), 240); if (tb != null) tiv.setImageBitmap(tb);
            th.addView(tiv, new FrameLayout.LayoutParams(-1, -1));
            GradientDrawable ring = box(0, INK); if (k == wsel && !wshuf) ring.setStroke(LW * 3, acc);
            th.setForeground(ring);
            TextView tx = L("\u2715", 11, PAPER); tx.setBackgroundColor(INK); tx.setGravity(Gravity.CENTER);
            tx.setOnClickListener(v -> {
                ArrayList<String> l = wallList(); if (k >= l.size()) return;
                new File(getFilesDir(), l.get(k)).delete(); l.remove(k); saveWalls(l);
                int sel = sp.getInt("wall_sel", -1); if (sel == k) sel = -1; else if (sel > k) sel--;
                sp.edit().putInt("wall_sel", sel).apply(); wallKey = null; rejectH(); rebuildHome(); buildSettings();
            });
            th.addView(tx, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.TOP | Gravity.END));
            th.setOnClickListener(v -> {
                sp.edit().putInt("wall_sel", k).putBoolean("wall_shuf", false).apply(); wallKey = null; confirmH(); rebuildHome(); buildSettings();
            });
            press(th);
            LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(dp(84), dp(84)); tl.rightMargin = dp(8); wrow.addView(th, tl);
        }
        TextView addT = D("+", 34, INK); addT.setGravity(Gravity.CENTER); addT.setBackground(box(0, INK));
        addT.setOnClickListener(v -> pickWalls()); press(addT);
        wrow.addView(addT, new LinearLayout.LayoutParams(dp(84), dp(84)));
        wscroll.addView(wrow); col.addView(wscroll, new LinearLayout.LayoutParams(-1, -2));
        col.addView(slider("Darken", "wall_dim", 85, 35), top(14));
        col.addView(slider("Blur", "wall_blur", 24, 0), top(8));
        LinearLayout wp = panel(INK);
        addRow(wp, false, 1, "Add pictures", null, v -> pickWalls());
        addRow(wp, false, 2, "Shuffle wallpapers", wshuf ? "On" : "Off", v -> {
            sp.edit().putBoolean("wall_shuf", !sp.getBoolean("wall_shuf", false)).apply(); wallKey = null; confirmH(); rebuildHome(); buildSettings();
        });
        addRow(wp, true, 3, "No wallpaper", null, v -> {
            sp.edit().putInt("wall_sel", -1).putBoolean("wall_shuf", false).apply(); wallKey = null; toast("Wallpaper off"); rebuildHome(); buildSettings();
        });
        col.addView(wp, top(12));

        col.addView(sectionLabel("Hero card photo"));
        LinearLayout ph = new LinearLayout(this); ph.setBackground(box(0, INK));
        TextView choose = L("Choose photo", 14, on(acc)); choose.setBackgroundColor(acc);
        TextView reset = L("Reset", 14, INK);
        TextView[] pc = {choose, reset};
        choose.setOnClickListener(v -> pickBg());
        reset.setOnClickListener(v -> { new File(getFilesDir(), "home_bg.jpg").delete(); homeBg = null; Tab ct = cur(); if (ct != null && ct.url.isEmpty()) buildHome(ct.priv, false); toast("Photo reset"); });
        for (int i = 0; i < 2; i++) { pc[i].setGravity(Gravity.CENTER); press(pc[i]); ph.addView(pc[i], new LinearLayout.LayoutParams(0, dp(52), 1f)); if (i == 0) ph.addView(vline(INK), vlp()); }
        col.addView(ph, new LinearLayout.LayoutParams(-1, -2));

        col.addView(sectionLabel("Accent colour"));
        LinearLayout sw = new LinearLayout(this);
        for (int i = 0; i < ACC.length; i++) {
            final int k = i; boolean sel = i == sp.getInt("acc", 0);
            TextView c = D(sel ? "\u2713" : "", 22, on(ACC[i])); c.setGravity(Gravity.CENTER);
            GradientDrawable g = box(ACC[i], INK); if (sel) g.setStroke(LW * 3, INK);
            c.setBackground(g); press(c);
            c.setOnClickListener(v -> { sp.edit().putInt("acc", k).apply(); refreshUI(); Tab ct = cur(); if (ct != null && ct.url.isEmpty()) buildHome(ct.priv, false); buildSettings(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f); lp.rightMargin = i < ACC.length - 1 ? dp(6) : 0;
            sw.addView(c, lp);
        }
        col.addView(sw, new LinearLayout.LayoutParams(-1, -2));

        col.addView(sectionLabel("Account"));
        LinearLayout acp = panel(INK);
        addRow(acp, false, 1, "Google account", googleSignedIn() ? "Signed in" : "Sign in", v -> googleAccount());
        addRow(acp, true, 2, "AI keys", null, v -> buildAiKeys());
        col.addView(acp, new LinearLayout.LayoutParams(-1, -2));

        col.addView(sectionLabel("Behaviour"));
        LinearLayout p = panel(INK);
        addRow(p, false, 1, "Touch ripple", sp.getBoolean("ripple", true) ? "On" : "Off", v -> toggle("ripple"));
        addRow(p, false, 2, "Motion blur", sp.getBoolean("mblur", true) ? "On" : "Off", v -> toggle("mblur"));
        addRow(p, false, 3, "Hide bar on pages", sp.getBoolean("autohide", true) ? "On" : "Off", v -> toggle("autohide"));
        addRow(p, false, 4, "Background play", sp.getBoolean("bgplay", true) ? "On" : "Off", v -> toggle("bgplay"));
        addRow(p, false, 5, "Theme", oled ? "OLED" : "Light", v -> { sp.edit().putBoolean("oled", !oled).apply(); recreate(); });
        addRow(p, false, 6, "Dark pages", sp.getBoolean("darkpages", true) ? "On" : "Off", v -> toggle("darkpages"));
        addRow(p, false, 7, "Haptics", sp.getBoolean("haptic", true) ? "On" : "Off", v -> toggle("haptic"));
        addRow(p, false, 8, "Search engine", ENGINES[eng()][0], v -> { sp.edit().putInt("se", (eng() + 1) % ENGINES.length).apply(); buildSettings(); });
        addRow(p, false, 9, "Float on restore", new String[]{"Ask", "Always", "Never"}[sp.getInt("min_mode", 0) % 3], v -> { sp.edit().putInt("min_mode", (sp.getInt("min_mode", 0) + 1) % 3).apply(); buildSettings(); });
        addRow(p, false, 10, "Auto update check", sp.getBoolean("autoupd", true) ? "On" : "Off", v -> toggle("autoupd"));
        addRow(p, true, 11, "Check for updates", "v" + appVersion(), v -> checkUpdate(true));
        col.addView(p, new LinearLayout.LayoutParams(-1, -2));
        sv.addView(col); attachMotion(sv, col);
        library.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    void toggle(String key) {
        confirmH();
        sp.edit().putBoolean(key, !sp.getBoolean(key, true)).apply();
        rippleOn = sp.getBoolean("ripple", true); blurOn = sp.getBoolean("mblur", true); autoHide = sp.getBoolean("autohide", true); hapticOn = sp.getBoolean("haptic", true);
        confirmH();
        SWebView.keep = sp.getBoolean("bgplay", true);
        if (!autoHide) { hnd.removeCallbacks(hideRun); revealDock(); }
        buildSettings();
    }

    // ================= notes (swipe right to open) =================
    long pl(String v) { try { return Long.parseLong(v); } catch (Exception e) { return 0; } }

    String when(String ts) { long t = pl(ts); return t == 0 ? "" : new SimpleDateFormat("MMM d \u00B7 h:mm a", Locale.getDefault()).format(new Date(t)); }

    void openNotes() { endEdit(); prepareNotes(); setNotes(true); }

    void prepareNotes() {
        int W = getResources().getDisplayMetrics().widthPixels;
        buildNotesList(); notesPanel.setVisibility(View.VISIBLE); if (!notesOpen) notesPanel.setTranslationX(-W);
    }

    void setNotes(boolean open) {
        int W = getResources().getDisplayMetrics().widthPixels;
        notesPanel.animate().cancel();
        if (open) {
            notesOpen = true; notesPanel.setVisibility(View.VISIBLE);
            notesPanel.animate().setStartDelay(0).translationX(0).setDuration(280).setInterpolator(new DecelerateInterpolator(2f)).start();
            confirmH();
        } else {
            saveCurrentNote(); notesOpen = false;
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(notesPanel.getWindowToken(), 0);
            notesPanel.animate().setStartDelay(0).translationX(-W).setDuration(240).setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> { if (!notesOpen) { notesPanel.setVisibility(View.GONE); noteEditing = false; } }).start();
            softTick();
        }
    }

    void buildNotesList() {
        notesPanel.removeAllViews(); noteEditing = false; curNote = null; noteTitle = null; noteBody = null;
        notesPanel.setBackgroundColor(PAPER);
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), topInset + dp(10), dp(16), dp(60));
        col.addView(header(INK, "Close", v -> setNotes(false)));
        final ArrayList<String[]> l = read("notes");
        Collections.sort(l, (x, y) -> {
            int px = "1".equals(x[4]) ? 1 : 0, py = "1".equals(y[4]) ? 1 : 0;
            return px != py ? py - px : Long.compare(pl(y[5]), pl(x[5]));
        });
        LinearLayout tr = new LinearLayout(this); tr.setGravity(Gravity.BOTTOM);
        tr.addView(D("Notes", 72, INK), new LinearLayout.LayoutParams(0, -2, 1f));
        tr.addView(D(n2(l.size()), 72, accentColor()));
        col.addView(tr, top(10));
        LinearLayout ar = new LinearLayout(this); ar.setBackground(box(0, INK));
        TextView nw = L("+ New note", 15, PAPER), pg = L("+ This page", 15, INK);
        nw.setBackgroundColor(INK); nw.setGravity(Gravity.CENTER); pg.setGravity(Gravity.CENTER);
        nw.setOnClickListener(v -> buildNoteEditor(null));
        pg.setOnClickListener(v -> {
            Tab t = cur(); if (t == null || t.url.isEmpty()) { toast("Open a page first"); return; }
            String id = String.valueOf(System.currentTimeMillis());
            ArrayList<String[]> all = read("notes");
            all.add(0, new String[]{id, t.title, t.url, "0", "0", id}); write("notes", all);
            confirmH(); buildNotesList();
        });
        press(nw); press(pg);
        ar.addView(nw, new LinearLayout.LayoutParams(0, dp(52), 1f)); ar.addView(vline(INK), vlp());
        ar.addView(pg, new LinearLayout.LayoutParams(0, dp(52), 1f));
        col.addView(ar, top(14));
        if (l.isEmpty()) {
            TextView e = D("No notes\nyet", 40, INK); e.setAlpha(.35f); col.addView(e, top(36));
            TextView h = L("Swipe right anywhere to open notes", 12, INK); h.setAlpha(.6f); col.addView(h, top(10));
        }
        int idx = 0;
        for (final String[] n : l) {
            int fill = NCOL[(int) (pl(n[3]) % NCOL.length)], tc = on(fill);
            LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(14), dp(12), dp(14), dp(14));
            c.setBackground(box(fill, (fill == WHITE || fill == PAPER) ? INK : 0));
            LinearLayout tp = new LinearLayout(this);
            tp.addView(L(when(n[5]), 11, tc), new LinearLayout.LayoutParams(0, -2, 1f));
            if ("1".equals(n[4])) tp.addView(L("\u2605 Pinned", 11, tc));
            c.addView(tp);
            TextView ti = D(n[1].isEmpty() ? "Untitled" : n[1], 28, tc); ti.setSingleLine(true); ti.setEllipsize(TextUtils.TruncateAt.END);
            c.addView(ti, top(6));
            if (!n[2].isEmpty()) {
                TextView bd = L(n[2].replace("\n", " "), 13, tc); bd.setMaxLines(2); bd.setEllipsize(TextUtils.TruncateAt.END); bd.setAlpha(.85f);
                c.addView(bd, top(6));
            }
            c.setOnClickListener(v -> buildNoteEditor(n)); press(c);
            c.setOnLongClickListener(v -> {
                hap(HapticFeedbackConstants.LONG_PRESS);
                dlg().setTitle("Delete note?").setPositiveButton("Delete", (d, w) -> {
                    ArrayList<String[]> all = read("notes");
                    for (int i = all.size() - 1; i >= 0; i--) if (all.get(i)[0].equals(n[0])) all.remove(i);
                    write("notes", all); rejectH(); buildNotesList();
                }).setNegativeButton("Cancel", null).show();
                return true;
            });
            col.addView(c, top(10)); slide(c, idx++);
        }
        sv.addView(col); attachMotion(sv, col);
        notesPanel.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    void buildNoteEditor(String[] note) {
        final boolean isNew = note == null;
        final String[] n = isNew ? new String[]{String.valueOf(System.currentTimeMillis()), "", "", "0", "0", String.valueOf(System.currentTimeMillis())} : note;
        curNote = n; noteEditing = true;
        final int fill = NCOL[(int) (pl(n[3]) % NCOL.length)], tc = on(fill);
        notesPanel.removeAllViews(); notesPanel.setBackgroundColor(fill);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), topInset + dp(10), dp(16), 0);
        col.addView(header(tc, "Done", v -> { saveCurrentNote(); buildNotesList(); }));
        TextWatcher tw = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { hnd.removeCallbacks(noteSave); hnd.postDelayed(noteSave, 600); }
            @Override public void afterTextChanged(Editable s) { }
        };
        noteTitle = new EditText(this); noteTitle.setBackground(null); noteTitle.setPadding(0, 0, 0, 0);
        noteTitle.setTypeface(TF_D); noteTitle.setTextScaleX(dScale); noteTitle.setTextSize(44); noteTitle.setTextColor(tc);
        noteTitle.setIncludeFontPadding(false); noteTitle.setLineSpacing(0, .86f); noteTitle.setAllCaps(true);
        noteTitle.setHint("TITLE"); noteTitle.setHintTextColor((tc & 0xFFFFFF) | 0x55000000); noteTitle.setText(n[1]);
        noteTitle.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        noteTitle.addTextChangedListener(tw);
        col.addView(noteTitle, top(14));
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-1, LW); ll.topMargin = dp(12); col.addView(vline(tc), ll);
        L(when(n[5]), 11, tc);
        TextView dt = L(when(n[5]) + ("1".equals(n[4]) ? "  \u2605 pinned" : ""), 11, tc); dt.setAlpha(.7f); col.addView(dt, top(8));
        noteBody = new EditText(this); noteBody.setBackground(null); noteBody.setPadding(0, dp(10), 0, dp(10)); noteBody.setGravity(Gravity.TOP);
        noteBody.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL)); noteBody.setTextSize(19); noteBody.setTextColor(tc);
        noteBody.setHint("WRITE SOMETHING\u2026"); noteBody.setHintTextColor((tc & 0xFFFFFF) | 0x55000000); noteBody.setText(n[2]);
        noteBody.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        noteBody.addTextChangedListener(tw);
        col.addView(noteBody, new LinearLayout.LayoutParams(-1, 0, 1f));
        col.addView(vline(tc), hlp());
        LinearLayout bar2 = new LinearLayout(this);
        String[] labels = {"1".equals(n[4]) ? "Unpin" : "Pin", "Colour", "Share", "Delete"};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            TextView c = L(labels[i], 13, tc); c.setGravity(Gravity.CENTER); press(c);
            c.setOnClickListener(v -> {
                saveCurrentNote();
                if (k == 0) { n[4] = "1".equals(n[4]) ? "0" : "1"; confirmH(); persistNote(n); buildNoteEditor(n); }
                else if (k == 1) { n[3] = String.valueOf((pl(n[3]) + 1) % NCOL.length); persistNote(n); buildNoteEditor(n); }
                else if (k == 2) {
                    Intent s = new Intent(Intent.ACTION_SEND); s.setType("text/plain");
                    s.putExtra(Intent.EXTRA_TEXT, (n[1].isEmpty() ? "" : n[1] + "\n\n") + n[2]); startActivity(Intent.createChooser(s, "Share note"));
                } else {
                    dlg().setTitle("Delete note?").setPositiveButton("Delete", (d, w) -> {
                        ArrayList<String[]> all = read("notes");
                        for (int j = all.size() - 1; j >= 0; j--) if (all.get(j)[0].equals(n[0])) all.remove(j);
                        write("notes", all); rejectH(); buildNotesList();
                    }).setNegativeButton("Cancel", null).show();
                }
            });
            bar2.addView(c, new LinearLayout.LayoutParams(0, dp(56), 1f));
            if (i < 3) bar2.addView(vline(tc), vlp());
        }
        col.addView(bar2);
        notesPanel.addView(col, new FrameLayout.LayoutParams(-1, -1));
        if (isNew) {
            noteTitle.requestFocus();
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(noteTitle, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    void persistNote(String[] n) {
        ArrayList<String[]> l = read("notes"); int at = -1;
        for (int i = 0; i < l.size(); i++) if (l.get(i)[0].equals(n[0])) at = i;
        if (n[1].isEmpty() && n[2].trim().isEmpty()) { if (at >= 0) { l.remove(at); write("notes", l); } return; }
        if (at >= 0) l.set(at, n); else l.add(0, n);
        write("notes", l);
    }

    void saveCurrentNote() {
        hnd.removeCallbacks(noteSave);
        if (!noteEditing || curNote == null || noteTitle == null || noteBody == null) return;
        String t = noteTitle.getText().toString().trim(), b = noteBody.getText().toString();
        if (!t.equals(curNote[1]) || !b.equals(curNote[2])) curNote[5] = String.valueOf(System.currentTimeMillis());
        curNote[1] = t; curNote[2] = b; persistNote(curNote);
    }

    // ================= AI assistants (ChatGPT, Claude, Gemini, Grok, Perplexity) =================
    // {name, id, default model, endpoint, web url, key page}
    static final String[][] AIS = {
        {"ChatGPT", "openai", "gpt-4o-mini", "https://api.openai.com/v1/chat/completions", "https://chatgpt.com/?q=", "https://platform.openai.com/api-keys"},
        {"Claude", "anthropic", "claude-sonnet-5-5", "https://api.anthropic.com/v1/messages", "https://claude.ai/new?q=", "https://console.anthropic.com/settings/keys"},
        {"Gemini", "gemini", "gemini-2.5-flash", "https://generativelanguage.googleapis.com/v1beta/models/", "https://gemini.google.com/app", "https://aistudio.google.com/apikey"},
        {"Grok", "xai", "grok-3-mini", "https://api.x.ai/v1/chat/completions", "https://grok.com/?q=", "https://console.x.ai"},
        {"Perplexity", "pplx", "sonar", "https://api.perplexity.ai/chat/completions", "https://www.perplexity.ai/search?q=", "https://www.perplexity.ai/settings/api"}};
    final ArrayList<String[]> aiLog = new ArrayList<>();
    LinearLayout aiList; ScrollView aiScroll; EditText aiInput;
    boolean aiBusy, aiUsePage;

    int aiSel() { return Math.min(AIS.length - 1, Math.max(0, sp.getInt("ai_sel", 0))); }
    boolean aiHasKey(int i) { return !dec(sp.getString("ai_key_" + AIS[i][1], "")).isEmpty(); }

    // API keys are encrypted with a key that lives in the Android Keystore
    String enc(String plain) {
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
            if (!ks.containsAlias("storm_ai")) {
                KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
                kg.init(new KeyGenParameterSpec.Builder("storm_ai", KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
                kg.generateKey();
            }
            SecretKey k = (SecretKey) ks.getKey("storm_ai", null);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, k);
            byte[] iv = c.getIV(), ct = c.doFinal(plain.getBytes("UTF-8"));
            return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ct, Base64.NO_WRAP);
        } catch (Exception e) { return "p:" + plain; }
    }

    String dec(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            if (s.startsWith("p:")) return s.substring(2);
            String[] p = s.split(":");
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
            SecretKey k = (SecretKey) ks.getKey("storm_ai", null);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(128, Base64.decode(p[0], Base64.NO_WRAP)));
            return new String(c.doFinal(Base64.decode(p[1], Base64.NO_WRAP)), "UTF-8");
        } catch (Exception e) { return ""; }
    }

    String aiRequest(int pi, ArrayList<String[]> hist) throws Exception {
        String id = AIS[pi][1]; String key = dec(sp.getString("ai_key_" + id, ""));
        if (key.isEmpty()) throw new Exception("No API key for " + AIS[pi][0]);
        String model = sp.getString("ai_model_" + id, AIS[pi][2]); if (model.trim().isEmpty()) model = AIS[pi][2];
        String sys = "You are Storm AI, a concise assistant built into a mobile web browser. Answer clearly in plain text; avoid heavy markdown.";
        String url = AIS[pi][3]; JSONObject body = new JSONObject(); JSONArray ms = new JSONArray();
        HttpURLConnection c;
        if (id.equals("anthropic")) {
            body.put("model", model.trim()); body.put("max_tokens", 1500); body.put("system", sys);
            for (String[] h : hist) { JSONObject m = new JSONObject(); m.put("role", h[0]); m.put("content", h[1]); ms.put(m); }
            body.put("messages", ms);
        } else if (id.equals("gemini")) {
            url = url + model.trim() + ":generateContent";
            body.put("systemInstruction", new JSONObject().put("parts", new JSONArray().put(new JSONObject().put("text", sys))));
            for (String[] h : hist) {
                JSONObject m = new JSONObject(); m.put("role", h[0].equals("assistant") ? "model" : "user");
                m.put("parts", new JSONArray().put(new JSONObject().put("text", h[1]))); ms.put(m);
            }
            body.put("contents", ms);
        } else {
            body.put("model", model.trim());
            ms.put(new JSONObject().put("role", "system").put("content", sys));
            for (String[] h : hist) { JSONObject m = new JSONObject(); m.put("role", h[0]); m.put("content", h[1]); ms.put(m); }
            body.put("messages", ms);
        }
        c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestMethod("POST"); c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(90000);
        c.setRequestProperty("Content-Type", "application/json");
        if (id.equals("anthropic")) { c.setRequestProperty("x-api-key", key.trim()); c.setRequestProperty("anthropic-version", "2023-06-01"); }
        else if (id.equals("gemini")) c.setRequestProperty("x-goog-api-key", key.trim());
        else c.setRequestProperty("Authorization", "Bearer " + key.trim());
        c.getOutputStream().write(body.toString().getBytes("UTF-8"));
        int code = c.getResponseCode();
        InputStream is = code < 400 ? c.getInputStream() : c.getErrorStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8")); StringBuilder sb = new StringBuilder(); String ln;
        while ((ln = br.readLine()) != null) sb.append(ln); br.close();
        String raw = sb.toString();
        if (code >= 400) {
            String msg = "HTTP " + code;
            try { Object er = new JSONObject(raw).opt("error"); msg = er instanceof JSONObject ? ((JSONObject) er).optString("message", raw) : (er != null ? er.toString() : raw); } catch (Exception x) { }
            throw new Exception(msg);
        }
        JSONObject j = new JSONObject(raw); StringBuilder out = new StringBuilder();
        if (id.equals("anthropic")) {
            JSONArray a = j.getJSONArray("content");
            for (int i = 0; i < a.length(); i++) { JSONObject b = a.getJSONObject(i); if ("text".equals(b.optString("type"))) out.append(b.optString("text")); }
        } else if (id.equals("gemini")) {
            JSONArray parts = j.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts");
            for (int i = 0; i < parts.length(); i++) out.append(parts.getJSONObject(i).optString("text"));
        } else {
            out.append(j.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content"));
            JSONArray cit = j.optJSONArray("citations");
            if (cit != null && cit.length() > 0) { out.append("\n\nSources:"); for (int i = 0; i < cit.length() && i < 6; i++) out.append("\n").append(i + 1).append(". ").append(cit.optString(i)); }
        }
        String r = out.toString().trim();
        return r.isEmpty() ? "(empty reply)" : r;
    }

    void openAi() { endEdit(); buildAi(); if (library.getVisibility() != View.VISIBLE) showOverlay(library); }

    TextView chip(String label, boolean filled, View.OnClickListener l) {
        TextView c = L(label, 13, filled ? PAPER : INK); c.setBackgroundColor(filled ? INK : 0); c.setGravity(Gravity.CENTER);
        c.setPadding(dp(14), dp(12), dp(14), dp(12)); c.setOnClickListener(l); press(c); return c;
    }

    void buildAi() {
        library.removeAllViews(); libMode = "ai"; library.setPadding(0, 0, 0, botInset); library.setBackgroundColor(PAPER);
        final int pi = aiSel();
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16), topInset + dp(10), dp(16), dp(8));
        root.addView(header(INK, "Done", v -> {
            endEdit(); ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(wrapper.getWindowToken(), 0);
            hideOverlay(library);
        }));
        root.addView(D("Ask AI", 56, INK), top(8));

        HorizontalScrollView hs = new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout pr = new LinearLayout(this); pr.setBackground(box(0, INK));
        for (int i = 0; i < AIS.length; i++) {
            final int k = i;
            pr.addView(chip(AIS[i][0] + (aiHasKey(i) ? "" : " \u00B7 no key"), i == pi, v -> { sp.edit().putInt("ai_sel", k).apply(); buildAi(); }));
            if (i < AIS.length - 1) pr.addView(vline(INK), vlp());
        }
        hs.addView(pr); root.addView(hs, top(12));

        HorizontalScrollView qs = new HorizontalScrollView(this); qs.setHorizontalScrollBarEnabled(false);
        LinearLayout qr = new LinearLayout(this); qr.setBackground(box(0, INK));
        String[] ql = {"Summarize page", "Key points", "Explain selection", "Translate page", "Ask on web", "Clear chat", "API keys"};
        for (int i = 0; i < ql.length; i++) {
            final int k = i;
            qr.addView(chip(ql[i], false, v -> {
                if (k == 0) aiPage("Summarize this web page in 5 short bullet points.", false);
                else if (k == 1) aiPage("List the key facts, names, numbers and dates from this page.", false);
                else if (k == 2) aiPage("Explain the following selected text in simple terms.", true);
                else if (k == 3) aiPage("Translate the main content of this page into English, keeping it brief.", false);
                else if (k == 4) aiWeb();
                else if (k == 5) { aiLog.clear(); buildAi(); }
                else buildAiKeys();
            }));
            if (i < ql.length - 1) qr.addView(vline(INK), vlp());
        }
        qs.addView(qr); root.addView(qs, top(8));

        aiScroll = new ScrollView(this); aiScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        aiList = new LinearLayout(this); aiList.setOrientation(LinearLayout.VERTICAL); aiList.setPadding(0, dp(10), 0, dp(10));
        aiScroll.addView(aiList);
        root.addView(aiScroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        if (aiLog.isEmpty()) {
            TextView e = D("Ask\nanything", 44, INK); e.setAlpha(.3f); aiList.addView(e, top(20));
            TextView h = L("Pick a provider above, then type a question or tap a quick action. Add your own API key under API keys.", 12, INK);
            h.setAlpha(.6f); aiList.addView(h, top(10));
        }
        for (String[] m : aiLog) addAiBubble(m[0].equals("user"), m[1], !m[0].equals("user"));

        LinearLayout in = new LinearLayout(this); in.setBackground(box(0, INK)); in.setGravity(Gravity.CENTER_VERTICAL);
        final TextView pg = chip("Page", aiUsePage, null);
        pg.setOnClickListener(v -> { aiUsePage = !aiUsePage; pg.setBackgroundColor(aiUsePage ? INK : 0); pg.setTextColor(aiUsePage ? PAPER : INK); confirmH(); });
        in.addView(pg); in.addView(vline(INK), new LinearLayout.LayoutParams(LW, dp(48)));
        aiInput = new EditText(this); aiInput.setBackground(null); aiInput.setPadding(dp(12), dp(8), dp(8), dp(8)); aiInput.setTypeface(TF_L);
        aiInput.setTextSize(16); aiInput.setTextColor(INK); aiInput.setHint("ASK " + AIS[pi][0].toUpperCase()); aiInput.setHintTextColor(ia(0x66));
        aiInput.setMaxLines(4); aiInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        in.addView(aiInput, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView send = L("Send", 14, PAPER); send.setBackgroundColor(INK); send.setGravity(Gravity.CENTER); send.setOnClickListener(v -> aiSubmit()); press(send);
        in.addView(send, new LinearLayout.LayoutParams(dp(76), dp(52)));
        root.addView(in, top(8));
        library.addView(root, new FrameLayout.LayoutParams(-1, -1));
    }

    void aiSubmit() {
        String q = aiInput == null ? "" : aiInput.getText().toString().trim();
        if (q.isEmpty()) return;
        aiInput.setText("");
        if (aiUsePage) aiPage(q, false); else aiSend(q, q);
    }

    View addAiBubble(boolean user, String text, boolean actions) {
        LinearLayout row = new LinearLayout(this); row.setGravity(user ? Gravity.END : Gravity.START);
        LinearLayout b = new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL);
        int fg = user ? PAPER : INK;
        if (user) b.setBackgroundColor(INK); else b.setBackground(box(0, INK));
        if (!user) { TextView nm = L(AIS[aiSel()][0], 10, INK); nm.setAlpha(.6f); nm.setPadding(dp(12), dp(8), dp(12), 0); b.addView(nm); }
        final TextView t = new TextView(this); t.setText(text); t.setTextSize(16); t.setTextColor(fg);
        t.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL)); t.setTextIsSelectable(true); t.setLineSpacing(0, 1.1f);
        t.setPadding(dp(12), dp(user ? 10 : 6), dp(12), dp(10)); b.addView(t);
        if (actions) {
            b.addView(vline(INK), hlp());
            LinearLayout ar = new LinearLayout(this);
            TextView cp = chip("Copy", false, v -> {
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("ai", t.getText())); toast("Copied"); confirmH();
            });
            TextView sv = chip("Save note", false, v -> {
                String id = String.valueOf(System.currentTimeMillis()); ArrayList<String[]> all = read("notes");
                all.add(0, new String[]{id, "AI \u00B7 " + AIS[aiSel()][0], t.getText().toString(), "0", "0", id}); write("notes", all);
                toast("Saved to Notes"); confirmH();
            });
            cp.setPadding(dp(12), dp(8), dp(12), dp(8)); sv.setPadding(dp(12), dp(8), dp(12), dp(8));
            ar.addView(cp); ar.addView(vline(INK), vlp()); ar.addView(sv); b.addView(ar);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(user ? -2 : -1, -2); lp.topMargin = dp(8);
        if (user) lp.leftMargin = dp(40); else lp.rightMargin = dp(8);
        row.addView(b, lp);
        aiList.addView(row, new LinearLayout.LayoutParams(-1, -2));
        aiScroll.post(() -> aiScroll.fullScroll(View.FOCUS_DOWN));
        return row;
    }

    void aiSend(final String shown, final String full) {
        if (aiBusy) { toast("Still thinking\u2026"); return; }
        final int pi = aiSel();
        if (!aiHasKey(pi)) { toast("Add your " + AIS[pi][0] + " API key first"); buildAiKeys(); return; }
        aiBusy = true;
        if (aiList == null || !"ai".equals(libMode)) { buildAi(); }
        aiList.removeAllViews();
        final String[] entry = {"user", shown, full}; aiLog.add(entry);
        for (String[] m : aiLog) addAiBubble(m[0].equals("user"), m[1], !m[0].equals("user"));
        final View think = addAiBubble(false, "Thinking\u2026", false);
        final ArrayList<String[]> hist = new ArrayList<>();
        int from = Math.max(0, aiLog.size() - 10);
        while (from < aiLog.size() && !aiLog.get(from)[0].equals("user")) from++;
        for (int i = from; i < aiLog.size(); i++) hist.add(new String[]{aiLog.get(i)[0], aiLog.get(i)[2]});
        confirmH();
        io.execute(() -> {
            String out; boolean err = false;
            try { out = aiRequest(pi, hist); } catch (Exception e) { err = true; out = e.getMessage() == null ? "Request failed" : e.getMessage(); }
            final String reply = out; final boolean failed = err;
            runOnUiThread(() -> {
                aiBusy = false;
                if (failed) aiLog.remove(entry); else aiLog.add(new String[]{"assistant", reply, reply});
                if ("ai".equals(libMode) && aiList != null) {
                    aiList.removeView(think);
                    if (failed) addAiBubble(false, "\u26A0 " + reply, false);
                    else addAiBubble(false, reply, true);
                }
                if (failed) rejectH(); else softTick();
            });
        });
    }

    void aiPage(final String instr, final boolean selection) {
        Tab t = cur();
        if (t == null || t.url.isEmpty() || t.web == null) { toast("Open a web page first"); return; }
        t.web.evaluateJavascript("(function(){return JSON.stringify({s:String(window.getSelection()),t:document.title,u:location.href,b:(document.body?document.body.innerText:'').slice(0,12000)});})()", v -> {
            try {
                String js = (String) new JSONTokener(v).nextValue(); JSONObject o = new JSONObject(js);
                String sel = o.optString("s");
                if (selection && sel.trim().isEmpty()) { toast("Select some text on the page first, then try again"); return; }
                String text = selection ? sel : o.optString("b");
                aiSend(instr, instr + "\n\nPage: " + o.optString("t") + "\n" + o.optString("u") + "\n\n" + text);
            } catch (Exception e) { toast("Couldn't read this page"); }
        });
    }

    // free route: open the provider's own website with the question
    void aiWeb() {
        int pi = aiSel(); String q = aiInput == null ? "" : aiInput.getText().toString().trim();
        if (q.isEmpty() && !aiLog.isEmpty()) for (String[] m : aiLog) if (m[0].equals("user")) q = m[1];
        if (!q.isEmpty()) ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("prompt", q));
        String base = AIS[pi][4];
        openInTab(base.endsWith("=") && !q.isEmpty() ? base + Uri.encode(q) : (base.endsWith("=") ? base.substring(0, base.indexOf('?')) : base));
        if (!q.isEmpty()) toast("Prompt copied \u2014 paste it if the site doesn't fill it in");
    }

    void buildAiKeys() {
        library.removeAllViews(); libMode = "aikeys"; library.setPadding(0, 0, 0, 0); library.setBackgroundColor(PAPER);
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), topInset + dp(10), dp(16), botInset + dp(40));
        col.addView(header(INK, "Back", v -> buildAi()));
        col.addView(D("AI keys", 64, INK), top(10));
        LinearLayout p = panel(INK);
        for (int i = 0; i < AIS.length; i++) { final int k = i; addRow(p, i == AIS.length - 1, i + 1, AIS[i][0], aiHasKey(i) ? "Key set" : "No key", v -> keyDialog(k)); }
        col.addView(p, top(14));
        TextView help = L("Storm talks directly to each provider with your own API key \u2014 nothing goes through our servers. Keys are encrypted on this phone. Each provider bills usage to your account. Tap a provider to add or change its key and model.", 12, INK);
        help.setAlpha(.75f); help.setLineSpacing(0, 1.15f); col.addView(help, top(18));
        sv.addView(col); library.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    void keyDialog(final int i) {
        final String id = AIS[i][1];
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(22), dp(10), dp(22), 0);
        final EditText key = new EditText(this); key.setHint(aiHasKey(i) ? "API key (leave blank to keep)" : "API key");
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); key.setSingleLine(true);
        final EditText model = new EditText(this); model.setHint("Model"); model.setSingleLine(true);
        model.setText(sp.getString("ai_model_" + id, AIS[i][2]));
        box.addView(key); box.addView(model);
        dlg().setTitle(AIS[i][0]).setView(box)
            .setPositiveButton("Save", (d, w) -> {
                String k = key.getText().toString().trim();
                SharedPreferences.Editor ed = sp.edit();
                if (!k.isEmpty()) ed.putString("ai_key_" + id, enc(k));
                ed.putString("ai_model_" + id, model.getText().toString().trim()).apply();
                toast("Saved"); buildAiKeys();
            })
            .setNegativeButton("Remove key", (d, w) -> { sp.edit().remove("ai_key_" + id).apply(); toast("Key removed"); buildAiKeys(); })
            .setNeutralButton("Get key", (d, w) -> openInTab(AIS[i][5])).show();
    }

    // ================= VPN (WireGuard, bring your own server / config) =================
    String vpnConfig() { return sp.getString("vpn_conf", ""); }
    boolean hasVpn() { return !vpnConfig().isEmpty(); }

    String vpnHost() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?im)^\\s*Endpoint\\s*=\\s*(\\[[^\\]]+\\]|[^\\s:]+)").matcher(vpnConfig());
        return m.find() ? m.group(1) : "";
    }

    String hms(long ms) { long s = Math.max(0, ms / 1000); return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60); }

    void vpnInit() {
        if (vpnBackend == null) {
            vpnBackend = new GoBackend(getApplicationContext());
            vpnTunnel = new Tunnel() {
                @Override public String getName() { return "storm"; }
                @Override public void onStateChange(Tunnel.State s) { }
            };
        }
    }

    void openVpnScreen() { endEdit(); buildVpn(); if (library.getVisibility() != View.VISIBLE) showOverlay(library); }

    void vpnToggle() {
        if (vpnBusy) return;
        if (!hasVpn()) { toast("Add a WireGuard config first"); openVpnScreen(); return; }
        if (vpnUp) { vpnSet(false); return; }
        Intent i = android.net.VpnService.prepare(this);
        if (i != null) { startActivityForResult(i, 88); return; }
        vpnSet(true);
    }

    void vpnSet(final boolean up) {
        vpnBusy = true; vpnChanged(); toast(up ? "Connecting\u2026" : "Disconnecting\u2026");
        io.execute(() -> {
            try {
                vpnInit();
                if (up) {
                    Config cfg = Config.parse(new ByteArrayInputStream(vpnConfig().getBytes("UTF-8")));
                    vpnBackend.setState(vpnTunnel, Tunnel.State.UP, cfg);
                } else vpnBackend.setState(vpnTunnel, Tunnel.State.DOWN, null);
                runOnUiThread(() -> { vpnUp = up; vpnSince = up ? System.currentTimeMillis() : 0; vpnBusy = false; confirmH(); vpnChanged(); });
            } catch (Exception e) {
                final String m = e.getMessage();
                runOnUiThread(() -> { vpnBusy = false; rejectH(); toast("VPN failed: " + (m == null ? "check your config" : m)); vpnChanged(); });
            }
        });
    }

    void vpnChanged() {
        Tab t = cur();
        if (t != null) { setUrlText(t.url); if (t.url.isEmpty()) buildHome(t.priv, false); }
        if (library.getVisibility() == View.VISIBLE && "vpn".equals(libMode)) buildVpn();
    }

    boolean validWg(String txt) {
        try { Config.parse(new ByteArrayInputStream(txt.getBytes("UTF-8"))); return true; } catch (Exception e) { return false; }
    }

    void saveVpnConfig(final String txt) {
        io.execute(() -> {
            if (!validWg(txt)) { runOnUiThread(() -> { rejectH(); toast("That isn't a valid WireGuard config"); }); return; }
            sp.edit().putString("vpn_conf", txt).apply();
            runOnUiThread(() -> { toast("Config saved"); confirmH(); vpnChanged(); });
        });
    }

    void importVpn(final Uri u) {
        io.execute(() -> {
            try {
                InputStream in = getContentResolver().openInputStream(u); java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                byte[] b = new byte[4096]; int n; while ((n = in.read(b)) > 0 && bo.size() < 65536) bo.write(b, 0, n); in.close();
                saveVpnConfig(bo.toString("UTF-8"));
            } catch (Exception e) { runOnUiThread(() -> toast("Couldn't read that file")); }
        });
    }

    void pasteVpn() {
        final EditText et = new EditText(this); et.setHint("[Interface]\nPrivateKey = \u2026\nAddress = \u2026\n\n[Peer]\n\u2026");
        et.setMinLines(8); et.setGravity(Gravity.TOP); et.setTextSize(13);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        dlg().setTitle("Paste WireGuard config").setView(et)
            .setPositiveButton("Save", (d, w) -> saveVpnConfig(et.getText().toString()))
            .setNegativeButton("Cancel", null).show();
    }

    final Runnable vpnTick = new Runnable() {
        @Override public void run() {
            if (vpnUp && vpnSessTv != null && "vpn".equals(libMode) && library.getVisibility() == View.VISIBLE) {
                vpnSessTv.setText(hms(System.currentTimeMillis() - vpnSince)); hnd.postDelayed(this, 1000);
            }
        }
    };

    void buildVpn() {
        library.removeAllViews(); libMode = "vpn"; library.setPadding(0, 0, 0, 0); library.setBackgroundColor(PAPER);
        final int acc = accentColor();
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(16), topInset + dp(10), dp(16), botInset + dp(40));
        col.addView(header(INK, "Done", v -> hideOverlay(library)));
        col.addView(D("VPN", 80, INK), top(10));

        int bf = vpnUp ? acc : INK, bt = vpnUp ? on(acc) : PAPER;
        LinearLayout big = new LinearLayout(this); big.setBackgroundColor(bf); big.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout bl = new LinearLayout(this); bl.setOrientation(LinearLayout.VERTICAL); bl.setPadding(dp(16), dp(12), dp(8), dp(12));
        bl.addView(L(vpnUp ? "Your traffic is encrypted" : "Tap to connect", 11, bt));
        bl.addView(D(vpnBusy ? "Working" : (vpnUp ? "Connected" : "Disconnected"), 34, bt), top(4));
        big.addView(bl, new LinearLayout.LayoutParams(0, -2, 1f));
        big.addView(vline(bt), new LinearLayout.LayoutParams(LW, dp(64)));
        FrameLayout box = new FrameLayout(this); box.setBackgroundColor(bt);
        TextView on = D(vpnUp ? "On" : "Off", 40, bf); on.setGravity(Gravity.CENTER); box.addView(on, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams bx = new LinearLayout.LayoutParams(dp(100), dp(60)); bx.setMargins(dp(14), 0, dp(14), 0);
        big.addView(box, bx);
        big.setOnClickListener(v -> vpnToggle()); press(big);
        col.addView(big, top(14));

        LinearLayout grid = new LinearLayout(this); grid.setBackground(box(0, INK));
        vpnSessTv = D(vpnUp ? hms(System.currentTimeMillis() - vpnSince) : "00:00:00", 22, INK);
        String host = vpnHost();
        TextView srv = D(host.isEmpty() ? "None" : host, 18, INK); srv.setSingleLine(true); srv.setEllipsize(TextUtils.TruncateAt.END);
        TextView ip = D(vpnUp ? "Hidden" : "Visible", 22, INK);
        String[] lb = {"Session", "Server", "IP"}; View[] vs = {vpnSessTv, srv, ip};
        for (int i = 0; i < 3; i++) {
            LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL); c.setPadding(dp(12), dp(10), dp(8), dp(12));
            c.addView(L(lb[i], 10, INK)); c.addView(vs[i], top(6));
            grid.addView(c, new LinearLayout.LayoutParams(0, -2, 1f)); if (i < 2) grid.addView(vline(INK), vlp());
        }
        col.addView(grid, top(14));

        col.addView(sectionLabel("Config"));
        LinearLayout p = panel(INK);
        addRow(p, false, 1, "Import .conf file", null, v -> {
            Intent it = new Intent(Intent.ACTION_GET_CONTENT); it.setType("*/*"); it.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(it, "WireGuard config"), 89);
        });
        addRow(p, false, 2, "Paste config", null, v -> pasteVpn());
        addRow(p, true, 3, "Remove config", hasVpn() ? "Saved" : "None", v -> {
            if (!hasVpn()) return;
            if (vpnUp) vpnSet(false);
            sp.edit().remove("vpn_conf").apply(); toast("Config removed"); vpnChanged();
        });
        col.addView(p, new LinearLayout.LayoutParams(-1, -2));

        TextView help = L("Storm builds a real WireGuard VPN from a config you provide: your own server, Cloudflare WARP, Proton VPN or Mullvad. Get a .conf file from your provider, import it here, then tap the black block to connect. Your traffic from every app goes through it.", 12, INK);
        help.setAlpha(.75f); help.setLineSpacing(0, 1.15f); col.addView(help, top(18));
        sv.addView(col); attachMotion(sv, col);
        library.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        hnd.removeCallbacks(vpnTick); if (vpnUp) hnd.postDelayed(vpnTick, 1000);
    }

    // ================= Google account =================
    String chromeUA() {
        if (cua == null) {
            String ua = WebSettings.getDefaultUserAgent(this);
            cua = ua.replace("; wv", "").replace(" Version/4.0", "");
        }
        return cua;
    }

    boolean isGoogleUrl(String u) {
        String h = hostOf(u == null ? "" : u);
        return h.endsWith("google.com") || h.endsWith("youtube.com") || h.endsWith("gstatic.com") || h.endsWith("googleusercontent.com");
    }

    boolean googleSignedIn() {
        try {
            String c = CookieManager.getInstance().getCookie("https://accounts.google.com");
            if (c == null) return false;
            for (String part : c.split(";")) {
                String n = part.trim(); int eq = n.indexOf('='); if (eq > 0) n = n.substring(0, eq);
                if (n.equals("SID") || n.equals("__Secure-1PSID") || n.equals("SAPISID")) return true;
            }
        } catch (Throwable e) { }
        return false;
    }

    void openInTab(String url) {
        if (library.getVisibility() == View.VISIBLE) hideOverlay(library);
        newTab(url, false);
    }

    void googleAccount() {
        if (googleSignedIn()) {
            dlg().setTitle("Google account").setItems(new String[]{"Open my account", "Sign out"}, (d, w) -> {
                if (w == 0) openInTab("https://myaccount.google.com/");
                else openInTab("https://accounts.google.com/Logout?continue=https://www.google.com/");
            }).setNegativeButton("Close", null).show();
        } else {
            openInTab("https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fwww.google.com%2F&hl=en");
        }
    }

    // ================= in-app updates (GitHub Releases) =================
    static final String REPO = "faliqfaizullah-cloud/storm-browser";
    volatile boolean dlCancel;

    String appVersion() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; } catch (Exception e) { return "0"; }
    }

    int[] vparts(String v) {
        String[] p = (v == null ? "" : v).replaceAll("[^0-9.]", "").split("\\.");
        int[] r = new int[3];
        for (int i = 0; i < 3 && i < p.length; i++) { try { r[i] = Integer.parseInt(p[i]); } catch (Exception e) { } }
        return r;
    }

    boolean newer(String a, String b) {
        int[] x = vparts(a), y = vparts(b);
        for (int i = 0; i < 3; i++) if (x[i] != y[i]) return x[i] > y[i];
        return false;
    }

    void cleanUpdates() {
        File d = new File(getCacheDir(), "updates"); File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) f.delete();
    }

    void checkUpdate(final boolean manual) {
        if (manual) toast("Checking for updates\u2026");
        io.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("https://api.github.com/repos/" + REPO + "/releases/latest").openConnection();
                c.setRequestProperty("Accept", "application/vnd.github+json"); c.setRequestProperty("User-Agent", "Storm-Browser");
                c.setConnectTimeout(10000); c.setReadTimeout(15000);
                if (c.getResponseCode() != 200) throw new Exception("http " + c.getResponseCode());
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder(); String ln; while ((ln = br.readLine()) != null) sb.append(ln); br.close();
                JSONObject j = new JSONObject(sb.toString());
                final String tag = j.getString("tag_name"), notes = j.optString("body", "");
                String url = null; JSONArray as = j.optJSONArray("assets");
                if (as != null) for (int i = 0; i < as.length(); i++) {
                    JSONObject a = as.getJSONObject(i);
                    if (a.getString("name").endsWith(".apk")) { url = a.getString("browser_download_url"); break; }
                }
                final String apk = url; final boolean isNew = newer(tag, appVersion());
                sp.edit().putLong("lastcheck", System.currentTimeMillis()).apply();
                runOnUiThread(() -> {
                    if (isNew && apk != null) { if (manual || !tag.equals(sp.getString("skipver", ""))) showUpdate(tag, notes, apk, manual); }
                    else if (manual) toast(isNew ? "That release has no APK file" : "You're up to date \u00B7 v" + appVersion());
                });
            } catch (Exception e) { if (manual) runOnUiThread(() -> toast("Couldn't check for updates")); }
        });
    }

    void showUpdate(final String tag, String notes, final String apk, final boolean manual) {
        String msg = "Version " + tag + " is ready (you have v" + appVersion() + ")."
            + (notes.isEmpty() ? "" : "\n\n" + (notes.length() > 400 ? notes.substring(0, 400) + "\u2026" : notes));
        dlg().setTitle("Update available").setMessage(msg)
            .setPositiveButton("Update now", (d, w) -> startDownload(tag, apk))
            .setNegativeButton("Later", (d, w) -> { if (!manual) sp.edit().putString("skipver", tag).apply(); }).show();
    }

    void startDownload(String tag, final String apk) {
        if (Build.VERSION.SDK_INT >= 26 && !getPackageManager().canRequestPackageInstalls()) {
            toast("Allow Storm to install updates, then tap Update again");
            startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
            return;
        }
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(24), dp(16), dp(24), dp(8));
        final TextView tx = new TextView(this); tx.setText("Starting\u2026"); tx.setTextSize(16); box.addView(tx);
        final ProgressBar pb = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); pb.setMax(100); box.addView(pb);
        dlCancel = false;
        final AlertDialog dg = dlg().setTitle("Downloading " + tag).setView(box).setCancelable(false)
            .setNegativeButton("Cancel", (d, w) -> dlCancel = true).create();
        dg.show();
        io.execute(() -> {
            try {
                File dir = new File(getCacheDir(), "updates"); dir.mkdirs();
                final File f = new File(dir, "Storm.apk"); if (f.exists()) f.delete();
                HttpURLConnection c = (HttpURLConnection) new URL(apk).openConnection();
                c.setRequestProperty("User-Agent", "Storm-Browser"); c.setConnectTimeout(15000); c.setReadTimeout(30000);
                c.setInstanceFollowRedirects(true);
                int len = c.getContentLength(); InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(f);
                byte[] buf = new byte[16384]; int n, last = -1; long got = 0;
                while ((n = in.read(buf)) > 0) {
                    if (dlCancel) { out.close(); in.close(); f.delete(); runOnUiThread(dg::dismiss); return; }
                    out.write(buf, 0, n); got += n;
                    if (len > 0) {
                        final int pc = (int) (got * 100 / len);
                        if (pc != last) { last = pc; runOnUiThread(() -> { pb.setProgress(pc); tx.setText(pc + "%"); }); }
                    }
                }
                out.close(); in.close();
                if (f.length() < 100000) throw new Exception("bad file");
                runOnUiThread(() -> { dg.dismiss(); installApk(f); });
            } catch (Exception e) { runOnUiThread(() -> { dg.dismiss(); toast("Download failed"); }); }
        });
    }

    void installApk(File f) {
        try {
            Uri u = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_VIEW); i.setDataAndType(u, "application/vnd.android.package-archive");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) { toast("Couldn't open the installer"); }
    }

    // ================= wallpapers (home page background) =================
    ArrayList<String> wallList() {
        ArrayList<String> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString("walls", "[]"));
            for (int i = 0; i < a.length(); i++) if (new File(getFilesDir(), a.getString(i)).exists()) l.add(a.getString(i));
        } catch (Exception e) { }
        return l;
    }

    void saveWalls(ArrayList<String> l) { sp.edit().putString("walls", new JSONArray(l).toString()).apply(); }

    Bitmap decodeImg(File f, int max) {
        if (!f.exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds = true; BitmapFactory.decodeFile(f.getPath(), o);
        int ss = 1; while (o.outWidth / ss > max || o.outHeight / ss > max) ss *= 2;
        o = new BitmapFactory.Options(); o.inSampleSize = ss;
        return BitmapFactory.decodeFile(f.getPath(), o);
    }

    String wallFor(Tab t) {
        ArrayList<String> l = wallList(); if (l.isEmpty() || t == null || t.priv) return null;
        if (sp.getBoolean("wall_shuf", false) && l.size() > 1) {
            if (t.wi < 0) t.wi = new java.util.Random().nextInt(l.size());
            return l.get(t.wi % l.size());
        }
        int sel = sp.getInt("wall_sel", -1);
        return sel >= 0 && sel < l.size() ? l.get(sel) : null;
    }

    Bitmap wallBitmap(Tab t) {
        String f = wallFor(t); if (f == null) return null;
        int blur = sp.getInt("wall_blur", 0); String key = f + "|" + blur;
        if (key.equals(wallKey) && wallBmp != null) return wallBmp;
        Bitmap b = decodeImg(new File(getFilesDir(), f), 1600); if (b == null) return null;
        if (blur > 0) {
            float k = 1f / (1f + blur * .7f);
            Bitmap sm = Bitmap.createScaledBitmap(b, Math.max(8, (int) (b.getWidth() * k)), Math.max(8, (int) (b.getHeight() * k)), true);
            b = Bitmap.createScaledBitmap(sm, b.getWidth(), b.getHeight(), true);
        }
        wallBmp = b; wallKey = key; return b;
    }

    void pickWalls() {
        Intent it = new Intent(Intent.ACTION_GET_CONTENT); it.setType("image/*"); it.addCategory(Intent.CATEGORY_OPENABLE);
        it.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(it, "Choose wallpapers"), 90);
    }

    void addWalls(ArrayList<Uri> us) {
        if (us.isEmpty()) return;
        final int total = Math.min(us.size(), 12); final AtomicInteger left = new AtomicInteger(total);
        for (int i = 0; i < total; i++) {
            final String name = "wall_" + System.currentTimeMillis() + "_" + i + ".jpg";
            saveImage(us.get(i), name, 2400, () -> {
                ArrayList<String> l = wallList(); l.add(name); saveWalls(l); sp.edit().putInt("wall_sel", l.size() - 1).putBoolean("wall_shuf", false).apply();
                if (left.decrementAndGet() == 0) {
                    wallKey = null; toast(total > 1 ? total + " wallpapers added" : "Wallpaper set"); rebuildHome();
                    if (library.getVisibility() == View.VISIBLE) buildSettings();
                }
            });
        }
    }

    View slider(final String label, final String key, int max, int def) {
        LinearLayout b = new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL);
        final String unit = key.equals("wall_blur") ? " px" : " %";
        final TextView t = L(label + " \u00B7 " + sp.getInt(key, def) + unit, 11, INK); b.addView(t);
        SeekBar sk = new SeekBar(this); sk.setMax(max); sk.setProgress(sp.getInt(key, def));
        sk.setProgressTintList(ColorStateList.valueOf(INK)); sk.setThumbTintList(ColorStateList.valueOf(accentColor()));
        sk.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) { t.setText(label + " \u00B7 " + p + unit); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { sp.edit().putInt(key, s.getProgress()).apply(); wallKey = null; rebuildHome(); }
        });
        b.addView(sk, new LinearLayout.LayoutParams(-1, dp(36)));
        return b;
    }

    // ================= minimize: floating tab bubbles =================
    int firstVisible(int skip) {
        for (int d = 1; d <= tabs.size(); d++) {
            int a = skip - d, b = skip + d;
            if (a >= 0 && a < tabs.size() && !tabs.get(a).min) return a;
            if (b >= 0 && b < tabs.size() && !tabs.get(b).min) return b;
        }
        return -1;
    }

    void ensureVisibleCur() {
        Tab c = cur();
        if (c != null && c.min) {
            int n = firstVisible(cur);
            if (n >= 0) cur = n; else { newTab(null, false); return; }
        }
        showCurrent();
    }

    void minimizeTab(Tab t) {
        if (t == null || t.url.isEmpty()) { toast("Open a page first, then minimize it"); return; }
        int idx = tabs.indexOf(t);
        if (t == cur()) snap(t);
        t.min = true; t.bx = -1;
        if (t == cur()) {
            int n = firstVisible(idx);
            if (n >= 0) select(n); else newTab(null, false);
        }
        confirmH(); refreshBubbles(); refreshUI(); toast("Tab minimized \u2014 tap its bubble to bring it back");
    }

    // bring a floating tab back; optionally float the tab you were on (asks first by default)
    void restoreTab(final Tab t) {
        final Tab c = cur();
        final Runnable show = () -> { t.min = false; select(tabs.indexOf(t)); refreshBubbles(); refreshUI(); };
        if (c == null || c == t || c.url.isEmpty() || c.min) { show.run(); return; }
        int mode = sp.getInt("min_mode", 0);
        if (mode == 2) { show.run(); return; }
        final Runnable both = () -> {
            snap(c); c.min = true; c.bx = -1; t.min = false; cur = tabs.indexOf(t); showCurrent(); refreshBubbles(); refreshUI(); confirmH();
        };
        if (mode == 1) { both.run(); return; }
        String name = c.title == null || c.title.isEmpty() ? hostOf(c.url) : c.title;
        dlg().setTitle("Float your current tab?")
            .setMessage("Minimize \u201C" + (name.length() > 40 ? name.substring(0, 40) + "\u2026" : name) + "\u201D into a floating bubble so you can jump back to it?")
            .setPositiveButton("Float it", (d, w) -> both.run())
            .setNegativeButton("Just switch", (d, w) -> show.run())
            .setNeutralButton("Cancel", null).show();
    }

    boolean bubbleHit(float x, float y) {
        int[] l = new int[2];
        for (int i = 0; i < bubbleLayer.getChildCount(); i++) {
            View b = bubbleLayer.getChildAt(i); b.getLocationOnScreen(l);
            if (x >= l[0] && x <= l[0] + b.getWidth() && y >= l[1] && y <= l[1] + b.getHeight()) return true;
        }
        return false;
    }

    void refreshBubbles() {
        if (bubbleLayer == null) return;
        bubbleLayer.removeAllViews();
        int n = 0;
        for (Tab t : tabs) if (t.min) bubbleLayer.addView(makeBubble(t, n++), new FrameLayout.LayoutParams(dp(62), dp(62), Gravity.TOP | Gravity.START));
    }

    View makeBubble(final Tab t, int index) {
        final int W = getResources().getDisplayMetrics().widthPixels, H = getResources().getDisplayMetrics().heightPixels;
        final int sz = dp(62), fill = t.priv ? PURPLE : accentColor();
        FrameLayout b = new FrameLayout(this); b.setBackground(box(fill, INK)); b.setElevation(dp(10));
        String host = hostOf(t.url); String ltr = host.isEmpty() ? "?" : host.substring(0, 1);
        TextView big = D(ltr, 28, on(fill)); big.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -1); bp.bottomMargin = dp(10); b.addView(big, bp);
        TextView sm = L(host.length() > 9 ? host.substring(0, 9) : host, 8, on(fill)); sm.setGravity(Gravity.CENTER); sm.setSingleLine(true);
        b.addView(sm, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM)); sm.setPadding(0, 0, 0, dp(3));
        TextView x = L("\u2715", 9, on(fill)); x.setGravity(Gravity.CENTER);
        x.setOnClickListener(v -> { rejectH(); int i = tabs.indexOf(t); if (i >= 0) { closeTab(i); refreshBubbles(); } });
        b.addView(x, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.TOP | Gravity.END));
        if (t.bx < 0) { t.bx = W - sz - dp(8); t.by = H * .26f + index * (sz + dp(8)); }
        b.setTranslationX(t.bx); b.setTranslationY(t.by);
        final float[] st = new float[4]; final boolean[] moved = {false};
        b.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) {
                st[0] = e.getRawX(); st[1] = e.getRawY(); st[2] = v.getTranslationX(); st[3] = v.getTranslationY(); moved[0] = false;
                v.animate().setStartDelay(0).scaleX(1.1f).scaleY(1.1f).setDuration(90).start(); return true;
            } else if (a == MotionEvent.ACTION_MOVE) {
                float dx = e.getRawX() - st[0], dy = e.getRawY() - st[1];
                if (!moved[0] && Math.hypot(dx, dy) > dp(8)) moved[0] = true;
                if (moved[0]) {
                    v.setTranslationX(Math.max(0, Math.min(W - sz, st[2] + dx)));
                    v.setTranslationY(Math.max(topInset, Math.min(H - sz - botInset, st[3] + dy)));
                }
                return true;
            } else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                v.animate().setStartDelay(0).scaleX(1f).scaleY(1f).setDuration(120).start();
                if (a == MotionEvent.ACTION_UP && !moved[0]) { tick(); restoreTab(t); }
                else if (moved[0]) {
                    float tx = v.getTranslationX() + sz / 2f < W / 2f ? dp(8) : W - sz - dp(8);
                    v.animate().translationX(tx).setDuration(180).setInterpolator(new DecelerateInterpolator(2f)).start();
                    t.bx = tx; t.by = v.getTranslationY(); softTick();
                }
                return true;
            }
            return false;
        });
        return b;
    }

    // ================= background photo =================
    Bitmap loadBg() {
        if (homeBg == null) { File f = new File(getFilesDir(), "home_bg.jpg"); if (f.exists()) homeBg = BitmapFactory.decodeFile(f.getPath()); }
        return homeBg;
    }

    void pickBg() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Choose photo"), 77);
    }

    @Override protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc == 77 && res == RESULT_OK && d != null && d.getData() != null) saveBg(d.getData());
        if (rc == 90 && res == RESULT_OK && d != null) {
            ArrayList<Uri> us = new ArrayList<>();
            if (d.getClipData() != null) { for (int i = 0; i < d.getClipData().getItemCount(); i++) us.add(d.getClipData().getItemAt(i).getUri()); }
            else if (d.getData() != null) us.add(d.getData());
            addWalls(us);
        }
        if (rc == 88 && res == RESULT_OK) vpnSet(true);
        if (rc == 89 && res == RESULT_OK && d != null && d.getData() != null) importVpn(d.getData());
        if (rc >= 100 && rc < 100 + QUICK.length && res == RESULT_OK && d != null && d.getData() != null) {
            final int k = rc - 100;
            saveImage(d.getData(), "sc_" + k + ".jpg", 900, () -> { scBmp[k] = null; rebuildHome(); toast("Picture updated"); });
        }
    }

    void saveBg(Uri u) {
        saveImage(u, "home_bg.jpg", 1600, () -> { homeBg = null; Tab ct = cur(); if (ct != null && ct.url.isEmpty() && !ct.priv) buildHome(false, false); toast("Photo updated"); });
    }

    void rebuildHome() { Tab ct = cur(); if (ct != null && ct.url.isEmpty()) buildHome(ct.priv, false); }

    Bitmap loadSc(int i) {
        if (scBmp[i] == null) { File f = new File(getFilesDir(), "sc_" + i + ".jpg"); if (f.exists()) scBmp[i] = BitmapFactory.decodeFile(f.getPath()); }
        return scBmp[i];
    }

    void pickSc(int i) {
        Intent it = new Intent(Intent.ACTION_GET_CONTENT); it.setType("image/*"); it.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(it, "Picture for " + QUICK[i][0]), 100 + i);
    }

    void scMenu(final int i) {
        final File f = new File(getFilesDir(), "sc_" + i + ".jpg");
        String[] items = f.exists() ? new String[]{"Change picture", "Remove picture"} : new String[]{"Choose picture"};
        dlg().setTitle(QUICK[i][0]).setItems(items, (d, w) -> {
            if (w == 0) pickSc(i);
            else { f.delete(); scBmp[i] = null; rebuildHome(); toast("Picture removed"); }
        }).show();
    }

    void saveImage(final Uri u, final String name, final int max, final Runnable done) {
        io.execute(() -> {
            try {
                BitmapFactory.Options o = new BitmapFactory.Options(); o.inJustDecodeBounds = true;
                InputStream in = getContentResolver().openInputStream(u); BitmapFactory.decodeStream(in, null, o); in.close();
                int ss = 1; while (o.outWidth / ss > 2000 || o.outHeight / ss > 2000) ss *= 2;
                o = new BitmapFactory.Options(); o.inSampleSize = ss;
                in = getContentResolver().openInputStream(u); Bitmap b = BitmapFactory.decodeStream(in, null, o); in.close();
                int rot = 0;
                try {
                    InputStream in2 = getContentResolver().openInputStream(u);
                    int ori = new ExifInterface(in2).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1); in2.close();
                    rot = ori == 6 ? 90 : ori == 3 ? 180 : ori == 8 ? 270 : 0;
                } catch (Exception e) { }
                if (rot != 0) { Matrix m = new Matrix(); m.postRotate(rot); b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true); }
                float sc = Math.min(1f, (float) max / Math.max(b.getWidth(), b.getHeight()));
                if (sc < 1f) b = Bitmap.createScaledBitmap(b, (int) (b.getWidth() * sc), (int) (b.getHeight() * sc), true);
                FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), name));
                b.compress(Bitmap.CompressFormat.JPEG, 90, fo); fo.close();
                runOnUiThread(done);
            } catch (Exception e) { runOnUiThread(() -> toast("Couldn't load that image")); }
        });
    }

    // ================= opening animation =================
    void playOpening() {
        final int H = getResources().getDisplayMetrics().heightPixels;
        final FrameLayout splash = new FrameLayout(this); splash.setBackgroundColor(PAPER); splash.setClickable(true);
        FrameLayout blk = new FrameLayout(this); blk.setBackgroundColor(ORANGE);
        ImageView ar = new ImageView(this); ar.setImageResource(R.drawable.ic_arrow); ar.setColorFilter(WHITE);
        blk.addView(ar, new FrameLayout.LayoutParams(dp(110), dp(110), Gravity.BOTTOM | Gravity.END));
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(dp(150), dp(150), Gravity.TOP | Gravity.END);
        bp.setMargins(0, topInset + dp(24), dp(20), 0); splash.addView(blk, bp);
        TextView big = D("Storm", 112, INK); big.setTextScaleX(dScale * 1.15f);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_VERTICAL | Gravity.START); gp.leftMargin = dp(20);
        splash.addView(big, gp);
        TextView sub = L("Web browser \u00B7 " + Calendar.getInstance().get(Calendar.YEAR), 13, INK);
        FrameLayout.LayoutParams sp1 = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.START); sp1.setMargins(dp(20), 0, 0, dp(48) + botInset);
        splash.addView(sub, sp1);
        View ld = new View(this); ld.setBackgroundColor(INK); ld.setPivotX(0); ld.setScaleX(0f);
        splash.addView(ld, new FrameLayout.LayoutParams(-1, dp(10), Gravity.BOTTOM));
        wrapper.addView(splash, new FrameLayout.LayoutParams(-1, -1));

        big.setAlpha(0f); big.setTranslationX(-dp(90));
        big.animate().alpha(1f).translationX(0).setDuration(520).setInterpolator(new DecelerateInterpolator(2.5f)).start();
        blk.setTranslationY(-dp(260));
        blk.animate().translationY(0).setStartDelay(120).setDuration(480).setInterpolator(new DecelerateInterpolator(2f)).start();
        ld.animate().scaleX(1f).setDuration(950).setInterpolator(new LinearInterpolator()).start();
        dock.setTranslationY(dp(140));
        splash.postDelayed(() -> {
            splash.animate().translationY(-H).setDuration(520).setInterpolator(new AccelerateInterpolator(1.6f))
                .withEndAction(() -> wrapper.removeView(splash)).start();
            dock.animate().setStartDelay(220).translationY(0).setDuration(420).setInterpolator(new DecelerateInterpolator(2f)).start();
        }, 1050);
    }

    // ================= touch ripple (square) =================
    class Ripples extends View {
        final ArrayList<float[]> xy = new ArrayList<>(); final ArrayList<Long> ts = new ArrayList<>();
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Ripples() { super(MainActivity.this); setClickable(false); setFocusable(false); }
        void spawn(float x, float y) { xy.add(new float[]{x, y}); ts.add(System.nanoTime()); invalidate(); }
        @Override protected void onDraw(Canvas c) {
            long now = System.nanoTime(); int col = accentColor() & 0xFFFFFF;
            for (int i = xy.size() - 1; i >= 0; i--) {
                float k = (now - ts.get(i)) / 520e6f;
                if (k >= 1f) { xy.remove(i); ts.remove(i); continue; }
                float e = 1f - (1f - k) * (1f - k); float r = dp(6) + dp(70) * e;
                float x = xy.get(i)[0], y = xy.get(i)[1];
                p.setStyle(Paint.Style.FILL); p.setColor(col | ((int) (90 * (1f - k)) << 24));
                c.drawRect(x - r, y - r, x + r, y + r, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(LW); p.setColor((INK & 0xFFFFFF) | ((int) (230 * (1f - k)) << 24));
                c.drawRect(x - r, y - r, x + r, y + r, p);
            }
            if (!xy.isEmpty()) postInvalidateOnAnimation();
        }
    }

    // ================= motion blur while scrolling (Android 12+) =================
    void attachMotion(ScrollView sv, final View target) { sv.setOnScrollChangeListener((v, x, y, ox, oy) -> motion(target, y - oy)); }

    void motion(View t, int dy) {
        if (Build.VERSION.SDK_INT < 31 || !blurOn) return;
        float lvl = Math.min(14f, Math.abs(dy) * .30f);
        if (lvl < 1.5f) return;
        if (blurTarget != null && blurTarget != t) clearBlur(blurTarget);
        blurTarget = t; blurLvl = Math.max(lvl, blurLvl * .85f);
        setBlur(t, blurLvl); hnd.removeCallbacks(blurDecay); hnd.postDelayed(blurDecay, 50);
    }

    void setBlur(View t, float r) {
        if (Build.VERSION.SDK_INT < 31) return;
        try { t.setRenderEffect(RenderEffect.createBlurEffect(0.01f, r, Shader.TileMode.CLAMP)); } catch (Throwable e) { }
    }

    void clearBlur(View t) {
        if (Build.VERSION.SDK_INT < 31) return;
        try { t.setRenderEffect(null); } catch (Throwable e) { }
    }

    // ================= background media =================
    class MediaBridge {
        @JavascriptInterface public void state(final boolean playing, final String title) { runOnUiThread(() -> onMedia(playing, title)); }
    }

    final Runnable mediaStop = () -> {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am == null || !am.isMusicActive()) { mediaOn = false; try { stopService(new Intent(this, MediaService.class)); } catch (Throwable e) { } }
    };

    void injectMedia(WebView v) {
        v.evaluateJavascript(MEDIA_JS.replace("%SPOOF%", sp.getBoolean("bgplay", true) ? SPOOF_JS : ""), null);
    }

    void onMedia(boolean playing, String title) {
        if (!sp.getBoolean("bgplay", true)) return;
        if (title != null && !title.isEmpty()) mediaTitle = title;
        hnd.removeCallbacks(mediaStop);
        if (playing) {
            mediaOn = true;
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 5);
            startMedia(mediaTitle, true);
        } else if (mediaOn) { startMedia(mediaTitle, false); hnd.postDelayed(mediaStop, 120000); }
    }

    void startMedia(String title, boolean playing) {
        try {
            Intent i = new Intent(this, MediaService.class); i.putExtra("title", title == null ? "Playing" : title); i.putExtra("playing", playing);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        } catch (Throwable e) { }
    }

    static void cmd(final String c) { final MainActivity a = inst; if (a != null) a.runOnUiThread(() -> a.mediaCmd(c)); }

    void mediaCmd(String c) {
        for (Tab t : tabs) if (t.web != null) t.web.evaluateJavascript("window.__stormCmd&&window.__stormCmd('" + c + "')", null);
        startMedia(mediaTitle, !c.equals("pause"));
    }

    // ================= web =================
    boolean isBlocked(WebResourceRequest r) {
        Uri u = r.getUrl(); String host = u.getHost(); if (host == null) return false;
        for (String h = host.toLowerCase(); ; ) {
            if (ADSET.contains(h)) return true;
            int d = h.indexOf('.'); if (d < 0) break; h = h.substring(d + 1);
        }
        String full = u.toString().toLowerCase();
        for (String p : PATS) if (full.contains(p)) return true;
        return false;
    }

    void countBlocked() { int n = blocked.incrementAndGet(); if (n % 20 == 0) sp.edit().putInt("blk_total", n).apply(); }

    void applyAds(Tab t, WebView w) {
        try {
            if (t.adHandle != null) { t.adHandle.remove(); t.adHandle = null; }
            if (blockAds && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT))
                t.adHandle = WebViewCompat.addDocumentStartJavaScript(w, ADS_JS, new HashSet<>(Collections.singletonList("*")));
        } catch (Throwable e) { }
    }

    void applyAdsAll() { for (Tab t : tabs) if (t.web != null) applyAds(t, t.web); }

    void hideCustom() {
        if (customView == null) return;
        wrapper.removeView(customView); customView = null;
        if (customCb != null) customCb.onCustomViewHidden();
    }

    WebView makeWeb(final Tab t) {
        final WebView w = new SWebView(this);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false); s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(t.desktop ? DESKTOP_UA : chromeUA());
        if (t.priv) { s.setCacheMode(WebSettings.LOAD_NO_CACHE); s.setSaveFormData(false); }
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, false);
        w.setBackgroundColor(oled ? PAPER : WHITE); w.setOverScrollMode(View.OVER_SCROLL_NEVER);
        w.addJavascriptInterface(new MediaBridge(), "StormMedia");
        applyAds(t, w);
        if (Build.VERSION.SDK_INT >= 29 && oled && sp.getBoolean("darkpages", true)) s.setForceDark(WebSettings.FORCE_DARK_ON);
        w.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) { dx0 = e.getX(); dy0 = e.getY(); dt0 = e.getEventTime(); }
            else if (a == MotionEvent.ACTION_UP && t == cur() && Math.abs(e.getX() - dx0) < dp(10)
                && Math.abs(e.getY() - dy0) < dp(10) && e.getEventTime() - dt0 < 350) { /* tapping the page does not reveal the bar */ }
            return false;
        });
        w.setOnScrollChangeListener((v, sx, sy, ox, oy) -> {
            motion(w, sy - oy);
            if (t == cur() && autoHide && barsShown && !editing && Math.abs(sy - oy) > dp(10) && System.currentTimeMillis() - revealAt > 1200) hideBars();
        });
        w.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (blockAds && !r.isForMainFrame() && isBlocked(r)) {
                    countBlocked();
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String sc = r.getUrl().getScheme();
                if ("http".equals(sc) || "https".equals(sc) || "about".equals(sc) || "file".equals(sc)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { toast("No app to open link"); }
                return true;
            }
            @Override public void onPageStarted(WebView v, String url, Bitmap f) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(v, isGoogleUrl(url));
                t.url = url; injectMedia(v); if (blockAds) v.evaluateJavascript(ADS_JS, null);
                if (t == cur()) { bar.setVisibility(View.VISIBLE); setUrlText(url); refreshUI(); if (autoHide) hideSoon(); else showBars(); }
            }
            @Override public void onPageFinished(WebView v, String url) {
                t.title = v.getTitle() == null ? url : v.getTitle(); injectMedia(v); if (blockAds) v.evaluateJavascript(ADS_JS, null);
                if (!t.priv) addHistory(t.title, url);
                if (t == cur()) { bar.setVisibility(View.INVISIBLE); refreshUI(); }
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView v, int p) { if (t == cur()) bar.setProgress(p); }
            @Override public void onReceivedTitle(WebView v, String title) { t.title = title; }
            @Override public void onShowCustomView(View v, CustomViewCallback cb) {
                customView = v; customCb = cb; v.setBackgroundColor(Color.BLACK);
                wrapper.addView(v, new FrameLayout.LayoutParams(-1, -1));
            }
            @Override public void onHideCustomView() { hideCustom(); }
        });
        w.setDownloadListener((url, ua, cd, mime, len) -> {
            try {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
                r.setMimeType(mime); r.addRequestHeader("User-Agent", ua);
                String ck = CookieManager.getInstance().getCookie(url); if (ck != null) r.addRequestHeader("cookie", ck);
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                String fn = URLUtil.guessFileName(url, cd, mime);
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fn);
                ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
                toast("Downloading " + fn);
            } catch (Exception e) { toast("Download failed"); }
        });
        w.setOnLongClickListener(v -> {
            WebView.HitTestResult r = w.getHitTestResult(); int ty = r.getType(); final String ex = r.getExtra();
            if (ex != null) hap(HapticFeedbackConstants.LONG_PRESS);
            if (ex != null && (ty == WebView.HitTestResult.SRC_ANCHOR_TYPE || ty == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                dlg().setTitle(ex).setItems(new String[]{"Open in new tab", "Copy link"}, (d, k) -> {
                    if (k == 0) newTab(ex, t.priv);
                    else ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("url", ex));
                }).show();
                return true;
            }
            return false;
        });
        return w;
    }
}
