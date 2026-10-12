# Redis cho backend engineering

Mục tiêu: hiểu Redis như một kho dữ liệu trong bộ nhớ, biết chọn đúng cách dùng và dự đoán điều gì xảy ra khi key hết hạn, bộ nhớ đầy, worker chết hoặc Redis failover.

## Nền tảng cần nắm trước

- Kiểu dữ liệu: String, Hash, List, Set, Sorted Set, Stream; chọn cấu trúc theo thao tác cần làm và độ phức tạp của lệnh.
- Key, TTL, `EXPIRE`, `SCAN`, `maxmemory` và chính sách eviction. TTL là thời hạn của key; eviction là việc Redis loại key khi chạm giới hạn bộ nhớ.
- Các lệnh nguyên tử trên một Redis instance; khi cần nhiều bước, tìm hiểu `MULTI`/`EXEC`, `WATCH` và Lua script. Không mặc định một chuỗi nhiều lệnh từ client là nguyên tử.

## 10 chủ đề ban đầu

| # | Chủ đề | Trọng tâm cần học |
| --- | --- | --- |
| 1 | Cache-aside | Đọc cache trước, tải từ database khi miss; TTL, cập nhật và xóa cache sau khi ghi database. |
| 2 | Distributed lock | `SET NX` kèm TTL, token sở hữu lock, chỉ chủ sở hữu được mở khóa; xử lý lock hết hạn khi công việc chưa xong. |
| 3 | Rate limiting | Fixed window, sliding window, token bucket; chọn key theo user/IP, đặt TTL và bảo đảm cập nhật nguyên tử. |
| 4 | Session store | Lưu session dùng chung giữa các app instance; TTL, gia hạn session, đăng xuất và tác động khi Redis mất dữ liệu. |
| 5 | Redis Streams | `XADD`, `XREADGROUP`, `XACK`, pending entries, reclaim và giới hạn độ dài stream; consumer có thể nhận lại message. |
| 6 | Delayed queue | Dùng Sorted Set với score là thời điểm chạy và worker lấy job đến hạn; thêm cơ chế claim, retry, timeout để tránh mất job. Đây là pattern xây trên Redis, không phải kiểu dữ liệu riêng. |
| 7 | Pub/Sub | Gửi thông báo tức thời cho subscriber đang kết nối; subscriber offline sẽ bỏ lỡ message. So sánh với Streams khi cần đọc lại hoặc xác nhận xử lý. |
| 8 | Idempotency | Dùng key + TTL để hỗ trợ chống xử lý lặp, nhưng vẫn phải xét giao dịch database và side effect. Xem topic [Idempotency](../idempotency/README.md). |
| 9 | Hot key | Một key bị truy cập quá nhiều làm nghẽn một instance/shard; đo lưu lượng theo key và cân nhắc cache cục bộ hoặc thay đổi cách phân phối đọc. |
| 10 | Cache breakdown (stampede) | Một key phổ biến hết hạn khiến nhiều request cùng truy vấn database; dùng single-flight/lock, làm mới sớm hoặc phục vụ dữ liệu cũ có kiểm soát. |

## Bắt đầu với cache-aside

Học theo ba lớp: nắm luồng và các lỗi nhất quán, thử các lệnh Redis độc lập, rồi chạy app Spring Boot kết nối Redis và H2.

- [Bài học cache-aside](./cache-aside/README.md)
- [Lệnh Redis CLI](../../redis/cache-aside/redis-commands.md)
- [Ứng dụng Spring Boot](../../redis/cache-aside/spring/README.md)

## Chủ đề nên bổ sung

| Ưu tiên | Chủ đề | Vì sao cần học |
| --- | --- | --- |
| Cao | Cache invalidation và consistency | Ghi database rồi xóa/cập nhật cache có race condition; xác định mức độ dữ liệu cũ chấp nhận được và cách xử lý khi xóa cache thất bại. |
| Cao | Cache penetration và cache avalanche | Penetration: request liên tục hỏi dữ liệu không tồn tại. Avalanche: nhiều key hết hạn cùng lúc. Phân biệt chúng với breakdown của *một hot key*. |
| Cao | TTL, memory và eviction | Hiểu `maxmemory`, LRU/LFU/`noeviction`, key bị loại trước TTL và hậu quả khi Redis từ chối lệnh ghi. |
| Cao | Atomicity và concurrency | Dùng `SET NX`, `INCR`, `MULTI`/`EXEC`, `WATCH` hoặc Lua đúng chỗ; thử các tình huống nhiều client cùng cập nhật. |
| Cao | Persistence và phục hồi | So sánh RDB, AOF và trường hợp không lưu xuống đĩa; chọn theo mức mất dữ liệu cho phép, rồi thử restart/restore. |
| Cao | Replication, Sentinel và Cluster | Hiểu replication bất đồng bộ, failover, khả năng mất write gần thời điểm failover và sharding trong Cluster. |
| Trung bình | Big key và hiệu năng lệnh | Big key khác hot key: một key chứa quá nhiều dữ liệu. Theo dõi kích thước key, tránh lệnh O(N) trên tập lớn trong request path, dùng `SCAN` thay vì quét toàn bộ keyspace bằng `KEYS`. |
| Trung bình | Quan sát và xử lý sự cố | Theo dõi hit/miss, `evicted_keys`, memory, latency, slow log, replication lag và độ dài pending của Streams; đo trước khi tối ưu. |
| Trung bình | Reliable job queue | Nếu dùng Redis làm queue, học claim, visibility timeout, retry, deduplication và dead-letter queue; delayed queue chỉ giải quyết thời điểm bắt đầu chạy. |

## Thứ tự học gợi ý

1. Nền tảng → cache-aside → cache invalidation → TTL/eviction → penetration, avalanche, breakdown, hot key.
2. Atomicity → rate limiting → distributed lock → idempotency → session store.
3. Pub/Sub → Streams → delayed queue → reliable job queue.
4. Persistence → replication/failover/Cluster → quan sát hiệu năng và big key.

## Thí nghiệm nhỏ nên làm

- Cho nhiều request cùng đọc một key vừa hết hạn; đếm số lần database bị gọi trước và sau khi thêm single-flight.
- Chạy nhiều client cùng tăng counter; so sánh `GET` + `SET` với `INCR` hoặc Lua.
- Tắt consumer Streams sau khi nhận message nhưng trước `XACK`; khởi động lại và kiểm tra pending/reclaim cùng việc xử lý trùng.
- Restart Redis sau khi ghi dữ liệu với các cấu hình persistence khác nhau; kiểm tra key nào còn và key nào mất.

## Tài liệu tham khảo

- [Redis use cases](https://redis.io/docs/latest/develop/use-cases/)
- [Redis data types](https://redis.io/docs/latest/develop/data-types/)
- [Key expiration](https://redis.io/docs/latest/develop/using-commands/keyspace/) và [key eviction](https://redis.io/docs/latest/develop/reference/eviction/)
- [Distributed locks](https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/)
- [Redis Streams](https://redis.io/docs/latest/develop/data-types/streams/) và [job queue](https://redis.io/docs/latest/develop/use-cases/job-queue/)
- [Persistence](https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/), [replication](https://redis.io/docs/latest/operate/oss_and_stack/management/replication/) và [Sentinel](https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/)
