package com.andrewjameskrause.calendargenerator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

public class CalendarGenerator {
    private static final float DPI = 72.0f;
    private static final float CORNER_RADIUS = 12.0f;
    private static final float PAGE_WIDTH = 11.25f;
    private static final float PAGE_HEIGHT = 11.25f;
    private static float DOWNWARD_SHIFT = 0.75f;

    private static Map<LocalDate, String> dates = new HashMap<>();
    private static PDType0Font font1;
    private static PDType1Font font2;
    
    private static void loadDates() throws IOException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        BufferedReader reader = new BufferedReader(new FileReader("dates2026.csv"));
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
        //  draw from center
        x -= (width / 2.0f);
        y += (height / 2.0f);
        
        //  set line width and color
        contentStream.setLineWidth(1);
        contentStream.setStrokingColor(Color.GRAY);
        contentStream.setNonStrokingColor(color);

        //  start path
        contentStream.moveTo(x + CORNER_RADIUS, y);

        //  top-right corner
        contentStream.lineTo(x + width - CORNER_RADIUS, y);
        contentStream.curveTo(x + width, y, x + width, y + CORNER_RADIUS, x + width, y + CORNER_RADIUS);

        //  bottom-right corner
        contentStream.lineTo(x + width, y + height - CORNER_RADIUS);
        contentStream.curveTo(x + width, y + height, x + width - CORNER_RADIUS, y + height, x + width - CORNER_RADIUS, y + height);

        //  bottom-left corner
        contentStream.lineTo(x + CORNER_RADIUS, y + height);
        contentStream.curveTo(x, y + height, x, y + height - CORNER_RADIUS, x, y + height - CORNER_RADIUS);

        //  top-left corner
        contentStream.lineTo(x, y + CORNER_RADIUS);
        contentStream.curveTo(x, y, x + CORNER_RADIUS, y, x + CORNER_RADIUS, y);

        //  close and fill/stroke
        contentStream.closePath();
        contentStream.fillAndStroke();
    }
    
    private static void centerText(PDFont font, PDPageContentStream contentStream, float fontSize, float x, float y, String text, Color color) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(color);
        contentStream.setFont(font, fontSize);
        float stringWidth = font.getStringWidth(text) / 1000 * fontSize;
        contentStream.newLineAtOffset(x - (stringWidth / 2.0f), y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private static void drawMinimonth(PDPageContentStream contentStream, int xOffset, int year, int month) throws IOException {
        final float TOP = 745.0f - (DOWNWARD_SHIFT * DPI);
        final float X_SPACING = 17.0f;
        final float Y_SPACING = 13.0f;
        
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

            float xPos = x * X_SPACING + xOffset;
            float yPos = TOP - y * Y_SPACING;
            centerText(font2, contentStream, 9.0f, xPos, yPos, Integer.toString(day), Color.BLACK);
        }        
        
        String[] weekdays = {"S", "M", "T", "W", "TH", "F", "S"};
        for (x = 0; x < 7; x++) {
            float xPos = x * X_SPACING + xOffset;
            float yPos = TOP + Y_SPACING;
            centerText(font2, contentStream, 9.0f, xPos, yPos, weekdays[x], Color.BLACK);
        }
        
        String monthName = new DateFormatSymbols().getMonths()[month - 1];
        centerText(font2, contentStream, 9.0f, xOffset + 3 * X_SPACING, TOP + (Y_SPACING * 2) + 5, monthName + " " + Integer.toString(year), Color.BLACK);
    }

    private static void generateMonth(PDDocument document, int year, int month) throws IOException {
        PDPage page = new PDPage(new PDRectangle(PAGE_WIDTH * DPI, PAGE_HEIGHT * DPI));
        PDRectangle mediaBox = page.getMediaBox();
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
/*        
        //  set page background color
        contentStream.setNonStrokingColor(new java.awt.Color(227, 248, 240));
        contentStream.addRect(mediaBox.getLowerLeftX(), mediaBox.getLowerLeftY(),
                mediaBox.getWidth(), mediaBox.getHeight());
        contentStream.fill();
*/    
        YearMonth yearMonth = YearMonth.of(year, month);
        WeekFields wf = WeekFields.of(Locale.US);
        int lastDayWeek = yearMonth.atEndOfMonth().get(wf.weekOfMonth());
        int firstDayWeek = yearMonth.atDay(1).get(wf.weekOfMonth());
        int weeksInMonth = lastDayWeek - firstDayWeek + 1;
        
        float width = ((PAGE_WIDTH - 1.0f) * DPI) / 7.0f;
        float height = ((PAGE_HEIGHT - 3.0f) * DPI) / (weeksInMonth < 6 ? 6.5f : 7.0f);
        
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

            float xPos = (x + 0.325f) * width + (width * 0.5f);
            float shift = ((weeksInMonth < 6 ? 0.2f : 0.4f) + DOWNWARD_SHIFT) * DPI;
            float yPos = mediaBox.getHeight() - shift - (y + 3.25f) * height;
            Color color = (x == 0 || x == 6) ? new Color(0.9f, 0.9f, 0.9f) : Color.WHITE; // Color.LIGHT_GRAY : Color.WHITE;
            drawBox(contentStream, xPos, yPos, width * 0.95f, height * 0.95f, color);

            centerText(font2, contentStream, 18.0f, xPos + 20.0f - (width / 2.0f), yPos + height - 30.0f + (height / 2.0f), Integer.toString(day), Color.BLACK);

            LocalDate key = LocalDate.of(year, month, day);
            if (dates.containsKey(key)) {
                String dateName = dates.get(key);
                String[] parts = dateName.split("\\\\n");
                float textY = yPos + 10.0f + (height / 2.0f);
                for (int i = parts.length - 1; i >= 0; i--) {
                    centerText(font2, contentStream, 10.0f, xPos, textY, parts[i], Color.BLACK);
                    textY += 15.0f;
                }
            }
        }
        
        //  draw day of week header
        height = ((PAGE_HEIGHT - 3.0f) * DPI) / 6.0f;
        for (int day = 0; day < 7; day++) {
            String[] NAMES = { "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "CATURDAY" };
            
            float xPos = (day + 0.325f) * width + (width * 0.5f);
            float yPos = mediaBox.getHeight() - (DOWNWARD_SHIFT * DPI) - (1.825f) * height;
            drawBox(contentStream, xPos, yPos, width * 0.95f, 0.35f * DPI, new Color(0.65f, 0.65f, 0.65f));

            centerText(font2, contentStream, 12.0f, xPos, yPos + 20.0f, NAMES[day], Color.WHITE);
        }

        //  month title
        String monthName = new DateFormatSymbols().getMonths()[month - 1];
        centerText(font1, contentStream, 54.0f, (mediaBox.getWidth() / 2.0f), mediaBox.getHeight() - (DOWNWARD_SHIFT * DPI) - (1.2f * DPI), monthName, Color.BLACK);

        //  minimonths
        int miniMonth = month - 1;
        int miniYear = year;
        if (miniMonth < 1) {
            miniMonth += 12;
            miniYear--;
        }
        drawMinimonth(contentStream, 50, miniYear, miniMonth);

        miniMonth = month + 1;
        miniYear = year;
        if (miniMonth > 12) {
            miniMonth -= 12;
            miniYear++;
        }
        drawMinimonth(contentStream, 650, miniYear, miniMonth); // TODO: magic number!
        
        contentStream.close();
        document.addPage(page);
    }
    
    public static void main(String[] args) throws IOException {
        loadDates();

        PDDocument document = new PDDocument();

        font1 = PDType0Font.load(document, new File("MarysonPrintPERSONALUSE-Regular.ttf"));
        font2 = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        for (int month = 1; month <= 12; month++) {
            generateMonth(document, 2026, month);
        }
        document.save("sample.pdf");
        document.close();
    }
}

