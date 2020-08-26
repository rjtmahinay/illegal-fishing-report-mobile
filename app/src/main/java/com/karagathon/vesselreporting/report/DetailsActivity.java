package com.karagathon.vesselreporting.report;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.amazonaws.util.StringUtils;
import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karagathon.vesselreporting.BuildConfig;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.adapter.PlacesAutoCompletedAdapter;
import com.karagathon.vesselreporting.model.Report;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class DetailsActivity extends AppCompatActivity {

    private static final String UPLOAD_URL = "http://192.168.0.109:1331/upload";
    private static final int LOCATION_REQ_CODE = 3;
    private TextInputLayout reportDescriptionInput, locationTextInput;
    private Button submitButton;
    private Intent reportIntent;
    private HashMap<String, Object> dataMap;
    private File singleMediaFile;
    private TextView nameView, dateText;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private PlacesAutoCompletedAdapter adapter;
    private AutoCompleteTextView locationText;
    private Date date;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private ProgressBar detailsProgressBar;
    private boolean isLastLocationNull;
    private boolean isGallery;
    private DateFormat dateFormat;
    private String userEmail;
    private String location;
    private String reportDescription;
    private int storeFileToCloudFail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        if (Objects.isNull(savedInstanceState)) {
            try {
                Amplify.addPlugin(new AWSCognitoAuthPlugin());
                Amplify.addPlugin(new AWSS3StoragePlugin());
                Amplify.configure(getApplicationContext());

                Log.i("MyAmplifyApp", "Initialized Amplify");
            } catch (AmplifyException e) {
                e.printStackTrace();
            }
        }

        dateText = findViewById(R.id.date);
        submitButton = findViewById(R.id.submit);
        locationTextInput = findViewById(R.id.locationInput);
        reportDescriptionInput = findViewById(R.id.reportDescription);
        nameView = findViewById(R.id.name);
        detailsProgressBar = findViewById(R.id.detailsProgressBar);
        detailsProgressBar.setVisibility(View.GONE);

        dataMap = new HashMap<>();
        dateFormat = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        auth = FirebaseAuth.getInstance();

        currentUser = auth.getCurrentUser();
        populateName();

        populateDate();

        Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_API_KEY);

        locationText = findViewById(R.id.location);
        locationText.setThreshold(3);

        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(4.588889, 116.65),
                new LatLng(21.113056, 126.604444));

        adapter = new PlacesAutoCompletedAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, bounds);

        locationText.setAdapter(adapter);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isHideNameEnabled = sharedPreferences.getBoolean("hide_name", false);

        if (isHideNameEnabled) {
            nameView.setVisibility(View.GONE);
        }

        submitButton.setOnClickListener(view -> {

            reportIntent = getIntent();
            isGallery = reportIntent.getBooleanExtra("isGallery", false);

            location = locationText.getText().toString();
            reportDescription = reportDescriptionInput.getEditText().getText().toString();

            if (!isSuccessValidation(location, reportDescription)) return;

            dataMap.put("name", nameView.getText().toString());
            dataMap.put("location", location);
            dataMap.put("date", dateText.getText().toString());
            dataMap.put("description", reportDescription);
            detailsProgressBar.setVisibility(View.VISIBLE);

            if (isGallery) {
                String[] galleryDataPaths = reportIntent.getStringArrayExtra("galleryDataPaths");

                processGalleryData(galleryDataPaths);

            } else {
                String absoluteFilePath = reportIntent.getStringExtra("absoluteFilePath");

                singleMediaFile = new File(absoluteFilePath);
                dataMap.put("files", Arrays.asList(singleMediaFile.getName()));
                storeFilesInObjectStorage(absoluteFilePath, singleMediaFile);
            }

            uploadDataAndLocation();

        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == LOCATION_REQ_CODE && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            showLocationDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Objects.nonNull(locationCallback) && !isLastLocationNull) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!isGallery && Objects.nonNull(singleMediaFile)) {
            singleMediaFile.delete();
        }

        reportIntent = null;
        dataMap = null;

    }

    private void uploadData(Map<String, Object> data) {

        if (storeFileToCloudFail == 1) {
            storeFileToCloudFail = 0;
            genericErrorDialog();
            return;
        }

        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        Request request = new JsonObjectRequest(Request.Method.POST, UPLOAD_URL, new JSONObject(data),
                response -> {
                    Log.i("Response", "Response");
                    //empty
                }, error -> {
            //empty
            for (StackTraceElement elem : error.getStackTrace()) {
                Log.e("Error", String.valueOf(elem));
            }
        });

        rQueue.add(request);

        retrieveEmail();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Report");

        String id = dbRef.push().getKey();
        Report report
                = new Report(id, nameView.getText().toString(), location,
                reportDescription, userEmail, date);

        dbRef.child(id).setValue(report);

        detailsProgressBar.setVisibility(View.GONE);

        goToReport();
    }

    private void genericErrorDialog() {
        detailsProgressBar.setVisibility(View.GONE);
        AlertDialog alertDialog = new AlertDialog.Builder(DetailsActivity.this).create();
        alertDialog.setTitle("Error");
        alertDialog.setMessage("Please try again later!");
        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Ok", ((dialogInterface, i) -> {
            //
        }));
        alertDialog.show();
    }

    private void processGalleryData(String[] galleryDataPaths) {
        File file;

        if (Objects.isNull(galleryDataPaths)) {
            String gallerySingleDataPath
                    = reportIntent.getStringExtra("gallerySingleDataPath");
            file = new File(gallerySingleDataPath);

            dataMap.put("files", Arrays.asList(file.getName()));
            storeFilesInObjectStorage(file.getName(), file);
            return;
        }

        List<String> fileNames = new ArrayList<>(galleryDataPaths.length);
        for (String galleryPaths : galleryDataPaths) {
            file = new File(galleryPaths);
            fileNames.add(file.getName());
            storeFilesInObjectStorage(file.getName(), file);
        }

        dataMap.put("files", fileNames);
    }

    private void storeFilesInObjectStorage(String fileName, File file) {
        Amplify.Storage.uploadFile(fileName, file, result ->
                        Log.i("MyAmplifyApp", "Successfully uploaded: " + result.getKey()),
                storageFailure -> {
                    Log.e("MyAmplifyApp", "Upload failed", storageFailure);
                    storeFileToCloudFail = 1;
                });
    }


    private void uploadDataAndLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQ_CODE);
        }
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
            try {
                Location location = task.getResult();

                if (Objects.nonNull(location)) {
                    isLastLocationNull = true;
                    Geocoder geocoder = new Geocoder(DetailsActivity.this, Locale.getDefault());

                    List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    dataMap.put("latitude", address.get(0).getLatitude());
                    dataMap.put("longitude", address.get(0).getLongitude());

                    Log.i("Details Activity Latitude", String.valueOf(address.get(0).getLatitude()));
                    Log.i("Details Activity Longitude", String.valueOf(address.get(0).getLongitude()));

                    uploadData(dataMap);

                } else {

                    locationRequest = LocationRequest.create();
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationRequest.setInterval(5000);
                    locationCallback = new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            if (Objects.isNull(locationResult)) {
                                Log.i("Details Location Result Null", String.valueOf(locationResult));
                            }

                            for (Location location : locationResult.getLocations()) {
                                if (Objects.nonNull(location)) {
                                    Log.i("Details Location Result Not Null", String.valueOf(location));

                                    Log.i("Details Activity Request Latitude", String.valueOf(location.getLatitude()));
                                    Log.i("Details Activity Request Longitude", String.valueOf(location.getLongitude()));
                                    Log.i("Details Activity Request Accuracy", String.valueOf(location.getAccuracy()));

                                    dataMap.put("latitude", location.getLatitude());
                                    dataMap.put("longitude", location.getLongitude());
                                    break;
                                }
                            }

                            uploadData(dataMap);
                        }
                    };
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
                }
            } catch (Exception e) {
                Log.e("Error", e.getLocalizedMessage());
            }

        });
    }


    private void turnGPSOn() {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        sendBroadcast(intent);
    }

    private void showLocationDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(DetailsActivity.this).create();
        alertDialog.setTitle("Permission Denied");
        alertDialog.setMessage("Without this permission, we cannot get an actual coordinates of the report");
        alertDialog.show();
    }

    private void goToReport() {
        Intent reportIntent = new Intent(getApplicationContext(), ReportActivity.class);
        reportIntent.putExtra("REPORT_SUCCESS_SUBMISSION", true);
        startActivity(reportIntent);
    }

    private void populateDate() {
        final Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        String format = "%d-%d-%d";
        month += 1;
        if (month < 10) {
            format = "%d-0%d-%d";
        }
        dateText.setText(String.format(Locale.getDefault(), format, day, month, year));
        try {
            date = dateFormat.parse(dateText.getText().toString());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void populateName() {
        if (!StringUtils.isBlank(currentUser.getDisplayName())) {
            nameView.setText(currentUser.getDisplayName());
            return;
        }
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("User");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                snapshot.getChildren().forEach(dataSnapshot -> {
                    Map<String, Object> userMap = (Map<String, Object>) dataSnapshot.getValue();
                    Map.Entry<String, Object> entry = userMap.entrySet().iterator().next();

                    nameView.setText(String.valueOf(entry.getValue()));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Database Error", error.getMessage());
            }
        });
    }

    private void retrieveEmail() {
        currentUser.getProviderData().forEach(u -> {

            switch (u.getProviderId()) {
                case "google.com":
                    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
                    userEmail = account.getEmail();
                    break;
                case "facebook.com":
                    requestData();
                    break;
                case "password":
                    userEmail = currentUser.getEmail();
            }
        });
    }

    private void requestData() {
        GraphRequest request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken(), (object, response) -> {
            JSONObject json = response.getJSONObject();
            try {
                if (Objects.nonNull(json)) {
                    String email = object.getString("email");
                    userEmail = email;
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

    private boolean isSuccessValidation(String location, String reportDescription) {
        boolean result = true;

        if (Objects.isNull(location) || location.isEmpty()) {
            locationTextInput.setError("Location must not be blank");
            result = false;
        } else {
            locationTextInput.setError(null);
            locationTextInput.setErrorEnabled(false);
        }

        if (Objects.isNull(reportDescription) || reportDescription.isEmpty()) {
            reportDescriptionInput.setError("Description must not be blank");
            result = false;
        } else {
            reportDescriptionInput.setError(null);
            reportDescriptionInput.setErrorEnabled(false);
        }

        return result;
    }

}