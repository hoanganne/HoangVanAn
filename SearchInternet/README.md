# SearchApp – Ứng dụng Tìm kiếm Internet bằng Java

**SearchApp** là một ứng dụng Java Swing sử dụng **Google Custom Search API** để tìm kiếm thông tin trên Internet và hiển thị kết quả trong giao diện đồ họa. Ứng dụng hỗ trợ nhập nhiều từ khóa, lọc theo domain và mở kết quả trong trình duyệt.

---

## Yêu cầu hệ thống

* Java 17 trở lên
* Maven 3.x
* Kết nối Internet
* API Key và CSE ID của Google Custom Search

---

## Cấu trúc project

```
GoogleCSE/
 ├─ src/
 │   └─ main/
 │       ├─ java/
 │       │   └─ com/mycompany/googlecse/
 │       │       └─ SearchApp.java
 │       └─ resources/
 │           └─ config.properties
 ├─ pom.xml
 └─ README.md
```

**Mô tả:**

* `SearchApp.java`: file chính của ứng dụng GUI.
* `config.properties`: chứa **API Key** và **CSE ID**.
* `pom.xml`: cấu hình Maven, bao gồm thư viện Jackson để xử lý JSON.
* `README.md`: hướng dẫn sử dụng project.

---

## Cài đặt và cấu hình

1. Tải project về máy.
2. Tạo file `config.properties` trong thư mục `src/main/resources/` với nội dung:

```properties
google.api.key=AIzaSyD-mee3cLKTxcJnSEGIxVu6HexiA_DpVHg
google.cse.id=85c64d04066364223
```

> Thay bằng key hoặc CSE ID của bạn nếu khác.

3. Cài Maven nếu chưa có: [Hướng dẫn cài Maven](https://maven.apache.org/install.html)

---

## Chạy ứng dụng

### 1. Bằng Maven:

```bash
mvn clean install
mvn exec:java -Dexec.mainClass="com.mycompany.googlecse.SearchApp"
```

### 2. Bằng IDE (NetBeans, IntelliJ, Eclipse):

* Mở project Maven.
* Chạy class `SearchApp.java` trực tiếp.

---

## Hướng dẫn sử dụng GUI

1. Nhập **Từ khóa 1** (bắt buộc).
2. Nhập **Từ khóa 2** và **Từ khóa 3** (tùy chọn).
3. Nhấn **Tìm kiếm**.
4. Kết quả hiển thị ở cột **Kết quả**.
5. Click đúp vào kết quả để mở trình duyệt.
6. Lọc kết quả theo **domain** từ danh sách bên trái hoặc chọn “Tất cả”.

---

## Các tính năng nổi bật

* Tìm kiếm với nhiều từ khóa.
* Lọc kết quả theo domain.
* Click mở kết quả trong trình duyệt mặc định.
* Giao diện Swing trực quan, dễ sử dụng.

---

## Lưu ý

* API Key có hạn mức miễn phí ~100 lượt truy vấn/ngày.
* Số lượng kết quả tối đa là 10.
* Máy tính cần kết nối Internet.
* Không thay đổi cấu trúc thư mục `src/main/resources/`.

---

## Mở rộng ý tưởng

* Thêm phân trang kết quả.
* Thêm tìm kiếm hình ảnh.
* Lưu lịch sử tìm kiếm.
* Tùy chỉnh giao diện Swing (màu sắc, font chữ, bố cục).
* Xử lý lỗi mạng hoặc quota hết hạn.

---

## License

Project này là **demo học tập**, không có license thương mại.
