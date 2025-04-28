package az.etaskify.auth.mapper;
import az.etaskify.auth.dao.entity.UsersEntity;
import az.etaskify.auth.dto.UserDto;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Component
@Mapper(componentModel = "SPRING")
public interface UserMapper {
    UserDto entityToDto(UsersEntity entity);
}
