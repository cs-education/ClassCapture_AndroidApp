package com.classtranscribe.classcapture.controllers.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
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
import com.classtranscribe.classcapture.models.Course;
import com.classtranscribe.classcapture.models.Recording;
import com.classtranscribe.classcapture.models.Section;
import com.classtranscribe.classcapture.models.User;
import com.classtranscribe.classcapture.services.CustomCB;
import com.classtranscribe.classcapture.services.GoogleAnalyticsTrackerService;
import com.classtranscribe.classcapture.services.UploadAlarmReceiver;
import com.classtranscribe.classcapture.services.UploadQueueProvider;
import com.classtranscribe.classcapture.services.UserService;
import com.classtranscribe.classcapture.services.UserServiceProvider;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.IOException;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import retrofit.Response;
import retrofit.Retrofit;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks,RecordingsFragment.OnFragmentInteractionListener,VideoCaptureFragment.OnFragmentInteractionListener,
        SettingsFragment.SettingsFragmentInteractionListener{

    public static final int REQUEST_VIDEO_CAPTURE = 1; // request code

    public static final int VIDEO_CAPTURE_FRAGMENT_POSITION = 0;
    public static final int LOGOUT_ITEM_POSITION = 1;

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

    User currUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up info for libraries
        this.defaultRealm = Realm.getDefaultInstance();
        UploadService.NAMESPACE = this.getPackageName(); // Sets so service can know which app to direct updates to...i think

        setContentView(R.layout.activity_main);


        navigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        title = getTitle();

        // Set up the drawer.
        navigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        this.setCurrentUser();
    }

    @Override
    public void onResume() {
        super.onResume();
        Tracker tracker = GoogleAnalyticsTrackerService.getDefaultTracker(this);
        tracker.setScreenName(this.getString(R.string.main_activity_screen_name));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        if (position == LOGOUT_ITEM_POSITION) {
            this.doLogout();
            return;
        }
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
            case VIDEO_CAPTURE_FRAGMENT_POSITION:
                return VideoCaptureFragment.newInstance();
            default:
                return RecordingsFragment.newInstance();
        }
    }

    /**
     * Sends logout request and goes back to login activity while showing a modal dialog
     */
    public void doLogout() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(getString(R.string.logout_dialog_title));
        dialog.setMessage(getString(R.string.logout_dialog_message));
        dialog.setCancelable(false);
        dialog.show();

        UserServiceProvider.getInstance(this).logout().enqueue(new CustomCB<Object>(this, MainActivity.class) {
            @Override
            public void onResponse(Response<Object> response, Retrofit retrofit) {
                MainActivity.this.clearDB();
                MainActivity.this.goBackToLogin();
                dialog.dismiss();
            }

            @Override
            public void onRequestFailure(Throwable t) {
                t.printStackTrace();
                MainActivity.this.clearDB();
                MainActivity.this.goBackToLogin();
                dialog.dismiss();
            }
        });
    }

    void clearDB() {
        Realm realm = Realm.getDefaultInstance();
        try {
            realm.beginTransaction();
            realm.clear(User.class);
            realm.clear(Course.class);
            realm.clear(Section.class);
            realm.commitTransaction();
        } finally {
            realm.close();
        }
    }

    void goBackToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        this.startActivity(intent);
        this.finish();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(title);
    }

    public void setCurrentUser() {
        this.currUser = this.defaultRealm.where(User.class).findFirst();
        if (this.currUser == null) {
            Toast.makeText(this, "Login Malfunction", Toast.LENGTH_LONG).show();
            this.doLogout();
        } else {
            // If there's no registered sections, Show a modal progress dialog that will blocks usage until user info is updated from API
            ProgressDialog dialog = null;

            if (this.currUser.getSections().isEmpty()) {
                dialog = new ProgressDialog(this);
                dialog.setTitle(this.getString(R.string.section_check_dialog_title));
                dialog.setMessage(this.getString(R.string.section_check_dialog_message));
                dialog.show();
            }


            final ProgressDialog finalDialog = dialog; // need to assign to final var so can be accessed within inline function
            // Send request to /user/me endpoint to get most up to date user info
            UserServiceProvider.getInstance(this).me().enqueue(new CustomCB<User>(this, MainActivity.class) {
                @Override
                public void onResponse(Response<User> response, Retrofit retrofit) {
                    if (finalDialog != null && finalDialog.isShowing()) {
                        finalDialog.dismiss();
                    }

                    if (response.isSuccess()) {
                        User updatedUser = response.body();

                        // Check if IDs are same
                        if (updatedUser.getId() == MainActivity.this.currUser.getId()) {
                            MainActivity.this.defaultRealm.beginTransaction();
                            MainActivity.this.defaultRealm.copyToRealmOrUpdate(updatedUser);
                            MainActivity.this.defaultRealm.commitTransaction();
                            MainActivity.this.currUser = updatedUser;

                            if (MainActivity.this.currUser.getSections().isEmpty()) {
                                Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.go_to_settings_prompt), Toast.LENGTH_SHORT).show();
                                MainActivity.this.openSettingsInBrowser();
                                MainActivity.this.finish();
                            }
                        } else {
                            // Something's gone wrong...IDs don't match...logout
                            Toast.makeText(MainActivity.this, "Login Malfunction", Toast.LENGTH_LONG).show();
                            MainActivity.this.doLogout();
                        }
                    } else {
                        // The auth cookie might've expired...logout
                        MainActivity.this.doLogout();
                    }
                }

                @Override
                public void onRequestFailure(Throwable t) {
                    if (finalDialog != null && finalDialog.isShowing()) {
                        finalDialog.dismiss();
                    }

                    t.printStackTrace();
                    Toast.makeText(MainActivity.this, "Login Malfunction", Toast.LENGTH_LONG).show();
                    MainActivity.this.doLogout();
                }
            });
        }
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
        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK && this.captureListener != null) {
            Uri videoUri = data.getData();
            this.captureListener.onVideoCapture(videoUri);
        } else {
            System.err.println("Error on Video Capture: requestCode " + requestCode + "\tresultCode: " + resultCode);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.defaultRealm.close();
    }

    public void openSettingsInBrowser() {
        String settingsURL = this.getString(R.string.api_base_url) + this.getString(R.string.account_settings_endpoint);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(settingsURL));
        this.startActivity(intent);
    }

    @Override
    public void registeredForSection() {

    }

    @Override
    public void deregisteredForSection() {
        // First check if any section objects exist
        Realm realm = Realm.getDefaultInstance();
        boolean noRegisteredSections = realm.allObjects(Section.class).isEmpty();

        if (noRegisteredSections) {

        }
    }

    public User getCurrentUser() {
        return this.currUser;
    }
}
