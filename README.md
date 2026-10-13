<img src="branding/alarmclock-logo-128.png" width="96" height="96" align="right" alt="Alarm Clock logo">

# Alarm Clock

Reminders for food, water, prayer, meditation and full Favor in Wurm Unlimited. A draggable clock on the HUD opens the alarm list. Your alarms keep running when the list is closed.

**[Download Alarm Clock 0.3.6](https://github.com/chamomilo/wurm-alarm-clock/releases/download/v0.3.6/alarmclock-0.3.6.zip)** · [All releases](https://github.com/chamomilo/wurm-alarm-clock/releases) · [Report an issue](https://github.com/chamomilo/wurm-alarm-clock/issues)

![Alarm list with separate reminder controls and colors](images/alarms.png)

## Installation

1. Install the Wurm Unlimited client mod launcher.
2. Download `alarmclock-0.3.6.zip` and extract its `mods` folder into your Wurm Unlimited client folder.
3. Restart the client. Click the HUD clock, or choose **Alarm Clock** from the Main menu.

The installable ZIP contains exactly three files:

```text
README.txt
mods/alarmclock.properties
mods/alarmclock/alarmclock.jar
```

The shared Chamomilo updater **1.5.0**, its common Settings editor, and the Chamomilo interface kit **0.4.11** are embedded in the JAR. No separate updater installation is required. Screenshots and source files are kept in this repository.

## Built-in alarms

| Alarm | Default behavior | Reset or stop |
| --- | --- | --- |
| Food | Remind at 20% remaining, then at every lower whole percentage down to 5% | Eating rearms the monitor; below 5% it stays silent |
| Water | Remind immediately below 50%, then every minute, including at 0% | Refill to 50% or higher |
| Prayer | Remind after 20 minutes, then every minute | A successful prayer restarts the timer when **Restart after praying** is selected |
| Meditation | Remind after 30 minutes, then every minute | A successful meditation restarts the timer when **Restart after meditation** is selected |
| Favor | Remind when current Favor reaches Faith, then every minute while full | Spending Favor stops the reminder; refilling rearms it |

Prayer and meditation timers restart after successful completion. Starting, cancelling or failing the activity leaves the timer running. **Restart** starts a fresh countdown manually.

Use **New alarm** to add custom timers or additional Food, Water and Favor monitors. A timer's repeat interval can be `0` for one reminder. Water and Favor require a positive repeat interval. Built-in alarms can be disabled; custom alarms can also be deleted.

## Choose how each alarm reminds you

Each alarm has four independent options:

- **Event message** writes to Wurm's existing Event channel.
- **On-screen message** shows large text with a three-second fade.
- **Alarm clock animation** rings the HUD clock for three seconds.
- **Chime** plays that alarm's sound. **Test chime** lets you hear it immediately.

Prayer has a clearer, stronger chime with three spaced notes and a soft fade. Sounds are queued so simultaneous alarms remain recognizable.

Choose a palette color or enter `#RRGGBB` on the card or in **Edit**. The color applies to the Event message, on-screen text, animated clock and card tint. Food, Water, Prayer and custom timers default to gold, Meditation to light blue, and Favor to mint green.

![Full-Favor reminder in the selected mint-green color](images/favor-reminder.png)

**Disable** stops the alarm, clears its visible reminders and cancels its queued chimes. This choice is saved across client restarts and reconnects. Saving an editor opened before disabling a Food, Water or Favor monitor keeps that monitor disabled.

## Edit alarms and common settings

**Edit** changes an alarm's name, reminder text and relevant timing or threshold parameters. Water uses **Alert below (%)** and **Repeat (minutes)**, with no lower cutoff.

![Water editor with its 50 percent threshold and one-minute repeat](images/water-editor.png)

Open the shared **Mods Registry** and click **Settings** on the Alarm Clock card to edit every alarm's enabled state, text, delivery options, color and parameters in one window. Hover help explains the fields. Valid changes are saved and applied immediately; invalid input leaves the saved value intact.

![Alarm Clock settings in the shared Chamomilo Settings editor](images/settings.png)

The shared updater selects English, Brazilian Portuguese, German or Russian for the interface. Names and messages you enter are preserved as written.

## HUD and saved preferences

Drag the HUD clock to move it. **HUD Settings** can hide or show the clock. Console command `alarmclock` opens the list; `alarmclockshow` restores the clock to its default position and shows it again.

Alarm settings and timer deadlines are saved in `.chamomilo/alarmclock.properties` under your Windows user folder. Existing preferences are retained when upgrading. Older Water settings migrate once to the 50% / one-minute defaults, preserving their enabled state and message.

Alarms stay silent while disconnected and resume without a burst of missed reminders. The mod reminds you to act; it does not eat, drink, pray, meditate or cast spells for you.
