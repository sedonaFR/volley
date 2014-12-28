volley
======

Android Volley - easier to use and bug corrections


## How To Use?
In all samples, we guess that the expected result model is List<Car> and the model on error is MyModelError

The entry point is RequestBuilder, example for a json result: 
```
RequestBuilder<List<Car>> rb = new RequestBuilder<>("http://yoururl");
rb.callbackResult(callback);
rb.parserJson(new TypeToken<QueryResult<List<Car>>>() {
}.getType());
rb.start();
```

We recommend to extends RequestBuilder and statically adds your Web Service generalities here (base url, signing key, ...)


## Callback

Quick callback:
```
QueryCallback<List<Car>, MyModelError> callback = new QueryCallback<>(){
    public void onQueryFinished(int idQuery, ResultInfo queryInfo, List<Car> data, Void error){
        if(data != null){
            //Do what you want
        } else{
            //Display the error message
        }
    }
}
```

Callback with full error handling:
Let's guess that the server returns the model MyModelError on error:

```
QueryCallback<List<Car>, MyModelError> callback = new QueryCallback<>(){
    public void onQueryFinished(int idQuery, ResultInfo queryInfo, List<Car> data, MyModelError error){
        if(data != null){
            //DO what you want
        } else if (error != null){
            //Display the server error message
        } else {
            switch(queryInfo.codeQuery){
                case TIMEOUT_ERROR:
                    break;
                case NOT_AUTHORIZED:
                    break;
                case SERVER_ERROR:
                    break;
                case NETWORK_ERROR:
                    break;
                default:
                    break;
            }
        }
    }
}
```

## Functionnalities
RequestBuilder has some builders methods to help creating the request:

### Cache
- rb.noCache();
- rb.cacheTimeToRefresh(10000L);
- rb.cacheTimeToLive(10000L);
- rb.allowBeanCache(true);

allowBeanCache allow to cache the parsed object, preventing the app to reparse it again later. The query will act as a fast DataStore

### POST
Manage multipart post, form post, raw post
You can cache the POST result if a key is provided through postAsCacheKey

- rb.postParamRaw("STRING_TO_POST");
- rb.postAddMultipartForm("FORM_KEY", "FORM_VALUE");
- rb.postAddMultipart("HEADER_MULTIPART", bytes);
- rb.postParamUrlEncoded(MAP<KEY, VALUE>);
- rb.postAsCacheKey();

### PARSING

#### JSON Parser
Manage JSON parser with GSON (In a future release, a streaming Json API will be used as default):
- rb.parserJson();

It takes for arguments:
```
new TypeToken<QueryResult<List<Car>>>() {
}.getType();
```

#### Custom Parser
- rb.parser();
It takes for arguments an SimpleParser

Some SimpleParser are implemented to parse a Boolean, an Integer, a String and a Date.

#### XML Parser
simple-xml has been chosen for its efficient data-model bindings.
1. Use simple-xml-2.7.1 http://simple.sourceforge.net/download.php and put the jar in a lib/ folder in the root of the project
2. Use the Class XmlParserSimple.

Also allow to parse the data on server error.
- rb.parserErrorJson();
- rb.parserError();

### Cookie
This version of Volley manages cookies

- rb.allowCookies();
- rb.clearAllCookies();

### OTHER
- tag(); set a tag object given back when the query end
- id(); set an integer id to identify the query

### CERTIFICATE
- loadCA() : load a certificate from the assets

'
