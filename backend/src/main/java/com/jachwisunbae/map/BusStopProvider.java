package com.jachwisunbae.map;

import java.math.BigDecimal;
import java.util.List;

public interface BusStopProvider {

    List<NearbyPlace> nearby(BigDecimal latitude, BigDecimal longitude, int radius);
}
