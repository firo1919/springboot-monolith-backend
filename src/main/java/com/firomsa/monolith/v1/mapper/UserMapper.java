package com.firomsa.monolith.v1.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import com.firomsa.monolith.model.User;
import com.firomsa.monolith.v1.dto.RegisterRequestDTO;
import com.firomsa.monolith.v1.dto.UserResponseDTO;
import com.firomsa.monolith.v1.dto.UserUpdateRequestDTO;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "role.name", target = "role")
    @Mapping(target = "createdAt", dateFormat = "dd.MM.yyyy")
    @Mapping(target = "profilePictureUrl", ignore = true)
    UserResponseDTO toDTO(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "confirmationOtps", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "imageKey", ignore = true)
    User toModel(RegisterRequestDTO requestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imageKey", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "confirmationOtps", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateModelFromDTO(@MappingTarget User user, UserUpdateRequestDTO requestDTO);
}
