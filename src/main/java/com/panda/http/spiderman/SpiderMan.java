package com.panda.http.spiderman;

import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.panda.http.spiderman.cookie.PersistentCookieJar;
import com.panda.http.spiderman.cookie.cache.SetCookieCache;
import com.panda.http.spiderman.cookie.persistence.SharedPrefsCookiePersistor;
import com.panda.http.spiderman.utils.GenericUtils;
import com.panda.http.spiderman.utils.ObjectUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.Cache;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionPool;
import okhttp3.CookieJar;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.CallAdapter;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.Call;
import rx.Observable;

/**
 * Created by steven on 2016/10/12.
 */

public final class SpiderMan {

    private final static String TAG = "SpiderMan";

    private Context mContext;
    protected OkHttpClient okHttpClient;
    protected Retrofit retrofit;
    protected BasicApiService mBasicApiService;

    private Map<String, Observable<ResponseBody>> downMaps = new HashMap<String, Observable<ResponseBody>>(){};

    private SpiderMan() {
    }


    public static final class Builder {

        private static final long caheMaxSize = 10 * 1024 * 1024; //10M

        private static final int DEFAULT_TIMEOUT = 5;
        private static final int MAX_IDLE = 5;
        private static final long  KEEP_ALIVE_DURATION = 8; //活跃时长

        private Context mContext;

        ExecutorService executorService = Executors.newFixedThreadPool(1);


        private OkHttpClient.Builder okhttpBuilder;
        private Retrofit.Builder retrofitBuilder;

        private Cache mCache = null;
        private Proxy mProxy;
        private File mCacheDirectory;

        private ConnectionPool mConnectionPool;

        private CookieJar mCookieJar;

        public Builder(Context mContext) {
            this.mContext = mContext;
            okhttpBuilder = new OkHttpClient.Builder();
            retrofitBuilder = new Retrofit.Builder();
        }

        public Builder baseUrl(String url) {
            retrofitBuilder.baseUrl(ObjectUtils.checkNotNull(url, "mBaseUrl == null"));
            return this;
        }

        public Builder addInterceptor(Interceptor interceptor) {
            okhttpBuilder.addInterceptor(ObjectUtils.checkNotNull(interceptor, "interceptor == null"));
            return this;
        }

        public Builder addNetworkInterceptor(Interceptor interceptor) {
            okhttpBuilder.addNetworkInterceptor(interceptor);
            return this;
        }

        public Builder addHeader(Map<String, String> headers) {
            if (headers != null && headers.size() > 0) {
                addInterceptor(new HeaderInterceptor(headers));
            }
            return this;
        }

        public Builder openLog() {
            addNetworkInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));
            return this;
        }

        public Builder addSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
            okhttpBuilder.sslSocketFactory(ObjectUtils.checkNotNull(sslSocketFactory, "sslSocketFactory = null"));
            return this;
        }

        public Builder addHostnameVerifier(HostnameVerifier hostnameVerifier) {
            okhttpBuilder.hostnameVerifier(ObjectUtils.checkNotNull(hostnameVerifier, "hostnameVerifier = null"));
            return this;
        }

       /* public Builder addParameters(Map<String, String> parameters) {
            this.parameters = parameters;
           // okhttpBuilder.addInterceptor(new BaseInterceptor((Utils.checkNotNull(parameters, "parameters == null"))));
            return this;
        }*/

        public Builder addCertificatePinner(CertificatePinner certificatePinner) {
            okhttpBuilder.certificatePinner(ObjectUtils.checkNotNull(certificatePinner, "certificatePinner = null"));
            return this;
        }


        /**
         * @param hosts
         * @param certificates RawResourceId
         * @return
         */
        public Builder addHttps(String[] hosts, int[] certificates) {
            if (hosts == null) throw new NullPointerException("hosts = null");
            if (certificates == null) throw new NullPointerException("ids = null");

            addSSLSocketFactory(HttpsFactroy.createSSLSocketFactory(mContext, certificates));
            addHostnameVerifier(HttpsFactroy.createHostnameVerifier(hosts));
            return this;
        }

        /**
         * 添加缓存请求头返回
         * @param cache
         * @return
         */
        public Builder addCache(Cache cache) {
            int maxStale = 60 * 60 * 24 * 3;
            return addCacheAge(cache, maxStale);
        }

        /**
         * @return
         */
        public Builder addCacheAge(Cache cache, final int cacheTime) {
            addCache(cache, String.format("max-age=%d", cacheTime));
            return this;
        }

        /**
         * @param cache
         * @param cacheTime ms
         * @return
         */
        public Builder addCacheStale(Cache cache, final int cacheTime) {
            addCache(cache, String.format("max-stale=%d", cacheTime));
            return this;
        }

        /**
         * @param cache
         * @param cacheControlValue Cache-Control值
         * @return
         */
        public Builder addCache(Cache cache, final String cacheControlValue) {
            Interceptor REWRITE_CACHE_CONTROL_INTERCEPTOR = new CaheInterceptor(mContext, cacheControlValue);
            addInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR);
            addNetworkInterceptor(REWRITE_CACHE_CONTROL_INTERCEPTOR);
            return this;
        }

        public Builder connectionPool(ConnectionPool connectionPool) {
            if (connectionPool == null) throw new NullPointerException("mConnectionPool == null");
            this.mConnectionPool = connectionPool;
            return this;
        }

        public Builder connectTimeout(int timeout, TimeUnit unit) {
            if (timeout != -1) {
                okhttpBuilder.connectTimeout(timeout, unit);
            } else {
                okhttpBuilder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            }
            return this;
        }

        public Builder writeTimeout(int timeout, TimeUnit unit) {
            if (timeout != -1) {
                okhttpBuilder.writeTimeout(timeout, unit);
            } else {
                okhttpBuilder.writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
            }
            return this;
        }

        public Builder proxy(Proxy proxy) {
            okhttpBuilder.proxy(ObjectUtils.checkNotNull(proxy, "mProxy == null"));
            return this;
        }

        public Builder cookieManager(CookieJar cookieJar) {
            okhttpBuilder.cookieJar(ObjectUtils.checkNotNull(cookieJar, "mCookieJar == null"));
            this.mCookieJar = cookieJar;
            return this;
        }


        /**
         * Add converter factory for serialization and deserialization of objects.
         */
        public Builder addConverterFactory(Converter.Factory factory) {
            retrofitBuilder.addConverterFactory(ObjectUtils.checkNotNull(factory, "ConverterFactory == null"));
            return this;
        }

        /**
         * Add a call adapter factory for supporting service method return types other than {@link CallAdapter
         * }.
         */
        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            retrofitBuilder.addCallAdapterFactory(ObjectUtils.checkNotNull(factory, "CallAdapterFactory == null"));
            return this;
        }


        public Builder callbackExecutor(Executor executor) {
            retrofitBuilder.callbackExecutor(ObjectUtils.checkNotNull(executor, "Executor == null"));
            return this;
        }

        public Builder addDefaultCallbackExecutor() {
            retrofitBuilder.callbackExecutor(executorService);
            return this;
        }

        public SpiderMan build() {
            //retrofit = retrofitBuilder.mBaseUrl(mBaseUrl).build();
            SpiderMan spiderMan = new SpiderMan();
            applyConfig(spiderMan);
            return spiderMan;
        }

        private void applyConfig(SpiderMan spiderMan) {

            spiderMan.mContext = mContext;

            //handle mCache
            if (mCacheDirectory == null) {
                mCacheDirectory = new File(mContext.getCacheDir(), "spiderman_cache");
            }
            try {
                if (mCache == null) {
                    mCache = new Cache(mCacheDirectory, caheMaxSize);
                }
            } catch (Exception e) {
                Log.e(TAG, "Could not create http mCache", e);
            }
            okhttpBuilder.cache(mCache);
            addCache(mCache);


            //handle mConnectionPool
            if (mConnectionPool == null) {
                mConnectionPool = new ConnectionPool(MAX_IDLE, KEEP_ALIVE_DURATION, TimeUnit.SECONDS);
            }
            okhttpBuilder.connectionPool(mConnectionPool);


            //handle cookie
            if (mCookieJar == null) {
                okhttpBuilder.cookieJar(
                        new PersistentCookieJar(
                                new SetCookieCache(),
                                new SharedPrefsCookiePersistor(mContext)));
            }

            spiderMan.okHttpClient = okhttpBuilder.build();


            spiderMan.retrofit = retrofitBuilder.client(spiderMan.okHttpClient)
                    .build();


            spiderMan.mBasicApiService = spiderMan.retrofit.create(BasicApiService.class);
        }

    }


    /**
     * create ApiService
     */
    public <T> T create(final Class<T> service) {
        return retrofit.create(service);
    }


    public <T> T get(String url, Map<String, String> maps, Class<T> type) {
        Call<ResponseBody> call = mBasicApiService.get(url, maps);
        try {
            Response<ResponseBody> result = call.execute();
            if (result.isSuccessful()) {
                byte[] bytes = result.body().bytes();
                String jsStr = new String(bytes);
                return JSONObject.parseObject(jsStr, type);
            } else {
                Log.e(TAG, result.errorBody().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> T post(String url, Map<String, String> maps, Class<T> type) {
        Call<ResponseBody> call = mBasicApiService.post(url, maps);
        try {
            Response<ResponseBody> result = call.execute();
            if (result.isSuccessful()) {
                byte[] bytes = result.body().bytes();
                String jsStr = new String(bytes);
                return JSONObject.parseObject(jsStr, type);
            } else {
                Log.e(TAG, result.errorBody().string());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> void callGet(String url, Map<String, String> maps, final ResponseCallBack<T> responseCallBack) {
        Call<ResponseBody> call = mBasicApiService.get(url, maps);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                byte[] bytes = new byte[0];
                try {
                    bytes = response.body().bytes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String jsStr = new String(bytes);
                if (responseCallBack != null) {

                    Type finalType = GenericUtils.getClassGenricType(responseCallBack);

                    Log.d(TAG, "-->:" + "Type:" + finalType);

                    T t = JSONObject.parseObject(jsStr, finalType);
                    responseCallBack.onSuccee(t);
                }


            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (responseCallBack != null) {
                    responseCallBack.onError(t);
                }
            }
        });

    }

    public <T> void callPost(String url, Map<String, String> maps, final ResponseCallBack<T> responseCallBack) {
        Call<ResponseBody> call = mBasicApiService.post(url, maps);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                byte[] bytes = new byte[0];
                try {
                    bytes = response.body().bytes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String jsStr = new String(bytes);
                if (responseCallBack != null) {

                    Type finalType = GenericUtils.getClassGenricType(responseCallBack);

                    Log.d(TAG, "-->:" + "Type:" + finalType);

                    T t = JSONObject.parseObject(jsStr, finalType);
                    responseCallBack.onSuccee(t);
                }


            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (responseCallBack != null) {
                    responseCallBack.onError(t);
                }
            }
        });

    }

    public void download(final String url, final DownloadCallBack downloadCallback) {
        Call<ResponseBody> call = mBasicApiService.downloadFile(url);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                DownLoadTask task = new DownLoadTask(mContext, response.body(), downloadCallback);
                DownloadManager.newInstance().addTask(url, task);

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (downloadCallback != null) {
                    downloadCallback.onError(t);
                }
            }
        });
    }

    public void upload(final String url, String name, File file, final UploadCallBack uploadCallBack) {

        UploadRequestBody body = new UploadRequestBody(file, uploadCallBack);

        Map<String,RequestBody> map = new HashMap<>();
        map.put(""+name+"\"; filename=\""+file.getName()+"", body);

        Call<ResponseBody> call = mBasicApiService.upLoadFile(url, map);


        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (uploadCallBack != null) {
                    if (response.isSuccessful()) {
                        uploadCallBack.onSucess(response.body());

                    } else {
                        uploadCallBack.onError(new RuntimeException("上传失败"));
                    }
                }

            }


            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (uploadCallBack != null) {
                    uploadCallBack.onError(t);
                }
            }
        });
    }

    public void upload(final String url, String name, String mediaType, File file, final UploadCallBack uploadCallBack) {

        UploadRequestBody body = new UploadRequestBody(
                RequestBody.create(MediaType.parse(mediaType),file),
                uploadCallBack);

        Map<String,RequestBody> map = new HashMap<>();
        map.put(""+name+"\"; filename=\""+ file.getName() +"", body);

        Call<ResponseBody> call = mBasicApiService.upLoadFile(url, map);


        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (uploadCallBack != null) {
                    uploadCallBack.onSucess(response.body());
                }

            }


            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (uploadCallBack != null) {
                    uploadCallBack.onError(t);
                }
            }
        });
    }


    public interface ResponseCallBack<T> {

        public abstract void onError(Throwable e);

        public abstract void onSuccee(T response);

    }

    public interface DownloadCallBack {

        public void onError(Throwable e);

        public void onProgress(long total, long downloadedSize);

        public void onSucess(String path, String name, long fileSize);

    }

    public interface UploadCallBack {

        public void onError(Throwable e);

        public void onProgress(long total, long uploadedSize);

        public void onSucess(ResponseBody body);

    }

}
