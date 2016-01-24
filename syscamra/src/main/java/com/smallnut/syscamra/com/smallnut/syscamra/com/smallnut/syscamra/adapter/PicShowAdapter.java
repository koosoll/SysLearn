package com.smallnut.syscamra.com.smallnut.syscamra.com.smallnut.syscamra.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.smallnut.syscamra.R;
import com.smallnut.syscamra.com.smallnut.syscamra.utils.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 展示相册中图片信息的 adapter
 * Created by koosol on 2016/1/19.
 */
public class PicShowAdapter extends BaseAdapter {

    private Context context;
    private List<String> mDatas;
    private String dirPath;
    private static Set<String> mSelectedImg = new HashSet<String>();

    public PicShowAdapter(Context context, List<String> mDatas, String dirPath) {
        this.context = context;
        this.mDatas = mDatas;
        this.dirPath = dirPath;
    }

    @Override
    public int getCount() {
        return mDatas.size();
    }

    @Override
    public Object getItem(int position) {
        return mDatas.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final MyHolder myHolder;
        if(convertView == null){
            convertView = View.inflate(context, R.layout.item_grideview,null);
            myHolder = new MyHolder();
            myHolder.mPic = (ImageView) convertView.findViewById(R.id.iv_pic);
            myHolder.mSelectedView = (ImageButton) convertView.findViewById(R.id.ib_selected);
            convertView.setTag(myHolder);

        }else{
            myHolder = (MyHolder) convertView.getTag();
        }

        myHolder.mPic.setImageResource(R.drawable.bulletscreen_icon_prevue);
        myHolder.mSelectedView.setImageResource(R.drawable.check_disable);
        myHolder.mPic.setColorFilter(null);

        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(dirPath+"/"+mDatas.get(position),myHolder.mPic);
        final String filePath = dirPath+"/"+mDatas.get(position);
        myHolder.mPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSelectedImg.contains(filePath)){
                    //已经被选择
                    mSelectedImg.remove(filePath);
                    myHolder.mPic.setColorFilter(null);
                    myHolder.mSelectedView.setImageResource(R.drawable.check_disable);
                }else {
                    //没有选择
                    mSelectedImg.add(filePath);
                    myHolder.mPic.setColorFilter(Color.parseColor("#77000000"));
                    myHolder.mSelectedView.setImageResource(R.drawable.check_selected);
                }
            }
        });

        if(mSelectedImg.contains(filePath)){
            myHolder.mPic.setColorFilter(Color.parseColor("#77000000"));
            myHolder.mSelectedView.setImageResource(R.drawable.check_selected);
        }

        return convertView;
    }

    private class MyHolder{
        ImageView mPic;
        ImageButton mSelectedView;
    }
}
