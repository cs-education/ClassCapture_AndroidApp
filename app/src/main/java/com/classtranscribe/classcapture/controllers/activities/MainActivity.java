package com.classtranscribe.classcapture.controllers.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.alexbbb.uploadservice.UploadService;
import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.models.Recording;
import com.classtranscribe.classcapture.controllers.fragments.NavigationDrawerFragment;
import com.classtranscribe.classcapture.controllers.fragments.RecordingsFragment;
import com.classtranscribe.classcapture.controllers.fragments.SettingsFragment;
import com.classtranscribe.classcapture.controllers.fragments.VideoCaptureFragment;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit.RetrofitError;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,RecordingsFragment.OnFragmentInteractionListener,VideoCaptureFragment.OnFragmentInteractionListener {

    public static final int REQUEST_VIDEO_CAPTURE = 1; // request code

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment navigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence title;

    /**
     * Callbacks for various fragment events
     */
    private VideoCaptureFragment.VideoCaptureListener captureListener;

    Realm defaultRealm;
    private Tracker defaultTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up info for libraries
        this.setupRealm();
        UploadService.NAMESPACE = this.getPackageName(); // Sets so service can know which app to direct updates to...i think

        setContentView(R.layout.activity_main);


        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        title = getTitle();

        // Set up the drawer.
        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    synchronized public Tracker getDefaultTracker() {
        if (this.defaultTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            this.defaultTracker = analytics.newTracker(R.xml.global_tracker);
        }

        return this.defaultTracker;
    }

    @Override
    public void onResume() {
        super.onResume();
        Tracker tracker = this.getDefaultTracker();
        tracker.setScreenName(this.getString(R.string.main_activity_screen_name));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, this.getFragmentInstance(position))
                .commit();
    }

    /**
     * Gets fragment for specific item click on navigation drawer
     * @param position index of item clicked in nav drawer
     * @return new instance of specified fragment
     */
    public Fragment getFragmentInstance(int position) {
        switch (position) {
            case 0:
                return RecordingsFragment.newInstance();
            case 1:
                return VideoCaptureFragment.newInstance();
            case 2:
                return SettingsFragment.newInstance();
            default:
                return RecordingsFragment.newInstance();
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(title);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!navigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void startVideoViewingActivity(Uri videoUri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(videoUri, "video/mp4"); // Change so video maps to correct mime type. Just assuming mp4 for now
        this.startActivity(intent);
    }

    @Override
    public void videoCaptureFragmentInteraction(VideoCaptureFragment.VideoCaptureListener captureListener) {
        // To interact with VideoCaptureFragment
        this.captureListener = captureListener;
    }

    @Override
    public void onVideoCaptureUploadSuccess(Recording recording) {
    }

    @Override
    public void onVideoCaptureUploadFailure(RetrofitError error) {
        Toast.makeText(this, "Video Upload Failed: " + error, Toast.LENGTH_SHORT).show();
    }

    /**
     * Called when video is captured from separate app
     * Found on http://developer.android.com/training/camera/videobasics.html
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            Uri videoUri = data.getData();
            this.captureListener.onVideoCapture(videoUri);
        } else {
            System.err.println("Error on Video Capture: requestCode " + requestCode + "\tresultCode: " + resultCode);
        }
    }

    /**
     * Sets Default realm config and sets this.defaultRealm
     */
    private void setupRealm() {
        RealmConfiguration.Builder configBuilder = new RealmConfiguration.Builder(this);
        RealmConfiguration defaultConfig = configBuilder.build();
        Realm.setDefaultConfiguration(defaultConfig);
        this.defaultRealm = Realm.getDefaultInstance();
    }

}
