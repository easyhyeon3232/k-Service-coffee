# Indexing and EXPLAIN Guide

## 1. 적용한 인덱스

### 1.1 `coffee_order`

- `idx_coffee_order_status_ordered_at_menu_id (status, ordered_at, menu_id)`
- `idx_coffee_order_member_id_ordered_at (member_id, ordered_at)`

적용 이유:

- 인기 메뉴 조회는 `status = COMPLETED`와 `ordered_at >= 최근 7일` 조건으로 주문을 좁힌 뒤 메뉴별 집계를 수행한다.
- 회원 주문 이력 조회나 향후 마이페이지 조회는 `member_id`와 시간 조건이 함께 쓰일 가능성이 높다.

### 1.2 `order_outbox`

- `idx_order_outbox_status_created_at (status, created_at)`

적용 이유:

- Outbox 재시도 스케줄러는 `PENDING` 상태를 생성 시각 오름차순으로 반복 조회한다.
- 상태 필터와 정렬 조건이 함께 사용되므로 복합 인덱스가 유리하다.

### 1.3 `point_wallet`

- `idx_point_wallet_member_id (member_id)`

적용 이유:

- 포인트 충전과 주문 결제 모두 회원 기준으로 포인트 지갑을 조회한다.
- `member_id`는 사실상 핵심 조회 키이므로 명시적으로 인덱스를 둔다.

## 2. EXPLAIN 확인 대상 쿼리

## 2.1 인기 메뉴 집계 쿼리

```sql
EXPLAIN
SELECT
    co.menu_id,
    COUNT(co.id) AS order_count
FROM coffee_order co
WHERE co.status = 'COMPLETED'
  AND co.ordered_at >= NOW() - INTERVAL 7 DAY
GROUP BY co.menu_id
ORDER BY order_count DESC, co.menu_id ASC
LIMIT 3;
```

확인 포인트:

- `type`이 `ALL`보다 `range` 또는 더 나은 형태인지 확인
- `key`에 `idx_coffee_order_status_ordered_at_menu_id`가 잡히는지 확인
- `rows`가 전체 주문 테이블 스캔보다 줄어드는지 확인

## 2.2 회원 주문 이력 조회 쿼리

```sql
EXPLAIN
SELECT
    co.id,
    co.member_id,
    co.menu_id,
    co.order_price,
    co.ordered_at
FROM coffee_order co
WHERE co.member_id = 1
ORDER BY co.ordered_at DESC
LIMIT 20;
```

확인 포인트:

- `key`에 `idx_coffee_order_member_id_ordered_at`가 잡히는지 확인
- 회원별 주문 건수가 많아져도 불필요한 정렬 비용이 줄어드는지 확인

## 2.3 Outbox 재시도 조회 쿼리

```sql
EXPLAIN
SELECT
    oo.id,
    oo.order_id,
    oo.status,
    oo.created_at
FROM order_outbox oo
WHERE oo.status = 'PENDING'
ORDER BY oo.created_at ASC
LIMIT 100;
```

확인 포인트:

- `key`에 `idx_order_outbox_status_created_at`가 선택되는지 확인
- `Using filesort`가 줄어드는지 확인
- PENDING 데이터가 많아질수록 효과가 커지는지 확인

## 2.4 포인트 지갑 조회 쿼리

```sql
EXPLAIN
SELECT
    pw.id,
    pw.member_id,
    pw.balance,
    pw.version
FROM point_wallet pw
WHERE pw.member_id = 1;
```

확인 포인트:

- `key`에 `idx_point_wallet_member_id`가 선택되는지 확인
- 단건 조회에서 `rows`가 1에 가깝게 나오는지 확인

## 3. 실행 결과에서 보면 좋은 항목

- `type`: `ALL`이면 풀스캔 가능성이 높다.
- `key`: 실제 사용된 인덱스 이름이다.
- `rows`: 읽을 것으로 예상하는 행 수다.
- `Extra`: `Using filesort`, `Using temporary` 여부를 함께 본다.

## 4. 해석 기준

- 인기 메뉴 집계는 `최근 7일 + COMPLETED` 조건으로 먼저 범위를 좁히는 것이 핵심이다.
- Outbox는 상태 필터 후 생성 시각 정렬이 자주 일어나므로 복합 인덱스가 중요하다.
- 포인트 지갑은 회원 기준 단건 조회가 핵심이므로 단순 인덱스만으로도 충분하다.
- 인덱스는 무조건 많이 넣는 것보다 실제 조회 패턴과 `EXPLAIN` 결과를 기준으로 유지하는 것이 좋다.
