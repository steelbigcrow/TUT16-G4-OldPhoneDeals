package com.oldphonedeals.enums;

public enum PhoneBrand {
    SAMSUNG("Samsung"),
    APPLE("Apple"),
    HTC("HTC"),
    HUAWEI("Huawei"),
    NOKIA("Nokia"),
    LG("LG"),
    MOTOROLA("Motorola"),
    SONY("Sony"),
    BLACKBERRY("BlackBerry");

    private final String displayName;

    PhoneBrand(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}