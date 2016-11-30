package moe.xing.fileuploader_app;

import android.databinding.BindingAdapter;
import android.text.TextUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;

import java.io.File;
import java.net.URI;
import java.util.List;

import moe.xing.baseutils.network.cookies.MyCookiesManager;
import moe.xing.network.RetrofitNetwork;
import okhttp3.Cookie;
import okhttp3.HttpUrl;

/**
 * Created by Qi Xingchen on 16-9-27.
 */

public class DataBindingAdapter {


    /**
     * 从网络加载图片
     */
    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        List<Cookie> cookies = new MyCookiesManager().loadForRequest(HttpUrl.get(URI.create(url)));
        StringBuilder cookieString = new StringBuilder();
        for (Cookie cookie : cookies) {
            cookieString.append(cookie.name()).append("=").append(cookie.value()).append(";");
        }

        GlideUrl glideUrl = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("User-Agent", RetrofitNetwork.UA())
                .addHeader("Cookie", cookieString.toString())
                .build());
        try {
            Glide.with(view.getContext()).load(glideUrl).centerCrop().into(view);
        } catch (IllegalArgumentException ignore) {
        }
    }

    /**
     * 加载资源图片
     */
    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, int url) {
        try {
            Glide.with(view.getContext()).load(url).into(view);
        } catch (IllegalArgumentException ignore) {
        }
    }

    /**
     * 从文件加载图片
     */
    @BindingAdapter({"imageUrl"})
    public static void loadImage(ImageView view, File url) {
        if (url == null || !url.exists()) {
            return;
        }
        try {
            Glide.with(view.getContext()).load(url).into(view);
        } catch (IllegalArgumentException ignore) {
        }
    }


}
