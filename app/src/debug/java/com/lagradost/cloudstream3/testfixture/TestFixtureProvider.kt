package com.lagradost.cloudstream3.testfixture

import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink

/**
 * Offline provider that serves fixed, made up entries. Debug builds only.
 *
 * It exists so the result screen, the episode list and the focus behaviour can
 * be exercised without installing a content repository. Because the data never
 * changes, before/after screenshots of a layout change are comparable.
 *
 * The entries deliberately cover the cases that break TV layouts: a very long
 * title, a season with many episodes, and missing artwork.
 */
class TestFixtureProvider : MainAPI() {
    override var name = "Test Fixture"
    override var mainUrl = FIXTURE_URL
    override var lang = "en"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$FIXTURE_URL/short" to "Short season",
        "$FIXTURE_URL/long" to "Long season",
        "$FIXTURE_URL/edge" to "Edge cases",
    )

    private data class Show(
        val slug: String,
        val title: String,
        val seasons: Int,
        val episodesPerSeason: Int,
        val hasPoster: Boolean,
    )

    private val shows = listOf(
        Show("short", "Three Episodes", 1, 3, true),
        Show("long", "Two Hundred Episodes", 1, 200, true),
        Show("seasons", "Six Seasons And A Movie", 6, 12, true),
        Show(
            "longtitle",
            "A Title So Long That It Has To Wrap Or Be Truncated Somewhere " +
                "On A Nineteen Twenty By Ten Eighty Panel",
            1, 8, true
        ),
        Show("noart", "No Artwork At All", 1, 5, false),
    )

    private fun Show.toSearchResponse(): SearchResponse =
        newTvSeriesSearchResponse(title, "$FIXTURE_URL/$slug", TvType.TvSeries) {
            this.posterUrl = if (hasPoster) POSTER_URL else null
        }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse =
        newHomePageResponse(
            request,
            when (request.data) {
                "$FIXTURE_URL/short" -> shows.filter { it.episodesPerSeason <= 12 }
                "$FIXTURE_URL/long" -> shows.filter { it.episodesPerSeason > 12 }
                else -> shows
            }.map { it.toSearchResponse() },
            hasNext = false
        )

    override suspend fun search(query: String): List<SearchResponse> =
        shows.filter { it.title.contains(query, ignoreCase = true) }
            .map { it.toSearchResponse() }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val show = shows.firstOrNull { url.endsWith("/${it.slug}") } ?: return null

        val episodes = buildList {
            for (season in 1..show.seasons) {
                for (number in 1..show.episodesPerSeason) {
                    add(episodeOf(show, season, number))
                }
            }
        }

        return newTvSeriesLoadResponse(
            show.title,
            url,
            TvType.TvSeries,
            episodes
        ) {
            this.posterUrl = if (show.hasPoster) POSTER_URL else null
            this.plot = "Fixture entry. ${show.seasons} season(s), " +
                "${show.episodesPerSeason} episode(s) each. Not playable."
            this.year = 2026
        }
    }

    private fun episodeOf(show: Show, season: Int, number: Int): Episode =
        newEpisode("$FIXTURE_URL/${show.slug}/$season/$number") {
            this.name = if (number == 1) {
                "Pilot With A Rather Long Episode Title For Wrapping"
            } else {
                "Episode $number"
            }
            this.season = season
            this.episode = number
            this.description = "Fixture episode $number of season $season."
            this.posterUrl = if (show.hasPoster) POSTER_URL else null
        }

    /** Nothing is playable, the fixture exists to exercise the browsing UI. */
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = false

    companion object {
        const val FIXTURE_URL = "https://fixture.invalid"

        /** Ships inside the apk, so the fixture needs no network. */
        val POSTER_URL =
            "android.resource://${com.lagradost.cloudstream3.BuildConfig.APPLICATION_ID}" +
                "/drawable/ic_banner_foreground"
    }
}
