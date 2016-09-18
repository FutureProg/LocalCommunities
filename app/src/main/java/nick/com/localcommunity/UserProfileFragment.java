package nick.com.localcommunity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import nick.com.localcommunity.UtilityViews.SquareImageView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ProfileFragmentListener} interface
 * to handle interaction events.
 * Use the {@link UserProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UserProfileFragment extends Fragment implements View.OnClickListener {
    private static final String ARG_USERNAME = "param1";
    private static final String ARG_BIO = "param2";
    private static final String ARG_IMAGE = "param3";
    private static final String ARG_ID = "param4";

    public static final int REQ_USER_PROFILE_EDIT = 53;

    private String mUsername;
    private String mBio;
    private String mImage;
    private String mId;

    private SquareImageView mUserImage;
    private TextView mUsernameView;
    private TextView mBioTextview;

    private ProfileFragmentListener mListener;
    private boolean mFabWasShown;
    private String mFabPrevTag;

    public UserProfileFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param username Parameter 1.
     * @param bio Parameter 2.
     * @return A new instance of fragment UserProfile.
     */
    public static UserProfileFragment newInstance(String username, String bio, String image, String id) {
        UserProfileFragment fragment = new UserProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_BIO, bio);
        args.putString(ARG_IMAGE, image);
        args.putString(ARG_ID,id);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (getArguments() != null) {
            mUsername = getArguments().getString(ARG_USERNAME);
            mBio = getArguments().getString(ARG_BIO);
            mImage = getArguments().getString(ARG_IMAGE);
            mId = getArguments().getString(ARG_ID);
        }
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState == null)return;
        mUsername = savedInstanceState.getString(ARG_USERNAME);
        mBio = savedInstanceState.getString(ARG_BIO);
        mImage = savedInstanceState.getString(ARG_IMAGE);
        mId = savedInstanceState.getString(ARG_ID);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_USERNAME,mUsername);
        outState.putString(ARG_BIO,mBio);
        outState.putString(ARG_IMAGE,mImage);
        outState.putString(ARG_ID,mId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View re = inflater.inflate(R.layout.fragment_user_profile, container, false);
        mUsernameView =((TextView)re.findViewById(R.id.profile_username));
        mUsernameView.setText(mUsername);
        mBioTextview = (TextView)re.findViewById(R.id.profile_bio);
        mBioTextview.setText(mBio);
        mUserImage = ((SquareImageView) re.findViewById(R.id.profile_image));
        if(mImage != null && !mImage.isEmpty()){
            if(ImageCache.inCache(mId)){
                Log.v(UserProfileFragment.class.getSimpleName(),"Loading profile image from cache");
                ImageCache.loadAsyncBitmap(mId, new ImageCache.BitmapLoadListener() {
                    @Override
                    public void loadedBitmap(byte[] data) {
                        Log.v(UserProfileFragment.class.getSimpleName(),"Loaded image");
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                        mUserImage.setImageBitmap(bitmap);
                    }
                });
            }else {
                ((MainActivity) getActivity()).getUserImageReference().child(mImage)
                        .getBytes(Constants.MAX_PROFILE_IMAGE_SIZE)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap img = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                                ((ImageView) getView().findViewById(R.id.profile_image)).setImageBitmap(img);
                                ImageCache.cacheBitmap(mId, img);
                            }
                        });
            }
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        //if not looking at own profile
        if(auth.getCurrentUser().isAnonymous() && auth.getCurrentUser().getUid() != mId){
            re.findViewById(R.id.edit_button).setAlpha(0);
            re.findViewById(R.id.edit_button).setOnClickListener(null);
            re.findViewById(R.id.signout_button).setAlpha(0);
            re.findViewById(R.id.signout_button).setOnClickListener(null);
        }else{
            re.findViewById(R.id.edit_button).setOnClickListener(this);
            re.findViewById(R.id.signout_button).setOnClickListener(this);
        }
        return re;
    }

    public void updateData(Intent data){
        Log.v(UserProfileFragment.class.getSimpleName(),"update profile data");
        if(data.getBooleanExtra(Constants.ARG_PROFILE_PIC,false)) {
            byte[] array = data.getByteArrayExtra(Constants.ARG_IMAGE_BYTES);
            ImageCache.loadAsyncBitmap(mId, new ImageCache.BitmapLoadListener() {
                @Override
                public void loadedBitmap(byte[] data) {
                    mUserImage.setImageBitmap(BitmapFactory.decodeByteArray(data,0,data.length));
                }
            });
        }
        if(data.hasExtra(Constants.ARG_USERNAME)){
            mUsername = data.getStringExtra(Constants.ARG_USERNAME);
            mUsernameView.setText(mUsername);
        }
        if(data.hasExtra(Constants.ARG_USER_BIO)){
            mBio = data.getStringExtra(Constants.ARG_USER_BIO);
            mBioTextview.setText(mBio);
        }
        ((MainActivity)getActivity()).mUser.username = mUsername;
        ((MainActivity)getActivity()).mUser.bio = mBio;
        ((MainActivity)getActivity()).mUser.imgName = mId;
        UploadChanges uploadChanges = new UploadChanges();
        uploadChanges.execute(data);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mFabWasShown = ((MainActivity)getActivity()).getFab().isVisible();
        if(mFabWasShown){
            mFabPrevTag = (String)((MainActivity)getActivity()).getFab().getTag();
        }
        ((MainActivity)getActivity()).getFab().hide();
        ((MainActivity)getActivity()).getFab().setTag(this.getClass().getSimpleName());
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (context instanceof ProfileFragmentListener) {
            ActionBar actionBar = ((MainActivity)getActivity()).getSupportActionBar();
            if(actionBar != null)actionBar.setTitle(R.string.profile);
            mListener = (ProfileFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if(mFabWasShown){
            ((MainActivity)getActivity()).getFab().show();
            ((MainActivity)getActivity()).getFab().setTag(mFabPrevTag);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.edit_button){
            if(mListener != null){
                mListener.onProfileInteraction(REQ_USER_PROFILE_EDIT);
            }
        }
        else if(v.getId() == R.id.signout_button){
            mListener.onProfileInteraction(Constants.REQ_SIGN_OUT);
        }
    }

    class UploadChanges extends AsyncTask<Intent,Void,Void> {

        @Override
        protected Void doInBackground(Intent... params) {
            Intent data = params[0];
            if(data.getBooleanExtra(Constants.ARG_PROFILE_PIC,false)){
                ImageCache.loadAsyncBitmap(mId, new ImageCache.BitmapLoadListener() {
                    @Override
                    public void loadedBitmap(byte[] data) {
                        StorageReference ref = ((MainActivity)getActivity()).getUserImageReference();
                        if(mImage == null || mImage.isEmpty()){
                            mImage = mId;
                            ((MainActivity) getActivity()).getUserReference().orderByChild("id").
                                    equalTo(mId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for(DataSnapshot data : dataSnapshot.getChildren()){
                                        data.getRef().child("imgName").setValue(mId);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                        ref.child(mImage).putBytes(data);
                    }
                });
            }
            if(data.hasExtra(Constants.ARG_USERNAME)){
                final String username = data.getStringExtra(Constants.ARG_USERNAME);
                DatabaseReference ref = ((MainActivity)getActivity()).getUserReference();
                ref.orderByChild("id").equalTo(mId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot data : dataSnapshot.getChildren()){
                            data.getRef().child("username").setValue(username);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
            if(data.hasExtra(Constants.ARG_USER_BIO)){
                final String bio = data.getStringExtra(Constants.ARG_USER_BIO);
                DatabaseReference ref = ((MainActivity)getActivity()).getUserReference();
                ref.orderByChild("id").equalTo(mId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot data : dataSnapshot.getChildren()){
                            data.getRef().child("bio").setValue(bio);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }
            return null;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface ProfileFragmentListener {
        void onProfileInteraction(int req);
    }
}
