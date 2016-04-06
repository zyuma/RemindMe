package com.zyuma.remindme;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by yuma on 2016-04-04.
 */
public class HandleVoiceActivity extends Activity {

    private static final String TAG = "HandleVoiceActivity()";
    private static final String ACTION_CREATE_NOTE = "com.google.android.gm.action.AUTO_SEND";

    private String mVoiceCommand;
    private String mReminder;
    private int mTime;
    private CustomNotification mNotification;
    private boolean flag_savingTaskRunning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);

        // Get the intent
        Intent intent = getIntent();
        Log.i(TAG, "Received intent: " + intent.getAction());
        if (ACTION_CREATE_NOTE.equals(intent.getAction())) {
            Log.i(TAG, "CREATE_NOTE INTENT");
            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                //mTitle = intent.getStringExtra("EXTRA_NAME");
                mVoiceCommand = intent.getStringExtra(Intent.EXTRA_TEXT);
                Log.i(TAG, "FOUND EXTRA_TEXT");
                Log.i(TAG, mVoiceCommand);

                if(parseVoiceCommand(mVoiceCommand)) {
                    showToast(mReminder + " in " + Integer.toString(mTime));
                    Random r = new Random();
                    int id = r.nextInt(9999 - 0) + 0;
                    scheduleNotification(id, mReminder, mTime);
                }
            }

        }
        else {
            Log.i(TAG, "NO CREATE_NOTE INTENT");
        }
        finish();
    }

    private Boolean parseVoiceCommand(String s) {
        String[] wordList = s.split(" ");
        if (!wordList[wordList.length-3].equals("in")) {
            showToast("Try \"XXX in XXX seconds\"");
            return false;
        }
        String time = wordList[wordList.length-2];
        String[] reminderWordList = Arrays.copyOfRange(wordList, 0, wordList.length - 3);
        String reminder = TextUtils.join(" ", reminderWordList);
        Log.i("SPLIT", time);
        try
        {
            mTime = Integer.parseInt(time.trim());
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("NumberFormatException: " + nfe.getMessage());
            showToast("Enter valid time in seconds");
            return false;
        }

        Log.i("SPLIT", reminder);
        mReminder = reminder;
        return true;
    }

    public void showToast(String toastMsg) {
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, toastMsg, duration);
        toast.show();
    }

    private void scheduleNotification(int id, String reminder, int time) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        Intent notificationIntent = new Intent("android.media.action.DISPLAY_NOTIFICATION");
        notificationIntent.addCategory("android.intent.category.DEFAULT");
        notificationIntent.putExtra("REMINDER", reminder);
        notificationIntent.putExtra("ID", id);

        PendingIntent broadcast = PendingIntent.getBroadcast(this, 100, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, time);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), broadcast);

        // Update the scheduled notification database
        mNotification = new CustomNotification();
        mNotification.setID(id);
        mNotification.setReminder(reminder);
        mNotification.setWaitTime(time);
        mNotification.setReminderTime(System.currentTimeMillis());

        if (!flag_savingTaskRunning) {
            flag_savingTaskRunning = true;
            new SaveToDatabaseTask(this).execute();
            Log.i("WITH VOICE", "Saved");
        }

        showToast("Reminder set for after " + Integer.toString(time) + " seconds");
    }

    private class SaveToDatabaseTask extends AsyncTask<Void, Void, Void> {

        private HandleVoiceActivity activity;
        private AlertDialog dialog;

        public SaveToDatabaseTask(HandleVoiceActivity activity) {
            super();
            this.activity = activity;
        }

        @Override
        protected void onPreExecute() {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("Saving");
            builder.setView(new ProgressBar(activity));
            builder.setCancelable(false);
            if(dialog == null) {
                dialog = builder.create();
            }
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {

            NotificationDBHelper helper = new NotificationDBHelper(activity);
            helper.addNotification(mNotification);
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            dialog.dismiss();
        }

    }
}
