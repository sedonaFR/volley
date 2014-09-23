/*
 * Copyright (C) 2014Modified by Sedona Paris
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.sedona.volley.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.StatFs;
import android.util.Log;
import android.support.v4.util.LruCache;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

public class HttpImageLoader {
    private static final int DEFAULT_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory()) / 8;
    private static HttpImageLoader instance;
    private ImageLoader imgLoader;
    private ImageLoader imgLoaderPNG;
    private ImageLoader imgLoaderPermanent;

    private RamImageCache ramCache;
    private DiskImageCache diskCache;
    private DiskImageCache diskCachePNG;
    private DiskImageCache diskCachePermanent;
    private WeakRamCache weakCache;
    private RequestQueue mRequestQueue;

    private HttpImageLoader() {

    }

    public ImageLoader getLoader(){
        if(imgLoader == null){
            Log.e("HttpImageLoader", "Must call initImageLoader method of HttpImageLoader");
        }
        return imgLoader;
    }

    public ImageLoader getLoader(boolean needTransparency, boolean needPermanency){
        if (needPermanency) {
            return imgLoaderPermanent;
        }
        if(needTransparency){
            return imgLoaderPNG;
        }
        return getLoader();
    }

    public static HttpImageLoader get() {
        if (instance == null) {
            instance = new HttpImageLoader();
        }
        return instance;
    }

    /**
     * Must be called once at app startup
     * @param ctx
     * @param ratioTransparency part of disk cache reserved for transparent images - 0 mean no transparent images
     */
    public void initImageLoader(Context ctx, int ratioTransparency) {
        initImageLoader(ctx, ratioTransparency, false);
    }

    /**
     * Must be called once at app startup
     * @param ctx
     * @param ratioTransparency part of disk cache reserved for transparent images - 0 mean no transparent images
     */
    public void initImageLoader(Context ctx, float ratioTransparency, boolean hasPermanentImages) {
        ramCache = new RamImageCache();
        weakCache = new WeakRamCache();

        long diskSpace = getDiskCacheSpace(ctx);
        if(ratioTransparency >1 || ratioTransparency < 0){
            ratioTransparency = 0;
        }

        // Share the disk space between PNG and JPEG (ratio must be between 0 and 1)
        diskCache = new DiskImageCache(ctx, Bitmap.CompressFormat.JPEG, (int) (diskSpace * (1-ratioTransparency)));
        if(ratioTransparency > 0){
            diskCachePNG = new DiskImageCache(ctx, Bitmap.CompressFormat.PNG, (int) (diskSpace * ratioTransparency));
        }
        if (hasPermanentImages) {
            diskCachePermanent = new DiskImageCache(ctx, Bitmap.CompressFormat.JPEG, 1024 * 3);
        }

        mRequestQueue = Volley.newRequestQueue(ctx);

        //"Null" and not "this" as second argument: We do not provide cache for Volley - implement it ourself instead, this is faster and synchrone
        imgLoader = new ImageLoader(mRequestQueue, volleyCacheProxy);
        imgLoaderPNG = new ImageLoader(mRequestQueue, volleyCachePNGProxy);
        imgLoaderPermanent = new ImageLoader(mRequestQueue, volleyCachePermanentProxy);
    }

    public void initImageLoader(Context ctx) {
        initImageLoader(ctx, 0);
    }

    private class ProxyCache implements ImageLoader.ImageCache{

        private boolean isPngCache = false;
        private boolean isPermanentCache = false;

        private ProxyCache(boolean isPngCache, boolean isPermanentCache) {
            this.isPngCache = isPngCache;
            this.isPermanentCache = isPermanentCache;
        }

        @Override
        public Bitmap getBitmap(String s) {
            //We getBitmapRam ourselves to do it synchronously - No need to implement it again
            //But we want ROM image to load async:
            if(diskCache != null) {
                if(s.startsWith("#W0#H0"))
                    s = s.substring(6);

                Bitmap bmp = null;
                if(isPngCache){
                    bmp = diskCachePNG.getBitmap(s);
                } else if (isPermanentCache) {
                    bmp = diskCachePermanent.getBitmap(s);
                } else {
                    bmp = diskCache.getBitmap(s);
                }

                if (bmp != null) {
                    ramCache.putBitmap(s, bmp);
                }
                return bmp;
            }
            return null;
        }

        @Override
        public void putBitmap(String s, Bitmap bitmap) {
            //Remove volley tags
            if(s.startsWith("#W0#H0")){
                s = s.substring(6);
            }

            ramCache.put(s, bitmap);
            if(isPngCache && diskCachePNG != null){
                diskCachePNG.putBitmap(s, bitmap);
            }
            else if (isPermanentCache && diskCachePermanent != null) {
                diskCachePermanent.putBitmap(s, bitmap);
            }
            else if (!isPngCache && !isPermanentCache && diskCache != null) {
                diskCache.putBitmap(s, bitmap);
            }
        }
    }

    private ImageLoader.ImageCache volleyCacheProxy = new ProxyCache(false, false);
    private ImageLoader.ImageCache volleyCachePNGProxy = new ProxyCache(true, false);
    private ImageLoader.ImageCache volleyCachePermanentProxy = new ProxyCache(false, true);

    public Bitmap getBitmapRam(String url) {
        Bitmap bmp = ramCache.getBitmap(url);

        if(bmp == null){
            bmp = weakCache.getBitmap(url);
        }
        return bmp;
    }

    public void cacheModifiedBitmap(String url, Bitmap bitmap, boolean isPNG) {
        cacheModifiedBitmap(url, bitmap, isPNG, false);
    }

    public void cacheModifiedBitmap(String url, Bitmap bitmap, boolean isPNG, boolean isPermanent) {
        ramCache.put(url, bitmap);
        if (diskCache != null && !isPNG) {
            diskCache.putBitmap(url, bitmap);
        }
        if (diskCachePNG != null && isPNG) {
            diskCachePNG.putBitmap(url, bitmap);
        }
        if (diskCachePermanent != null && isPermanent) {
            diskCachePermanent.putBitmap(url, bitmap);
        }
    }


    // /////////////////////////////////////////////////////////////////////////////////////
    // ////////////////// GESTION DU CACHE : ROM et RAM /////////////////////////
    // ///////////////////////////////////////////////////////////////////////////////////

    private class WeakRamCache implements ImageLoader.ImageCache {
        private ConcurrentHashMap<String, WeakReference<Bitmap>> sWeakBitmapCache = new ConcurrentHashMap<String, WeakReference<Bitmap>>(50);

        @Override
        public Bitmap getBitmap(String url) {
            WeakReference<Bitmap> bitmapReference = sWeakBitmapCache.get(url);

            if (bitmapReference != null) {
                final Bitmap bitmap = bitmapReference.get();
                if (bitmap != null) {
                    // Bitmap found in soft cache
                    return bitmap;
                } else {
                    // Weak reference has been Garbage Collected
                    sWeakBitmapCache.remove(url);
                }
            }
            return null;
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            sWeakBitmapCache.put(url, new WeakReference<Bitmap>(bitmap));
        }
    }

    private class RamImageCache extends LruCache<String, Bitmap> implements ImageLoader.ImageCache {

        public RamImageCache() {
            super(DEFAULT_CACHE_SIZE);
            if(VolleyLog.DEBUG) {
                Log.d("RamImageCache", "Cache size = " + DEFAULT_CACHE_SIZE);
            }
        }

        @Override
        protected int sizeOf(String key, Bitmap value) {
            int bytes = (value.getRowBytes() * value.getHeight());
            return bytes;
        }

        @Override
        public Bitmap getBitmap(String url) {
            Bitmap bmp = get(url);
            if (bmp!=null && bmp.isRecycled()) {
                return null;
            }
            return bmp;
        }

        @Override
        public void putBitmap(String url, Bitmap bitmap) {
            put(url, bitmap);
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            if(!evicted){
                return;
            }
            weakCache.putBitmap(key, oldValue);
            if (diskCache != null) {
                //TODO diskCache.cacheModifiedBitmap: Stack the url to a SdCardSaverPool that asynchronously retrieve the image from ramCache and save it to sdCard - Save it here only if it has not be done previously
                //diskCache.cacheModifiedBitmap(key, oldValue);
            }
        }
    }

    @SuppressLint("NewApi")
    private long getDiskCacheSpace(Context context){
        String cachePath = context.getCacheDir().getPath();
        File diskCacheDir = new File(cachePath);

        if (Build.VERSION.SDK_INT >= 9) {
            return Math.min(diskCacheDir.getFreeSpace() - (1024 * 1024 * 30), (long) 1024 * 1024 * 20);
        }
        StatFs statFs = new StatFs(diskCacheDir.getAbsolutePath());
        return (long)Math.min(statFs.getAvailableBlocks() * (long)statFs.getBlockSize()- (1024 * 1024 * 30), (long) 1024 * 1024 * 20);
    }

    private class DiskImageCache implements ImageLoader.ImageCache {

        private final int IO_BUFFER_SIZE = 8 * 1024;
        private final int APP_VERSION = 1;
        private final int VALUE_COUNT = 1;
        private DiskLruCache mDiskCache;
        private Bitmap.CompressFormat mCompressFormat;
        private int mCompressQuality = 75;

        public DiskImageCache(Context context, Bitmap.CompressFormat format, int diskCache) {
            mCompressFormat = format;

            if(context.getCacheDir() == null){
                return;
            }
            try {
                String cachePath = context.getCacheDir().getPath();
                File diskCacheDir = new File(cachePath + File.separator + "img");
                diskCacheDir.mkdirs();

                // Prend 20Mo de préférence, en laissant toujours au moins 30Mo de libre à Android
                long diskCacheSize = diskCache <= 0 ? (getDiskCacheSpace(context)) : diskCache;
                if(VolleyLog.DEBUG){
                    Log.d("DiskImageCache", "Cache size = " + diskCacheSize);
                }

                if (diskCacheSize > 0) {
                    mDiskCache = DiskLruCache.open(diskCacheDir, APP_VERSION, VALUE_COUNT, diskCacheSize);
                } else {
                    if(VolleyLog.DEBUG) {
                        Log.w("DiskImageCache", "Available space is null! No Hard Cache");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean writeBitmapToFile(Bitmap bitmap, DiskLruCache.Editor editor) throws IOException, FileNotFoundException {
            OutputStream out = null;
            try {
                out = new BufferedOutputStream(editor.newOutputStream(0), IO_BUFFER_SIZE);
                return bitmap.compress(mCompressFormat, mCompressQuality, out);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }

        @Override
        public void putBitmap(String key, Bitmap data) {
            if (mDiskCache == null) {
                return;
            }
            DiskLruCache.Editor editor = null;
            try {
                editor = mDiskCache.edit(md5(key));
                if (editor == null) {
                    return;
                }

                if (writeBitmapToFile(data, editor)) {
                    mDiskCache.flush();
                    editor.commit();
                } else {
                    editor.abort();
                }
            } catch (IOException e) {
                try {
                    if (editor != null) {
                        editor.abort();
                    }
                } catch (IOException ignored) {
                }
            }

        }

        @Override
        public Bitmap getBitmap(String key) {
            if (mDiskCache == null) {
                return null;
            }

            Bitmap bitmap = null;
            DiskLruCache.Snapshot snapshot = null;

            try {
                snapshot = mDiskCache.get(md5(key));
                if (snapshot == null) {
                    return null;
                }
                final InputStream in = snapshot.getInputStream(0);
                if (in != null) {
                    final BufferedInputStream buffIn = new BufferedInputStream(in, IO_BUFFER_SIZE);
                    bitmap = BitmapFactory.decodeStream(buffIn);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (snapshot != null) {
                    snapshot.close();
                }
            }

            return bitmap;
        }

        public void clearCache() {
            try {
                mDiskCache.delete();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String md5(String s) {
        if (s == null) {
            return null;
        }
        try {
            byte messageDigest[] = MessageDigest.getInstance("MD5").digest(s.getBytes());
            // Create Hex String
            BigInteger bi = new BigInteger(1, messageDigest);
            String result = bi.toString(16);
            if (result.length() % 2 != 0)
                result = (new StringBuilder("0")).append(result).toString();

            return result;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
