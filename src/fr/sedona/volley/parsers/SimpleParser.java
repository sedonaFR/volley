package fr.sedona.volley.parsers;

/**
 * Created by bdelville on 11/04/14.
 * Custom parser used for a query
 */
public interface SimpleParser<T> {
    public T parse(byte[] data);
}
