package com.classtranscribe.classcapture.controllers.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.classtranscribe.classcapture.R;

/**
 * Created by sourabhdesai on 6/18/15.
 */
public class RecordingsListAdapter implements ListAdapter {

    private final Context context;

    public RecordingsListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public String getItem(int position) {
        return "Item at " + position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View recordingListItemView = null;

        if (convertView != null) {
            // can be reused
            recordingListItemView = convertView;
        } else {
            recordingListItemView = View.inflate(this.context, R.layout.listitem_recording, null);
        }

        String recordingTitle = this.getItem(position);

        // Set text for textview in list item view to be recordingTitle
        TextView titleTextView = (TextView) recordingListItemView.findViewById(R.id.recordingTitle);
        titleTextView.setText(recordingTitle);

        return recordingListItemView;
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
        return this.getCount() == 0;
    }
}
