package com.yelp.android.bento.core;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.yelp.android.bento.R;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link Component} for displaying homogeneous lists of data all using the same presenter object
 * and {@link ComponentViewHolder} with support for showing dividers.
 *
 * @param <P> Presenter to attach for each list item.
 * @param <T> {@link ComponentViewHolder} type to use for each list item.
 */
public class ListComponent<P, T> extends Component {

    private final List<T> mData = new ArrayList<>();
    private final P mPresenter;
    private final Class<? extends ComponentViewHolder> mListItemViewHolder;
    private boolean mShouldShowDivider = true;
    private Class<? extends DividerViewHolder> mDividerViewHolder = DefaultDividerViewHolder.class;

    public ListComponent(
            P presenter, Class<? extends ComponentViewHolder<P, T>> listItemViewHolder) {
        mPresenter = presenter;
        mListItemViewHolder = listItemViewHolder;
    }

    public void setData(@NonNull List<T> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataChanged();
    }

    public void appendData(@NonNull List<T> data) {
        int oldSize = mData.size();
        int sizeChange = data.size();
        mData.addAll(data);
        notifyItemRangeInserted(oldSize, sizeChange);
    }

    public void removeData(T object) {
        int index = mData.indexOf(object);
        // Check if the object indeed is in the list.
        if (index != -1) {
            mData.remove(index);
            notifyItemRangeRemoved(getRemoveIndexStart(index), getRemoveItemCount());
        }
    }

    public void toggleDivider(boolean shouldShowDivider) {
        mShouldShowDivider = shouldShowDivider;
        notifyDataChanged();
    }

    public void setDividerViewHolder(
            @NonNull Class<? extends DividerViewHolder> dividerViewHolder) {
        mDividerViewHolder = dividerViewHolder;
        notifyDataChanged();
    }

    /**
     * When removing an item, we must also remove the divider if there is one. In general, we remove
     * the divider that is above the item. The exception to this is when we remove the first item as
     * there is no divider above it. In this case, remove the divider below it. Unless, of course,
     * it is the only item in the list. Then just remove the item. This method calculate the
     * starting index based on the item index in the list of data, not including dividers.
     *
     * @param index The index of the item in mData. Not including dividers.
     * @return An int representing the starting point of the data to remove.
     * @see #getRemoveItemCount()
     */
    @VisibleForTesting
    int getRemoveIndexStart(int index) {
        // If there is no divider or it is the first item, just return the index.
        // If there is divider, multiply by 2 to account for all other dividers.
        return mShouldShowDivider && index != 0 ? (index * 2) - 1 : index;
    }

    /**
     * Gets the number of items to remove. See {@link ListComponent#getRemoveIndexStart(int)} for
     * more information.
     *
     * @return An int representing the number of items to delete. Either 1 or 2 if we need to remove
     *     a divider.
     */
    @VisibleForTesting
    int getRemoveItemCount() {
        return mShouldShowDivider && mData.size() != 1 ? 2 : 1;
    }

    /**
     * Overridable method that is called each time a list data item is retrieved by {@link
     * #getItem}.
     *
     * @param position Index of the data item.
     */
    @CallSuper
    protected void onGetListItem(int position) {}

    @Override
    public Object getItem(int position) {
        if (mShouldShowDivider) {
            return isListItem(position) ? getListItem(position / 2) : null;
        }

        return getListItem(position);
    }

    @Override
    public P getPresenter(int position) {
        return mPresenter;
    }

    @Override
    public int getCount() {
        return mShouldShowDivider ? getTotalSizeWithSeparators(mData.size()) : mData.size();
    }

    @NonNull
    @Override
    public Class<? extends ComponentViewHolder> getHolderType(int position) {
        return isListItem(position) ? mListItemViewHolder : mDividerViewHolder;
    }

    @Override
    public SpanSizeLookup getSpanSizeLookup() {
        if (mSpanSizeLookup == null) {
            setSpanSizeLookup(super.getSpanSizeLookup());
        }
        return mSpanSizeLookup;
    }

    /**
     * Sets the {@link SpanSizeLookup} to use when getting the widths of the cells. This method will
     * take padding into account.
     * @param spanSizeLookup The new {@link SpanSizeLookup} to add.
     */
    @Override
    public void setSpanSizeLookup(final SpanSizeLookup spanSizeLookup) {
        mSpanSizeLookup = new SpanSizeLookup() {
            // If there is a gap in the list component, we want it to span the entire width.
            // Otherwise, return the requested span size lookup.
            @Override
            public int getSpanSize(int position) {
                if (hasGap(position)) {
                    return getNumberColumns();
                }
                return spanSizeLookup.getSpanSize(position - getPositionOffset());
            }
        };
    }

    private T getListItem(int position) {
        onGetListItem(position);
        return mData.get(position);
    }

    private boolean isListItem(int position) {
        return !mShouldShowDivider || position % 2 == 0;
    }

    private int getTotalSizeWithSeparators(int size) {
        return size == 0 ? 0 : size * 2 - 1;
    }

    @SuppressWarnings("WeakerAccess") // Required to be public for instantiation by reflection
    public abstract static class DividerViewHolder extends ComponentViewHolder {

        @Override
        public final void bind(Object presenter, Object element) {
            // Force do nothing.
        }
    }

    @SuppressWarnings("WeakerAccess") // Required to be public for instantiation by reflection
    public static class DefaultDividerViewHolder extends DividerViewHolder {

        @NonNull
        @Override
        public View inflate(@NonNull ViewGroup parent) {
            return LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_divider_default, parent, false);
        }
    }
}
