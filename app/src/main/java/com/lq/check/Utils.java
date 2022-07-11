package com.lq.check;

public class Utils {

    public static Object getParamClass(String fullName) {
        switch (fullName) {
            case "int":
                return int.class;
            case "long":
                return long.class;
            case "short":
                return short.class;
            case "byte":
                return byte.class;
            case "boolean":
                return boolean.class;
            case "char":
                return char.class;
            case "float":
                return float.class;
            case "double":
                return double.class;
            default:
                try {
                    return Class.forName(fullName);
                } catch (Exception e) {
                    return null;
                }
        }
    }
}
