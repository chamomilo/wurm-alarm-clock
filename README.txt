Chamomilo Alarm Clock 0.3.3
https://github.com/chamomilo/wurm-alarm-clock

Food, water, prayer, meditation and Favor reminders for Wurm Unlimited.
Alarms keep running when the list window is closed.

INSTALLATION
1. Install the Wurm Unlimited client mod launcher if it is not already installed.
2. Extract this ZIP's mods folder into the Wurm Unlimited client folder.
3. Restart the client.

This ZIP contains exactly three files:
  README.txt
  mods/alarmclock.properties
  mods/alarmclock/alarmclock.jar

Chamomilo updater 1.5.0 and interface kit 0.4.11 are embedded in the JAR.
No separate updater installation is required.

DEFAULT ALARMS
Food: at 20% remaining, then every lower whole percentage through 5%.
  Eating rearms the monitor; below 5% it stays silent.
Water: immediately below 50%, then every minute, including at 0%.
  Refilling to 50% or above stops reminders. There is no lower cutoff.
Prayer: after 20 minutes, then every minute. A successful prayer restarts
  the timer when Restart after praying is enabled.
Meditation: after 30 minutes, then every minute. A successful meditation
  restarts the timer when Restart after meditation is enabled.
Favor: when Favor reaches current Faith, then every minute while full.
  Spending Favor stops reminders; refilling rearms them.

New alarm creates custom timers or additional Food, Water and Favor alarms.
Timer repeat 0 means one reminder. Water and Favor require positive repeats.
Water has Alert below (%) and Repeat (minutes), without a Stop below setting.
Older built-in Water settings migrate once to 50% / one minute, preserving
its enabled state and message. Subsequent edits are retained.
Prayer and meditation restart only after successful completion; starting,
cancelling or failing the activity does not restart the timer.

REMINDERS
Every alarm shows large screen text with a three-second fade and also writes
one copy to the existing shared Event channel in the same color.
Food, Water, Prayer and custom timers use gold, Meditation light blue,
and Favor mint green. No separate Event tab is created.
Sound and Animation are independent switches; screen and Event text remain
active when either is off. Test chime previews the selected alarm's sound.
Reminder overlays let mouse input reach the game behind them.

Disable on the card, or Enabled off in Settings, stops that alarm's reminders,
removes its visible text and animation, and cancels queued chimes.
The disabled choice survives restarts and reconnects. Saving an already-open
Food, Water or Favor editor keeps that monitor disabled. Saving or enabling
a timer starts a fresh countdown. Other alarms keep running.

CONTROLS AND SETTINGS
Click the HUD clock to open the list; drag it to move it. The Main menu opens
Alarm Clock. HUD Settings can show or hide the clock icon.
New alarm creates an alarm. Edit changes its message and relevant parameters.
Disable / Enable turns an alarm off or on. Restart starts a fresh timer.
Custom alarms can be deleted; built-in alarms stay in the list.
Console commands:
  alarmclock      Open the list.
  alarmclockshow  Restore and show the HUD clock at its default position.

Settings in the shared Mods Registry opens presentation options and every
alarm's parameters, with hover help. Valid changes are saved and applied
immediately. The updater selects the interface language: English, Brazilian
Portuguese, German or Russian. Editable names and messages are kept as entered.
Settings and deadlines are saved in .chamomilo/alarmclock.properties under the
user's home folder, outside the game installation.
Alarms stay silent while disconnected and resume without a burst of missed
reminders. The mod does not perform game actions for the player.

Illustrated guide: https://github.com/chamomilo/wurm-alarm-clock
Latest release: https://github.com/chamomilo/wurm-alarm-clock/releases/latest
