package fr.sedona.volley.parsers;

/**
 * Created by bdelville on 09/07/2014.
 */
public class BooleanParser implements SimpleParser<Boolean> {
    @Override
    public Boolean parse(byte[] data) {
        return "true".equalsIgnoreCase(new String(data).trim());
    }
}
