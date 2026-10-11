Chamomilo Alarm Clock 0.2.6
https://github.com/chamomilo/wurm-alarm-clock

Installation
Download alarmclock-0.2.6.zip from GitHub Releases. Extract its mods folder into
the Wurm Unlimited client folder that already has the Wurm client mod launcher.
Restart the client. The shared Chamomilo updater and interface kit are embedded;
no separate updater installation is required.

HUD and controls
The small alarm clock is visible by default on the left side of the HUD.
Click it to open the alarm list; drag it to move. The Main menu also opens
Alarm Clock. HUD Settings can show or hide the icon. Console commands:
  alarmclock      Open the list.
  alarmclockshow  Restore and show the icon at its default position.
Reminders continue while the list is closed. The toolbar contains New alarm,
Sound and Animation. Manual Restart remains on each timer's own card.

Default alarms
- Food: reminds at 20% remaining, then at each lower whole percentage through
  5% inclusive. Message: Eat, you have {percent}% food left.
- Water: the same independent rule. Message: Drink, you have {percent}% water left.
- Prayer: waits 20 minutes, then reminds every minute. Successful prayer
  automatically restarts the timer; its card has a persistent restart checkbox.
- Meditation: waits 30 minutes, then reminds every minute in light blue.
  Successful meditation automatically restarts its own timer and checkbox.
- Favor: reminds immediately when Favor reaches the character's current Faith,
  then every minute while full. Spending Favor stops reminders; refilling starts
  them again. Message: Favor is full. It's time to cast a spell.
  Favor has a mint-green message and displays the current value / maximum.

Food, Water, Prayer, Meditation and Favor have distinct colored card backgrounds
and gentle chimes. Custom timers have violet cards. Test chime lets you learn
each sound. A ringing clock animates above the large native screen message for
three seconds. Sound and Animation can be enabled independently. Simultaneous
chimes are queued so they remain distinguishable.

New alarm creates a Timer, Food, Water or Favor alarm. Edit only shows parameters
used by the selected type: Wait / Repeat for timers, Threshold / Stop below for
Food and Water, and Repeat for Favor. Repeat 0 creates a one-shot timer; Favor
requires a positive repeat interval. Saving or enabling a timer restarts it.
Disabled alarms stay silent. Built-in alarms cannot be deleted.

Automatic prayer and meditation restart uses the character's successful inbound
server Event message. Starting, cancelling or failing an activity, chat and
another player's activity do not restart timers. Turn off the relevant checkbox
to use manual restart instead. Disabled alarms are never re-enabled automatically.

Languages and settings
The interface supports English, Brazilian Portuguese, German and Russian through
the shared updater's language setting. Reminder text is English by default and
can be edited. User names and messages are preserved across language changes.
Settings are saved in the user's .chamomilo/alarmclock.properties outside the
game folder. Timer deadlines survive restarts. No reminders are sent while
disconnected; overdue timers resume without a burst of missed reminders.
The mod does not pray, meditate, eat, drink or cast spells automatically.

Updates
Alarm Clock participates in the shared Chamomilo updater and appears last in
its catalogue with the medieval clock logo. Its release asset is
alarmclock-{version}.zip. Updates are installed manually from the downloaded ZIP.

Building from source
Use JDK 8 and Gradle 5.3.1 through the wrapper in the canonical Chamomilo updater
reference. The optional updater-reference ZIP in this release supplies that
reference; extract it outside this repository. Supply the required native
client/modlauncher libraries in the reference's libs folder. Proprietary client
libraries are not distributed. Set JAVA_HOME and optionally GRADLE_USER_HOME.
Run build.ps1 -UpdaterReference <reference-directory> -JavaFxJar <jfxrt.jar>.
Optional parameters: -JavaHome <JDK-directory>, -ClientLibDir <libraries-directory>,
and -UiKitReference <interface-kit-directory> for its additional embedding check.
The build tests alarms, settings compatibility, native hooks and interface
layout; it verifies the complete embedded updater and produces the installable
ZIP in dist. In-game rendering and live server sessions require manual testing.
