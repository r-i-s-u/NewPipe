package org.schabi.newpipe.local.subscription

import androidx.annotation.DrawableRes
import org.schabi.newpipe.R

enum class FeedGroupIcon(
    /**
     * The id that will be used to store and retrieve icons from some persistent storage (e.g. DB).
     */
    val id: Int,

    /**
     * The drawable resource.
     */
    @DrawableRes val drawableResource: Int
) {
    ALL(0, R.drawable.ic_asterisk),
    MUSIC(1, R.drawable.ic_asterisk),
    EDUCATION(2, R.drawable.ic_asterisk),
    FITNESS(3, R.drawable.ic_asterisk),
    SPACE(4, R.drawable.ic_asterisk),
    COMPUTER(5, R.drawable.ic_asterisk),
    GAMING(6, R.drawable.ic_asterisk),
    SPORTS(7, R.drawable.ic_asterisk),
    NEWS(8, R.drawable.ic_asterisk),
    FAVORITES(9, R.drawable.ic_asterisk),
    CAR(10, R.drawable.ic_asterisk),
    MOTORCYCLE(11, R.drawable.ic_asterisk),
    TREND(12, R.drawable.ic_asterisk),
    MOVIE(13, R.drawable.ic_asterisk),
    BACKUP(14, R.drawable.ic_asterisk),
    ART(15, R.drawable.ic_asterisk),
    PERSON(16, R.drawable.ic_asterisk),
    PEOPLE(17, R.drawable.ic_asterisk),
    MONEY(18, R.drawable.ic_asterisk),
    KIDS(19, R.drawable.ic_asterisk),
    FOOD(20, R.drawable.ic_asterisk),
    SMILE(21, R.drawable.ic_asterisk),
    EXPLORE(22, R.drawable.ic_asterisk),
    RESTAURANT(23, R.drawable.ic_asterisk),
    MIC(24, R.drawable.ic_asterisk),
    HEADSET(25, R.drawable.ic_asterisk),
    RADIO(26, R.drawable.ic_asterisk),
    SHOPPING_CART(27, R.drawable.ic_asterisk),
    WATCH_LATER(28, R.drawable.ic_asterisk),
    WORK(29, R.drawable.ic_asterisk),
    HOT(30, R.drawable.ic_asterisk),
    CHANNEL(31, R.drawable.ic_asterisk),
    BOOKMARK(32, R.drawable.ic_asterisk),
    PETS(33, R.drawable.ic_asterisk),
    WORLD(34, R.drawable.ic_asterisk),
    STAR(35, R.drawable.ic_asterisk),
    SUN(36, R.drawable.ic_asterisk),
    RSS(37, R.drawable.ic_asterisk),
    WHATS_NEW(38, R.drawable.ic_asterisk);

    @DrawableRes
    fun getDrawableRes(): Int {
        return drawableResource
    }
}
