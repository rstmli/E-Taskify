package az.etaskify.mapper;

import az.etaskify.dao.entity.OrganizationEntity;
import az.etaskify.dto.OrganizationCreateRequest;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "SPRING",unmappedTargetPolicy = ReportingPolicy.IGNORE,unmappedSourcePolicy = ReportingPolicy.IGNORE)
@RequiredArgsConstructor
@Component
public class OrganizationMapper {

    public OrganizationEntity orgDtoToEntity(OrganizationCreateRequest dto, Long ownerId) {
        return OrganizationEntity.builder()
                .name(dto.name())
                .isPrivate(dto.isPrivate())
                .ownerId(ownerId)
                .build();
    }
}
