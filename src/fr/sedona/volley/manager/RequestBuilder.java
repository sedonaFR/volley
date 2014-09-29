/*
 * Copyright (C) 2014 Modified by Sedona Paris
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

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import fr.sedona.volley.parsers.SimpleParser;

/**
 * Build a HTTP request supporting POST, GET, PUT, POST and any kind of data parsing
 * @param <T> Succes object type
 * @param <E> Error Object type
 */
public class RequestBuilder<T, E> extends Request<T> implements Response.ErrorListener {

    public static final int POST = Method.POST;
    public static final int GET = Method.GET;
    public static final int PUT = Method.PUT;
    public static final int DELETE = Method.DELETE;

    public static final int DEFAULT_REQUEST_TIMEOUT = 15000;
    public static final int NO_REQUEST_RETRY_COUNT = 0;
    public static final long CACHE_TIME_ONE_DAY = 1000 * 60 * 60 * 24;

    protected WeakReference<QueryCallback<T, E>> callbackRef;
    protected Object parser;
    protected Object parserError;
    protected int queryId;
    protected long cacheTimeToRefresh;
    protected long cacheTimeToLive;
    protected boolean allowBeanCache;
    private String paramEncoding;

    //protected String postParamRaw;
    protected ByteArrayOutputStream postParamRaw;
    protected String contentType;
    protected static String multipartBoundary = "AaB03xBounDaRy";
    protected Map<String, String> postParamUrlEncoded;

    protected static Gson gson;
    protected static RequestQueue queue;
    private Object tag;
    private StringPreprocessor preprocessor;

    public static RequestQueue getQueue(){
        return queue;
    }

    /**
     * Stop all queries and remove all cache
     */
    public static void clearAll(){
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        queue.getCache().clear();
        BeanCacheMap.get().clear();
    }

    public static void clearCacheUpdate(String... criteria){
        BeanCacheMap.get().clear();//Clear all RAM cache: this cache is used only for high performance when going back and up again
        RequestBuilder.getQueue().getCache().removeByCriteria(criteria);
    }

    public void noCache() {
        cacheTimeToRefresh(0);
        cacheTimeToLive(0);
        allowBeanCache(false);
    }

    public static interface StringPreprocessor {
        public String preprocess(String s);
    }

    public void setPreprocessor(StringPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    /**
     * Override to use a specific Gson (de)serializer
     */
    public static void setGson(Gson g){
        gson = g;
    }

    public static void init(Context ctx){
        gson = new Gson();
        queue = Volley.newRequestQueue(ctx);
    }

    /**
     * Use only if a specific queue is wanted
     * @param q
     */
    public static void setQueue(RequestQueue q){
        queue = q;
    }

    /**
     * Set the callback to receive the query result
     * @param callback
     * @return
     */
    public RequestBuilder callback(QueryCallback<T, E> callback){
        callbackRef = new WeakReference<QueryCallback<T, E>>(callback);
        return this;
    }

    /**
     * Send data with the query as it is (no formatting)
     * @param postParamRaw
     * @return
     */
    public RequestBuilder postParamRaw(String postParamRaw) {
        try {
            this.postParamRaw = new ByteArrayOutputStream();
            this.postParamRaw.write(postParamRaw.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    private boolean isMultipart = false;


    public RequestBuilder postAddMultipart(String headerText, byte[] postParamRaw) {
        isMultipart = true;
        if(contentType == null){
            contentType = "multipart/form-data, boundary="+multipartBoundary;
        }
        if(this.postParamRaw == null){
            this.postParamRaw = new ByteArrayOutputStream();
        }

        try{
            this.postParamRaw.write(("\r\n--"+multipartBoundary+"\r\n").getBytes());
            this.postParamRaw.write((headerText + "\r\n\r\n").getBytes());
            this.postParamRaw.write(postParamRaw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public RequestBuilder postAddMultipartForm(String name, String value) {
        isMultipart = true;
        if(contentType == null){
            contentType = "multipart/form-data, boundary="+multipartBoundary;
        }
        if(this.postParamRaw == null){
            this.postParamRaw = new ByteArrayOutputStream();
        }

        try{
            this.postParamRaw.write(("\r\n--"+multipartBoundary+"\r\n").getBytes());
            this.postParamRaw.write(("content-disposition: form-data; name=" + name+"\r\n").getBytes());
            this.postParamRaw.write(("\r\n" + value).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Force a content-Type
     * @param contentType
     * @return
     */
    public RequestBuilder contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    /**
     * Data to send with the query as UrlEncoded form
     * @param postParamUrlEncoded
     * @return
     */
    public RequestBuilder postParamUrlEncoded(Map<String, String> postParamUrlEncoded) {
        this.postParamUrlEncoded = postParamUrlEncoded;
        return this;
    }

    /**
     * Id of the request
     * @param id
     * @return
     */
    public RequestBuilder id(int id) {
        this.queryId = id;
        return this;
    }

    /**
     * Parser of success
     * @param type
     * @return
     */
    public RequestBuilder parserJson(Type type) {
        this.parser = type;
        return this;
    }

    /**
     * An object sent back with the query callback
     * @param tag
     * @return
     */
    public RequestBuilder tag(Object tag) {
        this.tag = tag;
        return this;
    }

    /**
     * Call it once to allow the cookie management for any query
     */
    public static void allowCookies(){
        BasicNetwork.allowCookies();
    }

    /**
     * Parser of success
     * @param type
     * @return
     */
    public RequestBuilder parser(SimpleParser type) {
        this.parser = type;
        return this;
    }

    /**
     * Parser of error
     * @param typeError
     * @return
     */
    public RequestBuilder parserErrorJson(Type typeError) {
        this.parserError = typeError;
        return this;
    }

    /**
     * Parser of error
     * @param typeError
     * @return
     */
    public RequestBuilder parserError(SimpleParser typeError) {
        this.parserError = typeError;
        return this;
    }

    /**
     * If the data is out of date, the result with still be returned but a query is loaded
     * @param cacheTimeToRefresh
     * @return
     */
    public RequestBuilder cacheTimeToRefresh(long cacheTimeToRefresh) {
        this.cacheTimeToRefresh = cacheTimeToRefresh;
        return this;
    }

    public RequestBuilder paramEncoding(String encoding){
        paramEncoding = encoding;
        return this;
    }

    /**
     * If the data is out of date, a query must be done before providing the result
     * @param cacheTimeToLive
     * @return
     */
    public RequestBuilder cacheTimeToLive(long cacheTimeToLive) {
        this.cacheTimeToLive = cacheTimeToLive;
        return this;
    }

    /**
     * Allow to cache parsed Object to the RAM with a auto-managed size limit
     * @param allowBeanCache
     * @return
     */
    public RequestBuilder allowBeanCache(boolean allowBeanCache) {
        this.allowBeanCache = allowBeanCache;
        return this;
    }

    /**
     * Json data to send with the query
     * @param data
     * @return
     */
    public RequestBuilder jsonData(Object data) {
        contentType("application/json");
        return postParamRaw(gson.toJson(data));
    }

    /**
     * Create a GET request
     * @param url Url to use
     * @param getParams Get params or null, mandatory because it is used to generate the url
     */
    public RequestBuilder(String url, Map<String, Object> getParams) {
        this(RequestBuilder.GET, url, getParams);
    }

    /**
     * Create a request with mandatory parameters without GET Params
     * @param method Volley post or get method, etc : RequestBuilder.GET, RequestBuilder.POST
     * @param url Url to use
     */
    public RequestBuilder(int method, String url) {
        this(method, null, null);
    }

    /**
     * Create a request with mandatory parameters
     * @param method Volley post or get method, etc : RequestBuilder.GET, RequestBuilder.POST
     * @param url Url to use
     * @param getParams Get params or null, mandatory because it is used to generate the url
     */
    public RequestBuilder(int method, String url, Map<String, Object> getParams) {
        super(method, null, null);
        setErrorListener(this);
        setUrl(generateUrl(url, getParams));
        setRetryPolicy(new DefaultRetryPolicy(DEFAULT_REQUEST_TIMEOUT, NO_REQUEST_RETRY_COUNT, 1f));

        //set Default cache values
        cacheTimeToRefresh = 0;
        cacheTimeToLive = 0;
        allowBeanCache = false;
    }

    /**
     * Start the query. At last!
     */
    public void start(){
        if (allowBeanCache) {
            Object dataParsed = BeanCacheMap.get().get(getUrl());
            if (dataParsed != null) {
                try {
                    ResultInfo queryResultInfo = new ResultInfo(ResultInfo.CODE_QUERY.SUCCESS);
                    sendCallback(queryResultInfo, (T) dataParsed, null);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        queue.add(this);
    }

    protected String generateUrl(String url, Map<String, Object> getParameters) {
        if(getParameters == null || url == null) {
            return url;
        }
        return url + generateGetParameters(url, getParameters);
    }

    private String generateGetParameters(String url, Map<String, Object> parameters) {
        boolean isFirstElement = !url.contains("?");
        String getParameters = "";
        Iterator it = parameters.entrySet().iterator();

        while (it.hasNext()) {
            if (isFirstElement) {
                isFirstElement = false;
                getParameters += "?";
            } else {
                getParameters += "&";
            }

            Map.Entry pairs = (Map.Entry)it.next();
            try {
                getParameters += (pairs.getKey() + "=" + URLEncoder.encode(pairs.getValue().toString(), "UTF-8"));
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return getParameters;
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        if (postParamRaw != null) {
            if(isMultipart){
                try{
                    this.postParamRaw.write(("\r\n--"+multipartBoundary+"--\r\n").getBytes());
                }catch (Exception e){}
            }

//            if(paramEncoding != null){
//                try {
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    OutputStreamWriter writer = new OutputStreamWriter(baos, paramEncoding);
//                    writer.append(postParamRaw);
//                    writer.close();
//
//                    return baos.toByteArray();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }

            return postParamRaw.toByteArray();
        }
        return super.getBody();
    }

    public String getBodyContentType() {
        if (contentType != null) {
            return contentType + "; charset=" + getParamsEncoding();
        }
        return super.getBodyContentType();
    }

    @Override
    protected String getParamsEncoding() {
        if(paramEncoding != null){
            return paramEncoding;
        }
        return super.getParamsEncoding();
    }

    @Override
    public String getCacheKey() {
        //Can be overriden to add specific info to the cache
        return super.getCacheKey();
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return postParamUrlEncoded;
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse networkResponse) {
        //Parse
        T dataParsed = parseData(parser, networkResponse.data);

        //Format Cache structure
        long now = System.currentTimeMillis();

        //Build cache entry for this occur
        Cache.Entry entry = new Cache.Entry();
        entry.data = networkResponse.data;
        entry.etag = null;
        entry.softTtl = now + cacheTimeToRefresh;
        entry.ttl = now + cacheTimeToLive;
        entry.serverDate = now;
        entry.responseHeaders = networkResponse.headers;

        return Response.success(dataParsed, entry);
    }

    @Override
    protected void deliverResponse(T dataParsed) {
        if (allowBeanCache) {
            BeanCacheMap.get().put(getUrl(), dataParsed);
        }

            //Build metadata for callback
            ResultInfo queryResultInfo = new ResultInfo();
            queryResultInfo.setTag(tag);

            if (queue.getCache() != null) {
                Cache.Entry currentDataCacheEntry = queue.getCache().get(this.getCacheKey());
                if (currentDataCacheEntry != null) {
                    //In this case, Volley return the data cache in first callback, then network result in a 2nd callback
                    queryResultInfo.dataIsRefreshing = !currentDataCacheEntry.isExpired() && currentDataCacheEntry.refreshNeeded();
                    queryResultInfo.dataDatetime = currentDataCacheEntry.serverDate;
                }
            }
            queryResultInfo.codeQuery = ResultInfo.CODE_QUERY.SUCCESS;

            sendCallback(queryResultInfo, dataParsed, null);
    }

    protected void sendCallback(ResultInfo queryResultInfo, T dataParsed, E dataError){
        if(callbackRef != null) {
            QueryCallback<T, E> callback = callbackRef.get();
            if (callback == null) {
                return;
            }
            callback.onQueryFinished(queryId, queryResultInfo, dataParsed, dataError);
        }
    }


    @SuppressWarnings("unchecked")
    @Override
    public void onErrorResponse(VolleyError error) {
        if (VolleyLog.DEBUG) {
            error.printStackTrace();
        }


            ResultInfo queryResultInfo = new ResultInfo(ResultInfo.CODE_QUERY.SERVER_ERROR);
            queryResultInfo.setTag(tag);
            E dataParsed = null;

            if (error instanceof TimeoutError || error instanceof NetworkError) {
                queryResultInfo.codeQuery = ResultInfo.CODE_QUERY.NETWORK_ERROR;

            } else if (error.networkResponse != null) {
                int http = error.networkResponse.statusCode;
                queryResultInfo.errorResponse = error.networkResponse.data;
                queryResultInfo.httpCode = http;

                if (http == 401 || http == 403) {
                    queryResultInfo.codeQuery = ResultInfo.CODE_QUERY.NOT_AUTHORIZED;
                }

                try{
                    dataParsed = parseData(parserError,  error.networkResponse.data);
                } catch(Exception e){
                    e.printStackTrace();
                }
            }

            sendCallback(queryResultInfo, null, dataParsed);
    }

    /**
     *
     * @param dataParser
     * @param data
     * @param <U> T or E depending of the data to be parsed
     * @return
     */
    private <U> U parseData(Object dataParser, byte[] data) {
        //String dataStr = new String(data);
        U dataParsed = null;

        if (dataParser != null) {
            if (dataParser instanceof SimpleParser) {
                //Custom parsing
                dataParsed = (U) ((SimpleParser)dataParser).parse(data);

            } else if (dataParser instanceof Type){
                //Json parsing
                Type type = (Type) dataParser;

                String s = new String(data);
                if(preprocessor != null) {
                    s = preprocessor.preprocess(s);
                }
                dataParsed = gson.fromJson(s, type);

//                    Reader reader = new InputStreamReader(new ByteArrayInputStream(data));
//                    JsonReader jsonReader = new JsonReader(reader);
//                    jsonReader.beginObject();
//                    gson.fromJson(jsonReader, type);

            }
        }
        return dataParsed;
    }

    /**
     * Do not use except if strictly required
     * @param context
     */
    public static void trustAllCertificate(Context context) {
        Log.e("trustAllCertificate", "Security warning: All ssl certificates will be trusted!!!");

        try {
            //BUILD SSL SOCKET FACTORY
            KeyStore keyStore = null;
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(keyStore);

            SSLContext sslctx = SSLContext.getInstance("TLS");
            sslctx.init(null, tmf.getTrustManagers(), null);

            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            sslctx.init(null, new TrustManager[]{tm}, null);
            //urlConnection.setSSLSocketFactory(sslctx.getSocketFactory());

            //BUILD CUSTOM STACK
            HttpStack stack;
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new HurlStack(null, sslctx.getSocketFactory());
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                String userAgent = "volley/0";
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }


            //SET QUEUE
            queue = Volley.newRequestQueue(context, stack);
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}