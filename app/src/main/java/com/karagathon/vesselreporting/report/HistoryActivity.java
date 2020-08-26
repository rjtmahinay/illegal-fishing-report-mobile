package com.karagathon.vesselreporting.report;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.model.Report;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private DateFormat dateFormat;
    private FirebaseUser currentUser;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        dataList = new ArrayList<>();
        listView = findViewById(R.id.history_list);

        dateFormat = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        progressBar = findViewById(R.id.history_progressbar);
        progressBar.setVisibility(View.VISIBLE);

        processHistoryByEmail();

        listView.setOnItemClickListener((parent, view, pos, id) -> {
            String strings = parent.getItemAtPosition(pos).toString();
        });
    }

    private void processHistory() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Report");
        dbRef.orderByChild("email").equalTo(userEmail)
                .addValueEventListener(new ValueEventListener() {
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

                        SimpleAdapter adapter = new SimpleAdapter(getApplicationContext(), dataList, R.layout.history_list_items,
                                new String[]{"location", "formattedParsedDate"}, new int[]{R.id.history_list_text_1, R.id.history_list_text_2});
                        listView.setAdapter(adapter);
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
    private void collectData(Report report) {
        if (Objects.isNull(report)) return;
        String formattedParsedDate;

        String id = report.getId();
        String location = report.getLocation();
        Date formattedDate = report.getDate();
        LocalDate localDate = convertToLocalDateViaMilisecond(formattedDate);

        String format = "%d-0%d-%d";
        if (Objects.nonNull(localDate) && localDate.getMonth().getValue() < 10) {
            try {
                formattedDate = dateFormat.parse(String.format(Locale.getDefault(),
                        format, localDate.getDayOfMonth(), localDate.getMonth().getValue(), localDate.getYear()));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        Map<String, String> historyMap = new HashMap<>();

        if (Objects.nonNull(formattedDate)
                && Objects.nonNull(location) && !location.isEmpty()) {

            formattedParsedDate = dateFormat.format(formattedDate);

            historyMap.put("id", id);
            historyMap.put("location", location);
            historyMap.put("formattedParsedDate", formattedParsedDate);
            dataList.add(historyMap);
        }
    }

    private void processHistoryByEmail() {
        currentUser.getProviderData().forEach(u -> {
            switch (u.getProviderId()) {
                case "google.com":
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                    userEmail = account.getEmail();
                    processHistory();
                    break;
                case "facebook.com":
                    processHistoryForFacebook();
                    break;
                case "password":
                    userEmail = currentUser.getEmail();
                    processHistory();
            }
        });
    }

    private void processHistoryForFacebook() {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), (object, response) -> {
            JSONObject json = response.getJSONObject();
            try {
                if (Objects.nonNull(json)) {
                    userEmail = object.getString("email");
                    processHistory();
                }

            } catch (JSONException e) {
                Log.e("Request Data JSON Exception", e.getMessage());
            }
        });
        Bundle parameters = new Bundle();
        parameters.putString("fields", "email");
        request.setParameters(parameters);
        request.executeAsync();
    }

    private LocalDate convertToLocalDateViaMilisecond(Date dateToConvert) {
        return Instant.ofEpochMilli(dateToConvert.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}