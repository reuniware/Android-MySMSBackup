package com.reuniware.apps.mysmsbackup2;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Utilisateur001 on 28/11/2015.
 */
public class AndroidSms {
    String TAG = "AndroidSms class";

    public String _id;
    public String thread_id;
    public String address;
    public String person;
    public String date;
    public String date_sent;
    public String protocol;
    public String read;
    public String status;
    public String type;
    public String reply_path_present;
    public String subject;
    public String body;
    public String service_center;
    public String locked;
    public String error_code;
    public String seen;
    public String deletable;
    public String sim_slot;
    public String sim_imsi;
    public String hidden;
    public String group_id;
    public String group_type;
    public String delivery_date;
    public String app_id;
    public String msg_id;
    public String callback_number;
    public String reserved;
    public String pri;
    public String teleservice_id;
    public String link_url;
    public String svc_cmd;
    public String svc_cmd_content;
    public String roam_pending;
    public String spam_report;
    public String contact_name;
    public String m_size;
    public String sim_id;
    public String ipmsg_id;
    public String ref_id;
    public String total_len;
    public String rec_len;


    public static Comparator<AndroidSms> compareByThreadId = new Comparator<AndroidSms>() {
        @Override
        public int compare(AndroidSms lhs, AndroidSms rhs) {
            int idSms1 = Integer.parseInt(lhs.thread_id);
            int idSms2 = Integer.parseInt(rhs.thread_id);
            if (idSms1 > idSms2) return -1;
            else if (idSms1 == idSms2) return 0;
            else return 1;
        }
    };

    public static Comparator<AndroidSms> compareByDate = new Comparator<AndroidSms>() {
        @Override
        public int compare(AndroidSms lhs, AndroidSms rhs) {
            Date date1 = null;
            Date date2 = null;
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            try {
                date1 = simpleDateFormat.parse(lhs.date);
                date2 = simpleDateFormat.parse(rhs.date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (date1.after(date2)) return -1;
            if (date1.equals(date2)) return 0;
            if (date1.before(date2)) return 1;

            return 0;
        }
    };

    public static Comparator<AndroidSms> compareByContactName = new Comparator<AndroidSms>() {
        @Override
        public int compare(AndroidSms lhs, AndroidSms rhs) {
            return (lhs.contact_name.compareTo(rhs.contact_name));
        }
    };

}
