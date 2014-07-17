package fr.sedona.volley.parsers;

/**
 * Created by bdelville on 09/07/2014.
 */
public class IntegerParser implements SimpleParser<Integer> {
    @Override
    public Integer parse(byte[] data) {
        return Integer.parseInt(new String(data));
    }
}
