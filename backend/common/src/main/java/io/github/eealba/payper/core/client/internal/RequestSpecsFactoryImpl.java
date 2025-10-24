package io.github.eealba.payper.core.client.internal;

import io.github.eealba.payper.core.client.RequestSpecsFactory;
import io.github.eealba.payper.core.client.Spec;
import io.github.eealba.payper.core.exceptions.PayperException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;

class RequestSpecsFactoryImpl extends RequestSpecsFactory {
    static final RequestSpecsFactory INSTANCE = new RequestSpecsFactoryImpl();

    @Override
    public <T1, R1, R2> T1 requestSpec(Spec<T1, R1, R2> spec) {
        RequestSpecImpl<T1, ?, R1, R2> obj = new RequestSpecImpl<>(spec);
        Object proxyObj = newProxyInstance(obj, spec);
        return castObject(proxyObj);
    }

    @SuppressWarnings("unchecked")
    <T1> T1 castObject(Object obj) {
        return (T1) obj;
    }

    private static Object newProxyInstance(Object obj, Spec<?, ?, ?> spec) {
        return Proxy.newProxyInstance(
                spec.clazz().getClassLoader(),
                new Class[]{spec.clazz()},
                new ProxyImpl(obj, spec.alias().orElse(Collections.emptyMap()))
        );
    }

    private static class ProxyImpl implements java.lang.reflect.InvocationHandler {
        private Object obj;
        private Map<String, String> alias;
        ProxyImpl(Object obj, Map<String, String> alias) {
            this.obj = obj;
            this.alias = alias;
        }

        public Object obj() {
            return obj;
        }

        public void obj(Object obj) {
            this.obj = obj;
        }

        public Map<String, String> alias() {
            return alias;
        }

        public void alias(Map<String, String> alias) {
            this.alias = alias;
        }

        @Override
        public Object invoke(Object proxy, Method m, Object[] args)
                throws Throwable {
            try {
                return invoke2(proxy, m, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            } catch (Exception e) {
                throw new PayperException("unexpected invocation exception: " + e.getMessage(), e);
            }
        }

        private Object invoke2(Object proxy, Method m, Object[] args) throws NoSuchMethodException,
                IllegalAccessException, InvocationTargetException {
            Object result;
            for (Map.Entry<String, String> entry : alias.entrySet()) {
                if (entry.getKey().equals(m.getName())) {
                    String[] values = entry.getValue().split(",");
                    if (values.length == 1) {
                        m = getMethod(obj, values[0], m.getParameterTypes());
                    } else {
                        args = getArguments(args, values);
                        Class<?>[] types = new Class[args.length];
                        for (int i = 0; i < args.length; i++) {
                            types[i] = args[i].getClass();
                        }
                        m = getMethod(obj, values[0], types);
                    }
                }
            }
            result = m.invoke(obj, args);
            if (result == obj) {
                return proxy;
            }
            return result;
        }

        private Method getMethod(Object obj, String name, Class<?>[] types) throws NoSuchMethodException {
            try {
                return obj.getClass().getMethod(name, types);
            }catch (NoSuchMethodException e){
                if (types.length == 2 && types[0] == String.class) {
                    // Special case for methods with String and Object parameters
                    return obj.getClass().getMethod(name, String.class, Object.class);
                }
                throw e;
            }
        }

        private static Object[] getArguments(Object[] args, String[] values) {
            Object[] params = new Object[args.length + 1];
            System.arraycopy(args, 0, params, 1, args.length);
            params[0] = values[1];
            args = params;
            return args;
        }
    }
}