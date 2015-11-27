package com.classtranscribe.classcapture.adapters;

import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;

import com.classtranscribe.classcapture.controllers.activities.MainActivity;
import com.classtranscribe.classcapture.models.Course;
import com.classtranscribe.classcapture.models.Section;
import com.classtranscribe.classcapture.services.CustomCB;
import com.classtranscribe.classcapture.services.SectionService;
import com.classtranscribe.classcapture.services.SectionServiceProvider;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by sourabhdesai on 9/20/15.
 */
public class SectionListAdapter implements ListAdapter {

    private final MainActivity context;
    private DataSetObserver observer;
    private List<Section> sections;
    // Keep track of whether the section at a particular position of the list has been updated locally from backend request
    private boolean[] updatedSectionAt;
    private OnSectionsLoadedListener onLoadedListener;

    public SectionListAdapter(MainActivity mainActivity) {
        this.context = mainActivity;
        this.sections = new ArrayList<Section>();
        this.updatedSectionAt = new boolean[0];
        this.onLoadedListener = null;
    }

    public void setOnSectionsLoadedListener(OnSectionsLoadedListener listener) {
        this.onLoadedListener = listener;
    }

    @Override
    public void registerDataSetObserver(final DataSetObserver observer) {
        this.observer = observer;
        SectionService sectionService = SectionServiceProvider.getInstance(this.context);
        sectionService.listSections(new CustomCB<List<Section>>(this.context, SectionListAdapter.class.getName()) {
            @Override
            public void onResponse(Response<List<Section>> response, Retrofit retrofit) {
                SectionListAdapter.this.sections = response.body();
                SectionListAdapter.this.updatedSectionAt = new boolean[sections.size()];

                // notify the listener if its set
                if (SectionListAdapter.this.onLoadedListener != null) {
                    SectionListAdapter.this.onLoadedListener.onLoaded(sections);
                }

                observer.onChanged();
            }

            @Override
            public void onRequestFailure(Throwable t) {
                if (SectionListAdapter.this.onLoadedListener != null) {
                    SectionListAdapter.this.onLoadedListener.onLoadingError(t);
                }
            }
        });
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        this.observer = null;
    }

    public void notifyDataSetChanged() {
        if (this.observer != null) {
            this.observer.onChanged();
        }
    }

    @Override
    public int getCount() {
        return this.sections.size();
    }

    @Override
    public Object getItem(int position) {
        return this.sections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.sections.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View itemView = null;

        if (convertView == null) {
            itemView = View.inflate(this.context, android.R.layout.simple_list_item_checked, null);
        } else {
            itemView = convertView;
        }

        Section currSection = (Section) this.getItem(position);
        Course currCourse = currSection.getCourse();
        String sectionStr = currCourse.getDepartment() + " " + currCourse.getNumber() + ": " + currSection.getName();

        CheckedTextView checkedTextView = (CheckedTextView) itemView;
        checkedTextView.setText(sectionStr);

        Realm realm = Realm.getDefaultInstance();
        Section registeredSection = realm.where(Section.class).equalTo("id", currSection.getId()).findFirst();
        boolean sectionIsRegistered = registeredSection != null;
        checkedTextView.setChecked(sectionIsRegistered);

        boolean alreadyUpdatedLocally = SectionListAdapter.this.updatedSectionAt[position];
        if (sectionIsRegistered && !alreadyUpdatedLocally) {
            // Update local copy of currSection with one received from backend
            realm.beginTransaction();
            realm.copyToRealmOrUpdate(currSection);
            realm.commitTransaction();
            this.updatedSectionAt[position] = true;
        }

        realm.close();

        return itemView;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.sections.isEmpty();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    public static interface OnSectionsLoadedListener {
        public void onLoaded(List<Section> sections);
        public void onLoadingError(Throwable t);
    }
}
