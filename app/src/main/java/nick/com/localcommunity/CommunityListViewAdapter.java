package nick.com.localcommunity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;

import java.util.List;


/**
 * {@link RecyclerView.Adapter} that can display a {@link nick.com.localcommunity.Communities.Community} and makes a call to the
 * specified {@link CommunityListFragment.CommunityListListener}.
 */
public class CommunityListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<Communities.Community> mValues;
    private final CommunityListFragment.CommunityListListener mListener;

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_VIEW = 1;
    String mArea;
    boolean mOpenNewActivity;

    public CommunityListViewAdapter(String area, List<Communities.Community> items,
                                    CommunityListFragment.CommunityListListener listener, boolean openActivity) {
        mArea = area;
        mValues = items;
        mListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if(viewType == TYPE_VIEW) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_community, parent, false);
            return new ContentHolder(view);
        }
        else if(viewType == TYPE_HEADER){
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.fragment_community_header,parent,false);
            return new HeaderHolder(view);
        }
        throw new RuntimeException("there is not type that matches the type : " + viewType);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        Log.v("UPDATE_LIST","updating at position" + position
            + "for holder type " + holder.getItemViewType());
        if(holder.getItemViewType() == TYPE_HEADER){
            ((HeaderHolder)holder).mTextView.setText(mArea);
            ((HeaderHolder)holder).mAreaName = mArea;
        }else if(holder.getItemViewType() == TYPE_VIEW){
            ((ContentHolder)holder).mItem = mValues.get(position-1);
            ((ContentHolder)holder).mNameView.setText(mValues.get(position-1).getName()+"");
            ((ContentHolder)holder).mDescView.setText(mValues.get(position-1).getDescription());
            //Load community image
            if(ImageCache.inCache("temp_community_image_"+((ContentHolder)holder).mItem.getId())){
                ImageCache.loadAsyncBitmap("temp_community_image_" + ((ContentHolder) holder)
                        .mItem.getId(),
                        new ImageCache.BitmapLoadListener() {
                            @Override
                            public void loadedBitmap(byte[] data) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
                                ((ContentHolder)holder).mItem.image = bitmap;
                                ((ContentHolder)holder).mImgView.setImageBitmap(bitmap);
                            }
                        });
            }else{
                FirebaseStorage.getInstance().getReference().child("community_images")
                        .child(((ContentHolder)holder).mItem.imgName)
                        .getBytes(1024*1024*2)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                                ImageCache.cacheBitmap(
                                        "temp_community_image_"+((ContentHolder)holder)
                                                .mItem.getId(),
                                        bitmap);
                                ((ContentHolder)holder).mItem.image = bitmap;
                                ((ContentHolder)holder).mImgView.setImageBitmap(bitmap);
                            }
                        });
            }
            ((ContentHolder)holder).mImgView.setImageBitmap(mValues.get(position-1).image);
            ((ContentHolder)holder).mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (null != mListener) {
                        // Notify the active callbacks interface (the activity, if the
                        // fragment is attached to one) that an item has been selected.
                        mListener.onCommunityListInteraction(((ContentHolder)holder).mItem, position-1, mOpenNewActivity);
                    }
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return mValues.size()+1;
    }



    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return TYPE_HEADER;
        }
        return TYPE_VIEW;
    }

    public class ContentHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mNameView;
        public final TextView mDescView;
        public final ImageView mImgView;
        public Communities.Community mItem;

        public ContentHolder(View view) {
            super(view);
            mView = view;
            mNameView = (TextView) view.findViewById(R.id.community_name);
            mDescView = (TextView) view.findViewById(R.id.community_desc);
            mImgView = (ImageView)view.findViewById(R.id.community_img);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }
    }

    public class HeaderHolder extends RecyclerView.ViewHolder{

        public final View mView;
        public final TextView mTextView;
        public String mAreaName;

        public HeaderHolder(View view){
            super(view);
            mView = view;
            mTextView = (TextView)view.findViewById(R.id.area_name);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + "'";
        }

    }
}
