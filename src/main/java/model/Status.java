package model;

public enum Status {
    OK(200, "OK");

    private final int code;
    private final String name;

    Status(int code, String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
