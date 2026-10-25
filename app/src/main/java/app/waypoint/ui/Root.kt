package app.waypoint.ui

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.waypoint.BuildConfig
import app.waypoint.R
import app.waypoint.container
import app.waypoint.data.Goal
import app.waypoint.data.Settings
import app.waypoint.domain.Method
import app.waypoint.domain.World
import app.waypoint.notify.Alarms
import app.waypoint.ui.compass.CompassTab
import app.waypoint.ui.compass.MethodScreen
import app.waypoint.ui.components.Sky
import app.waypoint.ui.components.rememberNow
import app.waypoint.ui.goals.GoalFormScreen
import app.waypoint.ui.goals.GoalScreen
import app.waypoint.ui.goals.GoalsTab
import app.waypoint.ui.journal.JournalTab
import app.waypoint.ui.journal.ReviewScreen
import app.waypoint.ui.lenses.GrowLens
import app.waypoint.ui.lenses.PremortemLens
import app.waypoint.ui.lenses.SmartLens
import app.waypoint.ui.lenses.WhysLens
import app.waypoint.ui.lenses.WoopLens
import app.waypoint.ui.onboarding.FirstStep
import app.waypoint.ui.onboarding.OnboardingScreen
import app.waypoint.ui.settings.SettingsScreen
import app.waypoint.ui.theme.Manrope
import app.waypoint.ui.theme.U
import app.waypoint.ui.today.TodayTab
import app.waypoint.ui.tools.BuffettScreen
import app.waypoint.ui.tools.DreamlineScreen
import app.waypoint.ui.tools.OdysseyScreen
import app.waypoint.ui.tools.QuizScreen
import app.waypoint.ui.tools.WheelScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

enum class Tab { TODAY, GOALS, COMPASS, JOURNAL }

/** Everything stored plus the settings, rebuilt once a minute or on any change. */
@Immutable
class WData(val world: World, val settings: Settings, val now: Long) {
    val today: LocalDate get() = world.today
    val day: Long get() = world.day
}

/** Everything a screen can open. */
class Go(private val nav: NavHostController) {
    fun goal(id: Long) = nav.navigate("goal/$id")

    /** The new-goal form; a method, a title and an area can be filled in already (from a tool or the quiz). */
    fun newGoal(method: Method? = null, title: String = "", area: Int = -1, idea: Long = 0) =
        nav.navigate("form?id=0&method=${method?.name.orEmpty()}&title=${Uri.encode(title)}&area=$area&idea=$idea")

    fun editGoal(id: Long) = nav.navigate("form?id=$id&method=&title=&area=-1&idea=0")

    /** After saving a new goal: replace the form with the goal itself. */
    fun openCreated(id: Long) = nav.navigate("goal/$id") { popUpTo("main") }

    fun lens(goalId: Long, lens: Method) = nav.navigate("lens/$goalId/${lens.name}")
    fun tool(tool: Method) = nav.navigate("tool/${tool.name}")
    fun method(m: Method) = nav.navigate("method/${m.name}")
    fun quiz() = nav.navigate("quiz")
    fun review() = nav.navigate("review")
    fun settings() = nav.navigate("settings")
    fun back() = nav.popBackStack()
    fun home() = nav.popBackStack("main", inclusive = false)
}

@Composable
fun WaypointRoot(openRoute: MutableStateFlow<String?>, settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    val go = remember(nav) { Go(nav) }
    var tab by rememberSaveable { mutableStateOf(Tab.TODAY) }
    val start = remember { if (settings.onboarded) "main" else "welcome" }

    val dao = container.dao
    val goals by dao.observeGoals().collectAsStateWithLifecycle(initialValue = null)
    val nodes by dao.observeNodes().collectAsStateWithLifecycle(initialValue = emptyList())
    val logs by dao.observeLogs().collectAsStateWithLifecycle(initialValue = emptyList())
    val entries by dao.observeEntries().collectAsStateWithLifecycle(initialValue = emptyList())
    val fields by dao.observeFields().collectAsStateWithLifecycle(initialValue = emptyList())
    val reviews by dao.observeReviews().collectAsStateWithLifecycle(initialValue = emptyList())
    val wheel by dao.observeWheel().collectAsStateWithLifecycle(initialValue = emptyList())
    val ideas by dao.observeIdeas().collectAsStateWithLifecycle(initialValue = emptyList())
    val minute = rememberNow(60_000)
    val day = remember(minute) { LocalDate.now(ZoneId.systemDefault()) }
    val data = remember(goals, nodes, logs, entries, fields, reviews, wheel, ideas, settings, day) {
        WData(World(goals.orEmpty(), nodes, logs, entries, fields, reviews, wheel, ideas, day), settings, System.currentTimeMillis())
    }

    val requested by openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(requested, settings.onboarded, goals != null) {
        val route = requested ?: return@LaunchedEffect
        if (!settings.onboarded || goals == null) return@LaunchedEffect
        nav.popBackStack("main", inclusive = false)
        tab = Tab.TODAY
        when {
            route == "review" -> go.review()
            route.startsWith("goal/") -> route.removePrefix("goal/").toLongOrNull()?.let { go.goal(it) }
        }
        openRoute.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        container.scope.launch { container.changed() }
    }
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Sky(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = start,
            enterTransition = { fadeIn(tween(240)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = { fadeOut(tween(180)) },
        ) {
            composable("welcome") {
                OnboardingScreen(data) { step ->
                    nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
                    when (step) {
                        FirstStep.GOAL -> go.newGoal()
                        FirstStep.QUIZ -> go.quiz()
                        FirstStep.WHEEL -> go.tool(Method.WHEEL)
                        FirstStep.LOOK -> Unit
                    }
                }
            }
            composable("main") { MainTabs(tab, { tab = it }, go, data) }
            composable("goal/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { e ->
                GoalScreen(e.arguments?.getLong("id") ?: 0L, data, go)
            }
            composable(
                "form?id={id}&method={method}&title={title}&area={area}&idea={idea}",
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("method") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("area") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("idea") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { e ->
                val a = e.arguments
                GoalFormScreen(
                    id = a?.getLong("id") ?: 0L,
                    method = a?.getString("method")?.takeIf { it.isNotEmpty() }?.let { Method.of(it) },
                    title = a?.getString("title").orEmpty(),
                    area = a?.getInt("area") ?: -1,
                    idea = a?.getLong("idea") ?: 0L,
                    data = data,
                    go = go,
                )
            }
            composable(
                "lens/{goal}/{lens}",
                arguments = listOf(navArgument("goal") { type = NavType.LongType }, navArgument("lens") { type = NavType.StringType }),
            ) { e ->
                val goal = data.world.goal(e.arguments?.getLong("goal") ?: 0L)
                if (goal != null) LensScreen(Method.of(e.arguments?.getString("lens")), goal, data, go)
            }
            composable("tool/{tool}", arguments = listOf(navArgument("tool") { type = NavType.StringType })) { e ->
                ToolScreen(Method.of(e.arguments?.getString("tool")), data, go)
            }
            composable("method/{m}", arguments = listOf(navArgument("m") { type = NavType.StringType })) { e ->
                MethodScreen(Method.of(e.arguments?.getString("m")), data, go)
            }
            composable("quiz") { QuizScreen(data, go) }
            composable("review") { ReviewScreen(data, go) }
            composable("settings") { SettingsScreen(settings, onBack = { go.back() }) }
        }
    }
}

/** A lens works on one goal: SMART wording, WOOP plans, GROW options, the five whys, a pre-mortem. */
@Composable
private fun LensScreen(lens: Method, goal: Goal, data: WData, go: Go) {
    when (lens) {
        Method.SMART -> SmartLens(goal, data, go)
        Method.WOOP -> WoopLens(goal, data, go)
        Method.GROW -> GrowLens(goal, data, go)
        Method.WHYS -> WhysLens(goal, data, go)
        Method.PREMORTEM -> PremortemLens(goal, data, go)
        else -> Unit
    }
}

/** Tools for choosing a goal stand on their own and can turn their results into goals. */
@Composable
private fun ToolScreen(tool: Method, data: WData, go: Go) {
    when (tool) {
        Method.WHEEL -> WheelScreen(data, go)
        Method.ODYSSEY -> OdysseyScreen(data, go)
        Method.BUFFETT -> BuffettScreen(data, go)
        Method.DREAMLINE -> DreamlineScreen(data, go)
        else -> Unit
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: WData) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.TODAY -> TodayTab(data, go, onTab)
                Tab.GOALS -> GoalsTab(data, go)
                Tab.COMPASS -> CompassTab(data, go)
                Tab.JOURNAL -> JournalTab(data, go)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.bg.copy(alpha = 0.92f)).navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.WbTwilight, stringResource(R.string.tab_today), tab == Tab.TODAY) { onTab(Tab.TODAY) }
            NavItem(Icons.Rounded.AutoAwesome, stringResource(R.string.tab_goals), tab == Tab.GOALS) { onTab(Tab.GOALS) }
            NavItem(Icons.Rounded.Explore, stringResource(R.string.tab_compass), tab == Tab.COMPASS) { onTab(Tab.COMPASS) }
            NavItem(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tab_journal), tab == Tab.JOURNAL) { onTab(Tab.JOURNAL) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
        ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Manrope, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}
