package com.karagathon.vesselreporting.report;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karagathon.vesselreporting.R;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HistoryActivity extends AppCompatActivity {

    private ListView listView;
    private List<String> reportsLocation;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        reportsLocation = new LinkedList<>();
        listView = findViewById(R.id.history_list);
        progressBar = findViewById(R.id.history_progressbar);

//        DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
//        List<String> reports = databaseHelper.getLocations();
        progressBar.setVisibility(View.VISIBLE);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Report");
        dbRef.orderByKey().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Report report = snapshot.getValue(Report.class);
                collectLocations((Map<String, Object>) snapshot.getValue());
                Log.i("Reports Location", reportsLocation.toString());

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, reportsLocation);
                listView.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

//        getActionBar().setDisplayHomeAsUpEnabled(true);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                String strings = parent.getItemAtPosition(pos).toString();
                Log.i("Item", strings);
            }
        });
    }

    private void collectLocations(Map<String, Object> reports) {
        for (Map.Entry<String, Object> entry : reports.entrySet()) {

            Map singleReport = (Map) entry.getValue();
            String location = (String) singleReport.get("location");
            if (Objects.nonNull(location) && !location.isEmpty()) {
                reportsLocation.add(location);
            }

        }

    }
}