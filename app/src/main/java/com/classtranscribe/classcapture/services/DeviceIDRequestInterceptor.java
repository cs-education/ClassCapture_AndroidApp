package com.classtranscribe.classcapture.services;

import android.content.Context;
import android.util.Log;

import com.classtranscribe.classcapture.R;

import retrofit.RequestInterceptor;

/**
 * Created by sourabhdesai on 9/18/15.
 */
public class DeviceIDRequestInterceptor implements RequestInterceptor {

    private final Context context;

    public DeviceIDRequestInterceptor(Context context) {
        this.context = context;
    }


    @Override
    public void intercept(RequestFacade request) {
        String deviceID = SettingsService.getDeviceID(this.context);
        String headerName = this.context.getString(R.string.device_id_header);
        request.addHeader(headerName, deviceID);
    }
}
