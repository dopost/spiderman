package com.panda.http.spiderman.utils;

/**
 * Created by steven on 2016/10/12.
 */

public class ObjectUtils {

    public static <T> T checkNotNull(T object, String message) {
        if (object == null) {
            throw new NullPointerException(message);
        }
        return object;
    }
}
