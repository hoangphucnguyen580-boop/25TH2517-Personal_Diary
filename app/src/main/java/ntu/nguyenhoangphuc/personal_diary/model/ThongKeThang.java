package ntu.nguyenhoangphuc.personal_diary.model;

// Model đại diện cho 1 cột trong biểu đồ "Số bài theo tháng" ở StatsFragment -
// ghép nhãn tháng hiển thị (vd "T3") với số bài viết trong tháng đó
public class ThongKeThang {

    private final String nhanThang; // vd "T3", "T4"...
    private final int soBai;

    public ThongKeThang(String nhanThang, int soBai) {
        this.nhanThang = nhanThang;
        this.soBai = soBai;
    }

    public String getNhanThang() {
        return nhanThang;
    }

    public int getSoBai() {
        return soBai;
    }
}