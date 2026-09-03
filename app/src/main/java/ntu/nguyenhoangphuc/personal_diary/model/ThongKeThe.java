package ntu.nguyenhoangphuc.personal_diary.model;

// Model đại diện cho 1 hàng trong "Thẻ hay dùng" ở StatsFragment - ghép tên
// thẻ với số lần thẻ đó xuất hiện trong toàn bộ nhật ký
public class ThongKeThe {

    private final String tenThe;
    private final int soLanDung;

    public ThongKeThe(String tenThe, int soLanDung) {
        this.tenThe = tenThe;
        this.soLanDung = soLanDung;
    }

    public String getTenThe() {
        return tenThe;
    }

    public int getSoLanDung() {
        return soLanDung;
    }
}