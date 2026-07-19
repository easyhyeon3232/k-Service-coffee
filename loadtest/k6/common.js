/**
 * k6 부하 테스트에서 공통으로 사용하는 환경 변수, 요청 헤더,
 * 포인트 충전/주문 호출 유틸 함수를 모아둔 스크립트다.
 */
import http from 'k6/http';
import { check } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const MEMBER_ID = Number(__ENV.MEMBER_ID || 1);
export const MEMBER_IDS = parseMemberIds(__ENV.MEMBER_IDS);
export const MENU_ID = Number(__ENV.MENU_ID || 1);
export const CHARGE_AMOUNT = Number(__ENV.CHARGE_AMOUNT || 5000);
export const ORDER_TEST_INITIAL_CHARGE = Number(__ENV.ORDER_TEST_INITIAL_CHARGE || 300000000);

export const jsonHeaders = {
    headers: {
        'Content-Type': 'application/json',
    },
};

export function assertSuccess(response, apiName) {
    check(response, {
        [`${apiName} status is 200`]: (res) => res.status === 200,
    });
}

export function uniqueId(prefix) {
    return `${prefix}-${__VU}-${__ITER}-${Date.now()}`;
}

export function parseMemberIds(memberIds) {
    if (!memberIds) {
        return [MEMBER_ID];
    }

    return memberIds
        .split(',')
        .map((value) => Number(value.trim()))
        .filter((value) => Number.isInteger(value) && value > 0);
}

export function getMemberIdForIteration() {
    const index = (__VU + __ITER) % MEMBER_IDS.length;
    return MEMBER_IDS[index];
}

export function getMemberIdForIterationFrom(memberIds) {
    const index = (__VU + __ITER) % memberIds.length;
    return memberIds[index];
}

export function chargePoint(memberId = MEMBER_ID, amount = CHARGE_AMOUNT) {
    return http.post(
        `${BASE_URL}/api/points/charge`,
        JSON.stringify({
            memberId,
            amount,
        }),
        jsonHeaders
    );
}

export function orderCoffee(memberId = MEMBER_ID, menuId = MENU_ID) {
    return http.post(
        `${BASE_URL}/api/orders`,
        JSON.stringify({
            memberId,
            menuId,
            idempotencyKey: uniqueId('load-order'),
        }),
        jsonHeaders
    );
}

export function preChargeMembersForOrderTest() {
    const successfulMemberIds = [];

    for (const memberId of MEMBER_IDS) {
        const response = chargePoint(memberId, ORDER_TEST_INITIAL_CHARGE);
        const success = check(response, {
            [`order test setup charge member ${memberId} status is 200`]: (res) => res.status === 200,
        });

        if (success) {
            successfulMemberIds.push(memberId);
        }
    }

    return successfulMemberIds;
}
