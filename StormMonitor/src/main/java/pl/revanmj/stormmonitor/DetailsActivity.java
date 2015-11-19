package pl.revanmj.stormmonitor;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import im.delight.android.webview.AdvancedWebView;


public class DetailsActivity extends AppCompatActivity {

    private AdvancedWebView webview;
    private ProgressBar loadingAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Getting parameters
        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        if (title.equals("map")) {
            getSupportActionBar().setTitle(R.string.title_activity_maps);
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Map (webView)")
                    .putContentType("Views")
                    .putContentId("mapWebView"));
        }
        else if (title.equals("details")) {
            getSupportActionBar().setTitle(R.string.title_activity_details);
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Details (webView screen)")
                    .putContentType("Views")
                    .putContentId("detailsWebView"));
        }

        // Setting up loading animation and WebView
        loadingAnim = (ProgressBar) findViewById(R.id.progressBar);
        webview = (AdvancedWebView) findViewById(R.id.webView);
        webview.getSettings().setAppCacheEnabled(true);
        checkForPermission();
        webview.addPermittedHostname("antistorm.eu");
        webview.addPermittedHostname("m.antistorm.eu");
        webview.addPermittedHostname("www.antistorm.eu");
        webview.addPermittedHostname("www.antistorm.eu/m");
        webview.setListener(this, new AdvancedWebView.Listener() {

            @Override
            public void onPageFinished(String url) {
                webview.setVisibility(View.VISIBLE);
                loadingAnim.setProgress(100);
                loadingAnim.setVisibility(View.GONE);
            }

            @Override
            public void onPageStarted(String url, Bitmap favicon) {
                webview.setVisibility(View.GONE);
                loadingAnim.setVisibility(View.VISIBLE);
                loadingAnim.setProgress(0);
            }

            @Override
            public void onPageError(int errorCode, String description, String failingUrl) {
                loadingAnim.setProgress(100);
                loadingAnim.setVisibility(View.GONE);
            }

            @Override
            public void onExternalPageRequest(String url) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }

            @Override
            public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                // some file is available for download
            }

        });


        if (url != null && !url.equals(""))
            webview.loadUrl(url);
    }

    private void checkForPermission() {
        int permission = ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION");

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"},
                    1);
            return;
        }

        webview.setGeolocationEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    webview.setGeolocationEnabled(true);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            webview.reload();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
