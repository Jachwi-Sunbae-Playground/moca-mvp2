package com.jachwisunbae.map;

import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.common.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/maps")
@Tag(name = "Maps", description = "주소 검색과 주변 시설 분석 API")
@SecurityRequirement(name = "bearerAuth")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/geocode")
    @Operation(summary = "주소 검색", description = "주소 검색어를 WGS84 좌표 후보로 변환합니다.")
    public ApiResponse<List<MapAddress>> geocode(@RequestParam String query) {
        return ApiResponse.of(mapService.geocode(query));
    }

    @GetMapping("/reverse-geocode")
    @Operation(
            summary = "역지오코딩",
            description = "WGS84 좌표의 도로명·지번 주소를 조회합니다."
    )
    public ApiResponse<MapAddress> reverseGeocode(@RequestParam BigDecimal latitude,
                                                  @RequestParam BigDecimal longitude) {
        return ApiResponse.of(mapService.reverseGeocode(latitude, longitude));
    }

    @GetMapping("/nearby")
    @Operation(
            summary = "주변 시설 조회",
            description = "반경과 카테고리에 맞는 실제 장소 좌표와 반환 장소 기준 집계를 조회합니다. "
                    + "Kakao 장소를 기본으로 사용하고 설정 시 TAGO의 중심 500m 실제 버스정류소를 합칩니다."
    )
    public ApiResponse<NearbyResponse> nearby(@RequestParam BigDecimal latitude,
                                              @RequestParam BigDecimal longitude,
                                              @RequestParam int radius,
                                              @RequestParam(required = false) String categories) {
        return ApiResponse.of(mapService.nearby(latitude, longitude, radius, parseCategories(categories)));
    }

    private Set<MapCategory> parseCategories(String value) {
        if (value == null || value.isBlank()) {
            return EnumSet.allOf(MapCategory.class);
        }
        EnumSet<MapCategory> result = EnumSet.noneOf(MapCategory.class);
        try {
            for (String category : value.split(",")) {
                result.add(MapCategory.valueOf(category.trim().toUpperCase()));
            }
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(DomainErrorCode.MAP_QUERY_INVALID,
                    "지원하지 않는 지도 카테고리입니다.");
        }
        return result;
    }
}
