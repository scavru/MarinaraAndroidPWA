package com.marinara.android;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Activity {
    private static final String PREFS = "marinara";
    private static final String DEFAULT_URL = "";
    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int PERMISSION_REQUEST = 1002;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 1003;
    private static final String NOTIFICATION_CHANNEL_ID = "marinara_web";

    private WebView webView;
    private SharedPreferences prefs;
    private String user = "";
    private String password = "";
    private String url = DEFAULT_URL;

    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraOutputUri;
    private PermissionRequest pendingWebPermissionRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        url = prefs.getString("url", DEFAULT_URL);
        user = prefs.getString("user", "");
        password = prefs.getString("password", "");

        hideBars();
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
        }

        if (user.length() == 0 || password.length() == 0 || url.length() == 0) {
            showSettings("");
        } else {
            openWeb();
        }
    }

    private void showSettings(String message) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));
        box.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("Marinara — настройки");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        box.addView(title);

        TextView msg = new TextView(this);
        msg.setText(message.length() == 0
                ? "Укажите адрес и данные HTTP Basic Auth."
                : message);
        msg.setTextSize(16);
        msg.setTextColor(Color.DKGRAY);
        msg.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(-1, -2);
        mlp.topMargin = dp(12);
        mlp.bottomMargin = dp(16);
        box.addView(msg, mlp);

        EditText address = new EditText(this);
        address.setHint("Адрес");
        address.setSingleLine(true);
        address.setText(url);
        box.addView(address);

        EditText login = new EditText(this);
        login.setHint("Логин");
        login.setSingleLine(true);
        login.setText(user);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(-1, -2);
        llp.topMargin = dp(8);
        box.addView(login, llp);

        EditText pass = new EditText(this);
        pass.setHint("Пароль");
        pass.setSingleLine(true);
        pass.setText(password);
        pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, -2);
        plp.topMargin = dp(8);
        box.addView(pass, plp);

        Button save = new Button(this);
        save.setText("Сохранить и открыть");
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = dp(18);
        box.addView(save, slp);

        save.setOnClickListener(v -> {
            String a = address.getText().toString().trim();
            if (a.length() == 0) {
                msg.setText("Введите адрес сервера.");
                return;
            }
            if (!a.startsWith("http://") && !a.startsWith("https://")) {
                a = "http://" + a;
            }

            String l = login.getText().toString();
            String p = pass.getText().toString();

            if (l.length() == 0 || p.length() == 0) {
                msg.setText("Введите логин и пароль Basic Auth.");
                return;
            }

            url = a;
            user = l;
            password = p;

            prefs.edit()
                    .putString("url", url)
                    .putString("user", user)
                    .putString("password", password)
                    .apply();

            openWeb();
        });

        Button clear = new Button(this);
        clear.setText("Очистить данные");
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.topMargin = dp(8);
        box.addView(clear, clp);
        clear.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            url = DEFAULT_URL;
            user = "";
            password = "";
            showSettings("Данные очищены.");
        });

        setContentView(box);
    }

    private void openWeb() {
        try {
            if (webView != null) {
                webView.destroy();
            }

            webView = new WebView(this);
            setContentView(webView);

            WebSettings s = webView.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setDatabaseEnabled(true);
            s.setBuiltInZoomControls(false);
            s.setDisplayZoomControls(false);
            s.setMediaPlaybackRequiresUserGesture(false);

            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

            webView.addJavascriptInterface(new NotificationBridge(), "MarinaraAndroid");

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                                  FileChooserParams params) {
                    if (filePathCallback != null) {
                        filePathCallback.onReceiveValue(null);
                    }
                    filePathCallback = callback;

                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSION_REQUEST);
                    }

                    try {
                        Intent content = params.createIntent();
                        Intent camera = createCameraIntent();

                        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
                        chooser.putExtra(Intent.EXTRA_INTENT, content);
                        if (camera != null) {
                            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
                        }
                        startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                    } catch (Exception e) {
                        filePathCallback = null;
                        callback.onReceiveValue(null);
                    }
                    return true;
                }

                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    runOnUiThread(() -> {
                        pendingWebPermissionRequest = request;
                        java.util.ArrayList<String> permissions = new java.util.ArrayList<>();

                        for (String resource : request.getResources()) {
                            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)
                                    && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                                    != PackageManager.PERMISSION_GRANTED) {
                                permissions.add(Manifest.permission.CAMERA);
                            }
                            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)
                                    && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                                    != PackageManager.PERMISSION_GRANTED) {
                                permissions.add(Manifest.permission.RECORD_AUDIO);
                            }
                        }

                        if (permissions.isEmpty()) {
                            request.grant(request.getResources());
                        } else {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    permissions.toArray(new String[0]),
                                    PERMISSION_REQUEST);
                        }
                    });
                }

                @Override
                public void onPermissionRequestCanceled(PermissionRequest request) {
                    if (pendingWebPermissionRequest == request) {
                        pendingWebPermissionRequest = null;
                    }
                }
            });

            webView.setDownloadListener((downloadUrl, userAgent, contentDisposition, mimeType, contentLength) -> {
                try {
                    android.app.DownloadManager.Request request =
                            new android.app.DownloadManager.Request(Uri.parse(downloadUrl));
                    request.setTitle("Marinara download");
                    request.setDescription("Downloading file");
                    request.setMimeType(mimeType);
                    request.addRequestHeader("User-Agent", userAgent);
                    String cookies = CookieManager.getInstance().getCookie(downloadUrl);
                    if (cookies != null) {
                        request.addRequestHeader("Cookie", cookies);
                    }
                    if (url != null && url.length() > 0) {
                        request.addRequestHeader("Referer", url);
                    }
                    request.setNotificationVisibility(
                            android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS,
                            guessFileName(downloadUrl, contentDisposition, mimeType));
                    android.app.DownloadManager dm =
                            (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                } catch (Exception ignored) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)));
                    } catch (Exception ignored2) {
                    }
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler,
                                                       String host, String realm) {
                    handler.proceed(user, password);
                }

                @Override
                public void onPageFinished(WebView view, String pageUrl) {
                    injectNotificationBridge(view);
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request,
                                             WebResourceError error) {
                    if (request != null && request.isForMainFrame()) {
                        String text = error == null ? "Неизвестная ошибка"
                                : String.valueOf(error.getDescription());
                        showError(text);
                    }
                }
            });

            webView.loadUrl(url);
        } catch (Exception e) {
            showError(e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage()));
        }
    }

    private Intent createCameraIntent() {
        try {
            File dir = new File(getCacheDir(), "camera");
            if (!dir.exists() && !dir.mkdirs()) return null;
            File file = File.createTempFile("camera_", ".jpg", dir);
            cameraOutputUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return intent;
        } catch (IOException e) {
            return null;
        }
    }

    private void injectNotificationBridge(WebView view) {
        String js =
                "(function() {" +
                "if(window.__marinaraNotifInstalled)return;" +
                "window.__marinaraNotifInstalled=true;" +
                "window.Notification=function(title,options){" +
                "options=options||{};" +
                "try{window.MarinaraAndroid.notify(String(title||''),String(options.body||''));}catch(e){}" +
                "};" +
                "window.Notification.permission='granted';" +
                "window.Notification.requestPermission=function(cb){" +
                "var p='granted'; if(cb) cb(p); return Promise.resolve(p);" +
                "};" +
                "})();";
        view.evaluateJavascript(js, null);
    }

    private class NotificationBridge {
        @android.webkit.JavascriptInterface
        public void notify(String title, String body) {
            showNativeNotification(title, body);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, "Marinara notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private void showNativeNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                : new Notification.Builder(this);

        builder.setSmallIcon(com.marinara.android.R.drawable.ic_launcher)
                .setContentTitle(title == null || title.length() == 0 ? "Marinara" : title)
                .setContentText(body == null ? "" : body)
                .setAutoCancel(true)
                .setContentIntent(pending);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private String guessFileName(String downloadUrl, String contentDisposition, String mimeType) {
        String name = "download";
        if (contentDisposition != null) {
            String marker = "filename=";
            int i = contentDisposition.toLowerCase().indexOf(marker);
            if (i >= 0) {
                name = contentDisposition.substring(i + marker.length()).trim()
                        .replace("\"", "").replace("'", "");
            }
        }
        if ("download".equals(name)) {
            try {
                String path = Uri.parse(downloadUrl).getLastPathSegment();
                if (path != null && path.length() > 0) name = path;
            } catch (Exception ignored) {
            }
        }
        if (name.equals("download") && mimeType != null) {
            if (mimeType.contains("pdf")) name += ".pdf";
            else if (mimeType.contains("image/jpeg")) name += ".jpg";
            else if (mimeType.contains("image/png")) name += ".png";
        }
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void showError(String reason) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView t = new TextView(this);
        t.setText("Не удалось открыть Marinara.\n\n" + reason);
        t.setTextSize(17);
        t.setTextColor(Color.DKGRAY);
        t.setGravity(Gravity.CENTER);
        box.addView(t);

        Button retry = new Button(this);
        retry.setText("Повторить");
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(-1, -2);
        rlp.topMargin = dp(18);
        box.addView(retry, rlp);
        retry.setOnClickListener(v -> openWeb());

        Button settings = new Button(this);
        settings.setText("Настройки");
        LinearLayout.LayoutParams stp = new LinearLayout.LayoutParams(-1, -2);
        stp.topMargin = dp(8);
        box.addView(settings, stp);
        settings.setOnClickListener(v -> showSettings("Проверьте адрес и данные Basic Auth."));

        setContentView(box);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) return;

        Uri[] results = null;
        if (resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                results = new Uri[]{data.getData()};
            } else if (cameraOutputUri != null) {
                results = new Uri[]{cameraOutputUri};
            }
        }
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
        cameraOutputUri = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST && pendingWebPermissionRequest != null) {
            PermissionRequest request = pendingWebPermissionRequest;
            pendingWebPermissionRequest = null;

            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                request.grant(request.getResources());
            } else {
                request.deny();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
