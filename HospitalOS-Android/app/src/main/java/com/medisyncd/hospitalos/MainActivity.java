package com.medisyncd.hospitalos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String PREFS = "medisyncd_hospitalos_settings";
    private static final String KEY_AI_ENDPOINT = "ai_endpoint";
    private static final String KEY_AI_TOKEN = "ai_gateway_token";
    private static final String KEY_SCREEN_SECURE = "screen_secure";
    private static final int REQ_FILE_CHOOSER = 2001;
    private static final int REQ_CREATE_DOCUMENT = 2002;
    private static final int REQ_WEB_PERMISSIONS = 2003;

    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private SharedPreferences prefs;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingExportText;
    private PermissionRequest pendingPermissionRequest;
    private String[] pendingPermissionResources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applyScreenSecurity();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(13, 34, 68));
        setContentView(root);

        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        ImageButton settingsButton = new ImageButton(this);
        settingsButton.setImageResource(android.R.drawable.ic_menu_manage);
        settingsButton.setContentDescription("HospitalOS settings");
        settingsButton.setBackgroundColor(Color.argb(210, 255, 255, 255));
        settingsButton.setPadding(dp(10), dp(10), dp(10), dp(10));
        FrameLayout.LayoutParams gearParams = new FrameLayout.LayoutParams(dp(48), dp(48));
        gearParams.gravity = Gravity.END | Gravity.BOTTOM;
        gearParams.setMargins(dp(12), dp(12), dp(14), dp(18));
        root.addView(settingsButton, gearParams);
        settingsButton.setOnClickListener(v -> showSettingsDialog());

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left, top, right, bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left; top = bars.top; right = bars.right; bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, top, right, bottom);
            return insets;
        });
        root.requestApplyInsets();

        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        configureWebView();

        if (savedInstanceState == null) {
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setGeolocationEnabled(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " MediSyncD-HospitalOS/4.1.0");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) settings.setSafeBrowsingEnabled(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        webView.addJavascriptInterface(new NativeBridge(), "MediSyncDAndroid");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse response = assetLoader.shouldInterceptRequest(request.getUrl());
                return response != null ? response : super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this, "HospitalOS could not load this page.", Toast.LENGTH_LONG).show();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent;
                try {
                    intent = params.createIntent();
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                } catch (Exception e) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                }
                try {
                    startActivityForResult(intent, REQ_FILE_CHOOSER);
                    return true;
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "No file picker is available.", Toast.LENGTH_LONG).show();
                    return false;
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermissionRequest(request));
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if (url == null) return;
                if (url.startsWith("blob:")) {
                    Toast.makeText(MainActivity.this, "Use the HospitalOS export button to save this file.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (!url.startsWith("https://")) {
                    Toast.makeText(MainActivity.this, "Blocked an insecure download.", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
                    String name = URLUtil.guessFileName(url, contentDisposition, mimetype);
                    req.setTitle(name);
                    req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                    req.setMimeType(mimetype);
                    if (userAgent != null) req.addRequestHeader("User-Agent", userAgent);
                    ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(req);
                    Toast.makeText(MainActivity.this, "Download started", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not start download.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if ("https".equals(scheme) && "appassets.androidplatform.net".equals(host)) return false;
        if ("about".equals(scheme)) return false;
        if ("https".equals(scheme) || "http".equals(scheme) || "mailto".equals(scheme) || "tel".equals(scheme)) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
            catch (Exception e) { Toast.makeText(this, "No app is available to open that link.", Toast.LENGTH_SHORT).show(); }
            return true;
        }
        return true;
    }

    private void handleWebPermissionRequest(PermissionRequest request) {
        if (request.getOrigin() == null ||
                !"appassets.androidplatform.net".equalsIgnoreCase(request.getOrigin().getHost())) {
            request.deny();
            return;
        }

        List<String> androidPermissions = new ArrayList<>();
        List<String> allowedResources = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                androidPermissions.add(Manifest.permission.RECORD_AUDIO);
                allowedResources.add(resource);
            } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                androidPermissions.add(Manifest.permission.CAMERA);
                allowedResources.add(resource);
            }
        }
        if (allowedResources.isEmpty()) { request.deny(); return; }

        List<String> missing = new ArrayList<>();
        for (String permission : androidPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) missing.add(permission);
        }
        if (missing.isEmpty()) {
            request.grant(allowedResources.toArray(new String[0]));
        } else {
            pendingPermissionRequest = request;
            pendingPermissionResources = allowedResources.toArray(new String[0]);
            requestPermissions(missing.toArray(new String[0]), REQ_WEB_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_WEB_PERMISSIONS && pendingPermissionRequest != null) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) { granted = false; break; }
            }
            if (granted && pendingPermissionResources != null) pendingPermissionRequest.grant(pendingPermissionResources);
            else pendingPermissionRequest.deny();
            pendingPermissionRequest = null;
            pendingPermissionResources = null;
        }
    }

    private void showSettingsDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, pad / 2, pad, 0);

        TextView help = new TextView(this);
        help.setText("Domonique 2.0 uses a secure HTTPS gateway so API keys are never stored in the APK. Enter your gateway URL and optional gateway access token.");
        help.setTextSize(14);
        box.addView(help);

        EditText endpoint = new EditText(this);
        endpoint.setHint("https://…/v1/messages");
        endpoint.setSingleLine(true);
        endpoint.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        endpoint.setText(prefs.getString(KEY_AI_ENDPOINT, ""));
        box.addView(endpoint);

        EditText gatewayToken = new EditText(this);
        gatewayToken.setHint("Optional gateway access token");
        gatewayToken.setSingleLine(true);
        gatewayToken.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        gatewayToken.setText(prefs.getString(KEY_AI_TOKEN, ""));
        box.addView(gatewayToken);

        CheckBox secureScreen = new CheckBox(this);
        secureScreen.setText("Protect clinical screens from screenshots/screen recording");
        secureScreen.setChecked(prefs.getBoolean(KEY_SCREEN_SECURE, true));
        box.addView(secureScreen);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("MediSyncD HospitalOS Settings")
                .setView(box)
                .setPositiveButton("Save", null)
                .setNeutralButton("App permissions", (d, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = endpoint.getText().toString().trim();
            if (!value.isEmpty() && !value.startsWith("https://")) {
                endpoint.setError("Use an HTTPS gateway URL.");
                return;
            }
            prefs.edit()
                    .putString(KEY_AI_ENDPOINT, value)
                    .putString(KEY_AI_TOKEN, gatewayToken.getText().toString())
                    .putBoolean(KEY_SCREEN_SECURE, secureScreen.isChecked())
                    .apply();
            applyScreenSecurity();
            Toast.makeText(this,
                    value.isEmpty() ? "Saved. AI gateway is not configured yet." : "Saved. Domonique 2.0 gateway configured.",
                    Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void applyScreenSecurity() {
        boolean secure = prefs != null && prefs.getBoolean(KEY_SCREEN_SECURE, true);
        if (secure) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = cm.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = cm.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (Exception e) {
            return false;
        }
    }

    private void saveTextWithPicker(String filename, String text, String mime) {
        pendingExportText = text;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType((mime == null || mime.isEmpty()) ? "text/plain" : mime);
        intent.putExtra(Intent.EXTRA_TITLE, sanitizeFilename(filename));
        try {
            startActivityForResult(intent, REQ_CREATE_DOCUMENT);
        } catch (Exception e) {
            pendingExportText = null;
            Toast.makeText(this, "No document provider is available to save this file.", Toast.LENGTH_LONG).show();
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) return "medisyncd-export.txt";
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE_CHOOSER) {
            if (filePathCallback != null) {
                Uri[] results = null;
                if (resultCode == RESULT_OK && data != null) {
                    Uri dataUri = data.getData();
                    if (dataUri != null) results = new Uri[]{dataUri};
                    else if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            }
        } else if (requestCode == REQ_CREATE_DOCUMENT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingExportText != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if (out != null) {
                        out.write(pendingExportText.getBytes(StandardCharsets.UTF_8));
                        out.flush();
                        Toast.makeText(this, "Export saved", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Could not save export: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            pendingExportText = null;
        }
    }

    public class NativeBridge {
        @JavascriptInterface public String getAiEndpoint() { return prefs.getString(KEY_AI_ENDPOINT, ""); }
        @JavascriptInterface public String getAiToken() { return prefs.getString(KEY_AI_TOKEN, ""); }
        @JavascriptInterface public boolean isOnline() { return MainActivity.this.isOnline(); }
        @JavascriptInterface public String getAppVersion() { return "4.1.0"; }
        @JavascriptInterface public void openSettings() { runOnUiThread(() -> showSettingsDialog()); }
        @JavascriptInterface public void saveTextFile(String filename, String text, String mime) {
            runOnUiThread(() -> saveTextWithPicker(filename, text, mime));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (pendingPermissionRequest != null) {
            pendingPermissionRequest.deny();
            pendingPermissionRequest = null;
        }
        if (webView != null) {
            webView.removeJavascriptInterface("MediSyncDAndroid");
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
