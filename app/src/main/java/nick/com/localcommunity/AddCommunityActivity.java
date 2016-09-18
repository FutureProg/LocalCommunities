package nick.com.localcommunity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddCommunityActivity extends AppCompatActivity {

    static Communities.Community mCommunity;

    EditText mTitleView;
    EditText mDescView;
    ImageView mImageView;
    String mImageKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_community);
        setTitle(R.string.add_community);

        mTitleView = (EditText) findViewById(R.id.community_name);
        mDescView = (EditText) findViewById(R.id.community_desc);
        mImageView = (ImageView) findViewById(R.id.community_img);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mImageKey != null){
            outState.putString(Constants.ARG_IMAGE,mImageKey);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null){
            if(savedInstanceState.containsKey(Constants.ARG_IMAGE)){
                mImageKey = savedInstanceState.getString(Constants.ARG_IMAGE);
                ImageCache.loadAsyncBitmap(mImageKey, new ImageCache.BitmapLoadListener() {
                    @Override
                    public void loadedBitmap(byte[] data) {
                        mImageView.setImageBitmap(BitmapFactory.decodeByteArray(data,0,data.length));
                    }
                });
            }
        }
    }

    public static Communities.Community getCommunity(){
        Communities.Community temp = mCommunity;
        mCommunity = null;
        return temp;
    }

    private Bitmap scaleDownBitmap(Bitmap photo, int newHeight, Context context) {

        final float densityMultiplier = context.getResources().getDisplayMetrics().density;

        int h= (int) (newHeight*densityMultiplier);
        int w= (int) (h * photo.getWidth()/((double) photo.getHeight()));

        photo=Bitmap.createScaledBitmap(photo, w, h, true);

        return photo;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.REQ_COMMUNITY_IMAGE && resultCode == RESULT_OK){
                Uri uri = data.getData();
                CropImage.activity(uri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1,1)
                        .setCropShape(CropImageView.CropShape.RECTANGLE)
                        .setFixAspectRatio(true)
                        .setAllowRotation(true)
                        .start(this);
        }
        else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK){
            Bitmap bitmap = null;
            try {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),result.getUri());
                bitmap = scaleDownBitmap(bitmap, Communities.Community.COMMUNITY_IMAGE_SIZE,this);
                mImageKey = "temp_" + this.getClass().getName();
                ImageCache.cacheBitmap(mImageKey,bitmap,true);
                mImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void onUploadImage(View view){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,Constants.REQ_COMMUNITY_IMAGE);
    }

    void onSubmit(View view){
        boolean exit = false;
        if(mImageKey == null){
            Toast.makeText(this,R.string.image_missing,Toast.LENGTH_LONG).show();
            exit = true;
        }
        if(mDescView.getText() == null || mDescView.getText().toString().isEmpty()){
            mDescView.setError(getResources().getString(R.string.desc_missing));
            exit = true;
        }
        if(mTitleView.getText() == null || mTitleView.getText().toString().isEmpty()){
            mTitleView.setError(getResources().getString(R.string.title_missing));
            exit = true;
        }
        if(exit)return;
        final DatabaseReference comRef = FirebaseDatabase.getInstance().getReference().child("Communities");
        comRef.child("LENGTH").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long length = (Long)dataSnapshot.getValue();
                DatabaseReference ref = comRef.push();
                String title = mTitleView.getText().toString();
                String desc = mDescView.getText().toString();
                final String imgUrl;
                if(mImageKey != null || !mImageKey.isEmpty()){
                    imgUrl = length + title;
                }else{
                    imgUrl = "";
                }
                //get the owner id
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                final Communities.Community community = new Communities.Community((int)length,title,imgUrl,desc,uid);
                community.setArea(MainActivity.mArea.getName());
                mCommunity = community;
                ref.setValue(community);
                comRef.child("LENGTH").setValue(length+1);
                if(mImageKey != null){
                    ImageCache.loadAsyncBitmap(mImageKey, new ImageCache.BitmapLoadListener() {
                        @Override
                        public void loadedBitmap(byte[] data) {
                            StorageReference imgRef = FirebaseStorage.getInstance().getReference().child("community_images");
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            BitmapFactory.decodeByteArray(data,0,data.length)
                                    .compress(Bitmap.CompressFormat.JPEG,30,out);
                            data = out.toByteArray();
                            out = new ByteArrayOutputStream();
                            BitmapFactory.decodeByteArray(data,0,data.length)
                                    .compress(Bitmap.CompressFormat.JPEG,30,out);
                            data = out.toByteArray();
                            Bitmap bmp = Bitmap.createScaledBitmap(
                                    BitmapFactory.decodeByteArray(data,0,data.length),
                                    Communities.Community.COMMUNITY_IMAGE_SIZE,
                                    Communities.Community.COMMUNITY_IMAGE_SIZE,
                                    false
                                    );
                            out = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.JPEG,30,out);
                            data = out.toByteArray();
                            imgRef.child(imgUrl).putBytes(data);
                            //Add the community reference
                            Intent intent = new Intent();
                            intent.putExtra(Constants.ARG_COMMUNITY_ID,community.getId());
                            setResult(RESULT_OK,intent);
                            finish();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
