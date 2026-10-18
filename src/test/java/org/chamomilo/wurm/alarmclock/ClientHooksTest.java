package org.chamomilo.wurm.alarmclock;

import javassist.*;
import javassist.expr.*;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class ClientHooksTest {
    @Test public void productionHooksCompileAgainstPinnedClientAndRetainCallbacks() throws Exception {
        new AlarmClockMod().preInit();
        ClassPool pool=HookManager.getInstance().getClassPool();
        verify(pool,"com.wurmonline.client.renderer.gui.HeadsUpDisplay","init","(II)V","onHudReady");
        verify(pool,"com.wurmonline.client.renderer.gui.HeadsUpDisplay","gameTick","()V","onHudTick");
        verify(pool,"com.wurmonline.client.game.PlayerObj","setHunger","(FB)V","foodUpdated");
        verify(pool,"com.wurmonline.client.game.PlayerObj","setHunger","(FBBBBB)V","foodUpdated");
        verify(pool,"com.wurmonline.client.game.PlayerObj","setThirst","(F)V","waterUpdated");
        verify(pool,"com.wurmonline.client.game.SkillLogicSet","addSkill","(Lcom/wurmonline/client/game/SkillLogic;)V","added",FavorReadings.class.getName());
        verify(pool,"com.wurmonline.client.game.SkillLogicSet","updateSkill","(JFB)V","updated",FavorReadings.class.getName());
        verify(pool,"com.wurmonline.client.game.SkillLogicSet","clear","()V","cleared",FavorReadings.class.getName());
        verify(pool,"com.wurmonline.client.comm.SimpleServerConnectionClass","disconnect","(Ljava/lang/String;)V","connectionEnded");
        verify(pool,"com.wurmonline.client.comm.SimpleServerConnectionClass","disconnectAndConnectTo","(Ljava/lang/String;I)V","connectionEnded");
        verify(pool,"com.wurmonline.client.comm.ServerConnectionListenerClass","textMessage","(Ljava/lang/String;FFFLjava/lang/String;B)V","serverMessage");
        verify(pool,"com.wurmonline.client.comm.ServerConnectionListenerClass","textMessage","(Ljava/lang/String;Ljava/util/List;B)V","serverMessage");
        verify(pool,"com.wurmonline.client.console.WurmConsole","handleDevInput","(Ljava/lang/String;[Ljava/lang/String;)Z","command");
        verify(pool,"com.wurmonline.client.renderer.gui.Renderer","execute","(Ljava/lang/Object;)V","isReminderOverlay","com.wurmonline.client.renderer.gui.AlarmClockBridge");
        verify(pool,"com.wurmonline.client.renderer.gui.Renderer","execute","(Ljava/lang/Object;)V","renderReminderOverlays","com.wurmonline.client.renderer.gui.AlarmClockBridge");
        assertTrue(pool.get("com.wurmonline.client.renderer.gui.Renderer").toBytecode().length>0);
        // Emit the instrumented listener as well: the hooks access its private World field.
        assertTrue(pool.get("com.wurmonline.client.comm.ServerConnectionListenerClass").toBytecode().length>0);
        assertTrue(pool.get("com.wurmonline.client.game.SkillLogicSet").toBytecode().length>0);
    }
    private static void verify(ClassPool pool,String type,String method,String descriptor,String callback) throws Exception {
        verify(pool,type,method,descriptor,callback,AlarmClockMod.class.getName());
    }
    private static void verify(ClassPool pool,String type,String method,String descriptor,String callback,String callbackClass) throws Exception {
        Set<String> calls=new HashSet<String>();
        CtMethod target=method.equals("clear")?pool.get(type).getDeclaredMethod(method):pool.get(type).getMethod(method,descriptor);
        target.instrument(new ExprEditor(){
            public void edit(MethodCall call){
                try {if(call.getClassName().equals(callbackClass))calls.add(call.getMethodName());}
                catch(ClassCastException oldInvokeDynamic) { }
            }
        });
        assertTrue(type+"."+method+" missing "+callback,calls.contains(callback));
    }
}
