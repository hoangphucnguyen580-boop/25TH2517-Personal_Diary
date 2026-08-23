package ntu.nguyenhoangphuc.personal_diary.model;

// Model đại diện cho 1 ảnh gắn kèm bài nhật ký, ánh xạ với bảng AnhNhatKy
// Quan hệ: 1 DiaryEntry có thể có nhiều DiaryPhoto (nhatKyId là khoá ngoại)
public class DiaryPhoto {

    private int id;
    private int nhatKyId;       // Khoá ngoại, trỏ về id của DiaryEntry
    private String duongDanAnh; // Đường dẫn/URI ảnh trong máy
    private String chuThich;    // Caption nhỏ, có thể null
    private String icon;        // Icon gắn lên ảnh, có thể null
    private int thuTu;          // Vị trí ảnh trong dải ảnh, từ 0 đến 4 (tối đa 5 ảnh)

    // khởi tạo KHÔNG có id — dùng khi tạo ảnh mới trước khi insert vào SQLite
    public DiaryPhoto(int nhatKyId, String duongDanAnh, String chuThich, String icon, int thuTu) {
        this.nhatKyId = nhatKyId;
        this.duongDanAnh = duongDanAnh;
        this.chuThich = chuThich;
        this.icon = icon;
        this.thuTu = thuTu;
    }

    // Khởi tạo CÓ id — dùng khi đọc dữ liệu từ SQLite ra
    public DiaryPhoto(int id, int nhatKyId, String duongDanAnh, String chuThich, String icon, int thuTu) {
        this.id = id;
        this.nhatKyId = nhatKyId;
        this.duongDanAnh = duongDanAnh;
        this.chuThich = chuThich;
        this.icon = icon;
        this.thuTu = thuTu;
    }

    public int getId() {
        return id;
    }

    public int getNhatKyId() {
        return nhatKyId;
    }

    public String getDuongDanAnh() {
        return duongDanAnh;
    }

    public String getChuThich() {
        return chuThich;
    }

    public String getIcon() {
        return icon;
    }

    public int getThuTu() {
        return thuTu;
    }
}
