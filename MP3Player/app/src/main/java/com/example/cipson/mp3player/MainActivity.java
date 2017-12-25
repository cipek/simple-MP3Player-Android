package com.example.cipson.mp3player;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Created by Cipson on 2017-11-03.
 */

public class MainActivity extends AppCompatActivity {

    private Messenger messenger, replyMessenger;

    public static final int PROGRESS = 0;
    public static final int RECONNECT = 1;

    private TextView tvProgress, tvDuration, tvName;
    private ProgressBar pbTime;
    private ImageButton bPP;

    //Indicates if music was chosen (is being played)
    private boolean isPlaying = false;
    //Which song from the list is currently being played
    private int songNumber = -1;

    private File fileList[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        createList();
        this.startService(new Intent(this, MyService.class));
    }

    //Initialize views
    private void initViews(){
        tvDuration = findViewById(R.id.tv_duration);
        tvProgress = findViewById(R.id.tv_progress);
        tvName = findViewById(R.id.tv_song_name);
        pbTime = findViewById(R.id.pb_time);
        bPP = findViewById(R.id.b_pp);
    }

    private void createList(){
        final ListView lv = (ListView) findViewById(R.id.listView);
        File musicDir = new File(
                Environment.getExternalStorageDirectory().getPath()+ "/Music/");
        fileList = musicDir.listFiles();

        if(fileList != null) {
            lv.setAdapter(new ArrayAdapter<File>(this,
                    android.R.layout.simple_list_item_1, fileList));
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> myAdapter,
                                        View myView,
                                        int myItemInt,
                                        long mylng) {
                    File selectedFromList = (File) (lv.getItemAtPosition(myItemInt));

                    loadSong(selectedFromList, myItemInt);
                }
            });
        }
    }

    //Connects with service to load the song
    private void loadSong(File selectedFromList, int myItemInt){
        setSong(selectedFromList.getName(), myItemInt);

        Message message = Message.obtain(null, MyService.LOAD_SONG, 0, 0);
        Bundle b = new Bundle();

        //Creates object with info about song which is currently playing
        SongInfo currentSongInfo = new SongInfo(selectedFromList.getAbsolutePath(), selectedFromList.getName(), myItemInt);
        b.putParcelable("SONG", currentSongInfo);
        message.setData(b);

        try {messenger.send(message);}

        catch (RemoteException e) {e.printStackTrace();}
    }

    //Functionality of play/pause button
    public void playPauseButton(View view) {
        if(songNumber != -1) {
            Message message;
            if (isPlaying) {
                message = Message.obtain(null, MyService.PAUSE_SONG, 0, 0);
                isPlaying = false;
                //Changes image of the button to play
                bPP.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_play));
            } else {
                message = Message.obtain(null, MyService.PLAY_SONG, 0, 0);
                isPlaying = true;
                //Changes image of the button to pause
                bPP.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_pause));
            }

            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    //Set all necessary variables which indicates which song is being played
    private void setSong(String name, int position){
        isPlaying = true;
        tvName.setText(name);
        songNumber = position;
        bPP.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_pause));
    }

    //Functionality of next button
    public void clickNext(View view) {
        if(fileList != null && fileList.length>0) {
            if ((++songNumber) >= fileList.length)
                songNumber = 0;
            loadSong(fileList[songNumber], songNumber);
        }
    }

    //Functionality of previous button
    public void clickPrevious(View view) {
        if(fileList != null && fileList.length>0) {
            if ((--songNumber) <= -1)
                songNumber = fileList.length - 1;
            loadSong(fileList[songNumber], songNumber);
        }
    }

    public void stopButton(View view) {
        if(songNumber != -1) {
            Message message = Message.obtain(null, MyService.STOP_SONG, 0, 0);
            isPlaying = false;
            //Changes image of the button to pause
            bPP.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                    R.drawable.ic_play));

            try {
                messenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
        tvProgress.setText("0:00");
        pbTime.setProgress(0);
    }


    private class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what)
            {
                //Response form service which updates progress bar and time
                case PROGRESS:
                    int duration = msg.getData().getInt("DURATION", 0);
                    int progress = msg.getData().getInt("PROGRESS", 0);
                    setProgressView(duration, progress);
                    break;
                //Response from service when the app is re-opened
                case RECONNECT:
                    SongInfo currentSongInfo = msg.getData().getParcelable("SONG");
                    setSong(currentSongInfo.getName(), currentSongInfo.getPosition());
                    //If song is paused get and set info about duration and progress
                    if(!currentSongInfo.getIsPlaying()) {
                        setProgressView(currentSongInfo.getDuration(), currentSongInfo.getProgress());
                        bPP.setImageBitmap(BitmapFactory.decodeResource(getResources(),
                                R.drawable.ic_play));
                        isPlaying = false;
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    //Set info about progress and duration on the view
    private void setProgressView(int duration, int progress) {
        tvProgress.setText(getTimeFormMilis(progress));
        tvDuration.setText(getTimeFormMilis(duration));
        pbTime.setMax(duration);
        pbTime.setProgress(progress);
    }

    private ServiceConnection serviceConnection = new ServiceConnection()
    {
        //Sends message to Service when that connecion is established
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            messenger = new Messenger(service);

            Message message = Message.obtain(null, MyService.SET_REPLY, 0, 0);
            message.replyTo = replyMessenger;
            try
            {
                messenger.send(message);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            messenger = null;
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(serviceConnection!=null) {
            unbindService(serviceConnection);
            serviceConnection = null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        this.bindService(new Intent(this, MyService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        replyMessenger = new Messenger(new MyHandler());
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    //Converts milliseconds to min and sec
    private String getTimeFormMilis(int millis){
        if(TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)) >= 10)
            return String.format("%d:%d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
        else
            return String.format("%d:0%d",
                    TimeUnit.MILLISECONDS.toMinutes(millis),
                    TimeUnit.MILLISECONDS.toSeconds(millis) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
            );
    }

}
