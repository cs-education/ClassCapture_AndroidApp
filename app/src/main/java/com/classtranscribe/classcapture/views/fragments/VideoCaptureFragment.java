package com.classtranscribe.classcapture.views.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.VideoView;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.models.Recording;
import com.classtranscribe.classcapture.views.activities.MainActivity;

import java.io.IOException;
import java.util.Date;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link VideoCaptureFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link VideoCaptureFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoCaptureFragment extends Fragment {

    // Fragment Bundle Argument parameters
    private static final String VIDEO_HAS_BEEN_CAPTURED = "videoCaptured";
    private static final String HAS_BEEN_INITIALIZED = "hasBeenInitialized";

    private OnFragmentInteractionListener listener;

    // View references
    VideoView videoView;
    TextView recordingDurationTextView;
    Button uploadButton;

    // Instance variables for recording
    Recording capturedRecording;
    boolean hasBeenInitialized;

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

        // init listeners
        this.uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoCaptureFragment.this.onUploadClicked();
            }
        });
    }

    public void onUploadClicked() {
        boolean videoHasBeenCaptured = this.getArguments().getBoolean(VIDEO_HAS_BEEN_CAPTURED, false);
        if (videoHasBeenCaptured) {

            if (this.capturedRecording.startTime == null) {
                System.err.println("capturedRecording.startTime is null");
            }

            if (this.capturedRecording.endTime== null) {
                System.err.println("capturedRecording.endTime is null");
            }

            this.capturedRecording.uploadRecording(this.getActivity(), new Callback<Recording>() {
                @Override
                public void success(Recording recording, Response response) {
                    VideoCaptureFragment.this.listener.onVideoCaptureUploadSuccess(recording);
                }

                @Override
                public void failure(RetrofitError error) {
                    System.err.println(error);
                    System.err.println("Body: " + error.getBody());
                    byte[] bodyBuffer = new byte[(int) error.getResponse().getBody().length()];
                    try {
                        error.getResponse().getBody().in().read(bodyBuffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.err.println(new String(bodyBuffer));

                    VideoCaptureFragment.this.listener.onVideoCaptureUploadFailure(error);
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
            // onActivityResult receives the videoUri and sends it along to this callback
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
                    VideoCaptureFragment.this.capturedRecording = new Recording(currContext, videoUri);

                    // Set metadata views appropriately
                    VideoCaptureFragment.this.updateRecordingDurationTextView();
                }
            });

            // Start video capture
            this.dispatchTakeVideoIntent();
        }
    }

    /**
     * Updates the video
     */
    private void updateRecordingDurationTextView() {
        Date start = this.capturedRecording.startTime;
        Date end   = this.capturedRecording.endTime;

        if (start == null || end == null) {
            throw new IllegalStateException("Recording Object isn't populated with duration metadata");
        }

        String text = start + " to " + end;
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
        public void onVideoCaptureUploadFailure(RetrofitError error);
    }

    public interface VideoCaptureListener {
        void onVideoCapture(Uri uri);
    }

}
