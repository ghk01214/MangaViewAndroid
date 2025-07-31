package ml.melun.mangaview.adapter;

import android.content.Context;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;


import java.util.ArrayList;

import ml.melun.mangaview.R;
import ml.melun.mangaview.mangaview.Comment;

import static ml.melun.mangaview.MainApplication.p;

// 댓글 목록을 표시하는 BaseAdapter
public class CommentsAdapter extends BaseAdapter {
    Context context;
    ArrayList<Comment> data; // 댓글 데이터 리스트
    LayoutInflater inflater;
    boolean dark; // 다크 테마 여부
    boolean save; // 데이터 절약 모드 여부

    public CommentsAdapter(Context context, ArrayList<Comment> data) {
        super();
        this.dark = p.getDarkTheme();
        this.save = p.getDataSave();
        this.context = context;
        this.data = data;
        this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            convertView = inflater.inflate(R.layout.item_comment,parent,false);
        }
        Comment c = data.get(position);
        ConstraintLayout layout = convertView.findViewById(R.id.comment_layout);
        ImageView icon = convertView.findViewById(R.id.comment_icon);
        TextView content = convertView.findViewById(R.id.comment_content);
        TextView timeStamp = convertView.findViewById(R.id.comment_time);
        TextView user = convertView.findViewById(R.id.comment_user);
        TextView likes = convertView.findViewById(R.id.comment_likes);
        TextView level = convertView.findViewById(R.id.comment_level);

        // 댓글 들여쓰기 설정
        layout.setPadding(60*c.getIndent(),0,0,0);
        // 아이콘 로드 (데이터 절약 모드가 아닐 때만)
        if(c.getIcon().length()>1 && !save) Glide.with(icon).load(c.getIcon()).into(icon);
        else icon.setImageResource(R.drawable.user);
        content.setText(c.getContent()); // 댓글 내용 설정
        timeStamp.setText(c.getTimestamp()); // 작성 시간 설정
        user.setText(c.getUser()); // 작성자 이름 설정
        level.setText(String.valueOf(c.getLevel())); // 작성자 레벨 설정
        // 좋아요 수 설정
        if(c.getLikes()>0) likes.setText(String.valueOf(c.getLikes()));
        else likes.setText("");
        return convertView;
    }

    @Override
    public Comment getItem(int position) {
        return data.get(position);
    }


    @Override
    public long getItemId(int position) {
        return 0;
    }
}

