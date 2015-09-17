package com.classtranscribe.classcapture.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.models.Section;

import java.util.List;

import io.realm.Realm;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public class SectionListAdapter implements SpinnerAdapter {

    private final Context context;
    private final List<Section> registeredSections;

    public SectionListAdapter(Context context) {
        this.context = context;
        this.registeredSections = Realm.getDefaultInstance().allObjects(Section.class);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View sectionListItemView = null;
        if (convertView != null) {
            // convertView is the view that used to be at this position, so returning it here will just recycle it
            sectionListItemView = convertView;
        } else {
            sectionListItemView = View.inflate(this.context, R.layout.text_listitem, null);
        }

        Section currSection = (Section) this.getItem(position);
        TextView sectionTextView = (TextView) sectionListItemView;
        sectionTextView.setText(currSection.toString());

        return sectionListItemView;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return this.registeredSections.size();
    }

    @Override
    public Object getItem(int position) {
        return this.registeredSections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.registeredSections.get(position).id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // To see the difference between getView and getDropDownView see this:
        //  http://stackoverflow.com/questions/13433874/difference-between-getview-getdropdownview-in-spinneradapter
        return this.getDropDownView(position, convertView, parent);
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
        return this.registeredSections.isEmpty();
    }
}
