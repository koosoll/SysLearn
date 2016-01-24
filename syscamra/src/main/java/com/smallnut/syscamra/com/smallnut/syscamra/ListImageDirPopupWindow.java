package com.smallnut.syscamra.com.smallnut.syscamra;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.smallnut.syscamra.R;
import com.smallnut.syscamra.com.smallnut.syscamra.bean.DirBean;
import com.smallnut.syscamra.com.smallnut.syscamra.utils.ImageLoader;

import java.util.List;
import java.util.zip.Inflater;

/**
 * Created by koosol on 2016/1/19.
 */
public class ListImageDirPopupWindow extends PopupWindow {
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<DirBean> mDatas;

    public interface OnDirSelectedListner{
        void onSelected(DirBean dirBean);
    }

    public OnDirSelectedListner mLinstner;

    public void setOnDirSelectedLinstner(OnDirSelectedListner mLinstner){
        this.mLinstner = mLinstner;
    }

    public ListImageDirPopupWindow(Context context, List<DirBean> mDatas) {
        this.mDatas = mDatas;
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popwindow_main,null);
        mDatas = mDatas;
        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);
        setFocusable(true);
        setTouchable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());

        setTouchInterceptor(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_OUTSIDE){
                    dismiss();
                    return true;
                }
                return false;
            }
        });
        initViews(context);
        initEvents();
    }

    /**初始化按钮*/
    private void initEvents() {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mLinstner != null){
                    mLinstner.onSelected(mDatas.get(position));
                }
            }
        });
    }

    /**初始化控件*/
    private void initViews(Context context){
        mListView = (ListView) mConvertView.findViewById(R.id.lv_galarys);
        mListView.setAdapter(new DirListAdapter(context,mDatas));
    }


    /**计算popuwindow的宽高*/
    private void calWidthAndHeight(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);

        mWidth = dm.widthPixels;
        mHeight = (int) (dm.heightPixels*0.8);
    }

    private class DirListAdapter extends ArrayAdapter<DirBean>{

        private List<DirBean> mDatas;
        private LayoutInflater mInflater;

        public DirListAdapter(Context context,List<DirBean> objects) {
            super(context, 0, objects);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MyHolder mHolder = null;
            if (convertView == null){
                convertView = mInflater.inflate(R.layout.item_popwindow_main,parent,false);
                mHolder = new MyHolder();
                mHolder.mPicShow = (ImageView) convertView.findViewById(R.id.iv_picshow);
                mHolder.mDirName = (TextView) convertView.findViewById(R.id.tv_galaryname);
                mHolder.mCount = (TextView) convertView.findViewById(R.id.tv_picnum);
                mHolder.mSelectedIcon = (ImageView) convertView.findViewById(R.id.iv_galaryselected);
                convertView.setTag(mHolder);
            }else{
                mHolder = (MyHolder) convertView.getTag();
            }

            DirBean bean = getItem(position);

            mHolder.mPicShow.setImageResource(R.drawable.bulletscreen_icon_prevue);
            ImageLoader.getInstance().loadImage(bean.getFirstImgPath(),mHolder.mPicShow);
            mHolder.mDirName.setText(bean.getName());
            mHolder.mCount.setText(bean.getCount()+"");
            return convertView;
        }

        private class MyHolder {
            ImageView mPicShow;
            TextView mDirName;
            TextView mCount;
            ImageView mSelectedIcon;
        }
    }
}
