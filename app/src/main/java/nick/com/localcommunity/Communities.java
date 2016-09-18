package nick.com.localcommunity;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
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
 * Created by Nick on 16-07-01.
 */
public class Communities {


    public static final List<Community> COMMUNITIES = new ArrayList<>();
    public static final List<Community> SUBSCRIBED_COMMUNITIES = new ArrayList<>();

    public static final Map<Integer,Community> ITEMS_MAP = new HashMap<>();

    public static void loadCommunities(final String area,
                                       DatabaseReference communityReference,
                                       final StorageReference communityImages,
                                       final CommunityListViewAdapter adapter){
        loadCommunities(area,communityReference,communityImages,adapter,null);
    }

    public static void loadSubscribedCommunities(final String comIds,
                                                 DatabaseReference communityReference,
                                                 final StorageReference communityImages,
                                                 final CommunityListViewAdapter adapter){

    }

    public static void loadCommunities(final String area,
                                       DatabaseReference communityReference,
                                       final StorageReference communityImages,
                                       final CommunityListViewAdapter adapter,
                                       final @Nullable SwipeRefreshLayout refreshLayout){
        COMMUNITIES.clear();
        final long MEGABYTE = 1024*1024;
        communityReference.orderByChild("area").equalTo(area)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot == null)return;
                //final long snapshotSize = dataSnapshot.getChildrenCount();
                Log.v("COMMUNITY", area);
                for(DataSnapshot data : dataSnapshot.getChildren()){
                    final Community c = data.getValue(Community.class);
                    c.imgName = (String)data.child("imgName").getValue();
                    //Log.v("COMMUNITY", c.imgName);
                    addCommunity(c);
                    adapter.notifyDataSetChanged();
                }
                if (refreshLayout != null){
                    refreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        /*for(int i = 0; i < communities.size();i++){
            String comId = communities.get(i);
            final int ci = i;
            communityReference.child(comId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Log.v("COMMUNITY", dataSnapshot.getValue().toString());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }*/
    }

    public static void loadSubscribedCommunities(final String comIds,
                                                 DatabaseReference communityReference,
                                                 final StorageReference communityImages,
                                                 final CommunityListViewAdapter adapter,
                                                 final @Nullable SwipeRefreshLayout refreshLayout){
        SUBSCRIBED_COMMUNITIES.clear();
        final long MEGABYTE = 1024*1024;
        String[] userComs = comIds.split(",");
        for(String com : userComs){
            if(ITEMS_MAP.containsKey(Integer.parseInt(com))){
                SUBSCRIBED_COMMUNITIES.add(ITEMS_MAP.get(Integer.parseInt(com)));
                continue;
            }
            communityReference.orderByChild("id").equalTo(Integer.parseInt(com))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if(dataSnapshot == null)return;
                            //final long snapshotSize = dataSnapshot.getChildrenCount();
                            for(DataSnapshot data : dataSnapshot.getChildren()){
                                final Community c = data.getValue(Community.class);
                                c.imgName = (String)data.child("imgName").getValue();
                                //Log.v("COMMUNITY", c.imgName);
                                addSubscribedCommunity(c);
                                adapter.notifyDataSetChanged();
                            }
                            if (refreshLayout != null){
                                refreshLayout.setRefreshing(false);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
        }

        /*for(int i = 0; i < communities.size();i++){
            String comId = communities.get(i);
            final int ci = i;
            communityReference.child(comId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    //Log.v("COMMUNITY", dataSnapshot.getValue().toString());
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }*/
    }

    private static void addCommunity(Community community){
        COMMUNITIES.add(community);
        ITEMS_MAP.put(community.getId(),community);
    }

    private static void addSubscribedCommunity(Community community){
        SUBSCRIBED_COMMUNITIES.add(community);
        ITEMS_MAP.put(community.getId(),community);
    }

    public static class Community{
        public static final int COMMUNITY_IMAGE_SIZE = 500;


        private int id;
        private String name;
        public String imgName;
        private String description;
        private String area;
        public String owner;

        public Bitmap image;

        public Community(){}

        public Community(int id, String name,String imgUrl, String desc,String owner){
            this.id = id;
            this.name = name;
            this.imgName = imgUrl;
            this.description = desc;
            this.owner = owner;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getArea(){
            return area;
        }

        public void setArea(String a){area = a;}

        @Override
        public String toString() {
            return Community.class.toString() + " " + getName();
        }
    }


}
