package rtmp.base;

public enum PublishType {
    LIVE,
    APPEND,
    RECORD;

    public String asString() {
        return this.name().toLowerCase();
    }

    public static PublishType parse(final String raw) {
        return PublishType.valueOf(raw.toUpperCase());
    }

    public static PublishType getType(final String raw) {
        switch (raw.toUpperCase()) {
            case "APPEND" :
                return APPEND;
            case "RECORD" :
                return RECORD;
            case "LIVE" :
            default :
                return LIVE;
        }
    }
}
