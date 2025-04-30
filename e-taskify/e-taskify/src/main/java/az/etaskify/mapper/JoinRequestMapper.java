package az.etaskify.mapper;

import az.etaskify.dao.entity.JoinRequestEntity;
import az.etaskify.dao.entity.OrganizationEntity;
import az.etaskify.dto.JoinRequestDto;
import org.springframework.stereotype.Component;


@Component
public class JoinRequestMapper {
    public JoinRequestDto mapToJoinRequestDto(JoinRequestEntity e) {
       if(e == null){
           return null;
       }

        OrganizationEntity organization = e.getOrganization();
        Long orgId = null;
        String orgName = null;

        if (organization != null) {
            orgId = organization.getId();
            orgName = organization.getName();
        }

        return JoinRequestDto
                .builder()
                .id(e.getId())
                .organizationId(orgId)
                .userId(e.getUserId())
                .organizationName(orgName)
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
