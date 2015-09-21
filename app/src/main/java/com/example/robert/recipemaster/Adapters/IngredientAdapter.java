package com.example.robert.recipemaster.Adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.robert.recipemaster.Ingredients;
import com.example.robert.recipemaster.R;

import java.util.ArrayList;

/**
 * Created by Robert on 19.09.2015.
 */
public class IngredientAdapter extends ArrayAdapter<Ingredients> {
    Context context;
    int layoutResourceId;
    ArrayList<Ingredients> data;

    public IngredientAdapter(Context context,int layoutResourceId,ArrayList<Ingredients> data){
        super(context,layoutResourceId,data);
        this.context=context;
        this.layoutResourceId=layoutResourceId;
        this.data=data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View row = convertView;
        IngredientHolder holder = null;
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        if(row == null)
        {
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);

            holder = new IngredientHolder();
            holder.ingredient = (TextView)row.findViewById(R.id.ingredient);

            row.setTag(holder);
        }
        else
        {
            holder = (IngredientHolder)row.getTag();
        }

        final Ingredients ingredient = data.get(position);
        holder.ingredient.setText(ingredient.ingredient);

        return row;
    }


    static class IngredientHolder{
        TextView ingredient;
    }
}
