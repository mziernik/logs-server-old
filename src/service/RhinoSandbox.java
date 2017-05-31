package service;

import java.io.*;
import java.util.*;
import org.mozilla.javascript.*;

/**
 *
 * @author milosz
 */
public class RhinoSandbox implements Closeable {

    public final org.mozilla.javascript.Context context;
    public final NativeObject scope;
    public final Set<Class> classWhiteList = new HashSet<>();
    private final SandboxContextFactory contextFactory = new SandboxContextFactory();

    public final void init() {
        if (!ContextFactory.hasExplicitGlobal())
            ContextFactory.initGlobal(contextFactory);
    }

    public RhinoSandbox() {
        context = org.mozilla.javascript.Context.enter();
        Scriptable prototype = context.initStandardObjects();
        Scriptable topLevel = new ImporterTopLevel(context);
        prototype.setParentScope(topLevel);
        scope = (NativeObject) context.newObject(prototype);
        scope.setPrototype(prototype);
    }

    public void put(String name, Object object) {
        put(null, name, object);
    }

    public void put(Scriptable parent, String name, Object object) {
        if (object != null)
            classWhiteList.add(object.getClass());
        Scriptable scr = parent != null ? parent : scope;
        ScriptableObject.putProperty(scr, name,
                object == null ? null
                : org.mozilla.javascript.Context.javaToJS(object, scr));
    }

    public Object callMethod(String name, Object... params) {
        return callMethod(null, name, params);
    }

    public Object callMethod(Scriptable parent, String name, Object... params) {
        Object obj = ScriptableObject.callMethod(parent != null ? parent : scope, name, params);

        if (obj instanceof NativeJavaObject)
            return ((NativeJavaObject) obj).unwrap();
        return obj;
    }

    public Object evaluate(String name, String code) {
        return evaluate(null, name, code);
    }

    public Object evaluate(Scriptable parent, String name, String code) {
        Object obj = context.evaluateString(parent != null ? parent : scope, code, name, 1, null);
        if (obj instanceof NativeJavaObject)
            return ((NativeJavaObject) obj).unwrap();
        return obj;
    }

    public Object get(String name) {
        return get(null, name);
    }

    public Object get(Scriptable parent, String name) {
        Object obj = ScriptableObject.getProperty(parent != null ? parent : scope, name);
        if (obj instanceof NativeJavaObject)
            return ((NativeJavaObject) obj).unwrap();
        return obj;
    }

    @Override
    public void close() throws IOException {
        org.mozilla.javascript.Context.exit();
    }

    public class SandboxObject extends NativeJavaObject {

        //    private final Object javaObject;
        //    private final Class staticType;
        // private final Scriptable scope;
        public SandboxObject(Scriptable scope, Object javaObject, Class staticType) {
            super(scope, javaObject, staticType);
        }

        @Override
        public Object get(String name, Scriptable start) {
            return super.get(name, start);
        }
    }

    public class SandboxClass extends NativeJavaClass {

        public SandboxClass(Scriptable scope, Class<?> javaClass) {
            super(scope, javaClass);
            boolean hasPermission = false;
            for (Class cls : classWhiteList)
                if (javaClass == cls) {
                    hasPermission = true;
                    break;
                }
            if (!hasPermission)
                throw new SecurityException("Odmowa dostępu do klasy " + javaClass.getName());

        }
    }

    public class SandboxWrapFactory extends WrapFactory {

        @Override
        public Scriptable wrapJavaClass(org.mozilla.javascript.Context cx, Scriptable scope, Class javaClass) {
            return new SandboxClass(scope, javaClass);
        }

        @Override
        public Scriptable wrapAsJavaObject(org.mozilla.javascript.Context cx,
                Scriptable scope, Object javaObject, Class staticType) {
            return new SandboxObject(scope, javaObject, staticType);
        }

    }

    public class SandboxContextFactory extends ContextFactory {

        @Override
        protected org.mozilla.javascript.Context makeContext() {
            org.mozilla.javascript.Context cx = super.makeContext();
            cx.setWrapFactory(new SandboxWrapFactory());
            return cx;
        }
    }

}
