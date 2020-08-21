package com.karagathon.vesselreporting.report;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.model.Report;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HistoryActivity extends AppCompatActivity {

    private ListView listView;
    private ProgressBar progressBar;
    private List<Map<String, String>> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        dataList = new ArrayList<>();
        listView = findViewById(R.id.history_list);
        progressBar = findViewById(R.id.history_progressbar);

        progressBar.setVisibility(View.VISIBLE);
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Report");
        dbRef.orderByKey().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                snapshot.getChildren().forEach(dataSnapshot -> {
                    Report report = dataSnapshot.getValue(Report.class);
                    collectData(report);
                });

                if (dataList.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                Comparator<Map<String, String>> comparator
                        = (map1, map2) -> map1.get("id").compareTo(map2.get("id"));

                dataList.sort(comparator.reversed());
                Log.i("Data List", dataList.toString());

                SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), dataList, R.layout.history_list_items,
                        new String[]{"location", "formattedParsedDate"}, new int[]{R.id.history_list_text_1, R.id.history_list_text_2});
                listView.setAdapter(adapter);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        listView.setOnItemClickListener((parent, view, pos, id) -> {
            String strings = parent.getItemAtPosition(pos).toString();
            Log.i("Item", strings);
        });
    }

    private void collectData(Report report) {
        if (Objects.isNull(report)) return;
        Date date;
        String formattedParsedDate = "";
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String id = report.getId();
        String location = report.getLocation();
        String formattedDate = report.getFormattedDate();

        Map<String, String> historyMap = new HashMap<>();

        if (Objects.nonNull(formattedDate)
                && Objects.nonNull(location) && !location.isEmpty()) {

            try {
                date = dateFormat.parse(formattedDate);
                formattedParsedDate = dateFormat.format(date);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Log.i("Parse Date", formattedParsedDate);
            Log.i("Location", location);

            historyMap.put("id", id);
            historyMap.put("location", location);
            historyMap.put("formattedParsedDate", formattedParsedDate);
            dataList.add(historyMap);
        }
    }
}