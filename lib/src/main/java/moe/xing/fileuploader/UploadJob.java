package moe.xing.fileuploader;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import moe.xing.baseutils.Init;
import moe.xing.rx_utils.RxBus;

/**
 * Created by Qi Xingchen on 16-11-28.
 * <p>
 * 上传任务
 */

class UploadJob extends Job {
    private static final int PRIORITY = 1;

    private File mFile;
    private String mTaskID;
    private int mIndex;

    protected UploadJob(@NonNull File file, @NonNull String taskID, int index) {
        super(new Params(PRIORITY).requireNetwork().persist());
        this.mFile = file;
        this.mIndex = index;
        this.mTaskID = taskID;
    }

    /**
     * Called when the job is added to disk and committed.
     * This means job will eventually run. This is a good time to update local database and dispatch events.
     * <p>
     * Changes to this class will not be preserved if your job is persistent !!!
     * <p>
     * Also, if your app crashes right after adding the job, {@code onRun} might be called without an {@code onAdded} call
     * <p>
     * Note that this method is called on JobManager's thread and will block any other action so
     * it should be fast and not make any web requests (File IO is OK).
     */
    @Override
    public void onAdded() {
        new UploadSQLiteHelper(Init.getApplication()).updateStatue(mTaskID, mIndex, UploadService.WAITING);
        Task task = new Task(mTaskID, mIndex, mFile);
        task.setStatue(UploadService.WAITING);
        sendTask(task);
    }

    /**
     * The actual method that should to the work.
     * It should finish w/o any exception. If it throws any exception,
     * {@link #shouldReRunOnThrowable(Throwable, int, int)} will be called to
     * decide either to dismiss the job or re-run it.
     *
     * @throws Throwable Can throw and exception which will mark job run as failed
     */
    @Override
    public void onRun() throws Throwable {
        //更改为正在上传
        new UploadSQLiteHelper(Init.getApplication()).updateStatue(mTaskID, mIndex, UploadService.UPLOADING);
        Task task = new Task(mTaskID, mIndex, mFile);
        task.setStatue(UploadService.UPLOADING);
        sendTask(task);

        //开始上传
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        UpimgBean.DataEntity imgBean = RetrofitNetwork.UploadImage(mFile);

        //上传成功

        task.setUrl(imgBean.getUrl());
        task.setHeight(imgBean.getHeight());
        task.setWidth(imgBean.getWidth());
        List<SoftReference<UploadService.UploadServiceEvent>> mEvents = UploadService.getEvents();
        for (SoftReference<UploadService.UploadServiceEvent> softReference : mEvents) {
            if (softReference.get() != null) {
                softReference.get().afterUpload(task);
            }
        }


        //后处理完成
        new UploadSQLiteHelper(Init.getApplication()).doneUpload(mTaskID, mIndex, imgBean.getUrl());
        task.setStatue(UploadService.DONE);
        sendTask(task);
    }

    /**
     * Called when a job is cancelled.
     *
     * @param cancelReason It is one of:
     *                     <ul>
     *                     REACHED_RETRY_LIMIT
     *                     CANCELLED_VIA_SHOULD_RE_RUN
     *                     CANCELLED_WHILE_RUNNING
     *                     SINGLE_INSTANCE_WHILE_RUNNING
     *                     SINGLE_INSTANCE_ID_QUEUED
     *                     </ul>
     * @param throwable    The exception that was thrown from the last execution of {@link #onRun()}
     */
    @Override
    protected void onCancel(int cancelReason, @Nullable Throwable throwable) {
        new UploadSQLiteHelper(Init.getApplication()).updateStatue(mTaskID, mIndex, UploadService.FAILED);
        Task task = new Task(mTaskID, mIndex, mFile);
        task.setStatue(UploadService.FAILED);
        task.setErrorMessage(throwable != null ? throwable.getLocalizedMessage() : "");
        sendTask(task);
    }

    /**
     * If {@code onRun} method throws an exception, this method is called.
     * <p>
     * If you simply want to return retry or cancel, you can use {@link RetryConstraint#RETRY} or
     * {@link RetryConstraint#CANCEL}.
     * <p>
     * You can also use a custom {@link RetryConstraint} where you can change the Job's priority or
     * add a delay until the next run (e.g. exponential back off).
     * <p>
     * Note that changing the Job's priority or adding a delay may alter the original run order of
     * the job. So if the job was added to the queue with other jobs and their execution order is
     * important (e.g. they use the same groupId), you should not change job's priority or add a
     * delay unless you really want to change their execution order.
     *
     * @param throwable   The exception that was thrown from {@link #onRun()}
     * @param runCount    The number of times this job run. Starts from 1.
     * @param maxRunCount The max number of times this job can run. Decided by {@link #getRetryLimit()}
     * @return A {@link RetryConstraint} to decide whether this Job should be tried again or not and
     * if yes, whether we should add a delay or alter its priority. Returning null from this method
     * is equal to returning {@link RetryConstraint#RETRY}.
     */
    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        if (runCount < maxRunCount) {
            new UploadSQLiteHelper(Init.getApplication()).updateStatue(mTaskID, mIndex, UploadService.RETRYING);
            Task task = new Task(mTaskID, mIndex, mFile);
            task.setStatue(UploadService.RETRYING);
            sendTask(task);
            return RetryConstraint.createExponentialBackoff(runCount, TimeUnit.SECONDS.toMillis(5));
        }
        return RetryConstraint.CANCEL;
    }

    private void sendTask(@NonNull Task task) {
        RxBus.getInstance().send(task);
    }
}
