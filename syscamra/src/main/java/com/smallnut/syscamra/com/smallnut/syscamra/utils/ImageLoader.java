package com.smallnut.syscamra.com.smallnut.syscamra.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 图片加载器 采用单例模式
 * Created by koosol on 2016/1/19.
 */
public class ImageLoader {
    /**
     * 私有化构造函数
     */
    private ImageLoader(int threadCount,Type type) {
        init(threadCount,type);
    };
    /**
     * 图片缓存
     */
    private LruCache<String, Bitmap> mLruCache;
    /**
     * 建立加载图片的线程池
     */
    private ExecutorService mThreadPool;
    private Semaphore mSemaphoreThreadPool;


    /**默认线程数*/
    private static final int DEAFULT_THREAD_COUNT = 1;
    /**加载方式*/
    public enum Type{
        FIFO,LIFO;
    }

    /**默认加载方式*/
    private Type mType = Type.LIFO;
    /**任务队列*/
    private LinkedList<Runnable> mTaskQueue;
    /**轮询线程*/
    private Thread mPoolThread;
    /**线程池handler*/
    private Handler mPoolHandler;
    /**同步信号量*/
    private Semaphore mSemaphorePoolHandler = new Semaphore(0);

    /**更新UI线程的handler*/
    private Handler mUiHandler;

    private static ImageLoader mInstance;

    /**初始化imageLoader*/
    private void init(int threadCount, Type type) {
        //建立轮询线程
        mPoolThread = new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                mPoolHandler = new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池中取出一个任务去执行
                        mThreadPool.execute(getTask());
                        try {
                            mSemaphoreThreadPool.acquire();
                        } catch (InterruptedException e) {
                        }
                    }
                };
                //初始化完毕，释放信号量
                mSemaphorePoolHandler.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        /**获取应用的最大可用内存*/
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        /**缓存占用的内存*/
        int cacheMemory = maxMemory/8;
        mLruCache = new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //测量每一个bitmap的大小
                return value.getRowBytes()*value.getHeight();
            }
        };
        /**创建线程池*/
        mThreadPool = Executors.newFixedThreadPool(threadCount);
        /**创建任务队列*/
        mTaskQueue = new LinkedList<Runnable>();
        mType = type;

        mSemaphoreThreadPool = new Semaphore(threadCount);
    }

    /**从任务队列中拿出任务*/
    private Runnable getTask() {
        if(mType == Type.FIFO){
            return mTaskQueue.removeFirst();
        }else if(mType == Type.LIFO){
            return mTaskQueue.removeLast();
        }
        return null;
    }

    public static ImageLoader getInstance(){
        return mInstance;
    }
    /**获取图片加载器的实例*/
    public static ImageLoader getInstance(int threadCount,Type type) {
        if (mInstance == null) {
            synchronized (ImageLoader.class) {
                mInstance = new ImageLoader(threadCount,type);
            }
        }

        return mInstance;
    }

    /**加载图片，此方法是运行在UI线程中的*/
    public void loadImage(final String path, final ImageView imageView){
        imageView.setTag(path);
        if(mUiHandler == null){
            mUiHandler = new Handler(){

                @Override
                public void handleMessage(Message msg) {
                    //获取图片，为imageview回调设置图片
                    ImageBeanHolder holder = (ImageBeanHolder) msg.obj;
                    Bitmap bm = holder.mbitmap;
                    ImageView imageview = holder.mImageview;
                    String path = holder.path;

                    //对比标签，设置图片,防止标签错位
                    if(imageview.getTag().toString().equals(path)){
                        imageview.setImageBitmap(bm);
                    }
                }
            };
        }
        //根据路径从缓存中获取图片
        Bitmap bm = getBitmapFromLruCache(path);
        //如果缓存中已经取到了图片 则设置，如果没取到图片，创建任务线程去取
        if(bm != null){
            refreshUi(path, imageView, bm);
        }else{
            //创建线程，并添加入任务池
            addTask(new Runnable(){
                @Override
                public void run() {
                    //获取图片
                    //压缩图片
                    //1.获取图片需要显示的大小
                    ImageSize imageSize = getImageViewSize(imageView);
                    //2.压缩图片
                    Bitmap bm = decodeSampledBitmapFromPath(path,imageSize.width,imageSize.height);
                    //3.将图片加入到缓存
                    addBitmap2LruCache(path,bm);
                    //刷新界面
                    refreshUi(path, imageView, bm);

                    mSemaphoreThreadPool.release();
                }
            });
        }
    }

    /**刷新界面*/
    private void refreshUi(String path, ImageView imageView, Bitmap bm) {
        ImageBeanHolder imageHolder = new ImageBeanHolder();
        imageHolder.mbitmap = bm;
        imageHolder.mImageview = imageView;
        imageHolder.path = path;
        Message msg = Message.obtain();
        msg.obj = imageHolder;
        mUiHandler.sendMessage(msg);
    }

    /**将图片加入缓存*/
    private void addBitmap2LruCache(String path, Bitmap bm) {
        if(getBitmapFromLruCache(path) == null){
            if(bm != null)
                mLruCache.put(path,bm);
        }
    }

    /**根据图片需要显示的宽和高压缩图片*/
    private Bitmap decodeSampledBitmapFromPath(String path, int width, int height) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        //只解析图片的大小
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path,options);
        //计算图片的压缩比
        options.inSampleSize = caculateSampleSize(options,width,height);

        options.inJustDecodeBounds = false;
        Bitmap bm = BitmapFactory.decodeFile(path,options);
        return bm;
    }

    /**计算压缩比*/
    private int caculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width = options.outWidth;
        int height = options.outHeight;

        int inSampleSize = 1;

        if(width > reqWidth || height > reqHeight){
            //压缩
            int widthRadio = Math.round(width*1.0f/reqWidth);
            int heightRadio = Math.round(height*1.0f/reqHeight);

            inSampleSize = Math.max(widthRadio,heightRadio);
        }
        return inSampleSize;
    }

    /**根据imageview获取需要压缩的宽和高*/
    private ImageSize getImageViewSize(ImageView imageView) {
        ImageSize is = new ImageSize();
        DisplayMetrics dis =  imageView.getContext().getResources().getDisplayMetrics();
        ViewGroup.LayoutParams lp =  imageView.getLayoutParams();

        int width = imageView.getWidth();//获取imageview的实际宽高
        if(width <= 0){
            width = lp.width;//获取imageview在layout中声明的宽高
        }

        if(width <= 0){
//            width = imageView.getMaxWidth();//检查最大值
            width = getImageViewFieldValue(imageView,"mMaxWidth");
        }

        if(width <= 0){
            width = dis.widthPixels;//屏幕宽度
        }

        int height = imageView.getHeight();//获取imageview的实际宽高
        if(height <= 0){
            height = lp.height;//获取imageview在layout中声明的宽高
        }

        if(height <= 0){
//            height = imageView.getMaxHeight();//检查最大值
            height = getImageViewFieldValue(imageView,"mMaxHeight");
        }

        if(height <= 0){
            height = dis.heightPixels;//屏幕高度
        }


        is.width = width;
        is.height = height;
        return is;
    }

    /**通过属性名获取属性值*/
    private static int getImageViewFieldValue(Object object,String fieldName){
        int value = 0;
        try {
            Field field = ImageView.class.getDeclaredField(fieldName);
            field.setAccessible(true);

            int fieldValue = field.getInt(object);
            if(fieldValue > 0 && fieldValue < Integer.MAX_VALUE){
                value = fieldValue;
            }
        } catch (Exception e) {
        }



        return value;
    }

    /**图片大小*/
    private class ImageSize{
        int width;
        int height;
    }

    /**将线程添加进任务队列*/
    private synchronized void addTask(Runnable runnable) {
        mTaskQueue.add(runnable);
        if(mPoolHandler == null){
            try {
                mSemaphorePoolHandler.acquire();
            } catch (InterruptedException e) {
            }
        }
        mPoolHandler.sendEmptyMessage(0x110);
    }

    /**根据路径获取LruCache中的图片*/
    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }

    class ImageBeanHolder{
        ImageView mImageview;
        String path;
        Bitmap mbitmap;

    }
}
