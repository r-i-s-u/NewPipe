package org.schabi.newpipe.error

enum class UserAction(val message: String) {
    SOMETHING_ELSE("something else"),
    GETTING_MAIN_SCREEN_TAB("getting main screen tab"),
    REQUESTED_BOOKMARK("requested bookmark"),
    REQUESTED_STREAM("requested stream"),
    REQUESTED_CHANNEL("requested channel"),
    REQUESTED_PLAYLIST("requested playlist"),
    REQUESTED_COMMENTS("requested comments"),
    REQUESTED_FEED("requested feed"),
    SEARCHED("searched"),
    UI_ERROR("ui error"),
    SUBSCRIPTION_IMPORT_EXPORT("subscription import export"),
    SUBSCRIPTION_GET("subscription get"),
    APP_UPDATE("app update"),
    SHARE_TO_NEWPIPE("share to newpipe"),
    OPEN_INFO_ITEM_DIALOG("open info item dialog"),
    DELETE_FROM_HISTORY("delete from history"),
    GET_SUGGESTIONS("get suggestions"),
    DOWNLOAD_OPEN_DIALOG("download open dialog"),
    DOWNLOAD_FAILED("download failed"),
    DATABASE_IMPORT_EXPORT("database import export"),
    CHECK_FOR_NEW_APP_VERSION("check for new app version")
}
