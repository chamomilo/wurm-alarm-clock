<img src="branding/alarmclock-logo-128.png" width="96" height="96" align="right" alt="Alarm Clock logo">

# Alarm Clock

Food, water, prayer, meditation and Favor reminders for Wurm Unlimited. A small clock on the HUD opens the alarm list; reminders keep running when the window is closed.

**[Download the latest release](https://github.com/chamomilo/wurm-alarm-clock/releases/latest)** · [Report an issue](https://github.com/chamomilo/wurm-alarm-clock/issues)

![Alarm list with colored cards, sound and animation controls](images/alarms.png)

## Install

1. Make sure the Wurm Unlimited client mod launcher is installed.
2. Download **alarmclock-0.3.3.zip** from [Releases](https://github.com/chamomilo/wurm-alarm-clock/releases/latest).
3. Extract the ZIP's `mods` folder into the client folder, then restart the client.

The ZIP contains exactly these three files:

```text
README.txt
mods/alarmclock.properties
mods/alarmclock/alarmclock.jar
```

The shared Chamomilo updater **1.5.0** and interface kit **0.4.11** are already inside the JAR. No separate updater installation is needed. Pictures, source code and tests remain in this repository.

## Alarms

| Alarm | Default reminder | How it stops or resets |
| --- | --- | --- |
| Food | At 20% remaining, then at every lower whole percentage through 5% | Eating rearms it; below 5% it stays silent |
| Water | Immediately below 50%, then every minute, including at 0% | Refilling to 50% or above stops it |
| Prayer | After 20 minutes, then every minute | A successful prayer restarts the timer when its checkbox is enabled |
| Meditation | After 30 minutes, then every minute | A successful meditation restarts the timer when its checkbox is enabled |
| Favor | When Favor reaches current Faith, then every minute while full | Spending Favor stops it; refilling rearms it |

**New alarm** also creates custom timers and additional Food, Water or Favor alarms. Timer repeat `0` means one reminder. Water and Favor use positive repeat intervals.

Water has a configurable **Alert below (%)** threshold and **Repeat (minutes)** interval, with no lower cutoff. Upgrading older settings changes the built-in Water alarm to 50% / one minute once, preserving its enabled state and message. Later edits are retained.

![Water editor with a 50 percent threshold and one-minute repeat](images/water-editor.png)

Prayer and meditation restart only after the character successfully completes the activity. Starting, cancelling or failing it does not restart the timer. Each timer also has a manual **Restart** button.

## Reminders

Alerts use large screen text with a three-second fade and are copied once to Wurm's existing **Event** channel in the same color. Food, Water, Prayer and custom timers use gold; Meditation uses light blue; Favor uses mint green.

![Mint-green full-Favor reminder](images/favor-reminder.png)

**Sound** and **Animation** are independent switches. Switching them off leaves screen text and Event messages active. **Test chime** previews the selected alarm's sound. Reminder overlays let mouse input reach the game behind them.

Use **Disable** on an alarm's card, or turn off its **Enabled** setting, to stop its reminders. This removes its visible text and animation and cancels queued chimes. The choice survives restarts and reconnects. Saving an already-open Food, Water or Favor editor keeps that monitor disabled; saving or enabling a timer starts a fresh countdown.

## Controls and settings

Click the HUD clock to open the list; drag it to move it. The Main menu opens Alarm Clock, and HUD Settings can show or hide its clock icon.

- **New alarm** creates an alarm; **Edit** changes its message and relevant parameters.
- **Disable / Enable** switches an alarm off or on.
- **Restart** starts a fresh timer countdown.
- Custom alarms can be deleted; built-in alarms stay in the list.
- Console `alarmclock` opens the list. `alarmclockshow` restores the HUD clock to its default position.

The shared Mods Registry's **Settings** button opens presentation options and every alarm's parameters, with hover help. Valid changes are saved and applied immediately. The shared updater controls the interface language: English, Brazilian Portuguese, German or Russian. Editable names and reminder messages are kept as entered.

Settings and timer deadlines are stored in `.chamomilo/alarmclock.properties` under the user's home folder, outside the game installation. Alarms stay silent while disconnected and resume without a burst of missed reminders. The mod does not eat, drink, pray, meditate or cast spells for the player.
