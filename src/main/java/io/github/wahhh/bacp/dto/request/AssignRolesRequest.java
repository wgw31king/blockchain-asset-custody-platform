package io.github.wahhh.bacp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Replaces all role bindings for a user.
 */
@Data
@Schema(description = "Assign roles to a user (replaces existing links)")
public class AssignRolesRequest {

    @Schema(description = "Role IDs to assign; empty clears all roles")
    private List<Long> roleIds;
}
