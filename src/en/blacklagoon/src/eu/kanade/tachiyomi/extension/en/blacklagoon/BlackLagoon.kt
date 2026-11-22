package eu.kanade.tachiyomi.extension.en.blacklagoon

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import keiyoushi.annotation.Source
import keiyoushi.network.Fetch
import keiyoushi.network.get
import keiyoushi.source.KeiSource
import keiyoushi.source.SMangaUpdate
import keiyoushi.utils.asJsoup

@Source
abstract class BlackLagoon : KeiSource() {

    private fun blackLagoon() = SManga.create().apply {
        title = "Black Lagoon"
        url = "/"
        thumbnail_url = "$baseUrl/wp-content/uploads/2025/08/227037l.webp"
        author = "HIROE Rei"
        artist = "HIROE Rei"
        genre = "Action, Adventure, Comedy, Drama, Ecchi, Mature, Seinen"
        status = SManga.ONGOING
    }

    override suspend fun getPopularManga(page: Int) =
        MangasPage(listOf(blackLagoon()), false)

    override suspend fun getLatestUpdates(page: Int) =
        MangasPage(listOf(blackLagoon()), false)

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList) =
        MangasPage(listOf(blackLagoon()), false)

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val doc = client.get(baseUrl, Fetch.DOCUMENT).asJsoup()

        val details = blackLagoon().apply {
            description = doc.selectFirst("meta[property=og:description]")?.attr("content")
        }

        val chapterList = doc.select("a[href*=/comic/black-lagoon-chapter-]")
            .map { it.absUrl("href") }
            .distinct()
            .map { link ->
                SChapter.create().apply {
                    setUrlWithoutDomain(link)
                    val num = link.trimEnd('/').substringAfterLast("chapter-").replace('-', '.')
                    name = "Chapter $num"
                    chapter_number = num.toFloatOrNull() ?: -1f
                }
            }
            .sortedByDescending { it.chapter_number }

        return SMangaUpdate(details, chapterList)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val doc = client.get(baseUrl + chapter.url, Fetch.DOCUMENT).asJsoup()
        return doc.select("img[src*=img.mangarchive.com], img[data-src*=img.mangarchive.com]")
            .mapIndexed { i, img ->
                val src = img.absUrl("data-src").ifEmpty { img.absUrl("src") }
                Page(i, imageUrl = src)
            }
    }
}
