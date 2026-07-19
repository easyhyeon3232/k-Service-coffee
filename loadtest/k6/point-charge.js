/**
 * 포인트 충전 API의 일반 부하 상황을 검증하는 k6 시나리오다.
 * 회원 1명 기준으로 반복 충전 요청을 보내며 응답 시간과 에러율을 확인한다.
 */
import { sleep } from 'k6';
import { assertSuccess, chargePoint } from './common.js';

export const options = {
    scenarios: {
        // 일반 부하 테스트: 평소보다 사용자가 조금 몰리는 상황을 가정한다.
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
        // 한계 테스트: 점진적으로 부하를 올려 어느 시점부터 응답 저하나 에러가 늘어나는지 확인한다.
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
        // 급상승 테스트: 짧은 시간에 요청이 빠르게 몰릴 때 락 경합과 에러율 변화를 본다.
        spike_load: {
            executor: 'ramping-vus',
            startTime: '4m50s',
            startVUs: 10,
            stages: [
                { duration: '15s', target: 120 },
                { duration: '20s', target: 120 },
                { duration: '15s', target: 10 },
            ],
            gracefulRampDown: '10s',
        },
        // 내구성 테스트: 오랜 시간 일정 부하를 유지했을 때 성능 저하나 자원 누수를 확인한다.
        endurance_load: {
            executor: 'constant-vus',
            startTime: '6m00s',
            vus: 30,
            duration: '10m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000', 'p(99)<1500'],
    },
};

export default function () {
    const response = chargePoint();
    assertSuccess(response, 'point charge');
    sleep(1);
}
