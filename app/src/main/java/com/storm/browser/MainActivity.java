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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
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
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends Activity {
    static final int PINK = 0xFFE86FB5, INK = 0xFF111214, PANEL = 0xFF1C1D20, WHITE = 0xFFFFFFFF;
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
    FrameLayout wrapper, content, switcher, home;
    Glass topGlass, bottomGlass;
    int topInset, botInset;
    boolean barsShown = true;
    float dx0, dy0; long dt0;
    final Handler hnd = new Handler(Looper.getMainLooper());
    final Runnable hideRun = () -> { if (switcher.getVisibility() != View.VISIBLE) hideBars(); };
    EditText urlField;
    ProgressBar bar;
    TextView tabCount, backBtn, fwdBtn;
    ArrayList<Tab> tabs = new ArrayList<>();
    int cur = -1;
    boolean blockAds = true;
    View customView;
    WebChromeClient.CustomViewCallback customCb;

    // ---------- helpers ----------
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

    TextView btn(String s, View.OnClickListener l) {
        TextView t = tv(s, 24, WHITE, false); t.setGravity(Gravity.CENTER); t.setOnClickListener(l); return t;
    }

    TextView circle(String s, int size, int bg, int fg, View.OnClickListener l) {
        TextView t = tv(s, size > 56 ? 30 : 20, fg, false); t.setGravity(Gravity.CENTER);
        t.setBackground(oval(bg)); t.setElevation(dp(6)); t.setOnClickListener(l); return t;
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    AlertDialog.Builder dlg() { return new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert); }

    void hideKb() {
        InputMethodManager m = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        m.hideSoftInputFromWindow(urlField.getWindowToken(), 0); urlField.clearFocus();
    }

    void showKb() {
        urlField.requestFocus();
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(urlField, 0);
    }

    int eng() { return sp.getInt("se", 0); }

    String fix(String s) {
        s = s.trim();
        if (s.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:(//)?.*") && (s.contains("://") || s.startsWith("about:") || s.startsWith("file:"))) return s;
        if (s.contains(".") && !s.contains(" ")) return "https://" + s;
        return ENGINES[eng()][1] + Uri.encode(s);
    }

    ArrayList<String[]> read(String k) {
        ArrayList<String[]> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(sp.getString(k, "[]"));
            for (int i = 0; i < a.length(); i++) { JSONArray e = a.getJSONArray(i); l.add(new String[]{e.getString(0), e.getString(1)}); }
        } catch (Exception e) { }
        return l;
    }

    void write(String k, ArrayList<String[]> l) {
        JSONArray a = new JSONArray();
        for (String[] e : l) { JSONArray x = new JSONArray(); x.put(e[0]); x.put(e[1]); a.put(x); }
        sp.edit().putString(k, a.toString()).apply();
    }

    void addHistory(String title, String url) {
        if (url == null || url.startsWith("about:")) return;
        ArrayList<String[]> l = read("hist");
        for (int i = l.size() - 1; i >= 0; i--) if (l.get(i)[1].equals(url)) l.remove(i);
        l.add(0, new String[]{title == null ? url : title, url});
        while (l.size() > 200) l.remove(l.size() - 1);
        write("hist", l);
    }

    // ---------- lifecycle ----------
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("storm", MODE_PRIVATE);
        blockAds = sp.getBoolean("ads", true);
        getWindow().setStatusBarColor(Color.TRANSPARENT); getWindow().setNavigationBarColor(Color.TRANSPARENT);
        topInset = dp(24); botInset = 0;
        wrapper = new FrameLayout(this); wrapper.setBackgroundColor(INK);
        wrapper.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        content = new FrameLayout(this); content.setBackgroundColor(INK);
        wrapper.addView(content, new FrameLayout.LayoutParams(-1, -1));
        topGlass = buildTop(); bottomGlass = buildBottom();
        topGlass.source = content; bottomGlass.source = content;
        wrapper.addView(topGlass, barLp(Gravity.TOP));
        wrapper.addView(bottomGlass, barLp(Gravity.BOTTOM));
        switcher = new FrameLayout(this); switcher.setVisibility(View.GONE);
        wrapper.addView(switcher, new FrameLayout.LayoutParams(-1, -1));
        wrapper.setOnApplyWindowInsetsListener((v, ins) -> {
            topInset = ins.getSystemWindowInsetTop(); botInset = ins.getSystemWindowInsetBottom();
            topGlass.setLayoutParams(barLp(Gravity.TOP)); bottomGlass.setLayoutParams(barLp(Gravity.BOTTOM));
            Tab t = cur(); if (t != null && t.url.isEmpty()) buildHome(t.priv);
            return ins;
        });
        setContentView(wrapper);
        wrapper.requestApplyInsets();
        home = new FrameLayout(this);
        handle(getIntent());
    }

    FrameLayout.LayoutParams barLp(int grav) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, grav);
        if (grav == Gravity.TOP) lp.setMargins(dp(12), topInset + dp(8), dp(12), 0);
        else lp.setMargins(dp(12), 0, dp(12), botInset + dp(8));
        return lp;
    }

    @Override public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN && barsShown) scheduleHide();
        return super.dispatchTouchEvent(e);
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
        super.onDestroy();
    }

    @Override public void onBackPressed() {
        if (customView != null) { hideCustom(); return; }
        if (switcher.getVisibility() == View.VISIBLE) { switcher.setVisibility(View.GONE); return; }
        Tab t = cur();
        if (t != null && !t.url.isEmpty()) { goBack(); return; }
        if (tabs.size() > 1) { closeTab(cur); return; }
        super.onBackPressed();
    }

    // ---------- UI ----------
    // Frosted-glass floating panel: blurs whatever is behind it
    class Glass extends FrameLayout {
        View source; final int tint; Bitmap bmp; final float rad;
        final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Rect dst = new Rect(); final RectF rf = new RectF();
        final Runnable tick = new Runnable() {
            @Override public void run() {
                if (isShown() && source != null) { refresh(); invalidate(); }
                postDelayed(this, 140);
            }
        };

        Glass(int tint, int radiusDp) {
            super(MainActivity.this);
            this.tint = tint; this.rad = dp(radiusDp); setWillNotDraw(false);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View v, Outline o) { o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), rad); }
            });
            setClipToOutline(true);
            stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeWidth(dp(1)); stroke.setColor(0x55FFFFFF);
        }

        @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); post(tick); }
        @Override protected void onDetachedFromWindow() { removeCallbacks(tick); super.onDetachedFromWindow(); }

        void refresh() {
            int w = getWidth(), h = getHeight(); if (w == 0 || h == 0) return;
            int bw = Math.max(1, w / 14), bh = Math.max(1, h / 14);
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

        @Override protected void onDraw(Canvas canvas) {
            if (bmp != null) { dst.set(0, 0, getWidth(), getHeight()); canvas.drawBitmap(bmp, null, dst, paint); }
            canvas.drawColor(tint);
            rf.set(0, 0, getWidth(), getHeight());
            canvas.drawRoundRect(rf, rad, rad, stroke);
        }
    }

    // ---------- auto-hide bars ----------
    void showBars() {
        hnd.removeCallbacks(hideRun);
        if (!barsShown) {
            barsShown = true;
            topGlass.setVisibility(View.VISIBLE); bottomGlass.setVisibility(View.VISIBLE);
            topGlass.animate().translationY(0).alpha(1f).setDuration(220).start();
            bottomGlass.animate().translationY(0).alpha(1f).setDuration(220).start();
        }
        scheduleHide();
    }

    void scheduleHide() {
        hnd.removeCallbacks(hideRun);
        Tab t = cur();
        if (t != null && !t.url.isEmpty() && !urlField.hasFocus()) hnd.postDelayed(hideRun, 3000);
    }

    void hideBars() {
        if (!barsShown || urlField.hasFocus()) return;
        barsShown = false;
        topGlass.animate().translationY(-(topGlass.getHeight() + topInset + dp(20))).alpha(0f).setDuration(250)
            .withEndAction(() -> { if (!barsShown) topGlass.setVisibility(View.INVISIBLE); }).start();
        bottomGlass.animate().translationY(bottomGlass.getHeight() + botInset + dp(20)).alpha(0f).setDuration(250)
            .withEndAction(() -> { if (!barsShown) bottomGlass.setVisibility(View.INVISIBLE); }).start();
    }

    Glass buildTop() {
        Glass g = new Glass(0xAAFFFFFF, 26);
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), 0, dp(6), 0);
        urlField = new EditText(this); urlField.setBackground(null); urlField.setSingleLine(true);
        urlField.setHint("Search or enter address"); urlField.setHintTextColor(0xFF555555);
        urlField.setTextColor(INK); urlField.setTextSize(15); urlField.setSelectAllOnFocus(true);
        urlField.setImeOptions(EditorInfo.IME_ACTION_GO);
        urlField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        urlField.setOnEditorActionListener((v, a, e) -> { load(v.getText().toString()); return true; });
        urlField.setOnFocusChangeListener((v, f) -> showBars());
        row.addView(urlField, new LinearLayout.LayoutParams(0, dp(48), 1f));
        TextView reload = btn("\u21BB", v -> {
            Tab t = cur(); if (t == null || t.url.isEmpty()) return;
            if (bar.getVisibility() == View.VISIBLE) t.web.stopLoading(); else t.web.reload();
        });
        reload.setTextColor(INK);
        row.addView(reload, new LinearLayout.LayoutParams(dp(44), dp(44)));
        g.addView(row, new FrameLayout.LayoutParams(-1, dp(52)));
        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100); bar.setProgressTintList(ColorStateList.valueOf(PINK)); bar.setVisibility(View.INVISIBLE);
        g.addView(bar, new FrameLayout.LayoutParams(-1, dp(3), Gravity.BOTTOM));
        return g;
    }

    Glass buildBottom() {
        Glass g = new Glass(0xAA111214, 32);
        LinearLayout b = new LinearLayout(this); b.setGravity(Gravity.CENTER_VERTICAL);
        b.setPadding(dp(8), dp(6), dp(8), dp(6));
        backBtn = btn("\u2039", v -> goBack()); backBtn.setTextSize(34);
        fwdBtn = btn("\u203A", v -> { Tab t = cur(); if (t != null && t.web.canGoForward()) t.web.goForward(); });
        fwdBtn.setTextSize(34);
        b.addView(backBtn, new LinearLayout.LayoutParams(0, dp(48), 1f));
        b.addView(fwdBtn, new LinearLayout.LayoutParams(0, dp(48), 1f));
        FrameLayout plusCell = new FrameLayout(this);
        TextView plus = circle("+", 48, WHITE, INK, v -> { newTab(null, false); showKb(); });
        plus.setElevation(0);
        plusCell.addView(plus, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.CENTER));
        b.addView(plusCell, new LinearLayout.LayoutParams(0, dp(48), 1f));
        FrameLayout tabCell = new FrameLayout(this);
        tabCount = tv("1", 13, WHITE, true); tabCount.setGravity(Gravity.CENTER);
        GradientDrawable box = rounded(0, 8); box.setStroke(dp(2), WHITE); tabCount.setBackground(box);
        tabCount.setOnClickListener(v -> openSwitcher());
        tabCell.addView(tabCount, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        b.addView(tabCell, new LinearLayout.LayoutParams(0, dp(48), 1f));
        b.addView(btn("\u22EF", v -> menu()), new LinearLayout.LayoutParams(0, dp(48), 1f));
        g.addView(b, new FrameLayout.LayoutParams(-1, -2));
        return g;
    }

    void buildHome(boolean priv) {
        home.removeAllViews();
        int[] cols = priv ? new int[]{0xFF3B2150, 0xFF1A0F26, 0xFF0E0F11} : new int[]{0xFF4A4D52, 0xFF1A1B1E, 0xFF0E0F11};
        home.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, cols));
        int H = getResources().getDisplayMetrics().heightPixels;

        TextView year = tv(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)), 96, 0x20FFFFFF, true);
        year.setLetterSpacing(-0.05f);
        FrameLayout.LayoutParams yp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        yp.bottomMargin = (int) (H * .40); home.addView(year, yp);

        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL); hero.setPadding(dp(24), dp(20) + topInset + dp(64), dp(24), 0);
        TextView brand = tv(priv ? "Private" : "Storm", 60, WHITE, true);
        brand.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL)); brand.setLetterSpacing(-0.04f);
        hero.addView(brand);
        SpannableString ss = new SpannableString("You choose the idea.\nWe handle the web from there.");
        int s = ss.toString().indexOf("idea");
        ss.setSpan(new ForegroundColorSpan(PINK), s, s + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new StyleSpan(Typeface.ITALIC), s, s + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new TypefaceSpan("serif"), s, s + 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView tag = tv("", 20, WHITE, true); tag.setText(ss); tag.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2); tp.topMargin = dp(10);
        hero.addView(tag, tp);
        TextView cta = tv("Start browsing  \u2192", 16, INK, true); cta.setBackground(rounded(WHITE, 28));
        cta.setPadding(dp(30), dp(16), dp(30), dp(16)); cta.setOnClickListener(v -> showKb());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2); cp.topMargin = dp(22);
        hero.addView(cta, cp);
        home.addView(hero, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        ScrollView sheet = new ScrollView(this);
        float r = dp(36);
        GradientDrawable sb = new GradientDrawable(); sb.setColor(WHITE);
        sb.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        sheet.setBackground(sb);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(24), dp(22), dp(24), dp(28) + botInset + dp(90));
        TextView where = tv("Where", 44, PINK, false);
        where.setTypeface(Typeface.create("serif", Typeface.ITALIC)); col.addView(where);
        TextView q = tv("would you like to go?", 26, INK, true); col.addView(q);
        TextView d = tv("Pick a favorite or type anywhere \u2014 we'll take care of the rest. No clutter, no trackers.", 15, 0xFF444444, false);
        LinearLayout.LayoutParams dp1 = new LinearLayout.LayoutParams(-2, -2); dp1.topMargin = dp(10);
        col.addView(d, dp1);
        TextView it = tv("You don't need to remember every address.", 15, INK, false);
        it.setTypeface(Typeface.create("serif", Typeface.ITALIC)); col.addView(it, dp1);

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
            c.setBackground(rounded(Color.parseColor(QUICK[i][2]), 22)); c.setOnClickListener(v -> load(url));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
            if (i % 2 == 1) lp.leftMargin = dp(10);
            row.addView(c, lp);
        }
        ArrayList<String[]> bms = read("bm");
        if (!bms.isEmpty()) {
            TextView h = tv("Bookmarks", 18, INK, true);
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-2, -2); hp.topMargin = dp(22);
            col.addView(h, hp);
            for (int i = 0; i < Math.min(6, bms.size()); i++) {
                final String url = bms.get(i)[1];
                TextView e = tv(bms.get(i)[0], 15, INK, false); e.setSingleLine(true); e.setEllipsize(TextUtils.TruncateAt.END);
                e.setPadding(0, dp(10), 0, dp(10)); e.setOnClickListener(v -> load(url)); col.addView(e);
            }
        }
        sheet.addView(col);
        home.addView(sheet, new FrameLayout.LayoutParams(-1, (int) (H * .46), Gravity.BOTTOM));
    }

    // ---------- tabs ----------
    Tab cur() { return cur >= 0 && cur < tabs.size() ? tabs.get(cur) : null; }

    void newTab(String url, boolean priv) {
        Tab t = new Tab(); t.priv = priv; t.web = makeWeb(t); tabs.add(t);
        select(tabs.size() - 1);
        if (url != null) load(url);
        switcher.setVisibility(View.GONE);
    }

    void select(int i) {
        Tab p = cur(); if (p != null) snap(p);
        cur = i; showCurrent();
    }

    void showCurrent() {
        Tab t = cur(); content.removeAllViews();
        if (t.url.isEmpty()) {
            buildHome(t.priv); content.addView(home, new FrameLayout.LayoutParams(-1, -1)); urlField.setText("");
        } else {
            content.addView(t.web, new FrameLayout.LayoutParams(-1, -1)); urlField.setText(t.url);
        }
        bar.setVisibility(View.INVISIBLE); refreshUI(); showBars();
    }

    void refreshUI() {
        Tab t = cur(); if (t == null) return;
        tabCount.setText(String.valueOf(tabs.size()));
        backBtn.setAlpha(!t.url.isEmpty() ? 1f : .35f);
        fwdBtn.setAlpha(!t.url.isEmpty() && t.web.canGoForward() ? 1f : .35f);
    }

    void load(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        Tab t = cur(); String u = fix(raw);
        t.url = u; t.web.loadUrl(u); showCurrent(); hideKb();
    }

    void goBack() {
        Tab t = cur();
        if (t.web.canGoBack()) t.web.goBack(); else { t.url = ""; showCurrent(); }
    }

    void closeTab(int i) {
        Tab t = tabs.remove(i);
        ViewGroup p = (ViewGroup) t.web.getParent(); if (p != null) p.removeView(t.web);
        t.web.destroy();
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

    // ---------- tab switcher ----------
    void openSwitcher() {
        Tab c = cur(); if (c != null) snap(c);
        buildSwitcher(); switcher.setVisibility(View.VISIBLE); hideKb();
    }

    void buildSwitcher() {
        switcher.removeAllViews(); switcher.setBackgroundColor(0xFFEFEFEF);
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(10), dp(20), dp(10), dp(140));
        for (int i = 0; i < tabs.size(); i += 2) {
            LinearLayout row = new LinearLayout(this);
            for (int j = i; j < i + 2; j++) {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(250), 1f);
                lp.setMargins(dp(8), dp(8), dp(8), dp(8));
                if (j < tabs.size()) row.addView(tabCard(j), lp); else row.addView(new View(this), lp);
            }
            col.addView(row);
        }
        sv.addView(col);
        switcher.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams p1 = new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        p1.bottomMargin = dp(28);
        switcher.addView(circle("+", 64, INK, WHITE, v -> { newTab(null, false); showKb(); }), p1);
        FrameLayout.LayoutParams p2 = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.BOTTOM | Gravity.START);
        p2.bottomMargin = dp(34); p2.leftMargin = dp(24);
        switcher.addView(circle("\u21A9", 52, WHITE, INK, v -> switcher.setVisibility(View.GONE)), p2);
        FrameLayout.LayoutParams p3 = new FrameLayout.LayoutParams(dp(52), dp(52), Gravity.BOTTOM | Gravity.END);
        p3.bottomMargin = dp(34); p3.rightMargin = dp(24);
        switcher.addView(circle("\uD83D\uDDD1", 52, WHITE, INK, v -> {
            for (Tab t : tabs) { ViewGroup p = (ViewGroup) t.web.getParent(); if (p != null) p.removeView(t.web); t.web.destroy(); }
            tabs.clear(); cur = -1; newTab(null, false);
        }), p3);
    }

    View tabCard(int i) {
        Tab t = tabs.get(i);
        FrameLayout f = new FrameLayout(this);
        GradientDrawable bg = rounded(t.priv ? 0xFF3B2150 : INK, 22);
        if (i == cur) bg.setStroke(dp(3), PINK);
        f.setBackground(bg); f.setClipToOutline(true); f.setElevation(dp(6));
        f.setRotation(i % 2 == 0 ? -1.5f : 1.5f);
        LinearLayout in = new LinearLayout(this); in.setOrientation(LinearLayout.VERTICAL);
        ImageView iv = new ImageView(this); iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (t.thumb != null) iv.setImageBitmap(t.thumb);
        else iv.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFF4A4D52, 0xFF1A1B1E}));
        in.addView(iv, new LinearLayout.LayoutParams(-1, 0, 1f));
        TextView title = tv(t.url.isEmpty() ? (t.priv ? "Private tab" : "New tab") : t.title, 13, WHITE, true);
        title.setSingleLine(true); title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(dp(14), dp(10), dp(40), dp(12)); in.addView(title);
        f.addView(in, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams xp = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.TOP | Gravity.END);
        xp.setMargins(0, dp(8), dp(8), 0);
        TextView x = tv("\u2715", 12, WHITE, false); x.setGravity(Gravity.CENTER); x.setBackground(oval(0x99000000));
        final int idx = i;
        x.setOnClickListener(v -> { closeTab(idx); buildSwitcher(); });
        f.addView(x, xp);
        f.setOnClickListener(v -> { select(idx); switcher.setVisibility(View.GONE); });
        return f;
    }

    // ---------- menu ----------
    void menu() {
        final Tab t = cur();
        String[] items = {"New tab", "New private tab", "Add bookmark", "Bookmarks", "History",
            "Desktop site: " + (t.desktop ? "on" : "off"), "Ad blocker: " + (blockAds ? "on" : "off"),
            "Search engine: " + ENGINES[eng()][0], "Share page", "Clear browsing data"};
        dlg().setItems(items, (d, w) -> {
            switch (w) {
                case 0: newTab(null, false); showKb(); break;
                case 1: newTab(null, true); showKb(); break;
                case 2:
                    if (t.url.isEmpty()) { toast("Open a page first"); break; }
                    ArrayList<String[]> l = read("bm"); l.add(0, new String[]{t.title, t.url}); write("bm", l);
                    toast("Bookmarked"); break;
                case 3: showList("bm", "Bookmarks"); break;
                case 4: showList("hist", "History"); break;
                case 5:
                    t.desktop = !t.desktop; t.web.getSettings().setUserAgentString(t.desktop ? DESKTOP_UA : null);
                    if (!t.url.isEmpty()) t.web.reload(); break;
                case 6: blockAds = !blockAds; sp.edit().putBoolean("ads", blockAds).apply(); toast("Ad blocker " + (blockAds ? "on" : "off")); break;
                case 7:
                    String[] names = new String[ENGINES.length];
                    for (int i = 0; i < names.length; i++) names[i] = ENGINES[i][0];
                    dlg().setTitle("Search engine").setItems(names, (d2, k) -> sp.edit().putInt("se", k).apply()).show(); break;
                case 8:
                    if (t.url.isEmpty()) break;
                    Intent s = new Intent(Intent.ACTION_SEND); s.setType("text/plain"); s.putExtra(Intent.EXTRA_TEXT, t.url);
                    startActivity(Intent.createChooser(s, "Share")); break;
                case 9:
                    CookieManager.getInstance().removeAllCookies(null); WebStorage.getInstance().deleteAllData();
                    for (Tab x : tabs) x.web.clearCache(true);
                    sp.edit().remove("hist").apply(); toast("Cleared"); break;
            }
        }).show();
    }

    void showList(String key, String title) {
        final ArrayList<String[]> l = read(key);
        if (l.isEmpty()) { toast("Nothing here yet"); return; }
        String[] names = new String[l.size()];
        for (int i = 0; i < names.length; i++) names[i] = l.get(i)[0] + "\n" + l.get(i)[1];
        dlg().setTitle(title).setItems(names, (d, w) -> load(l.get(w)[1]))
            .setNegativeButton("Clear", (d, w) -> sp.edit().remove(key).apply())
            .setPositiveButton("Close", null).show();
    }

    // ---------- web ----------
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
        s.setMediaPlaybackRequiresUserGesture(false);
        if (t.priv) { s.setCacheMode(WebSettings.LOAD_NO_CACHE); s.setSaveFormData(false); }
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, false);
        w.setBackgroundColor(WHITE);
        w.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) { dx0 = e.getX(); dy0 = e.getY(); dt0 = e.getEventTime(); }
            else if (a == MotionEvent.ACTION_UP && t == cur() && Math.abs(e.getX() - dx0) < dp(10)
                && Math.abs(e.getY() - dy0) < dp(10) && e.getEventTime() - dt0 < 350) showBars();
            return false;
        });
        w.setOnScrollChangeListener((v, sx, sy, ox, oy) -> {
            if (t != cur()) return;
            int d = sy - oy;
            if (d > dp(10)) hideBars(); else if (d < -dp(10)) showBars();
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
                t.url = url; bar.setVisibility(View.VISIBLE);
                if (t == cur()) { urlField.setText(url); refreshUI(); showBars(); }
            }
            @Override public void onPageFinished(WebView v, String url) {
                t.title = v.getTitle() == null ? url : v.getTitle();
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
