package com.example.cipson.mp3player;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Cipson on 2017-11-03.
 * Stores info about the song
 */

public class SongInfo implements Parcelable{

    private String path, name;
    private int position;

    private Boolean isPlaying;
    private int duration, progress;

    public SongInfo(String path, String name, int position){
        this.path = path;
        this.name = name;
        this.position = position;
    }

    protected SongInfo(Parcel in) {
        path = in.readString();
        name = in.readString();
        position = in.readInt();
    }

    public static final Creator<SongInfo> CREATOR = new Creator<SongInfo>() {
        @Override
        public SongInfo createFromParcel(Parcel in) {
            return new SongInfo(in);
        }

        @Override
        public SongInfo[] newArray(int size) {
            return new SongInfo[size];
        }
    };

    public String getPath(){return path;}

    public void setPath(String path){ this.path = path;}

    public String getName(){return name;}

    public void setName(String name){ this.name = name;}

    public int getPosition(){return position;}

    public void setPosition(int position){ this.position = position;}


    public void setIsPaused(int duration, int progress){
        this.isPlaying = false;
        this.duration = duration;
        this.progress = progress;
    }

    public boolean getIsPlaying(){return isPlaying;}

    public void setIsPlaying(boolean isPlaying){ this.isPlaying = isPlaying;}


    public int getDuration(){return duration;}

    public void setDuration(int duration){ this.duration = duration;}


    public int getProgress(){return progress;}

    public void setProgress(int progress){ this.progress = progress;}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(path);
        parcel.writeString(name);
        parcel.writeInt(position);
    }
}
