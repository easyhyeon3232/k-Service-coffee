insert ignore into member (id, created_at, updated_at)
values
    (1, now(), now()),
    (2, now(), now()),
    (3, now(), now());

insert ignore into coffee_menu (id, name, price, status, created_at, updated_at)
values
    (1, 'Americano', 3000, 'ON_SALE', now(), now()),
    (2, 'Latte', 4000, 'ON_SALE', now(), now()),
    (3, 'Mocha', 4500, 'ON_SALE', now(), now()),
    (4, 'Vanilla Latte', 4800, 'SOLD_OUT', now(), now()),
    (5, 'Cold Brew', 4200, 'HIDDEN', now(), now());

# insert ignore into point_wallet (id, member_id, version, balance, created_at, updated_at)
# values
#     (1, 1, 0, 15000, now(), now()),
#     (2, 2, 0, 9000, now(), now()),
#     (3, 3, 0, 3000, now(), now());

# insert ignore into coffee_order (id, member_id, menu_id, order_price, status, ordered_at, created_at, updated_at)
# values
#     (1, 1, 1, 3000, 'COMPLETED', now() - interval 1 day, now(), now()),
#     (2, 1, 2, 4000, 'COMPLETED', now() - interval 2 day, now(), now()),
#     (3, 2, 1, 3000, 'COMPLETED', now() - interval 3 day, now(), now()),
#     (4, 2, 1, 3000, 'COMPLETED', now() - interval 4 day, now(), now()),
#     (5, 3, 3, 4500, 'CANCELED', now() - interval 2 day, now(), now()),
#     (6, 1, 2, 4000, 'COMPLETED', now() - interval 8 day, now(), now());

# insert ignore into point_history (id, member_id, type, amount, balance_after, reference_order_id, created_at)
# values
#     (1, 1, 'CHARGE', 15000, 15000, null, now() - interval 5 day),
#     (2, 1, 'USE', 3000, 12000, 1, now() - interval 1 day),
#     (3, 1, 'USE', 4000, 8000, 2, now() - interval 2 day),
#     (4, 2, 'CHARGE', 15000, 15000, null, now() - interval 6 day),
#     (5, 2, 'USE', 3000, 12000, 3, now() - interval 3 day),
#     (6, 2, 'USE', 3000, 9000, 4, now() - interval 4 day),
#     (7, 3, 'CHARGE', 7500, 7500, null, now() - interval 3 day),
#     (8, 3, 'USE', 4500, 3000, 5, now() - interval 2 day);

# insert ignore into order_outbox (id, order_id, payload, status, retry_count, last_attempt_at, created_at, updated_at)
# values
#     (1, 1, '{"memberId":1,"menuId":1,"orderPrice":3000}', 'SENT', 0, now() - interval 1 day, now(), now()),
#     (2, 2, '{"memberId":1,"menuId":2,"orderPrice":4000}', 'SENT', 0, now() - interval 2 day, now(), now()),
#     (3, 3, '{"memberId":2,"menuId":1,"orderPrice":3000}', 'SENT', 1, now() - interval 3 day, now(), now()),
#     (4, 4, '{"memberId":2,"menuId":1,"orderPrice":3000}', 'PENDING', 0, null, now(), now()),
#     (5, 5, '{"memberId":3,"menuId":3,"orderPrice":4500}', 'FAILED', 2, now() - interval 2 day, now(), now());
