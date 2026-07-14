package com.xiuhui.campus.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiuhui.campus.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
