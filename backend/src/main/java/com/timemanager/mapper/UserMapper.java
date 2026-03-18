package com.timemanager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timemanager.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // custom queries can be declared here if needed
}
