package com.example.hcmanager

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.hcmanager.ui.theme.HCManagerTheme
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.SizeTransform
import androidx.compose.ui.draw.drawWithCache

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        setContent {
            HCManagerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp()
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    var selectedTeam by remember { mutableStateOf<Team?>(null) }
    var league by remember { mutableStateOf<League?>(null) }
    var currentTab by remember { mutableStateOf(0) }
    var simVersion by remember { mutableStateOf(0) }
    var loaded by remember { mutableStateOf(false) }
    val matchesListState = rememberLazyListState()
    var viewingRound by remember { mutableStateOf<Int?>(null) }
    var leagueTabMode by remember { mutableStateOf(0) }
    var splashDone by remember { mutableStateOf(false) }

    // ─── Загрузка сохранения при старте ───
    // ─── Загрузка сохранения при старте ───
    // ─── Загрузка сохранения при старте ───
    LaunchedEffect(Unit) {
        val saved = SaveManager.load(context)
        if (saved != null) {
            val team = SampleData.teams.firstOrNull { it.name == saved.selectedTeamName }
            if (team != null) {
                val restored = League(SampleData.teams, saved.seed)
                saved.matches.forEach { sm ->
                    val home = SampleData.teams.firstOrNull { it.name == sm.homeName }
                    val away = SampleData.teams.firstOrNull { it.name == sm.awayName }
                    if (home != null && away != null) {
                        restored.playedMatches.add(
                            Match(home, away, sm.homeGoals, sm.awayGoals, sm.outcome, sm.isPlayoff)
                        )
                    }
                }
                saved.playoff?.let { restored.restorePlayoffState(it) }

                // 👇 Восстанавливаем мораль, импульс и фактор побед (без дублей)
                SampleData.teams.forEach { t ->
                    t.momentum = saved.teamMomentums[t.name] ?: 0.0f
                    t.morale = saved.teamMorales[t.name] ?: 50
                    t.winningFactor = saved.teamWinningFactors[t.name] ?: 0.0f
                }

                selectedTeam = team
                league = restored
                restored.ensureFinalPhase()
            }
        }
        loaded = true
    }

    // Сплэш показываем минимум 1.5 секунды ИЛИ пока грузится сейв — что дольше
    LaunchedEffect(Unit) {
        delay(1500)
        splashDone = true
    }

    if (!loaded || !splashDone) {
        SplashScreen()
        return
    }
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // ─── ВКЛАДКА "КОМАНДА" ───
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        if (selectedTeam != null) {
                            TeamLogoImage(selectedTeam!!.name, size = 32.dp) // 👈 Было 24.dp
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = "Команда")
                        }
                    },
                    label = { Text("Команда") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // ─── ВКЛАДКА "МАТЧИ" ───
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(Icons.Filled.DateRange, contentDescription = "Матчи")
                    },
                    label = { Text("Матчи") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // ─── ВКЛАДКА "ЛИГА" ───
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Лига",
                            modifier = Modifier.size(28.dp), // 👈 Чуть крупнее
                            alpha = if (currentTab == 2) 1f else 0.5f // 👈 Полупрозрачность вместо перекрашивания
                        )
                    },
                    label = { Text("Лига") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { innerPadding ->

        val doSimulateFromTeam: () -> Unit = {
            val l = league
            if (l != null && !l.isSeasonOver) {
                when {
                    !l.isRegularSeasonOver -> {
                        viewingRound = l.currentRound
                        l.simulateNextRound()
                    }
                    l.playoffState.round == 0 -> {
                        viewingRound = null
                        l.startPlayoff()
                    }
                    else -> l.simulateNextRound()
                }
                simVersion++
                selectedTeam?.let { saveGame(context, it, l) }
            }
        }
        val doSimulateFromMatches: () -> Unit = {
            val l = league
            if (l != null && !l.isSeasonOver) {
                when {
                    !l.isRegularSeasonOver -> {
                        viewingRound = null
                        l.simulateNextRound()
                    }
                    l.playoffState.round == 0 -> l.startPlayoff()
                    else -> l.simulateNextRound()
                }
                simVersion++
                selectedTeam?.let { saveGame(context, it, l) }
            }
        }

        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally(animationSpec = tween(250)) { width -> width / 5 * direction } + fadeIn(tween(250))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(250)) { width -> -width / 5 * direction } + fadeOut(tween(250)))
            },
            label = "tab_transition",
            modifier = Modifier.padding(innerPadding)
        ) { tab ->
            when (tab) {
                0 -> key(simVersion) {
                    TeamTab(
                        selectedTeam = selectedTeam,
                        league = league,
                        viewingRound = viewingRound,
                        onTeamSelected = { team ->
                            selectedTeam = team
                            league = League(SampleData.teams)
                            viewingRound = null
                            simVersion = 0
                            saveGame(context, team, league!!)
                        },
                        onSimulate = doSimulateFromTeam,
                        onNextMatch = { viewingRound = null },
                        onReset = {
                            SaveManager.clear(context)
                            selectedTeam = null
                            league = null
                            viewingRound = null
                            simVersion = 0
                            currentTab = 0
                        }
                    )
                }
                1 -> key(simVersion) {
                    MatchesTab(
                        league = league,
                        listState = matchesListState,
                        onSimulate = doSimulateFromMatches
                    )
                }
                2 -> key(simVersion) {
                    LeagueTab(
                        league = league,
                        selectedTeamName = selectedTeam?.name,
                        mode = leagueTabMode,
                        onModeChange = { leagueTabMode = it }
                    )
                }
            }
        }
    }
}

private fun saveGame(context: Context, team: Team, league: League) {
    val momentums = SampleData.teams.associate { it.name to it.momentum }
    val morales = SampleData.teams.associate { it.name to it.morale }
    val winningFactors = SampleData.teams.associate { it.name to it.winningFactor }

    val saved = SavedGame(
        selectedTeamName = team.name,
        seed = league.seed,
        matches = league.playedMatches.map { m ->
            SavedMatch(m.homeTeam.name, m.awayTeam.name, m.homeGoals, m.awayGoals, m.outcome, m.isPlayoff)
        },
        playoff = league.exportPlayoffState(),
        teamMomentums = momentums,
        teamMorales = morales,          // 👈 НОВОЕ
        teamWinningFactors = winningFactors // 👈 НОВОЕ
    )
    SaveManager.save(context, saved)
}

// ─────────────────────────────────────────────────────────
// Экран «Команда»
// ─────────────────────────────────────────────────────────
@Composable
fun TeamTab(
    selectedTeam: Team?,
    league: League?,
    viewingRound: Int?,
    onTeamSelected: (Team) -> Unit,
    onSimulate: () -> Unit,
    onNextMatch: () -> Unit,
    onReset: () -> Unit
) {
    if (selectedTeam == null) {
        TeamSelectionScreen(onTeamSelected)
    } else {
        TeamCardScreen(
            selectedTeam = selectedTeam,
            league = league,
            viewingRound = viewingRound,
            onSimulate = onSimulate,
            onNextMatch = onNextMatch,
            onReset = onReset
        )
    }
}

@Composable
fun TeamSelectionScreen(onTeamSelected: (Team) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Выберите свою команду",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Сезон начнётся после выбора клуба",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(SampleData.teams, key = { it.name }) { team ->
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "card_scale"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .graphicsLayer { // 👈 Анимация масштаба
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(), // 👈 Волна нажатия
                            onClick = { onTeamSelected(team) }
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        TeamLogoImage(team.name, size = 64.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (team.name == "Шанхайские драконы") "Драконы" else team.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 18.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OpponentCard(opponent: Team, league: League) {
    val stats = remember(league.regularSeasonMatches.size) {
        TeamTable.build(SampleData.teams, league.regularSeasonMatches)
    }
    val oppPos = TeamForm.conferencePosition(opponent, stats)
    val confLabel = if (opponent.conference == Conference.WEST) "Запад" else "Восток"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("СОПЕРНИК", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TeamLogoImage(opponent.name, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(opponent.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = oppPos?.let { "$it-е место, $confLabel" } ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) { StatBar("Атака", opponent.baseAttack) } // 👈 используем base
                Box(modifier = Modifier.weight(1f)) { StatBar("Защита", opponent.baseDefense) } // 👈 используем base
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 👇 Ряд с Формой и Индексом силы
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Форма: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FormCircles(opponent, league.playedMatches)
                }
                PowerIndexIndicator(opponent)
            }
        }
    }
}

@Composable
fun TeamCardScreen(
    selectedTeam: Team,
    league: League?,
    viewingRound: Int?,
    onSimulate: () -> Unit,
    onNextMatch: () -> Unit,
    onReset: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Начать сезон заново?") },
            text = { Text("Текущий прогресс будет удалён. Это действие нельзя отменить.") },
            confirmButton = {
                Button(onClick = { showResetDialog = false; onReset() }) { Text("Начать заново") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) { Text("Отмена") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ─── ШАПКА КОМАНДЫ ───
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamLogoImage(teamName = selectedTeam.name, size = 64.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(selectedTeam.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (selectedTeam.conference == Conference.WEST) "Западная конференция" else "Восточная конференция",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // Характеристики
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        StatBar("Атака", selectedTeam.baseAttack) // 👈
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        StatBar("Защита", selectedTeam.baseDefense) // 👈
                    }
                }

                if (league != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    // 👇 Ряд с Формой и Индексом силы
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Форма: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FormCircles(selectedTeam, league.playedMatches)
                        }
                        PowerIndexIndicator(selectedTeam)
                    }
                }
            }
        }

        // ─── МАТЧИ / ПЛЕЙ-ОФФ ───
        if (league != null) {
            val showRegularSeason = !league.isRegularSeasonOver || viewingRound != null
            if (showRegularSeason) {
                // РЕГУЛЯРКА
                val isViewingPlayed = viewingRound != null && viewingRound < league.currentRound
                val actualRound = viewingRound ?: league.currentRound
                val pair = league.schedule.getOrNull(actualRound)?.firstOrNull {
                    it.first.name == selectedTeam.name || it.second.name == selectedTeam.name
                }

                if (actualRound < league.totalRounds) {
                    val homeAwaySuffix = when {
                        pair == null -> ""
                        pair.first.name == selectedTeam.name -> " (д)"
                        else -> " (в)"
                    }
                    Text(
                        text = (if (isViewingPlayed) "Тур ${actualRound + 1} сыгран" else "Тур ${actualRound + 1}") + homeAwaySuffix,
                        style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary
                    )

                    if (pair != null) {
                        val (home, away) = pair
                        val match = if (isViewingPlayed) league.matchOfRound(actualRound, home, away) else null
                        val myTeam = selectedTeam
                        val opponent = if (home.name == myTeam.name) away else home
                        val isMyHome = home.name == myTeam.name

                        val displayMatch = if (match != null) {
                            val hg = if (isMyHome) match.homeGoals else match.awayGoals
                            val ag = if (isMyHome) match.awayGoals else match.homeGoals
                            Match(myTeam, opponent, hg, ag, match.outcome, match.isPlayoff)
                        } else null

                        // Красивая карточка матча
                        MatchCard(myTeam, opponent, displayMatch, null, null)
                    } else {
                        Text("Команда отдыхает в этом туре", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Кнопка
                when {
                    isViewingPlayed -> Button(onClick = onNextMatch, modifier = Modifier.fillMaxWidth()) { Text("К следующему матчу") }
                    else -> Button(onClick = onSimulate, modifier = Modifier.fillMaxWidth()) { Text(if (pair != null) "Симулировать матч" else "Симулировать тур") }
                }

                // Карточка соперника
                if (pair != null && !isViewingPlayed) {
                    val opponent = if (pair.first.name == selectedTeam.name) pair.second else pair.first
                    OpponentCard(opponent, league)
                }

            } else {
                // ПЛЕЙ-ОФФ
                val ps = league.playoffState

                // Справка: итоговая позиция в регулярке
                run {
                    val stats = TeamTable.build(SampleData.teams, league.regularSeasonMatches)
                    val confStats = TeamTable.sort(stats).filter { it.team.conference == selectedTeam.conference }
                    val confPos = confStats.indexOfFirst { it.team.name == selectedTeam.name } + 1
                    val myStats = confStats[confPos - 1]
                    Text(
                        text = "Регулярка: $confPos-е место (${myStats.points} очков)",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (ps.finished) {
                    Text("🏆 Сезон завершён!", style = MaterialTheme.typography.headlineSmall)
                    ps.champion?.let { Text("Обладатель Кубка Гагарина: ${it.name}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 👇 Кнопка перехода в новый сезон
                    Button(
                        onClick = {
                            // 1. Эволюционируем команды
                            val newTeams = SeasonProgression.evolveTeams(
                                oldTeams = SampleData.teams,
                                league = league,
                                championName = ps.champion?.name
                            )
                            // 2. Обновляем глобальный список команд
                            SampleData.updateTeams(newTeams)

                            // 3. Сбрасываем лигу
                            onReset() // или создаем новую лигу
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Начать новый сезон")
                    }
                }
            }
        }

        OutlinedButton(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Начать сезон заново")
        }
    }
}

// Новая красивая карточка для отображения матча (в регулярке и плей-офф)
@Composable
fun MatchCard(home: Team, away: Team, match: Match?, homeSeriesWins: Int?, awaySeriesWins: Int?) {
    val homeIsWinner = match?.winner?.name == home.name
    val awayIsWinner = match?.winner?.name == away.name

    val grayColor = Color(0xFF6B7280) // Серый для проигравших
    val leftColor = if (homeIsWinner) TeamColors.getColor(home.name) else grayColor
    val rightColor = if (awayIsWinner) TeamColors.getColor(away.name) else grayColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache { // 👈 ИСПОЛЬЗУЕМ КЕШИРОВАНИЕ
                    val w = size.width
                    val h = size.height
                    val strokeWidth = 2.dp.toPx()
                    val radius = 12.dp.toPx()
                    val gap = 6.dp.toPx()

                    val leftPath = Path().apply {
                        moveTo(w / 2 - gap, 0f)
                        lineTo(radius, 0f)
                        arcTo(Rect(0f, 0f, radius * 2, radius * 2), 270f, -90f, false)
                        lineTo(0f, h - radius)
                        arcTo(Rect(0f, h - radius * 2, radius * 2, h), 180f, -90f, false)
                        lineTo(w / 2 - gap, h)
                    }
                    val rightPath = Path().apply {
                        moveTo(w / 2 + gap, 0f)
                        lineTo(w - radius, 0f)
                        arcTo(Rect(w - radius * 2, 0f, w, radius * 2), 270f, 90f, false)
                        lineTo(w, h - radius)
                        arcTo(Rect(w - radius * 2, h - radius * 2, w, h), 0f, 90f, false)
                        lineTo(w / 2 + gap, h)
                    }

                    onDrawBehind {
                        drawPath(leftPath, color = leftColor, style = Stroke(width = strokeWidth))
                        drawPath(rightPath, color = rightColor, style = Stroke(width = strokeWidth))
                    }
                }
                .padding(vertical = 16.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ─── ДОМАШНЯЯ КОМАНДА ───
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeamLogoImage(home.name, 48.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = TeamShortName.get(home.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (homeIsWinner) FontWeight.Bold else FontWeight.Normal,
                    color = if (match != null && !homeIsWinner) grayColor else MaterialTheme.colorScheme.onSurface
                )
            }

            // ─── ЦЕНТР (СЧЁТ И СТАТУС) ───
            Column(
                modifier = Modifier.width(110.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val suffix = when (match?.outcome) {
                    MatchOutcome.OVERTIME -> "ОТ"
                    MatchOutcome.SHOOTOUT -> "Буллиты"
                    else -> null
                }

                if (suffix != null) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(suffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                var scoreVisible by remember { mutableStateOf(match == null) }
                LaunchedEffect(match) {
                    if (match != null) {
                        scoreVisible = false
                        delay(50)
                        scoreVisible = true
                    }
                }
                val scoreScale by animateFloatAsState(if (scoreVisible) 1f else 0.5f, tween(300))
                val scoreAlpha by animateFloatAsState(if (scoreVisible) 1f else 0f, tween(300))

                Text(
                    text = if (match != null) "${match.homeGoals} - ${match.awayGoals}" else "— : —",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scoreScale
                        scaleY = scoreScale
                        alpha = scoreAlpha
                    }
                )

                if (homeSeriesWins != null && awaySeriesWins != null) {
                    Text(
                        text = "($homeSeriesWins - $awaySeriesWins)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ─── ГОСТЕВАЯ КОМАНДА ───
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TeamLogoImage(away.name, 48.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = TeamShortName.get(away.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (awayIsWinner) FontWeight.Bold else FontWeight.Normal,
                    color = if (match != null && !awayIsWinner) grayColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Экран «Матчи»
// ─────────────────────────────────────────────────────────
@Composable
fun MatchesTab(
    league: League?,
    listState: LazyListState,
    onSimulate: () -> Unit
) {
    if (league == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Сначала выберите команду на вкладке «Команда»")
        }
        return
    }
    if (league.isRegularSeasonOver) {
        PlayoffMatchesTab(league, listState, onSimulate)
        return
    }

    val offsets = remember(league.schedule.size) {
        IntArray(league.schedule.size + 1).also { arr ->
            var s = 0
            for (i in league.schedule.indices) {
                arr[i] = s
                s += league.schedule[i].size
            }
            arr[league.schedule.size] = s
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Регулярный чемпионат", style = MaterialTheme.typography.headlineSmall) }

        league.schedule.forEachIndexed { roundIndex, round ->
            item(key = roundIndex) {
                // 👇 Вычисляем матчи только для видимых туров
                val start = offsets[roundIndex]
                val end = offsets[roundIndex + 1]
                val roundMatches: List<Match> = if (end <= league.playedMatches.size) {
                    league.playedMatches.subList(start, end).toList()
                } else emptyList()

                RoundBlock(
                    roundNumber = roundIndex + 1,
                    round = round,
                    roundMatches = roundMatches,
                    isPlayed = roundIndex < league.currentRound,
                    isCurrent = roundIndex == league.currentRound,
                    onSimulate = if (roundIndex == league.currentRound) onSimulate else null
                )
            }
        }
    }
}

@Composable
fun PlayoffMatchesTab(
    league: League,
    listState: LazyListState,
    onSimulate: () -> Unit
) {
    val ps = league.playoffState
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val header = when {
                ps.finished -> "Плей-офф завершён"
                ps.round == 0 -> "Регулярный чемпионат завершён"
                else -> "${PlayoffState.roundName(ps.round)}  •  матч ${ps.currentGame + 1}"
            }
            Text(header, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))

            if (ps.finished) {
                ps.champion?.let {
                    Text("🏆 Обладатель Кубка Гагарина: ${it.name}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            } else if (ps.round == 0) {
                Button(onClick = onSimulate, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Начать плей-офф")
                }
            }
        }

        if (ps.round > 0) {
            for (round in 1..4) {
                val seriesInRound = ps.series.filter { it.round == round }
                val isCurrentRound = (round == ps.round && !ps.finished)

                if (seriesInRound.isEmpty()) {
                    if (isCurrentRound) {
                        item(key = "r${round}_g0_empty") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${PlayoffState.roundName(round)}  •  Матч 1  •  идёт",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = onSimulate,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Симулировать", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Text("Матчи ещё не сыграны", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    continue
                }

                val maxGames = seriesInRound.maxOf { it.matches.size }
                val totalGamesToShow = if (isCurrentRound) maxOf(maxGames, ps.currentGame + 1) else maxGames

                for (gameIdx in 0 until totalGamesToShow) {
                    val games = seriesInRound.mapNotNull { it.matches.getOrNull(gameIdx) }
                    val isCurrent = isCurrentRound && gameIdx == ps.currentGame

                    item(key = "r${round}_g$gameIdx") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${PlayoffState.roundName(round)}  •  Матч ${gameIdx + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCurrent) {
                                    Button(
                                        onClick = onSimulate,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Симулировать", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                            if (games.isEmpty()) {
                                Text("Матчи ещё не сыграны", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                games.forEach { m ->
                                    // В плей-офф мы не знаем счёт серии из самого матча, поэтому здесь передаем null
                                    MatchRowCard(m.homeTeam, m.awayTeam, m)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoundBlock(
    roundNumber: Int,
    round: List<Pair<Team, Team>>,
    roundMatches: List<Match>,
    isPlayed: Boolean,
    isCurrent: Boolean,
    onSimulate: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Шапка тура
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Тур $roundNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (onSimulate != null) {
                Button(
                    onClick = onSimulate,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Симулировать", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        // Матчи тура
        round.forEach { (home, away) ->
            val match = roundMatches.firstOrNull {
                it.homeTeam.name == home.name && it.awayTeam.name == away.name
            }
            MatchRowCard(home, away, match)
        }
    }
}

@Composable
fun MatchRow(
    home: Team,
    away: Team,
    match: Match?,
    homeSeriesWins: Int? = null,
    awaySeriesWins: Int? = null
) {
    val style = MaterialTheme.typography.bodyMedium
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            home.name,
            modifier = Modifier.weight(1f),
            style = style,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (homeSeriesWins != null) {
            Text(" ($homeSeriesWins)", style = style)
        }

        val scoreDisplay = if (match == null) {
            " — : — "
        } else {
            val suffix = when (match.outcome) {
                MatchOutcome.REGULATION -> ""
                MatchOutcome.OVERTIME -> " ОТ"
                MatchOutcome.SHOOTOUT -> " Б"
            }
            " ${match.homeGoals} : ${match.awayGoals}$suffix "
        }
        Text(
            scoreDisplay,
            modifier = Modifier.width(90.dp),
            style = style,
            textAlign = TextAlign.Center
        )

        if (awaySeriesWins != null) {
            Text("($awaySeriesWins) ", style = style)
        }
        Text(
            away.name,
            modifier = Modifier.weight(1f),
            style = style,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MatchRowCard(
    home: Team,
    away: Team,
    match: Match?,
    homeSeriesWins: Int? = null,
    awaySeriesWins: Int? = null
) {
    val homeIsWinner = match?.winner?.name == home.name
    val awayIsWinner = match?.winner?.name == away.name

    val grayColor = Color(0xFF6B7280)
    val leftColor = if (homeIsWinner) TeamColors.getColor(home.name) else grayColor
    val rightColor = if (awayIsWinner) TeamColors.getColor(away.name) else grayColor

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache { // 👈 КЕШИРУЕМ
                    val w = size.width
                    val h = size.height
                    val strokeWidth = 2.dp.toPx()
                    val radius = 12.dp.toPx()
                    val gap = 6.dp.toPx()

                    val leftPath = Path().apply {
                        moveTo(w / 2 - gap, 0f)
                        lineTo(radius, 0f)
                        arcTo(Rect(0f, 0f, radius * 2, radius * 2), 270f, -90f, false)
                        lineTo(0f, h - radius)
                        arcTo(Rect(0f, h - radius * 2, radius * 2, h), 180f, -90f, false)
                        lineTo(w / 2 - gap, h)
                    }
                    val rightPath = Path().apply {
                        moveTo(w / 2 + gap, 0f)
                        lineTo(w - radius, 0f)
                        arcTo(Rect(w - radius * 2, 0f, w, radius * 2), 270f, 90f, false)
                        lineTo(w, h - radius)
                        arcTo(Rect(w - radius * 2, h - radius * 2, w, h), 0f, 90f, false)
                        lineTo(w / 2 + gap, h)
                    }

                    onDrawBehind {
                        drawPath(leftPath, color = leftColor, style = Stroke(width = strokeWidth))
                        drawPath(rightPath, color = rightColor, style = Stroke(width = strokeWidth))
                    }
                }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ─── ДОМАШНЯЯ КОМАНДА ───
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = TeamShortName.get(home.name),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (homeIsWinner) FontWeight.Bold else FontWeight.Normal,
                    color = if (match != null && !homeIsWinner) grayColor else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                TeamLogoImage(home.name, size = 28.dp)
            }

            // ─── ЦЕНТР (СЧЁТ) ───
            Column(
                modifier = Modifier.width(110.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val suffix = when (match?.outcome) {
                    MatchOutcome.OVERTIME -> "ОТ"
                    MatchOutcome.SHOOTOUT -> "Буллиты"
                    else -> null
                }

                if (suffix != null) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(suffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                var scoreVisible by remember { mutableStateOf(match == null) }
                LaunchedEffect(match) {
                    if (match != null) {
                        scoreVisible = false
                        delay(50)
                        scoreVisible = true
                    }
                }
                val scoreScale by animateFloatAsState(if (scoreVisible) 1f else 0.5f, tween(300))
                val scoreAlpha by animateFloatAsState(if (scoreVisible) 1f else 0f, tween(300))

                Text(
                    text = if (match != null) "${match.homeGoals} - ${match.awayGoals}" else "— : —",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        scaleX = scoreScale
                        scaleY = scoreScale
                        alpha = scoreAlpha
                    }
                )

                if (homeSeriesWins != null && awaySeriesWins != null) {
                    Text(
                        text = "($homeSeriesWins - $awaySeriesWins)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ─── ГОСТЕВАЯ КОМАНДА ───
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogoImage(away.name, size = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = TeamShortName.get(away.name),
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (awayIsWinner) FontWeight.Bold else FontWeight.Normal,
                    color = if (match != null && !awayIsWinner) grayColor else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Экран «Лига»
// ─────────────────────────────────────────────────────────
@Composable
fun LeagueTab(
    league: League?,
    selectedTeamName: String?, // 👈 Добавили параметр
    mode: Int,
    onModeChange: (Int) -> Unit
) {
    if (league == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Сначала выберите команду на вкладке «Команда»")
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mode == 0) Button(onClick = { onModeChange(0) }) { Text("Регулярный чемпионат") }
            else OutlinedButton(onClick = { onModeChange(0) }) { Text("Регулярный чемпионат") }

            if (mode == 1) Button(onClick = { onModeChange(1) }, enabled = league.isRegularSeasonOver) { Text("Плей-офф") }
            else OutlinedButton(onClick = { onModeChange(1) }, enabled = league.isRegularSeasonOver) { Text("Плей-офф") }
        }

        if (mode == 0) {
            Text(
                text = when {
                    league.currentRound == 0 && !league.isRegularSeasonOver -> "Сезон не начат"
                    league.isRegularSeasonOver -> "Регулярный чемпионат завершён"
                    else -> "Сыграно туров: ${league.currentRound} из ${league.totalRounds}"
                },
                style = MaterialTheme.typography.titleMedium
            )

            // 👇 Оборачиваем в remember, чтобы не пересчитывать при каждом клике на вкладку
            val stats = remember(league.regularSeasonMatches.size) {
                TeamTable.build(SampleData.teams, league.regularSeasonMatches)
            }
            val byConf = remember(stats) {
                TeamTable.byConference(stats)
            }

            // 👇 Передаем selectedTeamName
            ConferenceTable("Запад", byConf[Conference.WEST].orEmpty(), selectedTeamName)
            ConferenceTable("Восток", byConf[Conference.EAST].orEmpty(), selectedTeamName)
        } else {
            PlayoffBracket(league.playoffState)
        }
    }
}

@Composable
fun ConferenceTable(title: String, stats: List<TeamStats>, selectedTeamName: String?) { // 👈 Добавили параметр
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Шапка таблицы
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Text("Команда", modifier = Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.labelMedium)
                Text("И", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Text("В", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Text("П", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Text("ОТ", modifier = Modifier.width(24.dp), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                Text("О", modifier = Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Строки таблицы
            stats.forEachIndexed { index, s ->
                TableRow(
                    position = (index + 1),
                    team = s.team,
                    games = s.gamesPlayed.toString(),
                    wins = s.totalWins.toString(),
                    losses = s.lossesRegulation.toString(),
                    lossesOt = s.lossesOvertime.toString(),
                    points = s.points.toString(),
                    isPlayoffZone = index < 8,
                    isSelected = s.team.name == selectedTeamName // 👈 Передаем флаг выделения
                )

                // Разделители
                if (index < stats.size - 1) {
                    if (index == 7) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .height(1.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Transparent,
                                            0.1f to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            0.9f to MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            1.0f to Color.Transparent
                                        )
                                    )
                                )
                        )
                    } else {
                        // Обычный разделитель
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun TableRow(
    position: Int,
    team: Team,
    games: String,
    wins: String,
    losses: String,
    lossesOt: String,
    points: String,
    isPlayoffZone: Boolean,
    isSelected: Boolean = false // 👈 Новый параметр
) {
    val style = MaterialTheme.typography.bodyMedium

    val positionColor = if (isPlayoffZone) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color(0xFFD23B3B)
    }

    // 👇 Выбираем цвет фона
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) // Полупрозрачный голубой (акцентный цвет темы)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor) // 👈 Применяем фон
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            position.toString(),
            modifier = Modifier.width(24.dp),
            style = style,
            fontWeight = if (isPlayoffZone) FontWeight.Bold else FontWeight.Normal,
            color = positionColor,
            textAlign = TextAlign.Center
        )

        // Команда: логотип + сокращение
        Row(
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamLogoImage(team.name, size = 20.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                TeamShortName.get(team.name),
                style = style,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, // 👈 Сделаем текст жирнее для выбранной
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface // 👈 И покрасим текст в акцентный цвет
            )
        }

        Text(games, modifier = Modifier.width(24.dp), style = style, textAlign = TextAlign.Center)
        Text(wins, modifier = Modifier.width(24.dp), style = style, textAlign = TextAlign.Center)
        Text(losses, modifier = Modifier.width(24.dp), style = style, textAlign = TextAlign.Center)
        Text(lossesOt, modifier = Modifier.width(24.dp), style = style, textAlign = TextAlign.Center)
        Text(points, modifier = Modifier.width(32.dp), style = style, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "КХЛ",
                modifier = Modifier.size(180.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Хоккейный Менеджер",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "КХЛ • Сезон 2025/2026",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun BracketCard(series: Series?, isFinal: Boolean = false) {
    Card(
        modifier = Modifier.width(200.dp).height(64.dp).padding(vertical = 4.dp), // Чуть шире и выше для логотипов
        colors = CardDefaults.cardColors(
            containerColor = if (isFinal) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (series == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("—", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            val higherName = series.higherSeed.team.name
            val lowerName = series.lowerSeed.team.name
            val higherIsWinner = series.isFinished && series.higherSeedWins >= 4
            val lowerIsWinner = series.isFinished && series.lowerSeedWins >= 4

            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                TeamScoreLine(
                    teamName = higherName,
                    games = series.matches.map { m ->
                        TeamGameStat(
                            goals = if (m.homeTeam.name == higherName) m.homeGoals else m.awayGoals,
                            won = m.winner.name == higherName
                        )
                    },
                    seriesWins = series.higherSeedWins,
                    isSeriesWinner = higherIsWinner
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.surfaceVariant)
                TeamScoreLine(
                    teamName = lowerName,
                    games = series.matches.map { m ->
                        TeamGameStat(
                            goals = if (m.homeTeam.name == lowerName) m.homeGoals else m.awayGoals,
                            won = m.winner.name == lowerName
                        )
                    },
                    seriesWins = series.lowerSeedWins,
                    isSeriesWinner = lowerIsWinner
                )
            }
        }
    }
}

data class TeamGameStat(val goals: Int, val won: Boolean)

@Composable
private fun TeamScoreLine(
    teamName: String,
    games: List<TeamGameStat>,
    seriesWins: Int,
    isSeriesWinner: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Логотип и сокращение
        TeamLogoImage(teamName, size = 16.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = TeamShortName.get(teamName),
            modifier = Modifier.width(40.dp), // Фиксированная ширина для выравнивания цифр
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSeriesWinner) FontWeight.Bold else FontWeight.Normal
        )

        // Цифры матчей
        for (i in 0 until 7) {
            val g = games.getOrNull(i)
            Text(
                text = g?.goals?.toString() ?: "·",
                modifier = Modifier.width(12.dp),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                fontWeight = if (g?.won == true) FontWeight.Bold else FontWeight.Normal
            )
        }

        // Итоговый счет серии
        Text(
            text = seriesWins.toString(),
            modifier = Modifier.width(16.dp),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            fontWeight = if (isSeriesWinner) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun PlayoffBracket(ps: PlayoffState) {
    if (ps.round == 0) {
        Text("Плей-офф ещё не начат", style = MaterialTheme.typography.bodyMedium)
        return
    }

    fun getSeries(r: Int, g: String) = ps.series.filter { it.round == r && it.group == g }

    val r1W = getSeries(1, "W").sortedBy { it.higherSeed.seed }
    val r1E = getSeries(1, "E").sortedBy { it.higherSeed.seed }
    val r2A = getSeries(2, "A")
    val r2B = getSeries(2, "B")
    val r3A = getSeries(3, "A").firstOrNull()
    val r3B = getSeries(3, "B").firstOrNull()
    val r4F = getSeries(4, "F").firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Сетка плей-офф", style = MaterialTheme.typography.headlineSmall)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .height(560.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                Text("1/8 Запад", style = MaterialTheme.typography.labelMedium)
                (0 until 4).forEach { idx -> BracketCard(r1W.getOrNull(idx)) }
            }
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                Text("1/4 A", style = MaterialTheme.typography.labelMedium)
                (0 until 2).forEach { idx -> BracketCard(r2A.getOrNull(idx)) }
            }
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text("1/2 A", style = MaterialTheme.typography.labelMedium)
                BracketCard(r3A)
            }

            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text("🏆 Финал", style = MaterialTheme.typography.titleMedium)
                BracketCard(r4F, isFinal = true)
            }

            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                Text("1/2 B", style = MaterialTheme.typography.labelMedium)
                BracketCard(r3B)
            }
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                Text("1/4 B", style = MaterialTheme.typography.labelMedium)
                (0 until 2).forEach { idx -> BracketCard(r2B.getOrNull(idx)) }
            }
            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                Text("1/8 Восток", style = MaterialTheme.typography.labelMedium)
                (0 until 4).forEach { idx -> BracketCard(r1E.getOrNull(idx)) }
            }
        }
    }
}

@Composable
fun TeamLogoImage(teamName: String, size: Dp = 48.dp) {
    val resId = TeamLogo.getLogoRes(teamName)
    if (resId != null) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = teamName,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
    } else {
        // Заглушка, если логотипа нет
        Box(
            modifier = Modifier.size(size).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(teamName.take(1), color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun StatBar(label: String, value: Int, maxValue: Int = 150) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.toString(), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Сама полоска
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value.toFloat() / maxValue).coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
fun FormCircles(team: Team, played: List<Match>) {
    val lastFive = TeamForm.lastFive(team, played)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (lastFive.isEmpty()) {
            Text("—", style = MaterialTheme.typography.bodyMedium)
        } else {
            lastFive.forEach { result ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (result == 'В') Color(0xFF22C55E) else Color(0xFFEF4444), // Зеленый или красный
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = result.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PowerIndexIndicator(team: Team) {
    // Считаем общий буст в процентах (например, 0.035 -> 3.5%)
    // Округляем до одного знака
    val percent = (team.dynamicModifier * 100).toFloat()

    if (kotlin.math.abs(percent) < 0.1f) return // Если изменений почти нет, не рисуем

    val isPositive = percent > 0
    val color = if (isPositive) Color(0xFF22C55E) else Color(0xFFEF4444)
    val arrow = if (isPositive) "▲" else "▼"

    // Форматируем текст: +2.5% или -1.2%
    val text = String.format(java.util.Locale.US, "%s %.1f%%", arrow, kotlin.math.abs(percent))

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}