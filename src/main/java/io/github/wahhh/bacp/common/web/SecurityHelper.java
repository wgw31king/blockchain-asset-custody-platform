package io.github.wahhh.bacp.common.web;

import io.github.wahhh.bacp.common.constant.SecurityConstants;
import io.github.wahhh.bacp.common.exception.AuthException;
import io.github.wahhh.bacp.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Static accessors for the current {@link LoginUser}.
 */
public final class SecurityHelper {

    private SecurityHelper() {
    }

    /**
     * Returns the authenticated {@link LoginUser} when present.
     *
     * @return optional principal
     */
    public static Optional<LoginUser> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return Optional.of(loginUser);
        }
        return Optional.empty();
    }

    /**
     * Requires a logged-in user id or throws {@link AuthException}.
     *
     * @return user id
     */
    public static Long currentUserIdOrThrow() {
        return currentUser().map(LoginUser::getUserId)
                .orElseThrow(() -> new AuthException(ResultCode.UNAUTHORIZED.getMessage()));
    }

    /**
     * Checks whether current user holds permission or wildcard.
     *
     * @param permission permission code
     * @return true if granted
     */
    public static boolean hasPermission(String permission) {
        return currentUser()
                .map(u -> u.getPermissions().contains(SecurityConstants.PERM_ALL)
                        || u.getPermissions().contains(permission))
                .orElse(false);
    }
}
