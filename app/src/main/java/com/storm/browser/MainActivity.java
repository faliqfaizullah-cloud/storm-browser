package com.storm.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.RadialGradient;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    static final int PINK = 0xFFE86FB5, PURPLE = 0xFFB48CFF, INK = 0xFF111214, WHITE = 0xFFFFFFFF;
    static final int DOCK = 0xB0111214, DOCK_PRIV = 0xB02A1640;
    static final int[] ACC = {0xFFE86FB5, 0xFF5B8CFF, 0xFF3DBE8B, 0xFFFF8A4C, 0xFFB48CFF, 0xFFE5B83B};
    static final int[] PAL = {0xFFE86FB5, 0xFF5B8CFF, 0xFF3DBE8B, 0xFFFF8A4C, 0xFF9B6BFF, 0xFFE5B83B};
    static final String DESKTOP_UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    static final String[][] ENGINES = {
        {"DuckDuckGo", "https://duckduckgo.com/?q="}, {"Google", "https://www.google.com/search?q="},
        {"Brave", "https://search.brave.com/search?q="}, {"Bing", "https://www.bing.com/search?q="}};
    static final String[] ADS = {"doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "google-analytics.com", "googletagmanager.com", "adnxs.com", "taboola.com", "outbrain.com",
        "criteo.com", "scorecardresearch.com", "adsrvr.org", "amazon-adsystem.com", "hotjar.com",
        "moatads.com", "pubmatic.com", "rubiconproject.com"};
    static final String[][] QUICK = {
        {"Google", "https://www.google.com", "#111214", "#FFFFFF"},
        {"YouTube", "https://m.youtube.com", "#E86FB5", "#111214"},
        {"Wikipedia", "https://en.m.wikipedia.org", "#C9F23A", "#111214"},
        {"GitHub", "https://github.com", "#2B2D31", "#FFFFFF"},
        {"Brave Search", "https://search.brave.com", "#FF5A2C", "#FFFFFF"},
        {"Reddit", "https://www.reddit.com", "#DCE6FF", "#111214"}};

    static class Tab {
        WebView web; String title = "New tab", url = ""; boolean priv, desktop; Bitmap thumb;
    }

    SharedPreferences sp;
    FrameLayout wrapper, content, switcher, library, home;
    Glass dock;
    ProgressBar bar;
    FrameLayout pill;
    TextView pillText, pillLeft, pillRight, tabCount, backBtn, fwdBtn;
    EditText urlField;
    LinearLayout toolbar;
    ImageView shareIc, bookIc;
    int lastSig = -1;
    ArrayList<Tab> tabs = new ArrayList<>();
    int cur = -1, topInset, botInset;
    boolean blockAds = true, editing, swPriv, imeUp, barsShown = true;
    float dx0, dy0; long dt0;
    final Handler hnd = new Handler(Looper.getMainLooper());
    Ripples ripples;
    boolean rippleOn = true, blurOn = true;
    Bitmap homeBg;
    View blurTarget; float blurLvl;
    final Runnable blurDecay = new Runnable() {
        @Override public void run() {
            View t = blurTarget; if (t == null) return;
            blurLvl *= .76f;
            if (blurLvl < 1f) { clearBlur(t); blurTarget = null; blurLvl = 0; return; }
            setBlur(t, blurLvl); hnd.postDelayed(this, 16);
        }
    };
    final Object lock = new Object();
    final ExecutorService io = Executors.newSingleThreadExecutor();
    final Runnable hideRun = () -> { if (switcher.getVisibility() != View.VISIBLE && library.getVisibility() != View.VISIBLE) hideBars(); };
    View customView;
    WebChromeClient.CustomViewCallback customCb;

    // ================= helpers =================
    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + .5f); }

    GradientDrawable rounded(int color, int r) {
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(r)); return g;
    }

    GradientDrawable oval(int color) {
        GradientDrawable g = new GradientDrawable(); g.setShape(GradientDrawable.OVAL); g.setColor(color); return g;
    }

    TextView tv(String s, float size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }

    void press(View v) {
        v.setOnTouchListener((x, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) x.animate().setStartDelay(0).scaleX(.9f).scaleY(.9f).setDuration(90).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL)
                x.animate().setStartDelay(0).scaleX(1f).scaleY(1f).setDuration(160).setInterpolator(new OvershootInterpolator(2f)).start();
            return false;
        });
    }

    TextView btn(String s, View.OnClickListener l) {
        TextView t = tv(s, 24, WHITE, false); t.setGravity(Gravity.CENTER); t.setOnClickListener(l); press(t); return t;
    }

    ImageView ic(int res, View.OnClickListener l) {
        ImageView i = new ImageView(this); i.setImageResource(res); i.setPadding(dp(13), dp(13), dp(13), dp(13));
        i.setOnClickListener(l); press(i); return i;
    }

    TextView circle(String s, int size, int bg, int fg, View.OnClickListener l) {
        TextView t = tv(s, size > 56 ? 30 : 20, fg, false); t.setGravity(Gravity.CENTER);
        t.setBackground(oval(bg)); t.setElevation(dp(6)); t.setOnClickListener(l); press(t); return t;
    }

    TextView badge(String host, int sz) {
        String h = host == null || host.isEmpty() ? "?" : host;
        TextView b = tv(h.substring(0, 1).toUpperCase(), sz > 40 ? 20 : 16, WHITE, true);
        b.setGravity(Gravity.CENTER); b.setBackground(oval(PAL[Math.abs(h.hashCode()) % PAL.length])); return b;
    }

    void pop(View v, int i) {
        v.setAlpha(0f); v.setScaleX(.85f); v.setScaleY(.85f); v.setTranslationY(dp(30));
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0).setStartDelay(Math.min(i, 8) * 45L)
            .setDuration(340).setInterpolator(new OvershootInterpolator(.9f)).start();
    }

    void showOverlay(View v) {
        v.animate().cancel(); v.setVisibility(View.VISIBLE); v.setAlpha(0f); v.setTranslationY(dp(48));
        v.animate().alpha(1f).translationY(0).setDuration(280).setInterpolator(new DecelerateInterpolator(1.8f)).start();
    }

    void hideOverlay(final View v) {
        v.animate().alpha(0f).translationY(dp(48)).setDuration(200).setInterpolator(new AccelerateInterpolator())
            .withEndAction(() -> { v.setVisibility(View.GONE); v.setAlpha(1f); v.setTranslationY(0); }).start();
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    AlertDialog.Builder dlg() { return new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert); }

    int eng() { return sp.getInt("se", 0); }

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

    // ================= lifecycle =================
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("storm", MODE_PRIVATE);
        blockAds = sp.getBoolean("ads", true);
        rippleOn = sp.getBoolean("ripple", true); blurOn = sp.getBoolean("mblur", true);
        getWindow().setStatusBarColor(Color.TRANSPARENT); getWindow().setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        topInset = dp(28); botInset = 0;
        wrapper = new FrameLayout(this); wrapper.setBackgroundColor(INK);
        if (Build.VERSION.SDK_INT >= 30) getWindow().setDecorFitsSystemWindows(false);
        else wrapper.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        content = new FrameLayout(this); content.setBackgroundColor(INK);
        wrapper.addView(content, new FrameLayout.LayoutParams(-1, -1));
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgressTintList(ColorStateList.valueOf(PINK)); bar.setVisibility(View.INVISIBLE);
        wrapper.addView(bar, barLp());
        dock = buildDock(); dock.source = content; applyStyle();
        wrapper.addView(dock, dockLp());
        library = new FrameLayout(this); library.setVisibility(View.GONE);
        wrapper.addView(library, new FrameLayout.LayoutParams(-1, -1));
        switcher = new FrameLayout(this); switcher.setVisibility(View.GONE);
        wrapper.addView(switcher, new FrameLayout.LayoutParams(-1, -1));
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
            bar.setLayoutParams(barLp()); dock.setLayoutParams(dockLp());
            if (oldTop != topInset) { Tab ct = cur(); if (ct != null && ct.url.isEmpty()) buildHome(ct.priv, false); }
            if (editing && imeUp && !ime) endEdit();
            imeUp = ime;
            return ins;
        });
        setContentView(wrapper);
        wrapper.requestApplyInsets();
        home = new FrameLayout(this);
        handle(getIntent());
        playOpening();
    }

    FrameLayout.LayoutParams barLp() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, dp(3), Gravity.TOP); lp.topMargin = topInset; return lp;
    }

    FrameLayout.LayoutParams dockLp() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lp.setMargins(dp(12), 0, dp(12), botInset + dp(8)); return lp;
    }

    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); handle(i); }

    void handle(Intent i) {
        Uri d = i == null ? null : i.getData();
        if (d != null) newTab(d.toString(), false);
        else if (tabs.isEmpty()) newTab(null, false);
    }

    @Override protected void onPause() { super.onPause(); CookieManager.getInstance().flush(); }

    @Override protected void onDestroy() {
        for (Tab t : tabs) if (t.web != null) t.web.destroy();
        io.shutdown();
        super.onDestroy();
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (rippleOn) { int[] l = new int[2]; ripples.getLocationOnScreen(l); ripples.spawn(e.getRawX() - l[0], e.getRawY() - l[1]); }
            if (barsShown && !editing) scheduleHide();
        }
        return super.dispatchTouchEvent(e);
    }

    @Override public void onBackPressed() {
        if (customView != null) { hideCustom(); return; }
        if (switcher.getVisibility() == View.VISIBLE) { hideOverlay(switcher); return; }
        if (library.getVisibility() == View.VISIBLE) { hideOverlay(library); return; }
        if (editing) { endEdit(); return; }
        Tab t = cur();
        if (t != null && !t.url.isEmpty()) { goBack(); return; }
        if (tabs.size() > 1) { closeTab(cur); return; }
        super.onBackPressed();
    }

    // ================= glass / solid / frosted / hydrogen panel =================
    class Glass extends FrameLayout {
        View source; Bitmap bmp; float rad; final float baseRad; boolean pending, animating;
        int mode = 1, accent = PINK; boolean dark = true, priv;
        final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG), bp = new Paint(Paint.ANTI_ALIAS_FLAG), bub = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Rect dst = new Rect(); final RectF rf = new RectF(); final Matrix mx = new Matrix();
        final RadialGradient[] rg = new RadialGradient[3];
        final long t0 = System.nanoTime();
        final Runnable frame = new Runnable() {
            @Override public void run() {
                animating = false;
                if (mode == 3 && isShown()) {
                    float tm = (System.nanoTime() - t0) / 1e9f;
                    rad = baseRad * (1f + .08f * (float) Math.sin(tm * 1.8f));
                    invalidateOutline(); invalidate();
                    animating = true; postOnAnimation(this);
                }
            }
        };

        Glass(int radiusDp) {
            super(MainActivity.this);
            baseRad = dp(radiusDp); rad = baseRad; setWillNotDraw(false);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad); }
            });
            setClipToOutline(true);
            stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(dp(1));
            buildShaders();
        }

        int[] cols() {
            return priv ? new int[]{PURPLE, 0xFF7A5CFF, PINK} : new int[]{accent, 0xFF5B8CFF, 0xFF3DE0C5};
        }

        void buildShaders() {
            int[] c = cols();
            for (int i = 0; i < 3; i++)
                rg[i] = new RadialGradient(0, 0, 1f, (c[i] & 0xFFFFFF) | 0xAA000000, c[i] & 0xFFFFFF, Shader.TileMode.CLAMP);
        }

        void setStyle(int m, boolean d, int a, boolean p) {
            mode = m; dark = d; accent = a; priv = p; buildShaders(); rad = baseRad;
            invalidateOutline(); invalidate(); kick(); requestBlur();
        }

        void kick() { if (mode == 3 && !animating && isShown()) { animating = true; postOnAnimation(frame); } }

        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); kick(); postDelayed(this::requestBlur, 500); }
        @Override protected void onVisibilityChanged(View changed, int vis) { super.onVisibilityChanged(changed, vis); if (vis == View.VISIBLE) kick(); }

        // Re-captures the blurred backdrop at most every ~90 ms, and only when asked
        void requestBlur() {
            if (pending || mode == 0) return;
            pending = true;
            postDelayed(() -> {
                pending = false;
                if (isShown() && source != null) { refresh(); invalidate(); }
            }, 90);
        }

        void refresh() {
            int w = getWidth(), h = getHeight(); if (w == 0 || h == 0) return;
            int sc = mode == 2 ? 40 : mode == 3 ? 22 : 18;
            int bw = Math.max(1, w / sc), bh = Math.max(1, h / sc);
            if (bmp == null || bmp.getWidth() != bw || bmp.getHeight() != bh)
                bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
            int[] a = new int[2], b = new int[2];
            getLocationInWindow(a); source.getLocationInWindow(b);
            try {
                Canvas c = new Canvas(bmp); c.drawColor(INK);
                c.scale(bw / (float) w, bh / (float) h);
                c.translate(b[0] - a[0], b[1] - a[1]);
                source.draw(c);
            } catch (Throwable e) { }
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(); if (w == 0) return;
            int base = dark ? (priv ? 0xFF2A1640 : 0xFF111214) : (priv ? 0xFFEFE6FF : 0xFFFFFFFF);
            if (mode == 0) c.drawColor(base);
            else {
                if (bmp != null) { dst.set(0, 0, (int) w, (int) h); c.drawBitmap(bmp, null, dst, paint); }
                int al = mode == 1 ? 0x44 : mode == 2 ? 0xB8 : 0x70;
                c.drawColor((al << 24) | (base & 0xFFFFFF));
                if (mode == 2) c.drawColor(dark ? 0x14FFFFFF : 0x30FFFFFF);
                if (mode == 3) {
                    float tm = (System.nanoTime() - t0) / 1e9f;
                    for (int i = 0; i < 3; i++) {
                        float ph = i * 2.1f;
                        float cx = w * (.5f + .45f * (float) Math.sin(tm * (.8f + .25f * i) + ph));
                        float cy = h * (.5f + .55f * (float) Math.cos(tm * (.6f + .2f * i) + ph * 1.3f));
                        float r = h * (1.1f + .35f * (float) Math.sin(tm * 1.3f + ph));
                        mx.setScale(r, r); mx.postTranslate(cx, cy);
                        rg[i].setLocalMatrix(mx); bp.setShader(rg[i]);
                        c.drawRect(0, 0, w, h, bp);
                    }
                    bub.setColor(dark ? 0x66FFFFFF : 0x99FFFFFF);
                    for (int k = 0; k < 7; k++) {
                        float bx = w * ((k * .15f + .07f) + .025f * (float) Math.sin(tm * 1.5f + k));
                        float by = h * (1f - ((tm * .16f + k * .17f) % 1f));
                        c.drawCircle(bx, by, dp(2) + dp(k % 3), bub);
                    }
                }
            }
            rf.set(0, 0, w, h);
            stroke.setColor(mode == 3 ? (cols()[0] & 0xFFFFFF) | 0x99000000 : dark ? 0x55FFFFFF : 0x33000000);
            c.drawRoundRect(rf, rad, rad, stroke);
        }
    }

    // ================= bottom dock (Safari style) =================
    Glass buildDock() {
        Glass g = new Glass(30);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(10), dp(10), dp(10), dp(4));

        pill = new FrameLayout(this);
        pill.setOnClickListener(v -> startEdit());
        pillText = tv("", 15, WHITE, true); pillText.setGravity(Gravity.CENTER); pillText.setSingleLine(true);
        pillText.setEllipsize(TextUtils.TruncateAt.END); pillText.setOnClickListener(v -> startEdit());
        FrameLayout.LayoutParams mid = new FrameLayout.LayoutParams(-1, -1); mid.setMargins(dp(48), 0, dp(48), 0);
        pill.addView(pillText, mid);
        urlField = new EditText(this); urlField.setBackground(null); urlField.setSingleLine(true);
        urlField.setHint("Search or enter website"); urlField.setHintTextColor(0x99FFFFFF);
        urlField.setTextColor(WHITE); urlField.setTextSize(15); urlField.setGravity(Gravity.CENTER_VERTICAL);
        urlField.setSelectAllOnFocus(true); urlField.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlField.setOnEditorActionListener((v, a, e) -> { load(v.getText().toString()); return true; });
        urlField.setOnFocusChangeListener((v, f) -> { if (!f) endEdit(); });
        urlField.setVisibility(View.GONE);
        FrameLayout.LayoutParams ef = new FrameLayout.LayoutParams(-1, -1); ef.setMargins(dp(16), 0, dp(48), 0);
        pill.addView(urlField, ef);
        pillLeft = tv("Aa", 15, WHITE, true); pillLeft.setGravity(Gravity.CENTER);
        pillLeft.setOnClickListener(v -> menu()); press(pillLeft);
        pill.addView(pillLeft, new FrameLayout.LayoutParams(dp(48), -1, Gravity.START));
        pillRight = tv("\u21BB", 20, WHITE, false); pillRight.setGravity(Gravity.CENTER);
        pillRight.setOnClickListener(v -> {
            if (editing) { urlField.setText(""); return; }
            Tab t = cur(); if (t == null || t.url.isEmpty() || t.web == null) return;
            if (bar.getVisibility() == View.VISIBLE) t.web.stopLoading(); else t.web.reload();
        });
        press(pillRight);
        pill.addView(pillRight, new FrameLayout.LayoutParams(dp(48), -1, Gravity.END));
        col.addView(pill, new LinearLayout.LayoutParams(-1, dp(44)));

        toolbar = new LinearLayout(this); toolbar.setGravity(Gravity.CENTER_VERTICAL);
        backBtn = btn("\u2039", v -> goBack()); backBtn.setTextSize(34);
        fwdBtn = btn("\u203A", v -> { Tab t = cur(); if (t != null && t.web != null && t.web.canGoForward()) t.web.goForward(); });
        fwdBtn.setTextSize(34);
        toolbar.addView(backBtn, new LinearLayout.LayoutParams(0, dp(46), 1f));
        toolbar.addView(fwdBtn, new LinearLayout.LayoutParams(0, dp(46), 1f));
        shareIc = ic(R.drawable.ic_share, v -> {
            Tab t = cur(); if (t == null || t.url.isEmpty()) { toast("Open a page first"); return; }
            Intent s = new Intent(Intent.ACTION_SEND); s.setType("text/plain"); s.putExtra(Intent.EXTRA_TEXT, t.url);
            startActivity(Intent.createChooser(s, "Share"));
        });
        toolbar.addView(shareIc, new LinearLayout.LayoutParams(0, dp(46), 1f));
        ImageView book = ic(R.drawable.ic_book, v -> openLibrary(false));
        bookIc = book;
        book.setOnLongClickListener(v -> { openLibrary(true); return true; });
        toolbar.addView(book, new LinearLayout.LayoutParams(0, dp(46), 1f));
        FrameLayout tabCell = new FrameLayout(this);
        tabCount = tv("1", 13, WHITE, true); tabCount.setGravity(Gravity.CENTER);
        GradientDrawable box = rounded(0, 8); box.setStroke(dp(2), WHITE); tabCount.setBackground(box);
        tabCount.setOnClickListener(v -> openSwitcher(false)); press(tabCount);
        tabCell.addView(tabCount, new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER));
        toolbar.addView(tabCell, new LinearLayout.LayoutParams(0, dp(46), 1f));
        col.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(46)));
        g.addView(col, new FrameLayout.LayoutParams(-1, -2));
        return g;
    }

    void startEdit() {
        if (editing) return;
        Tab t = cur(); if (t == null) return;
        editing = true; hnd.removeCallbacks(hideRun); revealDock();
        urlField.setText(t.url);
        pillText.setVisibility(View.GONE); urlField.setVisibility(View.VISIBLE); toolbar.setVisibility(View.GONE);
        pillRight.setText("\u2715"); pillRight.setTextSize(16);
        urlField.requestFocus(); urlField.selectAll();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(urlField, InputMethodManager.SHOW_IMPLICIT);
    }

    void endEdit() {
        if (!editing) return;
        editing = false;
        urlField.setVisibility(View.GONE); pillText.setVisibility(View.VISIBLE); toolbar.setVisibility(View.VISIBLE);
        pillRight.setText("\u21BB"); pillRight.setTextSize(20);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(urlField.getWindowToken(), 0);
        urlField.clearFocus(); scheduleHide();
    }

    void showKb() { startEdit(); }
    void hideKb() { endEdit(); }

    void setUrlText(String u) {
        if (u == null || u.isEmpty()) {
            pillText.setText("Search or enter website"); pillText.setAlpha(.7f);
        } else {
            pillText.setText((u.startsWith("https") ? "\uD83D\uDD12 " : "") + hostOf(u)); pillText.setAlpha(1f);
        }
        if (!editing) urlField.setText(u);
    }

    // ---- auto-hide dock ----
    void revealDock() {
        if (barsShown) return;
        barsShown = true; dock.setVisibility(View.VISIBLE);
        dock.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        dock.animate().setStartDelay(0).translationY(0).alpha(1f).scaleX(1f).scaleY(1f).setDuration(320)
            .setInterpolator(new OvershootInterpolator(.8f))
            .withEndAction(() -> dock.setLayerType(View.LAYER_TYPE_NONE, null)).start();
        dock.requestBlur();
    }

    void showBars() { hnd.removeCallbacks(hideRun); revealDock(); dock.requestBlur(); scheduleHide(); }

    void scheduleHide() {
        hnd.removeCallbacks(hideRun);
        Tab t = cur();
        if (t != null && !t.url.isEmpty() && !editing) hnd.postDelayed(hideRun, 3000);
    }

    void hideBars() {
        if (!barsShown || editing) return;
        barsShown = false;
        dock.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        dock.animate().setStartDelay(0).translationY(dock.getHeight() + botInset + dp(24)).alpha(0f).scaleX(.96f).scaleY(.96f)
            .setDuration(260).setInterpolator(new AccelerateInterpolator())
            .withEndAction(() -> { dock.setLayerType(View.LAYER_TYPE_NONE, null); if (!barsShown) dock.setVisibility(View.INVISIBLE); }).start();
    }

    // ================= home =================
    void buildHome(boolean priv, boolean animate) {
        home.removeAllViews();
        int[] cols = priv ? new int[]{0xFF3B2150, 0xFF1A0F26, 0xFF0E0F11} : new int[]{0xFF4A4D52, 0xFF1A1B1E, 0xFF0E0F11};
        Bitmap bg = priv ? null : loadBg();
        if (bg != null) {
            home.setBackgroundColor(INK);
            ImageView iv = new ImageView(this); iv.setImageBitmap(bg); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            home.addView(iv, new FrameLayout.LayoutParams(-1, -1));
            View ov = new View(this);
            ov.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x66000000, 0x33000000, 0xCC0E0F11}));
            home.addView(ov, new FrameLayout.LayoutParams(-1, -1));
        } else home.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, cols));
        int accent = priv ? PURPLE : PINK;
        int H = getResources().getDisplayMetrics().heightPixels;

        TextView year = tv(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)), 96, 0x20FFFFFF, true);
        year.setLetterSpacing(-0.05f);
        FrameLayout.LayoutParams yp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        yp.bottomMargin = (int) (H * .40); home.addView(year, yp);

        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL); hero.setPadding(dp(24), dp(28) + topInset, dp(24), 0);
        TextView brand = tv(priv ? "Private" : "Storm", 60, WHITE, true);
        brand.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL)); brand.setLetterSpacing(-0.04f);
        hero.addView(brand);
        SpannableString ss = new SpannableString(priv ? "No history. No traces.\nJust you and the idea." : "You choose the idea.\nWe handle the web from there.");
        int s = ss.toString().indexOf("idea");
        ss.setSpan(new ForegroundColorSpan(accent), s, s + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new StyleSpan(Typeface.ITALIC), s, s + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new TypefaceSpan("serif"), s, s + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView tag = tv("", 20, WHITE, true); tag.setText(ss); tag.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2); tp.topMargin = dp(10);
        hero.addView(tag, tp);
        TextView cta = tv("Start browsing  \u2192", 16, INK, true); cta.setBackground(rounded(WHITE, 28));
        cta.setPadding(dp(30), dp(16), dp(30), dp(16)); cta.setOnClickListener(v -> startEdit()); press(cta);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2); cp.topMargin = dp(22);
        hero.addView(cta, cp);
        home.addView(hero, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        if (!priv) {
            TextView bgb = circle("\uD83D\uDDBC", 40, 0x66000000, WHITE, v -> pickBg());
            FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(dp(42), dp(42), Gravity.TOP | Gravity.END);
            bl.setMargins(0, topInset + dp(12), dp(16), 0); home.addView(bgb, bl);
        }
        if (animate) { pop(brand, 0); pop(tag, 1); pop(cta, 2); }

        ScrollView sheet = new ScrollView(this);
        float r = dp(36);
        GradientDrawable sb = new GradientDrawable(); sb.setColor(WHITE);
        sb.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheet.setBackground(sb); sheet.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(24), dp(22), dp(24), dp(28) + botInset + dp(120));
        TextView where = tv("Where", 44, accent, false);
        where.setTypeface(Typeface.create("serif", Typeface.ITALIC)); col.addView(where);
        col.addView(tv("would you like to go?", 26, INK, true));
        TextView d = tv("Pick a favorite or type anywhere \u2014 we'll take care of the rest. No clutter, no trackers.", 15, 0xFF444444, false);
        LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(-2, -2); dp1.topMargin = dp(10);
        col.addView(d, dp1);

        LinearLayout row = null;
        for (int i = 0; i < QUICK.length; i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, dp(92)); rp.topMargin = dp(10);
                col.addView(row, rp);
            }
            final String url = QUICK[i][1];
            TextView c = tv(QUICK[i][0], 19, Color.parseColor(QUICK[i][3]), true);
            c.setGravity(Gravity.BOTTOM); c.setPadding(dp(16), dp(16), dp(16), dp(14));
            c.setBackground(rounded(Color.parseColor(QUICK[i][2]), 22)); c.setOnClickListener(v -> load(url)); press(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            if (i % 2 == 1) lp.leftMargin = dp(10);
            row.addView(c, lp);
            if (animate) pop(c, i + 3);
        }
        ArrayList<String[]> bms = read("bm");
        if (!bms.isEmpty()) {
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2); hp.topMargin = dp(22);
            col.addView(tv("Bookmarks", 18, INK, true), hp);
            for (int i = 0; i < Math.min(5, bms.size()); i++) {
                final String url = bms.get(i)[1];
                TextView e = tv(bms.get(i)[0], 15, INK, false); e.setSingleLine(true); e.setEllipsize(TextUtils.TruncateAt.END);
                e.setPadding(0, dp(10), 0, dp(10)); e.setOnClickListener(v -> load(url)); col.addView(e);
            }
        }
        sheet.addView(col); attachMotion(sheet, col);
        home.addView(sheet, new FrameLayout.LayoutParams(-1, (int) (H * .46), Gravity.BOTTOM));
        if (animate) {
            sheet.setTranslationY(dp(300));
            sheet.animate().translationY(0).setDuration(520).setInterpolator(new DecelerateInterpolator(2f)).start();
        }
    }

    // ================= tabs =================
    Tab cur() { return cur >= 0 && cur < tabs.size() ? tabs.get(cur) : null; }

    WebView web(Tab t) { if (t.web == null) t.web = makeWeb(t); return t.web; }

    void newTab(String url, boolean priv) {
        Tab t = new Tab(); t.priv = priv; tabs.add(t);
        select(tabs.size() - 1);
        if (url != null) load(url);
        if (switcher.getVisibility() == View.VISIBLE) hideOverlay(switcher);
    }

    void select(int i) {
        Tab p = cur(); if (p != null) snap(p);
        cur = i; showCurrent();
        content.setScaleX(.94f); content.setScaleY(.94f); content.setAlpha(.6f);
        content.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260).setInterpolator(new DecelerateInterpolator(1.6f)).start();
    }

    void showCurrent() {
        Tab t = cur(); content.removeAllViews();
        if (t.url.isEmpty()) {
            buildHome(t.priv, true); content.addView(home, new FrameLayout.LayoutParams(-1, -1)); setUrlText("");
        } else {
            content.addView(web(t), new FrameLayout.LayoutParams(-1, -1)); setUrlText(t.url);
        }
        bar.setVisibility(View.INVISIBLE); refreshUI(); showBars();
    }

    void refreshUI() {
        Tab t = cur(); if (t == null) return;
        int n = 0; for (Tab x : tabs) if (x.priv == t.priv) n++;
        tabCount.setText(String.valueOf(n));
        backBtn.setAlpha(!t.url.isEmpty() ? 1f : .35f);
        fwdBtn.setAlpha(!t.url.isEmpty() && t.web != null && t.web.canGoForward() ? 1f : .35f);
        applyStyle();
    }

    void load(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        Tab t = cur(); String u = fix(raw);
        t.url = u; web(t).loadUrl(u); showCurrent(); endEdit();
    }

    void goBack() {
        Tab t = cur();
        if (t.web != null && t.web.canGoBack()) t.web.goBack(); else { t.url = ""; showCurrent(); }
    }

    void closeTab(int i) {
        Tab t = tabs.remove(i);
        if (t.web != null) {
            ViewGroup p = (ViewGroup) t.web.getParent(); if (p != null) p.removeView(t.web);
            t.web.destroy();
        }
        if (tabs.isEmpty()) { cur = -1; newTab(null, false); }
        else { if (i < cur) cur--; if (cur >= tabs.size()) cur = tabs.size() - 1; showCurrent(); }
    }

    void snap(Tab t) {
        try {
            if (t.web == null || t.url.isEmpty() || t.web.getWidth() == 0) return;
            Bitmap b = Bitmap.createBitmap(t.web.getWidth() / 3, t.web.getHeight() / 3, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b); c.scale(1 / 3f, 1 / 3f); t.web.draw(c); t.thumb = b;
        } catch (Throwable e) { }
    }

    // ================= tab switcher (normal + private sections) =================
    void openSwitcher(boolean priv) {
        endEdit();
        Tab c = cur(); if (c != null) snap(c);
        swPriv = priv; buildSwitcher(); showOverlay(switcher);
    }

    int countTabs(boolean priv) { int n = 0; for (Tab x : tabs) if (x.priv == priv) n++; return n; }

    void buildSwitcher() {
        switcher.removeAllViews();
        int fg = swPriv ? WHITE : INK, accent = swPriv ? PURPLE : INK;
        switcher.setBackgroundColor(swPriv ? 0xFF150C20 : 0xFFEFEFEF);
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(10), topInset + dp(92), dp(10), dp(150) + botInset);
        ArrayList<Integer> idx = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) if (tabs.get(i).priv == swPriv) idx.add(i);
        if (idx.isEmpty()) {
            TextView e = tv(swPriv ? "No private tabs\nNothing you do here is saved." : "No tabs open", 18, fg, false);
            e.setTypeface(Typeface.create("serif", Typeface.ITALIC)); e.setGravity(Gravity.CENTER); e.setAlpha(.7f);
            e.setPadding(0, dp(120), 0, 0); col.addView(e);
        }
        for (int k = 0; k < idx.size(); k += 2) {
            LinearLayout row = new LinearLayout(this);
            for (int j = k; j < k + 2; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(250), 1f);
                lp.setMargins(dp(8), dp(8), dp(8), dp(8));
                if (j < idx.size()) { View c = tabCard(idx.get(j)); row.addView(c, lp); pop(c, j); }
                else row.addView(new View(this), lp);
            }
            col.addView(row);
        }
        sv.addView(col); attachMotion(sv, col);
        switcher.addView(sv, new FrameLayout.LayoutParams(-1, -1));

        // segmented control: Tabs | Private
        LinearLayout seg = new LinearLayout(this); seg.setBackground(rounded(swPriv ? 0x22FFFFFF : 0x14000000, 22));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int m = 0; m < 2; m++) {
            final boolean mode = m == 1;
            boolean sel = mode == swPriv;
            TextView s = tv((mode ? "Private " : "Tabs ") + countTabs(mode), 14, sel ? WHITE : fg, true);
            s.setGravity(Gravity.CENTER); s.setPadding(dp(22), dp(9), dp(22), dp(9));
            if (sel) s.setBackground(rounded(mode ? PURPLE : INK, 18)); else s.setAlpha(.7f);
            s.setOnClickListener(v -> { swPriv = mode; buildSwitcher(); });
            seg.addView(s);
        }
        FrameLayout.LayoutParams sp1 = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        sp1.topMargin = topInset + dp(18);
        switcher.addView(seg, sp1);

        FrameLayout.LayoutParams p1 = new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        p1.bottomMargin = dp(28) + botInset;
        switcher.addView(circle("+", 64, accent, WHITE, v -> { newTab(null, swPriv); startEdit(); }), p1);
        FrameLayout.LayoutParams p2 = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.BOTTOM | Gravity.START);
        p2.bottomMargin = dp(34) + botInset; p2.leftMargin = dp(24);
        switcher.addView(circle("\u21A9", 52, swPriv ? 0x33FFFFFF : WHITE, fg, v -> hideOverlay(switcher)), p2);
        FrameLayout.LayoutParams p3 = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.BOTTOM | Gravity.END);
        p3.bottomMargin = dp(34) + botInset; p3.rightMargin = dp(24);
        switcher.addView(circle("\uD83D\uDDD1", 52, swPriv ? 0x33FFFFFF : WHITE, fg, v -> {
            for (int i = tabs.size() - 1; i >= 0; i--) if (tabs.get(i).priv == swPriv) closeTab(i);
            if (switcher.getVisibility() == View.VISIBLE) buildSwitcher();
        }), p3);
    }

    View tabCard(int i) {
        Tab t = tabs.get(i);
        FrameLayout f = new FrameLayout(this);
        GradientDrawable bg = rounded(t.priv ? 0xFF3B2150 : INK, 22);
        if (i == cur) bg.setStroke(dp(3), t.priv ? PURPLE : accentColor());
        f.setBackground(bg); f.setClipToOutline(true); f.setElevation(dp(6));
        f.setRotation(i % 2 == 0 ? -1.5f : 1.5f);
        LinearLayout in = new LinearLayout(this); in.setOrientation(LinearLayout.VERTICAL);
        ImageView iv = new ImageView(this); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (t.thumb != null) iv.setImageBitmap(t.thumb);
        else iv.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            t.priv ? new int[]{0xFF5A3A78, 0xFF2A1640} : new int[]{0xFF4A4D52, 0xFF1A1B1E}));
        in.addView(iv, new LinearLayout.LayoutParams(-1, 0, 1f));
        TextView title = tv(t.url.isEmpty() ? (t.priv ? "Private tab" : "New tab") : t.title, 13, WHITE, true);
        title.setSingleLine(true); title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(dp(14), dp(10), dp(40), dp(12)); in.addView(title);
        f.addView(in, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP | Gravity.END);
        xp.setMargins(0, dp(8), dp(8), 0);
        TextView x = tv("\u2715", 12, WHITE, false); x.setGravity(Gravity.CENTER); x.setBackground(oval(0x99000000));
        final int idx = i;
        x.setOnClickListener(v -> { closeTab(idx); if (switcher.getVisibility() == View.VISIBLE) buildSwitcher(); });
        f.addView(x, xp);
        f.setOnClickListener(v -> { select(idx); hideOverlay(switcher); });
        press(f);
        return f;
    }

    // ================= library: bookmarks & history =================
    void openLibrary(boolean hist) {
        endEdit(); buildLibrary(hist); showOverlay(library);
    }

    void buildLibrary(final boolean hist) {
        library.removeAllViews();
        final int fg = hist ? WHITE : INK, sub = hist ? 0xFF9A9CA3 : 0xFF6B6B70;
        library.setBackgroundColor(hist ? INK : 0xFFF6F4F0);
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), topInset + dp(16), dp(20), botInset + dp(40));

        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        TextView it = tv(hist ? "where you've been" : "your favorite places", 17, PINK, false);
        it.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        titles.addView(it);
        titles.addView(tv(hist ? "History" : "Bookmarks", 36, fg, true));
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        if (hist) {
            TextView clr = tv("Clear", 14, 0xFFFF7A7A, true); clr.setPadding(dp(16), dp(10), dp(16), dp(10));
            clr.setBackground(rounded(0x22FF7A7A, 18)); press(clr);
            head.addView(clr);
        }
        TextView done = tv("Done", 14, hist ? WHITE : INK, true); done.setPadding(dp(18), dp(10), dp(18), dp(10));
        done.setBackground(rounded(hist ? 0x22FFFFFF : 0x14000000, 18));
        done.setOnClickListener(v -> hideOverlay(library)); press(done);
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(-2, -2); dl.leftMargin = dp(8);
        head.addView(done, dl);
        col.addView(head);

        final LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        if (hist) {
            final EditText q = new EditText(this); q.setSingleLine(true); q.setHint("Search history");
            q.setHintTextColor(0x88FFFFFF); q.setTextColor(WHITE); q.setTextSize(15);
            q.setBackground(rounded(0x1AFFFFFF, 18)); q.setPadding(dp(18), dp(12), dp(18), dp(12));
            q.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) { fillHistory(list, s.toString()); }
                @Override public void afterTextChanged(Editable s) { }
            });
            LinearLayout.LayoutParams ql = new LinearLayout.LayoutParams(-1, -2); ql.topMargin = dp(18);
            col.addView(q, ql);
            ((TextView) clr(head)).setOnClickListener(v -> dlg().setTitle("Clear history?")
                .setPositiveButton("Clear", (d, w) -> { sp.edit().remove("hist").apply(); fillHistory(list, q.getText().toString()); })
                .setNegativeButton("Cancel", null).show());
        } else {
            Tab t = cur();
            if (t != null && !t.url.isEmpty() && !t.priv) {
                TextView add = tv("\uFF0B  Add this page", 15, INK, true); add.setGravity(Gravity.CENTER);
                add.setBackground(rounded(PINK, 24)); press(add);
                add.setOnClickListener(v -> {
                    ArrayList<String[]> l = read("bm"); l.add(0, new String[]{t.title, t.url}); write("bm", l);
                    buildLibrary(false);
                });
                LinearLayout.LayoutParams al = new LinearLayout.LayoutParams(-1, dp(52)); al.topMargin = dp(18);
                col.addView(add, al);
            }
        }
        LinearLayout.LayoutParams ll = new LinearLayout.LayoutParams(-1, -2); ll.topMargin = dp(14);
        col.addView(list, ll);
        if (hist) fillHistory(list, ""); else fillBookmarks(list, sub, fg);
        sv.addView(col); attachMotion(sv, col);
        library.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    View clr(LinearLayout head) { return head.getChildAt(1); }

    void fillBookmarks(LinearLayout list, int sub, int fg) {
        list.removeAllViews();
        final ArrayList<String[]> l = read("bm");
        if (l.isEmpty()) {
            TextView e = tv("No bookmarks yet.\nOpen a page, then tap \u201CAdd this page\u201D.", 16, sub, false);
            e.setTypeface(Typeface.create("serif", Typeface.ITALIC)); e.setGravity(Gravity.CENTER); e.setPadding(0, dp(80), 0, 0);
            list.addView(e); return;
        }
        for (int i = 0; i < l.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            for (int j = i; j < i + 2; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(132), 1f);
                lp.setMargins(dp(6), dp(6), dp(6), dp(6));
                if (j >= l.size()) { row.addView(new View(this), lp); continue; }
                final int idx = j; final String[] e = l.get(j);
                FrameLayout card = new FrameLayout(this); card.setBackground(rounded(WHITE, 22)); card.setElevation(dp(3));
                LinearLayout in = new LinearLayout(this); in.setOrientation(LinearLayout.VERTICAL); in.setPadding(dp(14), dp(14), dp(14), dp(12));
                in.addView(badge(hostOf(e[1]), 40), new LinearLayout.LayoutParams(dp(40), dp(40)));
                TextView ti = tv(e[0], 14, INK, true); ti.setMaxLines(2); ti.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(-1, -2); tl.topMargin = dp(10); in.addView(ti, tl);
                TextView ho = tv(hostOf(e[1]), 12, sub, false); ho.setSingleLine(true); in.addView(ho);
                card.addView(in, new FrameLayout.LayoutParams(-1, -1));
                TextView x = tv("\u2715", 11, sub, false); x.setGravity(Gravity.CENTER);
                x.setOnClickListener(v -> { l.remove(idx); write("bm", l); fillBookmarks(list, sub, fg); });
                card.addView(x, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.TOP | Gravity.END));
                card.setOnClickListener(v -> { hideOverlay(library); load(e[1]); }); press(card);
                row.addView(card, lp); pop(card, j);
            }
            list.addView(row);
        }
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
                TextView h = tv(g, 13, PINK, true); h.setAllCaps(true); h.setLetterSpacing(.1f);
                h.setPadding(0, dp(18), 0, dp(6)); list.addView(h); last = g;
            }
            LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL); r.setPadding(0, dp(8), 0, dp(8));
            r.addView(badge(hostOf(e[1]), 36), new LinearLayout.LayoutParams(dp(36), dp(36)));
            LinearLayout mid = new LinearLayout(this); mid.setOrientation(LinearLayout.VERTICAL); mid.setPadding(dp(12), 0, dp(8), 0);
            TextView ti = tv(e[0], 15, WHITE, true); ti.setSingleLine(true); ti.setEllipsize(TextUtils.TruncateAt.END); mid.addView(ti);
            TextView ho = tv(hostOf(e[1]), 12, 0xFF9A9CA3, false); ho.setSingleLine(true); mid.addView(ho);
            r.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
            if (ts > 0) r.addView(tv(new SimpleDateFormat(ts >= today ? "h:mm a" : "MMM d", Locale.getDefault()).format(new Date(ts)), 12, 0xFF9A9CA3, false));
            r.setOnClickListener(v -> { hideOverlay(library); load(e[1]); });
            r.setOnLongClickListener(v -> {
                ArrayList<String[]> all = read("hist");
                for (int i = all.size() - 1; i >= 0; i--) if (all.get(i)[1].equals(e[1])) all.remove(i);
                write("hist", all); list.removeView(r); return true;
            });
            list.addView(r); n++;
        }
        if (n == 0) {
            TextView e = tv("Nothing here yet.", 16, 0xFF9A9CA3, false);
            e.setTypeface(Typeface.create("serif", Typeface.ITALIC)); e.setGravity(Gravity.CENTER); e.setPadding(0, dp(80), 0, 0);
            list.addView(e);
        }
    }

    // ================= appearance =================
    int accentColor() { return ACC[Math.min(ACC.length - 1, Math.max(0, sp.getInt("acc", 0)))]; }

    void applyStyle() {
        int st = sp.getInt("style", 1); boolean dark = sp.getBoolean("dark", true);
        Tab t = cur(); boolean priv = t != null && t.priv;
        int sig = st + (dark ? 10 : 0) + (priv ? 20 : 0) + accentColor() * 31;
        if (sig == lastSig) return;
        lastSig = sig;
        dock.setStyle(st, dark, accentColor(), priv);
        bar.setProgressTintList(ColorStateList.valueOf(priv ? PURPLE : accentColor()));
        int fg = dark ? WHITE : INK;
        pillText.setTextColor(fg); pillLeft.setTextColor(fg); pillRight.setTextColor(fg);
        backBtn.setTextColor(fg); fwdBtn.setTextColor(fg); tabCount.setTextColor(fg);
        urlField.setTextColor(fg); urlField.setHintTextColor(dark ? 0x99FFFFFF : 0x88000000);
        pill.setBackground(null);
        shareIc.setColorFilter(fg); bookIc.setColorFilter(fg);
        GradientDrawable box = rounded(0, 8); box.setStroke(dp(2), fg); tabCount.setBackground(box);
    }

    void openAppearance() { endEdit(); buildAppearance(); showOverlay(library); }

    TextView label(String s) {
        TextView l = tv(s, 13, PINK, true); l.setAllCaps(true); l.setLetterSpacing(.1f); l.setPadding(0, dp(24), 0, dp(10)); return l;
    }

    void buildAppearance() {
        library.removeAllViews(); library.setBackgroundColor(INK);
        final int st = sp.getInt("style", 1), acc = accentColor(); final boolean dark = sp.getBoolean("dark", true);
        ScrollView sv = new ScrollView(this); sv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(20), topInset + dp(16), dp(20), botInset + dp(40));

        LinearLayout head = new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        TextView it = tv("make it yours", 17, PINK, false); it.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        titles.addView(it); titles.addView(tv("Appearance", 36, WHITE, true));
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView done = tv("Done", 14, WHITE, true); done.setPadding(dp(18), dp(10), dp(18), dp(10));
        done.setBackground(rounded(0x22FFFFFF, 18)); done.setOnClickListener(v -> hideOverlay(library)); press(done);
        head.addView(done); col.addView(head);

        // live preview over a colourful backdrop
        FrameLayout prev = new FrameLayout(this); prev.setBackground(rounded(0xFF222428, 28)); prev.setClipToOutline(true);
        final FrameLayout bgv = new FrameLayout(this);
        bgv.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFFFF8A4C, 0xFFE86FB5, 0xFF5B8CFF}));
        View o1 = new View(this); o1.setBackground(oval(0xFF3DE0C5));
        FrameLayout.LayoutParams l1 = new FrameLayout.LayoutParams(dp(90), dp(90), Gravity.START | Gravity.TOP); l1.setMargins(dp(40), dp(8), 0, 0);
        bgv.addView(o1, l1);
        View o2 = new View(this); o2.setBackground(oval(0xFFE5B83B));
        FrameLayout.LayoutParams l2 = new FrameLayout.LayoutParams(dp(110), dp(110), Gravity.END | Gravity.BOTTOM); l2.setMargins(0, 0, dp(30), dp(-10));
        bgv.addView(o2, l2);
        prev.addView(bgv, new FrameLayout.LayoutParams(-1, -1));
        final Glass pg = new Glass(24); pg.source = bgv; pg.setStyle(st, dark, acc, false);
        int fg = dark ? WHITE : INK;
        FrameLayout in = new FrameLayout(this);
        TextView pl = tv("Aa", 15, fg, true); pl.setGravity(Gravity.CENTER);
        in.addView(pl, new FrameLayout.LayoutParams(dp(44), -1, Gravity.START));
        TextView pt = tv("\uD83D\uDD12 storm.browser", 15, fg, true); pt.setGravity(Gravity.CENTER);
        in.addView(pt, new FrameLayout.LayoutParams(-1, -1));
        TextView pr = tv("\u21BB", 20, fg, false); pr.setGravity(Gravity.CENTER);
        in.addView(pr, new FrameLayout.LayoutParams(dp(44), -1, Gravity.END));
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-1, dp(44)); ip.setMargins(dp(8), dp(8), dp(8), dp(8));
        pg.addView(in, ip);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER); gp.setMargins(dp(20), 0, dp(20), 0);
        prev.addView(pg, gp);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-1, dp(170)); pp.topMargin = dp(18);
        col.addView(prev, pp);
        pg.postDelayed(() -> pg.requestBlur(), 400);

        col.addView(label("Bar style"));
        String[][] S = {{"Solid", "Clean and opaque"}, {"Glass", "See-through blur"}, {"Frosted", "Soft, heavy blur"}, {"Hydrogen", "Fluid, alive motion"}};
        LinearLayout row = null;
        for (int k = 0; k < 4; k++) {
            if (k % 2 == 0) { row = new LinearLayout(this); col.addView(row, new LinearLayout.LayoutParams(-1, dp(86))); }
            final int kk = k; boolean sel = k == st;
            LinearLayout chip = new LinearLayout(this); chip.setOrientation(LinearLayout.VERTICAL); chip.setGravity(Gravity.CENTER_VERTICAL);
            chip.setPadding(dp(16), dp(10), dp(16), dp(10));
            GradientDrawable cb = rounded(sel ? (acc & 0xFFFFFF) | 0x33000000 : 0x14FFFFFF, 20);
            if (sel) cb.setStroke(dp(2), acc);
            chip.setBackground(cb);
            chip.addView(tv(S[k][0], 17, WHITE, true)); chip.addView(tv(S[k][1], 12, 0xFF9A9CA3, false));
            chip.setOnClickListener(v -> { sp.edit().putInt("style", kk).apply(); applyStyle(); buildAppearance(); }); press(chip);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, -1, 1f);
            cl.setMargins(k % 2 == 1 ? dp(5) : 0, dp(5), k % 2 == 0 ? dp(5) : 0, dp(5));
            row.addView(chip, cl); pop(chip, k);
        }

        col.addView(label("Theme"));
        LinearLayout seg = new LinearLayout(this); seg.setBackground(rounded(0x14FFFFFF, 22)); seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int m = 0; m < 2; m++) {
            final boolean d = m == 0; boolean sel = d == dark;
            TextView b = tv(d ? "Dark" : "Light", 15, sel ? (d ? WHITE : INK) : WHITE, true); b.setGravity(Gravity.CENTER);
            if (sel) b.setBackground(rounded(d ? 0xFF3A3D44 : WHITE, 18)); else b.setAlpha(.6f);
            b.setOnClickListener(v -> { sp.edit().putBoolean("dark", d).apply(); applyStyle(); buildAppearance(); });
            seg.addView(b, new LinearLayout.LayoutParams(0, dp(42), 1f));
        }
        col.addView(seg, new LinearLayout.LayoutParams(-1, -2));

        col.addView(label("Accent"));
        LinearLayout sw = new LinearLayout(this);
        for (int k = 0; k < ACC.length; k++) {
            final int kk = k;
            TextView c = tv(k == sp.getInt("acc", 0) ? "\u2713" : "", 16, INK, true); c.setGravity(Gravity.CENTER);
            GradientDrawable og = oval(ACC[k]); if (k == sp.getInt("acc", 0)) og.setStroke(dp(3), WHITE);
            c.setBackground(og); c.setOnClickListener(v -> { sp.edit().putInt("acc", kk).apply(); applyStyle(); buildAppearance(); }); press(c);
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, dp(44), 1f); cl.setMargins(dp(4), 0, dp(4), 0);
            sw.addView(c, cl);
        }
        col.addView(sw, new LinearLayout.LayoutParams(-1, -2));

        col.addView(label("Home background"));
        LinearLayout br = new LinearLayout(this);
        TextView choose = pb("Choose photo", acc, INK, v -> pickBg());
        TextView reset = pb("Reset", 0x22FFFFFF, WHITE, v -> {
            new File(getFilesDir(), "home_bg.jpg").delete(); homeBg = null;
            Tab ct = cur(); if (ct != null && ct.url.isEmpty()) buildHome(ct.priv, false);
            toast("Background reset");
        });
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, dp(48), 1f); b1.rightMargin = dp(6);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, dp(48), 1f); b2.leftMargin = dp(6);
        br.addView(choose, b1); br.addView(reset, b2);
        col.addView(br, new LinearLayout.LayoutParams(-1, -2));

        col.addView(label("Effects"));
        col.addView(toggle("Touch ripple", "ripple"));
        LinearLayout.LayoutParams tl2 = new LinearLayout.LayoutParams(-1, -2); tl2.topMargin = dp(8);
        col.addView(toggle("Motion blur while scrolling", "mblur"), tl2);
        sv.addView(col); attachMotion(sv, col);
        library.addView(sv, new FrameLayout.LayoutParams(-1, -1));
    }

    // ================= background photo, opening animation, ripple, motion blur =================
    TextView pb(String s, int bg, int fg, View.OnClickListener l) {
        TextView t = tv(s, 15, fg, true); t.setGravity(Gravity.CENTER); t.setBackground(rounded(bg, 24));
        t.setOnClickListener(l); press(t); return t;
    }

    View toggle(String label, final String key) {
        final boolean on = sp.getBoolean(key, true);
        LinearLayout r = new LinearLayout(this); r.setGravity(Gravity.CENTER_VERTICAL);
        r.setBackground(rounded(0x14FFFFFF, 20)); r.setPadding(dp(16), dp(14), dp(16), dp(14));
        r.addView(tv(label, 16, WHITE, true), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView st = tv(on ? "ON" : "OFF", 13, on ? INK : WHITE, true); st.setGravity(Gravity.CENTER);
        st.setPadding(dp(16), dp(6), dp(16), dp(6)); st.setBackground(rounded(on ? accentColor() : 0x33FFFFFF, 16));
        r.addView(st);
        r.setOnClickListener(v -> {
            sp.edit().putBoolean(key, !on).apply();
            rippleOn = sp.getBoolean("ripple", true); blurOn = sp.getBoolean("mblur", true);
            buildAppearance();
        });
        press(r); return r;
    }

    Bitmap loadBg() {
        if (homeBg == null) { File f = new File(getFilesDir(), "home_bg.jpg"); if (f.exists()) homeBg = BitmapFactory.decodeFile(f.getPath()); }
        return homeBg;
    }

    void pickBg() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Choose background"), 77);
    }

    @Override protected void onActivityResult(int rc, int res, Intent d) {
        super.onActivityResult(rc, res, d);
        if (rc == 77 && res == RESULT_OK && d != null && d.getData() != null) saveBg(d.getData());
    }

    void saveBg(final Uri u) {
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
                float sc = Math.min(1f, 1600f / Math.max(b.getWidth(), b.getHeight()));
                if (sc < 1f) b = Bitmap.createScaledBitmap(b, (int) (b.getWidth() * sc), (int) (b.getHeight() * sc), true);
                FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), "home_bg.jpg"));
                b.compress(Bitmap.CompressFormat.JPEG, 90, fo); fo.close();
                final Bitmap fb = b;
                runOnUiThread(() -> {
                    homeBg = fb;
                    Tab ct = cur(); if (ct != null && ct.url.isEmpty() && !ct.priv) buildHome(false, false);
                    dock.requestBlur(); toast("Background updated");
                });
            } catch (Exception e) { runOnUiThread(() -> toast("Couldn't load that image")); }
        });
    }

    // ---- opening animation ----
    void playOpening() {
        final FrameLayout splash = new FrameLayout(this); splash.setBackgroundColor(INK); splash.setClickable(true);
        for (int i = 0; i < 2; i++) {
            View ring = new View(this);
            GradientDrawable rg = new GradientDrawable(); rg.setShape(GradientDrawable.OVAL); rg.setColor(0); rg.setStroke(dp(2), accentColor());
            ring.setBackground(rg); ring.setAlpha(0f);
            splash.addView(ring, new FrameLayout.LayoutParams(dp(96), dp(96), Gravity.CENTER));
            ring.animate().alpha(.7f).setStartDelay(250 + i * 220L).setDuration(1).withEndAction(() ->
                ring.animate().setStartDelay(0).alpha(0f).scaleX(3.4f).scaleY(3.4f).setDuration(900).setInterpolator(new DecelerateInterpolator()).start()).start();
        }
        ImageView icon = new ImageView(this); icon.setImageResource(R.mipmap.ic_launcher); icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), dp(26)); }
        });
        icon.setClipToOutline(true); icon.setElevation(dp(10));
        splash.addView(icon, new FrameLayout.LayoutParams(dp(96), dp(96), Gravity.CENTER));
        TextView name = tv("Storm", 30, WHITE, true); name.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        name.setLetterSpacing(-0.03f);
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER); np.topMargin = dp(170);
        splash.addView(name, np);
        wrapper.addView(splash, new FrameLayout.LayoutParams(-1, -1));

        icon.setScaleX(.4f); icon.setScaleY(.4f); icon.setAlpha(0f);
        icon.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(560).setInterpolator(new OvershootInterpolator(1.5f)).start();
        name.setAlpha(0f); name.setTranslationY(dp(16));
        name.animate().alpha(1f).translationY(0).setStartDelay(260).setDuration(420).setInterpolator(new DecelerateInterpolator()).start();
        dock.setAlpha(0f); dock.setTranslationY(dp(90));
        content.setScaleX(.92f); content.setScaleY(.92f);
        splash.postDelayed(() -> {
            splash.animate().alpha(0f).scaleX(1.15f).scaleY(1.15f).setDuration(420).setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> wrapper.removeView(splash)).start();
            content.animate().scaleX(1f).scaleY(1f).setDuration(520).setInterpolator(new DecelerateInterpolator(2f)).start();
            dock.animate().setStartDelay(160).alpha(1f).translationY(0).setDuration(560).setInterpolator(new OvershootInterpolator(.9f)).start();
        }, 1250);
    }

    // ---- touch ripple layer ----
    class Ripples extends View {
        final ArrayList<float[]> xy = new ArrayList<>(); final ArrayList<Long> ts = new ArrayList<>();
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Ripples() { super(MainActivity.this); setClickable(false); setFocusable(false); }
        void spawn(float x, float y) { xy.add(new float[]{x, y}); ts.add(System.nanoTime()); invalidate(); }
        @Override protected void onDraw(Canvas c) {
            long now = System.nanoTime(); int col = accentColor() & 0xFFFFFF;
            for (int i = xy.size() - 1; i >= 0; i--) {
                float k = (now - ts.get(i)) / 600e6f;
                if (k >= 1f) { xy.remove(i); ts.remove(i); continue; }
                float e = 1f - (1f - k) * (1f - k);
                float r = dp(8) + dp(120) * e;
                p.setStyle(Paint.Style.FILL); p.setColor(col | ((int) (80 * (1f - k)) << 24));
                c.drawCircle(xy.get(i)[0], xy.get(i)[1], r, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(dp(2)); p.setColor(0xFFFFFF | ((int) (150 * (1f - k)) << 24));
                c.drawCircle(xy.get(i)[0], xy.get(i)[1], r, p);
            }
            if (!xy.isEmpty()) postInvalidateOnAnimation();
        }
    }

    // ---- directional motion blur while scrolling (Android 12+) ----
    void attachMotion(ScrollView sv, final View target) {
        sv.setOnScrollChangeListener((v, x, y, ox, oy) -> motion(target, y - oy));
    }

    void motion(View t, int dy) {
        if (Build.VERSION.SDK_INT < 31 || !blurOn) return;
        float lvl = Math.min(14f, Math.abs(dy) * .30f);
        if (lvl < 1.5f) return;
        if (blurTarget != null && blurTarget != t) clearBlur(blurTarget);
        blurTarget = t; blurLvl = Math.max(lvl, blurLvl * .85f);
        setBlur(t, blurLvl);
        hnd.removeCallbacks(blurDecay); hnd.postDelayed(blurDecay, 50);
    }

    void setBlur(View t, float r) {
        if (Build.VERSION.SDK_INT < 31) return;
        try { t.setRenderEffect(RenderEffect.createBlurEffect(0.01f, r, Shader.TileMode.CLAMP)); } catch (Throwable e) { }
    }

    void clearBlur(View t) {
        if (Build.VERSION.SDK_INT < 31) return;
        try { t.setRenderEffect(null); } catch (Throwable e) { }
    }

    // ================= menu =================
    void menu() {
        final Tab t = cur();
        String[] items = {"New tab", "New private tab", "Private tabs", "Bookmarks", "History", "Add bookmark",
            "Desktop site: " + (t.desktop ? "on" : "off"), "Ad blocker: " + (blockAds ? "on" : "off"),
            "Search engine: " + ENGINES[eng()][0], "Appearance", "Clear browsing data"};
        dlg().setItems(items, (d, w) -> {
            switch (w) {
                case 0: newTab(null, false); startEdit(); break;
                case 1: newTab(null, true); startEdit(); break;
                case 2: openSwitcher(true); break;
                case 3: openLibrary(false); break;
                case 4: openLibrary(true); break;
                case 5:
                    if (t.url.isEmpty() || t.priv) { toast("Open a normal page first"); break; }
                    ArrayList<String[]> l = read("bm"); l.add(0, new String[]{t.title, t.url}); write("bm", l);
                    toast("Bookmarked"); break;
                case 6:
                    t.desktop = !t.desktop;
                    if (t.web != null) { t.web.getSettings().setUserAgentString(t.desktop ? DESKTOP_UA : null); if (!t.url.isEmpty()) t.web.reload(); }
                    break;
                case 7: blockAds = !blockAds; sp.edit().putBoolean("ads", blockAds).apply(); toast("Ad blocker " + (blockAds ? "on" : "off")); break;
                case 8:
                    String[] names = new String[ENGINES.length];
                    for (int i = 0; i < names.length; i++) names[i] = ENGINES[i][0];
                    dlg().setTitle("Search engine").setItems(names, (d2, k) -> sp.edit().putInt("se", k).apply()).show(); break;
                case 9: openAppearance(); break;
                case 10:
                    CookieManager.getInstance().removeAllCookies(null); WebStorage.getInstance().deleteAllData();
                    for (Tab x : tabs) if (x.web != null) x.web.clearCache(true);
                    sp.edit().remove("hist").apply(); toast("Cleared"); break;
            }
        }).show();
    }

    // ================= web =================
    boolean isAd(String host) {
        if (host == null) return false;
        for (String a : ADS) if (host.equals(a) || host.endsWith("." + a)) return true;
        return false;
    }

    void hideCustom() {
        if (customView == null) return;
        wrapper.removeView(customView); customView = null;
        if (customCb != null) customCb.onCustomViewHidden();
    }

    WebView makeWeb(final Tab t) {
        final WebView w = new WebView(this);
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false); s.setLoadWithOverviewMode(true); s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false); s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (t.desktop) s.setUserAgentString(DESKTOP_UA);
        if (t.priv) { s.setCacheMode(WebSettings.LOAD_NO_CACHE); s.setSaveFormData(false); }
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, false);
        w.setBackgroundColor(WHITE); w.setOverScrollMode(View.OVER_SCROLL_NEVER);
        w.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) { dx0 = e.getX(); dy0 = e.getY(); dt0 = e.getEventTime(); }
            else if (a == MotionEvent.ACTION_UP && t == cur() && Math.abs(e.getX() - dx0) < dp(10)
                && Math.abs(e.getY() - dy0) < dp(10) && e.getEventTime() - dt0 < 350) showBars();
            return false;
        });
        w.setOnScrollChangeListener((v, sx, sy, ox, oy) -> {
            motion(w, sy - oy);
            if (t != cur()) return;
            int d = sy - oy;
            if (d > dp(10)) hideBars(); else if (d < -dp(10)) showBars(); else if (barsShown) dock.requestBlur();
        });
        w.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                if (blockAds && isAd(r.getUrl().getHost()))
                    return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String sc = r.getUrl().getScheme();
                if ("http".equals(sc) || "https".equals(sc) || "about".equals(sc) || "file".equals(sc)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { toast("No app to open link"); }
                return true;
            }
            @Override public void onPageStarted(WebView v, String url, Bitmap f) {
                t.url = url;
                v.setAlpha(.55f); v.animate().alpha(1f).setDuration(380).start();
                if (t == cur()) { bar.setVisibility(View.VISIBLE); setUrlText(url); refreshUI(); showBars(); }
            }
            @Override public void onPageFinished(WebView v, String url) {
                t.title = v.getTitle() == null ? url : v.getTitle();
                if (!t.priv) addHistory(t.title, url);
                if (t == cur()) { bar.setVisibility(View.INVISIBLE); refreshUI(); if (barsShown) dock.requestBlur(); }
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
