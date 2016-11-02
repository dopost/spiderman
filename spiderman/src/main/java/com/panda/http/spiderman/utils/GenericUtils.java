package com.panda.http.spiderman.utils;

import android.util.Log;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * Created by steven on 2016/10/13.
 */

public class GenericUtils {

    public static Type getClassGenricType(Object object) {
        Type[] types = object.getClass().getGenericInterfaces();
        Log.d(TAG, "types size: " + types.length);
        List<Type> needtypes = new ArrayList<>();

        for (Type paramType : types) {
            System.out.println("  " + paramType);
            // if Type is T
            if (paramType instanceof ParameterizedType) {
                Type[] parentypes = ((ParameterizedType) paramType).getActualTypeArguments();
                for (Type childtype : parentypes) {
                    needtypes.add(childtype);
                    if (childtype instanceof ParameterizedType) {
                        Type[] childtypes = ((ParameterizedType) childtype).getActualTypeArguments();
                        for (Type type : childtypes) {
                            needtypes.add(type);
                        }
                    }
                }
            }
        }

        if (needtypes.isEmpty()) {
            return Object.class;
        }
        final Type finalType = needtypes.get(0);
        return finalType;
    }
}
