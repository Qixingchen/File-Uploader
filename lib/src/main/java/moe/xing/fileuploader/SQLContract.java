package moe.xing.fileuploader;

import android.provider.BaseColumns;

/**
 * Created by Qi Xingchen on 16-11-29.
 * <p>
 * 数据库字段
 */

class SQLContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private SQLContract() {
    }

    /* Inner class that defines the table contents */
     static class UPLOAD implements BaseColumns {
        static final String TABLE_NAME = "UPLOAD_TASK";
         static final String TASK_ID = "task_id";
         static final String INDEX = "index";
         static final String FILE_PATH = "file_path";
         static final String STATUE = "statue";
         static final String URL = "url";
    }
}
