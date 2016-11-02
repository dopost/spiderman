package com.panda.http.spiderman;

import android.content.Context;
import android.util.Log;


import com.panda.http.spiderman.utils.NetworkUtil;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 缓存处理
 */
public class CaheInterceptor implements Interceptor {

    private final static String TAG = "CaheInterceptor";

    private Context context;
    private String cacheControlValue;
    private String cacheOnlineControlValue;
    //set cahe times is 3 days
    private static final int maxStale = 60 * 60 * 24 * 3;
    // read from cache for 60 s
    private static final int maxOnlineStale = 60;

    public CaheInterceptor(Context context) {
        this(context, String.format("max-stale=%d", maxOnlineStale));
    }

    public CaheInterceptor(Context context, String cacheControlValue) {
       this(context, cacheControlValue, String.format("max-stale=%d", maxStale));
    }

    public CaheInterceptor(Context context, String cacheControlValue, String cacheOnlineControlValue) {
        this.context = context;
        this. cacheControlValue = cacheControlValue;
        this.cacheOnlineControlValue = cacheOnlineControlValue;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        if (NetworkUtil.isNetworkAvailable(context)) {
            Response response = chain.proceed(request);
            String cacheControl = request.cacheControl().toString();
            Log.d(TAG, maxOnlineStale + " load cahe " + cacheControl);
            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "public, " + cacheOnlineControlValue)
                    .build();
        } else {
            Log.d(TAG, " no network load cahe");
            request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();
            Response response = chain.proceed(request);
            return response.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .header("Cache-Control", "public, only-if-cached, " + cacheControlValue)
                    .build();
        }
    }
}
