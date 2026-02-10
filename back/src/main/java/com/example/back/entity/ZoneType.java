// ZoneType.java
package com.example.back.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public enum ZoneType {
    TUNISIAN_ZONE,
    CUSTOM_ZONE;

    // Static list of Tunisian governorates for validation
    public static final List<String> TUNISIAN_ZONE_CODES = Arrays.asList(
            "TN-11", "TN-12", "TN-13", "TN-14",
            "TN-21", "TN-22", "TN-23",
            "TN-31", "TN-32", "TN-33", "TN-34",
            "TN-41", "TN-42", "TN-43",
            "TN-51", "TN-52", "TN-53",
            "TN-61", "TN-71", "TN-72", "TN-73",
            "TN-81", "TN-82", "TN-83"
    );

    public static boolean isTunisianZoneCode(String code) {
        return TUNISIAN_ZONE_CODES.contains(code);
    }
}