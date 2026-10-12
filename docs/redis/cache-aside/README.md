# Cache-aside

## Mục tiêu

Hiểu ứng dụng phối hợp database và Redis theo kiểu cache-aside, biết cache miss/hit diễn ra thế nào, và nhận ra các giới hạn về hết hạn, invalidation và đồng thời.

## Mô hình

Ứng dụng tự kiểm tra cache trước. Nếu key chưa có, ứng dụng đọc database, đưa kết quả vào Redis với TTL rồi trả về. Redis không tự tìm dữ liệu trong database.

```text
GET /products/42
    ├─ Redis GET product:42 → hit  → trả kết quả
    └─ Redis GET product:42 → miss → đọc database
                                      ├─ không có → trả 404
                                      └─ có → Redis SET ... EX 60 → trả kết quả
```

### Đọc dữ liệu

1. Đọc key trong Redis.
2. Nếu hit, trả dữ liệu trong cache.
3. Nếu miss, truy vấn database.
4. Nếu tìm thấy dữ liệu, ghi vào Redis với TTL rồi trả về.

### Cập nhật dữ liệu

1. Ghi database trước.
2. Khi ghi thành công, xóa key cache tương ứng.
3. Request đọc tiếp theo sẽ miss và nạp giá trị mới.

Xóa key thường dễ hiểu hơn cập nhật cache trực tiếp vì database là nguồn dữ liệu chính. Việc ghi database và xóa Redis không nằm trong cùng một transaction; nếu Redis lỗi sau khi database commit, cache cũ có thể còn đến khi hết TTL. Hệ thống cần quyết định sẽ báo lỗi, thử xóa lại, hay chấp nhận độ trễ nhất quán đó.

## Ưu và nhược điểm

**Ưu điểm**

- Chỉ cache dữ liệu thực sự được đọc.
- Database vẫn là nguồn dữ liệu chính; cache mất thì có thể nạp lại.
- Ứng dụng kiểm soát rõ cách nạp và thời hạn cache.

**Đánh đổi**

- Lần đọc đầu sau miss chậm hơn và vẫn phụ thuộc database.
- Có thể trả dữ liệu cũ nếu invalidation thất bại hoặc đua với request đang nạp cache.
- Nhiều request cùng miss một key có thể cùng truy vấn database (cache stampede).
- TTL giới hạn thời gian sống của key nhưng không tự đảm bảo dữ liệu luôn mới.

## Các tình huống cần thử

- Đọc cùng một key hai lần: lần đầu miss, lần sau hit.
- Chờ TTL hết hạn: request tiếp theo đọc lại database.
- Cập nhật database rồi xóa cache: lần đọc sau nhận giá trị mới.
- Gửi nhiều request cùng lúc khi key miss: đếm số truy vấn database.
- Giả lập Redis không sẵn sàng sau khi database đã commit: xác định hành vi mong muốn.
- Gửi ID không tồn tại nhiều lần: cân nhắc negative caching nếu đây là tải thực tế.

## Thực hành

- Thử lệnh trong [Redis CLI](../../../redis/cache-aside/redis-commands.md).
- Chạy [Spring Boot cache-aside](../../../redis/cache-aside/spring/README.md) để quan sát luồng qua HTTP, H2 và Redis.

## Tham khảo

- [Redis: Cache-aside pattern](https://redis.io/docs/latest/develop/use-cases/cache-aside/)
