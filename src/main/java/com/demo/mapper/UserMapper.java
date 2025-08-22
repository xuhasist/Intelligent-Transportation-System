package com.demo.mapper;

import com.demo.dto.UserDto;
import com.demo.model.its.User;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(User user);
}
