package com.lifeos.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifeos.launcher.R
import com.lifeos.launcher.data.ProjectEntity

@Composable
fun ProjectsScreen(vm: MainViewModel) {
    val projects by vm.projects.collectAsStateWithLifecycle()

    PageScroll(
        decor = {
            Deco(R.drawable.deco_tr, Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-22).dp).size(210.dp, 140.dp))
            Deco(R.drawable.deco_bl, Modifier.align(Alignment.BottomStart).offset(x = (-36).dp, y = 28.dp).size(190.dp, 170.dp))
        },
        bottom = {
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                Deco(R.drawable.ill_cottage, Modifier.align(Alignment.BottomEnd).offset(x = (-8).dp).size(190.dp, 142.dp))
            }
        },
    ) {
        PageHeader("Projects", "Bigger goals.")

        projects.forEach { p ->
            ProjectCard(p) { vm.editProject = p }
        }

        GlassCard(radius = 20.dp, padding = 14.dp, onClick = { vm.editProject = ProjectEntity(title = "") }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Icon(Icons.Rounded.Add, null, tint = C.Ink, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Project", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = C.Ink)
            }
        }
    }
}

@Composable
private fun ProjectCard(p: ProjectEntity, onClick: () -> Unit) {
    val color = C.ProjectColors[p.colorIdx.coerceIn(0, C.ProjectColors.lastIndex)]
    GlassCard(padding = 16.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(p.category, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
            Icon(Icons.Rounded.ChevronRight, null, tint = C.Brown, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(p.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = C.Ink)
        if (p.subtitle.isNotBlank()) Body(p.subtitle, Modifier.padding(top = 2.dp), size = 14.sp, color = C.Ink)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LoProgress(p.progress / 100f, Modifier.weight(1f), color = color, height = 6.dp)
            Spacer(Modifier.width(12.dp))
            Text("${p.progress}%", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = C.Ink)
        }
    }
}
