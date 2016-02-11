package com.reuniware.apps.mysmsbackup2;
/**
 * MySmsBackup2 differs from MySmsBackup because it deletes any previous backups in label MySmsBackup on Gmail
 * and then sends the current snapshot of all SMS's that are in folders Inbox and Sentbox on the mobile phone.
 * (MySmsBackup sends each SMS in separate emails in label MySmsBackup on Gmail).
 * MySmsBackup2 is a lot faster than MySmsBackup because it only sends one email to yourself and all SMS's in the body of the email.
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MySmsBackupDbHelper dbHelper = null;

    Button btnExportToFile = null;
    Button btnExportToGmail = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Contact us : reunisoft@gmail.com", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });*/

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest.Builder adRequest = new AdRequest.Builder();
        adRequest.addTestDevice("0C76BFEC06A6B036B743B85E20AE4F11");
        mAdView.loadAd(adRequest.build());

        dbHelper = new MySmsBackupDbHelper(this);

        btnExportToFile = (Button) findViewById(R.id.btnExportToFile);
        btnExportToFile.setOnClickListener(this);

        btnExportToGmail = (Button) findViewById(R.id.btnExportToGmail);
        btnExportToGmail.setOnClickListener(this);

        ((TextView) findViewById(R.id.textViewExportFile)).setText(getMainExportFilePath());

        //ExpandableListView listViewEmails = (ExpandableListView) findViewById(R.id.listViewEmails);
        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_expandable_list_item_1, emails);
        //listViewEmails.setAdapter(adapter);

        AccountManager accountManager = AccountManager.get(this.getApplicationContext());
        Account[] accounts = accountManager.getAccountsByType("com.google");
        Account account;
        if (accounts.length > 0) {
            for (int i = 0; i < accounts.length; i++) {
                account = accounts[i];
                Log.d(TAG, (account.name));
            }
        } else {
            account = null;
        }

        String[] emails = new String[accounts.length];
        for(int k=0;k<accounts.length;k++){
            emails[k] = accounts[k].name;
        }

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, R.layout.support_simple_spinner_dropdown_item, emails);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Object str = spinner.getSelectedItem();
        Log.d(TAG, "Current Selected Email = " + str);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "pos=" + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        int i= dbHelper.getCredentialsCount();
        Log.d(TAG," nb records in db = " + i);
        dbHelper.getAllCredentials();
        Log.d(TAG, "password = " + dbHelper.getPasswordForLogin((String)str));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static final String TAG = "MySmsBackup";

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnExportToFile:
                Log.d(TAG, "onClick: btnExportToFile");

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setTitle("Confirmation");
                alertDialogBuilder
                        .setMessage("This will overwrite any previous export file. Click yes to export to file " + getMainExportFilePath())
                        .setCancelable(false)
                        .setPositiveButton("Yes",new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                ExportSmsToFile();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();

                break;

            case R.id.btnExportToGmail:
                Log.d(TAG, "onClick: btnExportToGmail");

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Please enter Gmail pasword");
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                Spinner spinner = (Spinner) findViewById(R.id.spinner);
                Object str = spinner.getSelectedItem();
                Log.d(TAG, "Current Selected Email = " + str);
                final String currentSelectedEmail = (String) str;
                if (dbHelper.loginExistsInCredentials(currentSelectedEmail)){
                    input.setText(dbHelper.getPasswordForLogin(currentSelectedEmail));
                }

                builder.setView(input);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String currentPassword = input.getText().toString();
                        Log.d(TAG, "Current entered Password = " + currentPassword);

                        if (!dbHelper.loginExistsInCredentials(currentSelectedEmail)) {
                            dbHelper.insertCredentials(currentSelectedEmail, currentPassword);
                        } else {
                            dbHelper.updateCredentialsPassword(currentSelectedEmail, currentPassword);
                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                gmailTest(currentSelectedEmail, currentPassword);
                            }
                        }).start();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();

                break;

        }
    }

    private void ExportSmsToFile() {
        //showToastMessage("Getting SMS for export");

        ArrayList<AndroidSms> lstAndroidSms = getSmsMessages(SMS_TYPE.SMS_INBOX);
        lstAndroidSms.addAll(getSmsMessages(SMS_TYPE.SMS_SENT));
        Log.d(TAG, "nb sms = " + lstAndroidSms.size());

        Collections.sort(lstAndroidSms, AndroidSms.compareByThreadId);
        Collections.sort(lstAndroidSms, AndroidSms.compareByDate);
        Collections.sort(lstAndroidSms, AndroidSms.compareByContactName);

        //showToastMessage("Export started (" + lstAndroidSms.size() + " SMS)");

        try {
            File myFile = new File(getMainExportFilePath());
            if (myFile.exists()) {
                myFile.delete();
            }
            myFile.createNewFile();

            FileWriter fileWriter = new FileWriter(myFile, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (int i=0;i<lstAndroidSms.size();i++) {
                Log.d(TAG, "writing " + i);
                AndroidSms androidSms = lstAndroidSms.get(i);
                bufferedWriter.write("TID=" + androidSms.thread_id + " ID=" + androidSms._id + "\r\n");
                bufferedWriter.write(getContactNameByPhoneNumber(androidSms.address) + " (" + androidSms.address + ")" + "\r\n");

                if (androidSms.type.equals("1"))
                    bufferedWriter.write("Received on   ");
                else if (androidSms.type.equals("2"))
                    bufferedWriter.write("Sent on       ");
                bufferedWriter.write(androidSms.date + "\r\n");

                bufferedWriter.write(androidSms.body + "\r\n");
                bufferedWriter.write("\r\n");
                bufferedWriter.flush();
            }

            //bufferedWriter.close();
            fileWriter.flush();
            fileWriter.close();

            showOkDialog("Export OK", lstAndroidSms.size() + " SMS have been exported to file " + getMainExportFilePath());
            //showToastMessage("Export done (" + lstAndroidSms.size() + " SMS)");


        } catch (Exception e) {
            Log.d(TAG, "ExportSmsToFile:" + e.toString());
        }
    }

    private void ExportSmsToFileForGmail() {
        //showToastMessage("Getting SMS for export");

        ArrayList<AndroidSms> lstAndroidSms = getSmsMessages(SMS_TYPE.SMS_INBOX);
        lstAndroidSms.addAll(getSmsMessages(SMS_TYPE.SMS_SENT));
        Log.d(TAG, "nb sms = " + lstAndroidSms.size());

        Collections.sort(lstAndroidSms, AndroidSms.compareByThreadId);
        Collections.sort(lstAndroidSms, AndroidSms.compareByDate);
        Collections.sort(lstAndroidSms, AndroidSms.compareByContactName);

        //showToastMessage("Export started (" + lstAndroidSms.size() + " SMS)");

        try {
            File myFile = new File(getMainExportFilePathForGmail());
            if (myFile.exists()) {
                myFile.delete();
            }
            myFile.createNewFile();

            FileWriter fileWriter = new FileWriter(myFile, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            for (int i=0;i<lstAndroidSms.size();i++) {
                Log.d(TAG, "writing " + i);
                AndroidSms androidSms = lstAndroidSms.get(i);
                //bufferedWriter.write("TID=" + androidSms.thread_id + " ID=" + androidSms._id + "\r\n");

                if (androidSms.type.equals("1"))
                    bufferedWriter.write("Received from ");
                else if (androidSms.type.equals("2"))
                    bufferedWriter.write("Sent to       ");
                bufferedWriter.write((getContactNameByPhoneNumber(androidSms.address) + " (" + androidSms.address + ")").trim()+ "\r\n");

                if (androidSms.type.equals("1"))
                    bufferedWriter.write("Received on   ");
                else if (androidSms.type.equals("2"))
                    bufferedWriter.write("Sent on       ");
                bufferedWriter.write(androidSms.date + "\r\n");

                bufferedWriter.write("[" + androidSms.body + "]\r\n");
                bufferedWriter.write("\r\n");
                bufferedWriter.flush();
            }

            //bufferedWriter.close();
            fileWriter.flush();
            fileWriter.close();

            //showOkDialog("Export OK", lstAndroidSms.size() + " SMS have been exported to file " + getMainExportFilePathForGmail());
            //showToastMessage("Export done (" + lstAndroidSms.size() + " SMS)");

        } catch (Exception e) {
            Log.d(TAG, "ExportSmsToFileForGmail:" + e.toString());
        }
    }

    public void showOkDialog(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    AlertDialog alertDialog = null;

    public void showWaitDialog(String title, String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(title);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false);
        alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }

    public void hideWaitDialog() {
        if (alertDialog!=null)
            alertDialog.dismiss();
    }

    public void showToastMessage(String str) {
        Toast toast = new Toast(this);
        TextView textView = new TextView(this);
        textView.setText(" " + str + " ");
        textView.setBackgroundColor(Color.BLACK);
        textView.setTextColor(Color.GREEN);
        toast.setView(textView);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }


    private void getMmsMessages() {
        Log.d(TAG, "getMmsMessages START");
        //http://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android
        try {
            ContentResolver contentResolver = getContentResolver();
            final String[] projection = new String[]{"*"};
            Uri uri = Uri.parse("content://mms-sms/inbox/");
            Cursor query = contentResolver.query(uri, projection, null, null, null);
        }catch (Exception ex){
            Log.d(TAG, ex.toString());
        }

        Log.d(TAG, "getMmsMessages END");
    }



    public String getContactNameByPhoneNumber(String address) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address));
        Cursor cs = this.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, ContactsContract.PhoneLookup.NUMBER + "='" + address + "'", null, null);
        String name = "";
        if (cs.getCount() > 0) {
            cs.moveToFirst();
            name = cs.getString(cs.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        cs.close();
        return name;
    }

    private String convertMillisecondsToDate(String value) {
        String finalDateString = "";
        try {
            long milliSeconds = Long.parseLong(value);
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(milliSeconds);
            finalDateString = formatter.format(calendar.getTime());
            //Log.d(TAG, "date parsed =" + finalDateString);
        } catch (Exception pe) {
            Log.d(TAG, "date parsing exception:" + pe.toString());
        }
        return finalDateString;
    }

    public String getContacts() {
        String finalResult = "";
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            finalResult += name + ";" + phoneNumber + "\r\n";
        }
        cursor.close();
        return finalResult;
    }

    public void gmailTest(final String currentSelectedEmail, String currentPassword) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnExportToGmail.setEnabled(false);
            }
        });

        Properties props = new Properties();
        props.setProperty("mail.imap.host", "imap.gmail.com");
        props.setProperty("mail.imap.port", "993");
        props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.imap.socketFactory.fallback", "false");
        props.setProperty("mail.store.protocol", "imap");

        Session session = Session.getInstance(props);
        //session.setDebug(true);
        try {
            Store store = session.getStore("imap");
            store.connect("imap.gmail.com", 993, currentSelectedEmail, currentPassword);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showWaitDialog("Backup to Gmail (snapshot).", "Please wait while backup.");
                }
            });

            boolean found = false;
            Folder[] f = store.getDefaultFolder().list();
            for (Folder fd : f) {
                String folderName = fd.getFullName();
                //Log.d(TAG, fd.getFullName());
                if (folderName.equals("MySmsBackup")) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Log.d(TAG, "MySmsBackup folder has not been found.");
                Log.d(TAG, "MySmsBackup folder will be created.");
                boolean created = store.getDefaultFolder().getFolder("MySmsBackup").create(Folder.HOLDS_MESSAGES);
                if (created)
                    Log.d(TAG, "MySmsBackup folder has been created");
            }

            Message[] serverMessages = null;
            Folder msb = null;
            if (found) {
                Log.d(TAG, "MySmsBackup folder has been found");

                FetchProfile fp = new FetchProfile();
                fp.add(FetchProfile.Item.CONTENT_INFO);
                Log.d(TAG, "getting messages from server");
                msb = store.getDefaultFolder().getFolder("MySmsBackup");
                msb.open(Folder.READ_WRITE);
                serverMessages = new MimeMessage[msb.getMessages().length];
                serverMessages = msb.getMessages();
                Log.d(TAG, "retrieved:" + serverMessages.length);
                Log.d(TAG, "end of getting messages from server");
            }

            if (serverMessages.length>0){
                //il y a au moins un fichiers sur le compte gmail dans le répertoire MySmsBackup
                //dans ce cas on supprime tous les fichiers
                int count = 0;
                for (Message msg:serverMessages) {
                    Log.d(TAG, "deleting existing message on server " + "#" + count);
                    msg.setFlag(Flags.Flag.DELETED, true);
                }
            } else {
                Log.d(TAG, "no message to delete on server");
            }

            ArrayList<AndroidSms> lstAndroidSms = getSmsMessages(SMS_TYPE.SMS_SENT);
            lstAndroidSms.addAll(getSmsMessages(SMS_TYPE.SMS_INBOX));
            final int nbmsg = lstAndroidSms.size();
            Log.d(TAG, "nb sms inbox and sent = " + nbmsg);

            Collections.sort(lstAndroidSms, AndroidSms.compareByThreadId);
            Collections.sort(lstAndroidSms, AndroidSms.compareByDate);
            Collections.sort(lstAndroidSms, AndroidSms.compareByContactName);

            Log.d(TAG, "Will add one email with all SMSes to server");

            msb.close(false);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideWaitDialog();
                    showWaitDialog("Backup to Gmail (snapshot).", "Generating export file.");
                }
            });

            ExportSmsToFileForGmail();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideWaitDialog();
                    showWaitDialog("Backup to Gmail (snapshot).", "Preparing email to send.");
                }
            });

            Message[] messagesToSend = new MimeMessage[1];
            messagesToSend[0] = new MimeMessage(session);
            messagesToSend[0].setFrom(new InternetAddress(currentSelectedEmail));
            messagesToSend[0].setRecipient(Message.RecipientType.TO, new InternetAddress(currentSelectedEmail));
            messagesToSend[0].setSubject("MySmsBackup : List of exported SMS's");
            messagesToSend[0].setText("Here is the last list of exported SMS's");
            //messagesToSend[0]

            Log.d(TAG,"currentselectedemail="+ currentSelectedEmail);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            String filename = getMainExportFilePathForGmail();
            DataSource source = new FileDataSource(filename);
            mimeBodyPart.setDataHandler(new DataHandler(source));
            mimeBodyPart.setFileName(filename);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            messagesToSend[0].setContent(multipart);

            //getMmsMessages();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideWaitDialog();
                    showWaitDialog("Backup to Gmail (snapshot).", "Sending email to : " + currentSelectedEmail + " (to label MySmsBackup).");
                }
            });

            store.getDefaultFolder().getFolder("MySmsBackup").appendMessages(messagesToSend);

            store.close();

            /*runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });*/

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideWaitDialog();
                    showOkDialog("Backup to Gmail OK", "One email with " + nbmsg + " SMS's has been sent to " + currentSelectedEmail + " (to label MySmsBackup).");
                }
            });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnExportToGmail.setEnabled(true);
                }
            });

        } catch (Exception ex) {
            Log.d(TAG, "001:" + ex.toString());

            if (alertDialog != null)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideWaitDialog();
                    }
                });

            final String ex2str = ex.toString();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showOkDialog("Connection Error", "Please check your connection : " + ex2str.toString());
                }
            });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnExportToGmail.setEnabled(true);
                }
            });

        }


        //Log.d(TAG, "end of gmailTest()");

    }

    //Pour exportation depuis le bouton d'export vers fichier
    String mainExportFileName = "mysmsbackup.txt";
    public String getMainExportFilePath() {
        File file = Environment.getExternalStorageDirectory();
        String pathToMntSdCard = file.getPath();
        String pathToLogFile = pathToMntSdCard + "/" + mainExportFileName;
        return pathToLogFile;
    }

    //Pour exportation depuis le bouton d'export vers fichier
    String mainExportFileNameForGmail = "mysmsbackupgm.txt";
    public String getMainExportFilePathForGmail() {
        File file = Environment.getExternalStorageDirectory();
        String pathToMntSdCard = file.getPath();
        String pathToLogFile = pathToMntSdCard + "/" + mainExportFileNameForGmail;
        return pathToLogFile;
    }

    public void logToFile(String str) {
        try {
            File myFile = new File(getMainExportFilePath());
            if (!myFile.exists()) {
                myFile.createNewFile();
            }

            FileWriter fileWriter = new FileWriter(myFile, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(str + "\r\n");
            bufferedWriter.close();
            fileWriter.close();

        } catch (Exception e) {
        }
    }

    // todo:faire le rapprochement avec TextBasedSmsColumns?
    private enum SMS_TYPE {
        SMS_INBOX,
        SMS_SENT,
        SMS_DRAFT
    }

    public ArrayList<AndroidSms> getSmsMessages(SMS_TYPE smsType) {

        ArrayList<AndroidSms> lstAndroidSms = new ArrayList<>();

        try {
            String strSmsType = "";
            if (smsType.equals(SMS_TYPE.SMS_INBOX)) {
                strSmsType = "content://sms/inbox";//"content://sms/inbox";
            } else if (smsType.equals(SMS_TYPE.SMS_SENT)) {
                strSmsType = "content://sms/sent";
            } else if (smsType.equals(SMS_TYPE.SMS_DRAFT)) {
                strSmsType = "content://sms/draft";
            }

            Cursor cursor = getContentResolver().query(Uri.parse(strSmsType), null, null, null, null);
            String colName = "", value = "";

            if (cursor.moveToFirst()) { // must check the result to prevent exception
                do {
                    AndroidSms androidSms = new AndroidSms();
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        colName = cursor.getColumnName(idx);
                        value = cursor.getString(idx);

                        switch (colName) {
                            case "_id":
                                androidSms._id = value;
                                break;
                            case "thread_id":
                                androidSms.thread_id = value;
                                //Log.d(TAG, "thread_id=" + androidSms.thread_id);
                                break;
                            case "address":
                                androidSms.address = value;
                                //Log.d(TAG, "address=" + androidSms.address);
                                String name = getContactNameByPhoneNumber(androidSms.address);
                                //Log.d(TAG, "name=" + name);
                                if (!name.isEmpty())
                                    androidSms.contact_name = name;
                                else
                                    androidSms.contact_name = androidSms.address;
                                break;
                            case "person":
                                androidSms.person = value;
                                break;
                            case "date":
                                if (value != null)
                                    androidSms.date = convertMillisecondsToDate(value);
                                break;
                            case "date_sent":
                                if (value != null)
                                    androidSms.date_sent = convertMillisecondsToDate(value);
                                break;
                            case "protocol":
                                androidSms.protocol = value;
                                break;
                            case "read":
                                androidSms.read = value;
                                break;
                            case "status":
                                androidSms.status = value;
                                break;
                            case "type":
                                androidSms.type = value;
                                //Log.d(TAG, "type=" + androidSms.type);
                                break;
                            case "reply_path_present":
                                androidSms.reply_path_present = value;
                                break;
                            case "subject":
                                androidSms.subject = value;
                                break;
                            case "body":
                                androidSms.body = value;
                                //Log.d(TAG, "body=" + androidSms.body);
                                break;
                            case "service_center":
                                androidSms.service_center = value;
                                break;
                            case "locked":
                                androidSms.locked = value;
                                break;
                            case "error_code":
                                androidSms.error_code = value;
                                break;
                            case "seen":
                                androidSms.seen = value;
                                break;
                            case "deletable":
                                androidSms.deletable = value;
                                break;
                            case "sim_slot":
                                androidSms.sim_slot = value;
                                break;
                            case "sim_imsi":
                                androidSms.sim_imsi = value;
                                break;
                            case "hidden":
                                androidSms.hidden = value;
                                break;
                            case "group_id":
                                androidSms.group_id = value;
                                break;
                            case "group_type":
                                androidSms.group_type = value;
                                break;
                            case "delivery_date":
                                if (value != null)
                                    androidSms.delivery_date = convertMillisecondsToDate(value);
                                break;
                            case "app_id":
                                androidSms.app_id = value;
                                break;
                            case "msg_id":
                                androidSms.msg_id = value;
                                break;
                            case "callback_number":
                                androidSms.callback_number = value;
                                break;
                            case "reserved":
                                androidSms.reserved = value;
                                break;
                            case "pri":
                                androidSms.pri = value;
                                break;
                            case "teleservice_id":
                                androidSms.teleservice_id = value;
                                break;
                            case "link_url":
                                androidSms.link_url = value;
                                break;
                            case "svc_cmd":
                                androidSms.svc_cmd = value;
                                break;
                            case "svc_cmd_content":
                                androidSms.svc_cmd_content = value;
                                break;
                            case "roam_pending":
                                androidSms.roam_pending = value;
                                break;
                            case "spam_report":
                                androidSms.spam_report = value;
                                break;
                            case "m_size":
                                androidSms.m_size = value;
                                break;
                            case "sim_id":
                                androidSms.sim_id = value;
                                break;
                            case "ipmsg_id":
                                androidSms.ipmsg_id = value;
                                break;
                            case "ref_id":
                                androidSms.ref_id = value;
                                break;
                            case "total_len":
                                androidSms.total_len = value;
                                break;
                            case "rec_len":
                                androidSms.rec_len = value;
                                break;
                            default:
                                Log.d(TAG, "field not found : " + colName);
                                break;
                        }
                        //msgData += " " + colName + ":" + value + "\r\n";
                    }
                    // use msgData
                    lstAndroidSms.add(androidSms);
                } while (cursor.moveToNext());
            } else {
                // empty box, no SMS
            }
            cursor.close();
        } catch (Exception ex) {
            Log.d(TAG, "001" + ex.toString());
            ex.printStackTrace();
        }
        return lstAndroidSms;
    }

}
