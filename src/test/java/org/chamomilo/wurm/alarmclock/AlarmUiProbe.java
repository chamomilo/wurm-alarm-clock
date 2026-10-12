package org.chamomilo.wurm.alarmclock;

import javassist.*;

/** Runs real Wurm layout/input code, replacing only fonts, GPU drawing and HUD startup. */
public final class AlarmUiProbe {
    public static void main(String[] args) throws Throwable {
        ClassPool pool = new ClassPool(true);
        String gui = "com.wurmonline.client.renderer.gui.";
        CtClass fonts = pool.get(gui + "text.TextFont");
        fonts.getClassInitializer().setBody("{}");
        fonts.getDeclaredMethod("getText", new CtClass[]{pool.get("java.lang.String")})
                .setBody("{ return new com.wurmonline.client.renderer.gui.AlarmClockLayoutProbe.ProbeFont($1); }");
        pool.get(gui + "text.AlarmClockSystemFont").getDeclaredMethod("create")
                .setBody("{ return new com.wurmonline.client.renderer.gui.AlarmClockLayoutProbe.ProbeFont($1, false); }");
        CtClass options = pool.get("com.wurmonline.client.options.Options");
        options.getClassInitializer().setBody("{}");
        CtClass component = pool.get(gui + "WurmComponent");
        component.getClassInitializer().setBody("{}");
        component.getDeclaredMethod("fillRect").setBody("{ " + gui
                + "AlarmClockLayoutProbe.rect($2,$3,$4,$5,$6,$7,$8,$9); }");
        CtClass hud = pool.get(gui + "HeadsUpDisplay");
        hud.getClassInitializer().setBody("{ scissor = new com.wurmonline.client.renderer.backend.ScissorControl(); }");
        CtClass scissor = pool.get("com.wurmonline.client.renderer.backend.ScissorControl");
        scissor.getDeclaredMethod("pushClip").setBody("{ return " + gui + "AlarmClockLayoutProbe.clip($1,$2,$3,$4); }");
        scissor.getDeclaredMethod("popClip").setBody("{ " + gui + "AlarmClockLayoutProbe.unclip(); }");
        CtClass canvas = pool.get(gui + "ChamomiloUiV1Canvas");
        canvas.getDeclaredMethod("fill").setBody("{ " + gui
                + "AlarmClockLayoutProbe.rect($1.red,$1.green,$1.blue,$2,$3,$4,$5,$6); }");
        canvas.getDeclaredMethod("texture").setBody("{ return " + gui
                + "AlarmClockLayoutProbe.texture($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11); }");
        pool.get(gui + "text.ChamomiloUiV1Fonts").getDeclaredMethod("caption",
                new CtClass[]{CtClass.intType,CtClass.booleanType,pool.get("org.chamomilo.wurm.ui.v1.UiDensity")})
                .setBody("{ return " + gui + "AlarmClockLayoutProbe.captionFont($1,$2,$3); }");
        CtClass renderer = pool.get(gui + "Renderer");
        if (renderer.getClassInitializer() != null) renderer.getClassInitializer().setBody("{}");
        for (CtMethod method : renderer.getDeclaredMethods("texturedQuadAlphaBlend"))
            if (method.getParameterTypes().length == 14)
                method.setBody("{}");
        pool.get(gui + "WButton").getDeclaredMethod("renderComponent")
                .setBody("{ " + gui + "AlarmClockLayoutProbe.button(this); }");
        // Keep the real native editor state/input; replace its GPU/Options-dependent drawing only.
        pool.get(gui + "WurmInputField").getDeclaredMethod("renderComponent")
                .setBody("{ text.moveTo(x,y+text.getAscent()); text.paint($1,getText(),1f,1f,1f,1f); }");
        // The SDK replaces native dropdown drawing; retain its input/layout constructor,
        // substituting only the unused GPU arrow-buffer allocation in this headless probe.
        for (CtConstructor constructor : pool.get(gui + "WurmDropDown").getDeclaredConstructors())
            constructor.instrument(new javassist.expr.ExprEditor() {
                @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                    if (!call.getClassName().equals("com.wurmonline.client.renderer.backend.VertexBuffer")) return;
                    String method = call.getMethodName();
                    if (method.equals("create")) call.replace("{ $_ = null; }");
                    else if (method.equals("lock")) call.replace("{ $_ = java.nio.FloatBuffer.allocate(64); }");
                    else if (method.equals("unlock")) call.replace("{}");
                }
            });
        pool.get("org.chamomilo.wurm.update.SharedUpdateCoordinator").getDeclaredMethod("startOnce")
                .setBody("{ return false; }"); // Lifecycle checks must not launch a real online catalogue lookup.
        for (String menu : new String[]{"MainMenu", "HudSettings"})
        pool.get(gui + menu).getDeclaredMethod("registerComponent").instrument(new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.FieldAccess field) throws CannotCompileException {
                if (field.isReader() && field.getClassName().equals("com.wurmonline.client.options.Options")
                        && field.getFieldName().equals("guiSkins"))
                    field.replace("{ $_ = " + gui + "AlarmClockLayoutProbe.guiSkin(); }");
            }
            @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals("com.wurmonline.client.resources.textures.ResourceTextureLoader")
                        && call.getMethodName().equals("getTexture")) call.replace("{ $_ = null; }");
            }
        });
        // Preserve the real HUD list/menu/toggle implementation; omit unrelated game/quickbar state.
        javassist.expr.ExprEditor hudState = new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                String owner;
                String method;
                try {
                    owner = call.getClassName();
                    method = call.getMethodName();
                } catch (ClassCastException invokedynamic) {
                    // The bundled Javassist predates the client's Java 8 lambda call sites.
                    return;
                }
                if (owner.equals("com.wurmonline.client.options.BooleanOption")
                        && method.equals("value")) call.replace("{ $_ = false; }");
                if (owner.equals(gui + "PlayerStateButtons") && method.equals("setEnabled")) call.replace("{}");
            }
        };
        hud.getDeclaredMethod("addComponent").instrument(hudState);
        hud.getDeclaredMethod("removeComponent").instrument(hudState);
        // Bypass only the live connection/free-look getters. Keep native hit
        // testing, focus assignment and press dispatch intact for crash regression.
        hud.getDeclaredMethod("mousePressed").instrument(new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                String owner = call.getClassName(), method = call.getMethodName();
                if (owner.equals("com.wurmonline.client.game.World") && method.equals("getClient")
                        || owner.equals("com.wurmonline.client.WurmClientBase") && method.equals("getEventHandler"))
                    call.replace("{ $_ = null; }");
                else if (owner.equals("com.wurmonline.client.WurmEventHandler") && method.equals("getIsFreeLookMode"))
                    call.replace("{ $_ = false; }");
            }
        });
        // Freeze only render-time countdown text while comparing incoming alpha values.
        pool.get(gui + "AlarmClockWindow$Card").getDeclaredMethod("renderComponent").instrument(new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                if (call.getClassName().equals("java.lang.System") && call.getMethodName().equals("currentTimeMillis"))
                    call.replace("{ $_ = " + gui + "AlarmClockLayoutProbe.previewTime(); }");
            }
        });
        pool.get(gui + "AlarmClockAlert").getDeclaredMethod("renderComponent").instrument(new javassist.expr.ExprEditor() {
            @Override public void edit(javassist.expr.MethodCall call) throws CannotCompileException {
                if(call.getClassName().equals("java.lang.System")&&call.getMethodName().equals("currentTimeMillis"))
                    call.replace("{ $_ = " + gui + "AlarmClockLayoutProbe.previewTime(); }");
            }
        });
        Loader loader = new Loader(pool);
        loader.run(gui + "AlarmClockLayoutProbe", args);
    }
}
