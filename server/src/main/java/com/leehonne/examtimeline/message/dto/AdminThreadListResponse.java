package com.leehonne.examtimeline.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/** 管理端会话列表（契约 §11.14）：最新会话在前 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminThreadListResponse {

    private List<AdminThreadItemResponse> items;

    private int total;

    private int page;

    private int size;
}
