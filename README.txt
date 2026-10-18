Chamomilo Alarm Clock 0.3.10
https://github.com/chamomilo/wurm-alarm-clock

Food, water, prayer, meditation and full-Favor reminders for Wurm Unlimited.
Open the list from the draggable HUD clock or Main menu. Alarms keep running
when the list is closed.

INSTALL OR UPDATE
1. Install the Wurm Unlimited client mod launcher.
2. Close the game. Extract this ZIP's mods folder into your client folder.
   Replace the existing Alarm Clock files when updating.
3. Start the client and open Alarm Clock from the HUD clock or Main menu.

Saved alarms and preferences are retained. This ZIP contains exactly:
  README.txt
  mods/alarmclock.properties
  mods/alarmclock/alarmclock.jar

The shared Chamomilo updater 1.5.0, common Settings editor and interface kit
0.4.11 are embedded in the JAR. No separate updater download is needed.
Illustrated guide: https://github.com/chamomilo/wurm-alarm-clock#readme

ALL ALARMS
All on and All off beside New alarm enable or disable every built-in and custom
alarm. All on keeps running timers on schedule and starts fresh countdowns for
disabled timers. All off clears active reminders and queued chimes immediately.
Changes are saved.

BUILT-IN REMINDERS
Food: at 20% remaining, then each lower whole percentage through 5%.
  Eating rearms it; below 5% it stays silent.
Water: immediately below 50%, then every minute, including at 0%.
  Refill to 50% or higher to stop it. There is no lower cutoff.
Prayer: after 20 minutes, then every minute. Successful prayer restarts the
  countdown when Restart after praying is selected.
Meditation: after 30 minutes, then every minute. Successful meditation restarts
  the countdown when Restart after meditation is selected.
Favor: when current Favor reaches Faith, then every minute while full.
  Spending Favor stops reminders; refilling rearms them.

Starting, cancelling or failing prayer/meditation leaves its timer running.
Restart starts a fresh countdown manually. New alarm adds custom timers or
additional Food, Water and Favor monitors. Timer repeat 0 means one reminder.
Water and Favor require positive repeat intervals. Built-in alarms can be
disabled; custom alarms can also be deleted.

REMINDER OPTIONS AND COLORS
Each alarm has four independent switches:
  Event message: writes to Wurm's Event channel.
  On-screen message: large text above windows/popups, fading over three seconds.
  Alarm clock animation: rings the HUD clock for three seconds.
  Chime: plays the alarm's sound. Test chime previews it immediately.

Simultaneous chimes are queued. Prayer uses three clear, spaced notes with a
soft fade. Choose a named palette color on a card or in Edit. It colors the
messages, animated clock and card tint. Defaults: Food gold, Water light blue,
Prayer pink, Meditation lavender, Favor mint green and custom timers orange.
Cards and editors show the color name and swatch. Shared Settings also accepts
custom #RRGGBB or R,G,B values.

Disable immediately stops an alarm, clears visible reminders and cancels queued
chimes. This choice survives restarts and reconnects. Saving an editor opened
before disabling Food, Water or Favor keeps that monitor disabled.

EDITING AND COMMON SETTINGS
Edit changes an alarm's name, message and relevant timing or threshold fields.
Water has Alert below (%) and Repeat (minutes), without a lower cutoff.

Open the shared Mods Registry and click Settings on the Alarm Clock card to
edit all alarms in one window. Hover over fields for help. Valid changes save
and apply immediately; invalid input preserves the saved value.

The shared language selector supports English, Brazilian Portuguese, German
and Russian. Names and messages you enter are kept as written.

HUD AND SAVED DATA
Drag the HUD clock to move it. HUD Settings can hide or show it.
Console commands:
  alarmclock      Open the alarm list.
  alarmclockshow  Show the clock at its default position.

Alarms, preferences and deadlines are stored in .chamomilo/alarmclock.properties
under your Windows user folder. Upgrades keep existing preferences. Older Water
settings migrate once to 50% / one minute, keeping enabled state and message.
The updated palette replaces old standard colors once and preserves nonstandard
custom shades.

Alarms stay silent while disconnected and resume without a burst of missed
reminders. Alarm Clock provides reminders; game actions remain under your control.

IN THIS RELEASE
All on and All off next to New alarm control all built-in and custom alarms.
Running timers retain their deadlines; bulk changes are saved together.

Support: https://github.com/chamomilo/wurm-alarm-clock/issues
Releases: https://github.com/chamomilo/wurm-alarm-clock/releases
