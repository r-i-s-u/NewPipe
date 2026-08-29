package org.schabi.newpipe.local.subscription.item

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import org.schabi.newpipe.R
import org.schabi.newpipe.databinding.FeedGroupAddNewItemBinding

class FeedGroupAddNewItem : BindableItem<FeedGroupAddNewItemBinding>() {
    override fun getLayout(): Int = 0
    override fun initializeViewBinding(view: View) = FeedGroupAddNewItemBinding.bind(view)
    override fun bind(viewBinding: FeedGroupAddNewItemBinding, position: Int) {
        // this is a static item, nothing to do here
    }
}
