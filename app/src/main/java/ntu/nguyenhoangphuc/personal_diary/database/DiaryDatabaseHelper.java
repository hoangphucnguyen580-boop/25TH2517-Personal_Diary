package ntu.nguyenhoangphuc.personal_diary.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Set;

import ntu.nguyenhoangphuc.personal_diary.model.AnhKyNiem;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryEntry;
import ntu.nguyenhoangphuc.personal_diary.model.DiaryPhoto;

public class DiaryDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "nhat_ky.db";
    private static final int DATABASE_VERSION = 1;

    // Tên bảng và tên cột
    private static final String TABLE_NHAT_KY = "NhatKy";
    private static final String COL_ID = "id";
    private static final String COL_NGAY_THANG = "ngay_thang";
    private static final String COL_NOI_DUNG = "noi_dung";
    private static final String COL_TAM_TRANG = "tam_trang";
    private static final String COL_THE_GAN = "the_gan";
    private static final String COL_NGAY_TAO = "ngay_tao";

    private static final String TABLE_ANH_NHAT_KY = "AnhNhatKy";
    private static final String COL_ANH_ID = "id";
    private static final String COL_NHAT_KY_ID = "nhat_ky_id";
    private static final String COL_DUONG_DAN_ANH = "duong_dan_anh";
    private static final String COL_CHU_THICH = "chu_thich";
    private static final String COL_ICON = "icon";
    private static final String COL_THU_TU = "thu_tu";

    public DiaryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String taoBangNhatKy = "CREATE TABLE " + TABLE_NHAT_KY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NGAY_THANG + " TEXT NOT NULL, " +
                COL_NOI_DUNG + " TEXT NOT NULL, " +
                COL_TAM_TRANG + " TEXT, " +
                COL_THE_GAN + " TEXT, " +
                COL_NGAY_TAO + " TEXT NOT NULL" +
                ")";
        db.execSQL(taoBangNhatKy);

        String taoBangAnh = "CREATE TABLE " + TABLE_ANH_NHAT_KY + " (" +
                COL_ANH_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NHAT_KY_ID + " INTEGER NOT NULL, " +
                COL_DUONG_DAN_ANH + " TEXT NOT NULL, " +
                COL_CHU_THICH + " TEXT, " +
                COL_ICON + " TEXT, " +
                COL_THU_TU + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + COL_NHAT_KY_ID + ") REFERENCES " + TABLE_NHAT_KY + "(" + COL_ID + ") ON DELETE CASCADE" +
                ")";
        db.execSQL(taoBangAnh);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANH_NHAT_KY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NHAT_KY);
        onCreate(db);
    }

    public long insertDiary(DiaryEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NGAY_THANG, entry.getNgayThang());
        values.put(COL_NOI_DUNG, entry.getNoiDung());
        values.put(COL_TAM_TRANG, entry.getTamTrang());
        values.put(COL_THE_GAN, entry.getTheGan());
        values.put(COL_NGAY_TAO, entry.getNgayTao());
        long newId = db.insert(TABLE_NHAT_KY, null, values);
        db.close();
        return newId;
    }

    public List<DiaryEntry> getAllDiaries() {
        List<DiaryEntry> danhSach = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NHAT_KY + " ORDER BY " + COL_NGAY_THANG + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                DiaryEntry entry = new DiaryEntry(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_THANG)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOI_DUNG)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TAM_TRANG)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_THE_GAN)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_TAO))
                );
                danhSach.add(entry);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return danhSach;
    }

    public void deleteDiary(int diaryId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_NHAT_KY, COL_ID + " = ?", new String[]{String.valueOf(diaryId)});
        db.close();
    }

    public long insertPhoto(DiaryPhoto photo) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NHAT_KY_ID, photo.getNhatKyId());
        values.put(COL_DUONG_DAN_ANH, photo.getDuongDanAnh());
        values.put(COL_CHU_THICH, photo.getChuThich());
        values.put(COL_ICON, photo.getIcon());
        values.put(COL_THU_TU, photo.getThuTu());
        long newId = db.insert(TABLE_ANH_NHAT_KY, null, values);
        db.close();
        return newId;
    }

    public List<DiaryPhoto> getPhotosForDiary(int diaryId) {
        List<DiaryPhoto> danhSachAnh = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_ANH_NHAT_KY +
                " WHERE " + COL_NHAT_KY_ID + " = ? ORDER BY " + COL_THU_TU + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(diaryId)});

        if (cursor.moveToFirst()) {
            do {
                DiaryPhoto photo = new DiaryPhoto(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ANH_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_NHAT_KY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DUONG_DAN_ANH)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_CHU_THICH)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_ICON)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_THU_TU))
                );
                danhSachAnh.add(photo);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return danhSachAnh;
    }

    public void updateDiary(DiaryEntry entry) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NGAY_THANG, entry.getNgayThang());
        values.put(COL_NOI_DUNG, entry.getNoiDung());
        values.put(COL_TAM_TRANG, entry.getTamTrang());
        values.put(COL_THE_GAN, entry.getTheGan());
        db.update(TABLE_NHAT_KY, values, COL_ID + " = ?", new String[]{String.valueOf(entry.getId())});
        db.close();
    }

    public DiaryEntry getDiaryById(int diaryId) {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NHAT_KY + " WHERE " + COL_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(diaryId)});

        DiaryEntry entry = null;
        if (cursor.moveToFirst()) {
            entry = new DiaryEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_THANG)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NOI_DUNG)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_TAM_TRANG)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_THE_GAN)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_TAO))
            );
        }
        cursor.close();
        db.close();
        return entry;
    }

    public void deletePhotosForDiary(int diaryId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ANH_NHAT_KY, COL_NHAT_KY_ID + " = ?", new String[]{String.valueOf(diaryId)});
        db.close();
    }

    // ===================== STREAK =====================

    public int tinhSoNgayStreak() {
        List<String> danhSachNgay = layDanhSachNgayCoBaiVietDuyNhat();
        if (danhSachNgay.isEmpty()) {
            return 0;
        }

        SimpleDateFormat dinhDangLuu = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar homNay = Calendar.getInstance();
        ganVeDauNgay(homNay);

        Calendar ngayDangXet;
        try {
            ngayDangXet = Calendar.getInstance();
            ngayDangXet.setTime(dinhDangLuu.parse(danhSachNgay.get(0)));
            ganVeDauNgay(ngayDangXet);
        } catch (ParseException e) {
            return 0;
        }

        if (soNgayGiuaHaiMoc(ngayDangXet, homNay) > 1) {
            return 0;
        }

        int streak = 1;
        for (int i = 1; i < danhSachNgay.size(); i++) {
            Calendar ngayTiepTheo;
            try {
                ngayTiepTheo = Calendar.getInstance();
                ngayTiepTheo.setTime(dinhDangLuu.parse(danhSachNgay.get(i)));
                ganVeDauNgay(ngayTiepTheo);
            } catch (ParseException e) {
                break;
            }

            if (soNgayGiuaHaiMoc(ngayTiepTheo, ngayDangXet) == 1) {
                streak++;
                ngayDangXet = ngayTiepTheo;
            } else {
                break;
            }
        }

        return streak;
    }

    private List<String> layDanhSachNgayCoBaiVietDuyNhat() {
        List<String> danhSachNgay = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT DISTINCT " + COL_NGAY_THANG + " FROM " + TABLE_NHAT_KY +
                " ORDER BY " + COL_NGAY_THANG + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                danhSachNgay.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return danhSachNgay;
    }

    private void ganVeDauNgay(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private long soNgayGiuaHaiMoc(Calendar mocA, Calendar mocB) {
        long chenhLechMs = Math.abs(mocB.getTimeInMillis() - mocA.getTimeInMillis());
        return chenhLechMs / (24L * 60 * 60 * 1000);
    }

    // ===================== NGÀY NÀY NĂM XƯA =====================

    public DiaryEntry timBaiNgayNayNamXua() {
        Calendar homNay = Calendar.getInstance();
        String thangNgayHomNay = String.format(Locale.US, "%02d-%02d",
                homNay.get(Calendar.MONTH) + 1, homNay.get(Calendar.DAY_OF_MONTH));
        String ngayHomNayDayDu = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(homNay.getTime());

        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NHAT_KY +
                " WHERE substr(" + COL_NGAY_THANG + ", 6) = ?" +
                " AND " + COL_NGAY_THANG + " != ?" +
                " ORDER BY " + COL_NGAY_THANG + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{thangNgayHomNay, ngayHomNayDayDu});

        DiaryEntry entry = null;
        if (cursor.moveToFirst()) {
            entry = new DiaryEntry(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_THANG)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NOI_DUNG)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_TAM_TRANG)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_THE_GAN)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_TAO))
            );
        }
        cursor.close();
        db.close();
        return entry;
    }

    // ===================== TÌM KIẾM + LỌC THEO THẺ =====================

    public List<String> layDanhSachTheDaSuDung() {
        List<String> ketQua = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT " + COL_THE_GAN + " FROM " + TABLE_NHAT_KY +
                " WHERE " + COL_THE_GAN + " IS NOT NULL AND " + COL_THE_GAN + " != ''";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                String chuoiThe = cursor.getString(0);
                for (String the : chuoiThe.split(",")) {
                    String theSach = the.trim();
                    if (!theSach.isEmpty() && !ketQua.contains(theSach)) {
                        ketQua.add(theSach);
                    }
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        Collections.sort(ketQua);
        return ketQua;
    }

    public List<DiaryEntry> timKiemVaLoc(String tuKhoaTimKiem, List<String> danhSachTheDangLoc, boolean moiNhatTruoc) {
        List<DiaryEntry> tatCaBaiViet = getAllDiaries();
        List<DiaryEntry> ketQua = new ArrayList<>();

        String tuKhoaDaBoDau = boDauTiengViet(tuKhoaTimKiem == null ? "" : tuKhoaTimKiem.trim());

        for (DiaryEntry bai : tatCaBaiViet) {
            boolean khopTimKiem = tuKhoaDaBoDau.isEmpty()
                    || boDauTiengViet(bai.getNoiDung()).contains(tuKhoaDaBoDau);

            boolean khopThe = danhSachTheDangLoc == null || danhSachTheDangLoc.isEmpty()
                    || baiCoItNhat1TheTrongDanhSach(bai.getTheGan(), danhSachTheDangLoc);

            if (khopTimKiem && khopThe) {
                ketQua.add(bai);
            }
        }

        if (!moiNhatTruoc) {
            Collections.sort(ketQua, (baiA, baiB) -> baiA.getNgayThang().compareTo(baiB.getNgayThang()));
        }

        return ketQua;
    }

    private boolean baiCoItNhat1TheTrongDanhSach(String theGanCuaBai, List<String> danhSachDangLoc) {
        if (theGanCuaBai == null || theGanCuaBai.trim().isEmpty()) {
            return false;
        }
        for (String the : theGanCuaBai.split(",")) {
            if (danhSachDangLoc.contains(the.trim())) {
                return true;
            }
        }
        return false;
    }

    private String boDauTiengViet(String chuoiGoc) {
        if (chuoiGoc == null) return "";
        String daChuanHoa = Normalizer.normalize(chuoiGoc, Normalizer.Form.NFD);
        daChuanHoa = daChuanHoa.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        daChuanHoa = daChuanHoa.replace('đ', 'd').replace('Đ', 'D');
        return daChuanHoa.toLowerCase(Locale.getDefault());
    }

    // ===================== LỊCH =====================

    public Set<String> layDanhSachNgayCoBaiTrongThang(String thangNam) {
        Set<String> ketQua = new HashSet<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT DISTINCT " + COL_NGAY_THANG + " FROM " + TABLE_NHAT_KY +
                " WHERE " + COL_NGAY_THANG + " LIKE ?";
        Cursor cursor = db.rawQuery(query, new String[]{thangNam + "%"});

        if (cursor.moveToFirst()) {
            do {
                ketQua.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return ketQua;
    }

    public List<DiaryEntry> layDanhSachBaiTheoNgay(String ngayThang) {
        List<DiaryEntry> danhSach = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NHAT_KY + " WHERE " + COL_NGAY_THANG + " = ?" +
                " ORDER BY " + COL_NGAY_TAO + " DESC";
        Cursor cursor = db.rawQuery(query, new String[]{ngayThang});

        if (cursor.moveToFirst()) {
            do {
                DiaryEntry entry = new DiaryEntry(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_THANG)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NOI_DUNG)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_TAM_TRANG)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_THE_GAN)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_TAO))
                );
                danhSach.add(entry);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return danhSach;
    }

    // ===================== KỶ NIỆM (MỚI) =====================

    // Lấy TẤT CẢ ảnh trong toàn bộ nhật ký, kèm ngay_thang của bài viết chứa
    // ảnh đó (JOIN AnhNhatKy với NhatKy qua nhat_ky_id) - dùng cho
    // MemoriesFragment để nhóm ảnh theo tháng/năm. Sắp mới nhất trước theo
    // ngay_thang của BÀI VIẾT (không phải lúc ảnh được thêm), khớp đúng cách
    // toàn app đang hiểu "mới nhất" (giống getAllDiaries() đang ORDER BY DESC).
    public List<AnhKyNiem> layTatCaAnhKemNgay() {
        List<AnhKyNiem> ketQua = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT " + TABLE_ANH_NHAT_KY + "." + COL_NHAT_KY_ID + ", "
                + TABLE_ANH_NHAT_KY + "." + COL_DUONG_DAN_ANH + ", "
                + TABLE_NHAT_KY + "." + COL_NGAY_THANG +
                " FROM " + TABLE_ANH_NHAT_KY +
                " INNER JOIN " + TABLE_NHAT_KY + " ON " + TABLE_ANH_NHAT_KY + "." + COL_NHAT_KY_ID
                + " = " + TABLE_NHAT_KY + "." + COL_ID +
                " ORDER BY " + TABLE_NHAT_KY + "." + COL_NGAY_THANG + " DESC, "
                + TABLE_ANH_NHAT_KY + "." + COL_THU_TU + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                AnhKyNiem anh = new AnhKyNiem(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COL_NHAT_KY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_DUONG_DAN_ANH)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COL_NGAY_THANG))
                );
                ketQua.add(anh);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return ketQua;
    }
}