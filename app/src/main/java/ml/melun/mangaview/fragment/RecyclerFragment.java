package ml.melun.mangaview.fragment;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ml.melun.mangaview.ui.NpaLinearLayoutManager;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.TitleAdapter;
import ml.melun.mangaview.mangaview.MTitle;
import ml.melun.mangaview.mangaview.Manga;
import ml.melun.mangaview.mangaview.Title;

import static android.app.Activity.RESULT_OK;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.CODE_SCOPED_STORAGE;
import static ml.melun.mangaview.Utils.deleteRecursive;
import static ml.melun.mangaview.Utils.episodeIntent;
import static ml.melun.mangaview.Utils.filterFolder;
import static ml.melun.mangaview.Utils.readFileToString;
import static ml.melun.mangaview.Utils.readUriToString;
import static ml.melun.mangaview.Utils.showPopup;
import static ml.melun.mangaview.Utils.viewerIntent;

// 최근, 즐겨찾기, 다운로드 목록을 표시하는 재사용 가능한 프래그먼트
public class RecyclerFragment extends Fragment {
    int selectedPosition = -1; // 사용자가 선택한 아이템의 위치
    TitleAdapter titleAdapter; // RecyclerView 어댑터
    RecyclerView recyclerView;
    int mode = -1; // 현재 프래그먼트의 모드 (최근, 즐겨찾기, 다운로드)
    boolean loaded = false; // 뷰가 로드되었는지 여부
    SearchView searchView; // 검색 뷰


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // 상태 저장
        outState.putInt("mode", mode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true); // 옵션 메뉴 사용 설정
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.content_recycler , container, false);
        recyclerView = rootView.findViewById(R.id.recycler_list);
        titleAdapter = new TitleAdapter(getContext());
        recyclerView.setLayoutManager(new NpaLinearLayoutManager(getContext()));
        recyclerView.setAdapter(titleAdapter);

        // 어댑터에 클릭 리스너 설정
        titleAdapter.setClickListener(new TitleAdapter.ItemClickListener() {
            @Override
            public void onResumeClick(int position, int id) {
                // 이어보기 버튼 클릭 시
                selectedPosition = position;
                if(mode == R.id.nav_recent || mode == R.id.nav_favorite) {
                    openViewer(new Manga(id, "", "" , titleAdapter.getItem(position).getBaseMode()), 2);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                // 아이템 롱클릭 시 팝업 메뉴 표시
                Title title = titleAdapter.getItem(position);
                if(mode == R.id.nav_favorite) {
                    popup(view, position, title, 2);
                }else if(mode == R.id.nav_recent){
                    popup(view, position, title, 1);
                }else if(mode == R.id.nav_download){
                    popup(view, position, title,3);
                }
            }

            @Override
            public void onItemClick(int position) {
                // 아이템 클릭 시 에피소드 목록 화면으로 이동
                selectedPosition = position;
                Intent episodeView = episodeIntent(getContext(), titleAdapter.getItem(position));
                if(mode == R.id.nav_favorite) {
                    episodeView.putExtra("position", position);
                    episodeView.putExtra("favorite",true);
                    startActivityForResult(episodeView,1);
                }else if(mode == R.id.nav_recent) {
                    episodeView.putExtra("recent",true);
                    startActivityForResult(episodeView,2);
                }else if(mode == R.id.nav_download) {
                    episodeView.putExtra("online", false);
                    startActivity(episodeView);
                }
            }
        });

        // 저장된 상태 복원
        if(savedInstanceState != null){
            mode = savedInstanceState.getInt("mode");
        }
        if(mode > -1) {
            loaded = true;
            changeMode(mode);
        }
        return rootView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 다른 액티비티에서 돌아왔을 때의 결과 처리
        if(resultCode == RESULT_OK){
            if(titleAdapter != null && titleAdapter.getItemCount() > 0 && selectedPosition > -1) {
                switch (requestCode) {
                    case 1: // 즐겨찾기
                        boolean favorite_after = data.getBooleanExtra("favorite", true);
                        if (!favorite_after) // 즐겨찾기에서 해제되었다면 목록에서 제거
                            titleAdapter.remove(selectedPosition);
                        break;
                    case 2: // 최근 본 만화
                        // 맨 위로 이동
                        titleAdapter.moveItemToTop(selectedPosition);
                        break;

                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mode = -1;
        loaded = false;
    }

    // 표시할 데이터의 종류를 변경 (최근, 즐겨찾기, 다운로드)
    public void changeMode(int id){
        mode = id;
        if(!loaded)
            return;
        recyclerView.scrollToPosition(0);
        if(searchView != null){
            searchView.clearFocus();
            searchView.setQuery("", false);
        }
        switch(id){
            case R.id.nav_recent:
                titleAdapter.setResume(true);
                titleAdapter.setForceThumbnail(false);
                titleAdapter.setData(p.getRecent());
                break;
            case R.id.nav_favorite:
                titleAdapter.setResume(true);
                titleAdapter.setForceThumbnail(false);
                titleAdapter.setData(p.getFavorite());
                break;
            case R.id.nav_download:
                titleAdapter.setResume(false);
                titleAdapter.setForceThumbnail(true);
                titleAdapter.clearData();
                new OfflineReader().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
        }
    }


    // 오프라인 저장된 만화 목록을 읽어오는 AsyncTask
    public class OfflineReader extends AsyncTask<Void,Void,Integer>{
        List<Title> titles;
        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            titleAdapter.addData(titles);
        }
        @Override
        protected Integer doInBackground(Void... voids) {
            titles = new ArrayList<>();
            // Scoped Storage (Android 10 이상) 대응
            if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                Uri uri = Uri.parse(p.getHomeDir());
                DocumentFile home;
                try {
                    home = DocumentFile.fromTreeUri(getContext(), uri);
                }catch (IllegalArgumentException e){
                    return null;
                }
                if(home != null && home.canRead()){
                    for(DocumentFile f : home.listFiles()){
                        if(f.isDirectory()) {
                            DocumentFile d = f.findFile("title.gson");
                            if (d != null) {
                                try {
                                    // title.gson 파일에서 작품 정보 읽기
                                    Title title = new Gson().fromJson(readUriToString(getContext(), d.getUri()), new TypeToken<Title>() {}.getType());
                                    title.setPath(f.getUri().toString());
                                    if (title.getThumb().length() > 0) {
                                        DocumentFile t = f.findFile(title.getThumb());
                                        if (t != null) title.setThumb(t.getUri().toString());
                                    }
                                    titles.add(title);
                                } catch (Exception e) {
                                    // 실패 시 폴더 이름으로 기본 Title 객체 생성
                                    Title title = new Title(f.getName(), "", "", new ArrayList<>(), "", 0, MTitle.base_auto);
                                    title.setPath(f.getUri().toString());
                                    titles.add(title);
                                }
                            } else {
                                Title title = new Title(f.getName(), "", "", new ArrayList<>(), "", 0, MTitle.base_auto);
                                title.setPath(f.getUri().toString());
                                titles.add(title);
                            }
                        }
                    }
                }

            }else { // 구버전 안드로이드
                File homeDir = new File(p.getHomeDir());
                if (homeDir.exists()) {
                    File[] files = homeDir.listFiles();
                    if(files == null)
                        return null;
                    for (File f : files) {
                        if (f.isDirectory()) {
                            File data = new File(f, "title.gson");
                            if (data.exists()) {
                                try {
                                    Title title = new Gson().fromJson(readFileToString(data), new TypeToken<Title>() {}.getType());
                                    title.setPath(f.getAbsolutePath());
                                    if (title.getThumb().length() > 0)
                                        title.setThumb(f.getAbsolutePath() + '/' + title.getThumb());
                                    titles.add(title);
                                } catch (Exception e) {
                                    Title title = new Title(f.getName(), "", "", new ArrayList<>(), "", 0, MTitle.base_auto);
                                    title.setPath(f.getAbsolutePath());
                                    titles.add(title);
                                }

                            } else {
                                Title title = new Title(f.getName(), "", "", new ArrayList<>(), "", 0, MTitle.base_auto);
                                title.setPath(f.getAbsolutePath());
                                titles.add(title);
                            }
                        }
                    }
                }
            }
            return null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // 검색 메뉴 설정
        inflater.inflate(R.menu.search_menu, menu);
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.filter_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setQueryHint("검색");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                titleAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                titleAdapter.getFilter().filter(query);
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return item.getItemId() == R.id.filter_search;
    }

    // 뷰어 액티비티를 실행하는 헬퍼 메서드
    void openViewer(Manga manga, int code){
        Intent viewer = viewerIntent(getContext(),manga);
        viewer.putExtra("online",true);
        startActivityForResult(viewer, code);
    }

    // 롱클릭 시 나타나는 팝업 메뉴를 생성하고 표시하는 메서드
    void popup(View view, final int position, final Title title, final int m){
        PopupMenu popup = new PopupMenu(getContext(), view);
        popup.getMenuInflater().inflate(R.menu.title_options, popup.getMenu());

        // 모드(m)에 따라 메뉴 아이템의 표시 여부 설정
        switch(m){
            case 1: // 최근
                popup.getMenu().findItem(R.id.del).setVisible(true);
            case 0: // 검색 (사용 안함)
                popup.getMenu().findItem(R.id.favAdd).setVisible(true);
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                break;
            case 2: // 즐겨찾기
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                break;
            case 3: // 다운로드
                popup.getMenu().findItem(R.id.favAdd).setVisible(true);
                popup.getMenu().findItem(R.id.favDel).setVisible(true);
                popup.getMenu().findItem(R.id.remove).setVisible(true);
                break;
        }

        // 즐겨찾기 상태에 따라 '추가' 또는 '삭제' 메뉴만 표시
        if(m!=2) {
            if (p.findFavorite(title) > -1) popup.getMenu().removeItem(R.id.favAdd);
            else popup.getMenu().removeItem(R.id.favDel);
        }

        // 팝업 메뉴 아이템 클릭 리스너
        popup.setOnMenuItemClickListener(item -> {
            switch(item.getItemId()){
                case R.id.del: // 최근 목록에서 삭제
                    titleAdapter.remove(position);
                    p.removeRecent(position);
                    break;
                case R.id.favAdd:
                case R.id.favDel: // 즐겨찾기 토글
                    p.toggleFavorite(title,0);
                    if(m==2){ // 즐겨찾기 탭에서는 목록에서 바로 제거
                        titleAdapter.remove(position);
                    }
                    break;
                case R.id.remove: // 다운로드한 만화 삭제
                    DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // Scoped Storage 대응
                            if (Build.VERSION.SDK_INT >= CODE_SCOPED_STORAGE) {
                                DocumentFile f = DocumentFile.fromTreeUri(getContext(), Uri.parse(p.getHomeDir()));
                                DocumentFile target = f.findFile(title.getName());
                                if (target != null && target.delete()) {
                                    titleAdapter.remove(position);
                                    Toast.makeText(getContext(), "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                } else showPopup(getContext(), "알림", "삭제를 실패했습니다");
                            } else {
                                File folder = new File(p.getHomeDir(), filterFolder(title.getName()));
                                if (deleteRecursive(folder)) {
                                    titleAdapter.remove(position);
                                    Toast.makeText(getContext(), "삭제가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                } else showPopup(getContext(), "알림", "삭제를 실패했습니다");
                            }
                        }
                    };
                    AlertDialog.Builder builder;
                    if(p.getDarkTheme()) builder = new AlertDialog.Builder(getContext(),R.style.darkDialog);
                    else builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("정말로 삭제 하시겠습니까?").setPositiveButton("네", dialogClickListener)
                            .setNegativeButton("아니오", dialogClickListener).show();
                    break;
            }
            return false;
        });
        popup.show();
    }
}
