package nick.com.localcommunity;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 */
public class SubscriptionsFragment extends Fragment {


    private CommunityListFragment.CommunityListListener mListener;
    private CommunityListViewAdapter mListViewAdapter;
    private SwipeRefreshLayout mRefreshLayout;
    private String mCommunityIds;

    public SubscriptionsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO: Get community ids from the bundle
        mListViewAdapter = new CommunityListViewAdapter("",Communities.SUBSCRIBED_COMMUNITIES,mListener,true);
        Communities.loadCommunities(mCommunityIds,
                ((MainActivity)getActivity()).getCommunityReference(),
                ((MainActivity)getActivity()).getCommunityImageReference(),
                mListViewAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_my_communities, container, false);
    }

}
