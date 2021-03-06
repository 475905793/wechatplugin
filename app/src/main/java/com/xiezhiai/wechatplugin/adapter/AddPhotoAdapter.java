package com.xiezhiai.wechatplugin.adapter;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.xiezhiai.wechatplugin.R;
import com.xiezhiai.wechatplugin.core.Config;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by shijiwei on 2018/10/29.
 *
 * @Desc:
 */
public class AddPhotoAdapter extends RecyclerView.Adapter<AddPhotoAdapter.ViewHolder> {

    public static final String ADD_PATH = "add_photo_path";
    private List<String> photoPaths = new ArrayList<>();

    public AddPhotoAdapter() {
        this.photoPaths.add(Environment.getExternalStorageDirectory().getAbsolutePath() + Config.EXTERNAL_DIR + "/" + "user_3b4d15bab5f4dafe5da7dbd7025f52f8.png");
        this.photoPaths.add(Environment.getExternalStorageDirectory().getAbsolutePath() + Config.EXTERNAL_DIR + "/" + "user_3b4d15bab5f4dafe5da7dbd7025f52f8.png");
        this.photoPaths.add(Environment.getExternalStorageDirectory().getAbsolutePath() + Config.EXTERNAL_DIR + "/" + "user_3b4d15bab5f4dafe5da7dbd7025f52f8.png");
        this.photoPaths.add(ADD_PATH);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_add_photo, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int i) {

        if (photoPaths.get(i).equals(ADD_PATH)) {
            holder.btnDelete.setVisibility(View.GONE);
            Glide.with(holder.ivPhoto.getContext()).load(R.mipmap.icon_add_photo).into(holder.ivPhoto);
        } else {
            holder.btnDelete.setVisibility(View.VISIBLE);
            Glide.with(holder.ivPhoto.getContext()).load(photoPaths.get(i)).into(holder.ivPhoto);
        }

        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoPaths.remove(i);
                notifyDataSetChanged();
            }
        });

        holder.ivPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (i == photoPaths.size() - 1) {
                    /* add photo */
                    if (addPhotoListener != null) addPhotoListener.onSelectPhotoFromSystemAlbum();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoPaths == null ? 0 : photoPaths.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivPhoto;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }

    public interface AddPhotoListener {

        void onSelectPhotoFromSystemAlbum();

    }

    private AddPhotoListener addPhotoListener;

    public void setAddPhotoListener(AddPhotoListener addPhotoListener) {
        this.addPhotoListener = addPhotoListener;
    }
}
