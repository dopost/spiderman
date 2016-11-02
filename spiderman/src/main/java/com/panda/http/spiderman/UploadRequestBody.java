package com.panda.http.spiderman;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * 处理上传进度问题
 */
public class UploadRequestBody extends RequestBody {

    private RequestBody mRequestBody;
    private SpiderMan.UploadCallBack mProgressListener;

    private BufferedSink bufferedSink;

    private Handler handler;


    public UploadRequestBody(File file, SpiderMan.UploadCallBack progressListener) {
        this.mRequestBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
        this.mProgressListener = progressListener;
        handler = new Handler(Looper.getMainLooper());
    }

    public UploadRequestBody(RequestBody requestBody, SpiderMan.UploadCallBack progressListener) {
        this.mRequestBody = requestBody;
        this.mProgressListener = progressListener;
        handler = new Handler(Looper.getMainLooper());
    }

    //返回了requestBody的类型，想什么form-data/MP3/MP4/png等等等格式
    @Override
    public MediaType contentType() {
        return mRequestBody.contentType();
    }

    //返回了本RequestBody的长度，也就是上传的totalLength
    @Override
    public long contentLength() throws IOException {
        return mRequestBody.contentLength();
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (bufferedSink == null) {
            bufferedSink = Okio.buffer(sink(sink));
        }
        //写入
        mRequestBody.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            //当前写入字节数
            long bytesWritten = 0L;
            //总字节长度，避免多次调用contentLength()方法
            long contentLength = 0L;

            long lastTime = System.currentTimeMillis();
            long lastSize = 0;

            @Override
            public void write(Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    //获得contentLength的值，后续不再调用
                    contentLength = contentLength();
                }
                //增加当前写入的字节数
                bytesWritten += byteCount;


                //处理进度通知太频繁
                long nowTime = System.currentTimeMillis();
                long nowSize = bytesWritten;

                long rate = contentLength/10;

                if (nowTime - lastTime > 500 || nowSize-lastSize > rate
                        || nowSize == contentLength) {

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            //回调上传接口
                            mProgressListener.onProgress(contentLength, bytesWritten);
                        }
                    });

                }
            }
        };
    }
}