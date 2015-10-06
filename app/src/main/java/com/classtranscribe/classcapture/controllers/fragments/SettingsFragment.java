package com.classtranscribe.classcapture.controllers.fragments;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.adapters.SectionListAdapter;
import com.classtranscribe.classcapture.controllers.activities.MainActivity;
import com.classtranscribe.classcapture.models.Section;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmQuery;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends Fragment implements AdapterView.OnItemClickListener, SectionListAdapter.OnSectionsLoadedListener {

    ListView sectionsListView;
    ProgressDialog loadingDialog;

    /**
     * Use this factory method to create a new instance of
     * this fragment
     *
     * @return A new instance of fragment SettingsFragment
     */
    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        this.initializeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        Tracker tracker = ((MainActivity) this.getActivity()).getDefaultTracker();
        tracker.setScreenName(this.getString(R.string.settings_screen_name));
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void initializeFragment () {
        this.sectionsListView = (ListView) this.getView().findViewById(R.id.section_listview);
        this.loadingDialog = new ProgressDialog(this.getActivity());
        this.loadingDialog.setTitle(this.getString(R.string.loading_sections));
        this.loadingDialog.setMessage(this.getString(R.string.waiting_message));
        this.loadingDialog.setCancelable(false); // make it modal
        this.loadingDialog.show();

        // Set adapter for the section list view, will retrieve section info from backend internally
        SectionListAdapter sectionAdapter = new SectionListAdapter((MainActivity) this.getActivity());
        sectionAdapter.setOnSectionsLoadedListener(this); // set listener to be notified when loading stops and progressdialog can be dismissed
        this.sectionsListView.setAdapter(sectionAdapter); // will generate views and subview for explistview

        // Register on child click listener
        this.sectionsListView.setOnItemClickListener(this);
    }

    /**
     * Called when a section in the list is clicked
     * Checks Realm DB if the section exists in the DB, if it does then its registered
     * If its registered, deregister it
     * if its not, register it
     *
     * Also sends registration/deregistration events to Google Analytics
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        SectionListAdapter adapter = (SectionListAdapter) this.sectionsListView.getAdapter();
        Section clickedSection = (Section) adapter.getItem(position);

        Realm realm = Realm.getDefaultInstance();
        RealmQuery<Section> query = realm.where(Section.class).equalTo("id", clickedSection.getId());
        Section savedSection = query.findFirst();
        boolean sectionIsRegistered = savedSection != null;

        if (sectionIsRegistered) {
            // Deregister the section
            this.trackDeregistrationEvent(savedSection);
            realm.beginTransaction();
            savedSection.removeFromRealm();
            realm.commitTransaction();
        } else {
            // Register the section
            this.trackRegistrationEvent(clickedSection);
            realm.beginTransaction();
            realm.copyToRealm(clickedSection);
            realm.commitTransaction();
        }

        realm.close();

        adapter.notifyDataSetChanged(); // update views
    }

    private void trackRegistrationEvent(Section section) {
        this.trackSectionEvent(section, true);
    }

    private void trackDeregistrationEvent(Section section) {
        this.trackSectionEvent(section, false);
    }

    private void trackSectionEvent(Section section, boolean isRegistering) {
        int actionStringResourceID = isRegistering ? R.string.register_section_action_name : R.string.deregister_section_action_name;
        String sectionStr =  section.getCourse().getDepartment() + " " + section.getCourse().getNumber() + ": " + section.getName();
        Tracker tracker = ((MainActivity) this.getActivity()).getDefaultTracker();
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(this.getString(R.string.section_category_name))
                .setAction(this.getString(actionStringResourceID))
                .setLabel(sectionStr)
                .setValue(section.getId())
                .build());
    }

    @Override
    public void onLoaded(List<Section> sections) {
        if (this.loadingDialog != null && this.loadingDialog.isShowing()) {
            this.loadingDialog.dismiss();
        }
    }

    @Override
    public void onLoadingError(Exception e) {
        e.printStackTrace();
        if (this.loadingDialog != null && this.loadingDialog.isShowing()) {
            this.loadingDialog.dismiss();
        }
    }
}
