package com.andrewjameskrause.calendargenerator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class CalendarGenerator {
    private static final float DPI = 72.0f;
    private static final float CORNER_RADIUS = 12.0f;
    private static final float PAGE_WIDTH = 11.25f;
    private static final float PAGE_HEIGHT = 11.25f;

    private static Map<LocalDate, String> dates = new HashMap<>();
    private static PDType1Font font;
    
    private static void loadDates() throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        BufferedReader reader = new BufferedReader(new FileReader("dates.csv"));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            line = line.replaceAll("\"", "").replaceAll(",$", "");

            String[] parts = line.split(",", 2);
            if (parts.length < 2) continue;

            String dateStr = parts[0].trim();
            String name = parts[1].trim();

            LocalDate date = LocalDate.parse(dateStr, formatter);
            dates.put(date, name);
        }
    }
    
    private static void drawBox(PDPageContentStream contentStream, float x, float y, float width, float height, Color color) throws IOException {
        // Set line width and color
        contentStream.setLineWidth(1);
        contentStream.setStrokingColor(Color.GRAY);
        contentStream.setNonStrokingColor(color);

        // Start path
        contentStream.moveTo(x + CORNER_RADIUS, y);

        // Top-right corner
        contentStream.lineTo(x + width - CORNER_RADIUS, y);
        contentStream.curveTo(x + width, y, x + width, y + CORNER_RADIUS, x + width, y + CORNER_RADIUS);

        // Bottom-right corner
        contentStream.lineTo(x + width, y + height - CORNER_RADIUS);
        contentStream.curveTo(x + width, y + height, x + width - CORNER_RADIUS, y + height, x + width - CORNER_RADIUS, y + height);

        // Bottom-left corner
        contentStream.lineTo(x + CORNER_RADIUS, y + height);
        contentStream.curveTo(x, y + height, x, y + height - CORNER_RADIUS, x, y + height - CORNER_RADIUS);

        // Top-left corner
        contentStream.lineTo(x, y + CORNER_RADIUS);
        contentStream.curveTo(x, y, x + CORNER_RADIUS, y, x + CORNER_RADIUS, y);

        // Close and fill/stroke
        contentStream.closePath();
        contentStream.fillAndStroke();
    }
    
    private static void centerText(PDPageContentStream contentStream, float fontSize, float x, float y, String text) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(Color.BLACK);
        contentStream.setFont(font, fontSize);
        float stringWidth = font.getStringWidth(text) / 1000 * fontSize;
        contentStream.newLineAtOffset(x - (stringWidth / 2.0f), y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private static void generateMonth(PDDocument document, int year, int month) throws IOException {
        PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH * DPI, PAGE_HEIGHT * DPI));
        PDRectangle mediaBox = page.getMediaBox();
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        contentStream.setNonStrokingColor(new java.awt.Color(227, 248, 240));
        contentStream.addRect(mediaBox.getLowerLeftX(), mediaBox.getLowerLeftY(),
                mediaBox.getWidth(), mediaBox.getHeight());
        contentStream.fill();
    
        float width = ((PAGE_WIDTH - 1.0f) * DPI) / 7.0f;
        float height = ((PAGE_HEIGHT - 3.0f) * DPI) / 6.0f;

        LocalDate firstDay = LocalDate.of(year, month, 1);

        int x = firstDay.getDayOfWeek().getValue() - 1;
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        
        int y = 0;
        for (int day = 1; day <= daysInMonth; day++) {
            x++;
            if (x >= 7) {
                x = 0;
                if (day > 1) {
                    y++;
                }
            }

            float xPos = (x + 0.5f) * width;
            float yPos = mediaBox.getHeight() - (y + 2.75f) * height;
            Color color = (x == 0 || x == 6) ? Color.LIGHT_GRAY : Color.WHITE;
            drawBox(contentStream, xPos, yPos, width * 0.95f, height * 0.95f, color);

            centerText(contentStream, 18.0f, xPos + 20.0f, yPos + height - 30.0f, Integer.toString(day));

            LocalDate key = LocalDate.of(year, month, day);
            if (dates.containsKey(key)) {
                centerText(contentStream, 10.0f, xPos + width / 2, yPos + + 10.0f, dates.get(key));
            }
        }
        
        String monthName = new DateFormatSymbols().getMonths()[month - 1];
        centerText(contentStream, 54.0f, (mediaBox.getWidth() / 2.0f), mediaBox.getHeight() - (1.5f * DPI), monthName);

        contentStream.close();
        document.addPage(page);
    }
    
    public static void main(String[] args) throws IOException {
        loadDates();

        font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        PDDocument document = new PDDocument();
        for (int month = 1; month <= 12; month++) {
            generateMonth(document, 2026, month);
        }
        document.save("calendar.pdf");
        document.close();
    }
}

