# Cache-aside bằng Redis CLI

Cache-aside là luồng do ứng dụng điều phối. Các lệnh dưới đây chỉ minh họa phần đọc/ghi cache trên Redis; dòng `load from database` là bước ứng dụng thực hiện bên ngoài Redis.

## Đọc một sản phẩm

```redis
GET product:42
```

- Nếu trả về giá trị, cache hit: ứng dụng trả dữ liệu này cho caller.
- Nếu trả về `(nil)`, cache miss: ứng dụng tải dữ liệu từ database.

Sau khi ứng dụng lấy được `Mechanical Keyboard` từ database:

```redis
SET product:42 "Mechanical Keyboard" EX 60
```

`EX 60` đặt TTL 60 giây. Sau đó ứng dụng trả dữ liệu vừa tải. Nếu database không có sản phẩm, ứng dụng có thể trả 404; ví dụ cơ bản này không cache kết quả không tồn tại.

## Cập nhật

Ứng dụng ghi dữ liệu mới xuống database trước. Chỉ sau khi database ghi thành công, xóa cache:

```redis
DEL product:42
```

Lần đọc sau sẽ miss, tải dữ liệu mới từ database, rồi `SET` lại cache. Không chạy lệnh ghi database trong `redis-cli`: database là hệ thống riêng.

## Quan sát TTL

```redis
TTL product:42
```

Kết quả `-2` nghĩa là key không tồn tại; `-1` nghĩa là key không có expiration. Với cache-aside, key cache thường nên có TTL hữu hạn để dữ liệu cũ tự biến mất ngay cả khi invalidation bị bỏ lỡ.

## Giới hạn của ví dụ

Chuỗi `GET` → đọc database → `SET` không nguyên tử. Hai client cùng miss có thể cùng đọc database. Tương tự, database commit và `DEL` trên Redis là hai thao tác riêng biệt. Ví dụ CLI giúp hiểu lệnh; ứng dụng thực tế cần xử lý lỗi, timeout, serialization, stampede và chính sách nhất quán.
