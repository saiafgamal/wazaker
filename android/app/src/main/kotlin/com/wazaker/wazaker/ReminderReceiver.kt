package com.wazaker.wazaker

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val store = WazakerStore(context)
        val action = intent?.getStringExtra(MainActivity.EXTRA_ACTION)
        val actionVerse = VerseRepository.byKey(intent?.getStringExtra(MainActivity.EXTRA_VERSE_KEY))
        if (action == MainActivity.ACTION_SAVE) {
            if (!store.contains(MainActivity.KEY_FAVORITES, actionVerse.key)) {
                store.toggleSet(MainActivity.KEY_FAVORITES, actionVerse.key)
            }
            return
        }
        if (action == MainActivity.ACTION_READ_LATER) {
            if (!store.contains(MainActivity.KEY_READ_LATER, actionVerse.key)) {
                store.toggleSet(MainActivity.KEY_READ_LATER, actionVerse.key)
            }
            return
        }

        val now = System.currentTimeMillis()
        val scheduled = store.scheduledReminders()
        val due = scheduled.filter { it.scheduledAt <= now }.minByOrNull { it.scheduledAt }
        val verse = due?.let { VerseRepository.byKey(it.verseKey) } ?: VerseRepository.random()
        store.addHistory(verse, due?.scheduledAt ?: now)
        store.saveUpcoming(scheduled.filter { it.id != due?.id && it.scheduledAt > now })
        showReminder(context, verse)
        val next = store.scheduledReminders().minByOrNull { it.scheduledAt }
        if (next != null) MainActivity.scheduleAlarm(context, next.scheduledAt) else MainActivity.scheduleReminders(context)
    }

    companion object {
        fun showReminder(context: Context, verse: Verse) {
            MainActivity.createNotificationChannel(context)
            val openIntent = PendingIntent.getActivity(
                context,
                verse.key.hashCode(),
                Intent(context, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_VERSE_KEY, verse.key)
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val saveIntent = PendingIntent.getBroadcast(
                context,
                verse.key.hashCode() + 10,
                Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_SAVE)
                    putExtra(MainActivity.EXTRA_VERSE_KEY, verse.key)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val readLaterIntent = PendingIntent.getBroadcast(
                context,
                verse.key.hashCode() + 20,
                Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(MainActivity.EXTRA_ACTION, MainActivity.ACTION_READ_LATER)
                    putExtra(MainActivity.EXTRA_VERSE_KEY, verse.key)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, MainActivity.CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            val notification = builder
                .setSmallIcon(com.wazaker.wazaker.R.mipmap.ic_launcher)
                .setContentTitle(verse.reference)
                .setContentText(verse.text)
                .setStyle(Notification.BigTextStyle().bigText(verse.text))
                .setContentIntent(openIntent)
                .addAction(com.wazaker.wazaker.R.mipmap.ic_launcher, "حفظ", saveIntent)
                .addAction(com.wazaker.wazaker.R.mipmap.ic_launcher, "قراءة لاحقًا", readLaterIntent)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .build()

            context.getSystemService(NotificationManager::class.java)
                .notify(verse.key.hashCode(), notification)
        }
    }
}
