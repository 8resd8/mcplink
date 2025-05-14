package kr.co.mcplink.domain.mcpserver.entity;

public enum SecurityRank {
    UNRATED,
    LOW,
    MODERATE,
    HIGH,
    CRITICAL;

    public static SecurityRank fromString(String severityString) {
        if (severityString == null) {
            return UNRATED;
        }

        try {
            return SecurityRank.valueOf(severityString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SecurityRank.UNRATED;
        }
    }
}