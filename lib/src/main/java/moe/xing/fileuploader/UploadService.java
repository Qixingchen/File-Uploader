package moe.xing.fileuploader;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;

import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.log.CustomLogger;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import me.shaohui.advancedluban.Luban;
import moe.xing.baseutils.Init;
import moe.xing.baseutils.utils.LogHelper;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Qi Xingchen on 16-11-28.
 * <p>
 * 上传服务
 */

public class UploadService extends Service {

    @NonNull
    private UploadBinder mBinder = new UploadBinder();

    @DrawableRes
    private int logo;

    @ColorRes
    private int color;

    @Nullable
    private PendingIntent mPendingIntent;

    private JobManager mJobManager;

    @Nullable
    private UploadServiceEvent mEvent;

    public void setEvent(@Nullable UploadServiceEvent event) {
        mEvent = event;
    }

    private UploadJob.UploadEvent mUploadEvent = new UploadJob.UploadEvent() {
        @Override
        public void start(@NonNull File file, @NonNull String taskID, int index) {
            if (mEvent != null) {
                mEvent.start(file, taskID, index);
            }
        }

        @Override
        public void retrying(@NonNull File file, @NonNull String taskID, int index) {
            if (mEvent != null) {
                mEvent.retrying(file, taskID, index);
            }
        }

        @Override
        public void failed(@NonNull File file, @NonNull String taskID, int index, @NonNull String errorMessage) {
            if (mEvent != null) {
                mEvent.failed(file, taskID, index, errorMessage);
            }
            completedTaskSize++;
            startForegroundNotification(taskSize, completedTaskSize, doneTaskSize);
        }

        @Override
        public void done(@NonNull File file, @NonNull String taskID, int index, @NonNull String url) {
            if (mEvent != null) {
                mEvent.done(file, taskID, index, url);
            }
            completedTaskSize++;
            doneTaskSize++;
            startForegroundNotification(taskSize, completedTaskSize, doneTaskSize);
        }
    };

    public void onCreate() {
        super.onCreate();
        getJobManager();
    }

    private synchronized JobManager getJobManager() {
        if (mJobManager == null) {
            configureJobManager();
        }
        return mJobManager;
    }

    private void configureJobManager() {
        Configuration.Builder builder = new Configuration.Builder(this)
                .minConsumerCount(1)
                .maxConsumerCount(1)
                .loadFactor(1)
                .consumerKeepAlive((int) TimeUnit.MINUTES.toSeconds(4));

        mJobManager = new JobManager(builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopNotification();
    }


    class UploadBinder extends Binder {
        /**
         * 上传文件
         *
         * @param files  需要上传的文件
         * @param taskID 任务 ID
         */
        public void upload(List<File> files, @NonNull String taskID) {
            uploadFiles(files, taskID);
        }

        /**
         * 压缩并上传图片
         *
         * @param images 需要压缩上传的图片
         * @param taskID 任务 ID
         *               <p>
         *               压缩后的预计文件大小 默认 300KiB
         *               压缩后最大边的尺寸 默认 1920PX
         */
        public void compressAndUploadImage(List<File> images, @NonNull final String taskID) {
            compressAndUploadImage(images, taskID, 300);
        }

        /**
         * 压缩并上传图片
         *
         * @param images    需要压缩上传的图片
         * @param taskID    任务 ID
         * @param sizeInKiB 压缩后的预计文件大小 默认 300KiB
         *                  <p>
         *                  压缩后最大边的尺寸 默认 1920PX
         */
        public void compressAndUploadImage(List<File> images, @NonNull final String taskID, int sizeInKiB) {
            compressAndUploadImage(images, taskID, sizeInKiB, 1920);
        }

        /**
         * 压缩并上传图片
         *
         * @param images          需要压缩上传的图片
         * @param taskID          任务 ID
         * @param sizeInKiB       压缩后的预计文件大小 默认 300KiB
         * @param maxSideSizeInPX 压缩后最大边的尺寸 默认 1920PX
         */
        public void compressAndUploadImage(List<File> images, @NonNull final String taskID, int sizeInKiB, int maxSideSizeInPX) {
            compressImages(images, sizeInKiB, maxSideSizeInPX)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<File>>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            showCompressErrorNotification(e.getLocalizedMessage());
                        }

                        @Override
                        public void onNext(List<File> files) {
                            uploadFiles(files, taskID);
                        }
                    });
        }

        public void setNotifiInfo(@DrawableRes int notifiLogo, @ColorRes int appColor) {
            logo = notifiLogo;
            color = appColor;
        }

        public void setPendingIntent(@Nullable PendingIntent pendingIntent) {
            mPendingIntent = pendingIntent;
        }

        /**
         * 获取目前的上传状态
         *
         * @param taskID 主任务 ID,为空获取所有记录
         */
        public List<Task> getTask(@Nullable String taskID) {
            return new UploadSQLiteHelper(Init.getApplication()).getTaskList(taskID);
        }
    }


    private static final int NOTIFICATION_ID = 1123;
    private static final int ERROR_COMPRESS_NOTIFICATION_ID = 1124;
    private static final int COMPLETE_NOTIFICATION_ID = 1124;

    /**
     * 开启前台通知
     */
    private void startForegroundNotification(int taskSize, int completedTaskSize, int doneTaskSize) {
        if (taskSize == completedTaskSize) {
            stopNotification();
            showCompleteNotification(taskSize, doneTaskSize);
        }
        NotificationCompat.Builder notifiBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("正在上传图片")
                .setContentText("已完成" + completedTaskSize + " ,共" + taskSize)
                .setAutoCancel(false);
        if (color != 0) {
            notifiBuilder.setColor(ContextCompat.getColor(this, color));
        }
        if (logo != 0) {
            notifiBuilder.setSmallIcon(logo);
        }
        if (mPendingIntent != null) {
            notifiBuilder.setContentIntent(mPendingIntent);
        }
        startForeground(NOTIFICATION_ID, notifiBuilder.build());
    }

    private void showCompleteNotification(int taskSize, int doneTaskSize) {
        stopNotification();
        NotificationCompat.Builder notifiBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("上传已完成")
                .setContentText(String.format(Locale.getDefault(), "合计上传%d,其中失败%d",
                        taskSize, taskSize - doneTaskSize))
                .setAutoCancel(true);
        if (color != 0) {
            notifiBuilder.setColor(ContextCompat.getColor(this, color));
        }
        if (logo != 0) {
            notifiBuilder.setSmallIcon(logo);
        }
        if (mPendingIntent != null) {
            notifiBuilder.setContentIntent(mPendingIntent);
        }
        NotificationManagerCompat.from(this).notify(COMPLETE_NOTIFICATION_ID, notifiBuilder.build());
    }

    /**
     * 停止通知
     */
    private void stopNotification() {
        NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID);
    }

    /**
     * 显示压缩错误通知
     */
    private void showCompressErrorNotification(String errorMessage) {
        NotificationCompat.Builder notifiBuilder = new NotificationCompat.Builder(this)
                .setContentTitle("图片压缩出错")
                .setContentText(errorMessage)
                .setAutoCancel(true);
        if (color != 0) {
            notifiBuilder.setColor(ContextCompat.getColor(this, color));
        }
        if (logo != 0) {
            notifiBuilder.setSmallIcon(logo);
        }
        if (mPendingIntent != null) {
            notifiBuilder.setContentIntent(mPendingIntent);
        }
        NotificationManagerCompat.from(this).notify(ERROR_COMPRESS_NOTIFICATION_ID, notifiBuilder.build());
    }

    /**
     * 压缩
     *
     * @param images    需要被压缩的图片
     * @param sizeInKiB 压缩后预计文件尺寸
     * @param maxSidePX 压缩后最大边尺寸
     * @return 压缩后的图片(Observable)
     */
    private Observable<List<File>> compressImages(List<File> images, int sizeInKiB, int maxSidePX) {
        return Luban.get(this)
                .setMaxSize(sizeInKiB)
                .setMaxWidth(maxSidePX).setMaxWidth(maxSidePX)
                .putGear(Luban.CUSTOM_GEAR)
                .load(images)
                .asListObservable();
    }

    /*总共的任务数量*/
    private int taskSize = 0;
    /*结束的任务数量 包括失败的*/
    private int completedTaskSize = 0;
    /*成功的任务数量*/
    private int doneTaskSize = 0;

    private void uploadFiles(@NonNull List<File> files, @NonNull String taskID) {
        taskSize += files.size();
        startForegroundNotification(taskSize, completedTaskSize, doneTaskSize);
        for (int i = 0; i < files.size(); i++) {
            uploadFile(files.get(i), taskID, i);
        }
    }

    private void uploadFile(@NonNull File file, @NonNull String taskID, int index) {
        getJobManager().addJobInBackground(new UploadJob(mUploadEvent, file, taskID, index));
    }

    public static final int WAITING = 1;
    public static final int UPLOADING = 2;
    public static final int COMPRESSING = 3;
    public static final int RETRYING = 4;
    public static final int FAILED = 5;
    public static final int DONE = 6;

    @IntDef({WAITING, UPLOADING, COMPRESSING, RETRYING, FAILED, DONE})
    public @interface STATUE {
    }

    public interface UploadServiceEvent {
        void start(@NonNull File file, @NonNull String taskID, int index);

        void retrying(@NonNull File file, @NonNull String taskID, int index);

        void failed(@NonNull File file, @NonNull String taskID, int index, @NonNull String errorMessage);

        void done(@NonNull File file, @NonNull String taskID, int index, @NonNull String url);
    }

}
