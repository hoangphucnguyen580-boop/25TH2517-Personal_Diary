package ntu.nguyenhoangphuc.personal_diary;

import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import ntu.nguyenhoangphuc.personal_diary.fragment.CalendarFragment;
import ntu.nguyenhoangphuc.personal_diary.fragment.HomeFragment;
import ntu.nguyenhoangphuc.personal_diary.fragment.MemoriesFragment;
import ntu.nguyenhoangphuc.personal_diary.fragment.StatsFragment;

public class MainActivity extends AppCompatActivity {

    // Đúng bằng số item khai trong bottom_nav_menu.xml - dùng để chia đều
    // chiều rộng bottom_nav ra từng "ô" cho ruy băng nhảy tới
    private static final int SO_TAB_BOTTOM_NAV = 4;

    // Thời lượng animation trượt ngang, tính bằng mili giây
    private static final int THOI_LUONG_TRUOT_RUY_BANG_MS = 250;

    // Giữ làm field (thay vì biến local trong onCreate) vì cả 2 View này còn
    // cần dùng lại ở hàm diChuyenRuyBangToiTab() bên dưới, nằm ngoài onCreate
    private BottomNavigationView bottomNav;
    private View ribbonIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Chỉ load fragment mặc định khi Activity vừa tạo lần đầu (savedInstanceState == null).
        // Nếu bỏ điều kiện này, mỗi lần xoay màn hình (Activity bị Android tạo lại)
        // sẽ chồng thêm 1 HomeFragment mới đè lên fragment đang hiện, gây lỗi hiển thị.
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        bottomNav = findViewById(R.id.bottom_nav);
        ribbonIndicator = findViewById(R.id.ribbon_indicator);

        // MỚI - đợi bottomNav có kích thước THẬT (sau khi layout xong) rồi mới
        // đặt đúng vị trí ban đầu cho ruy băng (tab 0 = Trang chủ, vì đây là
        // fragment mặc định lúc mới mở app). Gọi thẳng trong onCreate sẽ bị
        // bottomNav.getWidth() = 0 vì View chưa được đo đạc xong - giống hệt lý
        // do resetZoom() trong ZoomableImageView phải đợi onLayoutChange
        bottomNav.post(() -> diChuyenRuyBangToiTab(0, false));

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment target;
            int viTriTab;

            if (item.getItemId() == R.id.nav_home) {
                target = new HomeFragment();
                viTriTab = 0;
            } else if (item.getItemId() == R.id.nav_calendar) {
                target = new CalendarFragment();
                viTriTab = 1;
            } else if (item.getItemId() == R.id.nav_memories) {
                target = new MemoriesFragment();
                viTriTab = 2;
            } else {
                target = new StatsFragment();
                viTriTab = 3;
            }

            diChuyenRuyBangToiTab(viTriTab, true);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, target)
                    .commit();
            return true;
        });
    }

    // Di chuyển ruy băng tới đúng vị trí GIỮA tab thứ viTriTab (0=Trang chủ,
    // 1=Lịch, 2=Kỷ niệm, 3=Thống kê - đúng thứ tự khai trong bottom_nav_menu.xml).
    // Tính bằng cách chia đều chiều rộng bottom_nav cho 4 tab thay vì lấy toạ độ
    // thật của từng icon qua reflection - đơn giản, dễ đọc, không phụ thuộc vào
    // cấu trúc view nội bộ của thư viện Material (dễ vỡ khi update thư viện).
    //
    // coAnimation=true -> trượt ngang mượt (dùng khi mày CHỦ ĐỘNG bấm đổi tab).
    // coAnimation=false -> nhảy thẳng tới vị trí luôn (chỉ dùng đúng 1 lần lúc
    // mới mở app, không animation vì lúc đó chưa cần "hiệu ứng" gì cả).
    private void diChuyenRuyBangToiTab(int viTriTab, boolean coAnimation) {
        float chieuRongMoiTab = bottomNav.getWidth() / (float) SO_TAB_BOTTOM_NAV;
        float tamTabX = chieuRongMoiTab * viTriTab + chieuRongMoiTab / 2f;
        float diChuyenToiX = tamTabX - ribbonIndicator.getWidth() / 2f;

        if (coAnimation) {
            ribbonIndicator.animate()
                    .translationX(diChuyenToiX)
                    .setDuration(THOI_LUONG_TRUOT_RUY_BANG_MS)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        } else {
            // Lần đầu mở app - set thẳng vị trí rồi MỚI hiện ruy băng lên (nó
            // đang để visibility="invisible" sẵn trong XML), tránh bị "giật
            // hình" hiện tạm ở góc trái (x=0, vị trí neo gốc trong XML) trong
            // 1 khung hình trước khi tính xong toạ độ đúng
            ribbonIndicator.setTranslationX(diChuyenToiX);
            ribbonIndicator.setVisibility(View.VISIBLE);
        }
    }
}