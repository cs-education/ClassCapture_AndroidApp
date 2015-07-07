package com.classtranscribe.classcapture.views.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ListView;

import com.classtranscribe.classcapture.controllers.adapters.RecordingsListAdapter;
import com.classtranscribe.classcapture.models.Recording;

/**
 * A fragment representing a list of Items.
 * <p/>
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnFragmentInteractionListener}
 * interface.
 */
public class RecordingsFragment extends ListFragment {

    private OnFragmentInteractionListener listener;
    private RecordingsListAdapter listAdapter;

    public static RecordingsFragment newInstance() {
        RecordingsFragment fragment = new RecordingsFragment();
        Bundle args = new Bundle(); // initially empty but must add things later
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public RecordingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.listAdapter = new RecordingsListAdapter(this.getActivity());
        setListAdapter(this.listAdapter);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        System.out.println("Click at pos " + position);

        if (listener != null) {
            System.out.println("Listener != null");
            // Notify the active callbacks interface (the activity, if the
            // fragment is attached to one) that an item has been selected.

            // Get Recording object corresponding to list item click
            Recording recording = this.listAdapter.getItem(position);
            // Create uri for selected video
            Uri videoUri = Uri.parse(recording.getVideoURL(this.getActivity()));
            // Send uri to main activity for
            listener.startVideoViewingActivity(videoUri);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        public void startVideoViewingActivity(Uri videoUri);
    }

}
