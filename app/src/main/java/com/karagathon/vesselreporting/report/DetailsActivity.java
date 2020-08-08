package com.karagathon.vesselreporting.report;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.karagathon.vesselreporting.R;

import java.util.Calendar;

public class DetailsActivity extends AppCompatActivity {

    private EditText dateText;
    private DatePickerDialog picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        dateText = findViewById(R.id.date);

        setTitle("Title");

        dateText.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                final Calendar cldr = Calendar.getInstance();
                int day = cldr.get(Calendar.DAY_OF_MONTH);
                int month = cldr.get(Calendar.MONTH);
                int year = cldr.get(Calendar.YEAR);

                picker = new DatePickerDialog(DetailsActivity.this, R.style.Theme_MaterialComponents_Light_Dialog_FixedSize, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                        dateText.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
                    }
                }, year, month, day);
                picker.show();
            }
        });
    }
}