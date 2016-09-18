package nick.com.localcommunity;

import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;

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
 * Created by Nick on 16-07-09.
 */
public class Comments {

    public static final List<Comment> ITEMS = new ArrayList<>();

    public static final Map<String,Comment> ITEMS_MAP = new HashMap<>();


    public static void loadComments(final String post_id,
                                    final Posts.Post post,
                                    DatabaseReference commentsDBReference,
                                    final DatabaseReference userDBRef,
                                    final StorageReference postImages,
                                    final CommentRecyclerViewAdapter adapter) {
        loadComments(post_id, post, commentsDBReference, userDBRef, postImages, adapter, null);
    }

    public static void loadComments(final String post_id,
                                    final Posts.Post post,
                                    DatabaseReference commentsDBReference,
                                    final DatabaseReference userDBRef,
                                    final StorageReference postImages,
                                    final CommentRecyclerViewAdapter adapter,
                                    final SwipeRefreshLayout refreshLayout){

        ITEMS.clear();
        ITEMS_MAP.clear();

        Comment c = new Comment();
        c.id = null;
        c.userid = post.userId;
        c.message = post.text;
        c.date = post.date;
        c.postid = post_id;
        c.loadData(userDBRef);
        addItem(c);
        commentsDBReference.orderByChild("postid").equalTo(post_id).limitToFirst(10)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(final DataSnapshot data : dataSnapshot.getChildren()){
                            final Comment comment = data.getValue(Comment.class);
                            /*userDBRef.orderByChild("id")
                                    .equalTo(comment.userid)
                                    .getRef()
                                    .child(comment.userid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for (DataSnapshot data : dataSnapshot.getChildren()){
                                        comment.username = (String)dataSnapshot.child("username").getValue();
                                    }*/
                            addItem(comment);

                                /*}

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });*/
                            //comment.loadData(userDBRef);
                            adapter.notifyDataSetChanged();
                        }
                        if(refreshLayout != null){
                            refreshLayout.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private static void addItem(Comment comment){
        ITEMS.add(comment);
        ITEMS_MAP.put(comment.id+"",comment);
    }


    public static class Comment{
        String id;
        String postid;
        String userid;
        String message;
        String date;
        String username;

        public void loadData(DatabaseReference ref){
            Log.d("Comment Load","Loading comment by user " + userid);
            ref.orderByChild("id").equalTo(userid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Log.d("Comment Load","Got " + dataSnapshot.toString() + " for " + userid);
                    for (DataSnapshot data : dataSnapshot.getChildren()){
                        username = (String)data.child("username").getValue();
                    }
                    //Log.v("USERNAME_LOAD","found " + dataSnapshot.toString());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

}
