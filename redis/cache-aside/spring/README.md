# Cache-aside với Spring Boot

Project nhỏ để quan sát ứng dụng đọc Redis trước, fallback sang H2 khi miss, và xóa cache sau khi cập nhật database.

## Yêu cầu

- JDK 25
- Docker Compose
- Maven (hoặc cài Maven Wrapper cho project)

## Chạy

Khởi động Redis:

```bash
docker compose up -d
```

Chạy ứng dụng từ thư mục project:

```bash
mvn spring-boot:run
```

H2 chạy in-memory và được tạo dữ liệu mẫu khi ứng dụng khởi động. Redis chạy tại `localhost:6379`.

## Quan sát cache hit và miss

Gọi lần đầu sẽ thấy log `Cache MISS`, đọc H2 rồi ghi `product:1` vào Redis với TTL 60 giây:

```bash
curl http://localhost:8080/products/1
```

Gọi lại cùng URL trong 60 giây sẽ thấy log `Cache HIT` và trả từ Redis. Có thể kiểm tra key trực tiếp:

```bash
docker compose exec redis redis-cli GET product:1
docker compose exec redis redis-cli TTL product:1
```

## Cập nhật và invalidation

Endpoint cập nhật ghi H2 trước rồi xóa key Redis:

```bash
curl -X PUT http://localhost:8080/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Ergonomic Keyboard"}'
```

Gọi `GET /products/1` lần tiếp theo sẽ miss, đọc tên mới từ H2 và nạp lại cache.

## Lần theo code

- `ProductService.findById`: cache lookup → database fallback → cache set kèm TTL.
- `ProductService.save`: database write → cache delete.
- `ProductRepository`: thao tác với H2 qua `JdbcTemplate`.
- `ProductController`: hai endpoint để tạo luồng đọc và cập nhật.

Ứng dụng cố ý để luồng dễ đọc. Chưa có negative caching, chống stampede, retry invalidation, timeout/fallback khi Redis lỗi hay xác thực request. Nếu Redis không sẵn sàng, request hiện tại sẽ lỗi; nếu Redis lỗi sau khi H2 commit ở endpoint PUT, dữ liệu H2 đã đổi nhưng API có thể trả lỗi và cache cũ sống đến khi TTL hết.

Đọc thêm phần khái niệm tại [bài học cache-aside](../../docs/redis/cache-aside/README.md) và [lệnh Redis CLI](../cache-aside/redis-commands.md).
