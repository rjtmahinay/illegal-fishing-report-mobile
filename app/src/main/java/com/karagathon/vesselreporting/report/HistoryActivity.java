package com.karagathon.vesselreporting.report;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.helper.DatabaseHelper;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        listView = findViewById(R.id.history_list);

        DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
        List<String> reports = databaseHelper.getLocations();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reports);
        listView.setAdapter(adapter);

//        getActionBar().setDisplayHomeAsUpEnabled(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String strings = parent.getItemAtPosition(pos).toString();
                Log.i("Item", strings);
            }
        });
    }
}