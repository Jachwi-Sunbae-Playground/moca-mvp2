package com.jachwisunbae.property.service;

import com.jachwisunbae.checklist.type.CheckStage;
import com.jachwisunbae.checklist.type.CheckStatus;
import com.jachwisunbae.common.exception.BusinessException;
import com.jachwisunbae.common.exception.DomainErrorCode;
import com.jachwisunbae.property.controller.dto.response.PropertyChecklistStageResponse;
import com.jachwisunbae.property.controller.dto.response.PropertyProgress;
import com.jachwisunbae.property.entity.Property;
import com.jachwisunbae.property.repository.query.PropertyChecklistItemQuery;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.springframework.stereotype.Component;

@Component
public class PropertyComparisonPdfRenderer {
    private static final String REGULAR_FONT = "/fonts/NanumGothic-Regular.ttf";
    private static final String BOLD_FONT = "/fonts/NanumGothic-Bold.ttf";
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final NumberFormat WON = NumberFormat.getIntegerInstance(Locale.KOREA);

    public byte[] render(final List<PropertyComparisonRecord> records) {
        try (PDDocument document = new PDDocument();
             InputStream regularInput = requiredResource(REGULAR_FONT);
             InputStream boldInput = requiredResource(BOLD_FONT);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont regular = PDType0Font.load(document, regularInput);
            PDFont bold = PDType0Font.load(document, boldInput);
            try (DocumentWriter writer = new DocumentWriter(document, regular, bold)) {
                drawComparisonOverview(writer, records);
                for (int index = 0; index < records.size(); index++) {
                    drawPropertyDetail(writer, records.get(index), index + 1, records.size());
                }
            }
            addPageFooters(document, regular);
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(DomainErrorCode.PROPERTY_COMPARISON_EXPORT_FAILED,
                    "매물 비교 PDF를 생성하지 못했습니다.", exception);
        }
    }

    private void drawComparisonOverview(final DocumentWriter writer,
                                        final List<PropertyComparisonRecord> records) throws IOException {
        PDRectangle landscape = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        writer.newPage(landscape);
        writer.heading("매물 비교 기록", 22);
        writer.text("선택한 매물에 저장된 사실을 나란히 정리했습니다. 점수, 추천, 자동 판단은 포함하지 않습니다.",
                9, DocumentWriter.INK_SOFT, 15);
        writer.spacer(8);

        List<String> names = records.stream().map(record -> record.property().getName()).toList();
        writer.tableRow("구분", names, true);
        writer.tableRow("주소", records.stream()
                .map(record -> display(record.property().getAddress())).toList(), false);
        writer.tableRow("보증금", records.stream()
                .map(record -> money(record.property().getDepositAmount())).toList(), false);
        writer.tableRow("월세", records.stream()
                .map(record -> money(record.property().getMonthlyRentAmount())).toList(), false);
        writer.tableRow("발견 경로", records.stream()
                .map(record -> abbreviate(display(record.property().getDiscoverySource()), 70)).toList(), false);
        writer.tableRow("사진", records.stream()
                .map(record -> record.photos().size() + "장").toList(), false);
        for (CheckStage stage : CheckStage.values()) {
            writer.tableRow(stageLabel(stage), records.stream()
                    .map(record -> stageSummary(record, stage)).toList(), false);
        }
        writer.tableRow("최근 활동", records.stream()
                .map(record -> format(record.property().getLastActivityAt())).toList(), false);
    }

    private void drawPropertyDetail(final DocumentWriter writer,
                                    final PropertyComparisonRecord record,
                                    final int index,
                                    final int total) throws IOException {
        writer.newPage(PDRectangle.A4);
        Property property = record.property();
        writer.eyebrow("매물 " + index + " / " + total);
        writer.heading(property.getName(), 21);
        writer.text("아래 내용은 이 매물에 저장된 기록 전체입니다.", 9, DocumentWriter.INK_SOFT, 14);

        writer.section("기본 정보");
        writer.keyValue("보증금", money(property.getDepositAmount()));
        writer.keyValue("월세", money(property.getMonthlyRentAmount()));
        writer.keyValue("발견 경로", display(property.getDiscoverySource()));
        writer.keyValue("표시 주소", display(property.getAddress()));
        writer.keyValue("도로명 주소", display(property.getRoadAddress()));
        writer.keyValue("지번 주소", display(property.getJibunAddress()));
        writer.keyValue("좌표", coordinates(property.getLatitude(), property.getLongitude()));
        writer.keyValue("등록 시각", format(property.getCreatedAt()));
        writer.keyValue("수정 시각", format(property.getUpdatedAt()));
        writer.keyValue("최근 활동", format(property.getLastActivityAt()));

        writer.keep(record.photos().isEmpty() ? 80 : 275);
        writer.section("사진 " + record.photos().size() + "장");
        if (record.photos().isEmpty()) {
            writer.empty("저장된 사진이 없습니다.");
        } else {
            for (int photoIndex = 0; photoIndex < record.photos().size(); photoIndex++) {
                var photo = record.photos().get(photoIndex);
                writer.photo(photo.bytes(), "사진 " + (photoIndex + 1)
                        + (photo.representative() ? " · 대표 사진" : ""));
            }
        }

        writer.keep(100);
        writer.section("메모");
        if (record.memo().items().isEmpty()) {
            writer.empty("저장된 구조화 메모 항목이 없습니다.");
        } else {
            for (var item : record.memo().items()) {
                writer.note(item.label(), display(item.content()));
            }
        }
        writer.note("자유 메모", display(record.memo().freeMemo()));

        writer.keep(110);
        writer.section("3단계 체크리스트");
        for (int stageIndex = 0; stageIndex < record.stages().size(); stageIndex++) {
            drawStage(writer, record.stages().get(stageIndex), stageIndex + 1);
        }
    }

    private void drawStage(final DocumentWriter writer,
                           final PropertyComparisonRecord.Stage stage,
                           final int stageNumber) throws IOException {
        PropertyChecklistStageResponse summary = stage.summary();
        writer.keep(summary.applied() ? 105 : 72);
        writer.subsection(stageNumber + "단계 · " + stageLabel(summary.stage()));
        if (!summary.applied() || stage.application() == null) {
            writer.empty("이 단계는 이 매물에 적용하지 않았습니다.");
            return;
        }
        writer.keyValue("체크리스트", display(summary.checklistName()));
        writer.keyValue("진행", progressText(summary.progress()));
        for (PropertyChecklistItemQuery item : stage.application().items()) {
            writer.checkItem(statusLabel(item.status()), item.question(), display(item.memo()));
        }
    }

    private void addPageFooters(final PDDocument document, final PDFont font) throws IOException {
        int total = document.getNumberOfPages();
        for (int index = 0; index < total; index++) {
            PDPage page = document.getPage(index);
            try (PDPageContentStream stream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                String label = "Moca 매물 비교 기록  ·  " + (index + 1) + " / " + total;
                float width = font.getStringWidth(label) / 1000f * 7.5f;
                stream.beginText();
                stream.setFont(font, 7.5f);
                stream.setNonStrokingColor(DocumentWriter.MUTED);
                stream.newLineAtOffset(page.getMediaBox().getWidth() - 42 - width, 22);
                stream.showText(label);
                stream.endText();
            }
        }
    }

    private InputStream requiredResource(final String path) throws IOException {
        InputStream input = getClass().getResourceAsStream(path);
        if (input == null) {
            throw new IOException("PDF 글꼴 리소스를 찾을 수 없습니다: " + path);
        }
        return input;
    }

    private static String stageSummary(final PropertyComparisonRecord record, final CheckStage stage) {
        return record.stages().stream()
                .filter(candidate -> candidate.summary().stage() == stage)
                .findFirst()
                .map(candidate -> candidate.summary().applied()
                        ? progressText(candidate.summary().progress())
                        : "미적용")
                .orElse("미적용");
    }

    private static String progressText(final PropertyProgress progress) {
        return progress.completedCount() + "/" + progress.totalCount()
                + " · 괜찮음 " + progress.goodCount()
                + " · 주의 " + progress.cautionCount()
                + " · 미확인 " + progress.unconfirmedCount();
    }

    private static String stageLabel(final CheckStage stage) {
        return switch (stage) {
            case ONLINE_PHONE -> "온라인·전화";
            case ON_SITE -> "집에서 확인";
            case PRE_CONTRACT -> "계약 전";
        };
    }

    private static String statusLabel(final CheckStatus status) {
        return switch (status) {
            case GOOD -> "괜찮음";
            case CAUTION -> "주의";
            case UNCONFIRMED -> "미확인";
        };
    }

    private static String money(final Long amount) {
        return WON.format(amount == null ? 0 : amount) + "원";
    }

    private static String coordinates(final BigDecimal latitude, final BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            return "입력 없음";
        }
        return latitude.toPlainString() + ", " + longitude.toPlainString();
    }

    private static String format(final LocalDateTime value) {
        return value == null ? "입력 없음" : DATE_TIME.format(value);
    }

    private static String display(final String value) {
        return value == null || value.isBlank() ? "입력 없음" : value;
    }

    private static String abbreviate(final String value, final int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private static final class DocumentWriter implements AutoCloseable {
        private static final java.awt.Color PRIMARY = new java.awt.Color(91, 121, 49);
        private static final java.awt.Color PRIMARY_LIGHT = new java.awt.Color(238, 242, 230);
        private static final java.awt.Color INK = new java.awt.Color(43, 50, 35);
        private static final java.awt.Color INK_SOFT = new java.awt.Color(91, 99, 79);
        private static final java.awt.Color MUTED = new java.awt.Color(124, 130, 113);
        private static final java.awt.Color BORDER = new java.awt.Color(218, 211, 188);
        private static final java.awt.Color SURFACE = new java.awt.Color(250, 248, 241);
        private static final float MARGIN = 42;
        private static final float FOOTER_TOP = 35;

        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private PDPage page;
        private PDPageContentStream stream;
        private PDRectangle pageSize;
        private float y;

        private DocumentWriter(final PDDocument document, final PDFont regular, final PDFont bold) {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
        }

        private void newPage(final PDRectangle size) throws IOException {
            closeStream();
            pageSize = size;
            page = new PDPage(size);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = size.getHeight() - MARGIN;
        }

        private void heading(final String value, final float size) throws IOException {
            ensure(size + 12);
            drawText(safe(value, bold), MARGIN, y - size, bold, size, INK);
            y -= size + 11;
        }

        private void eyebrow(final String value) throws IOException {
            ensure(18);
            drawText(safe(value, bold), MARGIN, y - 9, bold, 9, PRIMARY);
            y -= 17;
        }

        private void section(final String value) throws IOException {
            ensure(38);
            y -= 16;
            stream.setStrokingColor(BORDER);
            stream.setLineWidth(0.7f);
            stream.moveTo(MARGIN, y);
            stream.lineTo(pageSize.getWidth() - MARGIN, y);
            stream.stroke();
            y -= 22;
            drawText(safe(value, bold), MARGIN, y, bold, 14, INK);
            y -= 13;
        }

        private void subsection(final String value) throws IOException {
            ensure(34);
            y -= 9;
            drawText(safe(value, bold), MARGIN, y - 12, bold, 12, PRIMARY);
            y -= 28;
        }

        private void text(final String value, final float size, final java.awt.Color color,
                          final float lineHeight) throws IOException {
            List<String> lines = wrap(safe(value, regular), regular, size,
                    pageSize.getWidth() - MARGIN * 2);
            ensure(lines.size() * lineHeight);
            for (String line : lines) {
                drawText(line, MARGIN, y - size, regular, size, color);
                y -= lineHeight;
            }
        }

        private void keyValue(final String label, final String value) throws IOException {
            float labelWidth = 82;
            float valueWidth = pageSize.getWidth() - MARGIN * 2 - labelWidth;
            List<String> lines = wrap(safe(value, regular), regular, 9, valueWidth);
            float height = Math.max(22, lines.size() * 13 + 6);
            ensure(height);
            drawText(safe(label, bold), MARGIN, y - 10, bold, 8.5f, INK_SOFT);
            float lineY = y - 10;
            for (String line : lines) {
                drawText(line, MARGIN + labelWidth, lineY, regular, 9, INK);
                lineY -= 13;
            }
            y -= height;
        }

        private void empty(final String value) throws IOException {
            ensure(30);
            fillRect(MARGIN, y - 27, pageSize.getWidth() - MARGIN * 2, 27, SURFACE);
            drawText(safe(value, regular), MARGIN + 10, y - 18, regular, 8.5f, MUTED);
            y -= 35;
        }

        private void note(final String label, final String value) throws IOException {
            float width = pageSize.getWidth() - MARGIN * 2 - 20;
            List<String> lines = wrap(safe(value, regular), regular, 9, width);
            float height = 31 + lines.size() * 13;
            ensure(height + 7);
            fillRect(MARGIN, y - height, pageSize.getWidth() - MARGIN * 2, height, SURFACE);
            drawText(safe(label, bold), MARGIN + 10, y - 17, bold, 8.5f, PRIMARY);
            float lineY = y - 34;
            for (String line : lines) {
                drawText(line, MARGIN + 10, lineY, regular, 9, INK);
                lineY -= 13;
            }
            y -= height + 7;
        }

        private void checkItem(final String status, final String question, final String memo) throws IOException {
            float width = pageSize.getWidth() - MARGIN * 2 - 20;
            List<String> questionLines = wrap(safe(question, bold), bold, 9.5f, width - 55);
            List<String> memoLines = "입력 없음".equals(memo) ? List.of()
                    : wrap(safe("메모: " + memo, regular), regular, 8.3f, width);
            float height = 24 + questionLines.size() * 13 + memoLines.size() * 12;
            ensure(height + 5);
            stream.setStrokingColor(BORDER);
            stream.setLineWidth(0.5f);
            stream.addRect(MARGIN, y - height, pageSize.getWidth() - MARGIN * 2, height);
            stream.stroke();
            drawText(safe(status, bold), MARGIN + 10, y - 17, bold, 8, statusColor(status));
            float questionY = y - 17;
            for (String line : questionLines) {
                drawText(line, MARGIN + 65, questionY, bold, 9.5f, INK);
                questionY -= 13;
            }
            float memoY = y - 19 - questionLines.size() * 13;
            for (String line : memoLines) {
                drawText(line, MARGIN + 10, memoY, regular, 8.3f, INK_SOFT);
                memoY -= 12;
            }
            y -= height + 5;
        }

        private void photo(final byte[] bytes, final String label) throws IOException {
            float boxHeight = 230;
            ensure(boxHeight + 24);
            BufferedImage image;
            try {
                image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            } catch (IOException exception) {
                image = null;
            }
            if (image == null) {
                empty("사진을 PDF에 표시하지 못했습니다.");
                return;
            }
            PDImageXObject pdfImage = JPEGFactory.createFromImage(document, image, 0.78f, 130);
            float maxWidth = pageSize.getWidth() - MARGIN * 2;
            float maxHeight = boxHeight;
            float scale = Math.min(maxWidth / pdfImage.getWidth(), maxHeight / pdfImage.getHeight());
            float width = pdfImage.getWidth() * scale;
            float height = pdfImage.getHeight() * scale;
            float x = MARGIN + (maxWidth - width) / 2;
            fillRect(MARGIN, y - maxHeight, maxWidth, maxHeight, SURFACE);
            stream.drawImage(pdfImage, x, y - (maxHeight + height) / 2, width, height);
            y -= maxHeight + 14;
            drawText(safe(label, bold), MARGIN, y, bold, 8.5f, INK_SOFT);
            y -= 16;
        }

        private void tableRow(final String label, final List<String> values, final boolean heading) throws IOException {
            float tableWidth = pageSize.getWidth() - MARGIN * 2;
            float labelWidth = 72;
            float cellWidth = (tableWidth - labelWidth) / values.size();
            float fontSize = heading ? 8.5f : 7.6f;
            PDFont valueFont = heading ? bold : regular;
            List<List<String>> wrapped = values.stream()
                    .map(value -> wrap(safe(value, valueFont), valueFont, fontSize, cellWidth - 10))
                    .toList();
            int maxLines = wrapped.stream().mapToInt(List::size).max().orElse(1);
            float height = Math.max(29, maxLines * 11 + 12);
            ensure(height);
            if (heading) {
                fillRect(MARGIN, y - height, tableWidth, height, PRIMARY_LIGHT);
            }
            strokeRect(MARGIN, y - height, tableWidth, height, BORDER);
            drawText(safe(label, bold), MARGIN + 6, y - 17, bold, 8, heading ? PRIMARY : INK_SOFT);
            float x = MARGIN + labelWidth;
            for (List<String> lines : wrapped) {
                stream.setStrokingColor(BORDER);
                stream.setLineWidth(0.4f);
                stream.moveTo(x, y);
                stream.lineTo(x, y - height);
                stream.stroke();
                float lineY = y - 16;
                for (String line : lines) {
                    drawText(line, x + 5, lineY, valueFont, fontSize, INK);
                    lineY -= 11;
                }
                x += cellWidth;
            }
            y -= height;
        }

        private void spacer(final float value) {
            y -= value;
        }

        private void keep(final float requiredHeight) throws IOException {
            ensure(requiredHeight);
        }

        private void ensure(final float requiredHeight) throws IOException {
            if (y - requiredHeight < MARGIN + FOOTER_TOP) {
                newPage(pageSize == null ? PDRectangle.A4 : pageSize);
            }
        }

        private void drawText(final String value, final float x, final float baseline,
                              final PDFont font, final float size, final java.awt.Color color) throws IOException {
            if (value.isEmpty()) {
                return;
            }
            stream.beginText();
            stream.setFont(font, size);
            stream.setRenderingMode(RenderingMode.FILL);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(value);
            stream.endText();
        }

        private void fillRect(final float x, final float bottom, final float width, final float height,
                              final java.awt.Color color) throws IOException {
            stream.setNonStrokingColor(color);
            stream.addRect(x, bottom, width, height);
            stream.fill();
        }

        private void strokeRect(final float x, final float bottom, final float width, final float height,
                                final java.awt.Color color) throws IOException {
            stream.setStrokingColor(color);
            stream.setLineWidth(0.5f);
            stream.addRect(x, bottom, width, height);
            stream.stroke();
        }

        private List<String> wrap(final String value, final PDFont font,
                                  final float size, final float maxWidth) {
            String normalized = value.replace('\r', ' ').replace('\n', ' ').trim();
            if (normalized.isEmpty()) {
                return List.of("입력 없음");
            }
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            for (int offset = 0; offset < normalized.length();) {
                int codePoint = normalized.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                String candidate = current + character;
                if (current.length() > 0 && textWidth(candidate, font, size) > maxWidth) {
                    lines.add(current.toString().stripTrailing());
                    current = new StringBuilder(character.stripLeading());
                } else {
                    current.append(character);
                }
                offset += Character.charCount(codePoint);
            }
            if (!current.isEmpty()) {
                lines.add(current.toString().stripTrailing());
            }
            return lines.isEmpty() ? List.of("입력 없음") : lines;
        }

        private float textWidth(final String value, final PDFont font, final float size) {
            try {
                return font.getStringWidth(value) / 1000f * size;
            } catch (IOException | IllegalArgumentException exception) {
                return Float.MAX_VALUE;
            }
        }

        private String safe(final String value, final PDFont font) {
            String source = Objects.toString(value, "");
            StringBuilder result = new StringBuilder(source.length());
            for (int offset = 0; offset < source.length();) {
                int codePoint = source.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                try {
                    font.getStringWidth(character);
                    result.append(character);
                } catch (IOException | IllegalArgumentException exception) {
                    result.append('?');
                }
                offset += Character.charCount(codePoint);
            }
            return result.toString();
        }

        private java.awt.Color statusColor(final String status) {
            return switch (status) {
                case "괜찮음" -> PRIMARY;
                case "주의" -> new java.awt.Color(185, 126, 20);
                default -> MUTED;
            };
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        @Override
        public void close() throws IOException {
            closeStream();
        }
    }
}
