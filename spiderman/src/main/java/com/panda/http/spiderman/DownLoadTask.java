package com.panda.http.spiderman;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;

/**
 * Created by Steven on 2016-07-11.
 */
public class DownLoadTask {

    private SpiderMan.DownloadCallBack callBack;

    public static final String TAG = "DownLoadTask";

    private static String CONTENT_TYPE_APK = "application/vnd.android.package-archive";

    private static String CONTENT_TYPE_PNG = "image/png";

    private static String CONTENT_TYPE_JPG = "image/jpg";

    private String fileSuffix="";

    private Handler handler;

    private boolean isCancel = false;

    private Context mContext;

    private ResponseBody mBody;


    private String mUrl;

    public DownLoadTask(Context mContext, ResponseBody body, SpiderMan.DownloadCallBack callBack) {
        this.callBack = callBack;
        this.mContext = mContext;
        this.mBody =body;
        handler = new Handler(Looper.getMainLooper());
    }

    public void cancel(){
        isCancel = true;
    }

    private String generateTaskId() {
        return System.currentTimeMillis()+"";
    }


    public void setUrl(String url) {
        this.mUrl = url;
    }


    public boolean start() {

        Log.d(TAG, "contentType:>>>>"+ mBody.contentType().toString());

        String type = mBody.contentType().toString();

        if (type.equals(CONTENT_TYPE_APK)) {
           fileSuffix = ".apk";
        } else if (type.equals(CONTENT_TYPE_PNG)) {
            fileSuffix = ".png";
        }
        //TODO 其他同上 自己判断加入


        final String name = generateTaskId() + fileSuffix;
        final String savePath = mContext.getExternalFilesDir(null) + File.separator + name;
        Log.d(TAG, "savePath:-->" + savePath);
        try {
            File downloadFile = new File(savePath);

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                final long fileSize = mBody.contentLength();
                long fileSizeDownloaded = 0;
                Log.d(TAG, "file length: "+ fileSize);
                inputStream = mBody.byteStream();
                outputStream = new FileOutputStream(downloadFile);

                long lastTime = System.currentTimeMillis();
                long lastSize = 0;

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1 || isCancel) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);
                    fileSizeDownloaded += read;

                    long nowTime = System.currentTimeMillis();
                    long nowSize = fileSizeDownloaded;

                    long rate = fileSize/10;

                    if (nowTime - lastTime > 500 || nowSize-lastSize > rate
                            || nowSize == fileSize) {
                        lastTime = nowTime;
                        lastSize = nowSize;
                        Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                        if (callBack != null) {
                            final long finalFileSizeDownloaded = fileSizeDownloaded;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callBack.onProgress(fileSize, finalFileSizeDownloaded);
                                }
                            });
                        }
                    }

                }

                outputStream.flush();
                Log.d(TAG, "file downloaded: " + fileSizeDownloaded + " of " + fileSize);



                if (callBack != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callBack.onSucess(savePath, name, fileSize);
                            DownloadManager.newInstance().removeTask(mUrl);

                        }
                    });
                    Log.d(TAG, "file downloaded: " + fileSizeDownloaded + " of " + fileSize);
                }

                return true;
            } catch (final IOException e) {
                if (callBack != null) {
                    handler.post(new Runnable() {
                         @Override
                         public void run() {
                             callBack.onError(e);
                             DownloadManager.newInstance().removeTask(mUrl);
                         }
                     });

                }
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (final IOException e) {
            if (callBack != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onError(e);
                        DownloadManager.newInstance().removeTask(mUrl);
                    }
                });
            }
            return false;
        }
    }
}
