package org.chamomilo.wurm.alarmclock;

/** Engine-owned state; only the timer deadline is persisted in the legacy settings format. */
final class AlarmRuntimeState {
    long dueAt;
    long nextReminder;
    int lastPercent=101;
}
