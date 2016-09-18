package nick.com.localcommunity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class AddPostActivity extends AppCompatActivity {

    static Posts.Post mPost;
    public static Posts.Post getPost(){
        Posts.Post temp = mPost;
        mPost = null;
        return temp;
    }

    private long mCommunityId;

    private TextView mTitleView;
    private TextView mContentView;
    private ImageView mImageView;

    private String mImageKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);
        mImageView = (ImageView) findViewById(R.id.post_img);
        mTitleView = (TextView) findViewById(R.id.post_title);
        mContentView = (TextView) findViewById(R.id.post_content);
        if(getIntent() != null){
            mCommunityId = getIntent().getLongExtra(Constants.ARG_COMMUNITY_ID,0);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null){
            mCommunityId = savedInstanceState.getInt(Constants.ARG_COMMUNITY_ID);
            if(savedInstanceState.containsKey(Constants.ARG_IMAGE)){
                mImageKey = savedInstanceState.getString(Constants.ARG_IMAGE);
                ImageCache.loadAsyncBitmap(mImageKey, new ImageCache.BitmapLoadListener() {
                    @Override
                    public void loadedBitmap(byte[] data) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                        mImageView.setImageBitmap(bitmap);
                    }
                });
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mImageKey != null){
            outState.putString(Constants.ARG_IMAGE,mImageKey);
        }
        outState.putLong(Constants.ARG_COMMUNITY_ID,mCommunityId);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == Constants.REQ_POST_IMAGE && resultCode == RESULT_OK){
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),data.getData());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mImageView.setImageBitmap(bitmap);
                            }
                        });
                        mImageKey = "temp_new_post";
                        ImageCache.cacheBitmap(mImageKey,bitmap,true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
        }
    }

    public void onSetImage(View view){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent,Constants.REQ_POST_IMAGE);
    }

    public void onSubmit(View view){
        boolean exit = false;
        if(mTitleView.getText() == null || mTitleView.getText().toString().isEmpty()){
            mTitleView.setError(getResources().getString(R.string.title_missing));
            exit = true;
        }
        if(mContentView.getText() == null || mContentView.getText().toString().isEmpty()){
            mContentView.setError(getResources().getString(R.string.content_missing));
            exit = true;
        }
        if(exit)return;
        DatabaseReference posts = FirebaseDatabase.getInstance().getReference().child("Posts");
        DatabaseReference ref = posts.push();
        final Posts.Post post = new Posts.Post();
        if(mImageKey != null && !mImageKey.isEmpty()){
            post.image = ref.getKey();
            final StorageReference imgRef = FirebaseStorage.getInstance()
                    .getReference().child("post_images").child(post.image);
            ImageCache.loadAsyncBitmap(mImageKey, new ImageCache.BitmapLoadListener() {
                @Override
                public void loadedBitmap(byte[] data) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                    imgRef.putBytes(data);
                }
            });
        }else{
            post.image = "";
        }
        post.communityId = mCommunityId;
        DateFormat format = DateFormat.getDateTimeInstance();
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        post.date = format.format(new Date(0));
        post.pinned = false;
        post.text = mContentView.getText().toString();
        post.id = ref.getKey();
        post.title = mTitleView.getText().toString();
        post.time = new Date().getTime();
        post.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        final ProgressDialog dialog = ProgressDialog.show(this,getResources().getString(R.string.message_please_wait),
                getResources().getString(R.string.posting_wait_message),
                true);
        ref.setValue(post).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dialog.dismiss();
                Intent intent = new Intent();
                intent.putExtra(Constants.ARG_POST_ID,post.id);
                mPost = post;
                setResult(RESULT_OK,intent);
                finish();
            }
        });
    }

}
