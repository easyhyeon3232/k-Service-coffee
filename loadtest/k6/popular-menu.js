/**
 * 인기 메뉴 조회 API의 읽기 부하를 검증하는 k6 시나리오다.
 * 캐시 적중 이후 응답 시간이 안정적으로 유지되는지 확인하는 용도다.
 */
import http from 'k6/http';
import { sleep } from 'k6';
import { BASE_URL, assertSuccess } from './common.js';

export const options = {
    scenarios: {
        // 일반 부하 테스트: 평소보다 사용자가 몰린 상태에서 조회 응답 시간을 본다.
        normal_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 30 },
                { duration: '1m', target: 100 },
                { duration: '30s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
        // 한계 테스트: 읽기 요청을 점진적으로 증가시켜 캐시와 Redis ZSET 구간의 한계를 확인한다.
        limit_load: {
            executor: 'ramping-vus',
            startTime: '2m20s',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 100 },
                { duration: '30s', target: 200 },
                { duration: '30s', target: 300 },
                { duration: '30s', target: 400 },
                { duration: '20s', target: 0 },
            ],
            gracefulRampDown: '10s',
        },
        // 급상승 테스트: 짧은 시간에 인기 메뉴 조회가 몰릴 때 캐시가 안정적으로 응답하는지 본다.
        spike_load: {
            executor: 'ramping-vus',
            startTime: '4m50s',
            startVUs: 20,
            stages: [
                { duration: '15s', target: 250 },
                { duration: '20s', target: 250 },
                { duration: '15s', target: 20 },
            ],
            gracefulRampDown: '10s',
        },
        // 내구성 테스트: 일정 시간 조회 부하를 유지해 캐시 응답 시간 흔들림이 없는지 확인한다.
        endurance_load: {
            executor: 'constant-vus',
            startTime: '6m00s',
            vus: 50,
            duration: '10m',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
    },
};

export default function () {
    const response = http.get(`${BASE_URL}/api/menus/popular`);
    assertSuccess(response, 'popular menu');
    sleep(1);
}
