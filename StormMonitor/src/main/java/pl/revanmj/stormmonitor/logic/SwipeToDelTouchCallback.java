package pl.revanmj.stormmonitor.logic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import pl.revanmj.stormmonitor.R;
import pl.revanmj.stormmonitor.adapters.MainRecyclerViewAdapter;
import pl.revanmj.stormmonitor.data.StormDataProvider;

/**
 * Created by revanmj on 05.02.2016.
 */

public class SwipeToDelTouchCallback extends ItemTouchHelper.Callback {
    // we want to cache these and not allocate anything repeatedly in the onChildDraw method
    Context context;
    Drawable background;
    Drawable deleteIcon;
    int deleteIconMargin;

    public SwipeToDelTouchCallback(Context c) {
        context = c;
        background = new ColorDrawable(Color.RED);
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        deleteIconMargin = (int) context.getResources().getDimension(R.dimen.delete_margin);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    // not important, we don't want drag & drop
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView,
                                RecyclerView.ViewHolder viewHolder) {
        int dragFlags = 0;
        int swipeFlags = ItemTouchHelper.START;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public float getSwipeThreshold (RecyclerView.ViewHolder viewHolder){
        return 0.5f;
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
        int id = ((MainRecyclerViewAdapter.StormDataViewHolder)viewHolder)._id;

        String selection = StormDataProvider.KEY_ID + " = ?";
        String[] selArgs = {Integer.toString(id)};
        int count = context.getContentResolver().delete(StormDataProvider.CONTENT_URI, selection, selArgs);

        if (count > 0)
            Snackbar.make(viewHolder.itemView, "Deleted city succesfully", Snackbar.LENGTH_SHORT);
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;

        // not sure why, but this method get's called for viewholder that are already swiped away
        if (viewHolder.getAdapterPosition() == -1) {
            // not interested in those
            return;
        }

        // draw red background
        background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        background.draw(c);

        if (isCurrentlyActive) {
            // draw x mark when user is swiping the item
            int itemHeight = itemView.getBottom() - itemView.getTop();
            int intrinsicWidth = deleteIcon.getIntrinsicWidth();
            int intrinsicHeight = deleteIcon.getIntrinsicWidth();

            int xMarkLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
            int xMarkRight = itemView.getRight() - deleteIconMargin;
            int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight)/2;
            int xMarkBottom = xMarkTop + intrinsicHeight;

            deleteIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
            deleteIcon.draw(c);
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
