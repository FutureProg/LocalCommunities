package nick.com.localcommunity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
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
 * Activities containing this fragment MUST implement the {@link CommunityListListener}
 * interface.
 */
public class CommunityListFragment extends Fragment {

    private int mColumnCount = 1;
    private String mAreaName;

    private CommunityListListener mListener;
    private CommunityListViewAdapter mListViewAdapter;
    private SwipeRefreshLayout mRefreshLayout;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CommunityListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        if (savedInstanceState != null){
            mAreaName = savedInstanceState.getString("area");
        }else{
            mAreaName = getArguments().getString("area");
        }
        mListViewAdapter = new CommunityListViewAdapter(mAreaName,Communities.COMMUNITIES, mListener, false);
        Communities.loadCommunities(mAreaName,
                ((MainActivity)getActivity()).getCommunityReference(),
                ((MainActivity)getActivity()).getCommunityImageReference(),
                mListViewAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("area",mAreaName);
    }

    @Override
    public void onStart() {
        super.onStart();
        ActionBar actionBar = ((MainActivity)getActivity()).getSupportActionBar();
        if(actionBar != null){
            actionBar.setTitle(R.string.community_actionbar_title);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View re = inflater.inflate(R.layout.fragment_community_list, container, false);

        mRefreshLayout = (SwipeRefreshLayout)re.findViewById(R.id.swipe_refresh_layout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Communities.loadCommunities(mAreaName,
                        ((MainActivity)getActivity()).getCommunityReference(),
                        ((MainActivity)getActivity()).getCommunityImageReference(),
                        mListViewAdapter,
                        mRefreshLayout);
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
            recyclerView.setAdapter(mListViewAdapter);
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


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ActionBar actionBar = ((MainActivity)context).getSupportActionBar();
        if(actionBar != null)
            actionBar.setTitle(R.string.community_actionbar_title);
        if (context instanceof CommunityListListener) {
            mListener = (CommunityListListener) context;
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
    public interface CommunityListListener {
        void onCommunityListInteraction(Communities.Community item, int index, boolean openActivity);
        void onCommunityAddDialog();
    }
}
