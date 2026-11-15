package com.storm.browser;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/** Foreground service that keeps Storm alive while media is playing in the background. */
public class MediaService extends Service {
    static final String CH = "storm_media";
    PowerManager.WakeLock wl;
    MediaSession ms;

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CH, "Media playback", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "storm:media"); wl.acquire(4 * 60 * 60 * 1000L);
        ms = new MediaSession(this, "Storm");
        ms.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { MainActivity.cmd("play"); }
            @Override public void onPause() { MainActivity.cmd("pause"); }
        });
        ms.setActive(true);
    }

    @Override public int onStartCommand(Intent in, int flags, int id) {
        String a = in == null ? null : in.getAction();
        if (MainActivity.inst == null) { stopSelf(); return START_NOT_STICKY; }
        if ("stop".equals(a)) { MainActivity.cmd("pause"); stopForeground(true); stopSelf(); return START_NOT_STICKY; }
        if ("pause".equals(a) || "play".equals(a)) { MainActivity.cmd(a); return START_NOT_STICKY; }
        String title = in != null && in.getStringExtra("title") != null ? in.getStringExtra("title") : "Playing";
        boolean playing = in == null || in.getBooleanExtra("playing", true);
        show(title, playing);
        return START_NOT_STICKY;
    }

    PendingIntent svc(String action) {
        return PendingIntent.getService(this, action.hashCode(), new Intent(this, MediaService.class).setAction(action),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    void show(String title, boolean playing) {
        ms.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Storm").build());
        ms.setPlaybackState(new PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE)
            .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, 0, 1f).build());
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        b.setSmallIcon(R.drawable.ic_arrow).setContentTitle(title)
            .setContentText("Storm \u00B7 " + (playing ? "Playing" : "Paused"))
            .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT))
            .setOngoing(true).setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(new Notification.Action.Builder(Icon.createWithResource(this,
                playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play),
                playing ? "Pause" : "Play", svc(playing ? "pause" : "play")).build())
            .addAction(new Notification.Action.Builder(Icon.createWithResource(this,
                android.R.drawable.ic_menu_close_clear_cancel), "Stop", svc("stop")).build())
            .setStyle(new Notification.MediaStyle().setMediaSession(ms.getSessionToken()).setShowActionsInCompactView(0, 1));
        Notification n = b.build();
        if (Build.VERSION.SDK_INT >= 29) startForeground(7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else startForeground(7, n);
    }

    @Override public void onDestroy() {
        if (ms != null) { ms.setActive(false); ms.release(); }
        if (wl != null && wl.isHeld()) wl.release();
        super.onDestroy();
    }
}
