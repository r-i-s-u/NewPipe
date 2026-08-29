package org.schabi.newpipe.local.subscription.item

import android.view.View
import androidx.annotation.DrawableRes
import com.xwray.groupie.viewbinding.BindableItem
import org.schabi.newpipe.R
import org.schabi.newpipe.databinding.PickerIconItemBinding
import org.schabi.newpipe.local.subscription.FeedGroupIcon

class PickerIconItem(
    val icon: FeedGroupIcon
) : BindableItem<PickerIconItemBinding>() {
    @DrawableRes
    val iconRes: Int = icon.getDrawableRes()

    override fun getLayout(): Int = 0

    override fun bind(viewBinding: PickerIconItemBinding, position: Int) {
        viewBinding.iconView.setImageResource(iconRes)
    }

    override fun initializeViewBinding(view: View) = PickerIconItemBinding.bind(view)
}
