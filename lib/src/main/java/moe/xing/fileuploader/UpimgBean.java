package moe.xing.fileuploader;

import com.google.gson.annotations.SerializedName;

import moe.xing.network.BaseBean;

/**
 * Created by Qi Xingchen on 2016/4/29 0029.
 * <p>
 * <a href="http://m.ci123.com/apidoc/zhinan/#api-groupList2-PostHttpMCi123ComAppApiZhinanDiaryUpimg">上传图片后的返回
 * </a>
 */
class UpimgBean extends BaseBean {

    /**
     * url :
     * width : 0
     * height : 0
     */

    @SerializedName("data")
    private DataEntity data;

    public DataEntity getData() {
        return data;
    }

    public void setData(DataEntity data) {
        this.data = data;
    }

    public static class DataEntity {
        @SerializedName("url")
        private String url;
        @SerializedName("width")
        private String width;
        @SerializedName("height")
        private String height;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getWidth() {
            return width;
        }

        public void setWidth(String width) {
            this.width = width;
        }

        public String getHeight() {
            return height;
        }

        public void setHeight(String height) {
            this.height = height;
        }
    }
}
