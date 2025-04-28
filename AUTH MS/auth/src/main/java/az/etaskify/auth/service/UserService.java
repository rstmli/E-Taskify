package az.etaskify.auth.service;

import az.etaskify.auth.dao.repository.UsersRepository;
import az.etaskify.auth.dto.UserDto;
import az.etaskify.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;
    private final UserMapper userMapper;

    public UserDto getByUser(Long id){
        var entity = usersRepository.findById(id).orElseThrow(() -> new RuntimeException("user not found"));
        return userMapper.entityToDto(entity);
    }

    public UserDto getByUsername(String username) {
        return userMapper.entityToDto(usersRepository.findByUsername(username).orElseThrow(() ->
                new RuntimeException("user not found")));
    }


}
