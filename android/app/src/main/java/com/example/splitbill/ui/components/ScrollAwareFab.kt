package com.example.splitbill.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.splitbill.theme.Dimens
import com.example.splitbill.theme.SplitBillShapes

@Composable
fun ScrollAwareFab(
  listState: LazyListState,
  onClick: () -> Unit,
  icon: ImageVector,
  text: String,
  modifier: Modifier = Modifier,
) {
  // Determine if we should expand or collapse the FAB
  // Expand when at the top, or when scrolling up
  val isExpanded by remember {
    derivedStateOf {
      listState.firstVisibleItemIndex == 0 || !listState.canScrollForward || !listState.isScrollInProgress
    }
  }

  val fabCornerRadius by animateDpAsState(
    targetValue = if (isExpanded) 16.dp else 28.dp,
    label = "fab_corner_radius"
  )

  FloatingActionButton(
    onClick = onClick,
    modifier = modifier,
    shape = androidx.compose.foundation.shape.RoundedCornerShape(fabCornerRadius),
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    elevation = FloatingActionButtonDefaults.elevation(
      defaultElevation = Dimens.ElevationLevel3,
      pressedElevation = Dimens.ElevationLevel1
    )
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = if (isExpanded) Dimens.SpacingM else Dimens.SpacingM)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = text
      )
      
      AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally()
      ) {
        Row {
          Spacer(modifier = Modifier.width(Dimens.SpacingS))
          Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
          )
        }
      }
    }
  }
}
