package nick.com.localcommunity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import nick.com.localcommunity.UtilityViews.SimpleDividerItemDecoration;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link CommentInteractionListener}
 * interface.
 */
public class CommentFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";

    private int mColumnCount = 1;
    private String mPostId;
    private Posts.Post mPost;
    private int mPostIndex;
    private String mPostImage;
    private String mPostTitle;
    private String mCommentFragment;
    private CommentInteractionListener mListener;
    private CommentRecyclerViewAdapter mAdapter;
    private EditText mEditText;
    private SwipeRefreshLayout mRefreshLayout;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CommentFragment() {
    }

    public static CommentFragment newInstance(int columnCount, String postId, Posts.Post post, int postIndex, String title, String image) {
        CommentFragment fragment = new CommentFragment();
        fragment.mPost = post;
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        args.putString(Constants.ARG_POST_ID,postId);
        if(postIndex >= 0){
            args.putInt(Constants.ARG_INDEX, postIndex);
        }
        args.putString(Constants.ARG_POST_IMAGE, image);
        args.putString(Constants.ARG_POST_TITLE,title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
            mPostId = getArguments().getString(Constants.ARG_POST_ID);
            if(getArguments().containsKey(Constants.ARG_INDEX))
                mPostIndex = getArguments().getInt(Constants.ARG_INDEX);
        }
        mAdapter = new CommentRecyclerViewAdapter(mPost.title,mPost.image,Comments.ITEMS, mListener);
        Comments.loadComments(mPostId,mPost,
                ((MainActivity)getActivity()).getCommentsReference(),
                ((MainActivity)getActivity()).getUserReference(),
                ((MainActivity)getActivity()).getPostImageReference(),
                mAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View re = inflater.inflate(R.layout.fragment_comment_list, container, false);

        mRefreshLayout = (SwipeRefreshLayout)re.findViewById(R.id.swipe_refresh_layout);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Comments.loadComments(mPostId,mPost,
                        ((MainActivity)getActivity()).getCommentsReference(),
                        ((MainActivity)getActivity()).getUserReference(),
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
            ((MainActivity) getActivity()).getFab().hide(true);
        }
        mEditText = (EditText)re.findViewById(R.id.edit_text);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEND){
                    String txt = textView.getText().toString();
                    if(txt == null || txt.isEmpty())return false;
                    mListener.onPostComment(mPost,txt);
                    textView.setText("");
                    return true;
                }
                return false;
            }
        });
        return re;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CommentInteractionListener) {
            ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
            if(actionBar != null)actionBar.setTitle(R.string.post);
            mListener = (CommentInteractionListener) context;
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
    public interface CommentInteractionListener {
        void onCommentInteraction(Comments.Comment item);
        void onPostComment(Posts.Post post, String content);
    }
}
