package model;

public enum Version {
    HTTP_1_0("HTTP/1.1");

    private final String value;

    Version(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Version fromValue(String value) {
        for (Version v : values()) {
            if (v.value.equalsIgnoreCase(value)) {
                return v;
            }
        }

        throw new IllegalArgumentException("Unsupported HTTP version: " + value);
    }
}
