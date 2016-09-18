package nick.com.localcommunity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.multidex.MultiDex;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.melnykov.fab.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        LocationListener,
        CommunityListFragment.CommunityListListener,
        PostListFragment.OnPostListInteractionListener,
        NoNetworkFragment.NetworkFragmentListener,
        UserProfileFragment.ProfileFragmentListener,
        CommentFragment.CommentInteractionListener,
        FirebaseAuth.AuthStateListener{

    Location mCurrentLocation;
    public static Area mArea;
    User mUser;
    String mHomeTitle;//The title of the actionbar before leaving home
    FusedLocationListener mLocationListener;
    LocationLoader mLocationLoader;
    FloatingActionButton mFab;
    private NavigationView mNavigationView;
    ArrayList<Fragment> mHomeFragments = new ArrayList<>();

    //Authentication
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();

    //References to the Database
    DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
    DatabaseReference mAreaRef = mRootRef.child("Areas");
    DatabaseReference mCommunityRef = mRootRef.child("Communities");
    DatabaseReference mPostRef = mRootRef.child("Posts");
    DatabaseReference mUserRef = mRootRef.child("Users");
    DatabaseReference mCommentsRef = mRootRef.child("Comments");

    //Reference to the Storage
    FirebaseStorage mStorage = FirebaseStorage.getInstance();
    StorageReference mStorageReference = mStorage.getReferenceFromUrl("gs://localcommunities-a8fb9.appspot.com");
    StorageReference mCommunityImages = mStorageReference.child("community_images");
    StorageReference mPostImages = mStorageReference.child("post_images");
    StorageReference mUserImages = mStorageReference.child("user_images");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(BuildConfig.DEBUG){
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build());
        }
        super.onCreate(savedInstanceState);
        MultiDex.install(this);
        ImageCache.init(this);
        //Set the layout of the activity
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLocationListener = FusedLocationListener.getInstance(this,this);

        //onNetworkRequested();
        //Initialize Floating Action Button
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFabClicked(view);
            }
        });
        mFab.hide(false);

        //Initialize the Drawer Layout
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        mAuth.addAuthStateListener(this);
        onNetworkRequested();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);
    }

    void updateNavigationView(){
        if(!mAuth.getCurrentUser().isAnonymous()){
            if(mUser == null){
                return;
            }
            ((TextView)mNavigationView.findViewById(R.id.textView)).setText(mUser.username);
            //Update image profile
            final ImageView imageView = (ImageView)mNavigationView.findViewById(R.id.imageView);
            if(mUser.imgName != null && !mUser.imgName.isEmpty()){
                if(ImageCache.inCache(mUser.id)){
                    Log.v(UserProfileFragment.class.getSimpleName(),"Loading profile image from cache");
                    ImageCache.loadAsyncBitmap(mUser.id, new ImageCache.BitmapLoadListener() {
                        @Override
                        public void loadedBitmap(byte[] data) {
                            Log.v(UserProfileFragment.class.getSimpleName(),"Loaded image");
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                            imageView.setImageBitmap(bitmap);
                        }
                    });
                }else {
                    mUserImages.child(mUser.imgName)
                            .getBytes(Constants.MAX_PROFILE_IMAGE_SIZE)
                            .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    Bitmap img = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    imageView.setImageBitmap(img);
                                    ImageCache.cacheBitmap(mUser.id, img);
                                }
                            });
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mArea == null)return;
        outState.putString(Constants.ARG_AREA,mArea.getName());
        outState.putLong(Constants.ARG_AREA_ID,mArea.id);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState.containsKey(Constants.ARG_AREA)){
            mArea = new Area();
            mArea.id = savedInstanceState.getLong(Constants.ARG_AREA_ID);
            mArea.name = savedInstanceState.getString(Constants.ARG_AREA);
           openHome();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onStop(){
        super.onStop();
        mAuth.removeAuthStateListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void updateLocation() {
        final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String locationProvider = LocationManager.GPS_PROVIDER;
        if(!locationManager.isProviderEnabled(locationProvider)){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.location_disabled_message)
                    .setTitle(R.string.location_disabled)
                    .setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent,0xff);
                        }
                    })
                    .setCancelable(true)
                    .show();
            return;

        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    Constants.REQ_LOCATION_ACCESS);
            return;
        }
        Location location = locationManager.getLastKnownLocation(locationProvider);
        if(location == null){
            //locationManager.requestLocationUpdates(locationProvider, 0, 0, this);
            //locationManager.requestSingleUpdate(locationProvider,this,null);
            mLocationListener.start();
        }
        else
            onLocationChanged(location);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.REQ_LOCATION_ACCESS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateLocation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.REQ_LOCATION_ACCESS){
            updateLocation();
        }
        else if(requestCode == Constants.REQ_SIGN_IN){
            if(mAuth.getCurrentUser() == null || resultCode == RESULT_CANCELED){
                return;
            }
            checkSignup(mAuth);
        }
        else if(requestCode == UserProfileFragment.REQ_USER_PROFILE_EDIT){
            //openProfile(mUser);
            if(resultCode != RESULT_OK)return;
            FragmentManager manager = getSupportFragmentManager();
            Fragment fragment = manager.findFragmentByTag(UserProfileFragment.class.getSimpleName());
            if(fragment != null){
                ((UserProfileFragment)fragment).updateData(data);
            }else{
                Log.d("STUFF","Null Fragment");
            }
        }
        else if(requestCode == Constants.REQ_ADD_COMMUNITY && resultCode == RESULT_OK){
            //Open newly made community
            Communities.Community community = AddCommunityActivity.getCommunity();
            openPostList(community);
        }
        else if(requestCode == Constants.REQ_ADD_POST && resultCode == RESULT_OK){
            //Open newly made post
            Posts.Post post = AddPostActivity.getPost();
            openComments(post,0);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        FragmentManager manager = getSupportFragmentManager();
        Fragment f;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if(manager.findFragmentByTag(UserProfileFragment.class.getSimpleName()) != null){
            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null && mHomeTitle == null){
            mHomeTitle = actionBar.getTitle().toString();
            Log.v("mHomeTitle",mHomeTitle);
        }
        if(id == R.id.nav_home){
            openHome();
        }
        else if (id == R.id.nav_profile) {
            if (mAuth.getCurrentUser().isAnonymous()) {
                signIn();
            }else{
                FragmentManager fragmentManager = getSupportFragmentManager();
                if(fragmentManager.findFragmentByTag(UserProfileFragment.class.getSimpleName()) == null){
                    openProfile(mUser,false);
                }
            }
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        } else if(id == R.id.nav_settings){
            Intent intent = new Intent(this,SettingsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void signIn(){
        AuthUI authUI = AuthUI.getInstance();
        AuthUI.SignInIntentBuilder builder = authUI.createSignInIntentBuilder();
        Intent intent = builder.setProviders(/*AuthUI.FACEBOOK_PROVIDER, AuthUI.GOOGLE_PROVIDER,*/
                AuthUI.EMAIL_PROVIDER).build();
        startActivityForResult(intent,Constants.REQ_SIGN_IN);
    }

    void checkSignup(@NonNull final FirebaseAuth auth){
        final User user = new User();
        user.id = auth.getCurrentUser().getUid();
        mUserRef.orderByChild("id").equalTo(user.id)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot data: dataSnapshot.getChildren()){
                        mUser = data.getValue(User.class);
                    }
                }else{
                    DatabaseReference ref = mUserRef.push();
                    user.email = auth.getCurrentUser().getEmail();
                    user.username = auth.getCurrentUser().getDisplayName();
                    mUser = user;
                    ref.setValue(user);
                    Log.v("USER_INFO","Making new user");
                }
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateNavigationView();
                    }
                },500);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(location == null)return;
        mLocationListener.stop();
        mCurrentLocation = location;
        ((LocationManager) getSystemService(Context.LOCATION_SERVICE)).removeUpdates(this);
        String txt = "Lat: " + mCurrentLocation.getLatitude() + "\n" +
                    "Lon: " + mCurrentLocation.getLongitude() + "\n";

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        StringBuilder builder = new StringBuilder();
        try{
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            Address address = addressList.get(0);
            /*
            int maxlines = addressList.get(0).getMaxAddressLineIndex();
            for (int i = 0; i < maxlines;i++){
                String addressStr = addressList.get(0).getAddressLine(i);
                builder.append(addressStr);
                builder.append(" ");
            }
            txt += "\n" + builder.toString();*/
            txt += address.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mLocationLoader = new LocationLoader();
        mLocationLoader.execute(location.getLatitude(),location.getLongitude());

    }

    void openHome(){
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(UserProfileFragment.class.getSimpleName());
        if (fragment != null){
            FragmentManager manager = getSupportFragmentManager();
            manager.beginTransaction().remove(fragment).commit();
            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null && mHomeTitle != null){
                actionBar.setTitle(mHomeTitle);
                Log.v("mHomeTitle",mHomeTitle);
                mHomeTitle = null;
            }
        }else{
            openCommunityList();
        }
    }

    void openCommunityList(){
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        CommunityListFragment fragment = new CommunityListFragment();
        Bundle data = new Bundle();
        data.putString("area", mArea.getName());
        fragment.setArguments(data);
        fragment.setRetainInstance(true);
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(R.id.frame_layout,fragment).commit();
    }

    void openPostList(Communities.Community community){
        getSupportActionBar().setTitle(community.getName());
        PostListFragment fragment = new PostListFragment();
        Bundle args = new Bundle();
        args.putLong(PostListFragment.ARG_COMMUNITY_ID,community.getId());
        args.putString(PostListFragment.ARG_COMMUNITY_NAME,community.getName());
        fragment.setArguments(args);
        fragment.setRetainInstance(true);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right);
        transaction.replace(R.id.frame_layout,fragment,PostListFragment.class.getSimpleName())
                .addToBackStack(null).commit();
    }


    void openComments(Posts.Post post,int index){
        CommentFragment fragment = CommentFragment.newInstance(1,post.id,post,index,post.title,post.image);
        fragment.setRetainInstance(true);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                R.anim.slide_in_left, R.anim.slide_out_right);
        transaction.replace(R.id.frame_layout,fragment).addToBackStack(null).commit();
    }

    void openProfile(User user) {
        openProfile(user, true);
    }

    void openProfile(User user, boolean pushToBack){
        if(user == null)return;
        UserProfileFragment profileFragment = UserProfileFragment.newInstance(user.username,
                user.bio,user.imgName,user.id);
        profileFragment.setRetainInstance(true);
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction =manager.beginTransaction();
        if(pushToBack){
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left,
                    R.anim.slide_in_left, R.anim.slide_out_right);
            transaction.addToBackStack("ProfileBackstack");
            transaction.replace(R.id.frame_layout,profileFragment,UserProfileFragment.class.getSimpleName()+"_NOT_NAV").commit();
        }else{
            /*for(int i = 0; i < manager.getBackStackEntryCount();i++){
                manager.popBackStack();
            }*/

            transaction.add(R.id.frame_layout,profileFragment,UserProfileFragment.class.getSimpleName()).commit();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        if(provider.equals(LocationManager.GPS_PROVIDER)){
            updateLocation();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onCommunityListInteraction(Communities.Community item, int index, boolean openActivity) {
        if(item == null)return;
        if(!openActivity){
            openPostList(item);
        }else{
            //TODO: Open Community Activity
        }
    }

    @Override
    public void onCommunityAddDialog() {
        //TODO: Open Community adder activity
    }

    public DatabaseReference getCommunityReference(){
        return mCommunityRef;
    }

    public DatabaseReference getAreaReference(){
        return mAreaRef;
    }

    public DatabaseReference getPostReference(){return mPostRef;}

    public DatabaseReference getUserReference(){return mUserRef;}

    public DatabaseReference getCommentsReference(){return mCommentsRef;}

    public StorageReference getCommunityImageReference(){return mCommunityImages;}

    public StorageReference getPostImageReference(){
        return mPostImages;
    }

    public StorageReference getUserImageReference() { return mUserImages; }

    public FloatingActionButton getFab(){
        return mFab;
    }

    @Override
    public void onPostClick(Posts.Post post, int index) {
        openComments(post,index);
    }

    @Override
    public boolean onPostLongPress(final PostListViewAdapter adapter, final Posts.Post post, final int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(post.userId.equals(mAuth.getCurrentUser().getUid())){
                    if(i == 0){ //delete
                        mPostRef.child(post.id).removeValue();
                        mCommentsRef.orderByChild("postid").equalTo(post.id)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        for(DataSnapshot data : dataSnapshot.getChildren()){
                                            data.getRef().removeValue();
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                        Posts.ITEMS.remove(index);
                        Posts.ITEMS_MAP.remove(post.id);
                        adapter.notifyDataSetChanged();
                    }
                }
            }
        };
        int array_id = R.array.post_long_press;
        if(mAuth.getCurrentUser().getUid().equals(post.userId)){
            array_id = R.array.post_long_press_user;
        }
        builder.setItems(array_id, listener).show();
        return true;
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

    }

    @Override
    public void onNetworkRequested() {
        FragmentManager manager = getSupportFragmentManager();

        //check network connectivity
        ConnectivityManager connectivityManager =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if( info == null || info.getState() == NetworkInfo.State.UNKNOWN ||
                info.getState() == NetworkInfo.State.DISCONNECTED||
                info.getState() == NetworkInfo.State.SUSPENDED){
            manager.beginTransaction().replace(R.id.frame_layout,new NoNetworkFragment()).commit();
            return;
        }else{
            manager.beginTransaction().replace(R.id.frame_layout,new StartupFragment()).commit();
        }
        if (mAuth.getCurrentUser() == null) {
            mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (!task.isSuccessful()) {
                        Log.w("USER_AUTH", "signInAnonymously", task.getException());
                        /*Toast.makeText(MainActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();*/
                    }
                    //Retrieve the users location
                    updateLocation();
                }
            });
        }else if(!mAuth.getCurrentUser().isAnonymous()){
            checkSignup(mAuth);
            updateLocation();
        }else{
            updateLocation();
        }
    }

    @Override
    public void onProfileInteraction(int req) {
        if(req == UserProfileFragment.REQ_USER_PROFILE_EDIT){
            Intent intent = new Intent(this,EditProfileActivity.class);
            intent.putExtra(Constants.ARG_USER_ID,mUser.id);
            intent.putExtra(Constants.ARG_USERNAME,mUser.username);
            intent.putExtra(Constants.ARG_PROFILE_PIC,mUser.imgName);
            intent.putExtra(Constants.ARG_USER_BIO,mUser.bio);
            startActivityForResult(intent,req);
        }
        else if(req == Constants.REQ_SIGN_OUT){
            FirebaseAuth.getInstance().signOut();
            FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    ((TextView)mNavigationView.findViewById(R.id.textView)).setText("");
                    ((ImageView)mNavigationView.findViewById(R.id.imageView)).setImageBitmap(null);
                    mNavigationView.setCheckedItem(R.id.nav_home);
                    openCommunityList();
                }
            });
        }
    }

    public void onFabClicked(View view){
        if(mFab.getTag().equals(CommunityListFragment.class.getSimpleName())){
            Intent intent = new Intent(this,AddCommunityActivity.class);
            startActivityForResult(intent,Constants.REQ_ADD_COMMUNITY);
        }
        else if(mFab.getTag().equals(PostListFragment.class.getSimpleName())){
            Intent intent = new Intent(this,AddPostActivity.class);
            PostListFragment fragment = (PostListFragment) getSupportFragmentManager()
                    .findFragmentByTag(PostListFragment.class.getSimpleName());
            intent.putExtra(Constants.ARG_COMMUNITY_ID,fragment.getCommunityId());
            startActivityForResult(intent,Constants.REQ_ADD_POST);
        }
    }

    @Override
    public void onCommentInteraction(Comments.Comment item) {
        mUserRef.orderByChild("id").equalTo(item.userid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = null;
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    user = data.getValue(User.class);
                }
                if(user != null) openProfile(user,true);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onPostComment(Posts.Post post, String content) {
        if(mUser != null){
            DatabaseReference ref = mCommentsRef.push();
            Comments.Comment comment = new Comments.Comment();
            comment.userid = mUser.id;
            comment.postid = post.id;
            comment.id = ref.getKey();
            DateFormat format = DateFormat.getDateTimeInstance();
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            comment.date = format.format(new Date(0));
            comment.message = content;
            ref.setValue(comment);
        }
    }



    class LocationLoader extends AsyncTask<Double,Void,String>{

        String name;
        double lat;
        double lng;

        @Override
        protected String doInBackground(Double... params) {
            lat = params[0];
            lng = params[1];
            String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng="+lat+","+lng;
            Log.v("LOCATION",urlStr);
            try {
                URL url = URI.create(urlStr).toURL();
                URLConnection connection = url.openConnection();
                BufferedReader inputStream = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line = "";
                while((line = inputStream.readLine())!=null){
                    builder.append(line);
                }
                String data = builder.toString();
                JSONObject object = new JSONObject(data);
                Log.v("LOCATION",object.getJSONArray("results").getJSONObject(0).toString());
                JSONArray array = object.getJSONArray("results").getJSONObject(0).getJSONArray("address_components");
                //If area does not directly belong to a municipality, only do it for the city
                name = Utilities.getAreaInfoByType(array,"administrative_area_level_2");
                if(name == null){
                    name = Utilities.getAreaInfoByType(array,"locality");//TODO: Change to closest area in the future
                }
                Log.v("LOCATION",object.getJSONArray("results").getJSONObject(0).toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            s = name;
            if(isDestroyed()){
                return;
            }
            mAreaRef.orderByChild("name").equalTo(name).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.getValue() == null){
                        mAreaRef.child("LENGTH").addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Area area = new Area();
                                area.id = (long)dataSnapshot.getValue();
                                area.name = name;
                                mAreaRef.push().setValue(area);
                                mAreaRef.child("LENGTH").setValue(area.id+1);
                                mArea = area;
                                openCommunityList();
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                        return;
                    }
                    mArea = dataSnapshot.getChildren().iterator().next().getValue(Area.class);
                    Log.v("AREA",dataSnapshot.getValue()+"");
                    String txt = lat+","+lng+","+name;
                    openCommunityList();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
            //((TextView)findViewById(R.id.hello_text)).setText(txt);
            super.onPostExecute(s);
            this.cancel(true);
        }
    }
}
