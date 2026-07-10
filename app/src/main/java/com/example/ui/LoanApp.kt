package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldAmber
import kotlin.math.roundToInt

// Utility to format currency values cleanly
fun formatCurrency(value: Double, currency: String = "USD"): String {
    val symbol = when (currency) {
        "INR" -> "₹"
        "EUR" -> "€"
        else -> "$"
    }
    return symbol + String.format("%,.2f", value)
}

// Utility to format monthly durations in human-readable terms
fun formatDuration(months: Int): String {
    val yrs = months / 12
    val mos = months % 12
    return when {
        yrs > 0 && mos > 0 -> "$yrs yrs $mos mos"
        yrs > 0 -> "$yrs yrs"
        else -> "$mos mos"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApp(viewModel: LoanViewModel) {
    var activeTab by remember { mutableStateOf(0) }
    val savedLoans by viewModel.savedLoans.collectAsStateWithLifecycle()
    val summary by viewModel.loanSummary.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    
    val currentProfileId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val profileNameInput by viewModel.profileNameInput.collectAsStateWithLifecycle()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveNameText by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "App Icon Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Loan & Payoff Pro",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.resetToDefaults() },
                        modifier = Modifier.testTag("reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Calculations",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator Tab") },
                    label = { Text("Calculator", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_calculator")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = "Payoff Optimizer Tab") },
                    label = { Text("Optimizer", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_optimizer")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Amortization Schedule Tab") },
                    label = { Text("Schedule", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_schedule")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = "Saved Scenarios Tab") },
                    label = { Text("Saved Profiles", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_saved")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> CalculatorTabScreen(
                    viewModel = viewModel,
                    summary = summary,
                    onSaveClicked = {
                        if (currentProfileId != null) {
                            // If editing, update directly
                            viewModel.saveCurrentLoan(profileNameInput)
                        } else {
                            // Prompt for name to save
                            saveNameText = ""
                            showSaveDialog = true
                        }
                    }
                )
                1 -> PayoffOptimizerTabScreen(
                    viewModel = viewModel,
                    summary = summary
                )
                2 -> AmortizationScheduleTabScreen(
                    viewModel = viewModel,
                    summary = summary
                )
                3 -> SavedProfilesTabScreen(
                    viewModel = viewModel,
                    savedLoans = savedLoans,
                    onLoadProfile = { profile ->
                        viewModel.loadLoanProfile(profile)
                        activeTab = 0 // jump to calculator tab on load
                    }
                )
            }

            // Save Loan Profile Dialog
            if (showSaveDialog) {
                Dialog(onDismissRequest = { showSaveDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Save Loan Profile",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            OutlinedTextField(
                                value = saveNameText,
                                onValueChange = { saveNameText = it },
                                label = { Text("Scenario Name (e.g., House Loan 15Y)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_name_input"),
                                singleLine = true
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { showSaveDialog = false },
                                    modifier = Modifier.testTag("dialog_cancel_button")
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (saveNameText.isNotBlank()) {
                                            viewModel.saveCurrentLoan(saveNameText)
                                            showSaveDialog = false
                                        }
                                    },
                                    enabled = saveNameText.isNotBlank(),
                                    modifier = Modifier.testTag("dialog_confirm_button")
                                ) {
                                    Text("Save")
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
fun CalculatorTabScreen(
    viewModel: LoanViewModel,
    summary: LoanSummary,
    onSaveClicked: () -> Unit
) {
    val principal by viewModel.principalInput.collectAsStateWithLifecycle()
    val rate by viewModel.interestRateInput.collectAsStateWithLifecycle()
    val years by viewModel.tenureYearsInput.collectAsStateWithLifecycle()
    val months by viewModel.tenureMonthsInput.collectAsStateWithLifecycle()
    val isYears by viewModel.isTenureYears.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    
    val currencySymbol = when (selectedCurrency) {
        "INR" -> "₹"
        "EUR" -> "€"
        else -> "$"
    }
    
    val selectedId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val currentProfileName by viewModel.profileNameInput.collectAsStateWithLifecycle()
    
    val focusManager = LocalFocusManager.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (selectedId != null) {
            // Edit banner indicating an active loaded scenario
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Editing Profile: $currentProfileName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Changes will be updated directly upon saving.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.resetToDefaults() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel edit mode",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Section Title
        Text(
            text = "Loan EMI Calculator",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Inputs Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Currency Selector Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Currency Symbol",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        listOf("INR" to "₹ (INR)", "USD" to "$ (USD)", "EUR" to "€ (EUR)").forEach { (code, label) ->
                            val isSelected = selectedCurrency == code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { viewModel.updateCurrency(code) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Principal
                OutlinedTextField(
                    value = principal,
                    onValueChange = { viewModel.updatePrincipal(it) },
                    label = { Text("Loan Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("$currencySymbol ") },
                    leadingIcon = { Icon(Icons.Default.MonetizationOn, contentDescription = "Amount") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("principal_input")
                )

                // Interest Rate
                OutlinedTextField(
                    value = rate,
                    onValueChange = { viewModel.updateInterestRate(it) },
                    label = { Text("Interest Rate (% Per Annum)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text(" %") },
                    leadingIcon = { Icon(Icons.Default.Percent, contentDescription = "Interest Rate") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("interest_input")
                )

                // Tenure Layout (Slider + Toggle + Textbox)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isYears) "Tenure: $years Years" else "Tenure: $months Months",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isYears) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { viewModel.toggleTenureType() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Years",
                                color = if (isYears) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (!isYears) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { viewModel.toggleTenureType() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "Months",
                                color = if (!isYears) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (isYears) {
                    OutlinedTextField(
                        value = years,
                        onValueChange = { viewModel.updateTenureYears(it) },
                        label = { Text("Duration in Years") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Years") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tenure_years_input")
                    )
                    
                    Slider(
                        value = (years.toFloatOrNull() ?: 1f).coerceIn(1f, 40f),
                        onValueChange = { viewModel.updateTenureYears(it.roundToInt().toString()) },
                        valueRange = 1f..40f,
                        steps = 39,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tenure_years_slider")
                    )
                } else {
                    OutlinedTextField(
                        value = months,
                        onValueChange = { viewModel.updateTenureMonths(it) },
                        label = { Text("Duration in Months") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = "Months") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tenure_months_input")
                    )
                    
                    Slider(
                        value = (months.toFloatOrNull() ?: 12f).coerceIn(12f, 480f),
                        onValueChange = { viewModel.updateTenureMonths(it.roundToInt().toString()) },
                        valueRange = 12f..480f,
                        steps = 39,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tenure_months_slider")
                    )
                }
            }
        }

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onSaveClicked,
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_loan_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (selectedId != null) Icons.Default.Save else Icons.Default.BookmarkAdd,
                    contentDescription = "Save Profile Button"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (selectedId != null) "Update Profile" else "Save Scenario")
            }
            
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.resetToDefaults()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("clear_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ClearAll, contentDescription = "Reset Calculations")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear All")
            }
        }

        // Results Card
        Text(
            text = "Calculation Summary",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Big Display: EMI
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MONTHLY PAYMENT (EMI)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = formatCurrency(summary.originalMonthlyEmi, selectedCurrency),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("result_emi_text")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                // Detail Numbers + Custom Donut Chart Side-by-Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Details
                    Column(
                        modifier = Modifier.weight(1.2f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryRowItem(
                            label = "Principal Amount",
                            value = formatCurrency(viewModel.principalInput.value.toDoubleOrNull() ?: 0.0, selectedCurrency),
                            colorMarker = MaterialTheme.colorScheme.primary
                        )
                        SummaryRowItem(
                            label = "Interest Payable",
                            value = formatCurrency(summary.originalTotalInterest, selectedCurrency),
                            colorMarker = GoldAmber
                        )
                        SummaryRowItem(
                            label = "Total Cost",
                            value = formatCurrency(summary.originalTotalPayment, selectedCurrency),
                            colorMarker = Color.Transparent
                        )
                    }

                    // Right Column: Donut Chart
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val principalAmount = viewModel.principalInput.value.toDoubleOrNull() ?: 0.0
                        val interestAmount = summary.originalTotalInterest
                        val totalAmount = principalAmount + interestAmount
                        
                        val principalAngle = if (totalAmount > 0) (principalAmount / totalAmount * 360f).toFloat() else 0f
                        val interestAngle = if (totalAmount > 0) (interestAmount / totalAmount * 360f).toFloat() else 0f
                        
                        val principalColor = MaterialTheme.colorScheme.primary
                        val interestColor = GoldAmber

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val sizeOffset = strokeWidth
                            val innerSize = Size(size.width - sizeOffset, size.height - sizeOffset)
                            
                            // Draw Interest Arc
                            drawArc(
                                color = interestColor,
                                startAngle = -90f,
                                sweepAngle = interestAngle,
                                useCenter = false,
                                topLeft = Offset(sizeOffset / 2, sizeOffset / 2),
                                size = innerSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            
                            // Draw Principal Arc
                            drawArc(
                                color = principalColor,
                                startAngle = -90f + interestAngle,
                                sweepAngle = principalAngle,
                                useCenter = false,
                                topLeft = Offset(sizeOffset / 2, sizeOffset / 2),
                                size = innerSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        // Text inside center of Donut
                        val percentPrincipal = if (totalAmount > 0) (principalAmount / totalAmount * 100).roundToInt() else 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$percentPrincipal%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Principal",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryRowItem(
    label: String,
    value: String,
    colorMarker: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (colorMarker != Color.Transparent) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colorMarker)
            )
        }
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PayoffOptimizerTabScreen(
    viewModel: LoanViewModel,
    summary: LoanSummary
) {
    val monthlyPrep by viewModel.monthlyPrepaymentInput.collectAsStateWithLifecycle()
    val yearlyPrep by viewModel.yearlyPrepaymentInput.collectAsStateWithLifecycle()
    val customPrepayments by viewModel.oneTimePrepayments.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    
    val currencySymbol = when (selectedCurrency) {
        "INR" -> "₹"
        "EUR" -> "€"
        else -> "$"
    }
    
    val focusManager = LocalFocusManager.current
    
    // States for adding custom prepayment
    var tempMonth by remember { mutableStateOf("") }
    var tempAmount by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Payoff & Repayment Optimizer",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Accelerate debt-freedom by simulating advance payments. Add regular monthly/yearly prepayments or insert one-time lump-sums.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        // Benefits Metrics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "REPAYMENT SAVINGS BENEFITS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Interest Saved Gauge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Paid,
                                contentDescription = "Interest Saved Icon",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Interest Saved",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = formatCurrency(summary.interestSaved, selectedCurrency),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = EmeraldGreen,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("interest_saved_text")
                            )
                        }
                    }

                    // Time Saved Gauge
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Time Saved Icon",
                                tint = GoldAmber,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Time Saved",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = if (summary.monthsSaved > 0) formatDuration(summary.monthsSaved) else "0 months",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = GoldAmber,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("time_saved_text")
                            )
                        }
                    }
                }

                // Comparison stats: New payoff length vs original
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Original Repayment",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                        Text(
                            formatDuration(summary.originalTenureMonths),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.TrendingFlat,
                        contentDescription = "to revised",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Revised Repayment",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                        Text(
                            formatDuration(summary.revisedTenureMonths),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (summary.monthsSaved > 0) EmeraldGreen else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Custom Visual Paydown Curve (Canvas Chart)
        Text(
            text = "Loan Paydown Curve",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                val originalSchedule = summary.originalSchedule
                val revisedSchedule = summary.revisedSchedule
                val originalPrincipal = viewModel.principalInput.value.toDoubleOrNull() ?: 1.0

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw grid lines
                    for (i in 1..3) {
                        val yOffset = h * (i / 4f)
                        drawLine(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            start = Offset(0f, yOffset),
                            end = Offset(w, yOffset),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // 1. Plot Original Schedule
                    if (originalSchedule.isNotEmpty()) {
                        val origPoints = mutableListOf<Offset>()
                        origPoints.add(Offset(0f, 0f)) // starting point (Principal, represented at top of canvas, y=0)
                        
                        val maxOrigMonth = originalSchedule.size.toFloat()
                        originalSchedule.forEachIndexed { idx, payment ->
                            val x = w * (idx / maxOrigMonth)
                            val y = h * (1f - (payment.remainingBalance / originalPrincipal)).toFloat()
                            origPoints.add(Offset(x, y))
                        }
                        
                        val origPath = Path().apply {
                            moveTo(origPoints[0].x, origPoints[0].y)
                            for (p in 1 until origPoints.size) {
                                lineTo(origPoints[p].x, origPoints[p].y)
                            }
                        }
                        
                        drawPath(
                            path = origPath,
                            color = Color.LightGray,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 2. Plot Revised Schedule (Prepayments)
                    if (revisedSchedule.isNotEmpty() && originalSchedule.isNotEmpty()) {
                        val revPoints = mutableListOf<Offset>()
                        revPoints.add(Offset(0f, 0f))
                        
                        val maxOrigMonth = originalSchedule.size.toFloat() // use same horizontal scale
                        revisedSchedule.forEachIndexed { idx, payment ->
                            val x = w * (idx / maxOrigMonth)
                            val y = h * (1f - (payment.remainingBalance / originalPrincipal)).toFloat()
                            revPoints.add(Offset(x, y))
                        }
                        
                        val revPath = Path().apply {
                            moveTo(revPoints[0].x, revPoints[0].y)
                            for (p in 1 until revPoints.size) {
                                lineTo(revPoints[p].x, revPoints[p].y)
                            }
                        }

                        // Use a sleek golden gradient brush for payoff line
                        drawPath(
                            path = revPath,
                            brush = Brush.linearGradient(colors = listOf(GoldAmber, EmeraldGreen)),
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Chart Legend Labels
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color.LightGray))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Original Payoff Schedule", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(GoldAmber))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Revised (Accelerated) Schedule", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Inputs for prepayments
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Set Regular Extra Payments",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Extra Monthly
                OutlinedTextField(
                    value = monthlyPrep,
                    onValueChange = { viewModel.updateMonthlyPrepayment(it) },
                    label = { Text("Extra Monthly Payment") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("$currencySymbol ") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = "extra monthly icon") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("monthly_prepayment_input")
                )

                // Extra Yearly
                OutlinedTextField(
                    value = yearlyPrep,
                    onValueChange = { viewModel.updateYearlyPrepayment(it) },
                    label = { Text("Extra Annual Payment (Bonus, Tax Refund etc)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    prefix = { Text("$currencySymbol ") },
                    leadingIcon = { Icon(Icons.Default.Savings, contentDescription = "extra annual icon") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("yearly_prepayment_input")
                )
            }
        }

        // Custom One-time Lump-Sump prepayments builder
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add One-time Prepayments",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Have an expected future inflow? Simulate paying a lump sum on a specific month of the loan.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tempMonth,
                        onValueChange = { tempMonth = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Month #") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.8f)
                            .testTag("lump_sum_month_input")
                    )

                    OutlinedTextField(
                        value = tempAmount,
                        onValueChange = { tempAmount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text("Amount ($currencySymbol)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("$currencySymbol ") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("lump_sum_amount_input")
                    )

                    Button(
                        onClick = {
                            val mVal = tempMonth.toIntOrNull()
                            val aVal = tempAmount.toDoubleOrNull()
                            if (mVal != null && aVal != null && mVal > 0 && aVal > 0.0) {
                                viewModel.addOneTimePrepayment(mVal, aVal)
                                tempMonth = ""
                                tempAmount = ""
                                focusManager.clearFocus()
                            }
                        },
                        enabled = tempMonth.isNotBlank() && tempAmount.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .height(56.dp)
                            .testTag("add_lump_sum_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add prepayment")
                    }
                }

                // List of added Custom Prepayments
                if (customPrepayments.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Added Lump-Sums:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customPrepayments.forEach { prep ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Event,
                                        contentDescription = "one time prep event",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Month ${prep.month}: ${formatCurrency(prep.amount, selectedCurrency)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeOneTimePrepayment(prep.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove Prepayment item",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
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
fun AmortizationScheduleTabScreen(
    viewModel: LoanViewModel,
    summary: LoanSummary
) {
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    var scheduleType by remember { mutableStateOf(0) } // 0 = Original Schedule, 1 = Revised Schedule
    val activeSchedule = if (scheduleType == 0) summary.originalSchedule else summary.revisedSchedule
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Amortization Schedule",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Switch Schedule Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (scheduleType == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { scheduleType = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Original Plan",
                    color = if (scheduleType == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (scheduleType == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { scheduleType = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Payoff (Prepaid) Plan",
                    color = if (scheduleType == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        // Table Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Month", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(0.7f))
                Text("Principal", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                Text("Interest", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                if (scheduleType == 1) {
                    Text("Prepaid", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                }
                Text("Balance", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
            }
        }

        // Amortization List
        if (activeSchedule.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No data available. Fill loan amount on Calculator.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("schedule_list"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(activeSchedule) { payment ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (payment.paymentNumber % 2 == 0) MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "M${payment.paymentNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(0.7f)
                        )
                        Text(
                            text = formatCurrency(payment.principalPaid, selectedCurrency),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = formatCurrency(payment.interestPaid, selectedCurrency),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1.2f),
                            textAlign = TextAlign.End
                        )
                        if (scheduleType == 1) {
                            Text(
                                text = if (payment.extraPrepaymentPaid > 0) formatCurrency(payment.extraPrepaymentPaid, selectedCurrency) else "-",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (payment.extraPrepaymentPaid > 0) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1.2f),
                                textAlign = TextAlign.End
                            )
                        }
                        Text(
                            text = formatCurrency(payment.remainingBalance, selectedCurrency),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1.5f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedProfilesTabScreen(
    viewModel: LoanViewModel,
    savedLoans: List<LoanProfile>,
    onLoadProfile: (LoanProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Saved Scenarios",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Compare and reload different loan scenarios and customized repayment structures instantly from your library.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        if (savedLoans.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = "No Saved Loan Profiles icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        "No saved loan scenarios yet.",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        "Tap 'Save Scenario' on the calculator tab.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("saved_loans_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedLoans) { loan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLoadProfile(loan) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = "Bookmark",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = loan.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                IconButton(
                                    onClick = { viewModel.deleteLoanProfile(loan.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Delete Profile",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SavedItemDetail(label = "Principal", value = formatCurrency(loan.principal, loan.currency))
                                SavedItemDetail(label = "Interest", value = "${loan.annualInterestRate}%")
                                SavedItemDetail(label = "Tenure", value = formatDuration(loan.tenureMonths))
                            }

                            // If prepayments exist, show indicators
                            if (loan.monthlyPrepayment > 0 || loan.yearlyPrepayment > 0 || loan.oneTimePrepayments.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                        contentDescription = "Prepayments set indicator",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    
                                    val prepSummaryList = mutableListOf<String>()
                                    if (loan.monthlyPrepayment > 0) prepSummaryList.add("+${formatCurrency(loan.monthlyPrepayment, loan.currency)}/mo")
                                    if (loan.yearlyPrepayment > 0) prepSummaryList.add("+${formatCurrency(loan.yearlyPrepayment, loan.currency)}/yr")
                                    if (loan.oneTimePrepayments.isNotEmpty()) prepSummaryList.add("${loan.oneTimePrepayments.size} lump-sum(s)")
                                    
                                    Text(
                                        text = "Prepayment strategy: " + prepSummaryList.joinToString(", "),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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
fun SavedItemDetail(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
