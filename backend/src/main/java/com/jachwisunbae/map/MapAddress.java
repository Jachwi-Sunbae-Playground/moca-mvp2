package com.jachwisunbae.map;

import java.math.BigDecimal;

public record MapAddress(String roadAddress, String jibunAddress,
                         BigDecimal latitude, BigDecimal longitude) {

    public String address() {
        return roadAddress == null || roadAddress.isBlank() ? jibunAddress : roadAddress;
    }
}
