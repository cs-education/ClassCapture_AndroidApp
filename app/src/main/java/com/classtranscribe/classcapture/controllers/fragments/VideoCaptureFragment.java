package com.classtranscribe.classcapture.controllers.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.adapters.SectionSpinnerAdapter;
import com.classtranscribe.classcapture.controllers.activities.MainActivity;
import com.classtranscribe.classcapture.models.Recording;
import com.classtranscribe.classcapture.models.Section;
import com.classtranscribe.classcapture.services.GoogleAnalyticsTrackerService;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VideoCaptureFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VideoCaptureFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoCaptureFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    // Static UI Text
    private final static String UPLOAD_DIALOG_TITLE   = "Uploading Recording";
    private final static String UPLOAD_DIALOG_MESSAGE = "Just a sec...";

    // Fragment Bundle Argument parameters
    private static final String VIDEO_HAS_BEEN_CAPTURED = "videoCaptured";
    private static final String HAS_BEEN_INITIALIZED = "hasBeenInitialized";
    private static final String FRAG_HIT_HAS_BEEN_TRACKED = "hitTracked";

    private OnFragmentInteractionListener listener;

    // View references
    VideoView videoView;
    TextView recordingDurationTextView;
    Button uploadButton;
    Spinner sectionSpinner;

    // Instance variables for recording
    Recording capturedRecording;

    // Section to be attached with recording
    Section chosenSection;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("h:mm:ss a"); // format with which start and end time of video will be displayed

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment VideoCaptureFragment.
     */
    public static VideoCaptureFragment newInstance() {
        VideoCaptureFragment fragment = new VideoCaptureFragment();
        Bundle args = new Bundle(); // initially empty, must populate with objects

        args.putBoolean(VIDEO_HAS_BEEN_CAPTURED, false);
        args.putBoolean(HAS_BEEN_INITIALIZED, false);
        args.putBoolean(FRAG_HIT_HAS_BEEN_TRACKED, false);

        fragment.setArguments(args);
        return fragment;
    }

    public VideoCaptureFragment() {
        //  Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_video_capture, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!this.getArguments().getBoolean(HAS_BEEN_INITIALIZED, false)) {
            this.initFragmentInstance();
            this.getArguments().putBoolean(HAS_BEEN_INITIALIZED, true);
        }
    }

    /**
     * Does all one time setup for instance variables of Fragment
     */
    public void initFragmentInstance() {
        // Init view variables
        this.videoView = (VideoView) this.getView().findViewById(R.id.video_view);
        this.recordingDurationTextView = (TextView) this.getView().findViewById(R.id.recording_duration_textview);
        this.uploadButton = (Button) this.getView().findViewById(R.id.upload_button);
        this.sectionSpinner = (Spinner) this.getView().findViewById(R.id.section_spinner);

        // init listeners
        this.uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoCaptureFragment.this.onUploadClicked();
            }
        });

        // Init section spinner with User's registered sections
        // All sections stored in Realm DB are sections that user has registered for
        // Apparently, running queries on UI thread isn't too big of a deal:
        //  http://stackoverflow.com/questions/27805580/realm-io-and-asynchronous-queries
        // Given our data-set will be really small anyways, I think its worth just doing on UI thread for cleanliness
        this.sectionSpinner.setAdapter(new SectionSpinnerAdapter(getActivity()));
        this.sectionSpinner.setOnItemSelectedListener(this);
    }

    // Called when a section is selected from the spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        this.chosenSection = (Section) this.sectionSpinner.getItemAtPosition(position);
        this.uploadButton.setEnabled(true);
        // Start video capture
        this.dispatchTakeVideoIntent();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        this.chosenSection = null;
        this.uploadButton.setEnabled(false);

        // Prompt the user to sign up for a section via the settings screen
        AlertDialog goToSettingsDialog = new AlertDialog.Builder(this.getActivity())
                .setCancelable(false)
                .setTitle(getString(R.string.go_to_settings_prompt))
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity mainActivity = (MainActivity) VideoCaptureFragment.this.getActivity();
                        mainActivity.onNavigationDrawerItemSelected(MainActivity.SETTINGS_FRAGMENT_POSITION);
                    }
                }).create();

        goToSettingsDialog.show();
    }

    public void trackUploadClickEvent() {
        Tracker tracker = GoogleAnalyticsTrackerService.getDefaultTracker(this.getActivity());
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(this.getString(R.string.recording_category_name))
                .setAction(this.getString(R.string.upload_recording_action_name))
                .build());
    }

    public void onUploadClicked() {
        boolean videoHasBeenCaptured = this.getArguments().getBoolean(VIDEO_HAS_BEEN_CAPTURED, false);
        if (videoHasBeenCaptured) {

            if (this.chosenSection == null) {
                Toast.makeText(getActivity(), "Please choose a section for this recording", Toast.LENGTH_SHORT).show();
                return;
            }

            if (this.capturedRecording.getStartTime() == null) {
                System.err.println("capturedRecording.startTime is null");
                return;
            }

            if (this.capturedRecording.getEndTime()== null) {
                System.err.println("capturedRecording.endTime is null");
                return;
            }

            // Send the event to GA
            this.trackUploadClickEvent();

            // First, disable the upload button so video cant be uploaded twice
            uploadButton.setEnabled(false);

            // While the user is waiting for the video to upload, make sure to show a progress dialog
            final ProgressDialog progress = ProgressDialog.show(VideoCaptureFragment.this.getActivity(), UPLOAD_DIALOG_TITLE, UPLOAD_DIALOG_MESSAGE, true);
            progress.setCancelable(false); // Dialog will be modal

            this.capturedRecording.uploadRecording((MainActivity) this.getActivity(), new Callback<Recording>() {
                @Override
                public void onResponse(Response<Recording> response, Retrofit retrofit) {
                    VideoCaptureFragment.this.listener.onVideoCaptureUploadSuccess(response.body());
                    progress.dismiss(); // Finally dismiss the progress dialog
                }

                @Override
                public void onFailure(Throwable error) {
                    VideoCaptureFragment.this.listener.onVideoCaptureUploadFailure(error, VideoCaptureFragment.this.capturedRecording);
                    progress.dismiss(); // Finally dismiss the progress dialog
                    uploadButton.setEnabled(true); // If the upload failed for some reason, allow them to try again
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Bundle fragArgs = this.getArguments();

        // This block is for things that only need to be done to set up for video capture/upload
        if (!fragArgs.getBoolean(VIDEO_HAS_BEEN_CAPTURED, false)) {
            // Set fragment callback
            // This callback is called within onActivityResult of MainActivity
            // onActivityResult of MainActivity is used as the callback for when the user shoots a video
            // onActivityResult receives the videoFilePath and sends it along to this callback
            VideoCaptureFragment.this.listener.videoCaptureFragmentInteraction(new VideoCaptureListener() {
                @Override
                public void onVideoCapture(Uri videoUri) {
                    // Update VIDEO_HAS_BEEN_CAPTURED argument
                    Bundle fragArgs = VideoCaptureFragment
                            .this.getArguments();
                    fragArgs.putBoolean(VIDEO_HAS_BEEN_CAPTURED, true);

                    // Set videoView to be showing the just captured video and start it
                    VideoCaptureFragment.this.videoView.setVideoURI(videoUri);
                    VideoCaptureFragment.this.videoView.start();

                    // Set end time of capturedRecording to current time
                    Context currContext = VideoCaptureFragment.this.getActivity();
                    VideoCaptureFragment.this.capturedRecording = new Recording(currContext, videoUri, VideoCaptureFragment.this.chosenSection.getId());

                    // Set metadata views appropriately
                    VideoCaptureFragment.this.updateRecordingDurationTextView();
                }
            });
        }

        // Check if the fragment hit event has been tracked, if not then better track it
        if (!fragArgs.getBoolean(FRAG_HIT_HAS_BEEN_TRACKED, false)) {
            Tracker tracker = GoogleAnalyticsTrackerService.getDefaultTracker(this.getActivity());
            tracker.setScreenName(this.getString(R.string.video_capture_screen_name));
            tracker.send(new HitBuilders.ScreenViewBuilder().build());
            // Update FRAG_HIT_HAS_BEEN_TRACKED argument
            this.getArguments()
                .putBoolean(FRAG_HIT_HAS_BEEN_TRACKED, true);
        }
    }

    /**
     * Updates the video
     */
    private void updateRecordingDurationTextView() {
        Date start = this.capturedRecording.getStartTime();
        Date end   = this.capturedRecording.getEndTime();

        if (start == null || end == null) {
            throw new IllegalStateException("Recording Object isn't populated with duration metadata");
        }

        // Format according to formatter declared above
        String startStr = this.dateFormat.format(start);
        String endStr   = this.dateFormat.format(end);

        String text = startStr + " to " + endStr;
        this.recordingDurationTextView.setText(text);
    }

    /**
     * Will start video capture from separate app
     * Found on http://developer.android.com/training/camera/videobasics.html
     */
    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(this.getActivity().getPackageManager()) != null) {
            getActivity().startActivityForResult(takeVideoIntent, MainActivity.REQUEST_VIDEO_CAPTURE);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.listener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.listener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void videoCaptureFragmentInteraction(VideoCaptureFragment.VideoCaptureListener captureListener);
        public void onVideoCaptureUploadSuccess(Recording recording);
        public void onVideoCaptureUploadFailure(Throwable error, Recording recording);
    }

    public interface VideoCaptureListener {
        void onVideoCapture(Uri uri);
    }

}
