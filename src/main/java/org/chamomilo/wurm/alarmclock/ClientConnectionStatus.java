package org.chamomilo.wurm.alarmclock;

import com.wurmonline.client.comm.SimpleServerConnectionClass;
import java.lang.reflect.Field;

/** Wurm's isLoggedIn() retains its login flag after disconnect(); check the socket lifecycle too. */
public final class ClientConnectionStatus {
    private static Field disconnected;
    private ClientConnectionStatus() { }
    public static boolean connected(SimpleServerConnectionClass connection) {
        if (connection == null || !connection.isLoggedIn()) return false;
        try {
            // Resolve after HUD readiness; do not load/freeze the native class during preInit.
            if (disconnected == null) {
                disconnected = SimpleServerConnectionClass.class.getDeclaredField("disconnected");
                disconnected.setAccessible(true);
            }
            return !disconnected.getBoolean(connection);
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot read Wurm connection status", failure); }
    }
}
