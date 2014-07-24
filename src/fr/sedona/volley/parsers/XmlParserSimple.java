package fr.sedona.volley.parsers;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by bdelville on 23/07/2014.
 * The Persister is not referenced directly but dynamically to avoid being force to load the jar.
 * Use simple-xml-2.7.1 http://simple.sourceforge.net/download.php
 */
public class XmlParserSimple<T> implements SimpleParser<T> {

    private Class<T> clazz;

    private static Object serializer;
    private static Method method;

    /**
     *
     * @param clazz Class du résultat final à parser
     */
    public XmlParserSimple(Class<T> clazz) {
        this.clazz = clazz;
    }


    @Override
    public T parse(byte[] data) {
        try {
            if(serializer == null){
                serializer = Class.forName("org.simpleframework.xml.core.Persister").newInstance();
                method = serializer.getClass().getDeclaredMethod("read", java.lang.Class.class, java.io.InputStream.class);
            }
            return  (T) method.invoke(serializer, clazz, new ByteArrayInputStream(data));

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

//        Serializer serializer = new Persister();
//        try {
//            return serializer.read(clazz, new ByteArrayInputStream(data));
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return null;
    }


}
