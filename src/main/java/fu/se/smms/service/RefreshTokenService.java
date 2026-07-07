package fu.se.smms.service;

import fu.se.smms.dto.UserDetailDTO;
import fu.se.smms.entity.RefreshToken;
import fu.se.smms.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);

    RefreshToken findByToken(String token);

    void revokeByToken(String token);

    void revokeAllByUser(User user);

    void validateAndRevoke(String token);
}
