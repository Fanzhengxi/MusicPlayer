package Service;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.example.musicplayer.R;

import java.io.IOException;

import MusicUtility.Music;
import Utility.Utility;

public class MusicService extends Service {

    private MusicBinder musicBinder;
    private MediaPlayer musicPlayer;


    public MusicService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        musicPlayer=new MediaPlayer();
        //播放结束时，发送一条广播，自动播放下一首歌曲
        musicPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Intent intent=new Intent("com.example.musicplayer.BROADCAST");
                sendBroadcast(intent);
            }
        });
        musicBinder=new MusicBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return musicBinder;
    }

    public class MusicBinder extends Binder{
        //开始播放一首新歌
        public void startMusic(Music music){
            try {
                musicPlayer.reset();
                musicPlayer.setDataSource(music.getPath());
                musicPlayer.prepare();
                musicPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //暂停播放
        public void pauseMusic(){
            if (musicPlayer!=null&&musicPlayer.isPlaying()){
                musicPlayer.pause();
            }
        }

        //继续播放
        public void restartMusic(){
            if (musicPlayer!=null&&!musicPlayer.isPlaying()){
                musicPlayer.start();
            }
        }

        //播放上一首
        public void  preMusic(Music music){
            startMusic(music);
        }

        //播放下一首
        public void nextMusic(Music music){
            startMusic(music);
        }

        //获取当前播放的时间，用来更新显示时间
        public String getTime(){
            if (musicPlayer!=null){
                return Utility.formatTime(musicPlayer.getCurrentPosition());
            }
            return "00:00";
        }

        //获取当前播放的进度
        public int getProgress(){
            if (musicPlayer!=null){
                return musicPlayer.getCurrentPosition();
            }
            return 0;
        }

        //拖动进度条时跳转到播放位置
        public void seekTo(int progress){
            if (musicPlayer!=null){
                musicPlayer.seekTo(progress);
            }
        }

        //判断当前是否正在播放
        public boolean isPlaying(){
            return musicPlayer.isPlaying();
        }
    }
}
