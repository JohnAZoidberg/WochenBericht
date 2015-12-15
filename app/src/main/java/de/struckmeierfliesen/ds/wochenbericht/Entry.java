package de.struckmeierfliesen.ds.wochenbericht;

import android.text.format.DateFormat;

import java.util.Date;

public class Entry {
    public int id = -1;
    public String client;
    public Date date;
    public int duration;
    public int installerId;
    public String work;
    public String installer = null;
    private String picturePath = null;

    public Entry(String client, Date date, int duration, int installerId, String work) {
        this.client = client;
        this.date = date;
        this.duration = duration;
        this.installerId = installerId;
        this.work = work;
    }

    public String getClient() {
        return client;
    }

    public String getDate() {
        return DateFormat.format("dd.MM.yy", date).toString();
    }

    public String getDuration() {
        return Util.convertDuration(duration);
    }

    public String getDuration(String divider) {
        return Util.convertDuration(duration, divider);
    }

    public String getInstaller() {
        return installer;
    }

    public String getWork() {
        return work;
    }

    @Override
    public String toString() {
        String installer = installerId + "";
        if (this.installer != null) installer = this.installer;
        return "\"" + work + "\" ( " + Util.formatDate(date) + ")" +
                "bei " + client + " mit " + getInstaller() + "(" + installer + ")";
    }

    public String getPicturePath() {
        return picturePath;
    }

    public void setPicturePath(String picturePath) {
        this.picturePath = picturePath;
    }
}
