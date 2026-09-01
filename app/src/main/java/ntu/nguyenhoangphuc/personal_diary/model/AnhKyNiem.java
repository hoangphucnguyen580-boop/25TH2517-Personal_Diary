package ntu.nguyenhoangphuc.personal_diary.model;

public class AnhKyNiem {

    private final int nhatKyId;
    private final String duongDanAnh;
    private final String ngayThang; // yyyy-MM-dd, ngày của BÀI VIẾT chứa ảnh này

    public AnhKyNiem(int nhatKyId, String duongDanAnh, String ngayThang) {
        this.nhatKyId = nhatKyId;
        this.duongDanAnh = duongDanAnh;
        this.ngayThang = ngayThang;
    }

    public int getNhatKyId() {
        return nhatKyId;
    }

    public String getDuongDanAnh() {
        return duongDanAnh;
    }

    public String getNgayThang() {
        return ngayThang;
    }
}
