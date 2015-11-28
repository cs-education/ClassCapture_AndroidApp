package com.classtranscribe.classcapture.controllers.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
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
import com.classtranscribe.classcapture.controllers.fragments.NavigationDrawerFragment;
import com.classtranscribe.classcapture.controllers.fragments.RecordingsFragment;
import com.classtranscribe.classcapture.controllers.fragments.SettingsFragment;
import com.classtranscribe.classcapture.controllers.fragments.VideoCaptureFragment;
import com.classtranscribe.classcapture.models.Recording;
import com.classtranscribe.classcapture.services.GoogleAnalyticsTrackerService;
import com.classtranscribe.classcapture.services.UploadAlarmReceiver;
import com.classtranscribe.classcapture.services.UploadQueueProvider;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmConfiguration;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,RecordingsFragment.OnFragmentInteractionListener,VideoCaptureFragment.OnFragmentInteractionListener {

    public static final int REQUEST_VIDEO_CAPTURE = 1; // request code

    public static final int RECORDINGS_FRAGMENT_POSITION = 0;
    public static final int VIDEO_CAPTURE_FRAGMENT_POSITION = 1;
    public static final int SETTINGS_FRAGMENT_POSITION = 2;

    public static boolean UPLOAD_ALARM_HAS_BEEN_SET = false;
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
    Realm videoUploadRealm;
    private Tracker defaultTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up info for libraries
        this.setupDefaultRealm();
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

    @Override
    public void onResume() {
        super.onResume();
        Tracker tracker = GoogleAnalyticsTrackerService.getDefaultTracker(this);
        tracker.setScreenName(this.getString(R.string.main_activity_screen_name));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (!UPLOAD_ALARM_HAS_BEEN_SET) {
            this.scheduleUploadAlarm();
            UPLOAD_ALARM_HAS_BEEN_SET = true;
        }
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
            case RECORDINGS_FRAGMENT_POSITION:
                return RecordingsFragment.newInstance();
            case VIDEO_CAPTURE_FRAGMENT_POSITION:
                return VideoCaptureFragment.newInstance();
            case SETTINGS_FRAGMENT_POSITION:
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
            this.onNavigationDrawerItemSelected(SETTINGS_FRAGMENT_POSITION);
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
    public void onVideoCaptureUploadFailure(Throwable error, final Recording recording) {
        error.printStackTrace();
        // Write the recording to the upload queue on a new thread.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UploadQueueProvider.addToUploadQueue(MainActivity.this, recording);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
    private void setupDefaultRealm() {
        RealmConfiguration.Builder configBuilder = new RealmConfiguration.Builder(this);
        RealmConfiguration defaultConfig = configBuilder.build();
        Realm.setDefaultConfiguration(defaultConfig);
        this.defaultRealm = Realm.getDefaultInstance();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.defaultRealm.close();
        this.videoUploadRealm.close();
    }

    /*
     * Setup a recurring alarm every half hour
     * Got this from here: https://guides.codepath.com/android/Starting-Background-Services#using-with-alarmmanager-for-periodic-tasks
     */
    public void scheduleUploadAlarm() {
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), UploadAlarmReceiver.class);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, UploadAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Setup periodic alarm every 5 seconds
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
        // Interval can be INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, INTERVAL_HOUR, INTERVAL_DAY
        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                AlarmManager.INTERVAL_HALF_HOUR, pIntent);
    }
}
