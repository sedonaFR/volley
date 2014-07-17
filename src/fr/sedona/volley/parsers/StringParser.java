package fr.sedona.volley.parsers;

/**
 * Created by bdelville on 09/07/2014.
 */
public class StringParser implements SimpleParser<String> {
    @Override
    public String parse(byte[] data) {
        return new String(data);
    }
}
