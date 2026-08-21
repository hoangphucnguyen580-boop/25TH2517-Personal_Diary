package ntu.nguyenhoangphuc.personal_diary.model;

// Model đại diện cho 1 bài nhật ký
public class DiaryEntry {

    private int id;
    private String ngayThang;   // Định dạng yyyy-MM-dd
    private String noiDung;     // Nội dung bài viết
    private String tamTrang;    // Mã emoji
    private String theGan;      // Tên thẻ/nhãn
    private String ngayTao;     // Timestamp lúc tạo record

    public DiaryEntry(String ngayThang, String noiDung, String tamTrang, String theGan, String ngayTao) {
        this.ngayThang = ngayThang;
        this.noiDung = noiDung;
        this.tamTrang = tamTrang;
        this.theGan = theGan;
        this.ngayTao = ngayTao;
    }

    // Constructor CÓ id — dùng khi đọc dữ liệu từ SQLite ra
    public DiaryEntry(int id, String ngayThang, String noiDung, String tamTrang, String theGan, String ngayTao) {
        this.id = id;
        this.ngayThang = ngayThang;
        this.noiDung = noiDung;
        this.tamTrang = tamTrang;
        this.theGan = theGan;
        this.ngayTao = ngayTao;
    }

    public int getId() {
        return id;
    }

    public String getNgayThang() {
        return ngayThang;
    }

    public String getNoiDung() {
        return noiDung;
    }

    public String getTamTrang() {
        return tamTrang;
    }

    public String getTheGan() {
        return theGan;
    }

    public String getNgayTao() {
        return ngayTao;
    }

    public void setNgayThang(String ngayThang) {
        this.ngayThang = ngayThang;
    }

    public void setNoiDung(String noiDung) {
        this.noiDung = noiDung;
    }

    public void setTamTrang(String tamTrang) {
        this.tamTrang = tamTrang;
    }

    public void setTheGan(String theGan) {
        this.theGan = theGan;
    }
}
