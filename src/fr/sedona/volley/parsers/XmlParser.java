package fr.sedona.volley.parsers;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.ByteArrayInputStream;

/**
 * Created by bdelville on 23/07/2014.
 */
public class XmlParser<T> implements SimpleParser<T> {

    private Class<T> clazz;

    public XmlParser(Class<T> clazz) {
        this.clazz = clazz;
    }


    @Override
    public T parse(byte[] data) {
        Serializer serializer = new Persister();
        try {
            return (T) serializer.read(clazz, new ByteArrayInputStream(data));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
