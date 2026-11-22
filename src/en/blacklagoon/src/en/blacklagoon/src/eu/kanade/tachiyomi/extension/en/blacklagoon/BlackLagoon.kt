package eu.kanade.tachiyomi.extension.en.blacklagoon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

class BlackLagoon : HttpSource() {

    override val name = "Read Black Lagoon"
    override val baseUrl = "https://readblacklagoon.com"
    override val lang = "en"
    override val supportsLatest = false

    override fun headersBuilder(): Headers.Builder =
        super.headersBuilder().add("Referer", "$baseUrl/")

    private fun blackLagoon() = SManga.create().apply {
        title = "Black Lagoon"
        url = "/"
        thumbnail_url = "$baseUrl/wp-content/uploads/2025/08/227037l.webp"
        author = "HIROE Rei"
        artist = "HIROE Rei"
        genre = "Action, Adventure, Comedy, Drama, Ecchi, Mature, Seinen"
        status = SManga.ONGOING
        initialized = true
    }

    override fun popularMangaRequest(page: Int) = GET(baseUrl, headers)
    override fun popularMangaParse(response: Response) =
        MangasPage(listOf(blackLagoon()), false)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET(baseUrl, headers)
    override fun searchMangaParse(response: Response) =
        MangasPage(listOf(blackLagoon()), false)

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException()
    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException()

    override fun mangaDetailsRequest(manga: SManga) = GET(baseUrl, headers)
    override fun mangaDetailsParse(response: Response): SManga {
        val doc = Jsoup.parse(response.body.string())
        return blackLagoon().apply {
            description = doc.selectFirst("meta[property=og:description]")?.attr("content")
                ?: "Rokuro Okajima is dragged into the underworld by the crew of the Black Lagoon."
        }
    }

    override fun chapterListRequest(manga: SManga) = GET(baseUrl, headers)
    override fun chapterListParse(response: Response): List<SChapter> {
        val doc = Jsoup.parse(response.body.string(), baseUrl)
        return doc.select("a[href*=/comic/black-lagoon-chapter-]")
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
    }

    override fun pageListParse(response: Response): List<Page> {
        val doc = Jsoup.parse(response.body.string(), baseUrl)
        return doc.select("img[src*=img.mangarchive.com], img[data-src*=img.mangarchive.com]")
            .mapIndexed { i, img ->
                val src = img.absUrl("data-src").ifEmpty { img.absUrl("src") }
                Page(i, imageUrl = src)
            }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headers)
}
