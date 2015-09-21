package com.example.robert.recipemaster.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.robert.recipemaster.R;
import com.example.robert.recipemaster.Steps;

import java.util.ArrayList;

/**
 * Created by Robert on 19.09.2015.
 */
public class StepsAdapter extends ArrayAdapter<Steps> {

    Context context;
    int layoutResourceId;
    ArrayList<Steps> data;

    public StepsAdapter(Context context,int layoutResourceId,ArrayList<Steps> data){
        super(context,layoutResourceId,data);

        this.context=context;
        this.layoutResourceId=layoutResourceId;
        this.data=data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        StepHolder holder = null;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new StepHolder();
            holder.step = (TextView)row.findViewById(R.id.stepView);
            holder.stepDesc =(TextView)row.findViewById(R.id.stepDescView);

            row.setTag(holder);
        }
        else
        {
            holder = (StepHolder)row.getTag();
        }

        final Steps steps = data.get(position);
        holder.step.setText(steps.step);
        holder.stepDesc.setText(steps.stepDesc);

        return row;
    }


    static class StepHolder{
        TextView step;
        TextView stepDesc;
    }
}
