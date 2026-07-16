package com.example.coffee.domin.point.controller;

import com.example.coffee.common.response.CommonResponse;
import com.example.coffee.domin.point.dto.PointChargeRequest;
import com.example.coffee.domin.point.dto.PointChargeResponse;
import com.example.coffee.domin.point.service.PointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointController {

    private final PointService pointService;

    // 회원 포인트를 충전한다.
    @PostMapping("/charge")
    public ResponseEntity<CommonResponse<PointChargeResponse>> charge(
            @Valid @RequestBody PointChargeRequest request
    ) {
        PointChargeResponse response = pointService.charge(request.memberId(), request.amount());
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
