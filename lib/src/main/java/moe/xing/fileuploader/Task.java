package moe.xing.fileuploader;

import android.databinding.Bindable;
import android.databinding.Observable;
import android.databinding.PropertyChangeRegistry;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.Serializable;

import moe.xing.baseutils.Init;

/**
 * Created by Qi Xingchen on 16-11-29.
 * <p>
 * 任务
 */

public class Task implements Observable, Serializable {
    private String taskID;
    private int index;
    private File file;
    @UploadService.STATUE
    private int statue;
    @Nullable
    private String Url;
    @Nullable
    private String errorMessage;
    private transient PropertyChangeRegistry propertyChangeRegistry = new PropertyChangeRegistry();

    public Task() {
    }

    public Task(String taskID, int index, File file) {
        this.taskID = taskID;
        this.index = index;
        this.file = file;
    }

    @Nullable
    @Bindable
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        notifyChange(BR.errorMessage);
    }

    @Override
    public String toString() {
        return "Task{" +
                "taskID='" + taskID + '\'' +
                ", index=" + index +
                ", filePath='" + file.getAbsolutePath() + '\'' +
                ", statue=" + statue +
                ", Url='" + Url + '\'' +
                '}';
    }

    @Bindable
    public String getTaskID() {
        return taskID;
    }

    public void setTaskID(String taskID) {
        this.taskID = taskID;
        notifyChange(BR.taskID);
    }

    @Bindable
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
        notifyChange(BR.index);
    }

    @Bindable
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        notifyChange(BR.file);
    }

    @Bindable
    public int getStatue() {
        return statue;
    }

    public void setStatue(int statue) {
        this.statue = statue;
        notifyChange(BR.statue);
        notifyChange(BR.statueString);
    }

    @Bindable
    public String getStatueString() {
        return Init.getApplication().getResources().getStringArray(R.array.statue)[statue - 1];
    }

    public String getTaskIDAndIndex() {
        return "taskid:" + taskID + " index:" + index;
    }

    @Bindable
    public String getUrl() {
        return Url;
    }

    public void setUrl(String Url) {
        this.Url = Url;
        notifyChange(BR.url);
    }

    private void notifyChange(int propertyId) {
        if (propertyChangeRegistry == null) {
            propertyChangeRegistry = new PropertyChangeRegistry();
        }
        propertyChangeRegistry.notifyChange(this, propertyId);
    }

    @Override
    public void addOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        if (propertyChangeRegistry == null) {
            propertyChangeRegistry = new PropertyChangeRegistry();
        }
        propertyChangeRegistry.add(callback);

    }

    @Override
    public void removeOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        if (propertyChangeRegistry != null) {
            propertyChangeRegistry.remove(callback);
        }
    }
}
