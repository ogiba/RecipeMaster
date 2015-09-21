package com.example.robert.recipemaster;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.robert.recipemaster.Adapters.IngredientAdapter;
import com.example.robert.recipemaster.Adapters.StepsAdapter;
import com.facebook.FacebookActivity;
import com.facebook.FacebookDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class RecipeActivity extends Activity {
    private Intent intent;
    private ArrayList<Ingredients> ingredientsArrayList = new ArrayList<>();
    private ArrayList<Steps> stepsArrayList = new ArrayList<>();
    private String prodTitle;
    private String prodDesc;
    private Bitmap focusedImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        rotation();
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.red_A700));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        intent = getIntent();

        getDataAboutProducts();
        setUserData();

        ViewGroup mainLayout = (ViewGroup)findViewById(R.id.mainLayout);
        mainLayout.invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_recipe, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.home) {

            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                Intent i = new Intent(this, MainActivity.class);
                startActivity(i);
                return true;
            case R.id.share_option:
                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        String ingredList = "";
                        String stepsList = "";
                        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getActionBar().getTitle().toString());
                        emailIntent.setType("application/octet-stream");
                        for (int i = 0; i < ingredientsArrayList.size(); i++) {
                            ingredList += ("- "+ingredientsArrayList.get(i).ingredient+"\r\n");
                        }

                        for (int i = 0; i < stepsArrayList.size(); i++) {
                            stepsList+=(stepsArrayList.get(i).step+""+stepsArrayList.get(i).stepDesc+"\r\n");
                        }
                        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, prodTitle + "\r\n\r\n" + prodDesc
                                + "\r\n\r\n" + "Ingredients:" + "\r\n\r\n"+ingredList+"\r\n\r\n"+"Preparing:"+"\r\n\r\n"+stepsList);


                        startActivity(emailIntent);
                        return false;
                    }
                });
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    private void setUserData(){
        TextView userName = (TextView)findViewById(R.id.nameView);
        if(StaticData.loggedIn!=false) {
            userName.setText("Logged as " + intent.getStringExtra("name"));

            Bitmap bmp = StaticData.fbImage;
            if (bmp != null) {
                ImageView fbImageView = (ImageView) findViewById(R.id.fbImage);
                fbImageView.setImageBitmap(bmp);
            }
        }else{
            userName.setText("Hello Stranger. Please sign in with Facebook");
            ImageView fbImageView = (ImageView) findViewById(R.id.fbImage);
            Bitmap bmp = BitmapFactory.decodeResource(getResources(),R.drawable.fb);
            fbImageView.setImageBitmap(bmp);
        }
    }

    private void getDataAboutProducts(){
        final RequestQueue queue = Volley.newRequestQueue(this);

        queue.add(new JsonObjectRequest(Request.Method.GET, "http://mooduplabs.com/test/info.php", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    prodTitle = response.get("title").toString();
                    prodDesc = response.getString("description").toString();
                    JSONArray ingredPieces = response.getJSONArray("ingredients");
                    JSONArray prepareSteps = response.getJSONArray("preparing");
                    JSONArray prodImgs = response.getJSONArray("imgs");

                    for (int i = 0; i < ingredPieces.length(); i++) {
                        String ing = ingredPieces.get(i).toString();
                        ingredientsArrayList.add(new Ingredients(ing));
                    }

                    for (int i = 0; i < prepareSteps.length(); i++) {
                        String step = prepareSteps.get(i).toString();
                        stepsArrayList.add(new Steps((i + 1) + ".", step));
                    }

                    for (int i = 0; i < prodImgs.length(); i++) {
                        String imgUrl = prodImgs.get(i).toString();
                        loadImageFromUrl(queue, imgUrl);
                    }

                    setDataToViews(prodTitle, prodDesc);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }));
    }

    private void setDataToViews(String title, String desc){
        TextView prodTitleView = (TextView)findViewById(R.id.prodNameField);
        TextView prodDescView = (TextView)findViewById(R.id.descProdField);
        try {
            getActionBar().setTitle(title + " Recipe!");
        }catch (NullPointerException ex){
            ex.printStackTrace();
        }
        prodTitleView.setText(title+":");
        prodDescView.setText(desc);

        setIngredients();
        setPrepareSteps();
    }

    private void setIngredients(){
        IngredientAdapter ingredAdapter = new IngredientAdapter(this,R.layout.ingredients_list,ingredientsArrayList);
        TextView imgView = (TextView)findViewById(R.id.titleIngredView);

        ListView ingredientsListView = (ListView) this.findViewById(R.id.ingredListView);

        ingredientsListView.setAdapter(ingredAdapter);
        //fixScrollingListV(ingredientsListView);

        imgView.setText(R.string.ingrid_title);
        setListViewHeightBasedOnChildren(ingredientsListView);
    }

    private void setPrepareSteps(){
        StepsAdapter stepsAdapter = new StepsAdapter(this,R.layout.prepare_list,stepsArrayList);
        ListView preparingListView = (ListView)this.findViewById(R.id.prepareListView);
        TextView imgView = (TextView)findViewById(R.id.titlePrepareView);

        preparingListView.setAdapter(stepsAdapter);

        //ixScrollingListV(preparingListView);

        imgView.setText(R.string.prepare_title);
        setListViewHeightBasedOnChildren(preparingListView);
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, DrawerLayout.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (2+listView.getDividerHeight() * (listAdapter.getCount()-3));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    public void addImageToViewer(Bitmap bmp){
        LinearLayout linLay = (LinearLayout)findViewById(R.id.linLayImages);
        LinearLayout faceLay = (LinearLayout)findViewById(R.id.faceImageLay);
        LinearLayout nameLay = (LinearLayout)findViewById(R.id.nameLay);

        if(checkScreenSize()<7.5) {
            linLay.addView(insertPhoto(bmp, 350, 330));
            nameLay.setPadding(10, 0, 0, 0);
        }
        else if(checkScreenSize()>=7.5) {
            linLay.addView(insertPhoto(bmp, 450, 430));
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT, 0.5f);
            faceLay.setLayoutParams(param);
            faceLay.setGravity(Gravity.CENTER | Gravity.LEFT);
        }

    }

    private void loadImageFromUrl(RequestQueue queue,String url){
        ImageRequest request = new ImageRequest(url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        addImageToViewer(bitmap);
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        queue.add(request);
    }

    private View insertPhoto(final Bitmap bmp, int layoutParams, int imageParams){

        LinearLayout layout = new LinearLayout(getApplicationContext());
        layout.setLayoutParams(new ViewGroup.LayoutParams(layoutParams, layoutParams));
        layout.setGravity(Gravity.CENTER);

        final ImageView imageView = new ImageView(RecipeActivity.this);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(imageParams, imageParams));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.isClickable();
        imageView.isFocusable();
        imageView.setImageBitmap(bmp);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createSaveImgDialog(bmp);
            }
        });

        imageView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    focusedImg = bmp;
                }
            }
        });

        layout.addView(imageView);
        return layout;
    }

    private void createSaveImgDialog(final Bitmap bmp){

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.save_dialog);
        dialog.setTitle(R.string.save_file);
        dialog.setCancelable(true);

        ImageView dialogImg = (ImageView)dialog.findViewById(R.id.pizzaImg);
        dialogImg.setImageBitmap(bmp);


        Button cancelDialogBtn = (Button)dialog.findViewById(R.id.cancelDialogBtn);
        cancelDialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });

        Button okDialogBtn = (Button)dialog.findViewById(R.id.okDialogBtn);
        okDialogBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String imgPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString() + "/Camera/";
                File myDir = new File(imgPath);

                if (myDir.exists())

                {
                    String fname = "Image-" + bmp.getGenerationId() + ".jpg";
                    File file = new File(myDir, fname);
                    if (file.exists()) file.delete();
                    try {
                        FileOutputStream out = new FileOutputStream(file);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.flush();
                        out.close();

                        dialog.cancel();

                        MediaScannerConnection.scanFile(RecipeActivity.this,
                                new String[]{file.toString()}, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {

                                    }
                                });
                        Toast.makeText(RecipeActivity.this, R.string.positive_info_about_saving, Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(RecipeActivity.this, R.string.negative_info_about_saving, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        dialog.create();
        dialog.show();
    }

    private Double checkScreenSize(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width=dm.widthPixels;
        int height=dm.heightPixels;
        int dens=dm.densityDpi;
        double wi=(double)width/(double)dens;
        double hi=(double)height/(double)dens;
        double x = Math.pow(wi,2);
        double y = Math.pow(hi,2);
        double screenInches = Math.sqrt(x+y);

        return screenInches;
    }

    private void rotation(){
        if(checkScreenSize()>6.5) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
    }
}
