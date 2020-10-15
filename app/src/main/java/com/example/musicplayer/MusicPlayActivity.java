package com.example.musicplayer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import MusicUtility.Music;
import MusicUtility.MusicAdapter;
import Service.MusicService;
import Utility.Utility;

public class MusicPlayActivity extends AppCompatActivity {

    //----begin-----其他变量定义
    private long firstTime = 0;
    MusicFinishBroadcastReceiver receiver;//广播接收器
    SeekbarThread seekbarThread;
    private List<Music> allMusic;
    private List<Music> listMusic;
    private MusicAdapter musicAdapter;
    private Music currentMusic;
    private int playFlag=0;//标志变量：0代表从头开始播放，1代表暂停，2代表播放
    int playOrderFlag=0;//播放顺序标志：0顺序播放，1随机播放，2单曲循环
    private MusicService.MusicBinder musicBinder;
    private ServiceConnection connection=new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicBinder=(MusicService.MusicBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            musicProgressBar.setProgress(msg.what);
            currentTimeView.setText(musicBinder.getTime());
        }
    };
    ObjectAnimator animator;//动画
    //----end-------其他变量定义


    //----begin-----控件变量定义
    ImageButton playButton;//播放按钮
    DrawerLayout drawerLayout;//抽屉控件
    ListView musicListView;
    SearchView searchView;
    TextView listviewTitle;
    TextView musicTitleView;//音乐标题
    TextView authorNameView;//歌手标题
    SeekBar musicProgressBar;//进度信息
    TextView currentTimeView;//当前歌曲时间
    TextView totalTimeView;//当前歌曲总时间
    ImageView albumImageView;//中间黑胶唱片图片
    ImageView backgroundImageView;//背景图片设置
    //----end-------控件变量定义

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_play_activity);
        //隐藏状态栏
        if(Build.VERSION.SDK_INT>=21){
            View docerView=getWindow().getDecorView();
            docerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.parseColor("#9d282a"));
        }
        //申请权限
        requestPermission();
        listMusic=new ArrayList<>();
        listMusic.addAll(allMusic);

        //初始化控件
        initControl();

        //初始化动画
        initAnimation();

        //绑定服务
        Intent intent=new Intent(this,MusicService.class);
        bindService(intent,connection,BIND_AUTO_CREATE);

        //注册广播
        receiver=new MusicFinishBroadcastReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("com.example.musicplayer.BROADCAST");
        filter.addAction("notificationpre");
        filter.addAction("notificationnext");
        filter.addAction("notificationplay");
        filter.addAction("notificationclose");
        registerReceiver(receiver,filter);

        //初始化Bitmap


    }


    //申请权限
    private void requestPermission(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }else{
            allMusic= Utility.requestMusic(this);
        }
    }

    //初始化动画
    private void initAnimation(){
        animator = ObjectAnimator.ofFloat(albumImageView, "rotation", 0f, 360.0f);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());//匀速
        animator.setRepeatCount(-1);//设置动画重复次数（-1代表一直转）
        animator.setRepeatMode(ValueAnimator.RESTART);//动画重复模式
    }

    //初始化控件
    private void initControl(){
        currentTimeView=(TextView)findViewById(R.id.currenttimeTextView);
        totalTimeView=(TextView)findViewById(R.id.totalTimeTextView);
        Button openDrawerBtn=(Button)findViewById(R.id.drawer_open_button);
        drawerLayout=(DrawerLayout)findViewById(R.id.drawerLayout);
        openDrawerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        listviewTitle=(TextView)findViewById(R.id.listview_title);
        //listview初始化
        musicListView=(ListView)findViewById(R.id.allMusicListView);
        musicAdapter=new MusicAdapter(this,R.layout.listview_item,listMusic);
        musicListView.setAdapter(musicAdapter);
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                currentMusic=listMusic.get(position);
                musicBinder.startMusic(currentMusic);
                showNotification(false,Utility.subStringWithLength(currentMusic.getMusicName(),6),Utility.subStringWithLength(currentMusic.getAuthor(),6));
                playFlag=1;
                if (seekbarThread==null){
                    seekbarThread=new SeekbarThread();
                    new Thread(seekbarThread).start();
                }

                playButton.setImageResource(R.drawable.play_pause);
                updateMusicInfo();
                drawerLayout.closeDrawers();
            }
        });
        //searchView初始化
        searchView=(SearchView)findViewById(R.id.searchView);
        searchView.setOnSearchClickListener(new View.OnClickListener() {//在searchView被点击的时候，增加其长度，去掉本地音乐字样
            @Override
            public void onClick(View v) {
                listviewTitle.setVisibility(View.GONE);
                RelativeLayout.LayoutParams layoutParams=(RelativeLayout.LayoutParams) searchView.getLayoutParams();
                layoutParams.width=Utility.dpTopx(MusicPlayActivity.this,240);
                searchView.setLayoutParams(layoutParams);

            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {//在searchView被关闭的时候，显示本地音乐的字样
            @Override
            public boolean onClose() {
                listviewTitle.setVisibility(View.VISIBLE);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {//搜索框中文字变化的时候，动态进行搜索
                listMusic.clear();
                listMusic.addAll(searchMusic(newText));
                musicAdapter.notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {//用户提交搜索结果的时候，关闭搜索框
                //searchView.clearFocus();
                /*searchView.setIconified(true);
                listMusic.clear();
                listMusic.addAll(searchMusic(query));
                musicAdapter.notifyDataSetChanged();
               searchView.setIconified(true);*/
                return false;
            }
        });

        //--------begin------从偏好设置中读取保存的信息，并设置标题歌手等信息----------------
        musicTitleView=(TextView)findViewById(R.id.music_textView);
        authorNameView=(TextView)findViewById(R.id.author_name_textView);
        Music music=readMusic();
        if (music!=null){
            currentMusic=music;
        }else{
            currentMusic=allMusic.get(0);
        }

        //--------end--------从偏好设置中读取保存的信息，并设置标题歌手等信息----------------

        //设置进度条
        musicProgressBar=(SeekBar)findViewById(R.id.seekBar);
        musicProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!musicBinder.isPlaying())
                    seekBar.setProgress(0);
                else
                    musicBinder.seekTo(seekBar.getProgress());
            }
        });

        //--------begin------各个播放控制键设置----------------
        //暂停、播放按钮
        playButton=(ImageButton) findViewById(R.id.pause_Button);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (playFlag){
                    case 0://从头播放
                        musicBinder.startMusic(currentMusic);
                        playFlag=1;
                        if (seekbarThread==null){
                            seekbarThread=new SeekbarThread();
                            new Thread(seekbarThread).start();
                        }
                        animator.start();
                        showNotification(false,Utility.subStringWithLength(currentMusic.getMusicName(),6),Utility.subStringWithLength(currentMusic.getAuthor(),6));
                        playButton.setImageResource(R.drawable.play_pause);
                        break;
                    case 1://暂停
                        musicBinder.pauseMusic();
                        animator.pause();
                        playButton.setImageResource(R.drawable.play_play);
                        showNotification(true,Utility.subStringWithLength(currentMusic.getMusicName(),6),Utility.subStringWithLength(currentMusic.getAuthor(),6));
                        playFlag=2;
                        break;
                    case 2://重新播放
                        animator.resume();
                        musicBinder.restartMusic();
                        playButton.setImageResource(R.drawable.play_pause);
                        showNotification(false,Utility.subStringWithLength(currentMusic.getMusicName(),6),Utility.subStringWithLength(currentMusic.getAuthor(),6));
                        playFlag=1;
                        break;
                    default:
                        break;
                }

            }
        });
        //播放顺序设置
        final ImageButton playOrderButton=(ImageButton)findViewById(R.id.orderBtn);
        playOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playOrderFlag+=1;
                playOrderFlag=playOrderFlag%3;
                switch (playOrderFlag){
                    case 0:
                        playOrderButton.setImageResource(R.drawable.order_play);
                        break;
                    case 1:
                        playOrderButton.setImageResource(R.drawable.single_play);
                        break;
                    case 2:
                        playOrderButton.setImageResource(R.drawable.random_play);
                        break;
                    default:
                        break;
                }
            }
        });
        //播放下一曲
        ImageButton nextButton=(ImageButton)findViewById(R.id.next_Button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animator.pause();
                int currentMusicPosition=currentMusic.getLoaction();
                currentMusic=allMusic.get(Utility.nextMusicPosition(allMusic.size(),currentMusicPosition,playOrderFlag));
                musicBinder.startMusic(currentMusic);
                showNotification(false,Utility.subStringWithLength(currentMusic.getMusicName(),6),Utility.subStringWithLength(currentMusic.getAuthor(),6));
                if (seekbarThread==null){
                    seekbarThread=new SeekbarThread();
                    new Thread(seekbarThread).start();
                }
                playFlag=1;
                playButton.setImageResource(R.drawable.play_pause);
                updateMusicInfo();
            }
        });
        //播放上一曲
        ImageButton preButton=(ImageButton)findViewById(R.id.pre_Button);
        preButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animator.pause();
                int currentMusicPosition=currentMusic.getLoaction();
                currentMusic=allMusic.get(Utility.preMusicPosition(allMusic.size(),currentMusicPosition,playOrderFlag));
                musicBinder.startMusic(currentMusic);
                showNotification(false,Utility.subStringWithLength(currentMusic.getMusicName(),6),Utility.subStringWithLength(currentMusic.getAuthor(),6));
                if (seekbarThread==null){
                    seekbarThread=new SeekbarThread();
                    new Thread(seekbarThread).start();
                }
                playFlag=1;
                playButton.setImageResource(R.drawable.play_pause);
                updateMusicInfo();
            }
        });
        //--------end------各个播放控制键设置----------------

        //初始化中间黑胶唱片图片控件
        albumImageView=(ImageView)findViewById(R.id.disc);
        //初始化背景图片部分
        backgroundImageView=(ImageView)findViewById(R.id.music_background);
        //一切初始化结束后，更新界面
        updateMusicInfo();
    }

    //当播放歌曲变化时，更新界面显示信息
    private void updateMusicInfo(){
        musicTitleView.setText(Utility.subStringWithLength(currentMusic.getMusicName(),15));
        authorNameView.setText(Utility.subStringWithLength(currentMusic.getAuthor(),15));
        totalTimeView.setText(currentMusic.getDuration());
        musicProgressBar.setMax(currentMusic.getDurationTime());
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Bitmap bmp=Utility.getMusicBitemp(MusicPlayActivity.this,currentMusic.getId(),currentMusic.getAlbumID());
                Bitmap discBmp= BitmapFactory.decodeResource(MusicPlayActivity.this.getResources(),R.drawable.fm_play_disc);
                final Bitmap resultBmp=Utility.mergeThumbnailBitmap(discBmp,bmp);
                MusicPlayActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        albumImageView.setImageBitmap(resultBmp);
                        backgroundImageView.setImageBitmap(Utility.doBlur(bmp,30,true));
                        if (playFlag==1){
                            animator.start();
                        }
                    }
                });

            }
        }).start();
    }

    //对申请权限的结果查看
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    allMusic= Utility.requestMusic(this);
                }else{
                    Toast.makeText(this,"必须同意权限才能继续使用",Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    //从所有音乐中搜索符合结果的Music信息
    private List<Music> searchMusic(String info){
        List<Music> resultList=new ArrayList<>();
        for (Music music:allMusic){//
            if (music.getMusicName().indexOf(info)!=-1||music.getAuthor().indexOf(info)!=-1){
                resultList.add(music);
            }
        }
        return resultList;
    }

    //将上次播放的音乐保存到偏好设置中
    private void saveMusic(){
        if (currentMusic!=null){
            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putLong("id",currentMusic.getId());
            editor.putString("musicName",currentMusic.getMusicName());
            editor.putString("musicAuthor",currentMusic.getAuthor());
            editor.putString("duration",currentMusic.getDuration());
            editor.putString("path",currentMusic.getPath());
            editor.putInt("intTime",currentMusic.getDurationTime());
            editor.putInt("location",currentMusic.getLoaction());
            editor.putLong("albumID",currentMusic.getAlbumID());
            editor.apply();
        }
    }

    //从偏好设置中读取上次播放的音乐
    private Music readMusic(){
        Music music;
        SharedPreferences preferences=PreferenceManager.getDefaultSharedPreferences(this);
        String musicName=preferences.getString("musicName",null);
        if (musicName!=null){
            music=new Music();
            music.setId(preferences.getLong("id",0));
            music.setMusicName(musicName);
            music.setAuthor(preferences.getString("musicAuthor",null));
            music.setDuration(preferences.getString("duration",null));
            music.setPath(preferences.getString("path",null ));
            music.setDurationTime(preferences.getInt("intTime",-1));
            music.setLoaction(preferences.getInt("location",-1));
            music.setAlbumID(preferences.getLong("albumID",0));
            return music;
        }
        return null;
    }

    //显示通知栏函数:flag为0时，设置为暂停按钮，flag为1设置为播放按钮
    private void showNotification(boolean flag,String name,String author){
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "default");
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            NotificationChannel channelbody = new NotificationChannel("default","消息推送",NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channelbody);
        }
        RemoteViews remoteViews=new RemoteViews(getPackageName(),R.layout.notification_layout);
        if (flag){
            remoteViews.setImageViewResource(R.id.notifi_music_play,R.drawable.notification_play);
        }else{
            remoteViews.setImageViewResource(R.id.notifi_music_play,R.drawable.notification_pause);
        }
        remoteViews.setTextViewText(R.id.notification_music_name,name);
        remoteViews.setTextViewText(R.id.notification_music_author,author);

        //-----begin------设置通知中按钮点击事件
        //1.上一曲
        Intent buttonIntent=new Intent("notificationpre");
        PendingIntent intent_pre=PendingIntent.getBroadcast(this,0,buttonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notification_music_pre,intent_pre);
        //2.下一曲
        Intent nextIntent=new Intent("notificationnext");
        PendingIntent intent_next=PendingIntent.getBroadcast(this,0,nextIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notifi_music_next,intent_next);
        //3.暂停或继续播放
        Intent playIntent=new Intent("notificationplay");
        PendingIntent intent_play=PendingIntent.getBroadcast(this,0,playIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notifi_music_play,intent_play);
        //4.关闭程序
        Intent closeIntent=new Intent("notificationclose");
        PendingIntent intent_close=PendingIntent.getBroadcast(this,0,closeIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notification_music_close,intent_close);
        //-----end--------设置通知中按钮点击事件
        notificationBuilder.setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Default notification")
                .setCustomContentView(remoteViews)
                .setOnlyAlertOnce(true)
                .setContentText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.");
        Notification notification=notificationBuilder.build();
        notification.flags|= Notification.FLAG_NO_CLEAR;//设置通知不被用户取消
        notificationManager.notify(1, notification);
    }
    //活动结束后的清理工作
    @Override
    protected void onDestroy() {
        saveMusic();
        unbindService(connection);
        super.onDestroy();
        unregisterReceiver(receiver);
    }


    //子线程类，用来更新进度条
    class SeekbarThread implements Runnable{

        @Override
        public void run() {
            while (musicBinder!=null){
                handler.sendEmptyMessage(musicBinder.getProgress());
                try{
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //广播接收器类，当一首歌曲播放完毕后，根据播放顺序播放下一首
    class MusicFinishBroadcastReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if (action.equals("com.example.musicplayer.BROADCAST")){
                int currentMusicPosition=currentMusic.getLoaction();
                currentMusic=allMusic.get(Utility.nextMusicPosition(allMusic.size(),currentMusicPosition,playOrderFlag));
                musicBinder.startMusic(currentMusic);
                updateMusicInfo();
            }
            else if (action.equals("notificationplay")){
                switch (playFlag){
                    case 1://暂停
                        musicBinder.pauseMusic();
                        animator.pause();
                        playButton.setImageResource(R.drawable.play_play);
                        showNotification(true,Utility.subStringWithLength(currentMusic.getMusicName(),4),Utility.subStringWithLength(currentMusic.getAuthor(),4));
                        playFlag=2;
                        break;
                    case 2://重新播放
                        animator.resume();
                        musicBinder.restartMusic();
                        playButton.setImageResource(R.drawable.play_pause);
                        showNotification(false,Utility.subStringWithLength(currentMusic.getMusicName(),4),Utility.subStringWithLength(currentMusic.getAuthor(),4));
                        playFlag=1;
                        break;
                    default:
                        break;
                }
            }else if (action.equals("notificationpre")){
                animator.pause();
                int currentMusicPosition=currentMusic.getLoaction();
                currentMusic=allMusic.get(Utility.preMusicPosition(allMusic.size(),currentMusicPosition,playOrderFlag));
                musicBinder.startMusic(currentMusic);
                showNotification(false,Utility.subStringWithLength(currentMusic.getMusicName(),3),Utility.subStringWithLength(currentMusic.getAuthor(),4));
                if (seekbarThread==null){
                    seekbarThread=new SeekbarThread();
                    new Thread(seekbarThread).start();
                }
                playFlag=1;
                playButton.setImageResource(R.drawable.play_pause);
                updateMusicInfo();
            }else if (action.equals("notificationnext")){
                animator.pause();
                int currentMusicPosition=currentMusic.getLoaction();
                currentMusic=allMusic.get(Utility.nextMusicPosition(allMusic.size(),currentMusicPosition,playOrderFlag));
                musicBinder.startMusic(currentMusic);
                showNotification(false,Utility.subStringWithLength(currentMusic.getMusicName(),3),Utility.subStringWithLength(currentMusic.getAuthor(),4));
                if (seekbarThread==null){
                    seekbarThread=new SeekbarThread();
                    new Thread(seekbarThread).start();
                }
                playFlag=1;
                playButton.setImageResource(R.drawable.play_pause);
                updateMusicInfo();
            }else if (action.equals("notificationclose")){
                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                System.exit(0);
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
            long secondTime = System.currentTimeMillis();
            if (secondTime - firstTime > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                firstTime = secondTime;
                return true;
            } else {
                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                System.exit(0);
            }
        }

        return super.onKeyUp(keyCode, event);
    }
}
