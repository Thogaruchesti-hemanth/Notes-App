package com.example.NotesNest.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.view.View;

import androidx.core.content.FileProvider;

import com.example.NotesNest.databases.ViewModels.CategoryViewModel;
import com.example.NotesNest.databases.entities.NoteEntity;

import java.io.File;
import java.io.FileOutputStream;

public class NoteShareManager {

    private final Context context;

    public NoteShareManager(Context context) {
        this.context = context;
    }

    // 9. CONVERT HTML TO PLAIN TEXT
    public static String htmlToPlain(String html) {
        if (html == null) return "";
        return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim();
    }

    // 1. SHARE AS TEXT
    public void shareAsText(NoteEntity note, CategoryViewModel categoryViewModel) {
        categoryViewModel.getCategoryName(note.categoryId, categoryName -> {

            String title = note.title;
            String date = DateTimeUtils.getReadableDate(note.createdAt);
            String time = DateTimeUtils.getReadableTime(note.createdAt);
            String plainContent = htmlToPlain(note.content);

            String data = "📌 *" + title + "*\n" +
                    "🗂 Category: " + categoryName + "\n" +
                    "📅 Created: " + date + "\n" +
                    "⏰ Time: " + time + "\n\n" +
                    "📝 Note:\n" +
                    plainContent + "\n";

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, data);
            context.startActivity(
                    Intent.createChooser(send, "Share Note as Text")
            );
        });
    }


    // 2. CAPTURE VIEW AS BITMAP
    public Bitmap captureViewAsBitmap(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }


    // 3. SAVE BITMAP AS IMAGE (PNG)
    public File saveBitmapAsImage(Bitmap bitmap, String fileName) throws Exception {
        File file = new File(context.getExternalFilesDir(null), fileName + ".png");
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.flush();
        out.close();
        return file;
    }

    // 4. CREATE PDF FROM BITMAP
    public File createPdfFromBitmap(Bitmap bitmap, String fileName) throws Exception {
        File pdfFile = new File(context.getExternalFilesDir(null), fileName + ".pdf");

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(bitmap, 0f, 0f, null);
        document.finishPage(page);

        FileOutputStream out = new FileOutputStream(pdfFile);
        document.writeTo(out);
        document.close();
        out.close();

        return pdfFile;
    }


    // 5. SHARE IMAGE VIA INTENT
    public void shareImage(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    file
            );

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("image/png");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(share, "Share Image"));

        } catch (Exception e) {
            Log.e("NoteShareManager", "Error while sharing image: " + e.getMessage(), e);
        }
    }

    // 6. SHARE PDF VIA INTENT
    public void sharePdf(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".provider",
                    file
            );

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/pdf");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(share, "Share PDF"));

        } catch (Exception e) {
            Log.e("NoteShareManager", "Error while sharing PDF: " + e.getMessage(), e);
        }
    }

    // 7. SHARE NOTE AS IMAGE (FROM TITLE/CONTENT)
    public void shareAsImage(NoteEntity note, View noteView) {
        try {

            Bitmap noteBitMap = captureViewAsBitmap(noteView);
            File file = saveBitmapAsImage(noteBitMap, note.title);
            shareImage(file);

        } catch (Exception e) {
            Log.e("NoteShareManager", "Error while sharing note as image: " + e.getMessage(), e);
        }
    }

    // 8. SHARE NOTE AS PDF (FROM TITLE/CONTENT)
    public void shareAsPdf(NoteEntity note, View noteView) {
        try {

            Bitmap noteBitMap = captureViewAsBitmap(noteView);
            File pdfFile = createPdfFromBitmap(noteBitMap, note.title);
            sharePdf(pdfFile);

        } catch (Exception e) {
            Log.e("NoteShareManager", "Error while sharing note as pdf: " + e.getMessage(), e);
        }
    }
}
