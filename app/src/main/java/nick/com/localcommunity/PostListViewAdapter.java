package nick.com.localcommunity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;

import nick.com.localcommunity.UtilityViews.ImageViewerActivity;


/**
 * {@link RecyclerView.Adapter} that can display a {@link nick.com.localcommunity.Posts.Post} and makes a call to the
 * specified {@link nick.com.localcommunity.PostListFragment.OnPostListInteractionListener}.
 */
public class PostListViewAdapter extends RecyclerView.Adapter<PostListViewAdapter.ViewHolder> {

    private final List<Posts.Post> mValues;
    private final PostListFragment.OnPostListInteractionListener mListener;

    public PostListViewAdapter(List<Posts.Post> items, PostListFragment.OnPostListInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_post, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.mItem = mValues.get(position);
        holder.mTitleView.setText(mValues.get(position).title);
        String txt = mValues.get(position).text;
        //txt = txt.substring(0,Math.min(80,txt.length()));

        holder.mContentView.setText(txt);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onPostClick(holder.mItem,position);
                }
            }
        });
        final PostListViewAdapter thisadapter = this;
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return mListener.onPostLongPress(thisadapter,holder.mItem,position);
            }
        });
        if(holder.mItem.image != null && !holder.mItem.image.isEmpty()){
            if(ImageCache.inCache("temp_post_image_"+holder.mItem.image)){
                ImageCache.loadAsyncBitmap("temp_post_image_" + holder.mItem.image
                        , new ImageCache.BitmapLoadListener() {
                    @Override
                    public void loadedBitmap(byte[] data) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                        holder.mImageView.setImageBitmap(bitmap);
                    }
                });
            }else{
                FirebaseStorage.getInstance().getReference().child("post_images")
                        .child(holder.mItem.image)
                        .getBytes(1024*1024*2)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                                ImageCache.cacheBitmap("temp_post_image_"+holder.mItem.image,bitmap,true);
                                holder.mImageView.setImageBitmap(bitmap);
                            }
                        });
            }
            holder.mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
                    intent.putExtra(Constants.ARG_IMAGE,"temp_post_image_"+holder.mItem.image);
                    v.getContext().startActivity(intent);
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mTitleView;
        public final TextView mContentView;
        public final ImageView mImageView;
        public Posts.Post mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mTitleView = (TextView) view.findViewById(R.id.post_title);
            mContentView = (TextView) view.findViewById(R.id.post_preview_text);
            mImageView = (ImageView)view.findViewById(R.id.post_img);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
