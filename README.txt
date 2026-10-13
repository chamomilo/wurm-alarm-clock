Chamomilo Alarm Clock 0.3.6
https://github.com/chamomilo/wurm-alarm-clock

Reminders for food, water, prayer, meditation and full Favor in Wurm Unlimited.
Click the draggable HUD clock to open the list. Alarms continue while it is closed.

INSTALLATION
1. Install the Wurm Unlimited client mod launcher.
2. Extract this ZIP's mods folder into the Wurm Unlimited client folder.
3. Restart the client. Open Alarm Clock from the HUD clock or the Main menu.

This ZIP contains exactly three files:
  README.txt
  mods/alarmclock.properties
  mods/alarmclock/alarmclock.jar

The shared Chamomilo updater 1.5.0, its common Settings editor and the Chamomilo
interface kit 0.4.11 are embedded in the JAR. No separate updater is required.

BUILT-IN ALARMS
Food: at 20% remaining, then every lower whole percentage through 5%.
  Eating rearms it; below 5% it stays silent.
Water: immediately below 50%, then every minute, including at 0%.
  Refill to 50% or higher to stop it. There is no lower cutoff.
Prayer: after 20 minutes, then every minute. Successful prayer restarts the
  countdown when Restart after praying is selected.
Meditation: after 30 minutes, then every minute. Successful meditation restarts
  the countdown when Restart after meditation is selected.
Favor: when Favor reaches Faith, then every minute while full. Spending Favor
  stops reminders; refilling rearms them.

Starting, cancelling or failing prayer/meditation does not reset their timers.
Restart starts a fresh countdown manually. New alarm creates custom timers or
additional Food, Water and Favor monitors. Timer repeat 0 means one reminder.
Water and Favor require positive repeat intervals. Custom alarms can be deleted.

REMINDER OPTIONS
Every alarm has four independent options:
  Event message: writes to Wurm's existing Event channel.
  On-screen message: large text with a three-second fade.
  Alarm clock animation: rings the HUD clock for three seconds.
  Chime: plays that alarm's sound. Test chime previews it immediately.

Prayer uses a clearer, stronger chime with three spaced notes and a soft fade.
Simultaneous chimes are queued. Choose a palette color or enter #RRGGBB on a
card or in Edit. It colors the messages, animated clock and card background.
Food, Water, Prayer and custom timers default to gold, Meditation to light blue,
and Favor to mint green.

Disable stops an alarm, clears visible reminders and cancels queued chimes.
The disabled state survives restarts and reconnects. Saving an editor opened
before disabling Food, Water or Favor keeps that monitor disabled.

EDITING AND COMMON SETTINGS
Edit changes an alarm's name, reminder text and relevant timing/threshold fields.
Water has Alert below (%) and Repeat (minutes), without a lower cutoff.

Open the shared Mods Registry and click Settings on the Alarm Clock card.
The common editor contains every alarm's enabled state, text, delivery options,
color and parameters, with hover help. Valid changes are saved and applied
immediately. Invalid input leaves the saved value intact.

The shared updater selects English, Brazilian Portuguese, German or Russian.
Names and messages you enter are kept as written.

HUD AND SAVED PREFERENCES
Drag the HUD clock to move it. HUD Settings can hide or show it.
Console commands:
  alarmclock      Open the list.
  alarmclockshow  Restore and show the HUD clock at its default position.

Settings and deadlines are saved in .chamomilo/alarmclock.properties under your
Windows user folder. Upgrades keep existing preferences. Older Water settings
migrate once to 50% / one minute, preserving their enabled state and message.

Alarms stay silent while disconnected and resume without a burst of missed
reminders. The mod does not perform game actions for the player.

Illustrated guide: https://github.com/chamomilo/wurm-alarm-clock
Releases: https://github.com/chamomilo/wurm-alarm-clock/releases
