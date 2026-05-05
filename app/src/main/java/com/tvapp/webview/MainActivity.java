package com.tvapp.webview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS_NAME      = "TVWebAppPrefs";
    private static final String KEY_URL         = "saved_url";
    private static final String KEY_PERM_ASKED  = "perm_asked";
    private static final int    REQ_BATTERY     = 101;
    private static final int    REQ_OVERLAY     = 102;
    private long backPressedTime = 0;

    private WebView     webView;
    private ProgressBar progressBar;
    private View        setupScreen;
    private View        webviewScreen;
    private EditText    urlInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        // Screensaver va ekran o'chishini bloklash
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );

        setContentView(R.layout.activity_main);

        setupScreen   = findViewById(R.id.setup_screen);
        webviewScreen = findViewById(R.id.webview_screen);
        webView       = findViewById(R.id.webview);
        progressBar   = findViewById(R.id.progressBar);
        urlInput      = findViewById(R.id.url_input);

        // Ruxsatlar so'rash zanjiri boshlaydi
        askPermissions();
    }

    // ─── Ruxsatlar zanjiri ───────────────────────────────────────────────────
    // 1. Battery optimization → 2. Overlay → 3. Dastur

    private void askPermissions() {
        boolean alreadyAsked = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERM_ASKED, false);

        if (alreadyAsked) {
            // Ruxsatlar avval so'ralgan — to'g'ri dasturga o'tamiz
            proceedToApp();
            return;
        }

        // Birinchi marta — battery ruxsat so'raymiz
        askBatteryPermission();
    }

    // 1-qadam: Battery optimization
    private void askBatteryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_BATTERY);
                return; // onActivityResult → askOverlayPermission
            }
        }
        // Battery ruxsat kerak emas yoki allaqachon berilgan
        askOverlayPermission();
    }

    // 2-qadam: Overlay (ustidan ochilish)
    private void askOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQ_OVERLAY);
                return; // onActivityResult → proceedToApp
            }
        }
        // Overlay ruxsat kerak emas yoki allaqachon berilgan
        finishPermissions();
    }

    // 3-qadam: Ruxsatlar tugadi
    private void finishPermissions() {
        // Keyingi ochilganda so'ralmasin
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PERM_ASKED, true).apply();
        proceedToApp();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_BATTERY) {
            // Battery — Allow yoki Deny, farqi yo'q — overlay ga o'tamiz
            askOverlayPermission();

        } else if (requestCode == REQ_OVERLAY) {
            // Overlay — Allow yoki Deny, farqi yo'q — davom etamiz
            finishPermissions();
        }
    }

    // ─── Ruxsatdan keyin — URL yoki WebView ──────────────────────────────────

    private void proceedToApp() {
        String savedUrl = getSavedUrl();
        if (TextUtils.isEmpty(savedUrl)) {
            showSetupScreen();
        } else {
            showWebView(savedUrl);
        }
    }

    // ─── Setup ekrani ─────────────────────────────────────────────────────────

    private void showSetupScreen() {
        setupScreen.setVisibility(View.VISIBLE);
        webviewScreen.setVisibility(View.GONE);

        Button   btnSave = findViewById(R.id.btn_save);
        TextView tvError = findViewById(R.id.tv_error);

        btnSave.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            if (TextUtils.isEmpty(url)) {
                tvError.setText("URL kiriting!");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            tvError.setVisibility(View.GONE);
            saveUrl(url);
            showWebView(url);
        });

        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE) {
                btnSave.performClick();
                return true;
            }
            return false;
        });

        urlInput.requestFocus();
        urlInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(urlInput, InputMethodManager.SHOW_IMPLICIT);
        }, 300);
    }

    // ─── WebView ekrani ───────────────────────────────────────────────────────

    @SuppressLint("SetJavaScriptEnabled")
    private void showWebView(String url) {
        setupScreen.setVisibility(View.GONE);
        webviewScreen.setVisibility(View.VISIBLE);
        setupWebViewSettings();
        webView.loadUrl(url);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebViewSettings() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 11; Smart TV) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/91.0.4472.120 Safari/537.36 TVWebApp/1.0"
        );

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                view.loadUrl(req.getUrl().toString());
                return true;
            }
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String desc, String failingUrl) {
                view.loadUrl("file:///android_asset/error.html");
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                if (newProgress == 100) progressBar.setVisibility(View.GONE);
            }
        });
    }

    // ─── SharedPreferences ────────────────────────────────────────────────────

    private void saveUrl(String url) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_URL, url).apply();
    }

    private String getSavedUrl() {
        return getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URL, "");
    }

    // ─── TV pult ─────────────────────────────────────────────────────────────
    // Setup ekranida: orqaga = dasturdan chiqish
    // WebView da: orqaga = tarixda orqaga (chiqmaslik)

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (setupScreen.getVisibility() == View.VISIBLE) {
                // URL ekranida — ikki marta bosish kerak
                if (doubleBackToExit()) return true;
            }

            if (webviewScreen.getVisibility() == View.VISIBLE) {
                if (webView.canGoBack()) {
                    // Tarix bor — orqaga
                    webView.goBack();
                    return true;
                }
                // Tarix yo'q — ikki marta bosish kerak
                if (doubleBackToExit()) return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_F5 || keyCode == KeyEvent.KEYCODE_REFRESH) {
            if (webviewScreen.getVisibility() == View.VISIBLE) {
                webView.reload();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    // Ikki marta bosish — chiqish
    // true = birinchi bosish (toast chiqdi), false = ikkinchi bosish (chiqish)
    private boolean doubleBackToExit() {
        long now = System.currentTimeMillis();
        if (now - backPressedTime < 2000) {
            // 2 soniya ichida ikkinchi marta — chiqish
            finish();
            return false;
        }
        // Birinchi marta — ogohlantirish
        backPressedTime = now;
        Toast.makeText(this, "Chiqish uchun yana bir marta bosing", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override protected void onResume()  { super.onResume();  if (webView != null) webView.onResume(); }
    @Override protected void onPause()   { super.onPause();   if (webView != null) webView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (webView != null) webView.destroy(); }
}
