package moe.xing.fileuploader_app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import moe.xing.baseutils.Init;
import moe.xing.baseutils.utils.LogHelper;
import moe.xing.fileuploader.Task;
import moe.xing.fileuploader.UploadService;
import moe.xing.fileuploader_app.databinding.ActivityMainBinding;
import moe.xing.getimage.RxGetImage;
import rx.Subscriber;

public class MainActivity extends AppCompatActivity implements UploadService.UploadServiceEvent {

    List<File> files = new ArrayList<>();
    private ActivityMainBinding mBinding;
    private ImageAdapter mAdapter;
    private UploadService.UploadBinder mBinder;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinder.getService().removeEvent(MainActivity.this);
            mBinder = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (UploadService.UploadBinder) service;
            mBinder.setNotifiInfo(R.mipmap.ic_launcher, R.color.colorAccent);
            mAdapter.getDatas().clear();
            mAdapter.addData(mBinder.getTask("testID1"));

            mBinder.getService().addEvent(MainActivity.this);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.activity_main, null, false);
        setContentView(mBinding.getRoot());

        Init.getInstance(getApplication(), true, "1.0", " upload file ");

        Intent bindIntent = new Intent(this, UploadService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);

        mAdapter = new ImageAdapter();
        mBinding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mBinding.recyclerView.setAdapter(mAdapter);
        mBinding.recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        mBinding.multiple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                files.clear();
                RxGetImage.getInstance().getMultipleImage(Integer.MAX_VALUE).subscribe(new Subscriber<File>() {
                    @Override
                    public void onCompleted() {
                        if (mBinder != null) {
                            mBinder.compressAndUploadImage(files, "testID1", 150, 1920);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(v.getContext(), e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onNext(File file) {
                        files.add(file);
                    }
                });

            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinder.getService().addEvent(null);
        unbindService(connection);
    }

    private void addTask(final Task task) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mAdapter.getDatas().size(); i++) {
                    Task inList = mAdapter.getDatas().get(i);
                    if (task.getTaskIDAndIndex().equals(inList.getTaskIDAndIndex())) {
                        mAdapter.removeDate(inList);
                        break;
                    }
                }
                mAdapter.addData(task);
            }
        });
    }

    @Override
    public void taskChanged(@NonNull Task task) {
        addTask(task);
    }

    /**
     * 文件上传后的操作,例如添加至所需的位置
     * <p>
     * 如果出错请抛出异常以便重试
     * 在工作线程调用 请再处理结束后返回
     *
     * @param task
     */
    @Override
    public void afterUpload(@NonNull Task task) {
        LogHelper.i("TaskID:" + task.getTaskID() + " url:" + task.getUrl());
    }
}
