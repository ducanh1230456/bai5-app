# Quản lý khóa học

Ứng dụng Android native viết bằng Java, kết nối trực tiếp tới hai resource `categories` và `courses` trên MockAPI. Ứng dụng không thay dữ liệu API bằng danh sách cục bộ.

## Kết nối MockAPI

1. Tạo project trên MockAPI và thêm resource `categories` cùng `courses` theo các trường dưới đây.
2. Mở ứng dụng, chạm biểu tượng bánh răng và nhập Base URL, ví dụ `https://<project-id>.mockapi.io/api/v1`.
3. Ứng dụng ghi nhớ URL trên thiết bị và tải cả danh mục lẫn khóa học qua REST API.

`YOUR_PROJECT_ID` trong đề bài là chỗ giữ chỗ; thay bằng project ID riêng của bạn. Mỗi resource cần có trường `id` do MockAPI quản lý.

### Trường dữ liệu

| Resource | Trường |
| --- | --- |
| `categories` | `name` (String), `description` (String) |
| `courses` | `title` (String), `description` (String), `categoryId` (String), `instructor` (String), `price` (Number), `duration` (Number), `thumbnail` (String URL), `trailerUrl` (String URL), `createdAt` (String) |

### REST endpoints

| Hành động | Endpoint |
| --- | --- |
| Tải danh mục | `GET /categories` |
| Tạo danh mục | `POST /categories` |
| Sửa danh mục | `PUT /categories/{id}` |
| Xóa danh mục | `DELETE /categories/{id}` |
| Tải danh sách khóa học | `GET /courses` |
| Xem khóa học | lấy từ `GET /courses`, mở thẻ chi tiết |
| Tạo khóa học | `POST /courses` |
| Sửa khóa học | `PUT /courses/{id}` |
| Xóa khóa học | `DELETE /courses/{id}` |

## Chức năng

- Tải danh mục và khóa học, tìm theo tên khóa học/giảng viên, lọc theo danh mục.
- Xem mô tả, ảnh bìa, học phí, giảng viên và thời lượng; phát trailer từ URL video trực tiếp.
- Thêm, sửa, xóa khóa học và thêm, sửa, xóa danh mục. Không cho xóa danh mục đang được khóa học sử dụng.
- Báo trạng thái tải/lỗi mạng và kiểm tra dữ liệu nhập trước khi gửi API.

## Mở project

Mở thư mục này trong Android Studio và đồng bộ Gradle. Cần Android SDK Platform 35 và Android Gradle Plugin tải được từ Google Maven. Chọn thiết bị/emulator Android 6.0 trở lên rồi chạy cấu hình `app`.
