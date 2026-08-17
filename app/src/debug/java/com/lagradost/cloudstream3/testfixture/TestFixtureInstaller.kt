package com.lagradost.cloudstream3.testfixture

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.lagradost.cloudstream3.APIHolder
import com.lagradost.cloudstream3.MainAPI

/**
 * Registers [TestFixtureProvider] on startup.
 *
 * A ContentProvider is used purely because the system creates one before
 * Application.onCreate, which lets the fixture install itself without any hook
 * in the main source set. This class exists only in the debug source set, so
 * release builds neither contain it nor the fixture.
 */
class TestFixtureInstaller : ContentProvider() {
    override fun onCreate(): Boolean {
        val fixture: MainAPI = TestFixtureProvider()
        // allProviders backs the settings list, apis backs what the UI can pick.
        APIHolder.allProviders.add(fixture)
        APIHolder.addPluginMapping(fixture)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
