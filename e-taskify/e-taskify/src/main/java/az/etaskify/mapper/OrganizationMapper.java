package az.etaskify.mapper;

import az.etaskify.dao.entity.InviteEntity;
import az.etaskify.dao.entity.OrganizationEntity;
import az.etaskify.dto.InviteResponseDto;
import az.etaskify.dto.OrganizationCreateRequest;
import az.etaskify.dto.PublicOrganizationDto;
import lombok.RequiredArgsConstructor;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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

    public InviteResponseDto mapToInviteResponseDto(InviteEntity entity) {
        if (entity == null) return null;
        return InviteResponseDto.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization().getId())
                .organizationName(entity.getOrganization().getName())
                .invitedUserId(entity.getInvitedUserId())
                .inviterUserId(entity.getInviterUserId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<InviteResponseDto> entityToDtoList(List<InviteEntity> entities){
        List<InviteResponseDto> dtos = new ArrayList<>();
        for(InviteEntity e : entities){
            dtos.add(
                    InviteResponseDto.builder()
                            .id(e.getId())
                            .organizationId(e.getOrganization().getId())
                            .organizationName(e.getOrganization().getName())
                            .invitedUserId(e.getInvitedUserId())
                            .inviterUserId(e.getInviterUserId())
                            .status(e.getStatus())
                            .createdAt(e.getCreatedAt())
                            .build()
            );
        }
        return dtos;
    }

    public List<PublicOrganizationDto> mapToPublicOrganizationDtoList(List<OrganizationEntity> entities){
        List<PublicOrganizationDto> dtos = new ArrayList<>();
        for(OrganizationEntity e :entities){
            dtos.add(
                    PublicOrganizationDto.builder()
                            .id(e.getId())
                            .name(e.getName())
                            .build()
            );

        }
        return dtos;
    }


}
