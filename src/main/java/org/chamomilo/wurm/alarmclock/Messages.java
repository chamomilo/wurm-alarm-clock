package org.chamomilo.wurm.alarmclock;

import java.util.LinkedHashMap;
import java.util.Map;

/** Interface language comes from the shared updater; reminder text remains user-owned. */
public final class Messages {
    private static String language = "en";
    private static final Map<String, String[]> TEXT = new LinkedHashMap<String, String[]>();
    static {
        add("title", "Alarm Clock", "Despertador", "Wecker", "Будильник");
        add("launcher", "Alarm Clock icon", "Ícone do despertador", "Wecker-Symbol", "Ярлык будильника");
        add("new", "New alarm", "Novo alarme", "Neuer Alarm", "Новый будильник");
        add("allOn", "All on", "Ativar todos", "Alle an", "Включить все");
        add("allOff", "All off", "Desativar todos", "Alle aus", "Выключить все");
        add("allOnHint", "Enable every alarm. Already running timers keep their deadlines.", "Ativa todos os alarmes. Temporizadores em andamento mantêm seus prazos.", "Aktiviert alle Alarme. Laufende Timer behalten ihre Fristen.", "Включить все будильники. Уже работающие таймеры сохраняют время срабатывания.");
        add("allOffHint", "Disable every alarm and stop active reminders and queued chimes.", "Desativa todos os alarmes e interrompe lembretes ativos e sons na fila.", "Deaktiviert alle Alarme und beendet aktive Erinnerungen und wartende Klänge.", "Выключить все будильники, убрать активные напоминания и отменить звуки в очереди.");
        add("eventMessage","Event message","Mensagem no Event","Event-Meldung","Сообщение в Event");
        add("onScreenMessage","On-screen message","Mensagem na tela","Bildschirmmeldung","Сообщение на экране");
        add("alarmAnimation","Alarm clock animation","Animação do despertador","Weckeranimation","Анимация будильника");
        add("chime","Chime","Som","Klang","Звук");
        add("eventMessageHint","Write this reminder once to Wurm's Event channel.","Escreve este lembrete uma vez no canal Event.","Schreibt diese Erinnerung einmal in Wurms Event-Kanal.","Записывать напоминание в канал Event один раз.");
        add("onScreenMessageHint","Show large reminder text on the screen for three seconds.","Mostra o lembrete em letras grandes por três segundos.","Zeigt drei Sekunden lang großen Erinnerungstext.","Показывать крупный текст на экране в течение трёх секунд.");
        add("alarmAnimationHint","Show a ringing alarm clock for three seconds.","Mostra um despertador tocando por três segundos.","Zeigt drei Sekunden lang einen klingelnden Wecker.","Показывать анимацию звонящего будильника в течение трёх секунд.");
        add("chimeHint","Play this alarm's chime when it fires.","Toca o som deste alarme ao disparar.","Spielt beim Auslösen den Klang dieses Alarms.","Проигрывать звук этого будильника при срабатывании.");
        add("color","Color","Cor","Farbe","Цвет");
        add("colorHint","Choose a preset or enter #RRGGBB. Applies to Event, screen text, clock animation and the alarm card background.","Escolha uma cor ou digite #RRGGBB. Aplica ao Event, texto na tela, animação e fundo do alarme.","Farbe wählen oder #RRGGBB eingeben. Gilt für Event, Bildschirmtext, Animation und Kartenhintergrund.","Выберите цвет или введите #RRGGBB. Он применяется к Event, крупному тексту, анимации и фону карточки будильника.");
        add("gold","Gold","Dourado","Gold","Золотой");
        add("lightBlue","Light blue","Azul claro","Hellblau","Голубой");
        add("mint","Mint","Verde menta","Mintgrün","Мятный");
        add("pink","Pink","Rosa","Rosa","Розовый");
        add("orange","Orange","Laranja","Orange","Оранжевый");
        add("lavender","Lavender","Lilás","Lavendel","Сиреневый");
        add("white","White","Branco","Weiß","Белый");
        add("red","Red","Vermelho","Rot","Красный");
        add("customColor","Custom","Personalizada","Eigene Farbe","Свой цвет");
        add("sound", "Sound", "Som", "Klang", "Звук");
        add("animation", "Animation", "Animação", "Animation", "Анимация");
        add("soundHint", "Play a different gentle chime for each event. Test buttons let you learn the sounds.", "Toca um som suave diferente para cada evento. Use Ouvir para conhecer os sons.", "Spielt einen eigenen sanften Klang für jedes Ereignis. Mit Probehören lernst du die Klänge.", "Разные мягкие созвучия для событий. Кнопка «Послушать» поможет запомнить их.");
        add("animationHint", "Show a ringing alarm clock above the reminder for three seconds.", "Mostra um despertador tocando acima do lembrete por três segundos.", "Zeigt drei Sekunden lang einen klingelnden Wecker über der Erinnerung.", "Показывать звонящий будильник над напоминанием в течение трёх секунд.");
        add("testSound", "Test chime", "Ouvir", "Probehören", "Послушать");
        add("autoRestartPrayer", "Restart after praying", "Reiniciar após orar", "Nach dem Beten neu starten", "Перезапуск после молитвы");
        add("autoRestartPrayerHint", "Start a new timer after your character successfully finishes praying.", "Inicia um novo temporizador quando seu personagem termina de orar.", "Startet den Timer neu, sobald dein Charakter sein Gebet erfolgreich beendet.", "Начинать новый отсчёт после успешного завершения молитвы персонажа.");
        add("autoRestartMeditation", "Restart after meditation", "Reiniciar após meditar", "Nach Meditation neu starten", "Перезапуск после медитации");
        add("autoRestartMeditationHint", "Start a new timer after your character successfully finishes meditating.", "Inicia um novo temporizador quando seu personagem termina de meditar.", "Startet den Timer neu, sobald dein Charakter seine Meditation erfolgreich beendet.", "Начинать новый отсчёт после успешного завершения медитации персонажа.");
        add("intro", "Track your needs and time. Alarms keep running while this window is closed.", "Acompanhe suas necessidades e o tempo. Os alarmes continuam com a janela fechada.", "Bedürfnisse und Zeit verfolgen. Alarme laufen bei geschlossenem Fenster weiter.", "Следите за показателями и временем. Будильники работают и при закрытом окне.");
        add("food", "Food", "Comida", "Nahrung", "Еда");
        add("water", "Water", "Água", "Wasser", "Вода");
        add("prayer", "Prayer", "Oração", "Gebet", "Молитва");
        add("meditation", "Meditation", "Meditação", "Meditation", "Медитация");
        add("favor", "Favor", "Favor", "Favor", "Favor");
        add("full", "Full", "Cheio", "Voll", "Максимум");
        add("favorAtMax", "Alert when Favor reaches Faith", "Avisar quando Favor atingir Faith", "Warnung, wenn Favor Faith erreicht", "Напоминать при Favor = Faith");
        add("favorRepeatField", "Repeat (minutes)", "Repetir (minutos)", "Wiederholung (Minuten)", "Повтор (минуты)");
        add("favorHint", "Maximum Favor equals your current Faith. Reminders stop as soon as Favor is spent.", "Favor máximo equivale à sua Faith atual. Os lembretes param ao gastar Favor.", "Maximaler Favor entspricht deinem aktuellen Faith. Erinnerungen enden, sobald du Favor verbrauchst.", "Максимум Favor равен текущему Faith. Напоминания прекращаются после траты Favor.");
        add("timer", "Timer", "Temporizador", "Timer", "Таймер");
        add("edit", "Edit", "Editar", "Bearbeiten", "Изменить");
        add("disable", "Disable", "Desativar", "Deaktivieren", "Выключить");
        add("enable", "Enable", "Ativar", "Aktivieren", "Включить");
        add("restart", "Restart", "Reiniciar", "Neustart", "Перезапуск");
        add("delete", "Delete", "Excluir", "Löschen", "Удалить");
        add("off", "Disabled", "Desativado", "Deaktiviert", "Выключен");
        add("waiting", "Waiting for player data", "Aguardando dados do jogador", "Warte auf Spielerdaten", "Ожидание данных игрока");
        add("remaining", "left", "restante", "übrig", "осталось");
        add("due", "Time is up", "Tempo esgotado", "Zeit abgelaufen", "Время пришло");
        add("start", "Starts when you enter the game", "Começa ao entrar no jogo", "Startet beim Betreten des Spiels", "Запустится при входе в игру");
        add("threshold", "Alert at", "Avisar em", "Warnung bei", "Напоминать при");
        add("stop", "Stop below", "Parar abaixo de", "Stoppen unter", "Прекратить ниже");
        add("everyPercent", "every 1%", "a cada 1%", "bei jedem 1%", "каждый 1%");
        add("minutes", "min", "min", "Min.", "мин");
        add("repeat", "Repeat every", "Repetir a cada", "Wiederholen alle", "Повторять каждые");
        add("once", "One reminder", "Um lembrete", "Einmal erinnern", "Одно напоминание");
        add("name", "Name", "Nome", "Name", "Название");
        add("kind", "Type", "Tipo", "Typ", "Тип");
        add("delay", "Wait (minutes)", "Espera (minutos)", "Wartezeit (Minuten)", "Ожидание (минуты)");
        add("repeatField", "Repeat (minutes, 0 = once)", "Repetir (minutos, 0 = uma vez)", "Wiederholung (Minuten, 0 = einmal)", "Повтор (минуты, 0 = один раз)");
        add("thresholdField", "Threshold (%)", "Limite (%)", "Schwelle (%)", "Порог (%)");
        add("waterThresholdField", "Alert below (%)", "Avisar abaixo de (%)", "Warnung unter (%)", "Напоминать ниже (%)");
        add("alertBelow", "Alert below", "Avisar abaixo de", "Warnung unter", "Напоминать ниже");
        add("waterHint", "Repeat while water is below the threshold, including at 0%. {percent} inserts the current value.", "Repetir abaixo do limite, inclusive a 0%. {percent} insere o valor atual.", "Unter der Schwelle wiederholen, auch bei 0%. {percent} setzt den aktuellen Wert ein.", "Повторять ниже порога, включая 0%. {percent} — текущее значение.");
        add("stopField", "Stop below (%)", "Parar abaixo de (%)", "Stoppen unter (%)", "Прекратить ниже (%)");
        add("message", "Screen message", "Mensagem na tela", "Bildschirmmeldung", "Сообщение на экране");
        add("timerHint", "Saving starts a new timer. Repeat 0 means one reminder.", "Salvar inicia um novo temporizador. Repetir 0 gera um único lembrete.", "Speichern startet den Timer neu. Wiederholung 0 bedeutet eine Erinnerung.", "Сохранение запускает новый отсчёт. Повтор 0 — одно напоминание.");
        add("needsHint", "Each lower 1% triggers a reminder. {percent} inserts the current percentage.", "Cada queda de 1% gera um lembrete. {percent} insere a porcentagem atual.", "Bei jedem weiteren 1% Abfall erinnern. {percent} setzt den aktuellen Prozentwert ein.", "Напоминать при каждом снижении на 1%. {percent} — текущий процент.");
        add("save", "Save", "Salvar", "Speichern", "Сохранить");
        add("cancel", "Cancel", "Cancelar", "Abbrechen", "Отмена");
        add("invalid", "Check the name, message and numeric values.", "Verifique o nome, a mensagem e os valores numéricos.", "Name, Meldung und Zahlenwerte prüfen.", "Проверьте название, сообщение и числовые значения.");
        add("saveFailed", "Could not save settings. Check the client log.", "Não foi possível salvar. Veja o log do cliente.", "Einstellungen nicht gespeichert. Client-Protokoll prüfen.", "Не удалось сохранить настройки. Проверьте журнал клиента.");
        add("deleteTitle", "Delete alarm?", "Excluir alarme?", "Alarm löschen?", "Удалить будильник?");
        add("launcherHint", "Alarm Clock — click to open; drag to move", "Despertador — clique para abrir; arraste para mover", "Wecker — klicken zum Öffnen; ziehen zum Verschieben", "Будильник — нажмите для открытия; перетащите для перемещения");
    }
    private static void add(String key, String en, String pt, String de, String ru) { TEXT.put(key, new String[]{en, pt, de, ru}); }
    public static String language() { return language; }
    public static boolean select(String value) {
        if (!value.equals("en") && !value.equals("pt-BR") && !value.equals("de") && !value.equals("ru")) return false;
        language = value; return true;
    }
    public static String t(String key) {
        String[] row = TEXT.get(key);
        if (row == null) throw new IllegalArgumentException("Missing translation: " + key);
        return row[language.equals("pt-BR") ? 1 : language.equals("de") ? 2 : language.equals("ru") ? 3 : 0];
    }
    public static String name(Alarm alarm) {
        if (alarm.builtIn && alarm.name.equals(alarm.type().defaultName)) return t(alarm.type().nameKey);
        return alarm.name;
    }
}
