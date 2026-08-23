package com.jachwisunbae.property.service;

import com.jachwisunbae.property.controller.dto.response.PropertyListItemResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;

@Service
public class PropertyCsvService {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private final PropertyService propertyService;

    public PropertyCsvService(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    public byte[] export(Long memberId) {
        StringBuilder csv = new StringBuilder("이름,주소,보증금(원),월세(원),사진 수,체크 완료,체크 전체,진행률(%),최근 활동\r\n");
        for (PropertyListItemResponse item : propertyService.findList(memberId).items()) {
            csv.append(escape(item.name())).append(',')
                    .append(escape(item.address())).append(',')
                    .append(item.depositAmount()).append(',')
                    .append(item.monthlyRentAmount()).append(',')
                    .append(item.photoCount()).append(',')
                    .append(item.overallProgress().completedCount()).append(',')
                    .append(item.overallProgress().totalCount()).append(',')
                    .append(item.overallProgress().progressRate()).append(',')
                    .append(escape(item.lastActivityAt().toString())).append("\r\n");
        }
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[UTF8_BOM.length + content.length];
        System.arraycopy(UTF8_BOM, 0, result, 0, UTF8_BOM.length);
        System.arraycopy(content, 0, result, UTF8_BOM.length, content.length);
        return result;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r", " ").replace("\n", " ");
        return '"' + normalized.replace("\"", "\"\"") + '"';
    }
}
