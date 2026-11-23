package com.example.data.model

import androidx.annotation.StringRes
import com.example.R
import java.util.TimeZone

enum class ReflectionKind(@StringRes val labelRes: Int) {
    QURAN(R.string.reflection_kind_quran),
    HADITH(R.string.reflection_kind_hadith),
    WISDOM(R.string.reflection_kind_wisdom)
}

data class DailyReflection(
    val id: Int,
    val kind: ReflectionKind,
    val arabicText: String,
    val sourceReference: String,
    val spiritualReflection: String,
    val englishText: String = "",
    val englishSource: String = "",
    val englishReflection: String = ""
)

object DailyReflectionProvider {

    const val REFRESH_INTERVAL_MILLIS: Long = 86_400_000L

    val entries: List<DailyReflection> = listOf(
        DailyReflection(
            id = 1,
            kind = ReflectionKind.QURAN,
            arabicText = "«أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ»",
            sourceReference = "سورة الرعد • آية ٢٨",
            spiritualReflection = "مهما اشتدت مشاغل اليوم، دقيقة صادقة من ذكر الله تعيد للقلب سكينته واتّزانه.",
            englishText = "\"Unquestionably, by the remembrance of Allah do hearts find tranquility.\"",
            englishSource = "Surah Ar-Ra'd • 13:28",
            englishReflection = "No matter how busy the day grows, a sincere minute of remembering Allah restores calm and balance to the heart."
        ),
        DailyReflection(
            id = 2,
            kind = ReflectionKind.QURAN,
            arabicText = "«فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ»",
            sourceReference = "سورة البقرة • آية ١٥٢",
            spiritualReflection = "أعظم شرف للعبد أن يذكره ربه في الملأ الأعلى كلما لهج لسانه بالذكر والشكر.",
            englishText = "\"So remember Me; I will remember you. And be grateful to Me and do not deny Me.\"",
            englishSource = "Surah Al-Baqarah • 2:152",
            englishReflection = "The highest honor for a servant is to be remembered by their Lord whenever their tongue moves in praise and gratitude."
        ),
        DailyReflection(
            id = 3,
            kind = ReflectionKind.HADITH,
            arabicText = "«مَثَلُ الَّذِي يَذْكُرُ رَبَّهُ وَالَّذِي لَا يَذْكُرُ رَبَّهُ، مَثَلُ الحَيِّ وَالمَيِّتِ»",
            sourceReference = "صحيح البخاري",
            spiritualReflection = "حياة القلب الحقيقية تُقاس بصلته بالله؛ اجعل يومك عامراً بالتسبيح والتحميد.",
            englishText = "\"The likeness of one who remembers their Lord and one who does not is that of the living and the dead.\"",
            englishSource = "Sahih al-Bukhari",
            englishReflection = "The true vitality of the heart is measured by its connection with Allah; fill your day with Tasbeeh and praise."
        ),
        DailyReflection(
            id = 4,
            kind = ReflectionKind.QURAN,
            arabicText = "«وَهُوَ مَعَكُمْ أَيْنَ مَا كُنتُمْ ۚ وَاللَّهُ بِمَا تَعْمَلُونَ بَصِيرٌ»",
            sourceReference = "سورة الحديد • آية ٤",
            spiritualReflection = "استشعار معيّة الله في خلوتك وخطواتك يملأ نفسك أماناً وصدقاً في النية.",
            englishText = "\"And He is with you wherever you are. And Allah, of what you do, is Seeing.\"",
            englishSource = "Surah Al-Hadid • 57:4",
            englishReflection = "Feeling Allah's closeness in every step fills the soul with security and sincerity of intention."
        ),
        DailyReflection(
            id = 5,
            kind = ReflectionKind.HADITH,
            arabicText = "«كَلِمَتَانِ خَفِيفَتَانِ عَلَى اللِّسَانِ، ثَقِيلَتَانِ فِي المِيزَانِ، حَبِيبَتَانِ إِلَى الرَّحْمَٰنِ: سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ العَظِيمِ»",
            sourceReference = "متفق عليه",
            spiritualReflection = "اغتنم دقائق الانتظار والطريق بترديد هاتين الكلمتين ليثقل بهما ميزانك.",
            englishText = "\"Two phrases are light on the tongue, heavy on the Scale, and beloved to the Most Merciful: SubhanAllahi wa bihamdihi, SubhanAllahil-Azim.\"",
            englishSource = "Al-Bukhari & Muslim",
            englishReflection = "Turn quiet waiting moments into lasting reward by repeating these two effortless phrases."
        ),
        DailyReflection(
            id = 6,
            kind = ReflectionKind.QURAN,
            arabicText = "«فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا»",
            sourceReference = "سورة الشرح • آية ٥ - ٦",
            spiritualReflection = "الفرج يولد من رحم الضيق؛ أحسن الظن بالله وثق أن تيسيره أقرب مما تتخيل.",
            englishText = "\"For indeed, with hardship will be ease. Indeed, with hardship will be ease.\"",
            englishSource = "Surah Ash-Sharh • 94:5-6",
            englishReflection = "Relief is woven within difficulty itself; think well of Allah and trust His ease is closer than you imagine."
        ),
        DailyReflection(
            id = 7,
            kind = ReflectionKind.HADITH,
            arabicText = "«أَحَبُّ الْأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ»",
            sourceReference = "صحيح البخاري ومسلم",
            spiritualReflection = "القليل المستمر من الأذكار والورد اليومي يبني في روحك أثراً أعمق من الكثرة المنقطعة.",
            englishText = "\"The most beloved deeds to Allah are those done consistently, even if small.\"",
            englishSource = "Al-Bukhari & Muslim",
            englishReflection = "Consistent daily Adhkar — even brief — nourish the soul far more deeply than occasional bursts."
        ),
        DailyReflection(
            id = 8,
            kind = ReflectionKind.WISDOM,
            arabicText = "«مَنْ أَدَامَ الِاسْتِغْفَارَ فُتِحَتْ لَهُ مَغَالِيقُ الْأُمُورِ وَشُرِحَ صَدْرُهُ»",
            sourceReference = "إشراقة إيمانية",
            spiritualReflection = "الاستغفار يمحو عثرات الأمس ويستجلب بركة الرزق وانشراح الصدر ليومك.",
            englishText = "\"Whoever persists in seeking forgiveness finds closed doors opened and their heart expanded with peace.\"",
            englishSource = "Spiritual Wisdom",
            englishReflection = "Istighfar gently washes away yesterday's stumbles and welcomes blessing and relief into your day."
        ),
        DailyReflection(
            id = 9,
            kind = ReflectionKind.QURAN,
            arabicText = "«وَقَالَ رَبُّكُمُ ادْعُونِي أَسْتَجِبْ لَكُمْ»",
            sourceReference = "سورة غافر • آية ٦٠",
            spiritualReflection = "باب السماء لا يُغلق؛ ارفع حاجاتك بثقة ويقين أن الله يسمع همس قلبك.",
            englishText = "\"And your Lord says, 'Call upon Me; I will respond to you.'\"",
            englishSource = "Surah Ghafir • 40:60",
            englishReflection = "The door of mercy never closes; raise your needs with certainty that Allah hears the whisper of your heart."
        ),
        DailyReflection(
            id = 10,
            kind = ReflectionKind.HADITH,
            arabicText = "«لَا يَزَالُ لِسَانُكَ رَطْباً مِنْ ذِكْرِ اللَّهِ»",
            sourceReference = "رواه الترمذي وابن ماجه",
            spiritualReflection = "الذكر الدائم يحوّل ساعاتك العادية إلى عبادة هادئة تُنير دربك وتقوّي عزيمتك.",
            englishText = "\"Keep your tongue ever moist with the remembrance of Allah.\"",
            englishSource = "At-Tirmidhi & Ibn Majah",
            englishReflection = "Steady remembrance transforms ordinary hours into quiet worship that brightens your path."
        ),
        DailyReflection(
            id = 11,
            kind = ReflectionKind.QURAN,
            arabicText = "«وَمَن يَتَوَكَّلْ عَلَى اللَّهِ فَهُوَ حَسْبُهُ»",
            sourceReference = "سورة الطلاق • آية ٣",
            spiritualReflection = "اعمل بأسبابك ثم فوض النتائج إلى المدبّر الحكيم؛ فالمتوكل لا يخيب سعيه.",
            englishText = "\"And whoever relies upon Allah — then He is sufficient for him.\"",
            englishSource = "Surah At-Talaq • 65:3",
            englishReflection = "Take thoughtful action and entrust outcomes to the Wise Planner; trust in Allah never disappoints."
        ),
        DailyReflection(
            id = 12,
            kind = ReflectionKind.WISDOM,
            arabicText = "«إِذَا أَصْبَحْتَ فَلَا تَنْتَظِرِ الْمَسَاءَ، وَأَخْلِصْ فِي ذِكْرِ سَاعَتِكَ الحَاضِرَةِ»",
            sourceReference = "تأملات يومية",
            spiritualReflection = "عِش يومك حاضراً بقلب شاكر، ولا تثقل كاهلك بقلق الغد أو ندم الماضي.",
            englishText = "\"When you reach morning, do not wait for evening; be sincere in remembering Allah in this present hour.\"",
            englishSource = "Daily Reflection",
            englishReflection = "Live present today with a grateful heart rather than carrying tomorrow's worry."
        ),
        DailyReflection(
            id = 13,
            kind = ReflectionKind.QURAN,
            arabicText = "«لَئِن شَكَرْتُمْ لَأَزِيدَنَّكُمْ»",
            sourceReference = "سورة إبراهيم • آية ٧",
            spiritualReflection = "تأمّل ثلاث نعم تنعم بها الآن واحمد الله عليها بلسانك وقلبك لتتضاعف البركة.",
            englishText = "\"If you are grateful, I will surely increase you in favor.\"",
            englishSource = "Surah Ibrahim • 14:7",
            englishReflection = "Pause to name three blessings you enjoy right now and thank Allah from your heart."
        ),
        DailyReflection(
            id = 14,
            kind = ReflectionKind.HADITH,
            arabicText = "«مَا جَلَسَ قَوْمٌ يَذْكُرُونَ اللَّهَ إِلَّا حَفَّتْهُمُ الْمَلَائِكَةُ وَغَشِيَتْهُمُ الرَّحْمَةُ وَنَزَلَتْ عَلَيْهِمُ السَّكِينَةُ»",
            sourceReference = "صحيح مسلم",
            spiritualReflection = "اجعل لبيتك نصيباً من الذكر وتلاوة القرآن لتنزل عليه السكينة وتغشاه الرحمة.",
            englishText = "\"No people sit remembering Allah except that the angels surround them, mercy covers them, and tranquility descends upon them.\"",
            englishSource = "Sahih Muslim",
            englishReflection = "Fill your home with remembrance and Quran recitation so serenity and mercy descend upon it."
        ),
        DailyReflection(
            id = 15,
            kind = ReflectionKind.QURAN,
            arabicText = "«وَإِذَا سَأَلَكَ عِبَادِي عَنِّي فَإِنِّي قَرِيبٌ ۖ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ»",
            sourceReference = "سورة البقرة • آية ١٨٦",
            spiritualReflection = "لا وسيط بينك وبين خالقك؛ ناجِه بكلماتك البسيطة الصادقة في كل حين.",
            englishText = "\"And when My servants ask you concerning Me — indeed I am near. I respond to the invocation of the supplicant when he calls upon Me.\"",
            englishSource = "Surah Al-Baqarah • 2:186",
            englishReflection = "There is no barrier between you and your Creator; speak to Him in simple, honest prayer."
        ),
        DailyReflection(
            id = 16,
            kind = ReflectionKind.HADITH,
            arabicText = "«الطُّهُورُ شَطْرُ الْإِيمَانِ، وَالْحَمْدُ لِلَّهِ تَمْلَأُ الْمِيزَانَ»",
            sourceReference = "صحيح مسلم",
            spiritualReflection = "كلمة «الحمد لله» الصادقة في السراء والضراء تملأ القلب رضاً والميزان نوراً.",
            englishText = "\"Purity is half of faith, and Alhamdulillah (All praise be to Allah) fills the scale.\"",
            englishSource = "Sahih Muslim",
            englishReflection = "A sincere Alhamdulillah in both ease and trial fills the heart with contentment."
        ),
        DailyReflection(
            id = 17,
            kind = ReflectionKind.WISDOM,
            arabicText = "«أَذْكَارُ الصَّبَاحِ وَالمَسَاءِ دِرْعٌ حَصِينٌ يَحْفَظُ القَلْبَ مِنَ القَلَقِ وَالوَحْشَةِ»",
            sourceReference = "لطائف الأذكار",
            spiritualReflection = "الالتزام بأذكار الصباح والمساء يغرس في يومك سياجاً من الحفظ والطمأنينة.",
            englishText = "\"Morning and evening Adhkar form a fortress that shields the heart from anxiety and loneliness.\"",
            englishSource = "Pearls of Remembrance",
            englishReflection = "Guarding your morning and evening Adhkar surrounds your day with protection and calm."
        ),
        DailyReflection(
            id = 18,
            kind = ReflectionKind.QURAN,
            arabicText = "«سَلَامٌ قَوْلًا مِّن رَّبٍّ رَّحِيمٍ»",
            sourceReference = "سورة يس • آية ٥٨",
            spiritualReflection = "ما أجمل أن تسأل الله السلامة في دينك ودنياك وتنشر السلام فيمن حولك.",
            englishText = "\"Peace — a word from a Merciful Lord.\"",
            englishSource = "Surah Ya-Sin • 36:58",
            englishReflection = "Ask Allah for peace in faith and life, and gently spread peace to those around you."
        ),
        DailyReflection(
            id = 19,
            kind = ReflectionKind.HADITH,
            arabicText = "«تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ»",
            sourceReference = "رواه الترمذي",
            spiritualReflection = "جَمِّل ذكر الله في داخلك بحُسن الخلق ولين الكلام والابتسامة الصادقة في وجوه الناس.",
            englishText = "\"Your smile in the face of your brother is charity for you.\"",
            englishSource = "At-Tirmidhi",
            englishReflection = "Adorn your inner remembrance with gentle speech, warmth, and a sincere smile."
        ),
        DailyReflection(
            id = 20,
            kind = ReflectionKind.QURAN,
            arabicText = "«وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ»",
            sourceReference = "سورة الضحى • آية ٥",
            spiritualReflection = "عطاء الله لا يأتي على مقاس توقعاتنا المحدودة، بل يجيء أوسع وأجمل حتى يرضى القلب.",
            englishText = "\"And your Lord is going to give you, and you will be satisfied.\"",
            englishSource = "Surah Ad-Duha • 93:5",
            englishReflection = "Allah's generosity exceeds our limited expectations until the heart rests completely satisfied."
        ),
        DailyReflection(
            id = 21,
            kind = ReflectionKind.HADITH,
            arabicText = "«أَنَا عِنْدَ ظَنِّ عَبْدِي بِي، وَأَنَا مَعَهُ إِذَا ذَكَرَنِي»",
            sourceReference = "حديث قدسي • متفق عليه",
            spiritualReflection = "املأ قلبك بحسن الظن بربك؛ فهو معك أينما ذكرته في نفسك أو في ملأ.",
            englishText = "\"I am as My servant thinks of Me, and I am with him whenever he remembers Me.\"",
            englishSource = "Hadith Qudsi • Al-Bukhari & Muslim",
            englishReflection = "Fill your heart with hopeful expectation of Allah's mercy; He is ever near whenever you remember Him."
        ),
        DailyReflection(
            id = 22,
            kind = ReflectionKind.QURAN,
            arabicText = "«رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِن لَّدُنكَ رَحْمَةً»",
            sourceReference = "سورة آل عمران • آية ٨",
            spiritualReflection = "الثبات على الهداية نعمة عظمى تستحق أن نسأل الله إياها في كل سجدة.",
            englishText = "\"Our Lord, let not our hearts deviate after You have guided us and grant us from Yourself mercy.\"",
            englishSource = "Surah Ali 'Imran • 3:8",
            englishReflection = "Steadfastness upon guidance is an immense gift worth asking for in every prostration."
        ),
        DailyReflection(
            id = 23,
            kind = ReflectionKind.WISDOM,
            arabicText = "«بَرَكَةُ الوَقْتِ تَسْكُنُ السَّاعَاتِ الَّتِي تَبْدَأُ بِذِكْرِ اللَّهِ وَالصَّلَاةِ عَلَى نَبِيِّهِ ﷺ»",
            sourceReference = "حكمة يومية",
            spiritualReflection = "أكثر من الصلاة على النبي ﷺ في هذا اليوم، فهي كفاية للهمّ ومغفرة للذنب.",
            englishText = "\"Blessing in time settles into the hours begun with remembering Allah and sending blessings upon His Prophet ﷺ.\"",
            englishSource = "Daily Wisdom",
            englishReflection = "Send abundant Salawat upon the Prophet ﷺ today — it eases worries and brings forgiveness."
        ),
        DailyReflection(
            id = 24,
            kind = ReflectionKind.QURAN,
            arabicText = "«قُلْ بِفَضْلِ اللَّهِ وَبِرَحْمَتِهِ فَبِذَٰلِكَ فَلْيَفْرَحُوا هُوَ خَيْرٌ مِّمَّا يَجْمَعُونَ»",
            sourceReference = "سورة يونس • آية ٥٨",
            spiritualReflection = "أعمق فرح يعيشه المؤمن هو توفيقه للطاعة ولحظة الصفاء في مناجاة ربه.",
            englishText = "\"Say, 'In the bounty of Allah and in His mercy — in that let them rejoice; it is better than what they accumulate.'\"",
            englishSource = "Surah Yunus • 10:58",
            englishReflection = "True joy lies in moments of clarity, worship, and closeness to Allah."
        ),
        DailyReflection(
            id = 25,
            kind = ReflectionKind.HADITH,
            arabicText = "«عَجَباً لِأَمْرِ المُؤْمِنِ، إِنَّ أَمْرَهُ كُلَّهُ خَيْرٌ؛ إِنْ أَصَابَتْهُ سَرَّاءُ شَكَرَ، وَإِنْ أَصَابَتْهُ ضَرَّاءُ صَبَرَ»",
            sourceReference = "صحيح مسلم",
            spiritualReflection = "بين الشكر والصبر يرتقي المؤمن في كل أحواله ويجد في كل قدرٍ حكمةً ورحمة.",
            englishText = "\"Wondrous is the affair of the believer, for all of it is good: if ease comes, he is grateful; if hardship comes, he is patient.\"",
            englishSource = "Sahih Muslim",
            englishReflection = "Between gratitude and patience, the believer rises peacefully through every season of life."
        ),
        DailyReflection(
            id = 26,
            kind = ReflectionKind.QURAN,
            arabicText = "«وَاذْكُر رَّبَّكَ فِي نَفْسِكَ تَضَرُّعًا وَخِيفَةً وَدُونَ الْجَهْرِ مِنَ الْقَوْلِ بِالْغُدُوِّ وَالْآصَالِ»",
            sourceReference = "سورة الأعراف • آية ٢٠٥",
            spiritualReflection = "الذكر الخفي بينك وبين الله يُورث رقة في القلب وإخلاصاً بعيداً عن الرياء.",
            englishText = "\"And remember your Lord within yourself in humility and in fear without being apparent in speech — in the mornings and the evenings.\"",
            englishSource = "Surah Al-A'raf • 7:205",
            englishReflection = "Quiet remembrance between you and Allah nurtures tenderness of heart and sincerity."
        ),
        DailyReflection(
            id = 27,
            kind = ReflectionKind.WISDOM,
            arabicText = "«كُلُّ سَجْدَةٍ تَسْجُدُهَا لِلَّهِ تَرْفَعُ مِنْ رُوحِكَ وَتَضَعُ عَنْكَ ثِقَلَ الدُّنْيَا»",
            sourceReference = "إشراقة روحية",
            spiritualReflection = "تمهّل في صلاتك اليوم واستشعر أن أقرب ما تكون من ربك وأنت ساجد.",
            englishText = "\"Every prostration you make for Allah elevates your spirit and lifts the weight of the world from your shoulders.\"",
            englishSource = "Spiritual Insight",
            englishReflection = "Unrush your prayer today and cherish the closeness of prostration."
        ),
        DailyReflection(
            id = 28,
            kind = ReflectionKind.QURAN,
            arabicText = "«إِنَّ اللَّهَ وَمَلَائِكَتَهُ يُصَلُّونَ عَلَى النَّبِيِّ ۚ يَا أَيُّهَا الَّذِينَ آمَنُوا صَلُّوا عَلَيْهِ وَسَلِّمُوا تَسْلِيمًا»",
            sourceReference = "سورة الأحزاب • آية ٥٦",
            spiritualReflection = "اللهم صلِّ وسلّم على نبينا محمد؛ اجعلها ورداً عذباً يُنعش لسانك اليوم.",
            englishText = "\"Indeed, Allah confers blessing upon the Prophet, and His angels pray for him. O you who have believed, ask Allah to confer blessing upon him and ask Allah to grant him peace.\"",
            englishSource = "Surah Al-Ahzab • 33:56",
            englishReflection = "Make sending peace and blessings upon Prophet Muhammad ﷺ a sweet daily habit."
        ),
        DailyReflection(
            id = 29,
            kind = ReflectionKind.HADITH,
            arabicText = "«سَبَقَ الْمُفَرِّدُونَ: الذَّاكِرُونَ اللَّهَ كَثِيراً وَالذَّاكِرَاتُ»",
            sourceReference = "صحيح مسلم",
            spiritualReflection = "السباق الحقيقي إلى القرب من الله مفتوحٌ للجميع بكثرة الذكر وحضور القلب.",
            englishText = "\"The Mufarridun have gone ahead: the men and women who remember Allah much.\"",
            englishSource = "Sahih Muslim",
            englishReflection = "The noble race toward Allah's nearness is open to all through abundant remembrance."
        ),
        DailyReflection(
            id = 30,
            kind = ReflectionKind.QURAN,
            arabicText = "«رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ»",
            sourceReference = "سورة البقرة • آية ٢٠١",
            spiritualReflection = "دعاء جامع يضم صلاح الحال وسكينة القلب في الدنيا، والنجاة والرضوان في الآخرة.",
            englishText = "\"Our Lord, give us in this world good and in the Hereafter good, and protect us from the punishment of the Fire.\"",
            englishSource = "Surah Al-Baqarah • 2:201",
            englishReflection = "A complete prayer gathering peace and goodness in this life and eternal felicity in the next."
        )
    )

    fun epochDayFor(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val localMillis = nowMillis + timeZone.getOffset(nowMillis)
        return Math.floorDiv(localMillis, REFRESH_INTERVAL_MILLIS)
    }

    fun reflectionForEpochDay(epochDay: Long): DailyReflection {
        val index = Math.floorMod(epochDay, entries.size.toLong()).toInt()
        return entries[index]
    }

    fun reflectionForTimestamp(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): DailyReflection {
        val day = epochDayFor(nowMillis, timeZone)
        return reflectionForEpochDay(day)
    }

    fun millisUntilNextRefresh(
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val offset = timeZone.getOffset(nowMillis)
        val localMillis = nowMillis + offset
        val currentDay = Math.floorDiv(localMillis, REFRESH_INTERVAL_MILLIS)
        val nextDayLocalStart = (currentDay + 1L) * REFRESH_INTERVAL_MILLIS
        return (nextDayLocalStart - localMillis).coerceAtLeast(1_000L)
    }
}
