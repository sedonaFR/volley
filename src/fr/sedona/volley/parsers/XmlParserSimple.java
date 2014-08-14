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

package fr.sedona.volley.parsers;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
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
                method = serializer.getClass().getDeclaredMethod("read", java.lang.Class.class, java.io.InputStream.class, boolean.class);
            }
            return  (T) method.invoke(serializer, clazz, new ByteArrayInputStream(data), false);

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
