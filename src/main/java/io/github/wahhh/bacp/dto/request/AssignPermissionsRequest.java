package io.github.wahhh.bacp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Replaces all permission bindings for a role.
 */
@Data
@Schema(description = "Assign permissions to a role (replaces existing links)")
public class AssignPermissionsRequest {

    @Schema(description = "Permission IDs to assign; empty clears all permissions for the role")
    private List<Long> permIds;
}
