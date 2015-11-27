package com.classtranscribe.classcapture.services;

import android.content.Context;

import com.classtranscribe.classcapture.R;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;

/**
 * Created by sourabhdesai on 9/18/15.
 */
public class DeviceIDRequestInterceptor implements Interceptor {

    private final Context context;

    public DeviceIDRequestInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        String deviceID = SettingsService.getDeviceID(this.context);
        String headerName = this.context.getString(R.string.device_id_header);

        Request deviceIDRequest = request.newBuilder()
                .addHeader(headerName, deviceID)
                .build();

        return chain.proceed(deviceIDRequest);
    }
}
