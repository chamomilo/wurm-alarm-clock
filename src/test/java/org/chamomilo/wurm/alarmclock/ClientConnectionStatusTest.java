package org.chamomilo.wurm.alarmclock;

import com.wurmonline.client.comm.SimpleServerConnectionClass;
import java.lang.reflect.Field;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClientConnectionStatusTest {
    @Test public void staleLoggedInFlagDoesNotTreatDisconnectedClientAsOnline() throws Exception {
        Field unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        SimpleServerConnectionClass connection=(SimpleServerConnectionClass)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(SimpleServerConnectionClass.class);
        field("loggedIn").setBoolean(connection,true);field("disconnected").setBoolean(connection,false);
        assertTrue(ClientConnectionStatus.connected(connection));
        field("disconnected").setBoolean(connection,true);assertTrue(connection.isLoggedIn());
        assertFalse(ClientConnectionStatus.connected(connection));
        field("disconnected").setBoolean(connection,false);field("isConnecting").setBoolean(connection,true);
        assertFalse(ClientConnectionStatus.connected(connection));
    }
    private static Field field(String name) throws Exception {Field f=SimpleServerConnectionClass.class.getDeclaredField(name);f.setAccessible(true);return f;}
}
