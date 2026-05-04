package com.suburbscore.user.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();

    public void blacklist(String jti, Instant expiry) {
        evictExpired();
        blacklist.put(jti, expiry);
    }

    public boolean isBlacklisted(String jti) {
        Instant expiry = blacklist.get(jti);
        if (expiry == null) return false;
        if (expiry.isBefore(Instant.now())) {
            blacklist.remove(jti);
            return false;
        }
        return true;
    }

    private void evictExpired() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
