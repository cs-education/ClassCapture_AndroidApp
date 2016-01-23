package com.classtranscribe.classcapture.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.models.Course;
import com.classtranscribe.classcapture.models.Section;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.realm.Realm;

/**
 * Created by sourabhdesai on 9/17/15.
 */
public class SectionSpinnerAdapter implements SpinnerAdapter {

    private final Context context;
    private final Realm realm;
    private final List<Section> sections;

    public SectionSpinnerAdapter(Context context, Realm realm, List<Section> sections) {
        this.context = context;
        this.realm = realm;
        this.sections = sections;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View sectionListItemView = null;
        if (convertView != null) {
            // convertView is the view that used to be at this position, so returning it here will just recycle it
            sectionListItemView = convertView;
        } else {
            sectionListItemView = View.inflate(this.context, android.R.layout.simple_spinner_item, null);
        }

        Section currSection = this.getItem(position);
        Course currCourse = this.realm.where(Course.class).equalTo("id", currSection.getCourse()).findFirst();
        String sectionStr = currCourse.getDepartment() + " " + currCourse.getNumber() + ": " + currSection.getName();

        TextView sectionTextView = (TextView) sectionListItemView;
        sectionTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 25);
        sectionTextView.setText(sectionStr);

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
        return this.sections.size();
    }

    @Override
    public Section getItem(int position) {
        return this.sections.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.getItem(position).getId();
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
        return this.sections.isEmpty();
    }
}
