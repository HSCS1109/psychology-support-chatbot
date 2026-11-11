package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.SysFileInfo;

/**
 * 文件信息Mapper
 */
@Mapper
public interface SysFileInfoMapper extends BaseMapper<SysFileInfo> {
}
