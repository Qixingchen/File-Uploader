package moe.xing.fileuploader;

import android.accounts.NetworkErrorException;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import rx.Observable;

/**
 * Created by Qi Xingchen on 16-11-29.
 * <p>
 * 上传图片
 */

class RetrofitNetwork extends moe.xing.network.RetrofitNetwork {

    /**
     * 传图
     *
     * @param file 图片文件
     * @return 图片网络地址
     * @throws NetworkErrorException 网络错误等
     * @throws IOException           if a problem occurred talking to the server.
     * @throws RuntimeException      (and subclasses) if an unexpected error occurs creating the request
     *                               or decoding the response.
     */
    @NonNull
    static UpimgBean.DataEntity UploadImage(@NonNull File file) throws Throwable {
        Retrofit retrofit = new Retrofit.Builder()
                //.callbackExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                .client(okHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .baseUrl("http://crm.yunyuer.com/app/api/ci123/other/")
                .build();


        RequestBody requestBody = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body =
                MultipartBody.Part.createFormData("Img", file.getName(), requestBody);


        Response<UpimgBean> upimgBeanResponse = retrofit.create(uploadImg.class).uploadImage(body)
                .execute();
        if (!upimgBeanResponse.isSuccessful()) {
            throw new NetworkErrorException(upimgBeanResponse.errorBody().string());
        }
        if (!"1".equals(upimgBeanResponse.body().getRet())) {
            throw new NetworkErrorException(upimgBeanResponse.body().getErrMsg());
        }

        return upimgBeanResponse.body().getData();

    }

    interface uploadImg {
        @Multipart
        @POST("upimg/")
        Observable<UpimgBean> uploadPic(@Part MultipartBody.Part pic);

        @Multipart
        @POST("upimg/")
        Call<UpimgBean> uploadImage(@Part MultipartBody.Part image);
    }
}
