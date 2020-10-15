package MusicUtility;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.musicplayer.R;

import java.util.List;

public class MusicAdapter extends ArrayAdapter<Music> {
    private int resourceId;
    public MusicAdapter(Context context, int resourceID, List<Music> objects){
        super(context,resourceID,objects);
        resourceId=resourceID;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Music music=getItem(position);
        View view;
        ViewHolder viewHolder;
        if (convertView==null){
            view= LayoutInflater.from(getContext()).inflate(resourceId,parent,false);
            viewHolder=new ViewHolder();
            viewHolder.musicAuthorView=(TextView)view.findViewById(R.id.listview_musicauthor);
            viewHolder.musicNameView=(TextView)view.findViewById(R.id.listview_musicname);
            view.setTag(viewHolder);
        }else {
            view=convertView;
            viewHolder=(ViewHolder)view.getTag();
        }
        String musicName=music.getMusicName();
        if (musicName.length()>7){
            musicName=musicName.substring(0,6);
            musicName+="...";
        }
        viewHolder.musicNameView.setText(musicName);
        viewHolder.musicAuthorView.setText(music.getAuthor());
        return view;
    }

    class ViewHolder{
        TextView musicNameView;
        TextView musicAuthorView;
    }
}
