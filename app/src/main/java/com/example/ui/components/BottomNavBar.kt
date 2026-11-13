package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class TanScreen(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    TOURNAMENT_DASHBOARD("Arenas", Icons.Default.Dashboard),
    EXPLORE("Explore", Icons.Default.Explore),
    MY_GAMES("My Games", Icons.Default.SportsEsports),
    LEADERBOARD("Ranking", Icons.Default.EmojiEvents),
    PROFILE("Profile", Icons.Default.Person),
    BRACKET("Bracket", Icons.Default.AccountTree),
    CREATE_TOURNAMENT("Create", Icons.Default.AddCircle),
    ADMIN_PANEL("Admin", Icons.Default.AdminPanelSettings)
}

@Composable
fun TanPlayBottomNav(
    currentScreen: TanScreen,
    hasActiveMatch: Boolean = false,
    onNavigate: (TanScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        TanScreen.HOME,
        TanScreen.TOURNAMENT_DASHBOARD,
        TanScreen.MY_GAMES,
        TanScreen.BRACKET,
        TanScreen.PROFILE
    )

    Surface(
        color = ObsidianContainerLow.copy(alpha = 0.98f),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            navItems.forEach { screen ->
                val isSelected = currentScreen == screen
                val tint = if (isSelected) ElectricLime else SlateSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(screen) }
                        .testTag("nav_item_${screen.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = screen.title,
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        if (screen == TanScreen.MY_GAMES && hasActiveMatch) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-2).dp)
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ElectricLime)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = screen.title,
                        color = tint,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.SansSerif
                    )

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .clip(CircleShape)
                                .background(ElectricLime)
                        )
                    }
                }
            }
        }
    }
}
