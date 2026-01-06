package com.hm.mes_final_260106.service;

import com.hm.mes_final_260106.repository.BomRepository;
import com.hm.mes_final_260106.repository.MaterialRepository;
import com.hm.mes_final_260106.repository.ProductionLogRepository;
import com.hm.mes_final_260106.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionService {
    private final ProductionLogRepository logRepo;
    private final MaterialRepository matRepo;
    private final WorkOrderRepository orderRepo;
    private final BomRepository bomRepo;

    // 자재 입고

    // 작업 지시 생성

    // 설비 작업 할당

    // 생산 실적 보고

    // 수량 업데이트 및 완료 처리


}
