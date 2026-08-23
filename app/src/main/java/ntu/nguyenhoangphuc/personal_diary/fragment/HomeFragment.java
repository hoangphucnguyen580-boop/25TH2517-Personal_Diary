package ntu.nguyenhoangphuc.personal_diary.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.adapter.DiaryAdapter;
import ntu.nguyenhoangphuc.personal_diary.database.DiaryDatabaseHelper;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryEntry;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerDiary;
    private EditText editSearch;
    private TextView textStreak;
    private View bannerOnThisDay;
    private View bannerPin;
    private FloatingActionButton fabAddEntry;

    // ↓↓↓ MỚI THÊM - field để giữ tham chiếu tới DB và Adapter, dùng lại được
    //     ở các hàm khác trong fragment này (không chỉ trong onViewCreated)
    private DiaryDatabaseHelper dbHelper;
    private DiaryAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Toolbar tự vẽ ô tìm kiếm sẵn trong layout XML, ở đây chỉ cần gắn thêm
        // menu ⋮ (Xuất .txt, Sắp xếp) - MaterialToolbar tự hiện icon overflow luôn.
        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_diary_list);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_export_txt) {
                // TODO: gọi hàm xuất file .txt khi tới lượt làm tính năng này
                return true;
            } else if (id == R.id.action_sort) {
                // TODO: hiện menu chọn kiểu sắp xếp khi tới lượt làm tính năng này
                return true;
            }
            return false;
        });

        editSearch = view.findViewById(R.id.edit_search);
        textStreak = view.findViewById(R.id.text_streak);
        bannerOnThisDay = view.findViewById(R.id.banner_on_this_day);
        bannerPin = view.findViewById(R.id.banner_pin);
        recyclerDiary = view.findViewById(R.id.recycler_diary);
        fabAddEntry = view.findViewById(R.id.fab_add_entry);

        // Ẩn banner "Ngày này năm xưa" tạm thời vì chưa có logic kiểm tra ngày trùng.
        // Ẩn cả banner lẫn chấm ghim (bannerPin) - nếu chỉ ẩn banner mà quên chấm ghim,
        // chấm đó vẫn còn đứng lơ lửng trên màn hình vì nó là View riêng, không nằm trong banner.
        bannerOnThisDay.setVisibility(View.GONE);
        bannerPin.setVisibility(View.GONE);

        // ↓↓↓ MỚI THÊM - thay cho 2 dòng TODO cũ (gắn Adapter + load dữ liệu thật)
        dbHelper = new DiaryDatabaseHelper(requireContext());
        recyclerDiary.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<DiaryEntry> danhSachNhatKy = dbHelper.getAllDiaries();
        adapter = new DiaryAdapter(requireContext(), danhSachNhatKy, dbHelper);
        adapter.setOnItemClickListener(entry -> {
            // TODO: mở AddEditDiaryActivity ở chế độ SỬA khi màn đó viết xong,
            //       truyền entry.getId() qua Intent để biết đang sửa bài nào
        });
        recyclerDiary.setAdapter(adapter);

        // TODO: load textStreak thật + bật lại bannerOnThisDay/bannerPin khi có
        //       bài trùng ngày/tháng năm trước (việc #3, chưa làm - cần hàm tính
        //       streak trong DiaryDatabaseHelper trước, hiện chưa có)

        fabAddEntry.setOnClickListener(v -> {
            // TODO: mở AddEditDiaryActivity khi màn đó viết xong
        });
    }
}