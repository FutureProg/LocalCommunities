package nick.com.localcommunity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.IOException;
import java.nio.ByteBuffer;

public class EditProfileActivity extends AppCompatActivity {

    String mId;
    String mUsername;
    String mBio;


    ImageView mImageView;
    byte[] mImageBytes;
    EditText mUsernameView;
    EditText mBioView;

    private boolean mImageChanged = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        Intent intent = getIntent();
        mId = intent.getStringExtra(Constants.ARG_USER_ID);
        mUsername = intent.getStringExtra(Constants.ARG_USERNAME);
        mBio = intent.getStringExtra(Constants.ARG_USER_BIO);


        mImageView = (ImageView)findViewById(R.id.imageView);
        float densityMultiplier = getResources().getDisplayMetrics().density;
        /*mImageView.getLayoutParams().width = Math.round(User.DEFAULT_IMAGE_SIZE * densityMultiplier);
        mImageView.getLayoutParams().height = Math.round(User.DEFAULT_IMAGE_SIZE * densityMultiplier);*/
        if(ImageCache.inCache(mId)){
            ImageCache.loadAsyncBitmap(mId, new ImageCache.BitmapLoadListener() {
                @Override
                public void loadedBitmap(byte[] data) {
                    mImageBytes = data.clone();
                    mImageView.setImageBitmap(BitmapFactory.decodeByteArray(data,0,data.length));
                }
            });
        }
        mUsernameView = (EditText)findViewById(R.id.username);
        mUsernameView.setText(mUsername);
        mBioView = (EditText)findViewById(R.id.bio);
        mBioView.setText(mBio);
        mBioView.addTextChangedListener(new TextWatcher() {
            String txt;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                txt = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(mBioView.getLineCount() > mBioView.getMaxLines()){
                    mBioView.setText(txt);
                    mBioView.setSelection(mBioView.getText().length()-1);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }



    public void onChangeImage(View view){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,Constants.REQ_PROFILE_IMAGE);
    }

    private Bitmap scaleDownBitmap(Bitmap photo, int newHeight, Context context) {

        final float densityMultiplier = context.getResources().getDisplayMetrics().density;

        int h= (int) (newHeight*densityMultiplier);
        int w= (int) (h * photo.getWidth()/((double) photo.getHeight()));

        photo=Bitmap.createScaledBitmap(photo, w, h, true);

        return photo;
    }

    private byte[] shrinkImage(byte[] data,int width,int height){
        Bitmap bitmap;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        int heightRatio = (int)Math.ceil(options.outHeight/(float)height);
        int widthRatio = (int)Math.ceil(options.outWidth/(float)width);
        if (heightRatio > 1 || widthRatio > 1)
        {
            if (heightRatio > widthRatio)
            {
                options.inSampleSize = heightRatio;
            } else {
                options.inSampleSize = widthRatio;
            }
        }

        options.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeByteArray(data,0,data.length,options);
        ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
        bitmap.copyPixelsToBuffer(buffer);
        return buffer.array();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == Constants.REQ_PROFILE_IMAGE){
                CropImage.activity(data.getData())
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(1,1)
                        .setCropShape(CropImageView.CropShape.RECTANGLE)
                        .setFixAspectRatio(true)
                        .setAllowRotation(true)
                        .start(this);
        }
        else if(resultCode == RESULT_OK && requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            Bitmap bitmap = null;
            try {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),result.getUri());
            } catch (IOException e) {
                e.printStackTrace();
            }
            bitmap = scaleDownBitmap(bitmap,User.DEFAULT_IMAGE_SIZE,this);
            mImageView.setImageBitmap(bitmap);
            ImageCache.cacheBitmap(mId,bitmap,true);
            mImageChanged = true;
        }
    }

    public void onSaveChanges(View view){
        String nUsername = mUsernameView.getText().toString();
        Intent intent = new Intent();
        if(!nUsername.equals(mUsername)){
            intent.putExtra(Constants.ARG_USERNAME,nUsername);
        }
        String nBio = mBioView.getText().toString();
        if(!nBio.equals(mBio)){
            intent.putExtra(Constants.ARG_USER_BIO,nBio);
        }
        intent.putExtra(Constants.ARG_PROFILE_PIC,mImageChanged);
        setResult(RESULT_OK,intent);
        finish();
    }



}
