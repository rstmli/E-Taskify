package az.etaskify.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicOrganizationDto {
    private Long id;
    private String name;
}
