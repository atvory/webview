package com.example.webview;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.DownloadManager;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.*;
import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.ValueCallback;
import android.widget.Toast;
import java.io.File;

import android.webkit.WebChromeClient;
import static java.lang.System.*;

import com.example.webview.R;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private final String YOUR_URL_CONSTANT = "yourURLhere";

    private Context context;

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int FILECHOOSER_RESULTCODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;

    // Para android 5.0
    private ValueCallback<Uri[]> mFilePathCallback;
    private String rutaFotoCam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        // solicitud de permisos para aplicacion en ejecucion


        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);

        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }




        // fin solicitud permisos

        setContentView(R.layout.activity_main);

        if(webView == null){
            this.webView = (WebView) findViewById(R.id.webview);

            WebSettings mywebsettings = webView.getSettings();
            mywebsettings.setJavaScriptEnabled(true);

            //ajustes del webview
            webView.setWebViewClient(new WebViewClient());
            webView.getSettings().setLoadsImagesAutomatically(true);
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            webView.getSettings().setAppCacheEnabled(true);
            webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            webView.requestFocus();
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setDatabaseEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setSupportZoom(true);
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
            webView.getSettings().setGeolocationEnabled(true);
            webView.loadUrl(YOUR_URL_CONSTANT);
            webView.setWebViewClient(new WebViewClient(){

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // When user clicks a hyperlink, load in the existing WebView
                    if(url.contains("geo:")||url.contains("https://maps")) {
                        Uri gmmIntentUri = Uri.parse(url);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        if (mapIntent.resolveActivity(getPackageManager()) != null) {
                            startActivity(mapIntent);
                        }
                        return true;
                    }
                    view.loadUrl(url);
                    return true;
                }


            });

            //webView.zoomIn();
            //mywebsettings.setSupportZoom(true);
            //mywebsettings.setBuiltInZoomControls(true);
            mywebsettings.setDomStorageEnabled(true);
            mywebsettings.setUseWideViewPort(true);

            webView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));



                    request.allowScanningByMediaScanner();
                    Environment.getExternalStorageDirectory();
                    getApplicationContext().getFilesDir().getPath(); //which returns the internal app files directory path
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "albaran_dv.pdf");
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);

                    Toast.makeText(getApplicationContext(), "Descargando archivo", Toast.LENGTH_LONG).show();
                    /*Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);*/
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {






                public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                    // callback.invoke(String origin, boolean allow, boolean remember);
                    callback.invoke(origin, true, false);
                }


                // en caso de llamar un webview a un file chooser
                public boolean onShowFileChooser(
                        WebView webView, ValueCallback<Uri[]> filePathCallback,
                        WebChromeClient.FileChooserParams fileChooserParams) {
                    if (mFilePathCallback != null) {
                        mFilePathCallback.onReceiveValue(null);
                    }
                    mFilePathCallback = filePathCallback;

                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                        // creacion del file para alojar la foto *lollipop
                        File photoFile = null;
                        try {
                            photoFile = crearArchivoFoto();
                            takePictureIntent.putExtra("rutaFoto", rutaFotoCam);
                        } catch (Exception e) {
                            //System.out.println("fallo al crear la foto");
                            Log.e(TAG, "fallo creando la imagen", e);
                        }
                        // si tod ok
                        if (photoFile != null) {
                            rutaFotoCam = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }

                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType("image/*");

                    Intent[] intentArray;
                    if (takePictureIntent != null) {
                        intentArray = new Intent[]{takePictureIntent};
                    } else {
                        intentArray = new Intent[0];
                    }

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.image_chooser));
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                    startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);

                    return true;
                }

                // crea file para la imagen, necesario en versiones lollipop
                private File crearArchivoFoto(){

                    File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "webviewfiles");

                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs();
                    }
                    // creacion de imagen, saca substring de la hora en milisegundos le anade extension jpg
                    String hora = String.valueOf(currentTimeMillis());
                    String nombreFoto = hora.substring(8);
                    imageStorageDir = new File(imageStorageDir + File.separator + "imagen_" + nombreFoto + ".jpg");

                    return imageStorageDir;
                }

                /*private File crearArchivoFotoInterna(){

                    File imageStorageDir = new File(Environment.);
                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs();
                    }
                    String hora = String.valueOf(currentTimeMillis());
                    String nombreFoto = hora.substring(8);
                    imageStorageDir = new File(imageStorageDir + File.separator + "imagen_" + nombreFoto + ".jpg");

                    return imageStorageDir;
                }*/

            });
        }





        if(savedInstanceState!=null){
            ((WebView)findViewById(R.id.webview)).restoreState(savedInstanceState);
        }
    }


    private class WebChromClientCustomPoster extends WebChromeClient{
        @Override
        public Bitmap getDefaultVideoPoster(){
            return Bitmap.createBitmap(10,10,Bitmap.Config.ARGB_8888);
        }
    }
    // codigo que se ejecuta a la salida de la camara o galeria
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        // codigo para todas las versiones anteriores a lollipop
        /*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == this.mUploadMessage) {
                    return;
                }

                Uri result = null;

                try {
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        // Si no se recibe codigo del file chooser
                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e, Toast.LENGTH_LONG).show();
                }

                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }



        }*/

       /* // codigo para versiones viejas de android con lollipop
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode != FILECHOOSER_RESULTCODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }

            Uri[] results = null;

            // comprobacion del result code
            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.getData() == null) {
                    if (rutaFotoCam != null) {
                        results = new Uri[]{Uri.parse(rutaFotoCam)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;

        } // fin del codigo para android lollipop*/




        /////// test

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILECHOOSER_RESULTCODE)
        {
            Uri[] results = null;
            //Check if response is positive
            if (resultCode == Activity.RESULT_OK)
            {
                if (null == mFilePathCallback)
                {
                    return;
                }
                if (data == null || data.getData() == null)
                {
                    //Capture Photo if no image available
                    if (rutaFotoCam != null)
                    {
                        results = new Uri[]{Uri.parse(rutaFotoCam)};
                    }
                }
                else
                {
                    String dataString = data.getDataString();
                    if(dataString != null)
                    {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        }
    }

    /**
     * codigo para poder volver en webviews sin excepciones
     */
    @Override
    public void onBackPressed() {
        if(webView.canGoBack()){
            webView.goBack();
        }
        else {

            super.onBackPressed();
        }
    }

    int PERMISSION_ALL = 1;
    String[] PERMISSIONS = {

            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
    };

    @Override
    protected void onDestroy() {
        File cache = getCacheDir();
        webView.clearCache(true);
        context.getCacheDir().delete();
        super.onDestroy();
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }





    @Override
    public boolean onTouchEvent(MotionEvent event) {

        return super.onTouchEvent(event);
    }
    @Override
    protected void onSaveInstanceState(Bundle outState )
    {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
}