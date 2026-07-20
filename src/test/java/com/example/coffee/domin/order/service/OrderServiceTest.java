package com.example.coffee.domin.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CoffeeMenuRepository coffeeMenuRepository;

    @Mock
    private PointWalletRepository pointWalletRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private CoffeeOrderRepository coffeeOrderRepository;

    @Mock
    private OrderOutboxRepository orderOutboxRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private OrderEventPayloadMapper orderEventPayloadMapper;

    @Test
    @DisplayName("판매 중인 메뉴를 주문하면 포인트 차감, 주문 저장, outbox 저장이 함께 처리된다")
    void orderSuccess() {
        Member member = createMember(1L);
        CoffeeMenu coffeeMenu = createMenu(10L, 3000L, true);
        PointWallet pointWallet = PointWallet.create(member);
        pointWallet.charge(5000L);
        IdempotencyKey idempotencyKey = IdempotencyKey.reserve("order-1", 1L, 10L);

        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.empty());
        given(idempotencyKeyRepository.save(any(IdempotencyKey.class))).willReturn(idempotencyKey);
        given(coffeeMenuRepository.findById(10L)).willReturn(Optional.of(coffeeMenu));
        given(pointWalletRepository.findByMemberIdForUpdate(1L)).willReturn(Optional.of(pointWallet));
        given(coffeeOrderRepository.save(any(CoffeeOrder.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(pointHistoryRepository.save(any(PointHistory.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(orderEventPayloadMapper.toJson(any())).willReturn("{\"orderId\":1}");
        given(orderOutboxRepository.save(any(OrderOutbox.class))).willAnswer(invocation -> invocation.getArgument(0));

        OrderCreateResponse response = orderService.order(1L, 10L, "order-1");

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.menuId()).isEqualTo(10L);
        assertThat(response.orderPrice()).isEqualTo(3000L);
        assertThat(response.remainingPoint()).isEqualTo(2000L);
        verify(pointHistoryRepository).save(any(PointHistory.class));
        verify(orderOutboxRepository).save(any(OrderOutbox.class));
        verify(applicationEventPublisher).publishEvent(any(OrderCreatedEvent.class));
    }

    @Test
    @DisplayName("이미 완료된 idempotency key로 재요청하면 기존 주문 결과를 반환한다")
    void orderReturnsExistingResponseWhenIdempotencyKeyIsCompleted() {
        Member member = createMember(1L);
        CoffeeMenu coffeeMenu = createMenu(10L, 3000L, true);
        CoffeeOrder coffeeOrder = createOrder(100L, member, coffeeMenu, 3000L);
        PointWallet pointWallet = PointWallet.create(member);
        pointWallet.charge(2000L);
        IdempotencyKey idempotencyKey = IdempotencyKey.reserve("order-1", 1L, 10L);
        idempotencyKey.complete(coffeeOrder);

        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.of(idempotencyKey));
        given(coffeeOrderRepository.findById(100L)).willReturn(Optional.of(coffeeOrder));
        given(pointWalletRepository.findByMemberId(1L)).willReturn(Optional.of(pointWallet));

        OrderCreateResponse response = orderService.order(1L, 10L, "order-1");

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.menuId()).isEqualTo(10L);
        assertThat(response.orderPrice()).isEqualTo(3000L);
        assertThat(response.remainingPoint()).isEqualTo(2000L);
        verify(coffeeMenuRepository, never()).findById(anyLong());
        verify(orderOutboxRepository, never()).save(any(OrderOutbox.class));
    }

    @Test
    @DisplayName("같은 idempotency key를 다른 요청에 재사용하면 충돌 예외가 발생한다")
    void orderFailsWhenIdempotencyKeyIsReusedForDifferentRequest() {
        IdempotencyKey idempotencyKey = IdempotencyKey.reserve("order-1", 1L, 10L);
        idempotencyKey.complete(CoffeeOrder.completed(createMember(1L), createMenu(10L, 3000L, true), 3000L));

        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.of(idempotencyKey));

        assertThatThrownBy(() -> orderService.order(1L, 20L, "order-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
    }

    @Test
    @DisplayName("처리 중인 idempotency key로 재요청하면 진행 중 예외가 발생한다")
    void orderFailsWhenIdempotencyKeyIsInProgress() {
        IdempotencyKey idempotencyKey = IdempotencyKey.reserve("order-1", 1L, 10L);

        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.of(idempotencyKey));

        assertThatThrownBy(() -> orderService.order(1L, 10L, "order-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ORDER_REQUEST_IN_PROGRESS);
    }

    @Test
    @DisplayName("존재하지 않는 메뉴를 주문하면 예외가 발생한다")
    void orderFailsWhenMenuDoesNotExist() {
        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.empty());
        given(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .willReturn(IdempotencyKey.reserve("order-1", 1L, 10L));
        given(coffeeMenuRepository.findById(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.order(1L, 10L, "order-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    @Test
    @DisplayName("판매 중이 아닌 메뉴를 주문하면 예외가 발생한다")
    void orderFailsWhenMenuIsNotOnSale() {
        CoffeeMenu coffeeMenu = createMenu(10L, 4500L, false);
        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.empty());
        given(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .willReturn(IdempotencyKey.reserve("order-1", 1L, 10L));
        given(coffeeMenuRepository.findById(10L)).willReturn(Optional.of(coffeeMenu));

        assertThatThrownBy(() -> orderService.order(1L, 10L, "order-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MENU_NOT_ON_SALE);
    }

    @Test
    @DisplayName("포인트가 부족하면 주문은 실패하고 outbox 이벤트도 저장되지 않는다")
    void orderFailsWhenPointIsInsufficient() {
        Member member = createMember(1L);
        CoffeeMenu coffeeMenu = createMenu(10L, 4000L, true);
        PointWallet pointWallet = PointWallet.create(member);
        pointWallet.charge(1000L);

        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.empty());
        given(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .willReturn(IdempotencyKey.reserve("order-1", 1L, 10L));
        given(coffeeMenuRepository.findById(10L)).willReturn(Optional.of(coffeeMenu));
        given(pointWalletRepository.findByMemberIdForUpdate(1L)).willReturn(Optional.of(pointWallet));

        assertThatThrownBy(() -> orderService.order(1L, 10L, "order-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);

        verify(coffeeOrderRepository, never()).save(any(CoffeeOrder.class));
        verify(orderOutboxRepository, never()).save(any(OrderOutbox.class));
    }

    @Test
    @DisplayName("지갑이 없고 회원이 존재하면 포인트 부족 예외가 발생한다")
    void orderFailsWhenWalletDoesNotExist() {
        Member member = createMember(1L);
        CoffeeMenu coffeeMenu = createMenu(10L, 4500L, true);

        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.empty());
        given(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .willReturn(IdempotencyKey.reserve("order-1", 1L, 10L));
        given(coffeeMenuRepository.findById(10L)).willReturn(Optional.of(coffeeMenu));
        given(pointWalletRepository.findByMemberIdForUpdate(1L)).willReturn(Optional.empty());
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        assertThatThrownBy(() -> orderService.order(1L, 10L, "order-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_POINT);
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 주문은 실패한다")
    void orderFailsWhenMemberDoesNotExist() {
        CoffeeMenu coffeeMenu = createMenu(10L, 4200L, true);

        given(idempotencyKeyRepository.findByRequestKey("order-1")).willReturn(Optional.empty());
        given(idempotencyKeyRepository.save(any(IdempotencyKey.class)))
                .willReturn(IdempotencyKey.reserve("order-1", 1L, 10L));
        given(coffeeMenuRepository.findById(10L)).willReturn(Optional.of(coffeeMenu));
        given(pointWalletRepository.findByMemberIdForUpdate(1L)).willReturn(Optional.empty());
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.order(1L, 10L, "order-1"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    private Member createMember(Long memberId) {
        Member member = org.mockito.Mockito.mock(Member.class);
        lenient().when(member.getId()).thenReturn(memberId);
        return member;
    }

    private CoffeeMenu createMenu(Long menuId, long price, boolean orderable) {
        CoffeeMenu coffeeMenu = org.mockito.Mockito.mock(CoffeeMenu.class);
        lenient().when(coffeeMenu.getId()).thenReturn(menuId);
        lenient().when(coffeeMenu.getPrice()).thenReturn(price);
        lenient().when(coffeeMenu.isOrderable()).thenReturn(orderable);
        return coffeeMenu;
    }

    private CoffeeOrder createOrder(Long orderId, Member member, CoffeeMenu coffeeMenu, long orderPrice) {
        CoffeeOrder coffeeOrder = org.mockito.Mockito.mock(CoffeeOrder.class);
        lenient().when(coffeeOrder.getId()).thenReturn(orderId);
        lenient().when(coffeeOrder.getMember()).thenReturn(member);
        lenient().when(coffeeOrder.getMenu()).thenReturn(coffeeMenu);
        lenient().when(coffeeOrder.getOrderPrice()).thenReturn(orderPrice);
        return coffeeOrder;
    }
}
