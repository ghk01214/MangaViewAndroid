package ml.melun.mangaview.adapter;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.R;

/**
 * 소설 텍스트를 RecyclerView에 표시하기 위한 어댑터
 */
public class NovelTextAdapter extends RecyclerView.Adapter<NovelTextAdapter.NovelTextViewHolder> {
    
    private List<String> paragraphs;
    private int textSize = 16; // 기본 텍스트 크기
    private Runnable clickListener; // 클릭 리스너
    
    public NovelTextAdapter() {
        this.paragraphs = new ArrayList<>();
    }
    
    public void setContent(String content) {
        paragraphs.clear();
        if (content != null && !content.trim().isEmpty()) {
            // 텍스트를 문단별로 분리
            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (!trimmedLine.isEmpty()) {
                    paragraphs.add(trimmedLine);
                }
            }
        }
        notifyDataSetChanged();
    }
    
    public void setTextSize(int textSize) {
        this.textSize = textSize;
        notifyDataSetChanged();
    }
    
    public void setClickListener(Runnable clickListener) {
        this.clickListener = clickListener;
    }
    
    @NonNull
    @Override
    public NovelTextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_novel_text, parent, false);
        return new NovelTextViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NovelTextViewHolder holder, int position) {
        String paragraph = paragraphs.get(position);
        holder.textView.setText(Html.fromHtml(paragraph, Html.FROM_HTML_MODE_COMPACT));
        holder.textView.setTextSize(textSize);
        
        // 다크모드 감지 및 색상 적용
        Context context = holder.itemView.getContext();
        boolean isDarkMode = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        
        if (isDarkMode) {
            // 다크모드: 어두운 배경에 밝은 텍스트
            holder.itemView.setBackgroundColor(Color.parseColor("#1A1A1A")); // 어두운 회색
            holder.textView.setTextColor(Color.parseColor("#E0E0E0")); // 밝은 회색 텍스트
        } else {
            // 라이트모드: 밝은 배경에 어두운 텍스트
            holder.itemView.setBackgroundColor(Color.parseColor("#FFFFFF")); // 흰색
            holder.textView.setTextColor(Color.parseColor("#2C2C2C")); // 어두운 회색 텍스트
        }
        
        // 텍스트 선택 비활성화
        holder.textView.setTextIsSelectable(false);
        holder.textView.setLongClickable(false);
        
        // 클릭 리스너 설정 (툴바 토글용)
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.run();
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return paragraphs.size();
    }
    
    // 텍스트 뷰홀더
    static class NovelTextViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        
        NovelTextViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.novelTextView);
        }
    }
}