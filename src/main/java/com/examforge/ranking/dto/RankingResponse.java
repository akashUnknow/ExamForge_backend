package com.examforge.ranking.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Public leaderboard entry. Deliberately limited to what a leaderboard
 * needs - name, score, accuracy - and nothing else about the user
 * (no email, no mobile), per the spec's "do not expose unnecessary
 * personal information" instruction for the ranking endpoint.
 */
public record RankingResponse(
        int rank,
        UUID userId,
        String userName,
        BigDecimal score,
        BigDecimal accuracy
) {
}
