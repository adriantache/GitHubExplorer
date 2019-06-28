package com.adriantache.githubexplorer;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adriantache.githubexplorer.adapter.FileAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FileExplorer extends AppCompatActivity implements FileAdapter.OnRepoListener {
    public static final String GIT_HUB_API = "api.github.com";
    public static final String REPOS_ENDPOINT = "repos";
    public static final String CONTENT_ENDPOINT = "contents";
    private static final String TAG = "FileExplorer";
    ArrayList<Link> repos = new ArrayList<>();
    String username;
    String repoName;
    String path;
    ArrayList<String> lastPath = new ArrayList<>();
    String authHeader;
    private RecyclerView.Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        Intent intent = getIntent();
        repoName = intent.getStringExtra("repoName");
        username = intent.getStringExtra("username");
        authHeader = intent.getStringExtra("authorization");

        setTitle(repoName + "/");

        RecyclerView recyclerView = findViewById(R.id.list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        adapter = new FileAdapter(repos, this);
        recyclerView.setAdapter(adapter);

        //https://api.github.com/repos/adriantache/2048/contents/
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(GIT_HUB_API)
                .appendPath(REPOS_ENDPOINT)
                .appendPath(username)
                .appendPath(repoName)
                .appendPath(CONTENT_ENDPOINT);
        path = builder.build().toString();
        new NetworkWork().execute(path, authHeader);
    }

    void decodeJson(String responseBody) {
        if (TextUtils.isEmpty(responseBody) || responseBody.length() < 50) {
            Toast.makeText(this, "JSON response error: " + responseBody, Toast.LENGTH_SHORT).show();
        }

        repos.clear();

        try {
            JSONArray jsonRoot = new JSONArray(responseBody);

            if (!lastPath.isEmpty()) repos.add(new Link("..", "", "back"));

            for (int i = 0; i < jsonRoot.length(); i++) {
                JSONObject child = jsonRoot.optJSONObject(i);
                String repoNameJson = child.optString("name");
                String repoPath = child.optString("html_url");
                String repoType = child.optString("type");
                repos.add(new Link(repoNameJson, repoPath, repoType));
            }

            if (!repos.isEmpty()) {
                sortLinks(repos);
                //only works if we mutate dataset; if we change dataset we need to create a new adapter
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "No files returned from JSON!", Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.e("parseJSON", "Cannot parse JSON", e);
            finish();
        }
    }

    private void sortLinks(ArrayList<Link> repos) {
        ArrayList<Link> sorted = new ArrayList<>();

        if (repos.get(0).getType().equals("back")) sorted.add(repos.get(0));

        for (int i = 0; i < repos.size(); i++) {
            Link element = repos.get(i);
            if (element.getType().equals("dir")) sorted.add(element);
        }

        for (int i = 0; i < repos.size(); i++) {
            Link element = repos.get(i);
            if (element.getType().equals("file")) sorted.add(element);
        }

        for (int i = 0; i < repos.size(); i++) {
            Link element = repos.get(i);
            if (!element.getType().equals("file")
                    && !element.getType().equals("dir")
                    && !element.getType().equals("back"))
                sorted.add(element);
        }

        repos.clear();
        repos.addAll(sorted);
    }

    @Override
    public void onRepoClick(int position) {
        Link selected = repos.get(position);
        String name = selected.getName();
        String type = selected.getType();
        String link = selected.getPath();

        if (type.equals("file")) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(link));
            startActivity(i);
        } else {
            if (type.equals("back")) lastPath.remove(lastPath.size() - 1);
            else lastPath.add(name);

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority(GIT_HUB_API)
                    .appendPath(REPOS_ENDPOINT)
                    .appendPath(username)
                    .appendPath(repoName)
                    .appendPath(CONTENT_ENDPOINT);
            for (int i = 0; i < lastPath.size(); i++) {
                builder.appendPath(lastPath.get(i));
            }
            path = builder.build().toString();

            new NetworkWork().execute(path, authHeader);
        }
    }

    class NetworkWork extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            if (url == null) return null;

            Log.i(TAG, "Fetching " + url[0]);

            OkHttpClient client = new OkHttpClient();

            Request.Builder builder = new Request.Builder().url(url[0]);
            if (url.length > 1 && !TextUtils.isEmpty(url[1])) {
                builder.addHeader("Authorization", url[1]);
            }

            try (Response response = client.newCall(builder.build()).execute()) {
                return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (!TextUtils.isEmpty(s)) {
                decodeJson(s);
            } else {
                Toast.makeText(FileExplorer.this, "Error fetching file structure!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
