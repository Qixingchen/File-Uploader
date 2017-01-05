package moe.xing.fileuploader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import moe.xing.baseutils.utils.LogHelper;

import static moe.xing.fileuploader.UploadService.DONE;

/**
 * Created by Qi Xingchen on 16-11-29.
 * <p>
 * 数据库
 */

class UploadSQLiteHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "upload.db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + SQLContract.UPLOAD.TABLE_NAME + " (" +
                    SQLContract.UPLOAD._ID + " INTEGER PRIMARY KEY," +
                    SQLContract.UPLOAD.TASK_ID + TEXT_TYPE + COMMA_SEP +
                    SQLContract.UPLOAD.INDEX + INT_TYPE + COMMA_SEP +
                    SQLContract.UPLOAD.FILE_PATH + TEXT_TYPE + COMMA_SEP +
                    SQLContract.UPLOAD.URL + TEXT_TYPE + COMMA_SEP +
                    SQLContract.UPLOAD.STATUE + INT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + SQLContract.UPLOAD.TABLE_NAME;

    private SQLiteDatabase db;

    public UploadSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
    }

    /**
     * 添加新上传任务
     *
     * @param taskID   主任务ID
     * @param index    本任务在主任务下的序号
     * @param filePath 文件地址
     */
    void addTask(@NonNull String taskID, int index, @NonNull String filePath) {
        db.beginTransaction();
        ContentValues values = new ContentValues();
        values.put(SQLContract.UPLOAD.TASK_ID, taskID);
        values.put(SQLContract.UPLOAD.INDEX, index);
        values.put(SQLContract.UPLOAD.FILE_PATH, filePath);
        values.put(SQLContract.UPLOAD.STATUE, UploadService.WAITING);
        long id = db.insert(SQLContract.UPLOAD.TABLE_NAME, null, values);
        if (id < 0) {
            LogHelper.e(String.format(Locale.getDefault(), "数据库写入任务失败 主任务 id: %s,序号 %d,文件路径: %s",
                    taskID, index, filePath));
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * 更新任务状态
     *
     * @param taskID 主任务ID
     * @param index  本任务在主任务下的序号
     * @param statue 状态
     */
    void updateStatue(@NonNull String taskID, int index, @UploadService.STATUE int statue) {
        db.beginTransaction();

        ContentValues values = new ContentValues();
        values.put(SQLContract.UPLOAD.STATUE, statue);

        String selection = SQLContract.UPLOAD.TASK_ID + " = ? AND " + SQLContract.UPLOAD.INDEX + " = ?";
        String[] selectionArgs = {taskID, String.valueOf(index)};

        int count = db.update(
                SQLContract.UPLOAD.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        if (count > 1) {
            LogHelper.e("更新了多个状态:" + count);
        }
        if (count <= 0) {
            LogHelper.e("没有得到任何更新!");
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * 上传完成
     *
     * @param taskID 主任务ID
     * @param index  本任务在主任务下的序号
     * @param url    图片地址
     */
    void doneUpload(@NonNull String taskID, int index, @NonNull String url) {
        db.beginTransaction();

        ContentValues values = new ContentValues();
        values.put(SQLContract.UPLOAD.STATUE, DONE);
        values.put(SQLContract.UPLOAD.URL, url);

        String selection = SQLContract.UPLOAD.TASK_ID + " = ? AND " + SQLContract.UPLOAD.INDEX + " = ?";
        String[] selectionArgs = {taskID, String.valueOf(index)};

        int count = db.update(
                SQLContract.UPLOAD.TABLE_NAME,
                values,
                selection,
                selectionArgs);
        if (count > 1) {
            LogHelper.e("更新了多个状态:" + count);
        }
        if (count <= 0) {
            LogHelper.e("更新状态数量为" + count);
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @NonNull
    List<Task> getTaskList(@Nullable String taskID) {

        String selection = SQLContract.UPLOAD.TASK_ID + " = ?";
        String[] selectionArgs = {taskID};

        String sortOrder =
                SQLContract.UPLOAD.TASK_ID + " , " + SQLContract.UPLOAD.INDEX + " DESC";

        Cursor c;
        if (TextUtils.isEmpty(taskID)) {
            c = db.query(
                    SQLContract.UPLOAD.TABLE_NAME,                     // The table to query
                    null,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );

        } else {
            c = db.query(
                    SQLContract.UPLOAD.TABLE_NAME,                     // The table to query
                    null,                               // The columns to return
                    selection,                                // The columns for the WHERE clause
                    selectionArgs,                            // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    sortOrder                                 // The sort order
            );
        }

        List<Task> tasks = new ArrayList<>();
        c.moveToFirst();
        while (!c.isAfterLast()) {
            Task task = new Task();
            task.setTaskID(c.getString(c.getColumnIndex(SQLContract.UPLOAD.TASK_ID)));
            task.setIndex(c.getInt(c.getColumnIndex(SQLContract.UPLOAD.INDEX)));
            task.setFile(new File(c.getString(c.getColumnIndex(SQLContract.UPLOAD.FILE_PATH))));
            task.setStatue(c.getInt(c.getColumnIndex(SQLContract.UPLOAD.STATUE)));
            task.setUrl(c.getString(c.getColumnIndex(SQLContract.UPLOAD.URL)));
            tasks.add(task);
            c.moveToNext();
        }
        c.close();
        return tasks;
    }

    /**
     * 获取未完成任务的数量
     */
    int getUncompleteSize() {

        List<Task> tasks = getTaskList(null);
        int ans = 0;
        for (Task task : tasks) {
            if (task.getStatue() != DONE) {
                ans++;
            }
        }
        return ans;
    }
}
