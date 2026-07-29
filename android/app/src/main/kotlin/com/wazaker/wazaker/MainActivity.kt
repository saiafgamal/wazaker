package com.wazaker.wazaker

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import kotlin.random.Random

data class Verse(
    val text: String,
    val surah: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val ayahLabel: String = ayahNumber.toString(),
    val tafsir: String,
) {
    val key: String get() = "${surahNumber}_${ayahNumber}"
    val reference: String get() = "$surah : ${ayahLabel.toArabicDigits()}"
    val preview: String
        get() = tafsir.trim().let {
            if (it.length <= 120) it else it.take(120).trim() + "..."
        }
}

data class ReminderEntry(val id: String, val verseKey: String, val scheduledAt: Long)

object VerseRepository {
    val verses = listOf(
        Verse(
            text = "قَالَ إِن لَّبِثْتُمْ إِلَّا قَلِيلًا لَّوْ أَنَّكُمْ كُنتُمْ تَعْلَمُونَ",
            surah = "المؤمنون",
            surahNumber = 23,
            ayahNumber = 114,
            ayahLabel = "114",
            tafsir = "لو أنكم تعلمون قصر الدنيا وخلود الآخرة لصبرتم على الطاعة وما آثرتم الفاني على الباقي",
        ),
        Verse(
            text = "قُتِلَ ٱلْإِنسَـٰنُ مَآ أَكْفَرَهُ",
            surah = "عبس",
            surahNumber = 80,
            ayahNumber = 17,
            ayahLabel = "17",
            tafsir = "كيف يجحد نعمة ربه وإحسانه!",
        ),
        Verse(
            text = "وَٱصْبِرْ نَفْسَكَ مَعَ ٱلَّذِينَ يَدْعُونَ رَبَّهُم بِٱلْغَدَوٰةِ وَٱلْعَشِىِّ يُرِيدُونَ وَجْهَهُ وَلَا تَعْدُ عَيْنَاكَ عَنْهُمْ تُرِيدُ زِينَةَ ٱلْحَيَوٰةِ ٱلدُّنْيَا وَلَا تُطِعْ مَنْ أَغْفَلْنَا قَلْبَهُ عَن ذِكْرِنَا وَٱتَّبَعَ هَوَاهُ وَكَانَ أَمْرُهُ فُرُطًا",
            surah = "الكهف",
            surahNumber = 18,
            ayahNumber = 28,
            ayahLabel = "28",
            tafsir = "اذكر ربك واستعن بصحبة الذاكرين ولا تتبع الهوى",
        ),
        Verse(
            text = "أَلَمْ يَأْنِ لِلَّذِينَ ءَامَنُوٓا۟ أَن تَخْشَعَ قُلُوبُهُمْ لِذِكْرِ ٱللَّهِ وَمَا نَزَلَ مِنَ ٱلْحَقِّ وَلَا يَكُونُوا۟ كَٱلَّذِينَ أُوتُوا۟ ٱلْكِتَابَ مِن قَبْلُ فَطَالَ عَلَيْهِمُ ٱلْأَمَدُ فَقَسَتْ قُلُوبُهُمْ وَكَثِيرٌ مِّنْهُمْ فَاسِقُونَ",
            surah = "الحديد",
            surahNumber = 57,
            ayahNumber = 16,
            ayahLabel = "16",
            tafsir = "قل بلى يا رب قد آن، وأقلع عن معاصيك. اللهم لا تمتنا بقلوب قاسية واختم لنا بتوبة.",
        ),
        Verse(
            text = "أَرَضِيتُمْ بِالْحَيَاةِ الدُّنْيَا مِنَ الْآخِرَةِ ۚ فَمَا مَتَاعُ الْحَيَاةِ الدُّنْيَا فِي الْآخِرَةِ إِلَّا قَلِيلٌ",
            surah = "التوبة",
            surahNumber = 9,
            ayahNumber = 38,
            ayahLabel = "38",
            tafsir = "اللهم لا تزغ قلوبنا.",
        ),
        Verse(
            text = "أَفَرَأَيْتَ إِن مَّتَّعْنَاهُمْ سِنِينَ (205) ثُمَّ جَاءَهُم مَّا كَانُوا يُوعَدُونَ (206) مَا أَغْنَىٰ عَنْهُم مَّا كَانُوا يُمَتَّعُونَ (207)",
            surah = "الشعراء",
            surahNumber = 26,
            ayahNumber = 205,
            ayahLabel = "205 - 207",
            tafsir = "في الحديث الصحيح: \" يؤتى بالكافر فيغمس في النار غمسة، ثم يقال له: هل رأيت خيرا قط؟ هل رأيت نعيما قط؟ فيقول: لا [والله يا رب] . ويؤتى بأشد الناس بؤسا كان في الدنيا، فيصبغ في الجنة صبغة، ثم يقال له: هل رأيت بؤسا قط؟ فيقول: لا والله يا رب\"",
        ),
        Verse(
            text = "ولا تُخزِني يوم يُبعثون (87) يوم لا ينفع مال ولا بنون (88)",
            surah = "الشعراء",
            surahNumber = 26,
            ayahNumber = 87,
            ayahLabel = "87 - 88",
            tafsir = "غفرانك ربنا وعفوك.",
        ),
        Verse(
            text = "وَقَالَ ٱلشَّيْطَـٰنُ لَمَّا قُضِىَ ٱلْأَمْرُ إِنَّ ٱللَّهَ وَعَدَكُمْ وَعْدَ ٱلْحَقِّ وَوَعَدتُّكُمْ فَأَخْلَفْتُكُمْ‌ۖ وَمَا كَانَ لِىَ عَلَيْكُم مِّن سُلْطَـٰنٍ إِلَّآ أَن دَعَوْتُكُمْ فَٱسْتَجَبْتُمْ لِى‌ۖ فَلَا تَلُومُونِى وَلُومُوٓاْ أَنفُسَكُم‌ۖ مَّآ أَنَاْ بِمُصْرِخِكُمْ وَمَآ أَنتُم بِمُصْرِخِىَّ‌ۖ إِنِّى كَفَرْتُ بِمَآ أَشْرَكْتُمُونِ مِن قَبْلُ‌ۗ إِنَّ ٱلظَّـٰلِمِينَ لَهُمْ عَذَابٌ أَلِيمٌ",
            surah = "إبراهيم",
            surahNumber = 14,
            ayahNumber = 22,
            ayahLabel = "22",
            tafsir = "اللهم نعوذ بك من الشيطان ونخشى عذابك.",
        ),
        Verse(
            text = "يَوْمَ تَجِدُ كُلُّ نَفْسٍ مَّا عَمِلَتْ مِنْ خَيْرٍ مُّحْضَرًا وَمَا عَمِلَتْ مِن سُوٓءٍ تَوَدُّ لَوْ أَنَّ بَيْنَهَا وَبَيْنَهُۥٓ أَمَدَۢا بَعِيدًا‌ۗ وَيُحَذِّرُكُمُ ٱللَّهُ نَفْسَهُۥ‌ۗ وَٱللَّهُ رَءُوفُۢ بِٱلْعِبَادِ",
            surah = "آل عمران",
            surahNumber = 3,
            ayahNumber = 30,
            ayahLabel = "30",
            tafsir = "اللهم عفوك وغفرانك.",
        ),
        Verse(
            text = "يَوْمَ تَأْتِى كُلُّ نَفْسٍ تُجَـٰدِلُ عَن نَّفْسِهَا وَتُوَفَّىٰ كُلُّ نَفْسٍ مَّا عَمِلَتْ وَهُمْ لَا يُظْلَمُونَ",
            surah = "النحل",
            surahNumber = 16,
            ayahNumber = 111,
            ayahLabel = "111",
            tafsir = "لا يُظْلمون بنقص حسناتهم، ولا بزيادة سيئاتهم. اللهم أسرفنا على أنفسنا فاغفر لنا.",
        ),
        Verse(
            text = "يَوْمَ تَبْيَضُّ وُجُوهٌ وَتَسْوَدُّ وُجُوهٌ‌ۚ فَأَمَّا ٱلَّذِينَ ٱسْوَدَّتْ وُجُوهُهُمْ أَكَفَرْتُم بَعْدَ إِيمَـٰنِكُمْ فَذُوقُواْ ٱلْعَذَابَ بِمَا كُنتُمْ تَكْفُرُونَ (106) وَأَمَّا ٱلَّذِينَ ٱبْيَضَّتْ وُجُوهُهُمْ فَفِى رَحْمَةِ ٱللَّهِ هُمْ فِيهَا خَـٰلِدُونَ (107)",
            surah = "آل عمران",
            surahNumber = 3,
            ayahNumber = 106,
            ayahLabel = "106 - 107",
            tafsir = "ربنا اهدنا ولا تزغ قلوبنا بعد إذ هديتنا.",
        ),
        Verse(
            text = "زُيِّنَ لِلنَّاسِ حُبُّ ٱلشَّهَواتِ مِنَ ٱلنِّسَآءِ وَٱلْبَنِينَ وَٱلْقَنَـٰطِيرِ ٱلْمُقَنطَرَةِ مِنَ ٱلذَّهَبِ وَٱلْفِضَّةِ وَٱلْخَيْلِ ٱلْمُسَوَّمَةِ وَٱلْأَنْعَـٰمِ وَٱلْحَرْثِ‌ۗ ذالِكَ مَتَـٰعُ ٱلْحَيَواةِ ٱلدُّنْيَا‌ۖ وَٱللَّهُ عِندَهُۥ حُسْنُ ٱلْمَئاب (14) قُلْ أَؤُنَبِّئُكُم بِخَيْرٍ مِّن ذالِكُمْ‌ۚ لِلَّذِينَ ٱتَّقَوْاْ عِندَ رَبِّهِمْ جَنَّـٰتٌ تَجْرِى مِن تَحْتِهَا ٱلْأَنْهَـٰرُ خَـٰلِدِينَ فِيهَا وَأَزْواجٌ مُّطَهَّرَةٌ وَرِضْوانٌ مِّنَ ٱللَّهِ‌ۗ وَٱللَّهُ بَصِيرُۢ بِٱلْعِبَاد (15)",
            surah = "آل عمران",
            surahNumber = 3,
            ayahNumber = 14,
            ayahLabel = "14 - 15",
            tafsir = "فاختر لنفسك.",
        ),
        Verse(
            text = "يَوْمَ يَأْتِ لَا تَكَلَّمُ نَفْسٌ إِلَّا بِإِذْنِهِۦ‌ۚ فَمِنْهُمْ شَقِىٌّ وَسَعِيد (105) فَأَمَّا ٱلَّذِينَ شَقُواْ فَفِى ٱلنَّارِ لَهُمْ فِيهَا زَفِيرٌ وَشَهِيق (106) خَـٰلِدِينَ فِيهَا مَا دَامَتِ ٱلسَّمَـٰواتُ وَٱلْأَرْضُ إِلَّا مَا شَآءَ رَبُّكَ‌ۚ إِنَّ رَبَّكَ فَعَّالٌ لِّمَا يُرِيدُ (107) وَأَمَّا ٱلَّذِينَ سُعِدُواْ فَفِى ٱلْجَنَّةِ خَـٰلِدِينَ فِيهَا مَا دَامَتِ ٱلسَّمَـٰواتُ وَٱلْأَرْضُ إِلَّا مَا شَآءَ رَبُّكَ‌ۖ عَطَآءً غَيْرَ مَجْذُوذٍ (108)",
            surah = "هود",
            surahNumber = 11,
            ayahNumber = 105,
            ayahLabel = "105 - 108",
            tafsir = "فاختر لنفسك.",
        ),
        Verse(
            text = "يَوْمَ تَشْهَدُ عَلَيْهِمْ أَلْسِنَتُهُمْ وَأَيْدِيهِمْ وَأَرْجُلُهُم بِمَا كَانُواْ يَعْمَلُونَ",
            surah = "النور",
            surahNumber = 24,
            ayahNumber = 24,
            ayahLabel = "24",
            tafsir = "اللهم إنك عفو تحب العفو فاعف عنا.",
        ),
        Verse(
            text = "وَلَا تَكُونُواْ كَٱلَّذِينَ نَسُواْ ٱللَّهَ فَأَنسَـٰهُمْ أَنفُسَهُمْ‌ۚ أُوْلَـٰٓئِكَ هُمُ ٱلْفَـٰسِقُونَ",
            surah = "الحشر",
            surahNumber = 59,
            ayahNumber = 19,
            ayahLabel = "19",
            tafsir = "اللهم إني قصير الأجل، طويل الأمل، مسيء العمل، فاغفر لي.",
        )
    ).sortedWith(compareBy<Verse> { it.surahNumber }.thenBy { it.ayahNumber })

    fun random(): Verse = verses[Random.nextInt(verses.size)]
    fun byKey(key: String?): Verse = verses.firstOrNull { it.key == key } ?: random()
}

class WazakerStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

    var onboardingDone: Boolean
        get() = prefs.getBoolean(MainActivity.KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(MainActivity.KEY_ONBOARDING_DONE, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(MainActivity.KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(MainActivity.KEY_NOTIFICATIONS_ENABLED, value).apply()

    var frequency: String
        get() = prefs.getString(MainActivity.KEY_FREQUENCY, "every_3_days") ?: "every_3_days"
        set(value) = prefs.edit().putString(MainActivity.KEY_FREQUENCY, value).apply()

    var displayMode: String
        get() = prefs.getString(MainActivity.KEY_DISPLAY, "lockscreen") ?: "lockscreen"
        set(value) = prefs.edit().putString(MainActivity.KEY_DISPLAY, value).apply()

    var palette: String
        get() = prefs.getString(MainActivity.KEY_PALETTE, "charcoal") ?: "charcoal"
        set(value) = prefs.edit().putString(MainActivity.KEY_PALETTE, value).apply()

    fun setOf(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty()

    fun toggleSet(name: String, value: String): Boolean {
        val values = setOf(name).toMutableSet()
        val added = values.add(value)
        if (!added) values.remove(value)
        prefs.edit().putStringSet(name, values).apply()
        return added
    }

    fun contains(name: String, value: String): Boolean = setOf(name).contains(value)

    fun note(verse: Verse): String = prefs.getString("note_${verse.key}", "") ?: ""
    fun saveNote(verse: Verse, value: String) = prefs.edit().putString("note_${verse.key}", value.trim()).apply()

    fun recordOpened(verse: Verse) {
        prefs.edit()
            .putString(MainActivity.KEY_LAST_VERSE, verse.key)
            .putLong(MainActivity.KEY_LAST_OPENED_AT, System.currentTimeMillis())
            .apply()
    }

    fun lastOpenedVerse(): Verse? = prefs.getString(MainActivity.KEY_LAST_VERSE, null)?.let { VerseRepository.byKey(it) }
    fun lastOpenedAt(): Long = prefs.getLong(MainActivity.KEY_LAST_OPENED_AT, 0L)

    fun syncReminderBuckets(now: Long = System.currentTimeMillis()) {
        val scheduled = scheduledReminders().sortedBy { it.scheduledAt }
        val due = scheduled.filter { it.scheduledAt <= now }.sortedByDescending { it.scheduledAt }
        val upcoming = scheduled.filter { it.scheduledAt > now }
        if (due.isNotEmpty()) {
            val merged = (due + recentReminders()).distinctBy { it.id }.take(20)
            prefs.edit()
                .putString(MainActivity.KEY_HISTORY, encodeEntries(merged))
                .putString(MainActivity.KEY_SCHEDULED, encodeEntries(upcoming))
                .apply()
        }
    }

    fun saveUpcoming(entries: List<ReminderEntry>) {
        prefs.edit().putString(MainActivity.KEY_SCHEDULED, encodeEntries(entries.sortedBy { it.scheduledAt })).apply()
    }

    fun scheduledReminders(): List<ReminderEntry> = decodeEntries(prefs.getString(MainActivity.KEY_SCHEDULED, "") ?: "")
    fun recentReminders(): List<ReminderEntry> = decodeEntries(prefs.getString(MainActivity.KEY_HISTORY, "") ?: "")

    fun addHistory(verse: Verse, time: Long = System.currentTimeMillis()) {
        val entry = ReminderEntry("${time}_${verse.key}", verse.key, time)
        val merged = (listOf(entry) + recentReminders()).distinctBy { it.id }.take(20)
        prefs.edit().putString(MainActivity.KEY_HISTORY, encodeEntries(merged)).apply()
    }

    private fun encodeEntries(entries: List<ReminderEntry>): String =
        entries.joinToString("\n") { "${it.id}|${it.verseKey}|${it.scheduledAt}" }

    private fun decodeEntries(value: String): List<ReminderEntry> =
        value.lines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3) null else ReminderEntry(parts[0], parts[1], parts[2].toLongOrNull() ?: return@mapNotNull null)
        }
}

class MainActivity : Activity() {
    private lateinit var store: WazakerStore
    private lateinit var appTypeface: Typeface
    private lateinit var appTypefaceBold: Typeface
    private var selectedTab = 0
    private var showingDetail = false
    private var archiveTab = ArchiveTab.READ_LATER
    private var archiveQuery = ""
    private val colors: UiPalette
        get() = when (store.palette) {
            "forest" -> UiPalette(
                bg = 0xFF0D2818.toInt(),
                nav = 0xFF0A1F14.toInt(),
                surface = 0xFF1B5E20.toInt(),
                card = 0xFF143D28.toInt(),
                text = 0xFFFFFFFF.toInt(),
                verse = 0xFFF1F8E9.toInt(),
                secondary = 0xFFB8B0A0.toInt(),
                muted = 0xFF8A9488.toInt(),
                gold = 0xFFC9A84C.toInt(),
            )
            "ivory" -> UiPalette(
                bg = 0xFFFAF7F2.toInt(),
                nav = 0xFFF5F1EB.toInt(),
                surface = 0xFFEDE8E0.toInt(),
                card = 0xFFFFFFFF.toInt(),
                text = 0xFF2C2520.toInt(),
                verse = 0xFF2C2520.toInt(),
                secondary = 0xFF6E6258.toInt(),
                muted = 0xFFA8A098.toInt(),
                gold = 0xFF8B6914.toInt(),
            )
            else -> UiPalette(
                bg = C_BG,
                nav = C_NAV,
                surface = C_SURFACE,
                card = C_CARD,
                text = C_TEXT,
                verse = C_VERSE,
                secondary = C_SECONDARY,
                muted = C_MUTED,
                gold = C_GOLD,
            )
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = WazakerStore(this)
        appTypeface = loadTypeface("fonts/amiri_regular.ttf", Typeface.NORMAL)
        appTypefaceBold = loadTypeface("fonts/amiri_bold.ttf", Typeface.BOLD)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        createNotificationChannel(this)
        requestNotificationPermissionIfNeeded()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    @Deprecated("Deprecated in platform API")
    override fun onBackPressed() {
        when {
            showingDetail -> renderShell(selectedTab)
            archiveQuery.isNotEmpty() -> {
                archiveQuery = ""
                renderShell(1)
            }
            else -> super.onBackPressed()
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_VERSE_KEY)?.let {
            openVerse(VerseRepository.byKey(it))
            return
        }
        if (store.onboardingDone) renderShell(selectedTab) else renderOnboarding()
    }

    private fun renderOnboarding() {
        showingDetail = false
        val root = scroll {
            gravity = Gravity.RIGHT
            addView(spacer(32))
            addView(title("وذكّر", 42))
            addView(body("آيات مختارة تصل إليك بهدوء، ومعها مساحة صغيرة للتدبّر والحفظ والمشاركة.", 18))
            addView(spacer(28))
            addView(section("وتيرة التذكير"))
            addView(rowButtons(listOf(
                "يوميًا" to { store.frequency = "daily"; renderOnboarding() },
                "كل ٣ أيام" to { store.frequency = "every_3_days"; renderOnboarding() },
                "أسبوعيًا" to { store.frequency = "weekly"; renderOnboarding() },
            ), selected = frequencyLabel()))
            addView(spacer(18))
            addView(section("ظهور الإشعار"))
            addView(rowButtons(listOf(
                "شاشة القفل" to { store.displayMode = "lockscreen"; renderOnboarding() },
                "إشعار فقط" to { store.displayMode = "notification"; renderOnboarding() },
            ), selected = displayLabel()))
            addView(spacer(30))
            addView(actionButton("ابدأ", true) {
                store.onboardingDone = true
                store.notificationsEnabled = true
                scheduleReminders(this@MainActivity)
                renderShell(0)
            })
        }
        setContentView(root)
    }

    private fun renderShell(tab: Int) {
        selectedTab = tab
        showingDetail = false
        store.syncReminderBuckets()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colors.bg)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        applySystemBarInsets(root, includeTop = false, includeBottom = true)
        val frame = FrameLayout(this)
        root.addView(frame, LinearLayout.LayoutParams(match(), 0, 1f))
        root.addView(bottomNav())
        setContentView(root)

        frame.addView(
            when (tab) {
                1 -> archiveView()
                2 -> favoritesView()
                3 -> settingsView()
                else -> homeView()
            },
        )
    }

    private fun homeView(): View = scroll {
        addView(title("وذكر", 30))
        addView(body("آيات تذكرك بالآخرة", 17))
        addView(spacer(24))
        val recent = store.recentReminders()
        val lastOpened = store.lastOpenedVerse()
        val featured = recent.firstOrNull()?.let { VerseRepository.byKey(it.verseKey) } ?: lastOpened ?: VerseRepository.random()
        addView(verseCard(featured) { openVerse(featured) })
        addView(spacer(10))
        addView(actionButton("افتح التدبّر", true) { openVerse(featured) })
        if (lastOpened != null && lastOpened.key != featured.key) {
            addView(spacer(24))
            addView(section("آخر ما فتحت"))
            addView(compactVerseCard(lastOpened, relativeDate(store.lastOpenedAt())) { openVerse(lastOpened) })
        }
        addView(spacer(18))
        addView(actionButton("كل التذكيرات السابقة", false) { openReminderHistory() })
    }

    private fun archiveView(): View = scroll {
        addView(title("الأرشيف", 28))
        addView(body("كل الآيات في مكان واحد، وما تحفظه لاحقًا يبقى قريبًا.", 15))
        addView(spacer(14))
        addView(rowButtons(listOf(
            "للقراءة لاحقًا" to { archiveTab = ArchiveTab.READ_LATER; renderShell(1) },
            "كل الآيات" to { archiveTab = ArchiveTab.ALL; renderShell(1) },
        ), selected = if (archiveTab == ArchiveTab.READ_LATER) "للقراءة لاحقًا" else "كل الآيات"))
        addView(spacer(12))
        val search = EditText(this@MainActivity).apply {
            setText(archiveQuery)
            hint = "بحث في الآيات أو السورة"
            setSingleLine(true)
            setTextColor(colors.text)
            setHintTextColor(colors.muted)
            setBackgroundColor(colors.surface)
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            typeface = appTypeface
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        addView(search, LinearLayout.LayoutParams(match(), wrap()))
        addView(spacer(8))
        addView(actionButton("بحث", false) {
            archiveQuery = search.text.toString().trim()
            hideKeyboard(search)
            renderShell(1)
        })
        addView(spacer(18))
        val source = if (archiveTab == ArchiveTab.READ_LATER) {
            store.setOf(KEY_READ_LATER).map { VerseRepository.byKey(it) }
        } else {
            VerseRepository.verses
        }
        val filtered = source.filter {
            archiveQuery.isBlank() || it.text.contains(archiveQuery) || it.surah.contains(archiveQuery) || it.reference.contains(archiveQuery)
        }
        if (filtered.isEmpty()) {
            addView(emptyState(if (archiveTab == ArchiveTab.READ_LATER) "لا توجد آيات محفوظة للقراءة لاحقًا." else "لا توجد نتائج."))
        } else {
            filtered.forEach { verse ->
                addView(listVerseCard(verse) { openVerse(verse) })
                if (archiveTab == ArchiveTab.READ_LATER) {
                    addView(actionButton("إزالة من لاحقًا", false, compact = true, onClick = {
                        store.toggleSet(KEY_READ_LATER, verse.key)
                        renderShell(1)
                    }))
                }
                addView(spacer(10))
            }
        }
    }

    private fun favoritesView(): View = scroll {
        addView(title("المفضلة", 28))
        addView(body("الآيات التي لامستك محفوظة هنا.", 15))
        addView(spacer(16))
        val verses = store.setOf(KEY_FAVORITES).map { VerseRepository.byKey(it) }
        if (verses.isEmpty()) {
            addView(emptyState("لم تحفظ أي آية بعد."))
            addView(spacer(12))
            addView(actionButton("افتح آية مقترحة", true) { openVerse(VerseRepository.random()) })
        } else {
            verses.forEach { verse ->
                addView(listVerseCard(verse) { openVerse(verse) })
                addView(actionButton("إزالة من المفضلة", false, compact = true, onClick = {
                    store.toggleSet(KEY_FAVORITES, verse.key)
                    renderShell(2)
                }))
                addView(spacer(12))
            }
        }
    }

    private fun settingsView(): View = scroll {
        addView(title("الإعدادات", 28))
        addView(spacer(12))
        addView(section("المظهر"))
        addView(card {
            addView(body("النمط الحالي: ${paletteLabel()}", 16))
            addView(spacer(8))
            addView(rowButtons(listOf(
                "فحمي" to { store.palette = "charcoal"; renderShell(3) },
                "أخضر" to { store.palette = "forest"; renderShell(3) },
                "فاتح" to { store.palette = "ivory"; renderShell(3) },
            ), selected = paletteLabel()))
        })
        addView(spacer(18))
        addView(section("التذكيرات"))
        addView(card {
            addView(body("الحالة: ${if (store.notificationsEnabled) "مفعلة" else "متوقفة"}", 16))
            addView(body("الوتيرة: ${frequencyLabel()}", 16))
            addView(body("الظهور: ${displayLabel()}", 16))
            addView(spacer(10))
            addView(actionButton(if (store.notificationsEnabled) "إيقاف التذكيرات" else "تفعيل التذكيرات", false) {
                store.notificationsEnabled = !store.notificationsEnabled
                if (store.notificationsEnabled) scheduleReminders(this@MainActivity) else cancelReminders(this@MainActivity)
                renderShell(3)
            })
            addView(rowButtons(listOf(
                "يوميًا" to { store.frequency = "daily"; scheduleReminders(this@MainActivity); renderShell(3) },
                "كل ٣ أيام" to { store.frequency = "every_3_days"; scheduleReminders(this@MainActivity); renderShell(3) },
                "أسبوعيًا" to { store.frequency = "weekly"; scheduleReminders(this@MainActivity); renderShell(3) },
            ), selected = frequencyLabel()))
            addView(rowButtons(listOf(
                "شاشة القفل" to { store.displayMode = "lockscreen"; renderShell(3) },
                "إشعار فقط" to { store.displayMode = "notification"; renderShell(3) },
            ), selected = displayLabel()))
            addView(actionButton("إرسال إشعار تجريبي", true) {
                val verse = VerseRepository.random()
                store.addHistory(verse)
                ReminderReceiver.showReminder(this@MainActivity, verse)
                renderShell(3)
            })
            addView(actionButton("فتح إعدادات إشعارات النظام", false) {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                })
            })
        })
        addView(spacer(18))
        addView(section("الخط"))
        addView(card {
            addView(body("يستخدم التطبيق خط Thmanyah Serif Text المضمّن داخل نسخة Android الأصلية.", 15))
        })
    }

    private fun openVerse(verse: Verse) {
        showingDetail = true
        store.recordOpened(verse)
        val noteInput = EditText(this).apply {
            setText(store.note(verse))
            hint = "اكتب ما الذي لامس قلبك في هذه الآية..."
            minLines = 4
            gravity = Gravity.RIGHT or Gravity.TOP
            textDirection = View.TEXT_DIRECTION_RTL
            textAlignment = View.TEXT_ALIGNMENT_GRAVITY
            setTextColor(colors.text)
            setHintTextColor(colors.muted)
            setBackgroundColor(colors.card)
            typeface = appTypeface
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        setContentView(scroll {
            addView(topBar("تدبّر") { renderShell(selectedTab) })
            addView(card {
                addView(body(verse.text, 22, colors.verse))
                addView(spacer(10))
                addView(body(verse.reference, 15, colors.gold))
            })
            addView(spacer(18))
            addView(section("تأمّل"))
            addView(body(verse.tafsir, 17))
            addView(spacer(24))
            addView(section("تأملك الشخصي"))
            addView(noteInput, LinearLayout.LayoutParams(match(), wrap()))
            addView(spacer(12))
            addView(actionButton("حفظ التأمل", true) {
                store.saveNote(verse, noteInput.text.toString())
                hideKeyboard(noteInput)
                toast("تم حفظ التأمل")
            })
            addView(spacer(18))
            addView(rowButtons(listOf(
                (if (store.contains(KEY_FAVORITES, verse.key)) "إزالة الحفظ" else "حفظ") to {
                    store.toggleSet(KEY_FAVORITES, verse.key)
                    openVerse(verse)
                },
                (if (store.contains(KEY_READ_LATER, verse.key)) "إزالة لاحقًا" else "قراءة لاحقًا") to {
                    store.toggleSet(KEY_READ_LATER, verse.key)
                    openVerse(verse)
                },
                "مشاركة صورة" to { shareVerseImage(verse) },
            ), selected = ""))
        })
    }

    private fun openReminderHistory() {
        showingDetail = true
        val upcoming = store.scheduledReminders().sortedBy { it.scheduledAt }
        val recent = store.recentReminders().sortedByDescending { it.scheduledAt }
        setContentView(scroll {
            addView(topBar("سجل التذكيرات") { renderShell(0) })
            addView(section("القادمة"))
            if (upcoming.isEmpty()) {
                addView(emptyState("لا توجد تذكيرات قادمة."))
            } else {
                upcoming.take(10).forEach {
                    val verse = VerseRepository.byKey(it.verseKey)
                    addView(compactVerseCard(verse, formatDate(it.scheduledAt)) { openVerse(verse) })
                    addView(spacer(10))
                }
            }
            addView(spacer(18))
            addView(section("الأخيرة"))
            if (recent.isEmpty()) {
                addView(emptyState("لا توجد تذكيرات سابقة بعد."))
            } else {
                recent.forEach {
                    val verse = VerseRepository.byKey(it.verseKey)
                    addView(compactVerseCard(verse, formatDate(it.scheduledAt)) { openVerse(verse) })
                    addView(spacer(10))
                }
            }
        })
    }

    private fun compactVerseCard(verse: Verse, subtitle: String, onClick: () -> Unit): View = card {
        isClickable = true
        setOnClickListener { onClick() }
        addView(body(verse.text, 16, colors.verse).singleLineClamp(2))
        addView(spacer(6))
        addView(body("${verse.reference} - $subtitle", 13, colors.gold))
    }

    private fun listVerseCard(verse: Verse, onClick: () -> Unit): View = card {
        isClickable = true
        setOnClickListener { onClick() }
        addView(body(verse.text, 18, colors.verse).singleLineClamp(3))
        addView(spacer(8))
        addView(body(verse.reference, 14, colors.gold))
    }

    private fun verseCard(verse: Verse, onClick: () -> Unit): View = card {
        isClickable = true
        setOnClickListener { onClick() }
        addView(body(verse.text, 19, colors.verse))
        addView(spacer(10))
        addView(body(verse.reference, 15, colors.gold))
        if (verse.preview.isNotBlank()) {
            addView(spacer(8))
            addView(body(verse.preview, 14, colors.muted))
        }
    }

    private fun bottomNav(): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(8), dp(8), dp(8))
        setBackgroundColor(colors.nav)
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        listOf(
            3 to "الإعدادات",
            2 to "المفضلة",
            1 to "الأرشيف",
            0 to "الرئيسية",
        ).forEach { (index, label) ->
            addView(navButton(label, index == selectedTab) { renderShell(index) }, LinearLayout.LayoutParams(0, dp(52), 1f))
        }
    }

    private fun topBar(text: String, onBack: () -> Unit): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        addView(actionButton("رجوع", false, onBack), LinearLayout.LayoutParams(dp(96), dp(48)))
        addView(title(text, 20), LinearLayout.LayoutParams(0, wrap(), 1f))
    }

    private fun rowButtons(items: List<Pair<String, () -> Unit>>, selected: String): View = HorizontalScrollView(this).apply {
        layoutParams = LinearLayout.LayoutParams(match(), wrap())
        isHorizontalScrollBarEnabled = false
        isFillViewport = true
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(match(), wrap())
            items.forEach { (label, onClick) ->
                addView(actionButton(label, label == selected, onClick), LinearLayout.LayoutParams(wrap(), dp(46)).apply {
                    setMargins(dp(4), dp(6), dp(4), dp(6))
                })
            }
        })
    }

    private fun scroll(content: LinearLayout.() -> Unit): ScrollView = ScrollView(this).apply {
        setBackgroundColor(colors.bg)
        applySystemBarInsets(this, includeTop = true, includeBottom = true)
        addView(verticalRoot().apply {
            setPadding(dp(24), dp(18), dp(24), dp(34))
            content()
        })
    }

    private fun verticalRoot(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(colors.bg)
        layoutDirection = View.LAYOUT_DIRECTION_RTL
    }

    private fun card(content: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.RIGHT
        layoutDirection = View.LAYOUT_DIRECTION_RTL
        setPadding(dp(18), dp(14), dp(18), dp(14))
        setBackgroundColor(colors.card)
        layoutParams = LinearLayout.LayoutParams(match(), wrap()).apply {
            setMargins(0, dp(4), 0, dp(4))
        }
        content()
    }

    private fun title(text: String, size: Int): TextView = textView(text, size, colors.text, Typeface.BOLD)
    private fun section(text: String): TextView = textView(text, 18, colors.gold, Typeface.BOLD)
    private fun body(text: String, size: Int, color: Int = colors.secondary): TextView = textView(text, size, color, Typeface.NORMAL)

    private fun textView(text: String, size: Int, color: Int, style: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(color)
        typeface = if (style == Typeface.BOLD) appTypefaceBold else appTypeface
        gravity = Gravity.RIGHT
        textDirection = View.TEXT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_GRAVITY
        layoutParams = LinearLayout.LayoutParams(match(), wrap())
        includeFontPadding = true
        setLineSpacing(0f, 1.18f)
    }

    private fun actionButton(text: String, primary: Boolean, onClick: () -> Unit): Button =
        actionButton(text, primary, compact = false, onClick = onClick)

    private fun actionButton(text: String, primary: Boolean, compact: Boolean, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        isAllCaps = false
        typeface = appTypefaceBold
        textDirection = View.TEXT_DIRECTION_RTL
        textAlignment = View.TEXT_ALIGNMENT_CENTER
        gravity = Gravity.CENTER
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        textSize = if (compact) 14f else 16f
        setPadding(dp(10), if (compact) dp(4) else dp(8), dp(10), if (compact) dp(4) else dp(8))
        setTextColor(if (primary) colors.bg else colors.text)
        setBackgroundColor(if (primary) colors.gold else colors.surface)
        setOnClickListener { onClick() }
    }

    private fun navButton(text: String, primary: Boolean, onClick: () -> Unit): Button = actionButton(text, primary, onClick).apply {
        textSize = 15f
        setPadding(dp(4), 0, dp(4), 0)
    }

    private fun TextView.singleLineClamp(lines: Int): TextView = apply {
        maxLines = lines
        ellipsize = TextUtils.TruncateAt.END
    }

    private fun emptyState(text: String): View = card { addView(body(text, 17)) }
    private fun spacer(height: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(match(), dp(height)) }

    private fun shareVerseImage(verse: Verse) {
        val file = ShareCardRenderer.render(this, verse, appTypeface)
        val uri = Uri.parse("content://${packageName}.share/share_card.png")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(contentResolver, "wazaker-share-card.png", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "مشاركة"))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
    }

    private fun frequencyLabel(): String = when (store.frequency) {
        "daily" -> "يوميًا"
        "weekly" -> "أسبوعيًا"
        else -> "كل ٣ أيام"
    }

    private fun displayLabel(): String = if (store.displayMode == "lockscreen") "شاشة القفل" else "إشعار فقط"

    private fun paletteLabel(): String = when (store.palette) {
        "forest" -> "أخضر"
        "ivory" -> "فاتح"
        else -> "فحمي"
    }

    private fun hideKeyboard(view: View) {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun loadTypeface(assetPath: String, fallbackStyle: Int): Typeface {
        return try {
            Typeface.createFromAsset(assets, assetPath)
        } catch (_: Exception) {
            Typeface.create("serif", fallbackStyle)
        }
    }

    private fun applySystemBarInsets(view: View, includeTop: Boolean, includeBottom: Boolean) {
        val baseLeft = view.paddingLeft
        val baseTop = view.paddingTop
        val baseRight = view.paddingRight
        val baseBottom = view.paddingBottom
        view.setOnApplyWindowInsetsListener { target, insets ->
            val topInset: Int
            val bottomInset: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                topInset = bars.top
                bottomInset = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                topInset = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottomInset = insets.systemWindowInsetBottom
            }
            target.setPadding(
                baseLeft,
                baseTop + if (includeTop) topInset else 0,
                baseRight,
                baseBottom + if (includeBottom) bottomInset else 0,
            )
            insets
        }
        view.requestApplyInsets()
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PREFS_NAME = "wazaker_native"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_FREQUENCY = "reminder_frequency"
        const val KEY_DISPLAY = "notification_display"
        const val KEY_PALETTE = "palette"
        const val KEY_FAVORITES = "favorites"
        const val KEY_READ_LATER = "read_later"
        const val KEY_LAST_VERSE = "last_verse"
        const val KEY_LAST_OPENED_AT = "last_opened_at"
        const val KEY_SCHEDULED = "scheduled_reminders"
        const val KEY_HISTORY = "reminder_history"
        const val EXTRA_VERSE_KEY = "verse_key"
        const val EXTRA_ACTION = "action"
        const val ACTION_SAVE = "save_verse"
        const val ACTION_READ_LATER = "read_later"
        const val CHANNEL_ID = "wazaker_reminders"
        const val REMINDER_REQUEST_CODE = 2407

        const val C_BG = 0xFF141414.toInt()
        const val C_NAV = 0xFF0F0F0F.toInt()
        const val C_SURFACE = 0xFF282828.toInt()
        const val C_CARD = 0xFF1E1E1E.toInt()
        const val C_TEXT = 0xFFFFFFFF.toInt()
        const val C_VERSE = 0xFFF0EDE8.toInt()
        const val C_SECONDARY = 0xFFA0A0A0.toInt()
        const val C_MUTED = 0xFF707070.toInt()
        const val C_GOLD = 0xFFE8C06A.toInt()

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "تذكيرات وذكّر", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيهات دورية لتدبر آية جديدة"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }

        fun scheduleReminders(context: Context) {
            val store = WazakerStore(context)
            if (!store.notificationsEnabled) return
            createNotificationChannel(context)
            cancelReminders(context)
            val days = when (store.frequency) {
                "daily" -> 1
                "weekly" -> 7
                else -> 3
            }
            val now = System.currentTimeMillis()
            val entries = (1..30).map { index ->
                val at = nextReminderTime(days * index)
                val verse = VerseRepository.verses[(index - 1) % VerseRepository.verses.size]
                ReminderEntry("${at}_${verse.key}", verse.key, at)
            }
            store.saveUpcoming(entries)
            scheduleAlarm(context, entries.firstOrNull()?.scheduledAt ?: now + 60_000)
        }

        fun scheduleAlarm(context: Context, time: Long) {
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REMINDER_REQUEST_CODE,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        }

        fun cancelReminders(context: Context) {
            context.getSystemService(AlarmManager::class.java).cancel(
                PendingIntent.getBroadcast(
                    context,
                    REMINDER_REQUEST_CODE,
                    Intent(context, ReminderReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }

        private fun nextReminderTime(dayOffset: Int): Long {
            return Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    enum class ArchiveTab { READ_LATER, ALL }

    private data class UiPalette(
        val bg: Int,
        val nav: Int,
        val surface: Int,
        val card: Int,
        val text: Int,
        val verse: Int,
        val secondary: Int,
        val muted: Int,
        val gold: Int,
    )
}

object ShareCardRenderer {
    private const val CARD_SIZE = 1080
    private const val TEXT_RIGHT = 930f
    private const val TEXT_WIDTH = 820f
    private const val CONTENT_TOP = 175f
    private const val CONTENT_BOTTOM = 970f
    private const val SECTION_GAP = 36f
    private const val REFERENCE_TEXT_SIZE = 32f
    private const val MAX_VERSE_TEXT_SIZE = 48
    private const val MIN_VERSE_TEXT_SIZE = 28
    private const val MAX_REFLECTION_TEXT_SIZE = 34
    private const val MIN_REFLECTION_TEXT_SIZE = 24

    private data class TextBlock(
        val lines: List<String>,
        val textSize: Float,
        val ascent: Float,
        val descent: Float,
        val lineAdvance: Float,
    ) {
        val height: Float
            get() = -ascent + descent + (lines.size - 1).coerceAtLeast(0) * lineAdvance
    }

    private data class CardTextLayout(
        val verse: TextBlock,
        val reference: TextBlock,
        val reflection: TextBlock,
    ) {
        val height: Float
            get() = verse.height + SECTION_GAP + reference.height + SECTION_GAP + reflection.height
    }

    fun render(context: Context, verse: Verse, typeface: Typeface): File {
        val file = File(context.cacheDir, "share_card.png")
        val bitmap = Bitmap.createBitmap(CARD_SIZE, CARD_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.rgb(20, 20, 20))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.RIGHT
            this.typeface = typeface
        }
        paint.color = Color.rgb(232, 192, 106)
        paint.textSize = 54f
        canvas.drawText("وذكّر", 960f, 120f, paint)

        val layout = createLayout(verse, paint)
        var top = CONTENT_TOP
        top = drawBlock(canvas, layout.verse, top, paint, Color.rgb(240, 237, 232))
        top += SECTION_GAP
        top = drawBlock(canvas, layout.reference, top, paint, Color.rgb(232, 192, 106))
        top += SECTION_GAP
        drawBlock(canvas, layout.reflection, top, paint, Color.rgb(180, 180, 180))

        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    private fun createLayout(verse: Verse, paint: Paint): CardTextLayout {
        val availableHeight = CONTENT_BOTTOM - CONTENT_TOP
        for (verseSize in MAX_VERSE_TEXT_SIZE downTo MIN_VERSE_TEXT_SIZE step 2) {
            val reflectionSize = (verseSize * 0.72f)
                .coerceIn(MIN_REFLECTION_TEXT_SIZE.toFloat(), MAX_REFLECTION_TEXT_SIZE.toFloat())
            val candidate = CardTextLayout(
                verse = createBlock(verse.text, verseSize.toFloat(), paint),
                reference = createBlock(verse.reference, REFERENCE_TEXT_SIZE, paint),
                reflection = createBlock(verse.tafsir, reflectionSize, paint),
            )
            if (candidate.height <= availableHeight) return candidate
        }

        val reference = createBlock(verse.reference, REFERENCE_TEXT_SIZE, paint)
        val reflection = createBlock(
            verse.tafsir,
            MIN_REFLECTION_TEXT_SIZE.toFloat(),
            paint,
            maxLines = 2,
        )
        val roomForVerse = availableHeight - reference.height - reflection.height - SECTION_GAP * 2
        val fullVerse = createBlock(verse.text, MIN_VERSE_TEXT_SIZE.toFloat(), paint)
        val maximumVerseLines = if (roomForVerse <= 0f) {
            1
        } else {
            (1 + ((roomForVerse - (-fullVerse.ascent + fullVerse.descent)) / fullVerse.lineAdvance).toInt())
                .coerceAtLeast(1)
        }
        return CardTextLayout(
            verse = createBlock(
                verse.text,
                MIN_VERSE_TEXT_SIZE.toFloat(),
                paint,
                maxLines = maximumVerseLines,
            ),
            reference = reference,
            reflection = reflection,
        )
    }

    private fun createBlock(text: String, textSize: Float, paint: Paint, maxLines: Int = Int.MAX_VALUE): TextBlock {
        paint.textSize = textSize
        val allLines = wrap(text, paint)
        val lines = if (allLines.size <= maxLines) {
            allLines
        } else {
            allLines.take(maxLines).toMutableList().apply {
                this[lastIndex] = ellipsize(this[lastIndex], paint)
            }
        }
        val metrics = paint.fontMetrics
        return TextBlock(
            lines = lines.ifEmpty { listOf("") },
            textSize = textSize,
            ascent = metrics.ascent,
            descent = metrics.descent,
            lineAdvance = paint.fontSpacing * 1.08f,
        )
    }

    private fun wrap(text: String, paint: Paint): List<String> {
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= TEXT_WIDTH) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines.add(current)
                current = word
            }
        }
        if (current.isNotEmpty()) lines.add(current)
        return lines
    }

    private fun ellipsize(line: String, paint: Paint): String {
        val ellipsis = "…"
        if (paint.measureText(line + ellipsis) <= TEXT_WIDTH) return line + ellipsis
        val words = line.split(" ").toMutableList()
        while (words.isNotEmpty() && paint.measureText(words.joinToString(" ") + ellipsis) > TEXT_WIDTH) {
            words.removeLast()
        }
        return words.joinToString(" ") + ellipsis
    }

    private fun drawBlock(canvas: Canvas, block: TextBlock, top: Float, paint: Paint, color: Int): Float {
        paint.color = color
        paint.textSize = block.textSize
        val firstBaseline = top - block.ascent
        block.lines.forEachIndexed { index, line ->
            canvas.drawText(line, TEXT_RIGHT, firstBaseline + index * block.lineAdvance, paint)
        }
        return top + block.height
    }
}

fun Int.toArabicDigits(): String {
    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return toString().map { digits[it.digitToInt()] }.joinToString("")
}

fun formatDate(value: Long): String {
    if (value <= 0L) return "غير معروف"
    val cal = Calendar.getInstance().apply { timeInMillis = value }
    val day = cal.get(Calendar.DAY_OF_MONTH).toArabicDigits()
    val month = (cal.get(Calendar.MONTH) + 1).toArabicDigits()
    val hour = cal.get(Calendar.HOUR_OF_DAY).toArabicDigits()
    val minute = cal.get(Calendar.MINUTE).toString().padStart(2, '0').toArabicDigits()
    return "$day/$month $hour:$minute"
}

fun String.toArabicDigits(): String {
    val digits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return map { char ->
        if (char.isDigit()) digits[char.digitToInt()] else char
    }.joinToString("")
}

fun relativeDate(value: Long): String {
    if (value <= 0L) return "آخر آية رجعت لها"
    val diff = System.currentTimeMillis() - value
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "الآن"
        minutes < 60 -> "منذ ${minutes.toInt().toArabicDigits()} دقيقة"
        hours < 24 -> "منذ ${hours.toInt().toArabicDigits()} ساعة"
        else -> "منذ ${days.toInt().toArabicDigits()} يوم"
    }
}

fun match(): Int = ViewGroup.LayoutParams.MATCH_PARENT
fun wrap(): Int = ViewGroup.LayoutParams.WRAP_CONTENT
