package com.karagathon.vesselreporting.report;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.storage.s3.AWSS3StoragePlugin;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karagathon.vesselreporting.BuildConfig;
import com.karagathon.vesselreporting.R;
import com.karagathon.vesselreporting.adapter.PlacesAutoCompletedAdapter;
import com.karagathon.vesselreporting.model.Report;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.CloseableHttpResponse;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.CloseableHttpClient;
import cz.msebera.android.httpclient.impl.client.HttpClients;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

public class DetailsActivity extends AppCompatActivity {

    private static final String UPLOAD_URL = "http://192.168.0.109:1331/upload";
    public static final String TEXT_MESSAGE_API_URL = "https://api.semaphore.co/api/v4/messages";
    public static final int LOCATION_REQ_CODE = 3;
    private EditText dateText, locationText, reportDescription;
    private Button submitButton;
    private DatePickerDialog picker;
    private Intent reportIntent;
    private HashMap<String, Object> dataMap;
    private File singleMediaFile;
    private boolean isGallery;
    private TextView nameView;
    private FirebaseAuth auth;
    private PlacesAutoCompletedAdapter adapter;
    private AutoCompleteTextView autoCompleteTextView;
    private Date date;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isLastLocationNull;
    private ProgressBar detailsProgressBar;

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
        locationText = findViewById(R.id.location);
        reportDescription = findViewById(R.id.reportDescription);
        nameView = findViewById(R.id.name);
        detailsProgressBar = findViewById(R.id.detailsProgressBar);
        detailsProgressBar.setVisibility(View.GONE);

        dataMap = new HashMap<>();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        auth = FirebaseAuth.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        nameView.setText(currentUser.getDisplayName());

        dateText.setOnClickListener(view -> {
            final Calendar cldr = Calendar.getInstance();
            int day = cldr.get(Calendar.DAY_OF_MONTH);
            int month = cldr.get(Calendar.MONTH);
            int year = cldr.get(Calendar.YEAR);

            picker = new DatePickerDialog(DetailsActivity.this, R.style.Theme_MaterialComponents_Light_Dialog_FixedSize,
                    (datePicker, year1, month1, dayOfMonth)
                            -> {
                        dateText.setText(String.format("%d-%d-%d", year1, month1 + 1, dayOfMonth));
                        date = cldr.getTime();
                    }, year, month, day);
            picker.show();
        });


        Places.initialize(getApplicationContext(), BuildConfig.GOOGLE_API_KEY);

        autoCompleteTextView = findViewById(R.id.location);
        autoCompleteTextView.setThreshold(3);

        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(4.2158064, 114.0952145),
                new LatLng(21.3217806, 126.8072562));

        adapter = new PlacesAutoCompletedAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, bounds);

        autoCompleteTextView.setAdapter(adapter);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean isHideNameEnabled = sharedPreferences.getBoolean("hide_name", false);

        if (isHideNameEnabled) {
            nameView.setVisibility(View.GONE);
        }

//        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
//                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
//
//        // Specify the types of place data to return.
//        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME));
//
//        // Set up a PlaceSelectionListener to handle the response.
//        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
//            @Override
//            public void onPlaceSelected(Place place) {
//                // TODO: Get info about the selected place.
//                Log.i("Location", "Place: " + place.getName() + ", " + place.getId());
//            }
//
//
//            @Override
//            public void onError(Status status) {
//                // TODO: Handle the error.
//                Log.i("Location", "An error occurred: " + status);
//            }
//        });
//        locationText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {
//                if (locationText.length() < 1) {
//                    locationText.setError("Location is required");
//                }
//            }
//        });


        submitButton.setOnClickListener(view -> {

            reportIntent = getIntent();
            isGallery = reportIntent.getBooleanExtra("isGallery", false);

            dataMap.put("name", currentUser.getDisplayName());
            dataMap.put("location", autoCompleteTextView.getEditableText().toString());
            dataMap.put("date", dateText.getText().toString());
            dataMap.put("description", reportDescription.getText().toString());
            detailsProgressBar.setVisibility(View.VISIBLE);
            getLocation();

            if (isGallery) {
                String[] galleryDataPaths = reportIntent.getStringArrayExtra("galleryDataPaths");

                processGalleryData(galleryDataPaths);
                uploadData(dataMap);

            } else {
                String absoluteFilePath = reportIntent.getStringExtra("absoluteFilePath");

                Log.i("Details Absolute File Path", absoluteFilePath);

                singleMediaFile = new File(absoluteFilePath);
                dataMap.put("files", Arrays.asList(singleMediaFile.getName()));

                uploadData(dataMap);
            }

            //send sms
            Log.i("Sending a Message Now!", "Sending a Message Now!");
//           new SendTextMessage().execute();


            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference().child("Report");


            String id = dbRef.push().getKey();
            Report report
                    = new Report(id, nameView.getText().toString(), locationText.getText().toString(),
                    reportDescription.getText().toString(), date);

            Log.i("Details Date", String.valueOf(date));
//            dbRef.child(id).setValue(report);
//            DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
//            databaseHelper.add(report);


        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i("Inside Permission Result", "Inside Permission Result");
        Log.i("Grant Result", String.valueOf(grantResults[0]));
        if (requestCode == LOCATION_REQ_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Log.i("Inside Permission Condition", "Inside Permission Condition");
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
        Log.i("On Destroy", "On Destroy");
//        File f = new File(absoluteFilePath);
//        f.delete();

        if (!isGallery) {
            if (Objects.nonNull(singleMediaFile)) {
                singleMediaFile.delete();
            }
        }
        reportIntent = null;
        dataMap = null;

        try {
            Amplify.removePlugin(new AWSCognitoAuthPlugin());
            Amplify.removePlugin(new AWSS3StoragePlugin());
        } catch (AmplifyException e) {
            e.printStackTrace();
        }

    }

    private void uploadData(Map data) {

        Request request = new JsonObjectRequest(Request.Method.POST, UPLOAD_URL, new JSONObject(data),
                response -> {
                    //empty
                    detailsProgressBar.setVisibility(View.GONE);
                    goToReport();
                }, error -> {
            Log.e("Request Error", String.valueOf(error));
            detailsProgressBar.setVisibility(View.GONE);
        });

        RequestQueue rQueue = Volley.newRequestQueue(getApplicationContext());
        rQueue.add(request);

    }

    private void processGalleryData(String[] galleryDataPaths) {
        File file;

        if (Objects.isNull(galleryDataPaths)) {
            String gallerySingleDataPath
                    = reportIntent.getStringExtra("gallerySingleDataPath");
            file = new File(gallerySingleDataPath);

            dataMap.put("files", Arrays.asList(file.getName()));
//            storeFilesInObjectStorage(file.getName(), file);
            return;
        }

        List<String> fileNames = new ArrayList<>(galleryDataPaths.length);
        for (String galleryPaths : galleryDataPaths) {
            Log.i("Gallery Details Data", galleryPaths);
            file = new File(galleryPaths);
            fileNames.add(file.getName());
//            storeFilesInObjectStorage(file.getName(), file);
        }

        dataMap.put("files", fileNames);
    }

    private void storeFilesInObjectStorage(String fileName, File file) {
        Amplify.Storage.uploadFile(fileName, file, result ->
                        Log.i("MyAmplifyApp", "Successfully uploaded: " + result.getKey()),
                storageFailure -> Log.e("MyAmplifyApp", "Upload failed", storageFailure));
    }


    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQ_CODE);
        }
        Log.i("Get Location", "Get Location");
        fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
            try {
                Location location = task.getResult();
                Log.i("location", String.valueOf(location));


                if (Objects.nonNull(location)) {
                    isLastLocationNull = true;
                    Geocoder geocoder = new Geocoder(DetailsActivity.this, Locale.getDefault());

                    List<Address> address = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

                    dataMap.put("latitude", address.get(0).getLatitude());
                    dataMap.put("longitude", address.get(0).getLongitude());

                    Log.i("Details Activity Latitude", String.valueOf(address.get(0).getLatitude()));
                    Log.i("Details Activity Longitude", String.valueOf(address.get(0).getLongitude()));

                } else {
//                    Uri gmmIntentUri = Uri.parse("geo:12.8797, 121.7740");
//
//                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
//                    startActivity(mapIntent);

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
                        }
                    };
                    Log.i("CALL", "Request Location");
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
        startActivity(reportIntent);
    }

    private class SendTextMessage extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            sendSMS();
            return null;
        }

        private void sendSMS() {
            HttpPost post = new HttpPost(TEXT_MESSAGE_API_URL);

            String message = "Hi from Android";
            // add request parameter, form parameters
            List<NameValuePair> urlParameters = new ArrayList<>();
            urlParameters.add(new BasicNameValuePair("apikey", BuildConfig.SEMAPHORE_API_KEY));
            urlParameters.add(new BasicNameValuePair("number", "09272697150"));
            urlParameters.add(new BasicNameValuePair("message", message));

            try {
                post.setEntity(new UrlEncodedFormEntity(urlParameters));

                CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(post);
                Log.i("Send SMS", String.valueOf(response));

            } catch (UnsupportedEncodingException ue) {
                Log.e("Unsupported Exception", ue.getLocalizedMessage());
            } catch (IOException ie) {
                Log.e("IOException", ie.getLocalizedMessage());
            }
        }
    }

}