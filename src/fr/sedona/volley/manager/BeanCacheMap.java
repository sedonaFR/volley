package fr.sedona.volley.manager;

import java.util.LinkedHashMap;

/**
 * Created by bdelville on 10/02/14.
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
