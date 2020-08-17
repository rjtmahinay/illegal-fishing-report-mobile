package com.karagathon.vesselreporting.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.karagathon.vesselreporting.model.Report;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String REPORT_TABLE = "REPORT_TABLE";
    public static final String NAME = "NAME";
    public static final String LOCATION = "LOCATION";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String SUBMISSION_DATE = "SUBMISSION_DATE";
    private static final String REPORT_DB = "report.db";
    private DateTimeFormatter df;

    public DatabaseHelper(@Nullable Context context) {
        super(context, REPORT_DB, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createStatement
                = "CREATE TABLE " + REPORT_TABLE + "(ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                NAME + " VARCHAR(255), " + LOCATION + " VARCHAR(255), " + DESCRIPTION + " VARCHAR(255), " + SUBMISSION_DATE + " DATE)";

        db.execSQL(createStatement);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public boolean add(Report report) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        cv.put(NAME, report.getName());
        cv.put(LOCATION, report.getLocation());
        cv.put(DESCRIPTION, report.getDescription());
        cv.put(SUBMISSION_DATE, df.format(report.getSubmissionDate()));

        long insert = db.insert(REPORT_TABLE, null, cv);

        return (insert != -1);
    }

    public List<Report> getReports() {
        List<Report> reports = new ArrayList<>();

        String retrieveAllQuery = "SELECT * FROM " + REPORT_TABLE;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(retrieveAllQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(1);
                String location = cursor.getString(2);
                String description = cursor.getString(3);
                String date = cursor.getString(4);

                Report report = new Report(name, location, description, LocalDate.parse(date, df));
                reports.add(report);
            } while (cursor.moveToFirst());
        }

        cursor.close();
        db.close();
        return reports;
    }

    public List<String> getLocations() {
        List<String> reports = new ArrayList<>();

        String locationQuery = "SELECT " + LOCATION + " FROM " + REPORT_TABLE;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(locationQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String location = cursor.getString(0);

                reports.add(location);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return reports;
    }
}
