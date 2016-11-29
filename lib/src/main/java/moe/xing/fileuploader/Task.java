package moe.xing.fileuploader;

import android.support.annotation.Nullable;

/**
 * Created by Qi Xingchen on 16-11-29.
 * <p>
 * 任务
 */

public class Task {
    private String taskID;
    private int index;
    private String filePath;
    @UploadService.STATUE
    private int statue;
    @Nullable
    private String Url;

    public String getTaskID() {
        return taskID;
    }

    public void setTaskID(String taskID) {
        this.taskID = taskID;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @UploadService.STATUE
    public int getStatue() {
        return statue;
    }

    public void setStatue(int statue) {
        this.statue = statue;
    }

    @Nullable
    public String getUrl() {
        return Url;
    }

    public void setUrl(@Nullable String url) {
        Url = url;
    }
}
