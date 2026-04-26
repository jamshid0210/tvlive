package com.tvapp.webview;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
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

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "TVWebAppPrefs";
    private static final String KEY_URL    = "saved_url";

    private WebView     webView;
    private ProgressBar progressBar;
    private View        setupScreen;
    private View        webviewScreen;
    private EditText    urlInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // To'liq ekran
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
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

            // https:// yo'q bo'lsa qo'shamiz
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            tvError.setVisibility(View.GONE);
            saveUrl(url);
            showWebView(url);
        });

        // Enter bosilganda saqlash
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO ||
                actionId == EditorInfo.IME_ACTION_DONE) {
                btnSave.performClick();
                return true;
            }
            return false;
        });

        // Klaviaturani ko'rsat
        urlInput.requestFocus();
        urlInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(urlInput, InputMethodManager.SHOW_IMPLICIT);
            }
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
            "Mozilla/5.0 (Linux; Android 10; Smart TV) " +
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

    // ─── TV pult tugmalari ────────────────────────────────────────────────────

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webviewScreen.getVisibility() == View.VISIBLE) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) webView.goBack();
                return true; // ilovadan chiqmaslik
            }
            if (keyCode == KeyEvent.KEYCODE_F5 || keyCode == KeyEvent.KEYCODE_REFRESH) {
                webView.reload();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onResume()  { super.onResume();  if (webView != null) webView.onResume(); }
    @Override protected void onPause()   { super.onPause();   if (webView != null) webView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (webView != null) webView.destroy(); }
}
