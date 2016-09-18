package nick.com.localcommunity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;

import nick.com.localcommunity.UtilityViews.SimpleDividerItemDecoration;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnPostListInteractionListener}
 * interface.
 */
public class PostListFragment extends Fragment {

    public static String ARG_COMMUNITY_ID = "CommunityID",
                        ARG_COMMUNITY_NAME = "CommunityName";

    private int mColumnCount = 1;

    private OnPostListInteractionListener mListener;
    private PostListViewAdapter mAdapter;
    private SwipeRefreshLayout mRefreshLayout;

    private long mCommunityId;
    private String mCommunityName;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PostListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            mCommunityId = getArguments().getLong(ARG_COMMUNITY_ID);
            mCommunityName = getArguments().getString(ARG_COMMUNITY_NAME);
        }
        mAdapter = new PostListViewAdapter(Posts.ITEMS, mListener);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if(savedInstanceState != null){
            mCommunityId = savedInstanceState.getLong(ARG_COMMUNITY_ID);
            mCommunityName = savedInstanceState.getString(ARG_COMMUNITY_NAME);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ARG_COMMUNITY_ID,mCommunityId);
        outState.putString(ARG_COMMUNITY_NAME,mCommunityName);
    }

    @Override
    public void onStart() {
        super.onStart();
        Posts.loadPosts(mCommunityId,
                ((MainActivity)getActivity()).getPostReference(),
                ((MainActivity)getActivity()).getPostImageReference(),
                mAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View re = inflater.inflate(R.layout.fragment_post_list, container, false);

        mRefreshLayout = (SwipeRefreshLayout)re.findViewById(R.id.swipe_refresh_layout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Posts.loadPosts(mCommunityId,
                        ((MainActivity)getActivity()).getPostReference(),
                        ((MainActivity)getActivity()).getPostImageReference(),
                        mAdapter,mRefreshLayout);
            }
        });
        mRefreshLayout.setEnabled(true);

        View view = re.findViewById(R.id.list);
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            recyclerView.setAdapter(mAdapter);
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
            if(!FirebaseAuth.getInstance().getCurrentUser().isAnonymous()) {
                ((MainActivity) getActivity()).getFab().attachToRecyclerView(recyclerView);
                ((MainActivity) getActivity()).getFab().setTag(this.getClass().getSimpleName());
                ((MainActivity) getActivity()).getFab().show();
            }else{
                ((MainActivity) getActivity()).getFab().hide(false);
            }
        }
        return re;
    }

    public long getCommunityId(){
        return mCommunityId;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPostListInteractionListener) {
            mListener = (OnPostListInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnPostListInteractionListener {
        void onPostClick(Posts.Post post, int index);
        boolean onPostLongPress(PostListViewAdapter adapter, Posts.Post post, int index);
    }
}
