package com.panda.http.spiderman;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by steven on 2016/10/12.
 */

public class ParameterInterceptor implements Interceptor {

    private Map<String, String> parameters;

    public ParameterInterceptor(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        HttpUrl orgUrl = chain.request().url();
        HttpUrl.Builder newUrlBuilder = orgUrl.newBuilder();

        if (parameters != null && parameters.size() > 0) {
            Set<String> keys = parameters.keySet();
            for (String parameterKey : keys) {
                newUrlBuilder.addQueryParameter(parameterKey, parameters.get(parameterKey));
            }
        }
        Request.Builder builder = chain.request().newBuilder().url(newUrlBuilder.build());

        return chain.proceed(builder.build());
    }
}
