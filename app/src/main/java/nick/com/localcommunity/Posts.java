package nick.com.localcommunity;

import android.support.v4.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Nick on 16-07-03.
 */
public class Posts {

    public static final List<Post> ITEMS = new ArrayList<>();

    public static final Map<String,Post> ITEMS_MAP = new HashMap<>();

    public static void loadPosts(final long community_id,
                                 DatabaseReference postDatabaseReference,
                                 final StorageReference postImages,
                                 final PostListViewAdapter adapter) {
        loadPosts(community_id, postDatabaseReference, postImages, adapter, null);
    }

    public static void loadPosts(final long community_id,
                                 DatabaseReference postDatabaseReference,
                                 final StorageReference postImages,
                                 final PostListViewAdapter adapter,
                                 final SwipeRefreshLayout refreshLayout){
        ITEMS.clear();
        ITEMS_MAP.clear();

        postDatabaseReference.orderByChild("communityId").equalTo(community_id).limitToFirst(10)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot data : dataSnapshot.getChildren()){
                            Post post = data.getValue(Post.class);
                            addItem(post);
                        }
                        adapter.notifyDataSetChanged();
                        if (refreshLayout != null){
                            refreshLayout.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private static void addItem(Post post){
        ITEMS.add(post);
        ITEMS_MAP.put(post.id+"",post);
    }

    public static class Post{
        public String id;
        public String title;
        public String text;
        public boolean pinned;
        public String image;
        public String userId;
        public String date;
        public long communityId;
        public long time;

    }

}
