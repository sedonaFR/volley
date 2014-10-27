package fr.sedona.volley.parsers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by bdelville on 27/10/2014.
 */
public class DateString {
    private String value;
    private Date tmpDate;
    private static Map<String, SimpleDateFormat> formatters = new HashMap<String, SimpleDateFormat>();

    public static Date getDate(DateString dateStr, SimpleDateFormat sdf){
        if(dateStr == null){
            return null;
        }
        return dateStr.getDate(sdf);
    }

    public static Date getDate(DateString dateStr, String format){
        if(dateStr == null){
            return null;
        }
        return dateStr.getDate(format);
    }

    public Date getDate(String format){
        if(tmpDate == null && value != null){
            SimpleDateFormat sdf = formatters.get(format);
            if(sdf == null){
                sdf = new SimpleDateFormat(format, Locale.getDefault());
                formatters.put(format, sdf);
            }

            try {
                tmpDate = sdf.parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return tmpDate;
    }

    public Date getDate(SimpleDateFormat sdf){
        if(tmpDate == null && value != null){
            try {
                tmpDate = sdf.parse(value);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return tmpDate;
    }

    public static class DateStringsDeserializer implements JsonDeserializer<DateString> {
        public DateString deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            DateString out = new DateString();
            out.value = json.getAsString();
            return out;
        }
    }
}
