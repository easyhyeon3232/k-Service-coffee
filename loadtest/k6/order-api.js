/**
 * 주문/결제 API의 일반 부하와 급상승 부하를 함께 검증하는 k6 시나리오다.
 * 테스트 시작 전에 회원 포인트를 충분히 충전하고,
 * 매 요청마다 새로운 idempotencyKey를 생성해 실제 주문 요청처럼 테스트한다.
 */
import { sleep } from 'k6';
import {
    assertSuccess,
    getMemberIdForIterationFrom,
    MENU_ID,
    orderCoffee,
    preChargeMembersForOrderTest,
} from './common.js';

export const options = {
    scenarios: {
        // 일반 부하 테스트: 일상적인 혼잡 구간에서 주문 처리 성능과 성공률을 확인한다.
        normal_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '1m', target: 50 },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
        // 한계 테스트: 사용자 수를 단계적으로 늘리며 락 대기, 커넥션 풀, 에러 증가 시점을 찾는다.
        limit_load: {
            executor: 'ramping-vus',
            startTime: '2m20s',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },
                { duration: '30s', target: 100 },
                { duration: '30s', target: 150 },
                { duration: '30s', target: 200 },
                { duration: '20s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
        // 급상승 테스트: 짧은 시간에 주문이 몰릴 때 분산락과 DB 락 구간이 버티는지 본다.
        spike_load: {
            executor: 'ramping-vus',
            startTime: '4m50s',
            startVUs: 10,
            stages: [
                { duration: '20s', target: 200 },
                { duration: '20s', target: 200 },
                { duration: '20s', target: 10 },
            ],
            gracefulRampDown: '10s',
        },
        // 내구성 테스트: 일정 시간 동안 계속 주문을 발생시켜 응답 시간 악화와 자원 누수를 확인한다.
        endurance_load: {
            executor: 'constant-vus',
            startTime: '6m10s',
            vus: 30,
            duration: '10m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.03'],
        http_req_duration: ['p(95)<1500', 'p(99)<2500'],
    },
};

// 주문 부하 테스트 전에 대상 회원 포인트를 충분히 충전한다.
export function setup() {
    const availableMemberIds = preChargeMembersForOrderTest();

    if (availableMemberIds.length === 0) {
        throw new Error('주문 부하 테스트에 사용할 수 있는 회원이 없습니다. MEMBER_IDS와 테스트 데이터를 확인해주세요.');
    }

    return {
        availableMemberIds,
    };
}

export default function (data) {
    const memberId = getMemberIdForIterationFrom(data.availableMemberIds);
    const response = orderCoffee(memberId, MENU_ID);
    assertSuccess(response, 'order');
    sleep(1);
}
