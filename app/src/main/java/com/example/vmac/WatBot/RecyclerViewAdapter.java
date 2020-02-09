package com.example.vmac.WatBot;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.vmac.WatBot.models.HomeCard;
import com.ibm.cloud.sdk.core.service.security.IamOptions;
import com.ibm.watson.discovery.v1.Discovery;
import com.ibm.watson.discovery.v1.model.QueryOptions;
import com.ibm.watson.discovery.v1.model.QueryResponse;

import java.util.List;

import static androidx.constraintlayout.widget.Constraints.TAG;

/**
 * Created by Juned on 3/27/2017.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyView> {

    private List<HomeCard> list;

    public class MyView extends RecyclerView.ViewHolder {

        public TextView textView;
        public ImageView imageView;
        public MyView(View view) {
            super(view);

            textView = (TextView) view.findViewById(R.id.text);
            imageView = (ImageView) view.findViewById(R.id.imageView5);
        }
    }


    public RecyclerViewAdapter(List<HomeCard> horizontalList) {
        this.list = horizontalList;
    }
    TextView tv;
    @Override
    public MyView onCreateViewHolder(ViewGroup parent, final int viewType) {
        final Context context = parent.getContext();
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_horizontal_item, parent, false);
// Return a new holder instance
        final MyView viewHolder = new MyView(itemView);
        tv = (TextView)itemView.findViewById(R.id.text);
        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //context.startActivity(intent);

                if(viewHolder.textView.getText().equals("Need some help!!")){
                    context.startActivity(new Intent(context,Help.class));
                }
                else{
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:04067173402"));//change the number
                    context.startActivity(callIntent);
                }
//                if(tv.getText().equals("Need some help!!")){
//                    context.startActivity(new Intent(context,Help.class));
//                }
            }
        });
        return new MyView(itemView);
    }

    @Override
    public void onBindViewHolder(final MyView holder, final int position) {
        holder.textView.setText(list.get(position).getText());
        holder.imageView.setImageResource(list.get(position).getImage());


    }

    @Override
    public int getItemCount() {
        return list.size();
    }

}