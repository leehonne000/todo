package com.leehonne.examtimeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehonne.examtimeline.entity.EntryTag;

/**
 * 记录-标签关联 Mapper（t_entry_tag，复合主键）—— 只用 Wrapper 条件操作。
 */
public interface EntryTagMapper extends BaseMapper<EntryTag> {
}
