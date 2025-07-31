package ml.melun.mangaview;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import ml.melun.mangaview.mangaview.CustomHttpClient;



//@AcraCore(reportContent = { APP_VERSION_NAME, ANDROID_VERSION, PHONE_MODEL, STACK_TRACE, REPORT_ID})


// 앱의 전역 상태를 관리하고 초기화하는 Application 클래스
public class MainApplication extends MultiDexApplication {
    public static CustomHttpClient httpClient; // 전역 HTTP 클라이언트
    public static Preference p; // 전역 환경설정 객체

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        System.out.println("main app start");
        // ACRA (크래시 리포팅 라이브러리) 초기화
        ACRA.init(this, new CoreConfigurationBuilder()
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withPluginConfigurations(
                        new MailSenderConfigurationBuilder().withMailTo("mangaview@protonmail.com").build(),
                        new DialogConfigurationBuilder()
                                .withTitle("MangaView")
                                .withText(getResources().getText(R.string.acra_dialog_text).toString())
                                .withPositiveButtonText("확인")
                                .withNegativeButtonText("취소")
                                .build()
                ));
    }

    @Override
    public void onCreate() {
        // 벡터 드로어블 호환성 활성화
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        p = new Preference(this); // 환경설정 객체 초기화
        httpClient = new CustomHttpClient(); // HTTP 클라이언트 객체 초기화
        super.onCreate();
    }
}
