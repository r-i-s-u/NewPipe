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
    ALL(0, 0),
    MUSIC(1, 0),
    EDUCATION(2, 0),
    FITNESS(3, 0),
    SPACE(4, 0),
    COMPUTER(5, 0),
    GAMING(6, 0),
    SPORTS(7, 0),
    NEWS(8, 0),
    FAVORITES(9, 0),
    CAR(10, 0),
    MOTORCYCLE(11, 0),
    TREND(12, 0),
    MOVIE(13, 0),
    BACKUP(14, 0),
    ART(15, 0),
    PERSON(16, 0),
    PEOPLE(17, 0),
    MONEY(18, 0),
    KIDS(19, 0),
    FOOD(20, 0),
    SMILE(21, 0),
    EXPLORE(22, 0),
    RESTAURANT(23, 0),
    MIC(24, 0),
    HEADSET(25, 0),
    RADIO(26, 0),
    SHOPPING_CART(27, 0),
    WATCH_LATER(28, 0),
    WORK(29, 0),
    HOT(30, 0),
    CHANNEL(31, 0),
    BOOKMARK(32, 0),
    PETS(33, 0),
    WORLD(34, 0),
    STAR(35, 0),
    SUN(36, 0),
    RSS(37, 0),
    WHATS_NEW(38, 0);

    @DrawableRes
    fun getDrawableRes(): Int {
        return drawableResource
    }
}
