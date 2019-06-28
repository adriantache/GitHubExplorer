package com.adriantache.githubexplorer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.adriantache.githubexplorer.Link;
import com.adriantache.githubexplorer.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * RecyclerView adapter
 **/
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private ArrayList<Link> dataset;
    private OnRepoListener onRepoListener;

    // Provide a suitable constructor (depends on the kind of dataset)
    public FileAdapter(ArrayList<Link> dataset, OnRepoListener onRepoListener) {
        this.dataset = dataset;
        this.onRepoListener = onRepoListener;
    }

    // Create new views (invoked by the layout manager)
    @NotNull
    @Override
    public FileAdapter.ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        // create a new view
        TextView v = (TextView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.my_text_view, parent, false);

        ViewHolder vh = new ViewHolder(v, onRepoListener);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Link element = dataset.get(position);
        boolean isDirectory = element.getType().equals("dir");
        String text = element.getName() + (isDirectory ? " -> " : "");
        holder.textView.setText(text);
        //add icons based on link type
        holder.textView.setCompoundDrawablesWithIntrinsicBounds(isDirectory ? R.drawable.folder : R.drawable.file, 0, 0, 0);
        holder.textView.setCompoundDrawablePadding(50);
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
        TextView textView;
        OnRepoListener onRepoListener;

        ViewHolder(TextView v, OnRepoListener onRepoListener) {
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
