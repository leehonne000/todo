package com.leehonne.examtimeline.learn.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** 开始计时响应（契约 §5.2） */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StartSessionResponse {

    private Long id;

    /** 计时开始时刻（横屏进入计时那一刻） */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;
}
