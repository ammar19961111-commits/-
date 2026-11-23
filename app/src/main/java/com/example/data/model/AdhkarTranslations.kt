package com.example.data.model

data class EnglishDhikrTranslation(
    val titleEn: String,
    val translationEn: String,
    val virtueEn: String,
    val sourceEn: String
)

data class EnglishCounterTranslation(
    val titleEn: String,
    val meaningEn: String,
    val virtueEn: String
)

object AdhkarTranslations {

    private val dhikrMap: Map<Int, EnglishDhikrTranslation> = mapOf(
        // Morning Adhkar
        101 to EnglishDhikrTranslation(
            titleEn = "Ayat al-Kursi (Verse of the Throne)",
            translationEn = "I seek refuge in Allah from the accursed Satan.\nAllah — there is no deity worthy of worship except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth. Who is it that can intercede with Him except by His permission? He knows what is before them and what will be after them, and they encompass not a thing of His knowledge except for what He wills. His Kursi extends over the heavens and the earth, and their preservation tires Him not. And He is the Most High, the Most Great.",
            virtueEn = "Whoever recites it in the morning will be protected from the jinn until evening.",
            sourceEn = "Surah Al-Baqarah: 255"
        ),
        102 to EnglishDhikrTranslation(
            titleEn = "Al-Mu'awwidhat (Al-Ikhlas, Al-Falaq, An-Nas)",
            translationEn = "In the name of Allah, the Entirely Merciful, the Especially Merciful.\nSay, \"He is Allah, Who is One. Allah, the Eternal Refuge. He neither begets nor is born, nor is there to Him any equivalent.\"\n\nSay, \"I seek refuge in the Lord of daybreak, from the evil of that which He created, and from the evil of darkness when it settles, and from the evil of the blowers in knots, and from the evil of an envier when he envies.\"\n\nSay, \"I seek refuge in the Lord of mankind, the Sovereign of mankind, the God of mankind, from the evil of the retreating whisperer who whispers into the breasts of mankind, from among the jinn and mankind.\"",
            virtueEn = "Reciting these three surahs three times in the morning and evening suffices you against all harm.",
            sourceEn = "Abu Dawud & At-Tirmidhi"
        ),
        103 to EnglishDhikrTranslation(
            titleEn = "Morning Praise & Seeking Refuge",
            translationEn = "We have reached the morning and at this very time all sovereignty belongs to Allah, and all praise is for Allah. None has the right to be worshipped except Allah, alone, without partner; to Him belongs all sovereignty and praise, and He is over all things omnipotent. My Lord, I ask You for the good of this day and the good of what follows it, and I seek refuge in You from the evil of this day and the evil of what follows it. My Lord, I seek refuge in You from laziness and helpless old age. My Lord, I seek refuge in You from torment in the Fire and torment in the grave.",
            virtueEn = "A comprehensive supplication seeking the good of the day and protection from its evils.",
            sourceEn = "Sahih Muslim"
        ),
        104 to EnglishDhikrTranslation(
            titleEn = "Sayyid al-Istighfar (Master Supplication for Forgiveness)",
            translationEn = "O Allah, You are my Lord; there is none worthy of worship except You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me and I acknowledge my sin, so forgive me, for verily none can forgive sins except You.",
            virtueEn = "Whoever recites it with conviction in the morning and passes away that day will enter Paradise.",
            sourceEn = "Sahih al-Bukhari"
        ),
        105 to EnglishDhikrTranslation(
            titleEn = "Pleased with Allah as Lord",
            translationEn = "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad (peace and blessings be upon him) as my Prophet.",
            virtueEn = "Whoever says this three times in the morning and evening, Allah has promised to please them on the Day of Resurrection.",
            sourceEn = "Ahmad & Abu Dawud"
        ),
        106 to EnglishDhikrTranslation(
            titleEn = "Sufficient for Me is Allah",
            translationEn = "Allah is sufficient for me; there is none worthy of worship except Him. In Him I put my trust, and He is the Lord of the Mighty Throne.",
            virtueEn = "Whoever recites it seven times, Allah will suffice them against everything that worries them in this world and the next.",
            sourceEn = "Abu Dawud"
        ),
        107 to EnglishDhikrTranslation(
            titleEn = "In the Name of Allah",
            translationEn = "In the name of Allah, with Whose Name nothing on the earth or in the heavens can cause harm, and He is the All-Hearing, the All-Knowing.",
            virtueEn = "Whoever recites it three times will not be harmed by anything.",
            sourceEn = "At-Tirmidhi & Abu Dawud"
        ),
        108 to EnglishDhikrTranslation(
            titleEn = "Prayer for Well-Being in Both Worlds",
            translationEn = "O Allah, I ask You for pardon and well-being in this life and the Hereafter. O Allah, I ask You for pardon and well-being in my religion, my worldly affairs, my family, and my wealth. O Allah, conceal my faults and calm my fears. O Allah, protect me from before me and from behind me, from my right and from my left, and from above me, and I seek refuge in Your greatness from being taken unawares from beneath me.",
            virtueEn = "The Prophet (peace be upon him) never left this supplication in the evening or morning.",
            sourceEn = "Abu Dawud & Ibn Majah"
        ),
        109 to EnglishDhikrTranslation(
            titleEn = "O Ever-Living, O Sustainer",
            translationEn = "O Ever-Living, O Sustainer of all, in Your mercy I seek relief; set right all my affairs and do not leave me to myself even for the blink of an eye.",
            virtueEn = "Advised by the Prophet (peace be upon him) to Fatimah (may Allah be pleased with her) morning and evening.",
            sourceEn = "An-Nasa'i & Al-Hakim"
        ),
        110 to EnglishDhikrTranslation(
            titleEn = "Glory Be to Allah as Numerous as His Creation",
            translationEn = "Glory and praise be to Allah, as numerous as His creation, as much as pleases Him, as weighty as His Throne, and as vast as the ink of His words.",
            virtueEn = "Equals hours of continuous remembrance and carries immense reward.",
            sourceEn = "Sahih Muslim"
        ),
        111 to EnglishDhikrTranslation(
            titleEn = "Glory and Praise Be to Allah",
            translationEn = "Glory and praise be to Allah.",
            virtueEn = "Whoever recites it one hundred times will come on the Day of Resurrection with a reward surpassed by none except one who recited as much or more.",
            sourceEn = "Sahih Muslim"
        ),
        112 to EnglishDhikrTranslation(
            titleEn = "Declaration of Allah's Oneness (Tahlil)",
            translationEn = "None has the right to be worshipped except Allah, alone, without partner; to Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            virtueEn = "Counts as freeing ten slaves, records one hundred good deeds, erases one hundred sins, and serves as a shield against Satan.",
            sourceEn = "Al-Bukhari & Muslim"
        ),

        // Evening Adhkar
        201 to EnglishDhikrTranslation(
            titleEn = "Ayat al-Kursi (Verse of the Throne)",
            translationEn = "I seek refuge in Allah from the accursed Satan.\nAllah — there is no deity worthy of worship except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth. Who is it that can intercede with Him except by His permission? He knows what is before them and what will be after them, and they encompass not a thing of His knowledge except for what He wills. His Kursi extends over the heavens and the earth, and their preservation tires Him not. And He is the Most High, the Most Great.",
            virtueEn = "Whoever recites it in the evening will be protected from the jinn until morning.",
            sourceEn = "Surah Al-Baqarah: 255"
        ),
        202 to EnglishDhikrTranslation(
            titleEn = "Al-Mu'awwidhat (Al-Ikhlas, Al-Falaq, An-Nas)",
            translationEn = "In the name of Allah, the Entirely Merciful, the Especially Merciful.\nSay, \"He is Allah, Who is One. Allah, the Eternal Refuge. He neither begets nor is born, nor is there to Him any equivalent.\"\n\nSay, \"I seek refuge in the Lord of daybreak, from the evil of that which He created, and from the evil of darkness when it settles, and from the evil of the blowers in knots, and from the evil of an envier when he envies.\"\n\nSay, \"I seek refuge in the Lord of mankind, the Sovereign of mankind, the God of mankind, from the evil of the retreating whisperer who whispers into the breasts of mankind, from among the jinn and mankind.\"",
            virtueEn = "Reciting these three surahs three times in the evening suffices you against everything.",
            sourceEn = "Abu Dawud & At-Tirmidhi"
        ),
        203 to EnglishDhikrTranslation(
            titleEn = "Evening Praise & Seeking Refuge",
            translationEn = "We have reached the evening and at this very time all sovereignty belongs to Allah, and all praise is for Allah. None has the right to be worshipped except Allah, alone, without partner; to Him belongs all sovereignty and praise, and He is over all things omnipotent. My Lord, I ask You for the good of this night and the good of what follows it, and I seek refuge in You from the evil of this night and the evil of what follows it. My Lord, I seek refuge in You from laziness and helpless old age. My Lord, I seek refuge in You from torment in the Fire and torment in the grave.",
            virtueEn = "A comprehensive evening supplication seeking the night's blessings and protection from harm.",
            sourceEn = "Sahih Muslim"
        ),
        204 to EnglishDhikrTranslation(
            titleEn = "Sayyid al-Istighfar (Master Supplication for Forgiveness)",
            translationEn = "O Allah, You are my Lord; there is none worthy of worship except You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me and I acknowledge my sin, so forgive me, for verily none can forgive sins except You.",
            virtueEn = "Whoever recites it with conviction in the evening and passes away that night will enter Paradise.",
            sourceEn = "Sahih al-Bukhari"
        ),
        205 to EnglishDhikrTranslation(
            titleEn = "By Allah We Reach Evening and Morning",
            translationEn = "O Allah, by You we enter the evening, and by You we enter the morning; by You we live, and by You we die, and to You is the final return.",
            virtueEn = "A reminder of Allah's grace and renewed reliance upon Him each evening.",
            sourceEn = "At-Tirmidhi & Abu Dawud"
        ),
        206 to EnglishDhikrTranslation(
            titleEn = "Seeking Refuge in Allah's Perfect Words",
            translationEn = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            virtueEn = "Whoever recites it three times in the evening will not be harmed by any sting or venom that night.",
            sourceEn = "Sahih Muslim"
        ),
        207 to EnglishDhikrTranslation(
            titleEn = "In the Name of Allah",
            translationEn = "In the name of Allah, with Whose Name nothing on the earth or in the heavens can cause harm, and He is the All-Hearing, the All-Knowing.",
            virtueEn = "Whoever says this three times in the evening will not suffer sudden affliction until morning.",
            sourceEn = "At-Tirmidhi & Abu Dawud"
        ),
        208 to EnglishDhikrTranslation(
            titleEn = "Evening Gratitude for Every Blessing",
            translationEn = "O Allah, whatever blessing has reached me or any of Your creation this evening is from You alone, without partner; so to You belongs all praise and to You belongs all thanks.",
            virtueEn = "Whoever recites it in the evening has fulfilled their gratitude for that night.",
            sourceEn = "Abu Dawud & An-Nasa'i"
        ),
        209 to EnglishDhikrTranslation(
            titleEn = "Sufficient for Me is Allah",
            translationEn = "Allah is sufficient for me; there is none worthy of worship except Him. In Him I put my trust, and He is the Lord of the Mighty Throne.",
            virtueEn = "Whoever recites it seven times, Allah will suffice them in all their concerns.",
            sourceEn = "Abu Dawud"
        ),
        210 to EnglishDhikrTranslation(
            titleEn = "Glory and Praise Be to Allah",
            translationEn = "Glory and praise be to Allah.",
            virtueEn = "Sins are forgiven even if they are as abundant as the foam of the sea.",
            sourceEn = "Al-Bukhari & Muslim"
        ),

        // After-Prayer Adhkar
        301 to EnglishDhikrTranslation(
            titleEn = "Seeking Forgiveness & Peace",
            translationEn = "I seek forgiveness from Allah (three times).\nO Allah, You are Peace and from You comes peace; blessed are You, O Possessor of Majesty and Honor.",
            virtueEn = "When the Prophet (peace be upon him) concluded his prayer, he would seek forgiveness three times and say these words.",
            sourceEn = "Sahih Muslim"
        ),
        302 to EnglishDhikrTranslation(
            titleEn = "Tahlil After Prayer",
            translationEn = "None has the right to be worshipped except Allah, alone, without partner; to Him belongs all sovereignty and praise, and He is over all things omnipotent. O Allah, none can withhold what You give, and none can give what You withhold, and the wealth of the fortunate cannot avail them against You.",
            virtueEn = "The Prophet (peace be upon him) used to proclaim this after every obligatory prayer.",
            sourceEn = "Al-Bukhari & Muslim"
        ),
        303 to EnglishDhikrTranslation(
            titleEn = "SubhanAllah (33x)",
            translationEn = "Glory be to Allah.",
            virtueEn = "Whoever glorifies Allah 33 times after every prayer has their sins forgiven.",
            sourceEn = "Sahih Muslim"
        ),
        304 to EnglishDhikrTranslation(
            titleEn = "Alhamdulillah (33x)",
            translationEn = "All praise is due to Allah.",
            virtueEn = "And praises Allah 33 times after the prayer.",
            sourceEn = "Sahih Muslim"
        ),
        305 to EnglishDhikrTranslation(
            titleEn = "Allahu Akbar (33x)",
            translationEn = "Allah is the Greatest.",
            virtueEn = "And magnifies Allah 33 times, making ninety-nine in total.",
            sourceEn = "Sahih Muslim"
        ),
        306 to EnglishDhikrTranslation(
            titleEn = "Completing the Hundred",
            translationEn = "None has the right to be worshipped except Allah, alone, without partner; to Him belongs all sovereignty and praise, and He is over all things omnipotent.",
            virtueEn = "Completing the hundred after every obligatory prayer brings forgiveness even if sins are like the foam of the sea.",
            sourceEn = "Sahih Muslim"
        ),
        307 to EnglishDhikrTranslation(
            titleEn = "Ayat al-Kursi After Prayer",
            translationEn = "Allah — there is no deity worthy of worship except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth. Who is it that can intercede with Him except by His permission? He knows what is before them and what will be after them, and they encompass not a thing of His knowledge except for what He wills. His Kursi extends over the heavens and the earth, and their preservation tires Him not. And He is the Most High, the Most Great.",
            virtueEn = "Whoever recites it after every obligatory prayer, nothing prevents them from entering Paradise except death.",
            sourceEn = "An-Nasa'i"
        ),
        308 to EnglishDhikrTranslation(
            titleEn = "O Allah, Help Me Remember You",
            translationEn = "O Allah, help me to remember You, to thank You, and to worship You in the best manner.",
            virtueEn = "The Prophet (peace be upon him) advised Mu'adh ibn Jabal never to omit this after any prayer.",
            sourceEn = "Abu Dawud & An-Nasa'i"
        ),

        // Sleep Adhkar
        401 to EnglishDhikrTranslation(
            titleEn = "Reciting Al-Mu'awwidhat Before Sleep",
            translationEn = "Cup your hands together, blow gently into them, and recite Surah Al-Ikhlas, Surah Al-Falaq, and Surah An-Nas, then wipe over as much of your body as you can, beginning with your head, face, and front of the body.",
            virtueEn = "The Prophet (peace be upon him) used to do this three times every night when going to bed.",
            sourceEn = "Sahih al-Bukhari"
        ),
        402 to EnglishDhikrTranslation(
            titleEn = "Ayat al-Kursi Before Sleep",
            translationEn = "Allah — there is no deity worthy of worship except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and whatever is on the earth. Who is it that can intercede with Him except by His permission? He knows what is before them and what will be after them, and they encompass not a thing of His knowledge except for what He wills. His Kursi extends over the heavens and the earth, and their preservation tires Him not. And He is the Most High, the Most Great.",
            virtueEn = "A guardian from Allah will remain with you and no devil will come near you until morning.",
            sourceEn = "Sahih al-Bukhari"
        ),
        403 to EnglishDhikrTranslation(
            titleEn = "In Your Name I Lie Down",
            translationEn = "In Your name, my Lord, I lie down on my side, and in Your name I raise it. If You should take my soul, then have mercy upon it; and if You should return it, then protect it as You protect Your righteous servants.",
            virtueEn = "A supplication for the protection of one's soul when lying down to sleep.",
            sourceEn = "Al-Bukhari & Muslim"
        ),
        404 to EnglishDhikrTranslation(
            titleEn = "In Your Name I Die and Live",
            translationEn = "In Your name, O Allah, I die and I live.",
            virtueEn = "The Prophet (peace be upon him) used to say this when retiring to his bed at night.",
            sourceEn = "Sahih al-Bukhari"
        ),
        405 to EnglishDhikrTranslation(
            titleEn = "Tasbih Fatimah Before Sleep",
            translationEn = "Glory be to Allah (33) • All praise is for Allah (33) • Allah is the Greatest (34).",
            virtueEn = "Better than a helper; brings strength and blessing for the next day.",
            sourceEn = "Al-Bukhari & Muslim"
        ),
        406 to EnglishDhikrTranslation(
            titleEn = "O Allah, I Submit Myself to You",
            translationEn = "O Allah, I have submitted my soul to You, entrusted my affairs to You, turned my face to You, and relied completely upon You out of hope and reverence for You. There is no refuge nor escape from You except in You. I believe in Your Book which You revealed and Your Prophet whom You sent.",
            virtueEn = "Whoever recites this and passes away that night dies upon the natural state of faith (Fitrah).",
            sourceEn = "Al-Bukhari & Muslim"
        ),

        // Waking Adhkar
        501 to EnglishDhikrTranslation(
            titleEn = "Praise Upon Waking Up",
            translationEn = "All praise is for Allah who gave us life after having taken it from us, and unto Him is the resurrection.",
            virtueEn = "The very first words a believer says upon waking, in gratitude to Allah.",
            sourceEn = "Sahih al-Bukhari"
        ),
        502 to EnglishDhikrTranslation(
            titleEn = "Gratitude for Health & Life",
            translationEn = "All praise is for Allah who restored to me my health, returned my soul to me, and allowed me to remember Him.",
            virtueEn = "Thanking Allah for well-being, the return of one's soul, and the blessing of remembrance.",
            sourceEn = "At-Tirmidhi"
        ),
        503 to EnglishDhikrTranslation(
            titleEn = "Remembrance When Waking at Night",
            translationEn = "None has the right to be worshipped except Allah, alone, without partner; to Him belongs all sovereignty and praise, and He is over all things omnipotent. All praise is for Allah, glory be to Allah, none has the right to be worshipped except Allah, Allah is the Greatest, and there is no power nor strength except through Allah. O Allah, forgive me.",
            virtueEn = "Whoever wakes at night and says this, then supplicates, will be answered; and if they perform ablution and pray, their prayer will be accepted.",
            sourceEn = "Sahih al-Bukhari"
        ),

        // Quranic & Prophetic Duas
        601 to EnglishDhikrTranslation(
            titleEn = "Good in This World and the Hereafter",
            translationEn = "Our Lord, give us in this world good and in the Hereafter good, and protect us from the punishment of the Fire.",
            virtueEn = "This was the most frequent supplication of the Prophet (peace be upon him).",
            sourceEn = "Surah Al-Baqarah: 201"
        ),
        602 to EnglishDhikrTranslation(
            titleEn = "Steadfastness Upon Faith",
            translationEn = "O Turner of the hearts, keep my heart firm upon Your religion.",
            virtueEn = "One of the Prophet's most frequent prayers asking for steadfastness and guidance.",
            sourceEn = "At-Tirmidhi"
        ),
        603 to EnglishDhikrTranslation(
            titleEn = "Guidance, Piety, Chastity & Contentment",
            translationEn = "O Allah, I ask You for guidance, piety, chastity, and self-sufficiency.",
            virtueEn = "A comprehensive supplication encompassing purity of heart, character, and provision.",
            sourceEn = "Sahih Muslim"
        ),
        604 to EnglishDhikrTranslation(
            titleEn = "Relief from Distress",
            translationEn = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            virtueEn = "The supplication of Yunus (Dhul-Nun); no Muslim calls upon Allah with it for anything except that Allah answers them.",
            sourceEn = "Surah Al-Anbiya: 87 • At-Tirmidhi"
        ),
        605 to EnglishDhikrTranslation(
            titleEn = "Ease, Clarity & Peace of Chest",
            translationEn = "My Lord, expand for me my chest, ease for me my task, and untie the knot from my tongue that they may understand my speech.",
            virtueEn = "The prayer of Prophet Musa (peace be upon him) asking for serenity, ease, and eloquence.",
            sourceEn = "Surah Ta-Ha: 25-28"
        ),
        606 to EnglishDhikrTranslation(
            titleEn = "Prayer for Forgiveness & Mercy",
            translationEn = "Our Lord, we have wronged ourselves, and if You do not forgive us and have mercy upon us, we will surely be among the losers.",
            virtueEn = "The prayer of Adam and Hawwa (peace be upon them) seeking forgiveness and mercy.",
            sourceEn = "Surah Al-A'raf: 23"
        ),
        607 to EnglishDhikrTranslation(
            titleEn = "As-Salat al-Ibrahimiyyah (Blessings on the Prophet)",
            translationEn = "O Allah, send prayers upon Muhammad and upon the family of Muhammad, as You sent prayers upon Ibrahim and upon the family of Ibrahim; indeed You are Praiseworthy and Glorious. O Allah, send blessings upon Muhammad and upon the family of Muhammad, as You sent blessings upon Ibrahim and upon the family of Ibrahim; indeed You are Praiseworthy and Glorious.",
            virtueEn = "Whoever sends blessings upon the Prophet once, Allah sends blessings upon them tenfold.",
            sourceEn = "Al-Bukhari & Muslim"
        )
    )

    private val counterMap: Map<Int, EnglishCounterTranslation> = mapOf(
        1 to EnglishCounterTranslation(
            titleEn = "SubhanAllah",
            meaningEn = "Glory be to Allah",
            virtueEn = "Fills the scale of good deeds and erases sins"
        ),
        2 to EnglishCounterTranslation(
            titleEn = "Alhamdulillah",
            meaningEn = "All praise is due to Allah",
            virtueEn = "Fills the scale and is the finest supplication"
        ),
        3 to EnglishCounterTranslation(
            titleEn = "Allahu Akbar",
            meaningEn = "Allah is the Greatest",
            virtueEn = "Magnifies the Almighty above all things"
        ),
        4 to EnglishCounterTranslation(
            titleEn = "La ilaha illallah",
            meaningEn = "There is no deity worthy of worship except Allah",
            virtueEn = "The best remembrance and heaviest word on the scale"
        ),
        5 to EnglishCounterTranslation(
            titleEn = "Astaghfirullah al-Azim",
            meaningEn = "I seek forgiveness from Allah, the Almighty",
            virtueEn = "Allah grants a way out from every worry and distress"
        ),
        6 to EnglishCounterTranslation(
            titleEn = "La hawla wa la quwwata illa billah",
            meaningEn = "There is no power nor strength except through Allah",
            virtueEn = "A treasure from the treasures of Paradise"
        ),
        7 to EnglishCounterTranslation(
            titleEn = "Salawat upon the Prophet ﷺ",
            meaningEn = "O Allah, send peace and blessings upon Prophet Muhammad",
            virtueEn = "One blessing brings ten blessings from Allah in return"
        )
    )

    fun forDhikr(id: Int): EnglishDhikrTranslation? = dhikrMap[id]

    fun forCounter(id: Int): EnglishCounterTranslation? = counterMap[id]
}
