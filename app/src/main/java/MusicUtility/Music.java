package MusicUtility;

import android.util.Log;

public class Music {
    private long id;//音乐iD
    private long albumID;//专辑图片id
    private String musicName;//音乐名称
    private String author;//作者
    private String duration;//歌曲时间长度
    private String path;
    private int durationTime;//整型的歌曲时间长度

    public int getLoaction() {
        return loaction;
    }

    public void setLoaction(int loaction) {
        this.loaction = loaction;
    }

    private int loaction;//music在list中的位置
    public String getMusicName() {
        return musicName;
    }

    public int getDurationTime() {
        return durationTime;
    }

    public void setDurationTime(int durationTime) {
        this.durationTime = durationTime;
    }

    public void setMusicName(String musicName) {
        this.musicName = musicName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAlbumID() {
        return albumID;
    }

    public void setAlbumID(long albumID) {
        this.albumID = albumID;
    }
}
