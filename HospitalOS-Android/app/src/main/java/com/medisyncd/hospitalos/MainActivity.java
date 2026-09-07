package com.medisyncd.hospitalos;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.KeyguardManager;
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
import android.provider.CalendarContract;
import android.provider.Settings;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class MainActivity extends Activity {
    private static final String PREFS = "medisyncd_hospitalos_settings";
    private static final String KEY_AI_ENDPOINT = "ai_endpoint";
    private static final String KEY_AI_TOKEN = "ai_gateway_token";
    private static final String KEY_OPENAI_ENCRYPTED = "openai_key_encrypted";
    private static final String KEY_SCREEN_SECURE = "screen_secure";
    private static final String KEY_DEVICE_AUTH = "device_auth";
    private static final String KEYSTORE_ALIAS = "medisyncd_hospitalos_openai_v1";

    private static final int REQ_FILE_CHOOSER = 2001;
    private static final int REQ_CREATE_DOCUMENT = 2002;
    private static final int REQ_WEB_PERMISSIONS = 2003;
    private static final int REQ_IMPORT_BACKUP = 2004;
    private static final int REQ_DEVICE_AUTH = 2005;

    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private SharedPreferences prefs;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingExportText;
    private PermissionRequest pendingPermissionRequest;
    private String[] pendingPermissionResources;
    private FrameLayout lockOverlay;
    private boolean authInProgress = false;
    private long backgroundedAt = 0L;
    private final ExecutorService networkExecutor = Executors.newCachedThreadPool();

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
        settingsButton.setBackgroundColor(Color.argb(225, 255, 255, 255));
        settingsButton.setPadding(dp(10), dp(10), dp(10), dp(10));
        FrameLayout.LayoutParams gearParams = new FrameLayout.LayoutParams(dp(50), dp(50));
        gearParams.gravity = Gravity.END | Gravity.BOTTOM;
        gearParams.setMargins(dp(12), dp(12), dp(14), dp(18));
        root.addView(settingsButton, gearParams);
        settingsButton.setOnClickListener(v -> showSettingsDialog());

        lockOverlay = new FrameLayout(this);
        lockOverlay.setBackgroundColor(Color.rgb(6, 14, 30));
        TextView lockText = new TextView(this);
        lockText.setText("🔒\nMediSyncD HospitalOS\nAuthentication required");
        lockText.setTextColor(Color.WHITE);
        lockText.setTextSize(22);
        lockText.setGravity(Gravity.CENTER);
        lockText.setPadding(dp(24), dp(24), dp(24), dp(24));
        lockOverlay.addView(lockText, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(lockOverlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        lockOverlay.setVisibility(View.GONE);

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

        requestDeviceAuthIfNeeded();
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
        settings.setUserAgentString(settings.getUserAgentString() + " MediSyncD-HospitalOS/4.2.0");
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

        TextView status = new TextView(this);
        String aiStatus;
        if (!prefs.getString(KEY_AI_ENDPOINT, "").trim().isEmpty()) {
            aiStatus = "✅ AI: secure gateway configured";
        } else if (hasOpenAiKey()) {
            aiStatus = "✅ AI: OpenAI key encrypted on this device";
        } else {
            aiStatus = "⚠ AI: not configured";
        }
        status.setText("MediSyncD HospitalOS v4.2\n" + aiStatus + "\nCalendar: Android handoff ready\nSlack: Android sharing ready");
        status.setTextSize(14);
        status.setPadding(0, 0, 0, dp(12));
        box.addView(status);

        TextView help = new TextView(this);
        help.setText("AI Connection: For a private standalone install, enter your own OpenAI API key. Android encrypts it with the device Keystore and it is never compiled into the APK. For organization deployment, use a secure HTTPS gateway instead.");
        help.setTextSize(13);
        box.addView(help);

        EditText openAiKey = new EditText(this);
        openAiKey.setHint(hasOpenAiKey() ? "OpenAI key already saved — leave blank to keep it" : "OpenAI API key (sk-...)");
        openAiKey.setSingleLine(true);
        openAiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(openAiKey);

        CheckBox clearAiKey = new CheckBox(this);
        clearAiKey.setText("Remove saved OpenAI key");
        box.addView(clearAiKey);

        EditText endpoint = new EditText(this);
        endpoint.setHint("Optional secure AI gateway: https://…");
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

        CheckBox deviceAuth = new CheckBox(this);
        deviceAuth.setText("Require device PIN/biometric when opening HospitalOS");
        deviceAuth.setChecked(prefs.getBoolean(KEY_DEVICE_AUTH, true));
        box.addView(deviceAuth);

        Button exportButton = new Button(this);
        exportButton.setText("Export HospitalOS Backup");
        box.addView(exportButton);

        Button importButton = new Button(this);
        importButton.setText("Restore HospitalOS Backup");
        box.addView(importButton);

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

        exportButton.setOnClickListener(v -> {
            dialog.dismiss();
            exportAllLocalData();
        });
        importButton.setOnClickListener(v -> {
            dialog.dismiss();
            openBackupForRestore();
        });

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String gateway = endpoint.getText().toString().trim();
            if (!gateway.isEmpty() && !gateway.startsWith("https://")) {
                endpoint.setError("Use an HTTPS gateway URL.");
                return;
            }

            String enteredKey = openAiKey.getText().toString().trim();
            if (!enteredKey.isEmpty() && !enteredKey.startsWith("sk-")) {
                openAiKey.setError("This does not look like an OpenAI API key.");
                return;
            }

            try {
                if (clearAiKey.isChecked()) {
                    clearOpenAiKey();
                } else if (!enteredKey.isEmpty()) {
                    saveOpenAiKey(enteredKey);
                }
            } catch (Exception e) {
                Toast.makeText(this, "Could not secure the API key: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            prefs.edit()
                    .putString(KEY_AI_ENDPOINT, gateway)
                    .putString(KEY_AI_TOKEN, gatewayToken.getText().toString())
                    .putBoolean(KEY_SCREEN_SECURE, secureScreen.isChecked())
                    .putBoolean(KEY_DEVICE_AUTH, deviceAuth.isChecked())
                    .apply();
            applyScreenSecurity();

            String msg = (!gateway.isEmpty() || hasOpenAiKey())
                    ? "Saved. Domonique 2.0 AI is configured."
                    : "Saved. Add an AI key or gateway when you want live AI.";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            dialog.dismiss();

            if (deviceAuth.isChecked()) requestDeviceAuthIfNeeded();
        }));
        dialog.show();
    }

    private SecretKey getOrCreateKeystoreKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEYSTORE_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build());
        return generator.generateKey();
    }

    private void saveOpenAiKey(String value) throws Exception {
        SecretKey key = getOrCreateKeystoreKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        String stored = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + "." +
                Base64.encodeToString(encrypted, Base64.NO_WRAP);
        prefs.edit().putString(KEY_OPENAI_ENCRYPTED, stored).apply();
    }

    private String readOpenAiKey() {
        try {
            String stored = prefs.getString(KEY_OPENAI_ENCRYPTED, "");
            if (stored == null || stored.isEmpty() || !stored.contains(".")) return "";
            String[] parts = stored.split("\\.", 2);
            byte[] iv = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(parts[1], Base64.NO_WRAP);
            SecretKey key = getOrCreateKeystoreKey();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private boolean hasOpenAiKey() {
        return !readOpenAiKey().isEmpty();
    }

    private void clearOpenAiKey() {
        prefs.edit().remove(KEY_OPENAI_ENCRYPTED).apply();
    }

    private void requestOpenAI(String requestId, String prompt, int maxTokens, String systemPrompt) {
        networkExecutor.submit(() -> {
            HttpURLConnection connection = null;
            try {
                String apiKey = readOpenAiKey();
                if (apiKey.isEmpty()) throw new Exception("No OpenAI API key is saved on this device.");

                URL url = new URL("https://api.openai.com/v1/responses");
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(90000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);

                JSONObject body = new JSONObject();
                body.put("model", "gpt-5.6-sol");
                body.put("instructions", systemPrompt == null ? "" : systemPrompt);
                body.put("input", prompt == null ? "" : prompt);
                body.put("max_output_tokens", Math.max(64, Math.min(maxTokens, 4000)));

                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(payload);
                    out.flush();
                }

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();
                String responseText = readStream(stream);
                JSONObject response = new JSONObject(responseText);

                if (code < 200 || code >= 300) {
                    JSONObject error = response.optJSONObject("error");
                    String message = error != null ? error.optString("message", "OpenAI request failed") : "OpenAI request failed";
                    throw new Exception(message + " (" + code + ")");
                }

                String output = extractOpenAiText(response);
                if (output.trim().isEmpty()) throw new Exception("OpenAI returned no text output.");
                aiResolve(requestId, output);
            } catch (Exception e) {
                aiReject(requestId, e.getMessage() == null ? "AI request failed." : e.getMessage());
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private String extractOpenAiText(JSONObject response) {
        String direct = response.optString("output_text", "");
        if (!direct.isEmpty()) return direct;

        StringBuilder out = new StringBuilder();
        JSONArray output = response.optJSONArray("output");
        if (output == null) return "";
        for (int i = 0; i < output.length(); i++) {
            JSONObject item = output.optJSONObject(i);
            if (item == null) continue;
            JSONArray content = item.optJSONArray("content");
            if (content == null) continue;
            for (int j = 0; j < content.length(); j++) {
                JSONObject c = content.optJSONObject(j);
                if (c == null) continue;
                String type = c.optString("type", "");
                if ("output_text".equals(type) || "text".equals(type)) {
                    String text = c.optString("text", "");
                    if (!text.isEmpty()) {
                        if (out.length() > 0) out.append("\n");
                        out.append(text);
                    }
                }
            }
        }
        return out.toString();
    }

    private String readStream(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    private void aiResolve(String requestId, String text) {
        if (webView == null) return;
        final String js = "window.__medisyncdAiResolve(" +
                JSONObject.quote(requestId) + "," + JSONObject.quote(text) + ");";
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(js, null);
        });
    }

    private void aiReject(String requestId, String message) {
        if (webView == null) return;
        final String js = "window.__medisyncdAiReject(" +
                JSONObject.quote(requestId) + "," + JSONObject.quote(message) + ");";
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(js, null);
        });
    }

    private boolean addCalendarEvent(String json) {
        try {
            JSONObject appt = new JSONObject(json);
            String date = appt.optString("date", "");
            String time = appt.optString("time", "09:00");
            int duration = appt.optInt("duration", 30);
            if (date.isEmpty()) return false;

            LocalDateTime local = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
            long start = local.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long end = local.plusMinutes(Math.max(5, duration)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            String patient = appt.optString("patientName", "Patient");
            String type = appt.optString("type", "Appointment");
            String doctor = appt.optString("doctorName", "");
            String notes = appt.optString("notes", "");

            Intent intent = new Intent(Intent.ACTION_INSERT);
            intent.setData(CalendarContract.Events.CONTENT_URI);
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start);
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end);
            intent.putExtra(CalendarContract.Events.TITLE, "HospitalOS — " + type + " — " + patient);
            intent.putExtra(CalendarContract.Events.DESCRIPTION,
                    (doctor.isEmpty() ? "" : "Clinician: " + doctor + "\n") +
                            (notes.isEmpty() ? "" : notes + "\n") +
                            "Created from MediSyncD HospitalOS");
            runOnUiThread(() -> {
                try { startActivity(intent); }
                catch (Exception e) { Toast.makeText(this, "No calendar app is available.", Toast.LENGTH_LONG).show(); }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shareToSlack(String channel, String message) {
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, (channel == null || channel.isEmpty() ? "" : channel + "\n") + message);
            runOnUiThread(() -> {
                try {
                    Intent slack = new Intent(share);
                    slack.setPackage("com.Slack");
                    if (slack.resolveActivity(getPackageManager()) != null) {
                        startActivity(slack);
                    } else {
                        startActivity(Intent.createChooser(share, "Share HospitalOS alert"));
                    }
                } catch (Exception e) {
                    try { startActivity(Intent.createChooser(share, "Share HospitalOS alert")); }
                    catch (Exception ignored) { Toast.makeText(this, "No sharing app is available.", Toast.LENGTH_LONG).show(); }
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void exportAllLocalData() {
        if (webView == null) return;
        String script = "(function(){const o={};for(let i=0;i<localStorage.length;i++){const k=localStorage.key(i);o[k]=localStorage.getItem(k);}return JSON.stringify(o);})()";
        webView.evaluateJavascript(script, value -> {
            try {
                if (value == null || "null".equals(value)) throw new Exception("No local data found.");
                JSONObject wrapper = new JSONObject("{\"v\":" + value + "}");
                String json = wrapper.getString("v");
                saveTextWithPicker("MediSyncD-HospitalOS-v4.2-backup.json", json, "application/json");
            } catch (Exception e) {
                Toast.makeText(this, "Could not create backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openBackupForRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try {
            startActivityForResult(intent, REQ_IMPORT_BACKUP);
        } catch (Exception e) {
            Toast.makeText(this, "No document provider is available.", Toast.LENGTH_LONG).show();
        }
    }

    private String readUriText(Uri uri) throws Exception {
        InputStream stream = getContentResolver().openInputStream(uri);
        return readStream(stream);
    }

    private void confirmRestoreBackup(String json) {
        try {
            new JSONObject(json);
        } catch (Exception e) {
            Toast.makeText(this, "That file is not a valid HospitalOS JSON backup.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Restore HospitalOS backup?")
                .setMessage("This will replace matching local HospitalOS data on this device, then reload the app.")
                .setPositiveButton("Restore", (d, which) -> restoreBackup(json))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void restoreBackup(String json) {
        try {
            String encoded = Base64.encodeToString(json.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
            String script =
                    "(function(){const b=atob('" + encoded + "');const bytes=Uint8Array.from(b,c=>c.charCodeAt(0));" +
                    "const raw=new TextDecoder().decode(bytes);const o=JSON.parse(raw);" +
                    "Object.keys(o).forEach(k=>localStorage.setItem(k,o[k]));location.reload();})()";
            webView.evaluateJavascript(script, null);
            Toast.makeText(this, "HospitalOS backup restored.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

    private void applyScreenSecurity() {
        boolean secure = prefs != null && prefs.getBoolean(KEY_SCREEN_SECURE, true);
        if (secure) getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void requestDeviceAuthIfNeeded() {
        if (prefs == null || !prefs.getBoolean(KEY_DEVICE_AUTH, true)) {
            hideLockOverlay();
            return;
        }
        KeyguardManager keyguard = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (keyguard == null || !keyguard.isDeviceSecure()) {
            hideLockOverlay();
            Toast.makeText(this, "For clinical privacy, set a device PIN/biometric lock in Android settings.", Toast.LENGTH_LONG).show();
            return;
        }
        if (authInProgress) return;
        Intent auth = keyguard.createConfirmDeviceCredentialIntent(
                "Unlock MediSyncD HospitalOS",
                "Authenticate to access clinical information.");
        if (auth == null) {
            hideLockOverlay();
            return;
        }
        showLockOverlay();
        authInProgress = true;
        startActivityForResult(auth, REQ_DEVICE_AUTH);
    }

    private void showLockOverlay() {
        if (lockOverlay != null) {
            lockOverlay.setVisibility(View.VISIBLE);
            lockOverlay.bringToFront();
        }
    }

    private void hideLockOverlay() {
        if (lockOverlay != null) lockOverlay.setVisibility(View.GONE);
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

    public class NativeBridge {
        @JavascriptInterface public String getAiEndpoint() { return prefs.getString(KEY_AI_ENDPOINT, ""); }
        @JavascriptInterface public String getAiToken() { return prefs.getString(KEY_AI_TOKEN, ""); }
        @JavascriptInterface public boolean hasOpenAiKey() { return MainActivity.this.hasOpenAiKey(); }
        @JavascriptInterface public boolean isOnline() { return MainActivity.this.isOnline(); }
        @JavascriptInterface public String getAppVersion() { return "4.2.0"; }
        @JavascriptInterface public void openSettings() { runOnUiThread(() -> showSettingsDialog()); }
        @JavascriptInterface public void saveTextFile(String filename, String text, String mime) {
            runOnUiThread(() -> saveTextWithPicker(filename, text, mime));
        }
        @JavascriptInterface public void requestOpenAI(String requestId, String prompt, int maxTokens, String systemPrompt) {
            MainActivity.this.requestOpenAI(requestId, prompt, maxTokens, systemPrompt);
        }
        @JavascriptInterface public boolean addCalendarEvent(String json) {
            return MainActivity.this.addCalendarEvent(json);
        }
        @JavascriptInterface public boolean shareToSlack(String channel, String message) {
            return MainActivity.this.shareToSlack(channel, message);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (backgroundedAt > 0 && System.currentTimeMillis() - backgroundedAt > 60000 && !authInProgress) {
            backgroundedAt = 0;
            requestDeviceAuthIfNeeded();
        }
    }

    @Override
    protected void onStop() {
        if (!authInProgress) backgroundedAt = System.currentTimeMillis();
        super.onStop();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_DEVICE_AUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                hideLockOverlay();
            } else {
                finishAndRemoveTask();
            }
            return;
        }

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
        } else if (requestCode == REQ_IMPORT_BACKUP) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                try {
                    String json = readUriText(data.getData());
                    confirmRestoreBackup(json);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not read backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        networkExecutor.shutdownNow();
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
