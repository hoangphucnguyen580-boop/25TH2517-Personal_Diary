package ntu.nguyenhoangphuc.personal_diary.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import ntu.nguyenhoangphuc.personal_diary.R;
import ntu.nguyenhoangphuc.personal_diary.database.DiaryDatabaseHelper;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryEntry;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryPhoto;



/**
 * Màn Thêm/Sửa 1 bài nhật ký - dùng CHUNG 1 Activity cho cả 2 chế độ,
 * phân biệt bằng có truyền EXTRA_DIARY_ID qua Intent hay không.
 *
 * PHẦN 1: nội dung, ngày, mood, gợi ý viết, tag, lưu/tải DB.
 * PHẦN 2: ảnh (thêm/xoá/chú thích/xem phóng to).
 * PHẦN 3: giọng nói (RecognizerIntent), chia sẻ (ACTION_SEND), đọc to (TTS).
 */
public class AddEditDiaryActivity extends AppCompatActivity {

    // Key truyền id bài nhật ký qua Intent khi mở màn này ở chế độ SỬA.
    // HomeFragment sẽ dùng đúng hằng số này khi gọi intent.putExtra(...)
    public static final String EXTRA_DIARY_ID = "extra_diary_id";

    // Intent KHÔNG có EXTRA_DIARY_ID -> hiểu là chế độ THÊM MỚI
    private static final int KHONG_CO_ID = -1;

    // ===== View =====
    private ImageButton btnQuayLai, btnLuu, btnChiaSe, btnDocTo, btnMic, btnDoiCauGoiY, btnThemAnh;
    private LinearLayout llDaiAnh;

    private static final int SO_ANH_TOI_DA = 5;
    private static final int KICH_THUOC_ANH_LUU = 1280;   // px - cạnh dài nhất sau khi resize
    private static final int CHAT_LUONG_NEN_JPEG = 85;    // % chất lượng nén JPEG

    // 2 launcher xử lý kết quả trả về từ Thư viện ảnh / Camera - PHẢI đăng ký ngay
// trong onCreate (không được đăng ký bên trong onClickListener), vì Android
// bắt buộc launcher phải sẵn sàng TRƯỚC khi Activity chạy xong onCreate
    private ActivityResultLauncher<PickVisualMediaRequest> launcherThuVien;
    private ActivityResultLauncher<Uri> launcherCamera;

    // Giữ tham chiếu file ảnh Camera đang chụp dở, để biết xử lý file nào khi chụp xong
    private File fileAnhCameraTam;

    // Launcher mở màn nhận diện giọng nói của hệ thống (Google App), nhận lại
    // chữ đã nhận diện qua onActivityResult kiểu mới (ActivityResultContracts)
    private ActivityResultLauncher<Intent> launcherGiongNoi;

    // Engine đọc to (TTS) - khởi tạo 1 lần trong onCreate, dùng lại xuyên suốt vòng
    // đời Activity, PHẢI shutdown() ở onDestroy() để không rò rỉ bộ nhớ
    private TextToSpeech ttsEngine;

    // Đang đọc hay không - dùng để bấm lại icon loa thì DỪNG thay vì đọc lại từ đầu
    private boolean dangDocTo = false;

    private TextView tvTieuDe, tvNgayThang, tvGoiY;
    private EditText edtNoiDung, edtNhapTagMoi;
    private LinearLayout rowNgayThang;
    private ImageView ivMoodHappy, ivMoodCalm, ivMoodSad, ivMoodAngry, ivMoodNeutral;
    private ChipGroup cgTagGoiY;

    // ===== Dữ liệu / trạng thái =====
    private DiaryDatabaseHelper dbHelper;

    // -1 = đang Thêm mới, khác -1 = đang Sửa bài có id này
    private int diaryIdDangSua = KHONG_CO_ID;

    // Giữ lại ngay_tao gốc khi Sửa bài, để lưu xuống DB không mất thời điểm tạo bài ban đầu
    private String ngayTaoGocDaTai;

    // Ngày đang chọn, lưu sẵn ở định dạng yyyy-MM-dd (đúng định dạng cột ngay_thang trong DB)
    private String ngayDaChonYyyyMMdd;

    // Mã tâm trạng đang chọn ("happy"/"calm"/"sad"/"angry"/"neutral"), null nếu không chọn
    private String tamTrangDaChon;

    // ImageView mood nào đang setSelected(true) - dùng để bỏ chọn nó khi mày chọn mood khác
    private ImageView moodDangChon;

    private List<String> danhSachGoiY;
    private final Random ngauNhien = new Random();

    // dinhDangLuu: định dạng lưu DB. dinhDangHienThi: định dạng hiện lên màn hình cho dễ đọc
    // (giống hệt cách DiaryAdapter đang làm, để đồng bộ toàn app)
    private final SimpleDateFormat dinhDangLuu = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat dinhDangHienThi = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_diary);

        dbHelper = new DiaryDatabaseHelper(this);

        anhXaView();
        khoiTaoGoiY();
        khoiTaoLauncherAnh();
        khoiTaoLauncherGiongNoi();   // MỚI
        khoiTaoTTS();                // MỚI
        ganSuKien();

        diaryIdDangSua = getIntent().getIntExtra(EXTRA_DIARY_ID, KHONG_CO_ID);

        if (diaryIdDangSua != KHONG_CO_ID) {
            // Chế độ SỬA - tải dữ liệu bài cũ lên màn hình
            tvTieuDe.setText(R.string.tieu_de_sua);
            taiDuLieuBaiCu();
        } else {
            // Chế độ THÊM MỚI - mặc định chọn sẵn ngày hôm nay cho tiện, mày vẫn đổi được
            tvTieuDe.setText(R.string.tieu_de_them);
            Calendar homNay = Calendar.getInstance();
            ngayDaChonYyyyMMdd = dinhDangLuu.format(homNay.getTime());
            tvNgayThang.setText(dinhDangHienThi.format(homNay.getTime()));
        }
    }

    private void anhXaView() {
        btnQuayLai = findViewById(R.id.btnQuayLai);
        tvTieuDe = findViewById(R.id.tvTieuDe);
        btnLuu = findViewById(R.id.btnLuu);
        btnChiaSe = findViewById(R.id.btnChiaSe);
        btnDocTo = findViewById(R.id.btnDocTo);
        btnMic = findViewById(R.id.btnMic);

        edtNoiDung = findViewById(R.id.edtNoiDung);

        rowNgayThang = findViewById(R.id.rowNgayThang);
        tvNgayThang = findViewById(R.id.tvNgayThang);

        ivMoodHappy = findViewById(R.id.ivMoodHappy);
        ivMoodCalm = findViewById(R.id.ivMoodCalm);
        ivMoodSad = findViewById(R.id.ivMoodSad);
        ivMoodAngry = findViewById(R.id.ivMoodAngry);
        ivMoodNeutral = findViewById(R.id.ivMoodNeutral);

        tvGoiY = findViewById(R.id.tvGoiY);
        btnDoiCauGoiY = findViewById(R.id.btnDoiCauGoiY);

        cgTagGoiY = findViewById(R.id.cgTagGoiY);
        edtNhapTagMoi = findViewById(R.id.edtNhapTagMoi);

        btnThemAnh = findViewById(R.id.btnThemAnh);

        llDaiAnh = findViewById(R.id.llDaiAnh);
    }

    // Đọc string-array gợi ý viết trong strings.xml ra List cho dễ random
    private void khoiTaoGoiY() {
        String[] mang = getResources().getStringArray(R.array.danh_sach_goi_y_viet);
        danhSachGoiY = Arrays.asList(mang);
    }

    private void ganSuKien() {
        btnQuayLai.setOnClickListener(v -> finish());
        btnLuu.setOnClickListener(v -> luuNhatKy());

        btnThemAnh.setOnClickListener(v -> moBottomSheetChonNguonAnh());

        // Phần 3: giọng nói, share, TTS - đã làm xong, không còn là listener rỗng nữa
        btnMic.setOnClickListener(v -> moNhanDienGiongNoi());
        btnChiaSe.setOnClickListener(v -> chiaSeNhatKy());
        btnDocTo.setOnClickListener(v -> xuLyBamDocTo());

        rowNgayThang.setOnClickListener(v -> moChonNgay());

        ivMoodHappy.setOnClickListener(v -> xuLyBamMood(ivMoodHappy, "happy"));
        ivMoodCalm.setOnClickListener(v -> xuLyBamMood(ivMoodCalm, "calm"));
        ivMoodSad.setOnClickListener(v -> xuLyBamMood(ivMoodSad, "sad"));
        ivMoodAngry.setOnClickListener(v -> xuLyBamMood(ivMoodAngry, "angry"));
        ivMoodNeutral.setOnClickListener(v -> xuLyBamMood(ivMoodNeutral, "neutral"));

        btnDoiCauGoiY.setOnClickListener(v -> doiGoiYNgauNhien());

        // IME_ACTION_DONE = sự kiện bấm nút "Xong" trên bàn phím ảo, ứng với
        // imeOptions="actionDone" đã khai trong XML của edtNhapTagMoi
        edtNhapTagMoi.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String tenTagMoi = edtNhapTagMoi.getText().toString().trim();
                if (!tenTagMoi.isEmpty()) {
                    themTagMoi(tenTagMoi);
                    edtNhapTagMoi.setText("");
                }
                return true; // true = đã tự xử lý xong sự kiện này
            }
            return false;
        });
    }

    // ===================== NGÀY THÁNG =====================

    // DatePickerDialog = hộp thoại chọn ngày có sẵn của Android
    private void moChonNgay() {
        Calendar lich = Calendar.getInstance();
        if (ngayDaChonYyyyMMdd != null) {
            try {
                lich.setTime(dinhDangLuu.parse(ngayDaChonYyyyMMdd));
            } catch (ParseException e) {
                // Không parse được thì cứ giữ lịch hiện tại (hôm nay), không crash app
            }
        }

        int nam = lich.get(Calendar.YEAR);
        int thang = lich.get(Calendar.MONTH);
        int ngay = lich.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this, (view, namChon, thangChon, ngayChon) -> {
            Calendar ngayMoiChon = Calendar.getInstance();
            ngayMoiChon.set(namChon, thangChon, ngayChon);
            ngayDaChonYyyyMMdd = dinhDangLuu.format(ngayMoiChon.getTime());
            tvNgayThang.setText(dinhDangHienThi.format(ngayMoiChon.getTime()));
        }, nam, thang, ngay);

        dialog.show();
    }

    // ===================== MOOD =====================

    // Bấm vào 1 icon mood: đang chọn sẵn icon đó -> bấm lại để BỎ chọn (mood optional).
    // Chọn icon khác -> bỏ chọn icon cũ, chọn icon mới. setSelected(true/false) tự đổi
    // hình theo đúng selector_mood_*.xml (2 trạng thái: chọn/không chọn).
    private void xuLyBamMood(ImageView iconDuocBam, String maMood) {
        if (moodDangChon == iconDuocBam) {
            iconDuocBam.setSelected(false);
            moodDangChon = null;
            tamTrangDaChon = null;
            return;
        }

        if (moodDangChon != null) {
            moodDangChon.setSelected(false);
        }
        iconDuocBam.setSelected(true);
        moodDangChon = iconDuocBam;
        tamTrangDaChon = maMood;
    }

    // Dùng riêng khi TẢI dữ liệu bài cũ lên (chế độ Sửa) - set thẳng, không cần logic toggle
    private void chonMoodBanDau(String maMood) {
        ImageView iconTuongUng;
        switch (maMood) {
            case "happy":
                iconTuongUng = ivMoodHappy;
                break;
            case "calm":
                iconTuongUng = ivMoodCalm;
                break;
            case "sad":
                iconTuongUng = ivMoodSad;
                break;
            case "angry":
                iconTuongUng = ivMoodAngry;
                break;
            case "neutral":
            default:
                iconTuongUng = ivMoodNeutral;
                break;
        }
        iconTuongUng.setSelected(true);
        moodDangChon = iconTuongUng;
        tamTrangDaChon = maMood;
    }

    // ===================== GỢI Ý VIẾT =====================

    private void doiGoiYNgauNhien() {
        if (danhSachGoiY.isEmpty()) return;
        int viTri = ngauNhien.nextInt(danhSachGoiY.size());
        tvGoiY.setText(danhSachGoiY.get(viTri));
    }

    // ===================== TAG =====================

    // Đã có sẵn tag này (kể cả 5 tag dựng sẵn trong XML) -> chỉ tick chọn lên.
    // Chưa có -> tạo Chip mới có nút "x" để xoá (vì là tag mày tự gõ, không cố định)
    private void themTagMoi(String tenTag) {
        for (int i = 0; i < cgTagGoiY.getChildCount(); i++) {
            View con = cgTagGoiY.getChildAt(i);
            if (con instanceof Chip) {
                Chip chipDaCo = (Chip) con;
                if (chipDaCo.getText().toString().trim().equalsIgnoreCase(tenTag)) {
                    chipDaCo.setChecked(true);
                    return;
                }
            }
        }

        Chip chipMoi = new Chip(this);
        chipMoi.setText(tenTag);
        chipMoi.setCheckable(true);
        chipMoi.setChecked(true);
        chipMoi.setTextColor(ContextCompat.getColor(this, R.color.color_chu_chinh));
        chipMoi.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.chip_bg_selector));
        chipMoi.setChipStrokeColor(ContextCompat.getColorStateList(this, R.color.chip_stroke_selector));
        chipMoi.setChipStrokeWidth(dpSangPx(1.5f));
        chipMoi.setCheckedIconVisible(true);
        chipMoi.setCheckedIconTint(ContextCompat.getColorStateList(this, R.color.color_nhan));
        // closeIcon = icon "x" nhỏ để xoá riêng Chip này - chỉ Chip tự gõ mới có
        chipMoi.setCloseIconVisible(true);
        chipMoi.setOnCloseIconClickListener(v -> cgTagGoiY.removeView(chipMoi));

        cgTagGoiY.addView(chipMoi);
    }

    // Gom Chip đang tick chọn thành chuỗi phân cách dấu phẩy, đúng quy ước cột the_gan
    // (vd "Công việc,Gia đình"). Không Chip nào chọn -> null.
    private String layTagDangChon() {
        StringBuilder ketQua = new StringBuilder();
        for (int i = 0; i < cgTagGoiY.getChildCount(); i++) {
            View con = cgTagGoiY.getChildAt(i);
            if (con instanceof Chip) {
                Chip chip = (Chip) con;
                if (chip.isChecked()) {
                    if (ketQua.length() > 0) {
                        ketQua.append(",");
                    }
                    ketQua.append(chip.getText().toString().trim());
                }
            }
        }
        return ketQua.length() > 0 ? ketQua.toString() : null;
    }

    // ===================== GIỌNG NÓI (Phần 3) =====================

    private void khoiTaoLauncherGiongNoi() {
        launcherGiongNoi = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                ketQua -> {
                    if (ketQua.getResultCode() == RESULT_OK && ketQua.getData() != null) {
                        ArrayList<String> danhSachKetQua = ketQua.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        // Google trả về NHIỀU phương án đoán, lấy phương án đầu tiên
                        // (độ tin cậy cao nhất) là đủ dùng cho app này
                        if (danhSachKetQua != null && !danhSachKetQua.isEmpty()) {
                            noiChuVaoNoiDung(danhSachKetQua.get(0));
                        }
                    }
                    // ketQua.getResultCode() != RESULT_OK nghĩa là mày bấm back/huỷ -
                    // không làm gì cả, không crash
                });
    }

    private void moNhanDienGiongNoi() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        // Cố định tiếng Việt - KHÔNG phụ thuộc ngôn ngữ mặc định máy đang cài
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.dang_nghe));

        // Kiểm tra máy có app nào xử lý được Intent này không - tránh crash
        // ActivityNotFoundException trên máy không cài Google App (hiếm nhưng có)
        if (intent.resolveActivity(getPackageManager()) != null) {
            launcherGiongNoi.launch(intent);
        } else {
            Toast.makeText(this, "Máy không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show();
        }
    }

    // Nối chữ mới nhận được vào CUỐI nội dung đang gõ, có dấu cách ngăn cách nếu
    // nội dung cũ không rỗng - tránh dính liền chữ cũ với chữ mới thành 1 từ
    private void noiChuVaoNoiDung(String chuMoiNhanDuoc) {
        String noiDungHienTai = edtNoiDung.getText().toString();
        if (noiDungHienTai.trim().isEmpty()) {
            edtNoiDung.setText(chuMoiNhanDuoc);
        } else {
            edtNoiDung.setText(noiDungHienTai + " " + chuMoiNhanDuoc);
        }
        // Đưa con trỏ về cuối để mày gõ tiếp ngay được, không cần bấm lại vào ô
        edtNoiDung.setSelection(edtNoiDung.getText().length());
    }

    // ===================== ĐỌC TO - TTS (Phần 3) =====================

    private void khoiTaoTTS() {
        ttsEngine = new TextToSpeech(this, trangThaiKhoiTao -> {
            if (trangThaiKhoiTao == TextToSpeech.SUCCESS) {
                int ketQuaNgonNgu = ttsEngine.setLanguage(new Locale("vi", "VN"));
                if (ketQuaNgonNgu == TextToSpeech.LANG_MISSING_DATA
                        || ketQuaNgonNgu == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this,
                            "Máy chưa cài gói giọng đọc tiếng Việt - vào Cài đặt máy để tải thêm",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Không khởi tạo được chức năng đọc to", Toast.LENGTH_SHORT).show();
            }
        });

        // Lắng nghe lúc đọc XONG (hoặc lỗi) để tự trả dangDocTo về false - callback
        // này chạy trên luồng phụ của TTS, PHẢI runOnUiThread mới sửa biến an toàn
        // và đụng vào View được (dù ở đây chỉ sửa biến boolean, vẫn nên làm đúng chuẩn)
        ttsEngine.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                // Không cần làm gì lúc bắt đầu đọc
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> dangDocTo = false);
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> dangDocTo = false);
            }
        });
    }

    // Bấm 1 lần: bắt đầu đọc. Bấm lại LẦN NỮA khi đang đọc: DỪNG giữa chừng
    // (đúng yêu cầu mày chọn, cùng 1 icon loa, không cần đổi hình icon)
    private void xuLyBamDocTo() {
        if (dangDocTo) {
            ttsEngine.stop();
            dangDocTo = false;
            return;
        }

        String noiDung = edtNoiDung.getText().toString().trim();
        if (noiDung.isEmpty()) {
            Toast.makeText(this, "Chưa có nội dung để đọc", Toast.LENGTH_SHORT).show();
            return;
        }

        dangDocTo = true;
        // QUEUE_FLUSH = huỷ hàng đợi đọc cũ (nếu có) rồi đọc câu mới ngay lập tức,
        // tránh bị đọc chồng chéo nếu mày lỡ bấm liên tục nhiều lần
        ttsEngine.speak(noiDung, TextToSpeech.QUEUE_FLUSH, null, "doc_nhat_ky");
    }

    // ===================== CHIA SẺ (Phần 3) =====================

    private void chiaSeNhatKy() {
        String noiDung = edtNoiDung.getText().toString().trim();
        if (noiDung.isEmpty()) {
            Toast.makeText(this, "Chưa có nội dung để chia sẻ", Toast.LENGTH_SHORT).show();
            return;
        }

        String textDeChiaSe = taoNoiDungChiaSe(noiDung);
        ArrayList<Uri> danhSachUriAnh = layDanhSachUriAnhDeChiaSe();

        Intent intentChiaSe;
        if (danhSachUriAnh.isEmpty()) {
            // Không có ảnh -> share text đơn giản
            intentChiaSe = new Intent(Intent.ACTION_SEND);
            intentChiaSe.setType("text/plain");
            intentChiaSe.putExtra(Intent.EXTRA_TEXT, textDeChiaSe);
        } else {
            // Có ảnh -> share NHIỀU item cùng lúc (text + toàn bộ ảnh trong bài)
            intentChiaSe = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intentChiaSe.setType("image/*");
            intentChiaSe.putExtra(Intent.EXTRA_TEXT, textDeChiaSe);
            intentChiaSe.putParcelableArrayListExtra(Intent.EXTRA_STREAM, danhSachUriAnh);
            // Cấp quyền đọc file tạm thời cho app nhận share - bắt buộc vì
            // duong_dan_anh là content:// Uri riêng của app mình, app khác không
            // tự đọc được nếu thiếu dòng này
            intentChiaSe.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        startActivity(Intent.createChooser(intentChiaSe, getString(R.string.chia_se_nhat_ky)));
    }

    // Gộp ngày + nội dung thành 1 đoạn text để share - để đơn giản, CHƯA gộp
    // mood/tag vào đây, báo tao nếu mày muốn thêm
    private String taoNoiDungChiaSe(String noiDung) {
        StringBuilder ketQua = new StringBuilder();
        if (ngayDaChonYyyyMMdd != null) {
            try {
                ketQua.append(dinhDangHienThi.format(dinhDangLuu.parse(ngayDaChonYyyyMMdd))).append("\n\n");
            } catch (ParseException e) {
                // Parse lỗi thì bỏ qua ngày, vẫn share được nội dung bình thường
            }
        }
        ketQua.append(noiDung);
        return ketQua.toString();
    }

    // Đọc từng ảnh đang hiện trên llDaiAnh (giống hệt cách moXemAnhPhongTo() đang
    // đọc), bọc mỗi đường dẫn file thật thành content:// Uri qua FileProvider
    private ArrayList<Uri> layDanhSachUriAnhDeChiaSe() {
        ArrayList<Uri> danhSachUri = new ArrayList<>();
        int soAnh = demSoAnhHienTai();

        for (int i = 0; i < soAnh; i++) {
            View itemAnh = llDaiAnh.getChildAt(i);
            String duongDan = (String) itemAnh.getTag();
            if (duongDan == null) continue;

            File fileAnh = new File(duongDan);
            if (!fileAnh.exists()) continue; // ảnh bị mất file thì bỏ qua, không crash

            Uri uriAnh = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", fileAnh);
            danhSachUri.add(uriAnh);
        }
        return danhSachUri;
    }

    // ===================== LƯU / TẢI DB =====================

    private void taiDuLieuBaiCu() {
        DiaryEntry entry = dbHelper.getDiaryById(diaryIdDangSua);
        if (entry == null) {
            Toast.makeText(this, "Không tìm thấy bài nhật ký này", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ngayTaoGocDaTai = entry.getNgayTao();

        edtNoiDung.setText(entry.getNoiDung());

        ngayDaChonYyyyMMdd = entry.getNgayThang();
        try {
            tvNgayThang.setText(dinhDangHienThi.format(dinhDangLuu.parse(ngayDaChonYyyyMMdd)));
        } catch (ParseException e) {
            tvNgayThang.setText(ngayDaChonYyyyMMdd);
        }

        if (entry.getTamTrang() != null && !entry.getTamTrang().isEmpty()) {
            chonMoodBanDau(entry.getTamTrang());
        }

        if (entry.getTheGan() != null && !entry.getTheGan().trim().isEmpty()) {
            for (String tag : entry.getTheGan().split(",")) {
                String tagSach = tag.trim();
                if (!tagSach.isEmpty()) {
                    themTagMoi(tagSach);
                }
            }
        }

        // Tải lại dải ảnh cũ - kiểm tra file còn tồn tại không trước khi thêm vào dải,
        // tránh hiện ảnh vỡ nếu file bị mất đâu đó ngoài ý muốn (vd mày xoá thủ công
        // trong bộ nhớ máy, hoặc cài lại app làm mất thư mục files/anh_nhat_ky)
        List<DiaryPhoto> danhSachAnhCu = dbHelper.getPhotosForDiary(diaryIdDangSua);
        for (DiaryPhoto photo : danhSachAnhCu) {
            if (new File(photo.getDuongDanAnh()).exists()) {
                themAnhVaoDai(photo.getDuongDanAnh(), photo.getChuThich());
            }
        }
    }

    private void luuNhatKy() {
        String noiDung = edtNoiDung.getText().toString().trim();
        if (noiDung.isEmpty()) {
            Toast.makeText(this, "Bạn chưa viết nội dung gì cả", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ngayDaChonYyyyMMdd == null) {
            ngayDaChonYyyyMMdd = dinhDangLuu.format(Calendar.getInstance().getTime());
        }

        String theGan = layTagDangChon();
        int diaryIdVuaLuu;

        if (diaryIdDangSua == KHONG_CO_ID) {
            // Chế độ THÊM MỚI - sinh timestamp lúc tạo bài ngay bây giờ
            String ngayTaoHienTai = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Calendar.getInstance().getTime());
            DiaryEntry baiMoi = new DiaryEntry(ngayDaChonYyyyMMdd, noiDung, tamTrangDaChon, theGan, ngayTaoHienTai);
            long idMoiSinh = dbHelper.insertDiary(baiMoi);
            diaryIdVuaLuu = (int) idMoiSinh;
        } else {
            // Chế độ SỬA - giữ nguyên ngay_tao gốc, không ghi đè thời điểm tạo bài
            DiaryEntry baiDaSua = new DiaryEntry(diaryIdDangSua, ngayDaChonYyyyMMdd, noiDung, tamTrangDaChon, theGan, ngayTaoGocDaTai);
            dbHelper.updateDiary(baiDaSua);
            diaryIdVuaLuu = diaryIdDangSua;

            // Xoá sạch ảnh cũ trước, tránh lưu trùng lặp - vì tao ghi lại TOÀN BỘ
            // dải ảnh đang hiện trên màn hình xuống DB ở luuAnhVaoDb() bên dưới,
            // không xoá trước thì ảnh cũ (chưa xoá) sẽ bị insert đè thêm 1 lần nữa
            dbHelper.deletePhotosForDiary(diaryIdVuaLuu);
        }

        luuAnhVaoDb(diaryIdVuaLuu);

        Toast.makeText(this, "Đã lưu nhật ký", Toast.LENGTH_SHORT).show();
        // setResult(RESULT_OK) báo cho màn gọi mình (HomeFragment) biết là đã lưu thành công
        setResult(RESULT_OK);
        finish();
    }

    // Đọc từng item ảnh đang hiện trên llDaiAnh (bỏ qua btnThemAnh ở cuối), lấy
    // đường dẫn (đã setTag lúc thêm ảnh) + chú thích hiện tại trong ô edtChuThich,
    // ghi từng cái xuống DB theo đúng thứ tự đang hiển thị (thuTu = vị trí i)
    private void luuAnhVaoDb(int diaryId) {
        int soAnh = demSoAnhHienTai();
        for (int i = 0; i < soAnh; i++) {
            View itemAnh = llDaiAnh.getChildAt(i);
            String duongDanAnh = (String) itemAnh.getTag();
            if (duongDanAnh == null) continue;

            EditText edtChuThich = itemAnh.findViewById(R.id.edtChuThich);
            String chuThich = edtChuThich.getText().toString().trim();
            if (chuThich.isEmpty()) {
                chuThich = null;
            }

            // icon để dành làm sau (Phần 2 chưa làm bộ chọn icon) nên truyền null
            DiaryPhoto photo = new DiaryPhoto(diaryId, duongDanAnh, chuThich, null, i);
            dbHelper.insertPhoto(photo);
        }
    }

    // Đổi dp sang px - giống hệt hàm dpSangPx trong DiaryAdapter, cần vì set style Chip
    // bằng code (setChipStrokeWidth...) chỉ nhận đơn vị px, không tự hiểu "dp" như XML
    private int dpSangPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // ===================== ẢNH =====================

    private void khoiTaoLauncherAnh() {
        // Photo Picker hiện đại - KHÔNG cần xin quyền đọc bộ nhớ như ACTION_PICK kiểu cũ
        launcherThuVien = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null) {
                        xuLyAnhTuThuVien(uri);
                    }
                    // uri == null nghĩa là mày bấm back/huỷ ở màn chọn ảnh - không làm gì cả
                });

        // TakePicture() trả về boolean - true nghĩa là chụp thành công, ảnh đã được
        // ghi vào đúng Uri mày đưa lúc gọi launcherCamera.launch(...)
        launcherCamera = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                thanhCong -> {
                    if (thanhCong) {
                        xuLyAnhTuCamera();
                    }
                });
    }

    private void moBottomSheetChonNguonAnh() {
        if (demSoAnhHienTai() >= SO_ANH_TOI_DA) {
            Toast.makeText(this, "Tối đa " + SO_ANH_TOI_DA + " ảnh mỗi bài thôi", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        sheet.setContentView(R.layout.layout_bottom_sheet_chon_anh);

        View llChonThuVien = sheet.findViewById(R.id.llChonThuVien);
        View llChonMayAnh = sheet.findViewById(R.id.llChonMayAnh);

        if (llChonThuVien != null) {
            llChonThuVien.setOnClickListener(v -> {
                sheet.dismiss();
                moThuVienAnh();
            });
        }
        if (llChonMayAnh != null) {
            llChonMayAnh.setOnClickListener(v -> {
                sheet.dismiss();
                moCamera();
            });
        }

        sheet.show();
    }

    // llDaiAnh có N item ảnh + 1 nút "+" đứng cuối -> số ảnh thật = tổng số con - 1
    private int demSoAnhHienTai() {
        return llDaiAnh.getChildCount() - 1;
    }

    private void moThuVienAnh() {
        launcherThuVien.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void moCamera() {
        try {
            File thuMucTam = new File(getCacheDir(), "anh_nhat_ky_tam");
            if (!thuMucTam.exists()) {
                thuMucTam.mkdirs();
            }
            fileAnhCameraTam = File.createTempFile("camera_", ".jpg", thuMucTam);

            // FileProvider "bọc" file thật thành content:// Uri - từ Android 7 trở đi,
            // Camera app KHÔNG được phép ghi thẳng vào file:// Uri vì lý do bảo mật
            Uri uriAnhCameraTam = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", fileAnhCameraTam);

            launcherCamera.launch(uriAnhCameraTam);
        } catch (IOException e) {
            Toast.makeText(this, "Không mở được Camera, thử lại nhé", Toast.LENGTH_SHORT).show();
        }
    }

    private void xuLyAnhTuThuVien(Uri uri) {
        try {
            File fileTam = copyUriRaFileTam(uri);
            Bitmap bitmapDaThuNho = thuNhoAnhTuFile(fileTam, KICH_THUOC_ANH_LUU);
            fileTam.delete();

            if (bitmapDaThuNho == null) {
                Toast.makeText(this, "Không đọc được ảnh này", Toast.LENGTH_SHORT).show();
                return;
            }

            String duongDanFileThat = luuBitmapThanhFileThat(bitmapDaThuNho);
            bitmapDaThuNho.recycle();
            themAnhVaoDai(duongDanFileThat);

        } catch (IOException e) {
            Toast.makeText(this, "Có lỗi khi xử lý ảnh, thử lại nhé", Toast.LENGTH_SHORT).show();
        }
    }

    private void xuLyAnhTuCamera() {
        if (fileAnhCameraTam == null) return;

        try {
            Bitmap bitmapDaThuNho = thuNhoAnhTuFile(fileAnhCameraTam, KICH_THUOC_ANH_LUU);
            fileAnhCameraTam.delete();

            if (bitmapDaThuNho == null) {
                Toast.makeText(this, "Không đọc được ảnh vừa chụp", Toast.LENGTH_SHORT).show();
                return;
            }

            String duongDanFileThat = luuBitmapThanhFileThat(bitmapDaThuNho);
            bitmapDaThuNho.recycle();
            themAnhVaoDai(duongDanFileThat);

        } catch (IOException e) {
            Toast.makeText(this, "Có lỗi khi xử lý ảnh, thử lại nhé", Toast.LENGTH_SHORT).show();
        }
    }

    // Copy dữ liệu từ content:// Uri (ảnh chọn ở Thư viện) ra 1 file tạm trong cacheDir
// - làm vậy để bước thu nhỏ tiếp theo chỉ cần biết làm việc với File thật, không
// cần viết thêm 1 bộ code riêng để đọc trực tiếp từ Uri
    private File copyUriRaFileTam(Uri uri) throws IOException {
        File thuMucTam = new File(getCacheDir(), "anh_nhat_ky_tam");
        if (!thuMucTam.exists()) {
            thuMucTam.mkdirs();
        }
        File fileTam = File.createTempFile("thuvien_", ".jpg", thuMucTam);

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(fileTam)) {
            if (inputStream == null) {
                throw new IOException("Không mở được ảnh đã chọn");
            }
            byte[] boDem = new byte[8192];
            int soByteDaDoc;
            while ((soByteDaDoc = inputStream.read(boDem)) != -1) {
                outputStream.write(boDem, 0, soByteDaDoc);
            }
        }
        return fileTam;
    }

    // Thu nhỏ ảnh theo đúng pattern 2 bước mà DiaryAdapter.giaiMaAnhGonNhe đang dùng:
// bước 1 chỉ đọc kích thước, bước 2 mới decode thật với inSampleSize phù hợp -
// tránh OOM (hết bộ nhớ) khi ảnh Camera gốc có thể lên tới 10-20MB
    private Bitmap thuNhoAnhTuFile(File file, int canhDaiToiDa) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        int canhDaiGoc = Math.max(options.outWidth, options.outHeight);
        int tiLe = 1;
        while ((canhDaiGoc / tiLe) > canhDaiToiDa) {
            tiLe *= 2;
        }

        options.inSampleSize = tiLe;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    // Nén Bitmap thành JPEG rồi ghi xuống file thật trong bộ nhớ riêng của app
// (getFilesDir()) - ĐÚNG quy ước bắt buộc: duong_dan_anh trong DB phải là file
// path thật, không phải content:// Uri
    private String luuBitmapThanhFileThat(Bitmap bitmap) throws IOException {
        File thuMucAnh = new File(getFilesDir(), "anh_nhat_ky");
        if (!thuMucAnh.exists()) {
            thuMucAnh.mkdirs();
        }

        String tenFile = "anh_" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString().substring(0, 6) + ".jpg";
        File fileDich = new File(thuMucAnh, tenFile);

        try (OutputStream outputStream = new FileOutputStream(fileDich)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, CHAT_LUONG_NEN_JPEG, outputStream);
        }

        return fileDich.getAbsolutePath();
    }

    // Inflate item_anh_nhat_ky.xml, hiển thị ảnh, gắn nút xoá, rồi chèn vào llDaiAnh
// NGAY TRƯỚC btnThemAnh (luôn giữ nút "+" đứng cuối dải)
    // Dùng khi THÊM ảnh mới (Phần 2.1) - không có chú thích cũ
    private void themAnhVaoDai(String duongDanAnh) {
        themAnhVaoDai(duongDanAnh, null);
    }

    // Dùng khi TẢI lại ảnh cũ (chế độ Sửa) - có sẵn chú thích cũ cần điền vào
    private void themAnhVaoDai(String duongDanAnh, String chuThichBanDau) {
        View itemAnh = LayoutInflater.from(this).inflate(R.layout.item_anh_nhat_ky, llDaiAnh, false);

        ImageView ivAnh = itemAnh.findViewById(R.id.ivAnh);
        ImageButton btnXoaAnh = itemAnh.findViewById(R.id.btnXoaAnh);
        EditText edtChuThich = itemAnh.findViewById(R.id.edtChuThich);
        if (chuThichBanDau != null) {
            edtChuThich.setText(chuThichBanDau);
        }

        // Ảnh đã được thu nhỏ + nén sẵn ở bước trước nên decode trực tiếp là đủ nhanh
        ivAnh.setImageBitmap(BitmapFactory.decodeFile(duongDanAnh));

        // Bấm vào ảnh -> mở PhotoViewerDialog xem phóng to (Phần 2.3)
        ivAnh.setOnClickListener(v -> moXemAnhPhongTo(itemAnh));

        // Gắn đường dẫn file thật lên chính View - lúc bấm "Lưu" toàn bài (Phần 2.2),
        // tao đọc lại tag này để biết insertPhoto() ảnh nào, giống hệt cách
        // layTagDangChon() đang đọc text từ các Chip
        itemAnh.setTag(duongDanAnh);

        btnXoaAnh.setOnClickListener(v -> {
            llDaiAnh.removeView(itemAnh);
            // Xoá luôn file thật khỏi máy - không xoá thì bấm thêm/xoá nhiều lần sẽ
            // để lại rác ảnh mồ côi trong bộ nhớ app
            new File(duongDanAnh).delete();
            capNhatTrangThaiNutThemAnh();
        });

        int viTriChenVao = llDaiAnh.getChildCount() - 1;
        llDaiAnh.addView(itemAnh, viTriChenVao);

        capNhatTrangThaiNutThemAnh();
    }

    // ===================== XEM ẢNH PHÓNG TO (Phần 2.3) =====================

    // Bấm vào 1 ảnh trong dải ảnh -> mở PhotoViewerDialog xem phóng to, cho phép
    // pinch-zoom + vuốt qua các ảnh khác trong CÙNG bài đang soạn (kể cả ảnh mới
    // vừa thêm, chưa lưu xuống DB - vì đọc trực tiếp từ llDaiAnh đang hiện trên
    // màn hình, không đọc từ DB)
    private void moXemAnhPhongTo(View itemAnhDuocBam) {
        List<String> danhSachDuongDan = new ArrayList<>();
        List<String> danhSachChuThich = new ArrayList<>();
        int viTriBanDau = 0;

        int soAnh = demSoAnhHienTai();
        for (int i = 0; i < soAnh; i++) {
            View itemAnh = llDaiAnh.getChildAt(i);

            String duongDan = (String) itemAnh.getTag();
            danhSachDuongDan.add(duongDan);

            EditText edtChuThichCuaAnhNay = itemAnh.findViewById(R.id.edtChuThich);
            String chuThich = edtChuThichCuaAnhNay.getText().toString().trim();
            danhSachChuThich.add(chuThich.isEmpty() ? null : chuThich);

            if (itemAnh == itemAnhDuocBam) {
                viTriBanDau = i;
            }
        }

        PhotoViewerDialog dialog = new PhotoViewerDialog(this, danhSachDuongDan, danhSachChuThich, viTriBanDau);
        dialog.show();
    }

    // Ẩn nút "+" khi đã đủ 5 ảnh, hiện lại ngay khi xoá bớt
    private void capNhatTrangThaiNutThemAnh() {
        btnThemAnh.setVisibility(demSoAnhHienTai() >= SO_ANH_TOI_DA ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // BẮT BUỘC shutdown TTS khi Activity bị huỷ, không thì rò rỉ tài nguyên
        // hệ thống (native resource của engine đọc, không tự Garbage Collector dọn được)
        if (ttsEngine != null) {
            ttsEngine.stop();
            ttsEngine.shutdown();
        }
    }
}