package io.github.aedev.flow.ui.screens.home

/*
 * More Home rules the viewer can set, the same on every device (desktop: home_feed/content_rules.rs): keep
 * entertainment off the page, keep games off it, and keep only the languages the viewer reads. They go by
 * what a title says, so they are guesses: a video the guess cannot place is never ruled out.
 */

/** The languages the viewer can pick, in the order they are offered (the ones Uzbek speakers read). */
internal val HOME_LANGUAGE_CODES = listOf("uz", "ru", "en", "tr", "ar", "fa", "kk", "tg", "ky", "ko")

private val MEDIA_PHRASES =
    listOf(
        "movie clip",
        "movieclips",
        "film clip",
        "movie scene",
        "film scene",
        "fight scene",
        "full movie",
        "full film",
        "official trailer",
        "movie trailer",
        "teaser trailer",
        "blu-ray",
        "bluray",
        "tv series",
        "web series",
        "star wars",
        "stand-up",
        "fan theories",
        "fan theory",
        "behind the scenes",
        "award show",
        "movie review",
        "film analysis",
        "из фильма",
        "отрывок из",
        "tarjima kino",
    )

private val MEDIA_WORDS =
    setOf(
        "clip",
        "clips",
        "movie",
        "movies",
        "film",
        "films",
        "trailer",
        "teaser",
        "kino",
        "serial",
        "treyler",
        "multfilm",
        "fragman",
        "фильм",
        "фильмы",
        "кино",
        "трейлер",
        "сериал",
        "сцена",
        "мультфильм",
        "فيلم",
        "مسلسل",
        "netflix",
        "anime",
        "marvel",
        "disney",
        "sitcom",
        "standup",
        "comedy",
        "drama",
        "horror",
        "sci-fi",
        "oscars",
        "аниме",
        "дорама",
    )

private val GAMING_PHRASES =
    listOf(
        "let's play",
        "lets play",
        "call of duty",
        "league of legends",
        "apex legends",
        "game review",
        "retro gaming",
        "pc gaming",
        "mobile game",
        "indie game",
        "прохождение",
        "геймплей",
        "майнкрафт",
        "o'yin",
    )

private val GAMING_WORDS =
    setOf(
        "gameplay",
        "gaming",
        "gamer",
        "gamers",
        "walkthrough",
        "speedrun",
        "speedruns",
        "esports",
        "minecraft",
        "fortnite",
        "gta",
        "valorant",
        "pokemon",
        "pokémon",
        "nintendo",
        "playstation",
        "ps4",
        "ps5",
        "xbox",
        "roblox",
        "fifa",
        "warzone",
        "cs2",
        "csgo",
        "dota",
        "pubg",
        "игра",
        "игры",
        "стрим",
    )

private val EN_WORDS =
    setOf(
        "the",
        "and",
        "of",
        "to",
        "in",
        "for",
        "with",
        "how",
        "you",
        "your",
        "is",
        "are",
        "this",
        "that",
        "what",
        "why",
        "best",
        "new",
        "vs",
        "top",
        "from",
        "about",
        "my",
        "we",
        "it",
        "on",
        "at",
        "by",
    )

private val UZ_WORDS =
    setOf(
        "va",
        "bilan",
        "uchun",
        "qanday",
        "yangi",
        "eng",
        "haqida",
        "nima",
        "kerak",
        "uzbek",
        "dars",
        "darslik",
        "sara",
        "kuni",
        "bugun",
        "yaxshi",
        "qilish",
        "uzbekcha",
        "tilida",
    )

private val TR_WORDS =
    setOf(
        "ve",
        "bir",
        "için",
        "nasıl",
        "ile",
        "çok",
        "daha",
    )

private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
private val NON_WORD_KEEPING_APOSTROPHE = Regex("[^\\p{L}\\p{N}']+")

private fun titleWords(title: String): List<String> = title.split(NON_WORD)

/** Whether a title looks like entertainment: a movie or TV clip, a show, anime. */
internal fun looksLikeEntertainment(title: String): Boolean {
    val lower = normalizedForMusic(title)
    return MEDIA_PHRASES.any { lower.contains(it) } || titleWords(lower).any { it in MEDIA_WORDS }
}

/** Whether a title looks like a game video. */
internal fun looksLikeGaming(title: String): Boolean {
    val lower = normalizedForMusic(title)
    return GAMING_PHRASES.any { lower.contains(it) } || titleWords(lower).any { it in GAMING_WORDS }
}

/**
 * The language a title is written in, one of [HOME_LANGUAGE_CODES], or null when it cannot be told (no
 * letters, two scripts mixed, or a Latin title with no telling word).
 */
internal fun guessLanguage(text: String): String? {
    val lower = normalizedForMusic(text)
    var cyrillic = 0
    var arabic = 0
    var hangul = 0
    var latin = 0
    for (c in lower) {
        if (!c.isLetter()) continue
        when (c) {
            in '\u0400'..'\u04FF' -> cyrillic++
            in '\u0600'..'\u06FF', in '\u0750'..'\u077F' -> arabic++
            in '\uAC00'..'\uD7AF', in '\u1100'..'\u11FF' -> hangul++
            else -> if (c in 'a'..'z' || c in '\u00C0'..'\u024F') latin++
        }
    }
    val total = cyrillic + arabic + hangul + latin

    fun dominant(count: Int) = total > 0 && count * 10 >= total * 6
    return when {
        dominant(hangul) -> "ko"
        dominant(arabic) -> if (lower.any { it in "\u067e\u0686\u0698\u06af\u06a9\u06cc" }) "fa" else "ar"
        dominant(cyrillic) -> cyrillicLanguage(lower)
        dominant(latin) -> latinLanguage(lower)
        else -> null
    }
}

/** Which Cyrillic language, going by the letters only that language uses; Russian when none is telling. */
private fun cyrillicLanguage(text: String): String =
    when {
        '\u045e' in text -> "uz"
        text.any { it in "\u04e3\u04ef\u04b7" } -> "tg"
        text.any { it in "\u04d9\u04b1\u04bb\u0456" } -> "kk"
        text.any { it in "\u04af\u04e9\u04a3" } -> "ky"
        text.any { it in "\u049b\u0493\u04b3" } -> "uz"
        else -> "ru"
    }

/** Uzbek, Turkish or English, by telling letters and words; null when none stands out or two tie. */
private fun latinLanguage(text: String): String? {
    val words = text.split(NON_WORD_KEEPING_APOSTROPHE).map { it.trim('\'') }.filter { it.isNotEmpty() }
    val uzMarks = words.count { (it.contains("o'") || it.contains("g'")) && it != "o'clock" }
    val trMarks = if (text.any { it in "\u011f\u0131\u015f" }) 2 else 0
    val scores =
        listOf(
            "uz" to words.count { it in UZ_WORDS } + uzMarks,
            "tr" to words.count { it in TR_WORDS } + trMarks,
            "en" to words.count { it in EN_WORDS },
        )
    val best = scores.maxOf { it.second }
    val leaders = scores.filter { it.second == best }
    return if (best > 0 && leaders.size == 1) leaders.first().first else null
}

/** The languages in a saved choice: codes joined by commas ("uz,ru,en"). "any", empty and unknown codes give none. */
internal fun parseHomeLanguages(value: String): List<String> {
    val chosen = value.split(',').map { it.trim().lowercase() }.toSet()
    return HOME_LANGUAGE_CODES.filter { it in chosen }
}

/** The saved choice for a list of languages, in the order they are offered: "any" when there is none. */
internal fun serializeHomeLanguages(codes: Collection<String>): String =
    HOME_LANGUAGE_CODES.filter { it in codes }.joinToString(",").ifEmpty { "any" }

/** Whether a title is in a language outside [languages]; never for an empty list or a title that cannot be placed. */
internal fun isOutsideLanguages(
    title: String,
    languages: List<String>,
): Boolean {
    if (languages.isEmpty()) return false
    val language = guessLanguage(title) ?: return false
    return language !in languages
}
