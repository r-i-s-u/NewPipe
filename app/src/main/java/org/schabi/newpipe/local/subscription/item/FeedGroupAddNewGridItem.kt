package org.schabi.newpipe.local.subscription.item

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import org.schabi.newpipe.R
import org.schabi.newpipe.databinding.FeedGroupAddNewGridItemBinding

class FeedGroupAddNewGridItem : BindableItem<FeedGroupAddNewGridItemBinding>() {
    override fun getLayout(): Int = 0
    override fun initializeViewBinding(view: View) = FeedGroupAddNewGridItemBinding.bind(view)
    override fun bind(viewBinding: FeedGroupAddNewGridItemBinding, position: Int) {
        // this is a static item, nothing to do here
    }
}
