package nick.com.localcommunity.UtilityViews;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import nick.com.localcommunity.Constants;
import nick.com.localcommunity.ImageCache;
import nick.com.localcommunity.R;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ImageViewerActivity extends AppCompatActivity {

    String mImageKey;
    PhotoViewAttacher mAttacher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        getSupportActionBar().hide();
        mImageKey = getIntent().getStringExtra(Constants.ARG_IMAGE);
        final ImageView imageView =((ImageView)findViewById(R.id.imageView));
        mAttacher = new PhotoViewAttacher(imageView);
        ImageCache.loadAsyncBitmap(mImageKey, new ImageCache.BitmapLoadListener() {
            @Override
            public void loadedBitmap(byte[] data) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                imageView.setImageBitmap(bitmap);
                mAttacher.update();
            }
        });
    }
}
