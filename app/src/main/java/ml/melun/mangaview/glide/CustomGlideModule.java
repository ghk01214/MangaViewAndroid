package ml.melun.mangaview.glide;

import android.content.Context;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

// Glide 라이브러리의 설정을 커스터마이징하는 클래스
@GlideModule
public class CustomGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        // Glide의 기본 HTTP 통신 라이브러리를 앱에서 사용하는 CustomHttpClient(OkHttp)로 교체합니다.
        // 이를 통해 이미지 로딩 시에도 앱의 쿠키 및 TLS 설정을 동일하게 사용할 수 있습니다.
        System.out.println("glide module create");
        registry.replace(GlideUrl.class, InputStream.class, new OkHttpUrlLoader.Factory(httpClient.client));
    }

    // 매니페스트 파싱을 비활성화하여 초기 로딩 속도를 개선하고, 잠재적인 충돌을 방지합니다.
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
