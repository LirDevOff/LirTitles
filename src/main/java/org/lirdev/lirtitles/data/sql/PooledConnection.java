package org.lirdev.lirtitles.data.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

final class PooledConnection implements InvocationHandler {
    private final Connection physical;
    private final ConnectionPool pool;
    private boolean released;

    private PooledConnection(Connection physical, ConnectionPool pool) {
        this.physical = physical;
        this.pool = pool;
    }

    static Connection wrap(Connection physical, ConnectionPool pool) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new PooledConnection(physical, pool));
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (isClose(method)) {
            release(false);
            return null;
        }
        try {
            return method.invoke(physical, args);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof SQLException) {
                release(true);
            }
            throw cause;
        }
    }

    private static boolean isClose(Method method) {
        return "close".equals(method.getName()) && method.getParameterCount() == 0;
    }

    private void release(boolean discard) {
        if (released) {
            return;
        }
        released = true;
        pool.release(physical, discard);
    }
}
