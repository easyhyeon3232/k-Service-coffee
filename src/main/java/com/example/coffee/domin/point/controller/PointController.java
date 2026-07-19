package com.example.coffee.domin.point.controller;

import com.example.coffee.common.response.CommonResponse;
import com.example.coffee.domin.point.dto.PointChargeRequest;
import com.example.coffee.domin.point.dto.PointChargeResponse;
import com.example.coffee.domin.point.service.PointFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 포인트 충전 요청을 처리하는 API 컨트롤러다.
 */
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointFacade pointFacade;

    // 회원 포인트를 충전한다.
    @PostMapping("/charge")
    public ResponseEntity<CommonResponse<PointChargeResponse>> charge(
            @Valid @RequestBody PointChargeRequest request
    ) {
        PointChargeResponse response = pointFacade.charge(request.memberId(), request.amount());
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
