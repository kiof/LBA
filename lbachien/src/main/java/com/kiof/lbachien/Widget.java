package com.kiof.lbachien;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class Widget extends AppWidgetProvider {

	public void onEnabled(Context context) {
		super.onEnabled(context);
		// Toast.makeText(context, "Widget onEnabled", Toast.LENGTH_SHORT).show();
	}

	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		final int N = appWidgetIds.length;

		// Toast.makeText(context, "Widget onUpdate", Toast.LENGTH_SHORT).show();

		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int i = 0; i < N; i++) {
			int appWidgetId = appWidgetIds[i];

			// Create an Intent to launch PlaySound
			Intent intentPlaySound = new Intent(context, PlaySound.class);
			PendingIntent pendingIntentPlaySound = PendingIntent.getActivity( context, 0, intentPlaySound, 0);

			// Create an Intent to launch Soundboard
			Intent intentSoundboard = new Intent(context, Soundboard.class);
			PendingIntent pendingIntentSoundboard = PendingIntent.getActivity( context, 0, intentSoundboard, 0);

			// Get the layout for the App Widget and attach an on-click listener to the button
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			views.setOnClickPendingIntent(R.id.widgetbutton, pendingIntentPlaySound);
			views.setOnClickPendingIntent(R.id.widgeticon, pendingIntentSoundboard);

			// Tell the AppWidgetManager to perform an update on the current app widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}
}
