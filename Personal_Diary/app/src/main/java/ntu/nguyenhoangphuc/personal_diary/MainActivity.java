package ntu.nguyenhoangphuc.personal_diary;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import ntu.nguyenhoangphuc.personal_diary.fragment.CalendarFragment;
import ntu.nguyenhoangphuc.personal_diary.fragment.HomeFragment;
import ntu.nguyenhoangphuc.personal_diary.fragment.MemoriesFragment;
import ntu.nguyenhoangphuc.personal_diary.fragment.StatsFragment;

public class MainActivity extends AppCompatActivity {

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

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment target;

            if (item.getItemId() == R.id.nav_home) {
                target = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_calendar) {
                target = new CalendarFragment();
            } else if (item.getItemId() == R.id.nav_memories) {
                target = new MemoriesFragment();
            } else {
                target = new StatsFragment();
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, target)
                    .commit();
            return true;
        });
    }
}