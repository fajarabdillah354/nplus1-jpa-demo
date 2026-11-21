# N+1 Query Problem di JPA/Hibernate

Studi kasus kecil buat ngelihat langsung **N+1 query problem** dan **4 cara ngatasinnya**, pakai Spring Boot + Spring Data JPA.

Nggak perlu install database. Project ini pakai H2 in-memory, jadi tinggal clone dan jalanin.

## Kasusnya

Ada toko online dengan dua tabel:

```
orders (100 baris)  1 ──── N  order_items (300 baris)
```

Kita mau nampilin daftar semua order, lengkap dengan **jumlah item** dan **total harga** tiap order.

Kelihatannya gampang:

```java
List<Order> orders = orderRepository.findAll();   // 1 query

for (Order order : orders) {
    order.getItems().size();                      // +1 query, TIAP order
}
```

Hasilnya: **101 query** buat 100 order. Itulah N+1.

## Hasil perbandingan

Semua endpoint ngembaliin data yang **sama persis**. Yang beda cuma jumlah query-nya.

| Endpoint | Strategi | Jumlah query |
|----------|----------|--------------|
| `/api/orders/n-plus-one` | `findAll()` + `getItems()` di loop | **101** |
| `/api/orders/join-fetch` | `JOIN FETCH` | **1** |
| `/api/orders/entity-graph` | `@EntityGraph` | **1** |
| `/api/orders/batch-fetch` | Batch fetching (size 50) | **3** |
| `/api/orders/dto-projection` | DTO projection | **1** |

Mau lihat semuanya sekaligus? Buka `/api/orders/compare`.

## Cara jalanin

Butuh **Java 17+** dan **Maven**.

```bash
git clone <url-repo-ini>
cd nplus1-jpa-demo
mvn spring-boot:run
```

Lalu buka di browser atau pakai curl:

```bash
curl http://localhost:8080/api/orders/compare
```

Contoh hasilnya:

```json
[
  { "strategy": "n-plus-one",     "queryCount": 101, "durationMs": 45, "orderCount": 100 },
  { "strategy": "join-fetch",     "queryCount": 1,   "durationMs": 8,  "orderCount": 100 },
  { "strategy": "entity-graph",   "queryCount": 1,   "durationMs": 6,  "orderCount": 100 },
  { "strategy": "batch-fetch",    "queryCount": 3,   "durationMs": 7,  "orderCount": 100 },
  { "strategy": "dto-projection", "queryCount": 1,   "durationMs": 3,  "orderCount": 100 }
]
```

> `durationMs` bakal beda-beda di tiap komputer. Yang penting diperhatiin itu `queryCount`.

Sambil manggil endpoint, **lihat console-nya**. Semua SQL ditampilin di sana, jadi kamu bisa lihat sendiri 100 query yang berulang di `/n-plus-one`.

## Struktur project

Arsitekturnya layered biasa, biar gampang diikutin:

```
src/main/java/com/codejar/nplusone
├── controller/
│   └── OrderController.java     ← endpoint REST
├── service/
│   └── OrderService.java        ← 1 masalah + 4 solusi, baca dari sini
├── repository/
│   └── OrderRepository.java     ← query JOIN FETCH, @EntityGraph, DTO
├── entity/
│   ├── Order.java               ← @OneToMany (lazy)
│   └── OrderItem.java
├── dto/
│   ├── OrderSummary.java        ← hasil ringkasan tiap order
│   └── DemoResult.java          ← hasil + jumlah query
└── config/
    ├── QueryCounter.java        ← penghitung query
    └── DataSeeder.java          ← isi data contoh
```

Alurnya: `Controller → Service → Repository → Database`.

Kalau cuma punya waktu 5 menit, baca dua file ini: **`OrderService.java`** dan **`OrderRepository.java`**.

## Penjelasan tiap strategi

### Masalahnya: N+1

`@OneToMany` itu **lazy** secara default. Waktu `Order` di-load, `items`-nya belum diambil. Hibernate baru query pas `getItems()` dipakai. Karena dipanggilnya di dalam loop, query-nya jadi satu per order.

```sql
select * from orders;                              -- 1 query
select * from order_items where order_id = 1;      -- +1
select * from order_items where order_id = 2;      -- +1
-- ... sampai order ke-100
```

### Solusi 1: JOIN FETCH

```java
@Query("select o from Order o join fetch o.items")
List<Order> findAllWithJoinFetch();
```

Order dan item diambil sekaligus pakai JOIN. **1 query.**

Hati-hati: jangan digabung sama pagination (`Pageable`) kalau yang di-fetch itu list. Hibernate bakal narik semua data ke memory dulu.

### Solusi 2: @EntityGraph

```java
@EntityGraph(attributePaths = "items")
@Query("select o from Order o")
List<Order> findAllWithEntityGraph();
```

Efeknya sama kayak JOIN FETCH, tapi cukup nyebut nama relasinya. **1 query.**

### Solusi 3: Batch fetching

Kodenya sama persis kayak versi N+1. Bedanya, Hibernate ngambil item secara borongan:

```sql
select * from orders;
select * from order_items where order_id in (1, 2, ..., 50);
select * from order_items where order_id in (51, 52, ..., 100);
```

**3 query**, dan aman dipakai bareng pagination.

Di project beneran, biasanya cukup satu baris di `application.properties`:

```properties
spring.jpa.properties.hibernate.default_batch_fetch_size=50
```

Di demo ini batch size diatur per session (lihat `OrderService.batchFetch()`), supaya endpoint `/n-plus-one` tetap nunjukin masalah aslinya dan bisa dibandingin.

### Solusi 4: DTO projection

```java
@Query("""
        select new com.codejar.nplusone.dto.OrderSummary(
            o.id, o.customerName, count(i), sum(i.price * i.quantity))
        from Order o left join o.items i
        group by o.id, o.customerName
        order by o.id
        """)
List<OrderSummary> findAllSummaries();
```

Nggak ngambil entity sama sekali. Jumlah item dan total harga dihitung langsung di database. **1 query**, dan paling ringan.

## Jadi, pilih yang mana?

| Kebutuhan | Pakai |
|-----------|-------|
| Butuh entity + relasinya, tanpa pagination | JOIN FETCH atau `@EntityGraph` |
| Ada pagination, atau relasinya banyak | Batch fetching |
| Cuma buat ditampilin (list, tabel, laporan) | DTO projection |

## Gimana cara ngitung query-nya?

Lihat `config/QueryCounter.java`. Class ini implement `StatementInspector` punya Hibernate, yang dipanggil tiap kali Hibernate mau ngirim SQL. Kita cukup nambah angka di situ.

## Test

```bash
mvn test
```

Test di `OrderServiceTest.java` ngunci jumlah query tiap strategi (101, 1, 1, 3, 1), dan mastiin semua strategi ngembaliin data yang sama. Pola ini bisa kamu tiru di project sendiri, biar N+1 ketahuan sebelum sampai production.

## Coba eksperimen sendiri

- Ubah `demo.order-count` di `application.properties` jadi `1000`, lalu lihat `/compare` lagi.
- Aktifin `default_batch_fetch_size=50` di `application.properties`, lalu panggil `/n-plus-one`. Query-nya turun jadi berapa?
- Ganti `@OneToMany` jadi `fetch = FetchType.EAGER`, lalu panggil `/n-plus-one`. N+1-nya hilang nggak?

## Tech stack

Java 17+, Spring Boot 3.5, Spring Data JPA (Hibernate 6), H2.

---

Dibuat oleh **Codejar** sebagai pelengkap konten edukasi di LinkedIn. Kalau ada yang bingung atau nemu yang keliru, buka issue aja.
