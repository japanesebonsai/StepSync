package com.android.stepsync.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.android.stepsync.R
import com.android.stepsync.app.MyApplication
import com.android.stepsync.helper.StepTrackingService
import com.android.stepsync.utils.StepSyncConfig
import com.android.stepsync.utils.TrackingFormatters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.NumberFormat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private var currentUserId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(StepSyncConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val app = application as? MyApplication
        currentUserId = app?.firebaseAuth?.currentUser?.uid ?: ""
        
        val textStepLengthValue = findViewById<TextView>(R.id.text_step_length_value)
        val textDailyGoalValue = findViewById<TextView>(R.id.text_daily_goal_value)
        val autoCompleteUnits = findViewById<AutoCompleteTextView>(R.id.auto_complete_units)
        val autoCompleteTheme = findViewById<AutoCompleteTextView>(R.id.auto_complete_theme)
        val switchNotifications = findViewById<SwitchMaterial>(R.id.switch_notifications)
        val switchDataSync = findViewById<SwitchMaterial>(R.id.switch_data_sync)
        val switchPowerSaverTracking = findViewById<SwitchMaterial>(R.id.switch_power_saver_tracking)
        val textAppVersion = findViewById<TextView>(R.id.text_app_version)
        
        val layoutStepLength = findViewById<LinearLayout>(R.id.layout_step_length)
        val layoutDailyGoal = findViewById<LinearLayout>(R.id.layout_daily_goal)
        val layoutAboutDevelopers = findViewById<LinearLayout>(R.id.layout_about_developers)
        val layoutLogout = findViewById<LinearLayout>(R.id.layout_logout)
        val layoutDeleteAccount = findViewById<LinearLayout>(R.id.layout_delete_account)
        val buttonBack = findViewById<Button>(R.id.button_back)

        val stepLength = sharedPreferences.getInt(
            StepSyncConfig.KEY_STEP_LENGTH,
            StepSyncConfig.DEFAULT_STEP_LENGTH_CM
        )
        val dailyStepGoal = sharedPreferences.getInt(
            userKey(StepSyncConfig.KEY_DAILY_STEP_GOAL),
            StepSyncConfig.DEFAULT_DAILY_STEP_GOAL
        )
        val units = sharedPreferences.getString(StepSyncConfig.KEY_UNITS, StepSyncConfig.DEFAULT_UNITS)
        val theme = sharedPreferences.getString(StepSyncConfig.KEY_THEME, StepSyncConfig.DEFAULT_THEME)
        val notificationsEnabled = sharedPreferences.getBoolean(
            StepSyncConfig.KEY_NOTIFICATIONS,
            StepSyncConfig.DEFAULT_NOTIFICATIONS
        )
        val dataSyncEnabled = sharedPreferences.getBoolean(
            StepSyncConfig.KEY_DATA_SYNC,
            StepSyncConfig.DEFAULT_DATA_SYNC
        )
        val powerSaverTrackingEnabled = sharedPreferences.getBoolean(
            StepSyncConfig.KEY_POWER_SAVER_TRACKING,
            StepSyncConfig.DEFAULT_POWER_SAVER_TRACKING
        )
        
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        textStepLengthValue.text = "$stepLength cm"
        textDailyGoalValue.text = "${formatter.format(dailyStepGoal)} steps"
        
        val unitOptions = resources.getStringArray(R.array.unitofmeasure_items)
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, unitOptions)
        autoCompleteUnits.setAdapter(unitAdapter)
        
        autoCompleteUnits.threshold = Integer.MAX_VALUE
        autoCompleteUnits.setOnClickListener {
            autoCompleteUnits.showDropDown()
        }
        
        val updatedUnit = TrackingFormatters.normalizeUnitPreference(units)
        val safeUnit = if (unitOptions.contains(updatedUnit)) {
            updatedUnit
        } else {
            StepSyncConfig.DEFAULT_UNITS
        }
        autoCompleteUnits.setText(safeUnit, false)
        
        if (safeUnit != units) {
            sharedPreferences.edit().putString(StepSyncConfig.KEY_UNITS, safeUnit).apply()
        }
        
        val themeOptions = resources.getStringArray(R.array.theme_items)
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, themeOptions)
        autoCompleteTheme.setAdapter(themeAdapter)
        
        autoCompleteTheme.threshold = Integer.MAX_VALUE
        autoCompleteTheme.setOnClickListener {
            autoCompleteTheme.showDropDown()
        }
        
        val currentTheme = theme ?: StepSyncConfig.DEFAULT_THEME
        val updatedTheme = when {
            currentTheme == "System Default" -> "Light"
            themeOptions.contains(currentTheme) -> currentTheme
            else -> StepSyncConfig.DEFAULT_THEME
        }
        autoCompleteTheme.setText(updatedTheme, false)
        
        if (updatedTheme != theme) {
            sharedPreferences.edit().putString(StepSyncConfig.KEY_THEME, updatedTheme).apply()
            applyTheme(updatedTheme)
        }
        
        switchNotifications.isChecked = notificationsEnabled
        switchDataSync.isChecked = dataSyncEnabled
        switchPowerSaverTracking.isChecked = powerSaverTrackingEnabled
        
        val versionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            ).versionName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionName
        }
        textAppVersion.text = versionName
        
        layoutStepLength.setOnClickListener {
            val items = resources.getStringArray(R.array.step_length_items)
            val values = intArrayOf(60, 65, 70, 75, 80)
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Set Step Length")
                .setItems(items) { _, which ->
                    val newStepLength = values[which]
                    sharedPreferences.edit().putInt(StepSyncConfig.KEY_STEP_LENGTH, newStepLength).apply()
                    textStepLengthValue.text = "$newStepLength cm"
                    Toast.makeText(this, "Step length set to $newStepLength cm", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        
        layoutDailyGoal.setOnClickListener {
            val items = resources.getStringArray(R.array.daily_goal_items)
            val values = intArrayOf(5000, 7500, 10000, 15000, 20000)
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Set Daily Step Goal")
                .setItems(items) { _, which ->
                    val newStepGoal = values[which]
                    sharedPreferences.edit().putInt(userKey(StepSyncConfig.KEY_DAILY_STEP_GOAL), newStepGoal).apply()
                    textDailyGoalValue.text = "${formatter.format(newStepGoal)} steps"
                    Toast.makeText(this, "Daily goal set to ${items[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        
        autoCompleteUnits.setOnItemClickListener { _, _, position, _ ->
            val selectedUnit = unitOptions[position]
            sharedPreferences.edit().putString(StepSyncConfig.KEY_UNITS, selectedUnit).apply()
            
            val explanation = when(selectedUnit) {
                StepSyncConfig.UNIT_KILOMETERS -> "Distances will be shown in kilometers"
                StepSyncConfig.UNIT_MILES -> "Distances will be shown in miles"
                else -> "Units changed to $selectedUnit"
            }
            
            Toast.makeText(this, explanation, Toast.LENGTH_SHORT).show()
            val intent = Intent(StepSyncConfig.ACTION_UNITS_CHANGED)
                .putExtra(StepSyncConfig.EXTRA_UNIT_TYPE, selectedUnit)

            intent.setPackage(packageName)
            sendBroadcast(intent)
        }
        
        autoCompleteUnits.setOnDismissListener {
            autoCompleteUnits.setText(autoCompleteUnits.text.toString(), false)
        }
        
        autoCompleteTheme.setOnItemClickListener { _, _, position, _ ->
            val selectedTheme = resources.getStringArray(R.array.theme_items)[position]
            sharedPreferences.edit().putString(StepSyncConfig.KEY_THEME, selectedTheme).apply()
            applyTheme(selectedTheme)
            
            Toast.makeText(this, "Theme set to $selectedTheme", Toast.LENGTH_SHORT).show()
        }
        
        autoCompleteTheme.setOnDismissListener {
            autoCompleteTheme.setText(autoCompleteTheme.text.toString(), false)
        }
        
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(StepSyncConfig.KEY_NOTIFICATIONS, isChecked).apply()
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        switchDataSync.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(StepSyncConfig.KEY_DATA_SYNC, isChecked).apply()
            val message = if (isChecked) "Auto sync enabled" else "Auto sync disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        switchPowerSaverTracking.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(StepSyncConfig.KEY_POWER_SAVER_TRACKING, isChecked)
                .apply()

            if (sharedPreferences.getBoolean(StepTrackingService.PREF_IS_TRACKING, false)) {
                val serviceIntent = Intent(this, StepTrackingService::class.java).apply {
                    action = StepSyncConfig.ACTION_TRACKING_POWER_SAVER_CHANGED
                }
                startService(serviceIntent)
            }

            val message = if (isChecked) {
                "Power saver tracking enabled"
            } else {
                "Power saver tracking disabled"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        layoutAboutDevelopers.setOnClickListener {
            startActivity(Intent(this, DeveloperActivity::class.java))
        }

        buttonBack.setOnClickListener {
            finish()
        }

        layoutLogout.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton("Logout") { _, _ ->
                    val app = application as MyApplication
                    app.firebaseAuth.signOut()

                    getSharedPreferences(StepSyncConfig.LOGIN_PREFS_NAME, MODE_PRIVATE).edit().clear().apply()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .show()
        }
        
        layoutDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account and all associated data. This action cannot be undone. Are you sure?")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton("Delete Account") { _, _ ->
                    val app = application as MyApplication
                    val currentUser = app.firebaseAuth.currentUser
                    
                    if (currentUser != null) {
                        val userId = currentUser.uid
                        val dbRef = app.database.getReference("user_activities/$userId")
                        
                        dbRef.removeValue().addOnCompleteListener { databaseTask ->
                            if (databaseTask.isSuccessful) {
                                currentUser.delete().addOnCompleteListener { authTask ->
                                    if (authTask.isSuccessful) {
                                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                        
                                        getSharedPreferences(StepSyncConfig.LOGIN_PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
                                        getSharedPreferences(StepSyncConfig.PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
                                        
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finishAffinity()
                                    } else {
                                        Toast.makeText(this, "Error deleting account: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Error deleting account data: ${databaseTask.exception?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                .show()
        }
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun userKey(key: String): String {
        return StepSyncConfig.userScopedKey(currentUserId, key)
    }
    
    companion object {
        fun convertDistance(distance: Float, fromUnit: String, toUnit: String): Float {
            return TrackingFormatters.convertDistance(distance, fromUnit, toUnit)
        }
        
        fun getDistanceUnitString(unitPreference: String): String {
            return TrackingFormatters.distanceUnitCode(unitPreference)
        }
        
        fun calculatePace(distanceKm: Float, timeSeconds: Long, unitPreference: String): String {
            return TrackingFormatters.formatPace(distanceKm, timeSeconds, unitPreference)
        }
    }
}
