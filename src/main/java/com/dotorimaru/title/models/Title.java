package com.dotorimaru.title.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 칭호 데이터 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Title {
    
    /**
     * 플레이어 UUID
     */
    private UUID playerUUID;
    
    /**
     * 칭호 이름 (색상 코드 포함)
     * 예: &c&l전설의 용사, #FF5733전설
     */
    private String titleName;
    
    /**
     * 획득 시간 (밀리초)
     */
    private long obtainedAt;
    
    /**
     * 착용 여부
     */
    private boolean equipped;
}
