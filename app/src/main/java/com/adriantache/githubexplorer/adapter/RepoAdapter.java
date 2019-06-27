package com.adriantache.githubexplorer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.adriantache.githubexplorer.R;

import java.util.ArrayList;

/**
 * RecyclerView adapter
 **/
public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.ViewHolder> {
    private ArrayList<String> dataset;
    private OnRepoListener onRepoListener;

    // Provide a suitable constructor (depends on the kind of dataset)
    public RepoAdapter(ArrayList<String> dataset, OnRepoListener onRepoListener) {
        this.dataset = dataset;
        this.onRepoListener = onRepoListener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RepoAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.my_text_view, parent, false);

        ViewHolder vh = new ViewHolder(v, onRepoListener);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.textView.setText(dataset.get(position));
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public interface OnRepoListener {
        void onRepoClick(int position);
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        // each data item is just a string in this case
        public TextView textView;
        OnRepoListener onRepoListener;

        public ViewHolder(TextView v, OnRepoListener onRepoListener) {
            super(v);
            textView = v;
            this.onRepoListener = onRepoListener;

            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onRepoListener.onRepoClick(getAdapterPosition());
        }
    }
}
