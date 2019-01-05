package pl.revanmj.stormmonitor.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

import im.delight.android.webview.AdvancedWebView;

import pl.revanmj.stormmonitor.R;

/**
 * Created by revanmj on 25.01.2015.
 */

public class WebViewActivity extends AppCompatActivity {
    private AdvancedWebView mWebView;
    private ProgressBar mLoadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

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
        mLoadingBar = findViewById(R.id.progressBar);
        mWebView = findViewById(R.id.webView);
        mWebView.getSettings().setAppCacheEnabled(true);
        checkForPermission();
        mWebView.addPermittedHostname("antistorm.eu");
        mWebView.addPermittedHostname("m.antistorm.eu");
        mWebView.addPermittedHostname("www.antistorm.eu");
        mWebView.addPermittedHostname("www.antistorm.eu/m");
        mWebView.setListener(this, new AdvancedWebView.Listener() {

            @Override
            public void onPageFinished(String url) {
                mWebView.setVisibility(View.VISIBLE);
                mLoadingBar.setProgress(100);
                mLoadingBar.setVisibility(View.GONE);
            }

            @Override
            public void onPageStarted(String url, Bitmap favicon) {
                mWebView.setVisibility(View.GONE);
                mLoadingBar.setVisibility(View.VISIBLE);
                mLoadingBar.setProgress(0);
            }

            @Override
            public void onPageError(int errorCode, String description, String failingUrl) {
                mLoadingBar.setProgress(100);
                mLoadingBar.setVisibility(View.GONE);
            }

            @Override
            public void onDownloadRequested(String url, String suggestedFilename, String mimeType,
                                            long contentLength, String contentDisposition, String userAgent) {}

            @Override
            public void onExternalPageRequest(String url) {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }

        });

        if (url != null && !url.equals(""))
            mWebView.loadUrl(url);
    }

    private void checkForPermission() {
        int permission = ContextCompat.checkSelfPermission(this, "android.permission.ACCESS_FINE_LOCATION");
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION"},
                    1);
            return;
        }
        mWebView.setGeolocationEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mWebView.setGeolocationEnabled(true);
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mWebView.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_webview, menu);
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
            mWebView.reload();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
