package com.aegisterra.platform.application.contracts;

import java.math.BigDecimal;

public record GeometryValidateResponse(
    boolean valid,
    String reason,
    BigDecimal areaHa
) {}
