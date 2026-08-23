package ntu.nguyenhoangphuc.personal_diary.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

// MainActivity đang nằm ở package gốc (ntu.nguyenhoangphuc.personal_diary),
// còn SplashActivity nằm trong package con "activity" -> khác package nhau
// nên BẮT BUỘC phải import, không thể gọi thẳng tên lớp như khi cùng package.
import ntu.nguyenhoangphuc.personal_diary.MainActivity;
import ntu.nguyenhoangphuc.personal_diary.R;

public class SplashActivity extends AppCompatActivity {

    // Thời gian hiện splash, tính bằng mili giây (đã chốt: 1.5 giây)
    private static final int SPLASH_DURATION_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Đợi SPLASH_DURATION_MS rồi mới mở màn hình chính
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // TODO (QUAN TRỌNG): DiaryListActivity chưa được viết ở thời điểm này,
                // nên tạm thời trỏ về MainActivity (Hello World mẫu) để build/chạy thử được ngay.
                // Khi viết xong DiaryListActivity (cùng package "activity" -> không cần import thêm),
                // đổi dòng dưới thành: new Intent(SplashActivity.this, DiaryListActivity.class)
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // đóng SplashActivity để người dùng không bấm Back quay lại splash được
            }
        }, SPLASH_DURATION_MS);
    }
}