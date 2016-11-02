package com.panda.http.spiderman;

import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by steven on 2016/10/12.
 */

public interface BasicApiService {

    @POST("{url}")
    Call<ResponseBody> post(
            @Path("url") String url,
            @QueryMap Map<String, String> maps);

    @GET("{url}")
    Call<ResponseBody> get(
            @Path("url") String url,
            @QueryMap Map<String, String> maps);

    @Multipart
    @POST("{url}")
    Call<ResponseBody> upLoadFile(
            @Path("url") String url,
            @PartMap Map<String, RequestBody> params);

    @Multipart
    @POST("{url}")
    Call<ResponseBody> uploadFiles(
            @Path("url") String url,
            @Part("filename") String description,
            @PartMap() Map<String, RequestBody> maps);

    @Streaming
    @GET
    Call<ResponseBody> downloadFile(@Url String fileUrl);


    @GET
    Call<ResponseBody> getTest(@Url String fileUrl,
                               @QueryMap Map<String, String> maps);

}
