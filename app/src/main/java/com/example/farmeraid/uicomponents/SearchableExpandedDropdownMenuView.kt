// A customized version of https://github.com/Breens-Mbaka/Searchable-Dropdown-Menu-Jetpack-Compose

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.farmeraid.ui.theme.PrimaryColour


/**
 * 🚀 A Jetpack Compose Android Library to create a dropdown menu that is searchable.
 * @param modifier a modifier for this SearchableExpandedDropDownMenu and its children
 * @param listOfItems A list of objects that you want to display as a dropdown
 * @param enable controls the enabled state of the OutlinedTextField. When false, the text field will be neither editable nor focusable, the input of the text field will not be selectable, visually text field will appear in the disabled UI state
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the input text is empty. The default text style for internal [Text] is [Typography.subtitle1]
 * @param openedIcon the Icon displayed when the dropdown is opened. Default Icon is [Icons.Outlined.KeyboardArrowUp]
 * @param closedIcon Icon displayed when the dropdown is closed. Default Icon is [Icons.Outlined.KeyboardArrowDown]
 * @param parentTextFieldCornerRadius : Defines the radius of the enclosing OutlinedTextField. Default Radius is 12.dp
 * @param colors [TextFieldColors] that will be used to resolve color of the text and content
 * (including label, placeholder, leading and trailing icons, border) for this text field in
 * different states. See [TextFieldDefaults.outlinedTextFieldColors]
 * @param onDropDownItemSelected Returns the item that was selected from the dropdown
 * @param dropdownItem Provide a composable that will be used to populate the dropdown and that takes a type i.e String,Int or even a custom type
 * @param showDefaultSelectedItem If set to true it will show the default selected item with the position of your preference, it's value is set to false by default
 * @param defaultItemIndex Pass the index of the item to be selected by default from the dropdown list. If you don't provide any the first item in the dropdown will be selected
 * @param defaultItem Returns the item selected by default from the dropdown list
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchableExpandedDropDownMenuView(
    modifier: Modifier = Modifier,
    listOfItems: List<T>,
    enable: Boolean = true,
    enableSearch: Boolean = true,
    readOnly: Boolean = true,
    placeholder: @Composable (() -> Unit) = { Text(text = "Select Option") },
    openedIcon: ImageVector = Icons.Outlined.KeyboardArrowUp,
    closedIcon: ImageVector = Icons.Outlined.KeyboardArrowDown,
    parentTextFieldCornerRadius: Dp = 12.dp,
    color: Color = PrimaryColour,
    onDropDownItemSelected: (T) -> Unit = {},
    dropdownItem: @Composable (T) -> Unit,
    selectedOption: T? = null,
    isError: Boolean = false,
    fontSize: TextUnit = 16.sp,
) {
    var searchedOption by rememberSaveable { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var filteredItems = mutableListOf<T>()

    val itemHeights = remember { mutableStateMapOf<Int, Int>() }
    val baseHeight = 530.dp
    val density = LocalDensity.current

    val maxHeight = remember(itemHeights.toMap()) {
        if (itemHeights.keys.toSet() != listOfItems.indices.toSet()) {
            // if we don't have all heights calculated yet, return default value
            return@remember baseHeight
        }
        val baseHeightInt = with(density) { baseHeight.toPx().toInt() }

        // top+bottom system padding
        var sum = with(density) { DropdownMenuVerticalPadding.toPx().toInt() } * 2
        for ((_, itemSize) in itemHeights.toSortedMap()) {
            sum += itemSize
            if (sum >= baseHeightInt) {
                return@remember with(density) { (sum - itemSize / 2).toDp() }
            }
        }
        // all items fit into base height
        baseHeight
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                cursorColor = color,
                focusedBorderColor = color,
                focusedLabelColor = color,
                focusedSupportingTextColor = color,
                focusedLeadingIconColor = color,
            ),
            textStyle = TextStyle(
                fontSize = fontSize,
            ),
            value = selectedOption?.toString() ?: "",
            readOnly = readOnly,
            enabled = enable,
            onValueChange = {},
            placeholder = placeholder,
            trailingIcon = {
                IconToggleButton(
                    checked = expanded,
                    onCheckedChange = {
                        expanded = it
                    }
                ) {
                    if (expanded) Icon(
                        imageVector = openedIcon,
                        contentDescription = null,
                        tint = PrimaryColour,
                    ) else Icon(
                        imageVector = closedIcon,
                        contentDescription = null
                    )
                }
            },
            shape = RoundedCornerShape(parentTextFieldCornerRadius),
            isError = isError,
            interactionSource = remember { MutableInteractionSource() }
                .also { interactionSource ->
                    LaunchedEffect(interactionSource) {
                        interactionSource.interactions.collect {
                            if (it is PressInteraction.Release) {
                                expanded = !expanded
                            }
                        }
                    }
                }
        )
        if (expanded) {
            DropdownMenu(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .background(Color.White)
                    .requiredSizeIn(maxHeight = maxHeight),
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Column {
                    if (enableSearch) {
                        OutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textStyle = TextStyle(
                                fontSize = fontSize,
                            ),
                            value = searchedOption,
                            onValueChange = { selectedSport ->
                                searchedOption = selectedSport
                                filteredItems = listOfItems.filter {
                                    it.toString().contains(
                                        searchedOption,
                                        ignoreCase = true
                                    )
                                }.toMutableList()
                            },
                            leadingIcon = {
                                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                            },
                            placeholder = {
                                Text(text = "Search")
                            },
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                cursorColor = color,
                                focusedBorderColor = color,
                                focusedLabelColor = color,
                                focusedSupportingTextColor = color,
                                focusedLeadingIconColor = color,
                            ),
                        )
                    }

                    val items = if (filteredItems.isEmpty()) {
                        listOfItems
                    } else {
                        filteredItems
                    }

                    items.forEach { selectedItem ->
                        DropdownMenuItem(
                            onClick = {
                                onDropDownItemSelected(selectedItem)
                                searchedOption = ""
                                expanded = false
                            },
                            text = {
                                dropdownItem(selectedItem)
                            },
                            colors = MenuDefaults.itemColors()
                        )
                    }
                }
            }
        }
    }
}

private val DropdownMenuVerticalPadding = 8.dp
