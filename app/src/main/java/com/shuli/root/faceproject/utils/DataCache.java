/*
 * Copyright 2017 GcsSloop
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last modified 2017-03-12 02:50:16
 *
 * GitHub:  https://github.com/GcsSloop
 * Website: http://www.gcssloop.com
 * Weibo:   http://weibo.com/GcsSloop
 */

package com.shuli.root.faceproject.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.LruCache;

import com.shuli.root.faceproject.bean.User;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据缓存工具
 */
public class DataCache {
    private static int M = 1024 * 1024;
    ACache mDiskCache;
    LruCache<String, Object> mLruCache;

    public DataCache(Context context) {
        mDiskCache = ACache.get(new File(FileUtil.getExternalCacheDir(context.getApplicationContext(), "face-data")));
        mLruCache = new LruCache<>(5 * M);
    }

    public <T extends Serializable> void saveListData(String key, List<T> data) {
        ArrayList<T> datas = (ArrayList<T>) data;
        mLruCache.put(key, datas);
        mDiskCache.put(key, datas, ACache.TIME_WEEK);     // 数据缓存时间为 1 周
    }

    public <T extends Serializable> void saveData(@NonNull String key, @NonNull T data) {
        mLruCache.put(key, data);
        mDiskCache.put(key, data, ACache.TIME_WEEK);     // 数据缓存时间为 1 周
    }

    public <T extends Serializable> T getData(@NonNull String key) {
        T result = (T) mLruCache.get(key);
        if (result == null) {
            result = (T) mDiskCache.getAsObject(key);
            if (result != null) {
                mLruCache.put(key, result);
            }
        }
        return result;
    }

    public void removeDate(String key) {
        mDiskCache.remove(key);
    }

    public void saveUser(User user) {
        saveData("user", user);
    }

    public User getUser() {
        return getData("user");
    }





}
