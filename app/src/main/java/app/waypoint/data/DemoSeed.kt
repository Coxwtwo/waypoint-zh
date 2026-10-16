package app.waypoint.data

import app.waypoint.domain.Area
import app.waypoint.domain.Method
import app.waypoint.domain.NodeKind
import app.waypoint.domain.World
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.pow

/** Made-up goals, steps and reviews for README screenshots; only with `-Pdemo` (app.waypoint.demo). */
object DemoSeed {
    private var ru = false
    private fun t(en: String, r: String) = if (ru) r else en

    suspend fun seedIfEmpty(dao: WaypointDao, settings: SettingsStore) {
        if (dao.goals().isNotEmpty()) return
        ru = Locale.getDefault().language == "ru"
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone).toEpochDay()
        val now = System.currentTimeMillis()
        fun at(day: Long, hour: Int = 9) = LocalDate.ofEpochDay(day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        // Deterministic "random" misses, so screenshots look lived-in but stay the same.
        fun miss(day: Long, salt: Int, every: Int) = Math.floorMod((day * 31 + salt * 17).toInt(), every) == 0

        suspend fun goal(g: Goal): Long = dao.insertGoal(g)
        suspend fun node(n: Node): Long = dao.insertNode(n)
        suspend fun log(n: Long, g: Long, day: Long, v: Double = 1.0) = dao.upsertLog(Log(n, g, day, v, at(day, 8)))
        suspend fun field(g: Long, k: String, v: String) = dao.upsertField(Field(g, k, v))

        // 1. Mandala 64: a marathon.
        val mStart = today - 45
        val marathon = goal(
            Goal(
                title = t("Run a marathon in under 4 hours", "Пробежать марафон быстрее 4 часов"), method = Method.MANDALA.name,
                why = t("To prove to myself that I can do hard things", "Доказать себе, что я могу сложное"),
                area = Area.HEALTH.ordinal, startDay = mStart, endDay = today + 186, seed = 314, createdAt = at(mStart), sort = 0,
            )
        )
        val pillars = listOf(
            t("Endurance", "Выносливость") to listOf(t("Long run 25 km", "Длинная 25 км"), t("Easy run 8 km", "Лёгкий бег 8 км"), t("Zone 2", "Зона 2"), t("+10% a week", "+10% в неделю"), t("Cycling", "Велосипед"), t("Hills", "Горки"), t("Heart-rate strap", "Пульсометр"), t("Half marathon", "Полумарафон")),
            t("Speed", "Скорость") to listOf(t("Intervals", "Интервалы"), t("Tempo run", "Темповый"), t("Fartlek", "Фартлек"), t("Track day", "Стадион"), "8×400", t("Cadence 180", "Каденс 180"), t("Final sprint", "Финиш-спурт"), t("10 km test", "Тест 10 км")),
            t("Food", "Питание") to listOf(t("Gels", "Гели"), t("Carbs", "Углеводы"), t("2 l of water", "Вода 2 л"), t("Less sugar", "Меньше сахара"), t("Breakfast", "Завтрак"), t("Protein", "Белок"), t("No alcohol", "Без алкоголя"), t("Test a gel", "Пробный гель")),
            t("Sleep", "Сон") to listOf(t("Bed by 23:00", "Отбой в 23:00"), t("8 hours", "8 часов"), t("No screens", "Без экрана"), t("Dark room", "Темнота"), t("Routine", "Режим"), t("Nap", "Дневной сон"), t("Magnesium", "Магний"), t("Tracker", "Трекер")),
            t("Strength", "Сила") to listOf(t("Squats", "Присед"), t("Plank", "Планка"), t("Lunges", "Выпады"), t("Glutes", "Ягодицы"), t("Core", "Кор"), t("Calves", "Икры"), t("Gym twice a week", "Зал 2 раза в неделю"), t("Mobility", "Мобильность")),
            t("Technique", "Техника") to listOf(t("Stretch 15 min", "Растяжка 15 минут"), t("Posture", "Осанка"), t("Cadence", "Каденс"), t("Video check", "Видео-разбор"), t("Breathing", "Дыхание"), t("Foot strike", "Постановка стопы"), t("Arms", "Руки"), t("Foam roller", "Ролл")),
            t("Mind", "Голова") to listOf(t("Visualise", "Визуализация"), t("Mantra", "Мантра"), t("Race plan", "План забега"), t("Meditation", "Медитация"), t("Podcasts", "Подкасты"), t("Run diary", "Дневник"), t("Running club", "Клуб"), t("Pre-mortem", "Пре-мортем")),
            t("Gear", "Экипировка") to listOf(t("Shoes", "Кроссовки"), t("Socks", "Носки"), t("Watch", "Часы"), t("Belt", "Пояс"), t("Cap", "Кепка"), t("Test the kit", "Тест формы"), t("Anti-chafe", "Вазелин"), t("Race number", "Номер")),
        )
        val lit = listOf(5, 3, 4, 2, 3, 1, 4, 2)
        pillars.forEachIndexed { p, (name, actions) ->
            val pid = node(Node(goalId = marathon, kind = NodeKind.PILLAR.name, slot = p, title = name))
            actions.forEachIndexed { a, title ->
                // A few actions are habits that show up on Today.
                val days = when {
                    p == 0 && a == 1 -> 0b0010101 // Mon, Wed, Fri
                    p == 5 && a == 0 -> World.EVERY_DAY
                    p == 4 && a == 6 -> 0
                    else -> 0
                }
                val perWeek = if (p == 4 && a == 6) 2 else 0
                val done = a < lit[p] && !(p == 0 && a == 1) && !(p == 5 && a == 0)
                val id = node(
                    Node(
                        goalId = marathon, parentId = pid, kind = NodeKind.ACTION.name, slot = a, title = title, days = days, perWeek = perWeek,
                        done = done, doneAt = if (done) at(today - 40 + p * 4 + a) else null, remindAt = if (p == 5 && a == 0) 21 * 60 else -1,
                    )
                )
                if (days != 0) for (d in today - 30 until today) if (planned(days, d) && !miss(d, a + p, 6)) log(id, marathon, d)
                if (perWeek > 0) for (d in today - 28 until today) if (Math.floorMod(d, 7L) == 1L || Math.floorMod(d, 7L) == 4L) log(id, marathon, d)
            }
        }
        field(marathon, "woop.wish", t("Finish a marathon in under 4 hours", "Пробежать марафон быстрее 4 часов"))
        field(marathon, "woop.outcome", t("Crossing the line, the clock still showing 3:5x, and calling Mum from the finish", "Пересекаю финиш, на часах ещё 3:5x, звоню маме прямо с финиша"))
        field(marathon, "woop.obstacle", t("I skip runs when it rains or I'm tired after work", "Пропускаю пробежки в дождь или когда устаю после работы"))
        node(Node(goalId = marathon, kind = NodeKind.PLAN.name, title = t("it's raining in the morning", "утром идёт дождь"), note = t("I'll do 40 minutes on the treadmill", "я пробегу 40 минут на дорожке в зале")))
        node(Node(goalId = marathon, kind = NodeKind.PLAN.name, title = t("I'm too tired after work", "после работы нет сил"), note = t("I'll walk for 20 minutes at least", "я выйду хотя бы на 20 минут пешком")))

        // 2. OKR: English at work.
        val oStart = today - 40
        val okr = goal(
            Goal(
                title = t("Speak English well enough to work in it", "Заговорить по-английски так, чтобы работать на нём"), method = Method.OKR.name,
                why = t("A job in an international team", "Работа в международной команде"),
                area = Area.CAREER.ordinal, startDay = oStart, endDay = oStart + 90, seed = 2718, createdAt = at(oStart), sort = 1,
            )
        )
        val krs = listOf(
            Triple(t("Learn new words", "Выучить новые слова"), Triple(300.0, 1500.0, 980.0), t("words", "слов")),
            Triple(t("Calls held in English", "Провести созвоны на английском"), Triple(0.0, 12.0, 5.0), ""),
            Triple(t("Read a book in English", "Прочитать книгу на английском"), Triple(0.0, 10.0, 7.0), t("chapters", "глав")),
        )
        krs.forEachIndexed { i, (title, v, unit) ->
            val kr = node(Node(goalId = okr, kind = NodeKind.KR.name, slot = i, title = title, start = v.first, target = v.second, current = v.third, unit = unit))
            // Weekly check-ins climbing to today's value.
            for (wk in 0..5) {
                val d = oStart + wk * 7 + 6
                if (d >= today) break
                log(kr, okr, d, v.first + (v.third - v.first) * (wk + 1) / 6.0)
            }
            if (i == 0) {
                val anki = node(Node(goalId = okr, parentId = kr, kind = NodeKind.ACTION.name, title = t("30 minutes of Anki", "30 минут Anki"), days = World.EVERY_DAY, remindAt = 8 * 60))
                for (d in today - 35 until today) if (!miss(d, 3, 7)) log(anki, okr, d)
                log(anki, okr, today)
            }
            if (i == 1) node(Node(goalId = okr, parentId = kr, kind = NodeKind.ACTION.name, title = t("Speaking club", "Разговорный клуб"), days = 0b0001000))
        }
        field(okr, "okr.confidence", "7")
        field(okr, "smart.s", t("Hold a 30-minute work call in English without switching to Russian", "Провести 30-минутный рабочий созвон на английском, не переходя на русский"))
        field(okr, "smart.m", t("12 calls and 1500 words", "12 созвонов и 1500 слов"))
        field(okr, "smart.t", t("By the end of the quarter", "До конца квартала"))

        // 3. 12-week year: a portfolio site, week 5.
        val wStart = today - 32
        val site = goal(
            Goal(
                title = t("Launch my portfolio website", "Запустить сайт-портфолио"), method = Method.WEEKS12.name,
                area = Area.CAREER.ordinal, startDay = wStart, endDay = wStart + 83, seed = 1618, createdAt = at(wStart), sort = 2,
            )
        )
        field(site, "w12.vision", t("Clients find me through my own site, not through marketplaces", "Клиенты находят меня через мой сайт, а не через биржи"))
        val design = node(Node(goalId = site, kind = NodeKind.TACTIC.name, title = t("An hour of design", "Час на дизайн"), days = World.WEEKDAYS))
        for (d in wStart until today) if (planned(World.WEEKDAYS, d) && !miss(d, 5, 5)) log(design, site, d)
        val weekly = listOf(
            t("Pick a domain", "Выбрать домен"), t("Sketch the home page", "Набросать главную"), t("Choose the fonts", "Выбрать шрифты"),
            t("Write the first case study", "Написать первый кейс"),
        )
        weekly.forEachIndexed { i, title ->
            node(Node(goalId = site, kind = NodeKind.TACTIC.name, week = i + 1, title = title, done = true, doneAt = at(wStart + i * 7 + 3), dueDay = wStart + i * 7 + 3))
        }
        node(Node(goalId = site, kind = NodeKind.TACTIC.name, week = 5, title = t("Lay out the About page", "Сверстать страницу «Обо мне»"), dueDay = today, remindAt = 19 * 60))
        val cases = node(Node(goalId = site, kind = NodeKind.TACTIC.name, week = 5, title = t("Write a case study", "Написать кейс"), perWeek = 2))
        log(cases, site, World.weekStartOf(today))
        node(Node(goalId = site, kind = NodeKind.RISK.name, title = t("I got stuck polishing the design forever", "Я застрял в бесконечной полировке дизайна"), note = t("I publish a page as soon as it's good enough, and improve it live", "публикую страницу, как только она достаточно хороша, и дорабатываю вживую")))

        // 4. 30-day pact: meditation, day 18.
        val pStart = today - 17
        val pact = goal(
            Goal(
                title = t("Meditate for 10 minutes every morning", "Медитировать 10 минут каждое утро"), method = Method.PACT.name,
                area = Area.GROWTH.ordinal, startDay = pStart, endDay = pStart + 29, seed = 1414, createdAt = at(pStart), sort = 3,
            )
        )
        val sit = node(Node(goalId = pact, kind = NodeKind.ACTION.name, title = t("Meditate for 10 minutes every morning", "Медитировать 10 минут каждое утро"), days = World.EVERY_DAY, remindAt = 7 * 60 + 30))
        for (d in pStart until today) if (d != pStart + 6 && d != pStart + 11) log(sit, pact, d)
        field(pact, "pact.note.${pStart + 3}", t("Hard to sit still. Counted breaths to ten.", "Трудно усидеть. Считала вдохи до десяти."))
        field(pact, "pact.note.${pStart + 9}", t("First morning I didn't reach for the phone first.", "Первое утро, когда я не схватилась сразу за телефон."))
        field(pact, "pact.note.${today - 1}", t("Calmer on the commute. Interesting.", "В дороге стало спокойнее. Интересно."))
        field(pact, "whys.1", t("To feel calmer", "Чтобы чувствовать себя спокойнее"))
        field(pact, "whys.2", t("So I stop snapping at people I love", "Чтобы перестать срываться на близких"))
        field(pact, "whys.3", t("Because I want to be the person they can lean on", "Потому что хочу быть человеком, на которого можно опереться"))

        // 5. Kaizen: push-ups, 1 % a day.
        val kStart = today - 25
        val push = goal(
            Goal(
                title = t("Push-ups", "Отжимания"), method = Method.KAIZEN.name, area = Area.HEALTH.ordinal, startDay = kStart,
                unit = t("reps", "раз"), startValue = 10.0, targetValue = 50.0, currentValue = 12.0, seed = 577, createdAt = at(kStart), sort = 4,
            )
        )
        field(push, "kaizen.rate", "1")
        val reps = node(Node(goalId = push, kind = NodeKind.ACTION.name, title = t("Push-ups", "Отжимания"), days = World.EVERY_DAY))
        for (d in kStart until today) {
            if (miss(d, 9, 8)) continue
            val target = ceil(10.0 * 1.01.pow((d - kStart).toDouble()) - 1e-9)
            log(reps, push, d, target)
        }
        dao.upsertGoal(dao.goal(push)!!.copy(currentValue = ceil(10.0 * 1.01.pow(24.0))))

        // In the queue, reached and let go.
        goal(Goal(title = t("Learn to draw portraits", "Научиться рисовать портреты"), method = Method.SIMPLE.name, status = Goal.QUEUED, area = Area.FUN.ordinal, startDay = today, seed = 99, createdAt = now, sort = 5))
        val books = goal(
            Goal(
                title = t("Read 12 books this year", "Прочитать 12 книг за год"), method = Method.SIMPLE.name, status = Goal.DONE, area = Area.GROWTH.ordinal,
                startDay = today - 300, startValue = 0.0, targetValue = 12.0, currentValue = 12.0, unit = t("books", "книг"),
                seed = 777, createdAt = at(today - 300), closedAt = at(today - 20), note = t("Audiobooks on the commute did half the job", "Аудиокниги в дороге сделали половину работы"), sort = 6,
            )
        )
        node(Node(goalId = books, kind = NodeKind.MILESTONE.name, title = t("First 6 books", "Первые 6 книг"), done = true, doneAt = at(today - 150)))
        goal(
            Goal(
                title = t("Save a three-month safety cushion", "Накопить подушку на три месяца"), method = Method.FOURDX.name, status = Goal.DONE, area = Area.MONEY.ordinal,
                startDay = today - 200, startValue = 0.0, targetValue = 3.0, currentValue = 3.0, unit = t("months", "мес."), seed = 1234,
                createdAt = at(today - 200), closedAt = at(today - 60), note = t("Automatic transfer on payday", "Автоперевод в день зарплаты"), sort = 7,
            )
        )
        goal(
            Goal(
                title = t("Learn the guitar", "Научиться играть на гитаре"), method = Method.PACT.name, status = Goal.DROPPED, area = Area.FUN.ordinal,
                startDay = today - 120, endDay = today - 91, seed = 4321, createdAt = at(today - 120), closedAt = at(today - 90),
                note = t("I liked the idea more than the practice. That's fine.", "Мне нравилась идея больше, чем занятия. И это нормально."), sort = 8,
            )
        )

        // Choosing tools.
        dao.upsertWheel(WheelCheck.of(today - 62, listOf(6, 5, 4, 7, 6, 3, 4, 6), at(today - 62)))
        dao.upsertWheel(WheelCheck.of(today - 3, listOf(7, 6, 5, 7, 7, 6, 4, 6), at(today - 3)))
        val list25 = listOf(
            t("Run a marathon", "Пробежать марафон") to true, t("English for work", "Английский для работы") to true,
            t("Portfolio website", "Сайт-портфолио") to true, t("Meditate daily", "Медитировать каждый день") to true,
            t("Visit Japan", "Съездить в Японию") to true, t("Learn the guitar", "Научиться играть на гитаре") to false,
            t("Write a novel", "Написать роман") to false, t("Learn to cook Thai food", "Научиться готовить тайскую еду") to false,
            t("Get a dog", "Завести собаку") to false, t("Learn Spanish", "Выучить испанский") to false,
            t("Climb Elbrus", "Подняться на Эльбрус") to false, t("Open a café", "Открыть кафе") to false,
        )
        list25.forEachIndexed { i, (title, chosen) ->
            dao.upsertIdea(Idea(tool = Method.BUFFETT.name, title = title, chosen = chosen, sort = i, at = at(today - 70), goalId = if (i == 0) marathon else if (i == 1) okr else 0))
        }
        val lives = listOf(
            Triple(t("Senior designer in a good studio", "Старший дизайнер в хорошей студии"), t("Grow where I am: lead projects, mentor juniors, run the half marathon every spring.", "Расти там, где я есть: вести проекты, менторить джунов, каждую весну бегать полумарафон."), listOf(80, 60, 75, 70)),
            Triple(t("Freelancer with my own clients", "Фрилансер со своими клиентами"), t("If the studio closed tomorrow: a portfolio site, three steady clients, work from anywhere.", "Если студия завтра закроется: сайт-портфолио, три постоянных клиента, работа откуда угодно."), listOf(55, 80, 50, 75)),
            Triple(t("A small design school in Lisbon", "Маленькая школа дизайна в Лиссабоне"), t("If money didn't matter: teach, run by the ocean, speak English every day.", "Если бы деньги не имели значения: преподавать, бегать у океана, каждый день говорить по-английски."), listOf(30, 95, 35, 60)),
        )
        lives.forEachIndexed { i, (title, text, g) ->
            dao.upsertIdea(Idea(tool = Method.ODYSSEY.name, slot = i, title = title, text = text, a = g[0], b = g[1], c = g[2], d = g[3], at = at(today - 75), goalId = if (i == 1) site else 0))
        }
        listOf(
            Triple(0, t("Road bike", "Шоссейный велосипед") to Pair(90000.0, 0.0), ""),
            Triple(1, t("Fluent in English", "Свободно говорящий по-английски") to Pair(0.0, 6000.0), ""),
            Triple(2, t("Two weeks in Japan", "Две недели в Японии") to Pair(250000.0, 0.0), ""),
            Triple(2, t("Tokyo Marathon", "Токийский марафон") to Pair(40000.0, 0.0), ""),
        ).forEachIndexed { i, (slot, item, _) ->
            dao.upsertIdea(Idea(tool = Method.DREAMLINE.name, slot = slot, title = item.first, amount = item.second.first, monthly = item.second.second, sort = i, at = at(today - 50)))
        }
        dao.upsertField(Field(0, "dream.months", "12"))
        dao.upsertField(Field(0, "dream.currency", if (ru) "₽" else "€"))
        if (!ru) {
            // Euro prices read better a hundred times smaller.
            dao.ideas().filter { it.tool == Method.DREAMLINE.name }.forEach { dao.upsertIdea(it.copy(amount = it.amount / 100, monthly = it.monthly / 100)) }
        }

        // Three weeks of reviews.
        val ws = World.weekStartOf(today)
        listOf(
            Triple(t("Ran 32 km in a week, a record", "Пробежала 32 км за неделю — рекорд"), t("Two late evenings at work", "Два поздних вечера на работе"), t("Run in the morning on busy days", "В загруженные дни бегать утром")),
            Triple(t("First call fully in English", "Первый созвон целиком на английском"), t("Skipped the gym on Thursday", "Пропустила зал в четверг"), t("Gym bag ready the night before", "Собирать сумку в зал с вечера")),
            Triple(t("Domain bought, home page sketched", "Купила домен, набросала главную"), t("Lost an evening choosing fonts", "Потеряла вечер на выбор шрифтов"), t("Time-box design choices to 30 minutes", "Ограничить выбор в дизайне 30 минутами")),
        ).forEachIndexed { i, (went, blocked, change) ->
            val day = ws - 7L * (i + 1)
            dao.upsertReview(Review(day, went, blocked, change, mood = listOf(4, 5, 3)[i], at = at(day + 6, 19)))
        }

        settings.update {
            it.copy(onboarded = true, name = if (ru) "Аня" else "Anna", focusLimit = 5, seen = it.seen)
        }
    }

    private fun planned(days: Int, day: Long): Boolean = days and (1 shl (LocalDate.ofEpochDay(day).dayOfWeek.value - 1)) != 0
}
