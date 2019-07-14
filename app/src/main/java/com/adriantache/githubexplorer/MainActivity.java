package com.adriantache.githubexplorer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String GIT_HUB_API = "api.github.com";
    private static final String USERS_ENDPOINT = "users";
    private static final String USER_ENDPOINT = "user";
    private static final String REPOS_ENDPOINT = "repos";
    private static final String AUTHORIZATIONS_ENDPOINT = "authorizations";
    private static final String ERROR = "ERROR";
    private static final String ERROR_2FA = "ERROR_2FA";
    private static final String ERROR_RETRIES = "ERROR_RETRIES";
    private static final String TAG = "MainActivity";
    private EditText twoFA;
    private String username;
    private String authHeader;
    private boolean twoFactorRequested = false;
    private String twoFactorToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        twoFA = findViewById(R.id.twoFA);
        final EditText usernameText = findViewById(R.id.username);
        final EditText password = findViewById(R.id.password);
        final ImageView checkmark = findViewById(R.id.checkmark);

        //show checkmark if 2FA token is saved
        usernameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //not needed
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //check whether we have a 2FA token saved for this user and retrieve it
                String user = charSequence.toString().toLowerCase();

                SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
                if (sharedPreferences.contains(user)) {
                    checkmark.setVisibility(View.VISIBLE);
                    password.setVisibility(View.GONE);
                    twoFactorToken = sharedPreferences.getString(user, ERROR);
                } else {
                    checkmark.setVisibility(View.INVISIBLE);
                    password.setVisibility(View.VISIBLE);
                    twoFactorToken = null;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //not needed
            }
        });

        Button login = findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user = usernameText.getText().toString().toLowerCase();
                String pass = password.getText().toString();
                String twoFactorAuth = twoFA.getText().toString();

                //if we have a 2FA token saved use that, otherwise use username or password
                if (!TextUtils.isEmpty(twoFactorToken) && !twoFactorToken.equals(ERROR)) {
                    Toast.makeText(MainActivity.this, "Using saved 2FA token.", Toast.LENGTH_SHORT).show();
                    logIn(user, user, null);
                } else {
                    logIn(user, pass, twoFactorAuth);
                }
            }
        });
    }

    //todo [FEATURE] add username detection if login via email address
    private void logIn(String user, String pass, String twoFA) {
        if (TextUtils.isEmpty(user)) {
            Toast.makeText(this, "No user specified!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            username = user;
        }

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https");

        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "No password inserted, only showing public repos!", Toast.LENGTH_SHORT).show();

            builder.authority(GIT_HUB_API)
                    .appendPath(USERS_ENDPOINT)
                    .appendPath(user)
                    .appendPath(REPOS_ENDPOINT);

            new NetworkWork().execute(builder.build().toString());
        } else if (twoFactorRequested) {
            //if we detected login failed due to lack of 2FA, request an authorization from the API
            String base = Base64.encodeToString((user + ":" + pass).getBytes(), Base64.NO_WRAP);
            authHeader = "Basic " + base;

            builder.authority(GIT_HUB_API)
                    .appendPath(AUTHORIZATIONS_ENDPOINT);

            new NetworkWork().execute(builder.build().toString(), authHeader, twoFA);
        } else {
            if (TextUtils.isEmpty(twoFactorToken)) {
                String base = Base64.encodeToString((user + ":" + pass).getBytes(), Base64.NO_WRAP);
                authHeader = "Basic " + base;
            } else {
                authHeader = "token " + twoFactorToken;
            }

            builder.authority(GIT_HUB_API)
                    .appendPath(USER_ENDPOINT)
                    .appendPath(REPOS_ENDPOINT);

            if (TextUtils.isEmpty(twoFA) || !TextUtils.isEmpty(twoFactorToken)) {
                new NetworkWork().execute(builder.build().toString(), authHeader);
            } else {
                new NetworkWork().execute(builder.build().toString(), authHeader, twoFA);
            }
        }
    }

    class NetworkWork extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            if (url == null || url[0] == null) return ERROR;

            Log.i(TAG, "Fetching " + url[0]);

            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder().url(url[0]);
            if (url.length > 1) builder.addHeader("Authorization", url[1]);
            if (url.length > 2) {
                builder.addHeader("x-github-otp", url[2]);

                if (twoFactorRequested) {
                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    HashMap<String, String> map = new HashMap<>();
                    map.put("note", "GitHubExplorer");
                    JSONObject json = new JSONObject(map);

                    RequestBody body = RequestBody.create(json.toString(), JSON);
                    builder.post(body);
                }
            }

            try (Response response = client.newCall(builder.build()).execute()) {
                //can only access response once
                String responseString = response.body().string();

                if (response.code() == 401 && responseString.contains("Must specify two-factor authentication OTP code.")) {
                    return ERROR_2FA;
                } else if (response.code() == 403) {
                    return ERROR_RETRIES;
                } else if (!response.isSuccessful()) {
                    return ERROR;
                }

                return responseString;
            } catch (IOException e) {
                e.printStackTrace();
                return ERROR;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (TextUtils.isEmpty(s))
                Toast.makeText(MainActivity.this, "Empty response!", Toast.LENGTH_SHORT).show();

            switch (s) {
                case ERROR_2FA:
                    Toast.makeText(MainActivity.this, "Two-factor authentication is active, please enter code.", Toast.LENGTH_SHORT).show();
                    twoFA.setVisibility(View.VISIBLE);

                    //todo refactor authorization fetching flow
                    twoFactorRequested = true;
                    break;
                case ERROR_RETRIES:
                    Toast.makeText(MainActivity.this, "Maximum number of login attempts exceeded. Please try again later.", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    Toast.makeText(MainActivity.this, "Cannot fetch data from GitHub! Bad credentials?", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    if (twoFactorRequested) {
                        saveTwoFactorToken(s);
                    } else {
                        decodeJson(s);
                    }
                    break;
            }
        }

        private void decodeJson(String s) {
            if (TextUtils.isEmpty(s))
                Toast.makeText(MainActivity.this, "Empty response!", Toast.LENGTH_SHORT).show();

            ArrayList<String> repos = new ArrayList<>();

            try {
                JSONArray jsonRoot = new JSONArray(s);
                for (int i = 0; i < jsonRoot.length(); i++) {
                    JSONObject child = jsonRoot.optJSONObject(i);
                    String repoName = child.optString("name");
                    if (!TextUtils.isEmpty(repoName)) repos.add(repoName);
                }

                if (!repos.isEmpty()) {
                    Intent intent = new Intent(MainActivity.this, RepoList.class);
                    intent.putExtra("username", username);
                    intent.putExtra("authorization", authHeader);
                    intent.putStringArrayListExtra("repos", repos);
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Cannot extract repos from JSON!", Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Cannot decode JSON!", Toast.LENGTH_SHORT).show();
            }
        }

        private void saveTwoFactorToken(String s) {
            try {
                JSONObject jsonRoot = new JSONObject(s);
                String accessToken = jsonRoot.optString("token");

                if (!TextUtils.isEmpty(accessToken)) {
                    SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
                    SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
                    sharedPreferencesEditor.putString(username, accessToken);
                    sharedPreferencesEditor.apply();

                    twoFactorToken = accessToken;

                    twoFactorRequested = false;
                }

            } catch (JSONException e) {
                Toast.makeText(MainActivity.this, "Cannot decode 2FA JSON!", Toast.LENGTH_SHORT).show();
            }

            //proceed with fetching data
            logIn(username, username, null);
        }
    }

}
