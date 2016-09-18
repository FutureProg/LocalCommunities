package nick.com.localcommunity;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Created by Nick on 16-07-07.
 */
public class ImageCache {

    private static final String DIR_NAME = "images";

    private static HashMap<String,String> mImageCache;
    private static File mCacheDir;

    private static final Object mDiskCacheLock = new Object();

    public static final void init(final Context context){
        if(mImageCache != null)return;
        //get the cache directory
            final Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    synchronized (mDiskCacheLock){
                        String cachePath;
                        if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                            cachePath = context.getExternalCacheDir().getPath();
                        }else{
                            cachePath = context.getCacheDir().getPath();
                        }
                        mCacheDir = new File(cachePath + File.pathSeparator + DIR_NAME);
                        mImageCache = new HashMap<>();
                        if(!mCacheDir.exists()) {
                            Log.v("ImageCache","Creating cache dir");
                            mCacheDir.mkdir();
                        }else {
                            for (File file : mCacheDir.listFiles()) {
                                mImageCache.put(file.getName(), file.getName());
                                Log.v("ImageCache", "Putting items in cache " + file.getName());
                            }
                        }
                    }
                }
            });
            thread.start();

    }

    public static void cacheBitmap(@NonNull String key, @NonNull Bitmap bitmap){
        cacheBitmap(key,bitmap,false);
    }

    public static void cacheBitmap(@NonNull final String key, @NonNull final Bitmap bitmap, boolean overwrite){
        if(mImageCache == null){
            Log.w(ImageCache.class.getSimpleName(),"Image Cache not initialized," +
                    " unable to cache bitmap with key " + key + "\n" + Log.getStackTraceString(new Exception()));
            return;
        }
        if(mImageCache.containsKey(key) && !overwrite){
            Log.w(ImageCache.class.getSimpleName(),"Cache key " + key + "already exists." +
                    " Unable to cache bitmap " + bitmap + "\n" + Log.getStackTraceString(new Exception()));

            return;
        }
        final String fileName = key;// + bitmap.hashCode();
        final File file = new File(mCacheDir.getPath() + File.pathSeparator + fileName);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mDiskCacheLock) {
                    try {
                        file.createNewFile();
                        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));

                        int bytes = bitmap.getByteCount();
                        ByteBuffer buffer = ByteBuffer.allocate(bytes);
                        bitmap.copyPixelsToBuffer(buffer);
                        byte[] array = buffer.array();

                        BitmapSaver saver = new BitmapSaver();
                        saver.execute(out, array,bitmap);
                        mImageCache.put(key, fileName);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
    }

    public static void loadAsyncBitmap(@NonNull String key, @NonNull final BitmapLoadListener listener){
        if(mImageCache == null){
            Log.w(ImageCache.class.getSimpleName(),"Image Cache not initialized," +
                    " unable to cache bitmap with key " + key + "\n" + Log.getStackTraceString(new Exception()));
            return;
        }
        if(!mImageCache.containsKey(key)){
            Log.w(ImageCache.class.getSimpleName(),"Cache key " + key + "does not exist." +
                    " Unable to load bitmap." + "\n" + Log.getStackTraceString(new Exception()));
            return;
        }
        String filepath = mCacheDir.getPath() + File.pathSeparator + mImageCache.get(key);
        final File file = new File(filepath);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (mDiskCacheLock) {
                    try {
                        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
                        BitmapLoader loader = new BitmapLoader();
                        loader.execute(inputStream, file.length(), listener);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();

    }

    public static boolean inCache(String key){
        return mImageCache.containsKey(key);
    }



    private static class BitmapSaver extends AsyncTask<Object,Void,Void>{

        @Override
        protected Void doInBackground(Object... params) {
            synchronized (mDiskCacheLock){
                BufferedOutputStream out = (BufferedOutputStream) params[0];
                byte[] bytes = (byte[])params[1];
                Bitmap bitmap = (Bitmap)params[2];
                bitmap.compress(Bitmap.CompressFormat.JPEG,40,out);
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                    /*out.write(bytes,0,bytes.length);
                    out.flush();
                    out.close();*/
                return null;
            }
        }
    }

    private static class BitmapLoader extends AsyncTask<Object,Void,byte[]>{

        BitmapLoadListener listener;

        @Override
        protected byte[] doInBackground(Object... params) {
            synchronized (mDiskCacheLock){
                BufferedInputStream in = (BufferedInputStream) params[0];
                long size = (Long)params[1];
                if(params.length == 3)
                    listener = (BitmapLoadListener)params[2];
                byte[] array = new byte[(int)size];
                try {
                    in.read(array);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return array;
            }
        }

        @Override
        protected void onPostExecute(byte[] array) {
            super.onPostExecute(array);
            if(listener != null){
                listener.loadedBitmap(array);
            }
        }
    }

    public interface BitmapLoadListener{
        public void loadedBitmap(byte[] data);
    }


}
