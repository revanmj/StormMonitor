package pl.revanmj.stormmonitor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import pl.revanmj.StormMonitor;

import im.delight.android.webview.AdvancedWebView;


public class DetailsActivity extends AppCompatActivity {

    AdvancedWebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // Get tracker.
        Tracker t = ((StormMonitor) DetailsActivity.this.getApplication()).getTracker(StormMonitor.TrackerName.GLOBAL_TRACKER);
        // Send a screen view.
        t.send(new HitBuilders.AppViewBuilder().build());

        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        if (title.equals("map")) {
            getSupportActionBar().setTitle(R.string.title_activity_maps);
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Map (webView)")
                    .putContentType("Screens")
                    .putContentId("screen-2"));
        }
        else if (title.equals("details")) {
            getSupportActionBar().setTitle(R.string.title_activity_details);
            Answers.getInstance().logContentView(new ContentViewEvent()
                    .putContentName("Details (webView screen)")
                    .putContentType("Screens")
                    .putContentId("screen-4"));
        }

        webview = (AdvancedWebView) findViewById(R.id.webView);
        webview.getSettings().setAppCacheEnabled(true);
        webview.setGeolocationEnabled(true);
        webview.addPermittedHostname("antistorm.eu");
        webview.addPermittedHostname("m.antistorm.eu");
        webview.addPermittedHostname("www.antistorm.eu");

        webview.setListener(this, new AdvancedWebView.Listener() {

            @Override
            public void onPageFinished(String url) {
                // a new page finished loading
            }

            @Override
            public void onPageStarted(String url, Bitmap favicon) {
                // a new page started loading
            }

            @Override
            public void onPageError(int errorCode, String description, String failingUrl) {
                // the new page failed to load
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
