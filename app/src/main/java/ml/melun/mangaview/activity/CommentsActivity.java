package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.CommentsAdapter;
import ml.melun.mangaview.fragment.CommentsTabFragment;
import ml.melun.mangaview.mangaview.Comment;
import ml.melun.mangaview.mangaview.Login;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.writeComment;

// 댓글 화면을 담당하는 액티비티
public class CommentsActivity extends AppCompatActivity {

  /** The {@link ViewPager} that will host the section contents. */
  private ViewPager mViewPager; // 섹션 내용을 호스팅할 ViewPager

  ArrayList<Comment> comments, bcomments; // 일반 댓글 목록, 베스트 댓글 목록
  public CommentsAdapter adapter, badapter; // 일반 댓글 어댑터, 베스트 댓글 어댑터
  Context context; // 액티비티 컨텍스트
  TabLayout tab; // 탭 레이아웃
  int id; // 현재 만화 ID
  ImageButton submit; // 댓글 제출 버튼
  EditText input; // 댓글 입력 필드

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (p.getDarkTheme()) setTheme(R.style.AppThemeDarkNoTitle);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_comments);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    context = this;
    Intent intent = getIntent();
    tab = this.findViewById(R.id.tab_layout);

    String gsonData = intent.getStringExtra("comments");
    if (gsonData != null && gsonData.length() > 0) {
      Gson gson = new Gson();
      comments = gson.fromJson(gsonData, new TypeToken<ArrayList<Comment>>() {}.getType());
      if (comments == null) comments = new ArrayList<>();
      adapter = new CommentsAdapter(context, comments);
    } else {
      comments = new ArrayList<>();
      adapter = new CommentsAdapter(context, comments);
    }

    gsonData = intent.getStringExtra("bestComments");
    if (gsonData != null && gsonData.length() > 0) {
      Gson gson = new Gson();
      bcomments = gson.fromJson(gsonData, new TypeToken<ArrayList<Comment>>() {}.getType());
      if (bcomments == null) bcomments = new ArrayList<>();
      badapter = new CommentsAdapter(context, bcomments);
    } else {
      bcomments = new ArrayList<>();
      badapter = new CommentsAdapter(context, bcomments);
    }
    
    // 총 댓글 수를 계산해서 제목에 표시
    int totalComments = comments.size() + bcomments.size();
    if (totalComments > 0) {
      getSupportActionBar().setTitle("댓글 " + totalComments + "개");
    } else {
      getSupportActionBar().setTitle("댓글 없음");
    }

    id = intent.getIntExtra("id", 0);

    SectionsPagerAdapter mSectionsPagerAdapter =
        new SectionsPagerAdapter(getSupportFragmentManager());

    // Set up the ViewPager with the sections adapter.
    mViewPager = findViewById(R.id.container);
    mViewPager.setAdapter(mSectionsPagerAdapter);
    mViewPager.requestFocus();

    tab.addTab(tab.newTab().setText("전체 댓글"));
    tab.addTab(tab.newTab().setText("베스트 댓글"));

    mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tab));
    tab.addOnTabSelectedListener(
        new TabLayout.OnTabSelectedListener() {
          @Override
          public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setCurrentItem(tab.getPosition());
          }

          @Override
          public void onTabUnselected(TabLayout.Tab tab) {
            //
          }

          @Override
          public void onTabReselected(TabLayout.Tab tab) {
            //
          }
        });

    submit = this.findViewById(R.id.commentButton);
    input = this.findViewById(R.id.comment_editText);
    final Login login = p.getLogin();
    if (login != null && login.isValid()) {
      submit.setOnClickListener(
          view -> {
            if (input.length() > 0) {
              submit.setEnabled(false);
              input.setEnabled(false);
              new submitComment(login, id, input.getText().toString(), p.getUrl())
                  .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
          });
    } else {
      this.findViewById(R.id.comment_input).setVisibility(View.GONE);
    }
  }

  public class SectionsPagerAdapter extends FragmentPagerAdapter {

    public SectionsPagerAdapter(FragmentManager fm) {
      super(fm);
    }

    @Override
    @NonNull
    public Fragment getItem(int position) {
      CommentsTabFragment tab = new CommentsTabFragment();
      switch (position) {
        case 1:
          // best
          tab.setAdapter(badapter);
          return tab;
        default:
          // comments
          tab.setAdapter(adapter);
          return tab;
      }
    }

    @Override
    public int getCount() {
      // Show 3 total pages.
      return 2;
    }
  }

  private class submitComment extends AsyncTask<Void, Void, Integer> {
    Login login;
    int id;
    String baseUrl;
    String content;

    public submitComment(Login login, int id, String content, String baseUrl) {
      this.login = login;
      this.id = id;
      this.content = content;
      this.baseUrl = baseUrl;
    }

    @Override
    protected void onPreExecute() {
      super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Integer integer) {
      super.onPostExecute(integer);
      if (integer == 0) {
        // success
        // update login
        p.setLogin(login);
        comments.add(new Comment("나", "", "", content, 0, 0, 0));
        adapter.notifyDataSetChanged();
        Toast.makeText(context, "댓글 등록 성공", Toast.LENGTH_SHORT).show();
        input.getText().clear();
      } else {
        Toast.makeText(context, "실패", Toast.LENGTH_SHORT).show();
        // failed
      }
      submit.setEnabled(true);
      input.setEnabled(true);
    }

    @Override
    protected Integer doInBackground(Void... voids) {
      if (writeComment(httpClient, login, id, content, baseUrl)) return 0;
      else {
        // login again and try again
        // login.submit(httpClient);
        if (writeComment(httpClient, login, id, content, baseUrl)) return 0;
      }
      return 1;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      Intent settingIntent = new Intent(this, SettingsActivity.class);
      startActivityForResult(settingIntent, 0);
      return true;
    } else if (id == R.id.action_debug) {
      Intent debug = new Intent(this, DebugActivity.class);
      startActivity(debug);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
