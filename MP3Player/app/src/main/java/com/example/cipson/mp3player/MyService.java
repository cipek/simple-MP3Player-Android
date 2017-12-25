package com.example.cipson.mp3player;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Created by Cipson on 2017-11-03.
 */

public class MyService extends Service {

    private Messenger messenger, replyto;
    private MP3Player mp3Player = new MP3Player();
    private Player player;

    //Stores info about current song
    private SongInfo currentSongInfo = null;

    public static final int SET_REPLY = 0;
    public static final int LOAD_SONG = 1;
    public static final int PLAY_SONG = 2;
    public static final int PAUSE_SONG = 3;
    public static final int STOP_SONG = 4;

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                case SET_REPLY:
                    replyto = msg.replyTo;
                    //If music is still playing in the background or is paused, inform activity
                    if(mp3Player.state == MP3Player.MP3PlayerState.PLAYING || mp3Player.state == MP3Player.MP3PlayerState.PAUSED ){
                        Message reply = Message.obtain(null, MainActivity.RECONNECT, 0, 0);
                        Bundle b = new Bundle();

                        if(mp3Player.state == MP3Player.MP3PlayerState.PLAYING)
                            currentSongInfo.setIsPlaying(true);
                        else
                            currentSongInfo.setIsPaused(mp3Player.getDuration(), mp3Player.getProgress());


                        b.putParcelable("SONG", currentSongInfo);
                        reply.setData(b);
                        try {
                            replyto.send(reply);
                        } catch (RemoteException e) {
                            Log.d("g53mdp", e.toString());
                        }
                    }
                    break;
                case LOAD_SONG:
                    //If another song is being played stop it
                    if(mp3Player.state == MP3Player.MP3PlayerState.PLAYING)
                        mp3Player.stop();

                    currentSongInfo = msg.getData().getParcelable("SONG");
                    mp3Player.load(currentSongInfo.getPath());

                    //If there is no error, play music
                    if(mp3Player.state!= MP3Player.MP3PlayerState.ERROR) {
                        mp3Player.play();
                        player = new Player();
                    }
                    createNotification(true);
                    break;
                case PLAY_SONG:
                    //If music was paused continue playing
                    Log.e("Service state", mp3Player.state.toString());
                    if(mp3Player.state == MP3Player.MP3PlayerState.PAUSED)
                        mp3Player.play();
                    //If music was stopped reload it
                    else if(mp3Player.state == MP3Player.MP3PlayerState.STOPPED) {
                        if(currentSongInfo!= null && currentSongInfo.getPath()!= null){
                            mp3Player.load(currentSongInfo.getPath());
                            //If there is no error, play music
                            if(mp3Player.state!= MP3Player.MP3PlayerState.ERROR) {
                                mp3Player.play();
                            }
                        }
                    }
                    player = new Player();
                    createNotification(true);
                    break;
                case PAUSE_SONG:
                    mp3Player.pause();
                    createNotification(false);
                    break;
                case STOP_SONG:
                    mp3Player.stop();
                    //Get rid of notification
                    stopForeground(true);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    public void onCreate()
    {
        messenger = new Messenger(new MyHandler());
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return messenger.getBinder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        player = null;
        super.onDestroy();
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    //Informs activity about the progress of the song
    public class Player extends Thread implements Runnable{

        public Player()
        {
            this.start();
        }

        public void run()
        {
            //If music is playing
            while(mp3Player.state == MP3Player.MP3PlayerState.PLAYING)
            {
                //If messenger is connected with activity
                if(replyto!= null) {
                    Message reply = Message.obtain(null, MainActivity.PROGRESS, 0, 0);
                    Bundle b = new Bundle();
                    b.putInt("PROGRESS", mp3Player.getProgress());
                    b.putInt("DURATION", mp3Player.getDuration());
                    reply.setData(b);
                    try {
                        replyto.send(reply);
                    } catch (RemoteException e) {
                        Log.d("Error", e.toString());
                    }
                }
                try {Thread.sleep(100);} catch(Exception e) {return;}

            }

        }

    }

    //Displays Notification
    private void createNotification(boolean isPlaying){
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        Notification notification;
        if(isPlaying)
            notification = new NotificationCompat.Builder(this)
                .setTicker(("message"))
                .setSmallIcon(R.drawable.ic_play)
                .setContentTitle("MP3 Player")
                .setContentText(currentSongInfo.getName())
                .setContentIntent(pi)
                .setAutoCancel(false)
                .build();
        else
                notification = new NotificationCompat.Builder(this)
                .setTicker(("message"))
                .setSmallIcon(R.drawable.ic_pause)
                .setContentTitle("MP3 Player")
                .setContentText(currentSongInfo.getName())
                .setContentIntent(pi)
                .setAutoCancel(false)
                .build();


        //Let service works in background after activity is destroyed
        startForeground(1337, notification);
    }

}
