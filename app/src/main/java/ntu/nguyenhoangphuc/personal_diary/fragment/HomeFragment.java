package ntu.nguyenhoangphuc.personal_diary.fragment;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.adapter.DiaryAdapter;
import ntu.nguyenhoangphuc.personal_diary.database.DiaryDatabaseHelper;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryEntry;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryPhoto;
import ntu.nguyenhoangphuc.personal_diary.activity.AddEditDiaryActivity;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerDiary;
    private EditText editSearch;
    private TextView textStreak;
    private View bannerOnThisDay;
    private View bannerPin;
    private TextView bannerTitle;
    private ImageView bannerThumb;
    private TextView bannerExcerpt;
    private FloatingActionButton fabAddEntry;

    private DiaryDatabaseHelper dbHelper;
    private DiaryAdapter adapter;

    private DiaryEntry baiNgayNayNamXua;

    // MỚI - danh sách tên thẻ đang được CHỌN để lọc (rỗng = không lọc theo thẻ,
    // hiện tất cả). Giữ ở field vì cần dùng lại mỗi lần search text đổi HOẶC
    // mỗi lần bấm Áp dụng lọc trong bottom sheet.
    private final List<String> danhSachTheDangLoc = new ArrayList<>();

    // MỚI - true = mới nhất trước (mặc định, khớp đúng hành vi cũ trước khi có
    // tính năng Sắp xếp), false = cũ nhất trước. Giữ ở field vì cần dùng lại
    // mỗi lần apDungTimKiemVaLoc() được gọi, giống hệt cách danhSachTheDangLoc
    // đang được giữ cho bộ lọc thẻ
    private boolean sapXepMoiNhatTruoc = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_diary_list);
        toolbar.setOverflowIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_vert));
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_export_txt) {
                xuatFileTxt();
                return true;
            } else if (id == R.id.action_sort) {
                moDialogSapXep();
                return true;
            } else if (id == R.id.action_loc_tag) {
                moBottomSheetLocTag();
                return true;
            }
            return false;
        });

        editSearch = view.findViewById(R.id.edit_search);
        textStreak = view.findViewById(R.id.text_streak);
        bannerOnThisDay = view.findViewById(R.id.banner_on_this_day);
        bannerPin = view.findViewById(R.id.banner_pin);
        bannerTitle = view.findViewById(R.id.banner_title);
        bannerThumb = view.findViewById(R.id.banner_thumb);
        bannerExcerpt = view.findViewById(R.id.banner_excerpt);
        recyclerDiary = view.findViewById(R.id.recycler_diary);
        fabAddEntry = view.findViewById(R.id.fab_add_entry);

        dbHelper = new DiaryDatabaseHelper(requireContext());
        recyclerDiary.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new DiaryAdapter(requireContext(), new ArrayList<>(), dbHelper);
        adapter.setOnItemClickListener(entry -> {
            Intent intent = new Intent(requireContext(), AddEditDiaryActivity.class);
            intent.putExtra(AddEditDiaryActivity.EXTRA_DIARY_ID, entry.getId());
            startActivity(intent);
        });
        recyclerDiary.setAdapter(adapter);

        // MỚI - live search: lọc lại danh sách MỖI KHI mày gõ thêm/xoá 1 ký tự,
        // không cần bấm Enter hay icon tìm kiếm nào cả
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                apDungTimKiemVaLoc();
            }
        });

        bannerOnThisDay.setOnClickListener(v -> {
            if (baiNgayNayNamXua != null) {
                Intent intent = new Intent(requireContext(), AddEditDiaryActivity.class);
                intent.putExtra(AddEditDiaryActivity.EXTRA_DIARY_ID, baiNgayNayNamXua.getId());
                startActivity(intent);
            }
        });

        apDungTimKiemVaLoc(); // load danh sách đầy đủ lần đầu (chưa search/lọc gì)
        capNhatStreakVaBanner();

        fabAddEntry.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditDiaryActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // SỬA: gọi apDungTimKiemVaLoc() thay vì getAllDiaries() thẳng - để nếu
        // mày đang search/lọc dở mà bấm vào 1 bài để Sửa rồi quay lại Home,
        // bộ lọc vẫn được giữ nguyên chứ không bị mất trắng về danh sách đầy đủ
        if (adapter != null && dbHelper != null) {
            apDungTimKiemVaLoc();
            capNhatStreakVaBanner();
        }
    }

    // Gộp CẢ từ khoá đang gõ LẪN danh sách thẻ đang lọc thành 1 lần gọi DB duy
    // nhất, rồi cập nhật lại RecyclerView - gọi mỗi khi 1 trong 2 điều kiện này
    // đổi (gõ search HOẶC bấm Áp dụng/Bỏ lọc trong bottom sheet)
    private void apDungTimKiemVaLoc() {
        if (adapter == null || dbHelper == null) return;
        String tuKhoa = editSearch.getText().toString();
        List<DiaryEntry> ketQua = dbHelper.timKiemVaLoc(tuKhoa, danhSachTheDangLoc, sapXepMoiNhatTruoc);
        adapter.capNhatDanhSach(ketQua);
    }

    // ===================== XUẤT FILE .TXT (MỚI) =====================

    // Xuất TOÀN BỘ nhật ký (KHÔNG theo bộ tìm kiếm/lọc đang áp dụng - đã chốt
    // với mày là xuất hết để tránh hiểu lầm "tưởng đã backup hết" trong khi
    // đang lọc dở 1 thẻ nào đó), rồi mở share intent để mày chọn nơi lưu/gửi -
    // tận dụng lại đúng pattern FileProvider + ACTION_SEND đã dùng cho chia sẻ
    // ảnh/nội dung trong AddEditDiaryActivity, không cần xin thêm quyền lưu trữ
    private void xuatFileTxt() {
        List<DiaryEntry> tatCaBaiViet = dbHelper.getAllDiaries();

        if (tatCaBaiViet.isEmpty()) {
            Toast.makeText(requireContext(), R.string.khong_co_bai_de_xuat, Toast.LENGTH_SHORT).show();
            return;
        }

        String noiDungFile = taoNoiDungFileTxt(tatCaBaiViet);

        try {
            File fileTxt = ghiNoiDungRaFileTxt(noiDungFile);
            Uri uriFile = FileProvider.getUriForFile(requireContext(),
                    requireContext().getPackageName() + ".fileprovider", fileTxt);

            Intent intentChiaSe = new Intent(Intent.ACTION_SEND);
            intentChiaSe.setType("text/plain");
            intentChiaSe.putExtra(Intent.EXTRA_STREAM, uriFile);
            // Cấp quyền đọc file tạm thời cho app nhận share - giống hệt lý do
            // đã giải thích ở chiaSeNhatKy() bên AddEditDiaryActivity
            intentChiaSe.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intentChiaSe, getString(R.string.xuat_file_txt)));

        } catch (IOException e) {
            Toast.makeText(requireContext(), R.string.loi_khi_xuat_file, Toast.LENGTH_SHORT).show();
        }
    }

    // Ghép tất cả bài viết thành 1 đoạn text dài, mỗi bài có ngày/tâm trạng/thẻ/
    // nội dung, phân cách rõ ràng bằng 1 dòng gạch ngang cho dễ đọc khi mở file
    private String taoNoiDungFileTxt(List<DiaryEntry> danhSachBai) {
        SimpleDateFormat dinhDangLuu = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dinhDangHienThi = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        StringBuilder noiDung = new StringBuilder();
        noiDung.append("NHẬT KÝ CÁ NHÂN\n");
        noiDung.append("Xuất ngày: ").append(dinhDangHienThi.format(Calendar.getInstance().getTime())).append("\n");
        noiDung.append("Tổng số bài: ").append(danhSachBai.size()).append("\n");
        noiDung.append("========================================\n\n");

        for (DiaryEntry bai : danhSachBai) {
            String ngayHienThi;
            try {
                ngayHienThi = dinhDangHienThi.format(dinhDangLuu.parse(bai.getNgayThang()));
            } catch (ParseException e) {
                ngayHienThi = bai.getNgayThang();
            }

            noiDung.append("Ngày: ").append(ngayHienThi).append("\n");

            if (bai.getTamTrang() != null && !bai.getTamTrang().isEmpty()) {
                noiDung.append("Tâm trạng: ").append(tenMoodTheoMa(bai.getTamTrang())).append("\n");
            }

            if (bai.getTheGan() != null && !bai.getTheGan().trim().isEmpty()) {
                noiDung.append("Thẻ: ").append(bai.getTheGan().replace(",", ", ")).append("\n");
            }

            noiDung.append("\n").append(bai.getNoiDung()).append("\n");
            noiDung.append("\n----------------------------------------\n\n");
        }

        return noiDung.toString();
    }

    // Đổi mã mood ("happy"/"calm"/...) thành tên tiếng Việt - giống hệt cách
    // AddEditDiaryActivity.nhanTenMoodTheoMa() đang làm. Viết lại riêng ở đây vì
    // 2 class không kế thừa chung, để tránh phải tạo thêm 1 class Util mới chỉ
    // cho 1 hàm nhỏ này (có thể gom lại sau nếu mày thấy trùng lặp nhiều quá)
    private String tenMoodTheoMa(String maMood) {
        switch (maMood) {
            case "happy":
                return getString(R.string.mood_happy_label);
            case "calm":
                return getString(R.string.mood_calm_label);
            case "sad":
                return getString(R.string.mood_sad_label);
            case "angry":
                return getString(R.string.mood_angry_label);
            case "neutral":
            default:
                return getString(R.string.mood_neutral_label);
        }
    }

    // Ghi nội dung xuống file .txt trong cacheDir - dùng cache vì file này chỉ
    // để share tạm thời, không cần giữ mãi (giống hệt cách ảnh chụp Camera tạm
    // đang được xử lý trong AddEditDiaryActivity). Tên file có timestamp để mở
    // xuất nhiều lần không bị đè lên nhau.
    private File ghiNoiDungRaFileTxt(String noiDung) throws IOException {
        File thuMucXuat = new File(requireContext().getCacheDir(), "xuat_nhat_ky");
        if (!thuMucXuat.exists()) {
            thuMucXuat.mkdirs();
        }

        String tenFile = "nhat_ky_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Calendar.getInstance().getTime()) + ".txt";
        File fileTxt = new File(thuMucXuat, tenFile);

        try (FileWriter writer = new FileWriter(fileTxt)) {
            writer.write(noiDung);
        }

        return fileTxt;
    }

    // ===================== SẮP XẾP (MỚI) =====================

    // AlertDialog kiểu radio 2 lựa chọn - bấm chọn 1 cái là áp dụng ngay + đóng
    // dialog luôn, không cần thêm nút "OK" riêng cho gọn. Khác với BottomSheet
    // lọc thẻ (cho chọn NHIỀU thẻ cùng lúc nên cần nút Áp dụng riêng), đây chỉ
    // chọn 1 trong 2 nên chọn phát là xong ngay.
    private void moDialogSapXep() {
        String[] luaChon = {
                getString(R.string.sap_xep_moi_nhat_truoc),
                getString(R.string.sap_xep_cu_nhat_truoc)
        };
        int viTriDangChon = sapXepMoiNhatTruoc ? 0 : 1;

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.sap_xep_tieu_de)
                .setSingleChoiceItems(luaChon, viTriDangChon, (dialog, viTriMoiChon) -> {
                    sapXepMoiNhatTruoc = (viTriMoiChon == 0);
                    apDungTimKiemVaLoc();
                    dialog.dismiss();
                })
                .show();
    }

    // ===================== LỌC THEO THẺ (MỚI) =====================

    // Mở BottomSheetDialog cho mày chọn thẻ muốn lọc - đổ danh sách thẻ ĐÃ TỪNG
    // dùng thật (không phải danh sách cố định) vào ChipGroup, tick sẵn thẻ nào
    // đang lọc từ trước (nếu mày mở lại sau khi đã áp dụng 1 lần)
    private void moBottomSheetLocTag() {
        List<String> danhSachTheDaDung = dbHelper.layDanhSachTheDaSuDung();

        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        sheet.setContentView(R.layout.layout_bottom_sheet_loc_tag);

        ChipGroup chipGroup = sheet.findViewById(R.id.cgTheDeLoc);
        TextView tvChuaCoThe = sheet.findViewById(R.id.tvChuaCoThe);
        Button btnBoLoc = sheet.findViewById(R.id.btnBoLoc);
        Button btnApDungLoc = sheet.findViewById(R.id.btnApDungLoc);

        if (chipGroup == null || btnBoLoc == null || btnApDungLoc == null) return;

        if (danhSachTheDaDung.isEmpty()) {
            chipGroup.setVisibility(View.GONE);
            if (tvChuaCoThe != null) tvChuaCoThe.setVisibility(View.VISIBLE);
        } else {
            chipGroup.setVisibility(View.VISIBLE);
            if (tvChuaCoThe != null) tvChuaCoThe.setVisibility(View.GONE);

            for (String ten : danhSachTheDaDung) {
                Chip chip = new Chip(requireContext());
                chip.setText(ten);
                chip.setCheckable(true);
                // Tick sẵn nếu thẻ này đang nằm trong bộ lọc hiện tại
                chip.setChecked(danhSachTheDangLoc.contains(ten));
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                chip.setChipBackgroundColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_bg_selector));
                chip.setChipStrokeColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_stroke_selector));
                chip.setCheckedIconVisible(true);
                chip.setCheckedIconTint(ContextCompat.getColorStateList(requireContext(), R.color.accent_terracotta));
                chipGroup.addView(chip);
            }
        }

        btnBoLoc.setOnClickListener(v -> {
            danhSachTheDangLoc.clear();
            apDungTimKiemVaLoc();
            sheet.dismiss();
            Toast.makeText(requireContext(), "Đã bỏ lọc thẻ", Toast.LENGTH_SHORT).show();
        });

        btnApDungLoc.setOnClickListener(v -> {
            List<String> theDuocChon = new ArrayList<>();
            for (int i = 0; i < chipGroup.getChildCount(); i++) {
                View con = chipGroup.getChildAt(i);
                if (con instanceof Chip && ((Chip) con).isChecked()) {
                    theDuocChon.add(((Chip) con).getText().toString());
                }
            }
            danhSachTheDangLoc.clear();
            danhSachTheDangLoc.addAll(theDuocChon);
            apDungTimKiemVaLoc();
            sheet.dismiss();

            if (danhSachTheDangLoc.isEmpty()) {
                Toast.makeText(requireContext(), "Chưa chọn thẻ nào - hiện tất cả bài", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(),
                        "Đang lọc theo: " + String.join(", ", danhSachTheDangLoc),
                        Toast.LENGTH_SHORT).show();
            }
        });

        sheet.show();
    }

    // ===================== STREAK + BANNER (không đổi so với trước) =====================

    private void capNhatStreakVaBanner() {
        int soNgayStreak = dbHelper.tinhSoNgayStreak();
        if (soNgayStreak <= 0) {
            textStreak.setText(R.string.streak_chua_co);
        } else {
            textStreak.setText(getString(R.string.streak_so_ngay, soNgayStreak));
        }

        baiNgayNayNamXua = dbHelper.timBaiNgayNayNamXua();
        if (baiNgayNayNamXua == null) {
            bannerOnThisDay.setVisibility(View.GONE);
            bannerPin.setVisibility(View.GONE);
            return;
        }

        bannerOnThisDay.setVisibility(View.VISIBLE);
        bannerPin.setVisibility(View.VISIBLE);

        int namHomNay = Calendar.getInstance().get(Calendar.YEAR);
        int namBaiCu = Integer.parseInt(baiNgayNayNamXua.getNgayThang().substring(0, 4));
        int soNamCachDay = Math.max(1, namHomNay - namBaiCu);
        bannerTitle.setText(getString(R.string.banner_title_nam, soNamCachDay));

        bannerExcerpt.setText(baiNgayNayNamXua.getNoiDung());
        ganAnhBanner(baiNgayNayNamXua.getId());
    }

    private void ganAnhBanner(int diaryId) {
        List<DiaryPhoto> danhSachAnh = dbHelper.getPhotosForDiary(diaryId);
        if (danhSachAnh.isEmpty()) {
            bannerThumb.setImageDrawable(null);
            return;
        }

        String duongDan = danhSachAnh.get(0).getDuongDanAnh();
        if (new File(duongDan).exists()) {
            bannerThumb.setImageBitmap(BitmapFactory.decodeFile(duongDan));
        } else {
            bannerThumb.setImageDrawable(null);
        }
    }
}