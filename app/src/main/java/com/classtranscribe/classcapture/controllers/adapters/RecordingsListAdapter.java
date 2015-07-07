package com.classtranscribe.classcapture.controllers.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.classtranscribe.classcapture.R;
import com.classtranscribe.classcapture.models.Recording;
import com.classtranscribe.classcapture.models.RecordingService;

import java.util.ArrayList;
import java.util.List;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by sourabhdesai on 6/18/15.
 */
public class RecordingsListAdapter implements ListAdapter {

    private final Context context;
    private final RecordingService recordingService;
    private List<Recording> recordings;

    public RecordingsListAdapter(Context context) {
        // Save context for view creation within adapter
        this.context = context;

        this.recordingService = RecordingServiceProvider.getInstance(this.context);

        this.recordings = new ArrayList<Recording>(); // Initially empty
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
    public void registerDataSetObserver(final DataSetObserver observer) {
        // While the DataSetObserver is still in scope, make the request
        // Can call on observers methods within callback
        this.recordingService.recordingList(new Callback<List<Recording>>() {
            @Override
            public void success(List<Recording> recordings, Response response) {
                RecordingsListAdapter.this.recordings = recordings;
                observer.onChanged();
            }

            @Override
            public void failure(RetrofitError error) {
                System.out.println(error);
                observer.onInvalidated();
            }
        });
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return this.recordings.size();
    }

    @Override
    public Recording getItem(int position) {
        return this.recordings.get(position);
    }

    @Override
    public long getItemId(int position) {
        return this.recordings.get(position).id;
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

        Recording recording = this.getItem(position);

        // Set text for textview in list item view to be recordingTitle
        TextView titleTextView = (TextView) recordingListItemView.findViewById(R.id.recordingTitle);
        titleTextView.setText(recording.toString());

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
        return this.recordings.isEmpty();
    }
}
