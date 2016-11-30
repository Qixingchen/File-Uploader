package moe.xing.fileuploader_app;

import android.databinding.DataBindingUtil;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;

import java.io.File;

import moe.xing.fileuploader.Task;
import moe.xing.fileuploader_app.databinding.ItemFileBinding;
import moe.xing.rvutils.BaseSortedRVAdapter;

/**
 * Created by Qi Xingchen on 16-11-30.
 */

class ImageAdapter extends BaseSortedRVAdapter<Task, ImageAdapter.ViewHolder> {

    ImageAdapter() {
        setDatas(new SortedList<>(Task.class, new SortedListAdapterCallback<Task>(this) {
            @Override
            public int compare(Task o1, Task o2) {
                int compare = o1.getStatue() - o2.getStatue();
                if (compare == 0) {
                    return o1.getIndex() - o2.getIndex();
                }
                return compare;
            }

            @Override
            public boolean areContentsTheSame(Task oldItem, Task newItem) {
                return oldItem.toString().equals(newItem.toString());
            }

            @Override
            public boolean areItemsTheSame(Task item1, Task item2) {
                return item1.getTaskID().equals(item2.getTaskID()) && item1.getIndex() == item2.getIndex();
            }
        }));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ItemFileBinding binding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_file, parent, false);
        return new ViewHolder(binding.getRoot());
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mBinding.setTask(datas.get(position));
        holder.mBinding.executePendingBindings();
        File file = datas.get(position).getFile();
        if (file.exists()) {
            holder.mBinding.fileSize.setText("文件尺寸" + file.length());
            Glide.with(holder.itemView.getContext()).load(file).into(holder.mBinding.image);
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        ItemFileBinding mBinding;

        ViewHolder(View itemView) {
            super(itemView);
            mBinding = DataBindingUtil.findBinding(itemView);
        }
    }
}
