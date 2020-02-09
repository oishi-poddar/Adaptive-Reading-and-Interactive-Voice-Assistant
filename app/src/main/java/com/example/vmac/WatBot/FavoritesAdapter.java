package com.example.vmac.WatBot;


import android.content.Context;
//import android.support.v7.widget.RecyclerView;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.example.vmac.WatBot.models.SavedDocs;
import com.ibm.watson.compare_comply.v1.model.Contact;

import java.util.ArrayList;
import java.util.List;


public class FavoritesAdapter extends
        RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {
    // Store a member variable for the contacts
    private ArrayList<SavedDocs> docs;

    // Pass in the contact array into the constructor
    public FavoritesAdapter(ArrayList<SavedDocs> docs) {
        this.docs = docs;
    }
    // ... constructor and member variables
    public class ViewHolder extends RecyclerView.ViewHolder {
        // Your holder should contain a member variable
        // for any view that will be set as you render a row
        public TextView nameTextView;

        // We also create a constructor that accepts the entire item row
        // and does the view lookups to find each subview
        public ViewHolder(View itemView) {
            // Stores the itemView in a public final member variable that can be used
            // to access the context from any ViewHolder instance.
            super(itemView);

            nameTextView = (TextView) itemView.findViewById(R.id.text);
        }
    }
    // Usually involves inflating a layout from XML and returning the holder
    @Override
    public FavoritesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View view = inflater.inflate(R.layout.favourite_item, parent, false);

        // Return a new holder instance
        final ViewHolder viewHolder = new ViewHolder(view);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context,FileViewer1.class);
                intent.putExtra("url",context.getSharedPreferences("Favourite Documents",Context.MODE_PRIVATE).getString(viewHolder.nameTextView.getText().toString(),"Not Found"));

                context.startActivity(intent);
            }
        });
        return viewHolder;
    }

    // Involves populating data into the item through holder
    @Override
    public void onBindViewHolder(FavoritesAdapter.ViewHolder viewHolder, int position) {
        // Get the data model based on position
        String name = docs.get(position).getName();

        // Set item views based on your views and data model
        TextView textView = viewHolder.nameTextView;
        textView.setText(name);
    }

    // Returns the total count of items in the list
    @Override
    public int getItemCount() {
        return docs.size();
    }

}