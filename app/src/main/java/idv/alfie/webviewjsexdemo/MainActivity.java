package idv.alfie.webviewjsexdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    WebView webView;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String photoPath;
    private ValueCallback<Uri> uriValueCallback;
    private ValueCallback<Uri[]> uriValueCallbackArray;
    private final static int FCR=1;

    @SuppressLint({"SetJavaScriptEnabled", "WrongViewCast", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /******設定全螢幕*****/
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隱藏title bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);//隱藏title bar
        getSupportActionBar().hide();//隱藏action bar
        /******設定全螢幕*****/
        setContentView(R.layout.activity_main);

        final Context context = this;

        //危險權限管理
        if(Build.VERSION.SDK_INT >=23 &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        ||ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                        ||ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        ||ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                )) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, 1);
        }

        //取得待載入的網址連結
//        String inputUrl = String.valueOf("https://developer.android.com/index.html");
        String inputUrl = String.valueOf("file:///android_asset/index.html");

        //webview環境設定
        webView = (WebView) findViewById(R.id.my_web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setGeolocationEnabled(true); //取得定位
        webView.getSettings().setGeolocationDatabasePath(getApplicationContext().getFilesDir().getPath()); //取得定位路徑
        webView.getSettings().setUseWideViewPort(true); //將圖片調整到全螢幕
        webView.getSettings().setLoadWithOverviewMode(true); //自動縮放網頁到全螢幕
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true); //使用瀏覽器看console => chrome://inspect/
        }
        if (isConnected()){
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);//有網路連線重載
        }else{
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);//沒有網路連線載入快取
            Toast.makeText(MainActivity.this,"網路連線失敗",Toast.LENGTH_LONG).show();
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            webView.getSettings().setMixedContentMode(0);
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }



        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Toast.makeText(getApplicationContext(), "網路連線錯誤", Toast.LENGTH_SHORT).show();
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();//接受憑證
                //Log.e("SSL","ERROR");
                super.onReceivedSslError(view, handler, error);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //callJs();
                callJs2("請輸入1-3");
                super.onPageFinished(view, url);
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            /*****<input type=file>點擊調用相機相簿文件夾(4.4.0-2 KITKAT版本不支援)*****/
            //For Android 4.1+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                uriValueCallback = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                MainActivity.this.startActivityForResult(Intent.createChooser(i, "開啟應用程式"), FCR);
            }
            //For Android 5.0+
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,WebChromeClient.FileChooserParams fileChooserParams){
                if(uriValueCallbackArray != null){
                    uriValueCallbackArray.onReceiveValue(null);
                }
                uriValueCallbackArray = filePathCallback;
                //拍照
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if(takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null){
                    File photoFile = null;
                    try{
                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", photoPath);
                    }catch(IOException ex){
                        Log.e(TAG, "Image file creation failed", ex);
                    }
                    if(photoFile != null){
                        photoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    }else{
                        takePictureIntent = null;
                    }
                }
                //開啟文件
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                //錄影
                Intent recordVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                //錄音
                Intent recordAudioIntent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
                Intent[] intentArray;
                intentArray = new Intent[]{takePictureIntent,recordVideoIntent,recordAudioIntent};

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "開啟應用程式");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, FCR);
                return true;
            }
            /*****<input type=file>點擊調用相機相簿文件夾(4.4.0-2 KITKAT版本不支援)*****/
            @Override
            public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result) {
                new AlertDialog.Builder(context)
//                        .setTitle("alert")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {result.confirm();}
                        })
                        .setCancelable(false)
                        .create().show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                new AlertDialog.Builder(context)
//                        .setTitle("confirm")
                        .setMessage(message)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {result.confirm();}
                        })
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {result.cancel();}
                        })
                        .create().show();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, final JsPromptResult result) {
                final EditText editText = new EditText(MainActivity.this);
                //editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                //editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                editText.setText(defaultValue);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(message)
                        .setView(editText)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {result.confirm(editText.getText().toString());}
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {result.cancel();}
                        })
                        .setCancelable(false)
                        .show();


                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
                super.onGeolocationPermissionsShowPrompt(origin, callback);
            }


        });

        webView.addJavascriptInterface(new JavascriptFunction(context),"Mobile");
        webView.loadUrl(inputUrl);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;
            //Check if response is positive
            if(resultCode== Activity.RESULT_OK){
                if(requestCode == FCR){
                    if(null == uriValueCallbackArray){
                        return;
                    }
                    if(intent == null){
                        //Capture Photo if no image available
                        if(photoPath != null){
                            results = new Uri[]{Uri.parse(photoPath)};
                        }
                    }else{
                        String dataString = intent.getDataString();
                        if(dataString != null){
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                }
            }
            uriValueCallbackArray.onReceiveValue(results);
            uriValueCallbackArray = null;
        }else{
            if(requestCode == FCR){
                if(null == uriValueCallback) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                uriValueCallback.onReceiveValue(result);
                uriValueCallback = null;
            }
        }
    }

    // Create an image file
    private File createImageFile() throws IOException{
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "img_"+timeStamp+"_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName,".jpg",storageDir);
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        new AlertDialog.Builder(this)
                                .setIcon(R.mipmap.ic_launcher_round)
                                .setTitle("警告")
                                .setMessage("離開應用程式")
                                .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        finish();
                                    }
                                })
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                    }
                                })
                                .show();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean isConnected(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    class JavascriptFunction{
        Context context;
        JavascriptFunction(Context context){
            this.context = context;
        }

        @JavascriptInterface
        public void callByJavascript(){
            Toast.makeText(context,"被javascript呼叫",Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface
        public String callByJavascript2(String text){
            return text;
        }
    }

    public void callJs(){
        if (Build.VERSION.SDK_INT < 19){
            webView.loadUrl("javascript:callJs()");
        }else{
            webView.evaluateJavascript("javascript:callJs()", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {

                }
            });
        }
    }

    public void callJs2(String text){
        if (Build.VERSION.SDK_INT < 19){
            webView.loadUrl("javascript:callJs2('"+text+"')");
        }else{
            webView.evaluateJavascript("javascript:callJs2('"+text+"')", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    Toast.makeText(getApplicationContext(), value, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
