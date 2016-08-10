package soganiabhijeet.com.myapplication;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

public class MainActivity extends Activity {
    public static int OVERLAY_PERMISSION_REQ_CODE_CHATHEAD = 1234;
    public static int OVERLAY_PERMISSION_REQ_CODE_CHATHEAD_MSG = 5678;
    public static Button btnStartService, btnShowMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WebView webView = new WebView( this );
        webView.getSettings().setAppCacheMaxSize( 5 * 1024 * 1024 ); // 5MB
        webView.getSettings().setAppCachePath( getApplicationContext().getCacheDir().getAbsolutePath() );
        webView.getSettings().setAllowFileAccess( true );
        webView.getSettings().setAppCacheEnabled( true );
        webView.getSettings().setJavaScriptEnabled( true );// load online by default
        webView.getSettings().setCacheMode( WebSettings.LOAD_CACHE_ELSE_NETWORK );
        webView.loadUrl( "http://172.16.1.145:8000/deals/shopping_assistant_poc/");
        /*WebView view = new WebView(getApplicationContext());
        view.loadUrl("http://www.facebook.com/");*/

        btnStartService = (Button)findViewById(R.id.btnStartService);
        btnShowMsg = (Button)findViewById(R.id.btnMsg);

        btnStartService.setOnClickListener(lst_StartService);
        btnShowMsg.setOnClickListener(lst_ShowMsg);
    }


    Button.OnClickListener lst_StartService = new Button.OnClickListener(){

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            Log.d(Utils.LogTag, "lst_StartService -> Utils.canDrawOverlays(Main.this): " + Utils.canDrawOverlays(MainActivity.this));

            if(Utils.canDrawOverlays(MainActivity.this))
                startChatHead();
            else{
                requestPermission(OVERLAY_PERMISSION_REQ_CODE_CHATHEAD);
            }
        }

    };


    Button.OnClickListener lst_ShowMsg = new Button.OnClickListener(){

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            if(Utils.canDrawOverlays(MainActivity.this))
                showChatHeadMsg();
            else{
                requestPermission(OVERLAY_PERMISSION_REQ_CODE_CHATHEAD_MSG);
            }

        }

    };

    private void startChatHead(){

        startService(new Intent(MainActivity.this, ShoppingWizardService.class));
    }
    private void showChatHeadMsg(){
        java.util.Date now = new java.util.Date();
        String str = "test by abhijeet  " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);

        Intent it = new Intent(MainActivity.this, ShoppingWizardService.class);
        it.putExtra(Utils.EXTRA_MSG, str);
        startService(it);
    }

    private void needPermissionDialog(final int requestCode){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("You need to allow permission");
        builder.setPositiveButton("OK",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        requestPermission(requestCode);
                    }
                });
        builder.setNegativeButton("Cancel", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub

            }
        });
        builder.setCancelable(false);
        builder.show();
    }



    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();


    }

    private void requestPermission(int requestCode){
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_PERMISSION_REQ_CODE_CHATHEAD) {
            if (!Utils.canDrawOverlays(MainActivity.this)) {
                needPermissionDialog(requestCode);
            }else{
                startChatHead();
            }

        }else if(requestCode == OVERLAY_PERMISSION_REQ_CODE_CHATHEAD_MSG){
            if (!Utils.canDrawOverlays(MainActivity.this)) {
                needPermissionDialog(requestCode);
            }else{
                showChatHeadMsg();
            }

        }

    }
}

