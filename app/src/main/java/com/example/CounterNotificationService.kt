package com.example

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.Locale

class CounterNotificationService : Service(), TextToSpeech.OnInitListener {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var pollJob: Job? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val knownOrderIds = mutableSetOf<String>()
    private var isFirstPoll = true

    companion object {
        const val LIVE_CHANNEL_ID = "moon_light_counter_live_channel"
        const val ALERT_CHANNEL_ID = "moon_light_order_alert_channel"
        const val ONGOING_NOTIFICATION_ID = 4001

        private const val ACTION_START = "START_SERVICE"
        private const val ACTION_STOP = "STOP_SERVICE"
        private const val ACTION_PREPARE = "ACTION_PREPARE"
        private const val ACTION_STOP_ALARM = "ACTION_STOP_ALARM"

        fun startService(context: Context) {
            val intent = Intent(context, CounterNotificationService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CounterNotificationService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("CounterService", "Service onCreate")
        createNotificationChannels()
        tts = TextToSpeech(this, this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d("CounterService", "onStartCommand action: $action")
        when (action) {
            ACTION_START -> {
                startForegroundCompat()
                startPolling()
            }
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_PREPARE -> {
                val orderId = intent.getStringExtra("order_id")
                val notificationId = intent.getIntExtra("notification_id", -1)
                if (orderId != null) {
                    updateOrderStatusToPreparing(orderId, notificationId)
                }
            }
            ACTION_STOP_ALARM -> {
                val notificationId = intent.getIntExtra("notification_id", -1)
                if (notificationId != -1) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(notificationId)
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val notification = NotificationCompat.Builder(this, LIVE_CHANNEL_ID)
            .setContentTitle("Staff Server Sync Active")
            .setContentText("Listening for live table orders in background...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = serviceScope.launch {
            while (isActive) {
                try {
                    val freshOrders = RetrofitClient.firebaseService.getOrders() ?: emptyMap()
                    if (isFirstPoll) {
                        knownOrderIds.clear()
                        knownOrderIds.addAll(freshOrders.keys)
                        isFirstPoll = false
                        Log.d("CounterService", "Initial service poll: stored legacy orders")
                    } else {
                        val newKeys = freshOrders.keys.filter { it !in knownOrderIds }
                        for (key in newKeys) {
                            val order = freshOrders[key]
                            if (order != null && order.status == "new") {
                                triggerNewOrderAlert(key, order)
                            }
                        }
                        knownOrderIds.addAll(freshOrders.keys)
                    }
                } catch (e: Exception) {
                    Log.e("CounterService", "Failed fetching incoming database orders: ${e.message}")
                }
                delay(3000)
            }
        }
    }

    private fun triggerNewOrderAlert(orderId: String, order: FirebaseOrder) {
        serviceScope.launch(Dispatchers.Main) {
            // Screen Wake System Lock Overrule
            try {
                val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
                val wl = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "MoonLightCafe:NewOrderWakeLock"
                )
                wl.acquire(6000)
            } catch (e: Exception) {
                Log.e("CounterService", "Could not wake up mobile screen: ${e.message}")
            }

            // Extreme Vibration Waves
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                    vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                    v.vibrate(pattern, -1)
                }
            } catch (e: Exception) {
                Log.e("CounterService", "Vibrator trigger fault: ${e.message}")
            }

            // Text To Speech Order announcement
            if (ttsReady) {
                val itemSummary = order.items.joinToString(", ") { "${it.qty} ${it.name}" }
                val speakText = "Attention Staff, new order for Table ${order.tableNumber}. Ordered items: $itemSummary."
                tts?.speak(speakText, TextToSpeech.QUEUE_FLUSH, null, "CounterServiceTTS")
            }

            val notificationId = orderId.hashCode()

            // Intent to Cook/Prepare
            val prepareIntent = Intent(this@CounterNotificationService, CounterNotificationService::class.java).apply {
                action = ACTION_PREPARE
                putExtra("order_id", orderId)
                putExtra("notification_id", notificationId)
            }
            val preparePendingIntent = PendingIntent.getService(
                this@CounterNotificationService,
                notificationId,
                prepareIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent to stop Alarm ringing sound (Silence Alarm)
            val stopAlarmIntent = Intent(this@CounterNotificationService, CounterNotificationService::class.java).apply {
                action = ACTION_STOP_ALARM
                putExtra("notification_id", notificationId)
            }
            val stopAlarmPending = PendingIntent.getService(
                this@CounterNotificationService,
                notificationId + 1,
                stopAlarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Intent to click entry and navigate into Counter app module
            val openAppIntent = Intent(this@CounterNotificationService, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                this@CounterNotificationService,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val summaryText = order.items.joinToString(", ") { "${it.qty}x ${it.name}" }
            
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val alertNotification = NotificationCompat.Builder(this@CounterNotificationService, ALERT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("🔥 NEW ORDER ALERT: TABLE ${order.tableNumber}!")
                .setContentText(summaryText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(
                    "Table ${order.tableNumber} ordered:\n$summaryText" + 
                    (if (!order.note.isNullOrEmpty()) "\nNote: ${order.note}" else "")
                ))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true) // Keeps ringing alert from being swiped away easily
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent)
                .setSound(ringtoneUri)
                .addAction(android.R.drawable.ic_media_play, "👨‍🍳 Prepare (Cook)", preparePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "🔇 Silence Alarm", stopAlarmPending)
                .build()

            // FLAG_INSISTENT makes the ringtone LOOP indefinitely until dismissed/clicked ("रोइरहोस् नहटाउन जेल")
            alertNotification.flags = alertNotification.flags or Notification.FLAG_INSISTENT

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, alertNotification)
        }
    }

    private fun updateOrderStatusToPreparing(orderId: String, notificationId: Int) {
        serviceScope.launch {
            try {
                RetrofitClient.firebaseService.updateOrderStatus(orderId, StatusUpdate("preparing"))
                Log.d("CounterService", "Updated order $orderId to preparing directly from background notification action.")
                
                if (notificationId != -1) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.cancel(notificationId)
                }
            } catch (e: Exception) {
                Log.e("CounterService", "Could not apply order status updates in background: ${e.message}")
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val liveChannel = NotificationChannel(
                LIVE_CHANNEL_ID,
                "Moon Light Sync Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps staff devices connected to the kitchen database"
                setShowBadge(false)
            }
            manager.createNotificationChannel(liveChannel)

            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Ringing Order Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm sounds for incoming kitchen list updates"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                
                val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                setSound(ringtoneUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = true
                tts?.setSpeechRate(0.85f)
                tts?.setPitch(1.05f)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CounterService", "Service onDestroy")
        pollJob?.cancel()
        serviceJob.cancel()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("CounterService", "TTS cleanup error: ${e.message}")
        }
    }
}
