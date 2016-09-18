package nick.com.localcommunity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NetworkFragmentListener} interface
 * to handle interaction events.
 */
public class NoNetworkFragment extends Fragment implements View.OnClickListener{

    private NetworkFragmentListener mListener;

    public NoNetworkFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View re = inflater.inflate(R.layout.fragment_no_network, container, false);
        /*ConnectivityManager manager = (ConnectivityManager)getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        ImageView imageView = (ImageView)re.findViewById(R.id.imageView);
        if(info == null){
            imageView.setImageResource(R.drawable.ic_signal_wifi_off_black_24dp);
        }
        else if(info.getState() == NetworkInfo.State.DISCONNECTED ||
                info.getState() == NetworkInfo.State.UNKNOWN){
            imageView.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_24dp);
        }*/
        re.setOnClickListener(this);

        return re;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof NetworkFragmentListener) {
            mListener = (NetworkFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        if(mListener != null){
            mListener.onNetworkRequested();
        }
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
    public interface NetworkFragmentListener {
        void onNetworkRequested();
    }
}
