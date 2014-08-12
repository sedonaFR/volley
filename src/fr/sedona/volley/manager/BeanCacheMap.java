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

import java.util.LinkedHashMap;

/**
 * Cache des Bean en RAM, déjà parsé, pour un accès synchrone et rapide
 */
public class BeanCacheMap extends LinkedHashMap<String, Object> {

    private static int MAX_ENTRIES = 15;
    private static BeanCacheMap instance;

    private BeanCacheMap(){
        super(MAX_ENTRIES, 1);
    }

    public static BeanCacheMap get(){
        if(instance == null){
            instance = new BeanCacheMap();
        }
        return instance;
    }

    public static void setMaxEntriesCount(int value){
        MAX_ENTRIES = value;
    }

    @Override
    protected boolean removeEldestEntry(Entry eldest) {
        return size() > MAX_ENTRIES;
    }

}
