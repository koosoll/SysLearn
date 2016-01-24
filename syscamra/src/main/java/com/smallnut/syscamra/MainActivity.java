package com.smallnut.syscamra;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.smallnut.syscamra.com.smallnut.syscamra.ListImageDirPopupWindow;
import com.smallnut.syscamra.com.smallnut.syscamra.bean.DirBean;
import com.smallnut.syscamra.com.smallnut.syscamra.com.smallnut.syscamra.adapter.PicShowAdapter;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {

    private GridView mGridView;
    private List<String> mImags;
    private TextView mGalaryName, mPicCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<DirBean> mDirBeans = new ArrayList<DirBean>();

    private ProgressDialog mProgressDialog;

    private static final int SCAN_PIC_FINISHED = 0x110;

    private PicShowAdapter adapter;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SCAN_PIC_FINISHED) {
                mProgressDialog.dismiss();
                data2View();

                initPopuWindow();
            }
        }
    };

    /**初始化popwindow*/
    private void initPopuWindow() {
        listImageDirPopupWindow = new ListImageDirPopupWindow(MainActivity.this,mDirBeans);
        listImageDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener(){

            @Override
            public void onDismiss() {
                lightOn();
            }
        });

        listImageDirPopupWindow.setOnDirSelectedLinstner(new ListImageDirPopupWindow.OnDirSelectedListner() {
            @Override
            public void onSelected(DirBean dirBean) {
                mCurrentDir = new File(dirBean.getDir());
                mImags = Arrays.asList(mCurrentDir.list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
                            return true;
                        return false;
                    }
                }));

                adapter = new PicShowAdapter(MainActivity.this,mImags,mCurrentDir.getAbsolutePath());
                mGridView.setAdapter(adapter);
                mGalaryName.setText(mImags.size()+"");
                mPicCount.setText(dirBean.getName());
                listImageDirPopupWindow.dismiss();
            }
        });
    }

    /**变亮*/
    private void lightOn() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 1.0f;
        getWindow().setAttributes(lp);
    }

    /**变暗*/
    private void lightOff() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.alpha = 0.3f;
        getWindow().setAttributes(lp);
    }

    private ListImageDirPopupWindow listImageDirPopupWindow;

    private RelativeLayout mBottomView;


    /**
     * 绑定数据到view
     */
    private void data2View() {
        if (mCurrentDir == null) {
            Toast.makeText(MainActivity.this, "没有扫描到图片...", Toast.LENGTH_SHORT).show();
            return;
        }

        mImags = Arrays.asList(mCurrentDir.list());

        adapter = new PicShowAdapter(MainActivity.this, mImags, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(adapter);

        mPicCount.setText(mMaxCount+"");
        mGalaryName.setText(mCurrentDir.getName());

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initDatas();
        setLinstners();
    }
    private String[] args =  new String[]{"image/jpeg","image/png"};
    /**
     * 初始化数据
     * 利用ContentProvider扫描手机上的所有图片
     */
    private void initDatas() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(MainActivity.this, "当前存储卡不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(MainActivity.this, null, "扫描中请稍后...");
        //开启线程开始扫描
        new Thread() {
            @Override
            public void run() {

                Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(
                                            uri,
                                            null,
                                            MediaStore.Images.Media.MIME_TYPE + " =? or " + MediaStore.Images.Media.MIME_TYPE + "=?",
                                            args,
                                            MediaStore.Images.Media.DATE_MODIFIED);
                Set<String> mDirPaths = new HashSet<String>();
                while (cursor.moveToNext()) {
                    //得到路径
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    if (parentFile == null) continue;
                    String dirPath = parentFile.getAbsolutePath();
                    DirBean dirBean = null;

                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        dirBean = new DirBean();
                        dirBean.setDir(dirPath);
                        dirBean.setFirstImgPath(path);

                        if (parentFile.list() == null) {
                            continue;
                        }

                        int picSize = parentFile.list(new FilenameFilter() {
                            @Override
                            public boolean accept(File dir, String filename) {
                                if (filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".png"))
                                    return true;
                                return false;
                            }
                        }).length;

                        dirBean.setCount(picSize);

                        mDirBeans.add(dirBean);

                        if (picSize > mMaxCount) {
                            mMaxCount = picSize;
                            mCurrentDir = parentFile;
                        }
                    }
                }

                cursor.close();
                mHandler.sendEmptyMessage(SCAN_PIC_FINISHED);
            }
        }.start();


    }

    /**
     * 设置监听
     */
    private void setLinstners() {
        mBottomView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listImageDirPopupWindow.showAsDropDown(mBottomView,0,0);
                lightOff();
            }
        });
    }


    /**
     * 初始化视图
     */
    private void initViews() {
        mGridView = (GridView) findViewById(R.id.gv_pic_grid);
        mGalaryName = (TextView) findViewById(R.id.tv_gallaryname);
        mPicCount = (TextView) findViewById(R.id.tv_piccount);
        mBottomView = (RelativeLayout) findViewById(R.id.rl_bottomitem);
    }
}
