package io.github.wahhh.bacp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.wahhh.bacp.entity.TxRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * {@link TxRecord} persistence.
 */
@Mapper
public interface TxRecordMapper extends BaseMapper<TxRecord> {
}
