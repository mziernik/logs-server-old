package service;

import com.StrUtils;
import com.json.Escape;
import com.json.JSON;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import com.mlogger.Log;

public class LogUtils {

    public static void objectDetails(Object object) {

        StringBuilder sb = new StringBuilder();

        Class<? extends Object> cls = object.getClass();

        sb.append("package ").append(cls.getPackage().getName()).append(";\n\n");

        sb.append(Modifier.toString(cls.getModifiers()))
                .append(" class ").append(cls.getSimpleName()).append(" {\n");

        for (Field f : object.getClass().getDeclaredFields()) {

            String type = f.getType().getName();
            if (type.startsWith("java.lang."))
                type = type.substring("java.lang.".length());

            sb.append("    ").append(Modifier.toString(f.getModifiers()))
                    .append(" ").append(type).append(" ")
                    .append(f.getName());

            try {

                f.setAccessible(true);
                Object obj = f.get(object);

                if (obj == null)
                    sb.append(" = null;");
                else {
                    String val = Escape.escape(obj);

                    if (obj instanceof String || obj instanceof Number || obj instanceof Boolean)
                        sb.append(" = ").append(val).append(";");
                    else
                        if (obj instanceof Date)
                            sb.append(" = new Date(").append(val).append(");");
                        else
                            sb.append("; //").append(val);
                }

            } catch (Throwable e) {
            }

            sb.append("\n");

        }

        sb.append("}");

        Log.debug("Field", sb.toString());
    }

}
