package nick.com.localcommunity.UtilityViews;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by Nick on 16-07-02.
 */
public class SquareImageView extends ImageView {

    public SquareImageView(Context context) {
        super(context);
    }

    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = getMeasuredHeight();
        if(height > 0){
            setMeasuredDimension(height, height);
            setMaxHeight(height);
            setMaxWidth(height);
        }else{
            int width = getMeasuredWidth();
            setMeasuredDimension(width,width);
            setMaxHeight(width);
            setMaxWidth(width);
        }
    }

}
