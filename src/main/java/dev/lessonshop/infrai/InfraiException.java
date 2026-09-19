package dev.lessonshop.infrai;

import java.util.Map;

public final class InfraiException extends RuntimeException {
    private final String code;
    private final int status;
    private final Map<String, Object> detail;

    public InfraiException(String code, Map<String, Object> detail, int status) {
        super("Infrai request rejected: " + code);
        this.code = code;
        this.status = status;
        this.detail = Map.copyOf(detail);
    }

    public String code() { return code; }
    public int status() { return status; }
    public Map<String, Object> detail() { return detail; }
}
