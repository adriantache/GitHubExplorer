package com.adriantache.githubexplorer;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String GIT_HUB_API = "api.github.com";
    private static final String USERS_ENDPOINT = "users";
    private static final String USER_ENDPOINT = "user";
    private static final String REPOS_ENDPOINT = "repos";
    private static final String ERROR = "ERROR";
    private static final String ERROR_2FA = "ERROR_2FA";
    private EditText twoFA;
    private String username;
    private String authHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button login = findViewById(R.id.login);
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText username = findViewById(R.id.username);
                EditText password = findViewById(R.id.password);
                twoFA = findViewById(R.id.twoFA);

                String user = username.getText().toString();
                String pass = password.getText().toString();
                String twoFactorAuth = twoFA.getText().toString();

                logIn(user, pass, twoFactorAuth);
            }
        });
    }

    private void logIn(String user, String pass, String twoFA) {
        if (TextUtils.isEmpty(user)) {
            Toast.makeText(this, "No user inserted!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            username = user;
        }

        Uri.Builder builder = new Uri.Builder();

        if (TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "No password inserted, only showing public repos!", Toast.LENGTH_SHORT).show();

            builder.scheme("https")
                    .authority(GIT_HUB_API)
                    .appendPath(USERS_ENDPOINT)
                    .appendPath(user)
                    .appendPath(REPOS_ENDPOINT);

            new NetworkWork().execute(builder.build().toString());
        } else {
            String base = Base64.encodeToString((user + ":" + pass).getBytes(), Base64.NO_WRAP);
            authHeader = "Basic " + base;

            Log.i("XXX", "logIn: "+ authHeader);

            builder.scheme("https")
                    .authority(GIT_HUB_API)
                    .appendPath(USER_ENDPOINT)
                    .appendPath(REPOS_ENDPOINT);

            if (TextUtils.isEmpty(twoFA)) {
                new NetworkWork().execute(builder.build().toString(), authHeader);
            } else {
                new NetworkWork().execute(builder.build().toString(), authHeader, twoFA);
            }
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
                Intent intent = new Intent(this, RepoList.class);
                intent.putExtra("username", username);
                intent.putExtra("authorization", authHeader);
                intent.putStringArrayListExtra("repos", repos);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Cannot extract repos from JSON!", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Toast.makeText(this, "Cannot decode JSON!", Toast.LENGTH_SHORT).show();
        }
    }

    class NetworkWork extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            if (url == null || url[0] == null) return ERROR;

            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder().url(url[0]);
            if (url.length > 1) builder.addHeader("Authorization", url[1]);
            if (url.length > 2) builder.addHeader("x-github-otp", url[2]);

            try (Response response = client.newCall(builder.build()).execute()) {
                if (response.code() == 401 && response.body().string().contains("Must specify two-factor authentication OTP code.")) {
                    return ERROR_2FA;
                } else if (!response.isSuccessful()) return ERROR;

                return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
                return ERROR;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (s.equals(ERROR_2FA)) {
                Toast.makeText(MainActivity.this, "Two-factor authentication is active, please enter code.", Toast.LENGTH_SHORT).show();
                twoFA.setVisibility(View.VISIBLE);
            } else if (s.equals(ERROR)) {
                Toast.makeText(MainActivity.this, "Cannot fetch data from GitHub!", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(s)) {
                Toast.makeText(MainActivity.this, "Empty response!", Toast.LENGTH_SHORT).show();
            } else {
                Log.i("XXX", "onPostExecute: "+s);
                decodeJson(s);
            }
        }
    }

}
