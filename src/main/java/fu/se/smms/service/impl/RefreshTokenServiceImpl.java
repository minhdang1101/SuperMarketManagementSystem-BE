package fu.se.smms.service.impl;

import fu.se.smms.entity.RefreshToken;
import fu.se.smms.entity.User;
import fu.se.smms.repository.RefreshTokenRepository;
import fu.se.smms.service.RefreshTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days default
    private long refreshExpirationMs;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(refreshExpirationMs);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .filter(rt -> !rt.isExpired())
                .orElse(null);
    }

    @Override
    @Transactional
    public void revokeByToken(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.debug("Revoked refresh token for user: {}", rt.getUser().getUsername());
        });
    }

    @Override
    @Transactional
    public void revokeAllByUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
        log.debug("Revoked all refresh tokens for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void validateAndRevoke(String token) {
        Optional<RefreshToken> opt = refreshTokenRepository.findByTokenAndRevokedFalse(token);
        if (opt.isPresent()) {
            RefreshToken rt = opt.get();
            if (rt.isExpired()) {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            }
        }
    }
}
