package com.champions.destiny.musicplayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.util.NotificationUtil.IMPORTANCE_HIGH

class PlayerService : Service() {

    private val serviceBinder: IBinder = ServiceBinder()
    lateinit var player: ExoPlayer
    private lateinit var notificationManager: PlayerNotificationManager

    override fun onBind(intent: Intent): IBinder {
        return serviceBinder
    }

    inner class ServiceBinder: Binder(){
        fun getPlayerService(): PlayerService{
            return this@PlayerService
        }
    }


    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(applicationContext).build()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        player.setAudioAttributes(audioAttributes, true)
        val channelId = resources.getString(R.string.app_name) + " Music channel"
        val notificationId = 1
        notificationManager = PlayerNotificationManager.Builder(this, notificationId, channelId)
            .setNotificationListener(notificationListener)
            .setMediaDescriptionAdapter(descriptionAdapter)
            .setChannelImportance(IMPORTANCE_HIGH)
            .setSmallIconResourceId(R.drawable.not_icon)
            .setChannelDescriptionResourceId(R.string.app_name)
            .setNextActionIconResourceId(R.drawable.ic_skip_next)
            .setPreviousActionIconResourceId(R.drawable.ic_skip_previous)
            .setPauseActionIconResourceId(R.drawable.ic_pause)
            .setPlayActionIconResourceId(R.drawable.ic_play)
            .setChannelNameResourceId(R.string.app_name)
            .build()

        notificationManager.setPlayer(player)
        notificationManager.setPriority(NotificationCompat.PRIORITY_MAX)
        notificationManager.setUseRewindAction(false)
        notificationManager.setUseFastForwardAction(false)
    }

    private val notificationListener = object : PlayerNotificationManager.NotificationListener{
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            super.onNotificationCancelled(notificationId, dismissedByUser)
            stopForeground(true)
            if(player.isPlaying){
                player.pause()
            }
                player.stop()
                player.release()
                stopSelf()
        }

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            startForeground(notificationId, notification)
        }


    }

    private val descriptionAdapter = object : PlayerNotificationManager.MediaDescriptionAdapter{
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return player.currentMediaItem!!.mediaMetadata.title as String
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            val openAppIntent = Intent(applicationContext, MainActivity::class.java)
            return PendingIntent.getActivity(applicationContext, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return null
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val imageView = ImageView(applicationContext)
            imageView.setImageResource(
                player.currentMediaItem?.mediaMetadata?.artist.toString().toInt()
            )
            val bitmapDrawable: BitmapDrawable = imageView.drawable as BitmapDrawable
            return bitmapDrawable.bitmap
        }
    }

    override fun onDestroy() {
        if(player.isPlaying) {
            player.stop()
        }
        player.release()
        stopForeground(true)
        stopSelf()
        super.onDestroy()
    }
}