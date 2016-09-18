package nick.com.localcommunity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

import nick.com.localcommunity.dummy.DummyContent.DummyItem;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyItem} and makes a call to the
 * specified {@link nick.com.localcommunity.CommentFragment.CommentInteractionListener}.
 */
public class CommentRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_COMMENT = 1;

    private final List<Comments.Comment> mValues;
    private final String mTitle;
    private final String mImage;
    private final CommentFragment.CommentInteractionListener mListener;

    public CommentRecyclerViewAdapter(String title, String image, List<Comments.Comment> items, CommentFragment.CommentInteractionListener listener) {
        mTitle = title;
        mImage = image;
        mValues = items;
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_HEADER){
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.post_header, parent, false);
            return new HeaderHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment, parent, false);
        return new CommentHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if(holder.getItemViewType() == TYPE_HEADER){
            ((HeaderHolder)holder).mTitleView.setText(mTitle);
            /*if(mImage != null && !mImage.isEmpty()){
                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference ref = storage.getReference();
                if(ImageCache.inCache("temp_post_img_"+mImage)){
                    ImageCache.loadAsyncBitmap("temp_post_img_" + mImage, new ImageCache.BitmapLoadListener() {
                        @Override
                        public void loadedBitmap(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                            ((HeaderHolder)holder).mPostedImageView.setImageBitmap(bitmap);
                        }
                    });
                }else{
                    ref.child("post_images").child(mImage).getBytes(Constants.MAX_PROFILE_IMAGE_SIZE)
                            .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                @Override
                                public void onSuccess(byte[] bytes) {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                                    ((HeaderHolder)holder).mPostedImageView.setImageBitmap(bitmap);
                                    ImageCache.cacheBitmap("temp_post_img_" + mImage,bitmap);
                                }
                            });
                }
            }*/
        }
        ((CommentHolder)holder).mItem = mValues.get(position);
        final Comments.Comment comment = mValues.get(position);
        ((CommentHolder)holder).mTextView.setText(comment.message);
        if(comment.username == null || comment.username.isEmpty()){
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users");
            ref.orderByChild("id").equalTo(comment.userid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Log.v("FOUND",dataSnapshot.toString());
                    for(DataSnapshot data : dataSnapshot.getChildren())
                        comment.username = (String)data.child("username").getValue();
                    ((CommentHolder)holder).mUsernameView.setText(comment.username);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }else{
            ((CommentHolder)holder).mUsernameView.setText(comment.username);
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference ref = storage.getReference();
        final String key = "temp_comment_"+((CommentHolder)holder).mItem.userid;
        if(ImageCache.inCache(key)){
            ImageCache.loadAsyncBitmap(key, new ImageCache.BitmapLoadListener() {
                @Override
                public void loadedBitmap(byte[] bytes) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    ((CommentHolder)holder).mImageView.setImageBitmap(bitmap);
                }
            });
        }else{
            ref.child("user_images").child(((CommentHolder)holder).mItem.userid).getBytes(Constants.MAX_PROFILE_IMAGE_SIZE)
                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                        @Override
                        public void onSuccess(byte[] bytes) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                            ((CommentHolder)holder).mImageView.setImageBitmap(bitmap);
                            ImageCache.cacheBitmap(key,bitmap);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //TODO: Set the userimage to the default one
                }
            });
        }

        ((CommentHolder)holder).mView.findViewById(R.id.userbar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onCommentInteraction(((CommentHolder)holder).mItem);
                }
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return TYPE_HEADER;
        }
        return TYPE_COMMENT;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class HeaderHolder extends CommentHolder{
        public final ImageView mPostedImageView;
        public final TextView mTitleView;

        public HeaderHolder(View view) {
            super(view);
            mPostedImageView = (ImageView)view.findViewById(R.id.imageView);
            mTitleView = (TextView)view.findViewById(R.id.title);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + "'";
        }
    }

    public class CommentHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTextView;
        public final TextView mUsernameView;
        public final ImageView mImageView;
        public Comments.Comment mItem;

        public CommentHolder(View view) {
            super(view);
            mView = view;
            mImageView = (ImageView)view.findViewById(R.id.user_image);
            mTextView = (TextView) view.findViewById(R.id.comment_text);
            mUsernameView = (TextView) view.findViewById(R.id.username);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + "'";
        }
    }
}
