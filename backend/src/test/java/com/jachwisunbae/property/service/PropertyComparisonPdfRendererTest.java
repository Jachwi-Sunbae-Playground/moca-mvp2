package com.jachwisunbae.property.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.checklist.type.CheckStatus;
import com.jachwisunbae.property.controller.dto.response.PropertyChecklistStageResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyProgress;
import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.query.PropertyChecklistApplicationQuery;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemQuery;
import com.jachwisunbae.property.repository.query.PropertyMemoItemQuery;
import com.jachwisunbae.property.repository.query.PropertyMemoQuery;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class PropertyComparisonPdfRendererTest {

    @Test
    void 선택한_매물의_모든_기록을_한글_PDF로_생성한다() throws Exception {
        var renderer = new PropertyComparisonPdfRenderer();
        byte[] pdf = renderer.render(List.of(record(10L, "신림역 원룸", true),
                record(11L, "망원동 투룸", false)));

        assertThat(pdf).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
        Path sample = Path.of("build", "reports", "pdf", "moca-property-comparison-sample.pdf");
        Files.createDirectories(sample.getParent());
        Files.write(sample, pdf);

        try (var document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(3);
            assertThat(document.getPage(0).getMediaBox().getWidth()).isGreaterThan(PDRectangle.A4.getWidth());
            assertThat(new PDFTextStripper().getText(document))
                    .contains("매물 비교 기록")
                    .contains("신림역 원룸")
                    .contains("망원동 투룸")
                    .contains("부동산 앱에서 발견")
                    .contains("입주 가능일")
                    .contains("창문 방향 재확인")
                    .contains("관리비에 수도 포함")
                    .contains("주차 가능 여부를 확인했나요?");
        }
    }

    private PropertyComparisonRecord record(final Long propertyId, final String name,
                                            final boolean withPhoto) throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 8, 25, 11, 30);
        Property property = Property.reconstruct(propertyId, 1L, name, 10_000_000L, 550_000L,
                "부동산 앱에서 발견", "서울 관악구 신림로 12", "서울 관악구 신림동 10-1",
                new BigDecimal("37.484"), new BigDecimal("126.929"), now.minusDays(3), now.minusDays(1), now);
        List<PropertyComparisonRecord.Photo> photos = withPhoto
                ? List.of(new PropertyComparisonRecord.Photo(81L, samplePhoto(), "image/png", true))
                : List.of();
        PropertyMemoQuery memo = new PropertyMemoQuery(propertyId, "창문 방향 재확인", List.of(
                new PropertyMemoItemQuery(101L, 1L, "입주 가능일", 1, "9월 1일"),
                new PropertyMemoItemQuery(102L, 2L, "관리비", 2, "관리비에 수도 포함")));
        PropertyProgress onlineProgress = new PropertyProgress(2, 1, 1, 0, 1, 50);
        PropertyChecklistApplicationQuery online = new PropertyChecklistApplicationQuery(
                71L, propertyId, 7L, "온라인 확인 기본", CheckStage.ONLINE_PHONE, List.of(
                new PropertyChecklistItemQuery(711L, 1L, "주차 가능 여부를 확인했나요?", 1,
                        CheckStatus.GOOD, "주차 1대 가능"),
                new PropertyChecklistItemQuery(712L, 2L, "입주 가능일은 언제인가요?", 2,
                        CheckStatus.UNCONFIRMED, "")));
        PropertyProgress empty = new PropertyProgress(0, 0, 0, 0, 0, 0);
        return new PropertyComparisonRecord(property, photos, memo, List.of(
                new PropertyComparisonRecord.Stage(new PropertyChecklistStageResponse(CheckStage.ONLINE_PHONE,
                        true, 71L, "온라인 확인 기본", 7L, onlineProgress), online),
                new PropertyComparisonRecord.Stage(new PropertyChecklistStageResponse(CheckStage.ON_SITE,
                        false, null, null, null, empty), null),
                new PropertyComparisonRecord.Stage(new PropertyChecklistStageResponse(CheckStage.PRE_CONTRACT,
                        false, null, null, null, empty), null)));
    }

    private byte[] samplePhoto() throws Exception {
        BufferedImage image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(224, 214, 188));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.setColor(new Color(91, 121, 49));
        graphics.fillRoundRect(90, 70, 460, 220, 28, 28);
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
