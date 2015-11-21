package de.struckmeierfliesen.ds.wochenbericht;

import android.content.Context;
import android.content.SharedPreferences;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ReportGenerator {
    public static final int CUTOFF = 85;

    private DataBaseConnection dbConn;
    private SharedPreferences sharedPrefs;

    private PDDocument pdfTemplate;
    private PDPageContentStream contentStream;
    private PDPage page;
    private PDFont font;

    public ReportGenerator(Context context) throws IOException, COSVisitorException {
        dbConn = new DataBaseConnection(context);
        sharedPrefs = context.getSharedPreferences(
                "de.struckmeierfliesen.ds.wochenbericht.SETTINGS", Context.MODE_PRIVATE);

        pdfTemplate = PDDocument.load(context.getResources().openRawResource(R.raw.sample_report_raw));
        // TODO preload on fridays
        page = (PDPage) pdfTemplate.getDocumentCatalog().getAllPages().get(0);
        font = PDType1Font.HELVETICA;
        contentStream = new PDPageContentStream(pdfTemplate, page, true, true);
        page.getContents().getStream();
    }

    private void addText(String content, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.moveTextPositionByAmount(x, y);
        contentStream.drawString(content);
        contentStream.endText();
    }

    private void addText(String content, float x, float y, PDFont font, int size) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, size);
        contentStream.moveTextPositionByAmount(x, y);
        contentStream.drawString(content);
        contentStream.endText();
    }

    public void fillIn(Date date, String comment, String pageNumber, String yearNumber) throws IOException, COSVisitorException {
        // load Data
        List<List<Entry>> week = getWeekOfEntries(date);
        String[] names = loadNames();
        Date[] datesOfLastWeek = Util.getDatesOfLastWeek(date);
        String startDate = Util.formatDate(datesOfLastWeek[0]);
        String endDate = Util.formatDate(datesOfLastWeek[4]);

        float height = page.getMediaBox().getHeight();
        // Vorname
        addText(names[0], 95, height - 55, font, 12);
        // Nachname
        addText(names[1], 370, height - 55);
        // Blatt Nummer
        addText(pageNumber, 140, height - 105);
        // Ausbildungsjahr
        addText(yearNumber, 480, height - 80);
        // Woche vom
        addText(startDate, 285, height - 105);
        // Woche bis
        addText(endDate, 425, height - 105);
        // Arbeitsbeschreibungen
        final float LINE_HEIGHT = 12.5f;
        final float MONDAY_TOP_MARGIN = 166;
        int weekDurationSum = -1;
        for (int dayNr = 0; dayNr < week.size() && dayNr < 6; dayNr++) {
            int dayDurationSum = -1;
            List<Entry> dayEntries = week.get(dayNr);
            for (int entryNr = 0; entryNr < dayEntries.size() && entryNr < 5; entryNr++) {
                Entry entry = dayEntries.get(entryNr);
                if (dayDurationSum == -1) dayDurationSum = 0;
                dayDurationSum += entry.duration;

                // Arbeitsbeschreibung
                addText(entry.getWork(), 40, height - (MONDAY_TOP_MARGIN + dayNr * 75 + entryNr * LINE_HEIGHT),
                        font, 10);
                // Arbeitsdauer
                addText(entry.getDuration(), 470, height - (MONDAY_TOP_MARGIN + dayNr * 75 + entryNr * LINE_HEIGHT),
                        font, 10);
            }
            if (weekDurationSum == -1) weekDurationSum = 0;
            weekDurationSum += dayDurationSum;

            // Arbeitsdauer - Tagessumme
            addText(Util.convertDuration(dayDurationSum), 510, height - (MONDAY_TOP_MARGIN + dayNr * 75 + 4 * LINE_HEIGHT));
        }
        // Wochenstunden
        addText(Util.convertDuration(weekDurationSum), 510, height - 606);
        // Bemerkungen
        String[] comments = new String[] {comment, ""};
        if(comment.length() > CUTOFF) {
            comments[0] = comment.substring(0, CUTOFF + 1);
            comments[1] = comment.substring(CUTOFF + 1);
        }
        // Bemerkungen Zeile 1
        addText(comments[0], 45, height - 633);
        // Bemerkungen Zeile 2
        addText(comments[1], 45, height - 644.5f);

        contentStream.close();

        pdfTemplate.save(Util.newFile("newPDF.pdf"));
        pdfTemplate.close();
    }

    private String[] loadNames() {
        String firstName = sharedPrefs.getString("firstName", "");
        String name = sharedPrefs.getString("name", "");
        return new String[] {firstName, name};
    }

    private List<List<Entry>> getWeekOfEntries(Date date) {
        dbConn.open();
        List<List<Entry>> entries = dbConn.getLastWeekEntries(date);// TODO rename this shit
        dbConn.close();
        return entries;
    }
}
