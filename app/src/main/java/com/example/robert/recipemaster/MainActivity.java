package com.example.robert.recipemaster;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.github.clans.fab.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import com.facebook.FacebookSdk;
import com.github.clans.fab.FloatingActionMenu;


public class MainActivity extends Activity {

    private ImageView imageViewRounded;
    private int currentImage= 0;
    private boolean loggedIn = false;
    private CallbackManager callbackManager;
    private String fbUserName;
    private ProfileTracker mProfileTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        rotation();

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.red_A700));

        FacebookSdk.sdkInitialize(this.getApplicationContext());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(isNetworkConnected()!=false) {

            imageViewRounded = (ImageView) findViewById(R.id.roundedImageView);

            callbackManager = CallbackManager.Factory.create();
            final LoginButton fbLoginBtn;
            fbLoginBtn = (LoginButton) findViewById(R.id.authButton);
            fbLoginBtn.setReadPermissions("user_friends");

            registerFBCallback(fbLoginBtn);



            Profile fbProfile = Profile.getCurrentProfile();

            if (fbProfile != null) {
                fbUserName = fbProfile.getName();
                getImageFromFb(fbProfile);
                StaticData.loggedIn=true;
            }
            setUserLabel(fbProfile);
            logginToFacebok();
            getRecipe();
        }else{
            RoundedImageView roundImg = (RoundedImageView)findViewById(R.id.roundedImageView);
            Bitmap noWifi = BitmapFactory.decodeResource(getResources(),R.drawable.no_wifi_error);
            roundImg.setImageBitmap(noWifi);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        //manage login result
        callbackManager.onActivityResult(requestCode, resultCode, data);

        setUserLabel(Profile.getCurrentProfile());

    }

    @Override
    protected void onStart() {
        super.onStart();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getData();
            }
        }, 0, 30000);
        famCustomClick();

        AccessTokenTracker accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                       AccessToken currentAccessToken) {
                if (currentAccessToken == null) {
                    FloatingActionButton logFbFab = (FloatingActionButton)findViewById(R.id.menu_item_fb);
                    logFbFab.setLabelText("Zaloguj przez Facebooka");
                    StaticData.loggedIn=false;
                }
            }
        };
        accessTokenTracker.isTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        rotation();

    }

    @Override
    protected void onPause() {
        super.onPause();
        FloatingActionMenu fam = (FloatingActionMenu)findViewById(R.id.menu);
        fam.animate();
        fam.close(true);
        setBgToNormal();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement

        return super.onOptionsItemSelected(item);
    }

    Timer timer = new Timer();

    private void famCustomClick(){
        final FloatingActionMenu fam = (FloatingActionMenu) findViewById(R.id.menu);

        fam.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!fam.isOpened()) {

                    fam.animate();
                    fam.open(true);
                    setWhiterBackground();
                } else {
                    fam.animate();
                    fam.close(true);
                    setBgToNormal();
                }
            }
        });
    }

    public void getData() {
        final RequestQueue queue = Volley.newRequestQueue(MainActivity.this);


        queue.add(new JsonObjectRequest(Request.Method.GET, "http://mooduplabs.com/test/info.php", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                try {

                    JSONArray array = response.getJSONArray("imgs");
                    String a = array.get(currentImage).toString();
                    loadImageFromUrl(queue, a);
                    currentImage++;
                    if (currentImage >= array.length())
                        currentImage = 0;

                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String err = error.toString();
            }
        }));

    }

    private void rotation(){
        if(checkScreenSize()>6.5) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        }
    }

    private void loadImageFromUrl(RequestQueue queue,String url){


        ImageRequest request = new ImageRequest(url,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        imageViewRounded.setImageBitmap(bitmap);
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });
// Access the RequestQueue through your singleton class.
        queue.add(request);
    }

    private void setUserLabel(Profile fbProfile){
        if(fbProfile!=null) {
            fbUserName = fbProfile.getName();
            FloatingActionButton logFbFab = (FloatingActionButton)findViewById(R.id.menu_item_fb);
            logFbFab.setLabelText(fbUserName);
        }
    }

    private void logginToFacebok(){
        final LoginButton fbLoginBtn = (LoginButton)findViewById(R.id.authButton);
        FloatingActionButton fbFab = (FloatingActionButton)findViewById(R.id.menu_item_fb);

        fbFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loggedIn != true) {
                    fbLoginBtn.performClick();

                    registerFBCallback(fbLoginBtn);
                    loggedIn=true;
                } else {
                    fbLoginBtn.performClick();
                    loggedIn=false;


                }
               /* LoginManager.getInstance().logOut();*/
            }
        });
    }

    private void getImageFromFb(Profile profile){
        RequestQueue queue = Volley.newRequestQueue(this);
        try {
            URL picture = new URL("https://graph.facebook.com/v2.4/" + profile.getId() + "/picture?type=large");
            String fbPicture = picture.toString();
            ImageRequest request = new ImageRequest(fbPicture,
                    new Response.Listener<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap bitmap) {
                            StaticData.fbImage = bitmap;
                        }
                    }, 0, 0, null,
                    new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                        }
                    });
            queue.add(request);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }
    }

    private void registerFBCallback(LoginButton fbLoginButton){
        fbLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                makeTrack(loginResult);

            }

            @Override
            public void onCancel() {
                // App code
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
            }

        });
    }

    private void getRecipe(){
        FloatingActionButton getRecipeFab = (FloatingActionButton)findViewById(R.id.menu_item_getRecipe);
        getRecipeFab.hasFocus();
        getRecipeFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent transIntent = new Intent(MainActivity.this, RecipeActivity.class);
                transIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                if (fbUserName != null) {
                    transIntent.putExtra("name",fbUserName);
                }
                startActivity(transIntent);
            }
        });

    }

    private void makeTrack(final LoginResult loginResult){
        mProfileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile profile, Profile profile2) {
                setUserLabel(profile2);
                if(profile2!=null) {
                    getImageFromFb(profile2);
                }
                StaticData.fbToken = loginResult.getAccessToken();
                mProfileTracker.stopTracking();
            }
        };
        mProfileTracker.startTracking();
        StaticData.loggedIn = true;
        Profile profile = Profile.getCurrentProfile();
    }

    private void setWhiterBackground(){

        RelativeLayout relLayImage = (RelativeLayout)findViewById(R.id.relLayImage);
        Float alpha = new Float(0.2);
        relLayImage.setAlpha(alpha);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.pink_s));

        getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.pink_b)));

    }

    private void setBgToNormal(){
        RelativeLayout relLayImage = (RelativeLayout)findViewById(R.id.relLayImage);
        Float alpha = new Float(1);
        relLayImage.setAlpha(alpha);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.red_A700));

        getActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.red_A500)));
    }

    private Double checkScreenSize(){
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width=dm.widthPixels;
        int height=dm.heightPixels;
        int dens=dm.densityDpi;
        double wi=(double)width/(double)dens;
        double hi=(double)height/(double)dens;
        double x = Math.pow(wi, 2);
        double y = Math.pow(hi,2);
        double screenInches = Math.sqrt(x + y);

        return screenInches;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }

}
