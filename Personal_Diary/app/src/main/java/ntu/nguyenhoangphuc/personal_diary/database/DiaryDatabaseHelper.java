package ntu.nguyenhoangphuc.personal_diary.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

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

    // xoá bảng cũ rồi tạo lại cho đơn giản
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANH_NHAT_KY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NHAT_KY);
        onCreate(db);
    }

    //CRUD cơ bản cho NhatKy

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

    //CRUD cơ bản cho AnhNhatKy

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
}