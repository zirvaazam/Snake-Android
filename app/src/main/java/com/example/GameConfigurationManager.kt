package com.example

import android.content.Context

class GameConfigurationManager(private val context: Context) {
    val gameTitleDisplay: String
        get() = context.getString(R.string.game_title_display)
    
    val gamePackageIdentifier: String
        get() = context.getString(R.string.game_package_identifier)
    
    val gameVersionCode: Int
        get() = context.resources.getInteger(R.integer.game_version_code)
    
    val gameVersionName: String
        get() = context.getString(R.string.game_version_name)
    
    val developerCreditString: String
        get() = context.getString(R.string.developer_credit_string)
}
