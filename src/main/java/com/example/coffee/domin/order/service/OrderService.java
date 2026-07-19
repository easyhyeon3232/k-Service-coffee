package com.example.coffee.domin.order.service;

import com.example.coffee.common.exception.BusinessException;
import com.example.coffee.common.exception.ErrorCode;
import com.example.coffee.domin.member.entity.Member;
import com.example.coffee.domin.member.repository.MemberRepository;
import com.example.coffee.domin.menu.entity.CoffeeMenu;
import com.example.coffee.domin.menu.repository.CoffeeMenuRepository;
import com.example.coffee.domin.order.dto.OrderCreateResponse;
import com.example.coffee.domin.order.entity.CoffeeOrder;
import com.example.coffee.domin.order.entity.IdempotencyKey;
import com.example.coffee.domin.order.entity.OrderOutbox;
import com.example.coffee.domin.order.repository.CoffeeOrderRepository;
import com.example.coffee.domin.order.repository.IdempotencyKeyRepository;
import com.example.coffee.domin.order.repository.OrderOutboxRepository;
import com.example.coffee.domin.point.entity.PointHistory;
import com.example.coffee.domin.point.entity.PointWallet;
import com.example.coffee.domin.point.repository.PointHistoryRepository;
import com.example.coffee.domin.point.repository.PointWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커피 주문, 포인트 차감, 주문 이력 저장과 outbox 이벤트 발행을 담당하는 서비스다.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final CoffeeMenuRepository coffeeMenuRepository;
    private final PointWalletRepository pointWalletRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final CoffeeOrderRepository coffeeOrderRepository;
    private final OrderOutboxRepository orderOutboxRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    // 메뉴 주문과 포인트 결제를 하나의 트랜잭션으로 처리한다.
    @Transactional
    public OrderCreateResponse order(Long memberId, Long menuId, String requestKey) {
        IdempotencyKey idempotencyKey = reserveIdempotencyKey(requestKey, memberId, menuId);
        if (idempotencyKey.isCompleted()) {
            return getExistingOrderResponse(idempotencyKey);
        }

        CoffeeMenu coffeeMenu = coffeeMenuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        if (!coffeeMenu.isOrderable()) {
            throw new BusinessException(ErrorCode.MENU_NOT_ON_SALE);
        }

        PointWallet pointWallet = pointWalletRepository.findByMemberIdForUpdate(memberId)
                .orElseGet(() -> PointWallet.create(findMember(memberId)));

        pointWallet.use(coffeeMenu.getPrice());

        CoffeeOrder coffeeOrder = coffeeOrderRepository.save(
                CoffeeOrder.completed(pointWallet.getMember(), coffeeMenu, coffeeMenu.getPrice())
        );

        pointHistoryRepository.save(
                PointHistory.use(pointWallet.getMember(), coffeeMenu.getPrice(), pointWallet.getBalance(), coffeeOrder.getId())
        );

        OrderOutbox orderOutbox = orderOutboxRepository.save(
                OrderOutbox.pending(coffeeOrder, createPayload(coffeeOrder))
        );

        idempotencyKey.complete(coffeeOrder);
        applicationEventPublisher.publishEvent(new OrderCreatedEvent(orderOutbox.getId()));

        return OrderCreateResponse.of(coffeeOrder, pointWallet.getBalance());
    }

    private IdempotencyKey reserveIdempotencyKey(String requestKey, Long memberId, Long menuId) {
        return idempotencyKeyRepository.findByRequestKey(requestKey)
                .map(savedKey -> validateExistingKey(savedKey, memberId, menuId))
                .orElseGet(() -> saveNewKey(requestKey, memberId, menuId));
    }

    private IdempotencyKey validateExistingKey(IdempotencyKey savedKey, Long memberId, Long menuId) {
        if (!savedKey.matches(memberId, menuId)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
        }

        if (!savedKey.isCompleted()) {
            throw new BusinessException(ErrorCode.ORDER_REQUEST_IN_PROGRESS);
        }

        return savedKey;
    }

    private IdempotencyKey saveNewKey(String requestKey, Long memberId, Long menuId) {
        try {
            return idempotencyKeyRepository.save(IdempotencyKey.reserve(requestKey, memberId, menuId));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.ORDER_REQUEST_IN_PROGRESS);
        }
    }

    private OrderCreateResponse getExistingOrderResponse(IdempotencyKey idempotencyKey) {
        CoffeeOrder coffeeOrder = coffeeOrderRepository.findById(idempotencyKey.getOrder().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));
        long remainingPoint = pointWalletRepository.findByMemberId(coffeeOrder.getMember().getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_POINT))
                .getBalance();

        return OrderCreateResponse.of(coffeeOrder, remainingPoint);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    private String createPayload(CoffeeOrder coffeeOrder) {
        return String.format(
                "{\"memberId\":%d,\"menuId\":%d,\"orderPrice\":%d}",
                coffeeOrder.getMember().getId(),
                coffeeOrder.getMenu().getId(),
                coffeeOrder.getOrderPrice()
        );
    }
}
