package nick.com.localcommunity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by Nick on 16-07-10.
 * Source from @link(http://stackoverflow.com/questions/17169143/android-location-listener-in-service-does-not-work-until-i-reopen-wifi-mobile-ne)
 */
public class FusedLocationListener implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {


    private LocationListener mListener;

    public static final String TAG = "Fused";
    private GoogleApiClient locationClient;
    private LocationRequest locationRequest;

    protected int minDistanceToUpdate = 1000;
    protected int minTimeToUpdate = 10 * 1000;

    protected Context mContext;


    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "Connected");
        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(1);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(30000);
        locationRequest.setNumUpdates(1);
        if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location l = LocationServices.FusedLocationApi.getLastLocation(locationClient);
        if(l == null){
            LocationServices.FusedLocationApi.requestLocationUpdates(locationClient,locationRequest,this);
        }else{
            mListener.onLocationChanged(l);
        }
        //mListener.onLocationChanged(LocationServices.FusedLocationApi.getLastLocation(locationClient));
        //locationClient.registerConnectionCallbacks(this);
        //locationClient.registerConnectionFailedListener(this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG,"Connection Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Failed");
    }


    private static FusedLocationListener instance;

    public static synchronized FusedLocationListener getInstance(Context context, LocationListener listener){
        if (null==instance) {
            instance = new FusedLocationListener(context, listener);
        }
        return instance;
    }


    private FusedLocationListener(Context context, LocationListener listener){
        mContext = context;
        mListener = listener;
    }


    public void start(){

        Log.d(TAG, "Listener started");
        locationClient = new GoogleApiClient.Builder(mContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        locationClient.connect();

    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location received: " + location.getLatitude() + ";" + location.getLongitude());
        //notify listener with new location
        mListener.onLocationChanged(location);
    }


    public void stop() {
        if(locationClient == null)return;
        locationClient.unregisterConnectionCallbacks(this);
        locationClient.unregisterConnectionFailedListener(this);
        LocationServices.FusedLocationApi.removeLocationUpdates(locationClient,this);
        locationClient.disconnect();
    }
}