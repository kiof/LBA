package com.kiof.lbaanimal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.webkit.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class HtmlAlertDialog extends AlertDialog {

    HtmlAlertDialog(Context context, int resourceId, String title, int iconId) {
        super(context);

        WebView wv = new WebView(context);
        wv.setBackgroundColor(0); // transparent

        String htmlString = HtmlAlertDialog.loadRawResourceString(
                context.getResources(), resourceId);
        wv.loadData(htmlString, "text/html", "utf-8");

        if (!title.equals(""))
            this.setTitle(title);
        if (iconId != 0)
            this.setIcon(iconId);
        this.setView(wv);
        this.setButton(BUTTON_POSITIVE, context.getResources().getString(R.string.Ok),
                (dialog, id) -> dialog.cancel()
        );
    }

    private static String loadRawResourceString(Resources res, int resourceId) {
        StringBuilder builder = new StringBuilder();
        InputStream is = res.openRawResource(resourceId);
        InputStreamReader reader = new InputStreamReader(is);
        char[] buf = new char[1024];
        int numRead;
        try {
            while (-1 != (numRead = reader.read(buf))) {
                builder.append(buf, 0, numRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

}
