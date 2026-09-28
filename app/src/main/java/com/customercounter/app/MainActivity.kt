package com.customercounter.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.customercounter.app.data.Customer
import com.customercounter.app.ui.CustomerCounterTheme
import com.customercounter.app.util.ExportManager
import com.customercounter.app.util.Formatters
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CustomerCounterApp()
        }
    }
}

private enum class Tab { HOME, STATS, SETTINGS }

@Composable
fun CustomerCounterApp(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(Tab.HOME) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var permissionDialog by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var sortRecent by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(context.getSharedPreferences("prefs", 0).getString("theme", "system") ?: "system") }
    var language by remember { mutableStateOf(context.getSharedPreferences("prefs", 0).getString("language", "system") ?: "system") }
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val callPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    val phoneStatePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.sync() }
    val scope = rememberCoroutineScope()

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { runCatching { vm.backup(uri) }.onSuccess { Toast.makeText(context, context.getString(R.string.backup_success), Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(context, context.getString(R.string.export_failed), Toast.LENGTH_SHORT).show() } }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { runCatching { vm.restore(uri); vm.sync() }.onSuccess { Toast.makeText(context, context.getString(R.string.restore_success), Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(context, context.getString(R.string.invalid_backup), Toast.LENGTH_LONG).show() } }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { runCatching { ExportManager(context).writeCsv(uri, vm.allCustomers()) }.onSuccess { Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(context, context.getString(R.string.export_failed), Toast.LENGTH_SHORT).show() } }
    }
    val xlsxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { uri ->
        if (uri != null) scope.launch { runCatching { ExportManager(context).writeXlsx(uri, vm.allCustomers()) }.onSuccess { Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_SHORT).show() }.onFailure { Toast.makeText(context, context.getString(R.string.export_failed), Toast.LENGTH_SHORT).show() } }
    }

    LaunchedEffect(Unit) {
        if (!callPermission) permissionDialog = true
    }

    LaunchedEffect(callPermission, phoneStatePermission) {
        if (callPermission) {
            com.customercounter.app.calllog.CallLogMonitor.start(context)
            vm.sync()
        }
    }

    CustomerCounterTheme(darkTheme = dark) {
        if (selectedId != null) {
            CustomerDetailsScreen(
                id = selectedId!!,
                vm = vm,
                onBack = { selectedId = null }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(if (tab == Tab.HOME) stringResource(com.customercounter.app.R.string.customers) else if (tab == Tab.STATS) stringResource(com.customercounter.app.R.string.statistics) else stringResource(com.customercounter.app.R.string.settings)) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    BottomAppBar(modifier = Modifier.navigationBarsPadding()) {
                        NavigationBarItem(selected = tab == Tab.HOME, onClick = { tab = Tab.HOME }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.customers)) })
                        NavigationBarItem(selected = tab == Tab.STATS, onClick = { tab = Tab.STATS }, icon = { Icon(Icons.Default.Sort, null) }, label = { Text(stringResource(R.string.statistics)) })
                        NavigationBarItem(selected = tab == Tab.SETTINGS, onClick = { tab = Tab.SETTINGS }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text(stringResource(R.string.settings)) })
                    }
                }
            ) { padding ->
                when (tab) {
                    Tab.HOME -> HomeScreen(vm, search, { search = it }, sortRecent, { sortRecent = it }, { selectedId = it }, padding)
                    Tab.STATS -> StatsScreen(vm, padding)
                    Tab.SETTINGS -> SettingsScreen(
                        permissionGranted = callPermission,
                        onPermission = { permissionDialog = true },
                        onOpenSettings = { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) },
                        onBackup = { backupLauncher.launch("customer-counter-backup.json") },
                        onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) },
                        onCsv = { csvLauncher.launch("customer-counter.csv") },
                        onExcel = { xlsxLauncher.launch("customer-counter.xlsx") },
                        theme = theme,
                        onTheme = { theme = it; context.getSharedPreferences("prefs", 0).edit().putString("theme", it).apply() },
                        language = language,
                        onLanguage = {
                            language = it
                            context.getSharedPreferences("prefs", 0).edit().putString("language", it).apply()
                            applyLanguage(context, it)
                        },
                        padding = padding
                    )
                }
            }
        }
    }

    if (permissionDialog) {
        AlertDialog(
            onDismissRequest = { permissionDialog = false },
            title = { Text(stringResource(R.string.call_log_permission)) },
            text = { Text(stringResource(R.string.permission_explanation)) },
            confirmButton = {
                TextButton(onClick = {
                    permissionDialog = false
                    permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_PHONE_STATE))
                }) { Text(stringResource(R.string.request_permission)) }
            },
            dismissButton = { TextButton(onClick = { permissionDialog = false }) { Text(android.R.string.cancel) } }
        )
    }
}

@Composable
private fun HomeScreen(
    vm: MainViewModel,
    search: String,
    onSearch: (String) -> Unit,
    sortRecent: Boolean,
    onSort: (Boolean) -> Unit,
    onSelect: (Long) -> Unit,
    padding: PaddingValues
) {
    val customersByNumber by vm.customersByNumber.collectAsState()
    val customersByRecent by vm.customersByRecent.collectAsState()
    val customers = if (sortRecent) customersByRecent else customersByNumber
    val filtered = remember(customers, search) {
        if (search.isBlank()) customers else customers.filter {
            it.displayPhone.contains(search.trim(), ignoreCase = true) || it.customerNumber.toString() == search.trim() || it.customerNumber.toString().contains(search.trim())
        }
    }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("${customers.size} ${stringResource(R.string.customers)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = search,
            onValueChange = onSearch,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text(stringResource(R.string.search)) },
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(8.dp))
        FilterChip(selected = sortRecent, onClick = { onSort(!sortRecent); vm.recentSort.value = !sortRecent }, label = { Text(if (sortRecent) stringResource(R.string.sort_recent) else stringResource(R.string.sort_customer_number)) }, leadingIcon = { Icon(Icons.Default.Sort, null) })
        Spacer(Modifier.height(8.dp))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_customers), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(filtered, key = { it.id }) { customer -> CustomerRow(customer, onSelect) }
            }
        }
    }
}

@Composable
private fun CustomerRow(customer: Customer, onSelect: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(customer.id) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("#${customer.customerNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(customer.displayPhone, style = MaterialTheme.typography.bodyLarge)
                Text("${stringResource(R.string.last_call)}: ${Formatters.dateTime(customer.lastCallTimestamp)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CustomerDetailsScreen(id: Long, vm: MainViewModel, onBack: () -> Unit) {
    val customer by vm.customer(id).collectAsState(initial = null)
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("#${customer?.customerNumber ?: ""}") }, navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineLarge) } }) }) { padding ->
        customer?.let { c ->
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(c.displayPhone, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                DetailItem(stringResource(R.string.first_call), Formatters.dateTime(c.firstCallTimestamp))
                DetailItem(stringResource(R.string.last_call), Formatters.dateTime(c.lastCallTimestamp))
                DetailItem(stringResource(R.string.call_count), c.totalCalls.toString())
                Button(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(c.displayPhone)}"))) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Call, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.call))
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Card(shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp)); Text(value, fontWeight = FontWeight.SemiBold) } }
}

@Composable
private fun StatsScreen(vm: MainViewModel, padding: PaddingValues) {
    var stats by remember { mutableStateOf<MainViewModel.Stats?>(null) }
    LaunchedEffect(Unit) { stats = vm.stats() }
    stats?.let { s ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { StatCard(stringResource(R.string.total_customers), s.totalCustomers.toString()) }
            item { StatCard(stringResource(R.string.new_customers_today), s.newToday.toString()) }
            item { StatCard(stringResource(R.string.new_customers_week), s.newThisWeek.toString()) }
            item { StatCard(stringResource(R.string.total_calls), s.totalCalls.toString()) }
            item { StatCard(stringResource(R.string.calls_today), s.callsToday.toString()) }
            item { Text("Most contacted", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)) }
            itemsIndexed(s.mostContacted) { _, c -> CustomerRow(c) {} }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp)); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun SettingsScreen(
    permissionGranted: Boolean,
    onPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onCsv: () -> Unit,
    onExcel: () -> Unit,
    theme: String,
    onTheme: (String) -> Unit,
    language: String,
    onLanguage: (String) -> Unit,
    padding: PaddingValues
) {
    var themeMenu by remember { mutableStateOf(false) }
    var languageMenu by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SettingRow(stringResource(R.string.call_log_permission), if (permissionGranted) stringResource(R.string.granted) else stringResource(R.string.not_granted), Icons.Default.Phone) { if (permissionGranted) onOpenSettings() else onPermission() }
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { SettingRow(stringResource(R.string.backup), "", Icons.Default.CloudUpload, onBackup) }
        item { SettingRow(stringResource(R.string.restore_backup), "", Icons.Default.CloudDownload, onRestore) }
        item { SettingRow(stringResource(R.string.export_csv), "", Icons.Default.FileUpload, onCsv) }
        item { SettingRow(stringResource(R.string.export_excel), "", Icons.Default.FileDownload, onExcel) }
        item {
            Box {
                SettingRow(stringResource(R.string.theme), theme, Icons.Default.DarkMode) { themeMenu = true }
                DropdownMenu(expanded = themeMenu, onDismissRequest = { themeMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.system)) }, onClick = { onTheme("system"); themeMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.light)) }, onClick = { onTheme("light"); themeMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.dark)) }, onClick = { onTheme("dark"); themeMenu = false })
                }
            }
        }
        item {
            Box {
                SettingRow(stringResource(R.string.language), language, Icons.Default.Language) { languageMenu = true }
                DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.english)) }, onClick = { onLanguage("en"); languageMenu = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.arabic)) }, onClick = { onLanguage("ar"); languageMenu = false })
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }
        item { Text(stringResource(R.string.about_text), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text(stringResource(R.string.privacy_text), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun applyLanguage(context: android.content.Context, language: String) {
    if (Build.VERSION.SDK_INT >= 33) {
        val manager = context.getSystemService(android.app.LocaleManager::class.java)
        manager.applicationLocales = android.os.LocaleList.forLanguageTags(language)
    } else {
        val locale = java.util.Locale(language)
        java.util.Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        if (context is ComponentActivity) context.recreate()
    }
}

