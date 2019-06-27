package com.adriantache.githubexplorer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.adriantache.githubexplorer.adapter.RepoAdapter;

import java.util.ArrayList;

public class RepoList extends AppCompatActivity implements RepoAdapter.OnRepoListener {
    ArrayList<String> repos = new ArrayList<>();
    private String username;
    private String authHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo_list);

        Intent intent = getIntent();
        repos = intent.getStringArrayListExtra("repos");
        username = intent.getStringExtra("username");
        authHeader = intent.getStringExtra("authorization");

        if (repos == null) finish();

        setTitle("Repos for " + username);

        RecyclerView recyclerView = findViewById(R.id.list);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);

        // use a linear layout manager
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // specify an adapter (see also next example)
        RecyclerView.Adapter adapter = new RepoAdapter(repos, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onRepoClick(int position) {
        String repoName = repos.get(position);

        Intent intent = new Intent(this, FileExplorer.class);
        intent.putExtra("repoName", repoName);
        intent.putExtra("username", username);
        intent.putExtra("authorization", authHeader);
        startActivity(intent);
    }
}
