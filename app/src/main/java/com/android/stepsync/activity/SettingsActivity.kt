package com.android.stepsync.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MotionEvent
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.NumberFormat
import java.util.Locale
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "step_sync_prefs"
    private var currentUserId: String = ""
    
    // Constants for preference keys
    private val KEY_STEP_LENGTH = "step_length"
    private val KEY_DAILY_STEP_GOAL = "daily_step_goal" // Base key, will be prefixed with user ID
    private val KEY_UNITS = "units"
    private val KEY_THEME = "theme"
    private val KEY_NOTIFICATIONS = "notifications"
    private val KEY_DATA_SYNC = "data_sync"
    
    // Default values
    private val DEFAULT_STEP_LENGTH = 65 // 65 cm
    private val DEFAULT_STEP_GOAL = 10000 // 10,000 steps
    private val DEFAULT_UNITS = "Kilometers (km)"
    private val DEFAULT_THEME = "Light"
    private val DEFAULT_NOTIFICATIONS = true
    private val DEFAULT_DATA_SYNC = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Get current user ID
        val app = application as? MyApplication
        currentUserId = app?.firebaseAuth?.currentUser?.uid ?: ""
        
        val textStepLengthValue = findViewById<TextView>(R.id.text_step_length_value)
        val textDailyGoalValue = findViewById<TextView>(R.id.text_daily_goal_value)
        val autoCompleteUnits = findViewById<AutoCompleteTextView>(R.id.auto_complete_units)
        val autoCompleteTheme = findViewById<AutoCompleteTextView>(R.id.auto_complete_theme)
        val switchNotifications = findViewById<SwitchMaterial>(R.id.switch_notifications)
        val switchDataSync = findViewById<SwitchMaterial>(R.id.switch_data_sync)
        val textAppVersion = findViewById<TextView>(R.id.text_app_version)
        
        val layoutStepLength = findViewById<LinearLayout>(R.id.layout_step_length)
        val layoutDailyGoal = findViewById<LinearLayout>(R.id.layout_daily_goal)
        val layoutAboutDevelopers = findViewById<LinearLayout>(R.id.layout_about_developers)
        val layoutLogout = findViewById<LinearLayout>(R.id.layout_logout)
        val layoutDeleteAccount = findViewById<LinearLayout>(R.id.layout_delete_account)
        val buttonBack = findViewById<Button>(R.id.button_back)

        // Load current settings
        val stepLength = sharedPreferences.getInt(KEY_STEP_LENGTH, DEFAULT_STEP_LENGTH)
        val dailyStepGoal = sharedPreferences.getInt("${currentUserId}_${KEY_DAILY_STEP_GOAL}", DEFAULT_STEP_GOAL)
        val units = sharedPreferences.getString(KEY_UNITS, DEFAULT_UNITS)
        val theme = sharedPreferences.getString(KEY_THEME, DEFAULT_THEME)
        val notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, DEFAULT_NOTIFICATIONS)
        val dataSyncEnabled = sharedPreferences.getBoolean(KEY_DATA_SYNC, DEFAULT_DATA_SYNC)
        
        // Format and display current values
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        textStepLengthValue.text = "$stepLength cm"
        textDailyGoalValue.text = "${formatter.format(dailyStepGoal)} steps"
        
        // Set up dropdowns properly
        // For Units dropdown
        val unitOptions = resources.getStringArray(R.array.unitofmeasure_items)
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, unitOptions)
        autoCompleteUnits.setAdapter(unitAdapter)
        
        // Make sure dropdown properly shows all items
        autoCompleteUnits.threshold = Integer.MAX_VALUE
        autoCompleteUnits.setOnClickListener {
            autoCompleteUnits.showDropDown()
        }
        
        // Set current unit value, with fallback for backward compatibility
        val currentUnit = units ?: DEFAULT_UNITS
        val updatedUnit = when {
            currentUnit == "Metric" || currentUnit == "Metric (km)" -> "Kilometers (km)"
            currentUnit == "Imperial" || currentUnit == "Imperial (mi)" -> "Miles (mi)"
            unitOptions.contains(currentUnit) -> currentUnit
            else -> DEFAULT_UNITS
        }
        autoCompleteUnits.setText(updatedUnit, false)
        
        // Save the updated unit format if necessary
        if (updatedUnit != units) {
            sharedPreferences.edit().putString(KEY_UNITS, updatedUnit).apply()
        }
        
        // Handle theme dropdown setup and migration of preferences
        val themeOptions = resources.getStringArray(R.array.theme_items)
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, themeOptions)
        autoCompleteTheme.setAdapter(themeAdapter)
        
        // Make sure dropdown properly shows all items
        autoCompleteTheme.threshold = Integer.MAX_VALUE
        autoCompleteTheme.setOnClickListener {
            autoCompleteTheme.showDropDown()
        }
        
        // Convert any "System Default" theme setting to "Light"
        val currentTheme = theme ?: DEFAULT_THEME
        val updatedTheme = when {
            currentTheme == "System Default" -> "Light"
            themeOptions.contains(currentTheme) -> currentTheme
            else -> DEFAULT_THEME
        }
        autoCompleteTheme.setText(updatedTheme, false)
        
        // Save the updated theme if necessary
        if (updatedTheme != theme) {
            sharedPreferences.edit().putString(KEY_THEME, updatedTheme).apply()
            
            // Apply the theme
            when (updatedTheme) {
                "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        
        // Set up switches
        switchNotifications.isChecked = notificationsEnabled
        switchDataSync.isChecked = dataSyncEnabled
        
        // Set app version
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        textAppVersion.text = versionName
        
        // Step Length Setting
        layoutStepLength.setOnClickListener {
            val items = resources.getStringArray(R.array.step_length_items)
            val values = intArrayOf(60, 65, 70, 75, 80)
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Set Step Length")
                .setItems(items) { _, which ->
                    val newStepLength = values[which]
                    sharedPreferences.edit().putInt(KEY_STEP_LENGTH, newStepLength).apply()
                    textStepLengthValue.text = "$newStepLength cm"
                    Toast.makeText(this, "Step length set to $newStepLength cm", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        
        // Daily Step Goal Setting
        layoutDailyGoal.setOnClickListener {
            val items = resources.getStringArray(R.array.daily_goal_items)
            val values = intArrayOf(5000, 7500, 10000, 15000, 20000)
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Set Daily Step Goal")
                .setItems(items) { _, which ->
                    val newStepGoal = values[which]
                    sharedPreferences.edit().putInt("${currentUserId}_${KEY_DAILY_STEP_GOAL}", newStepGoal).apply()
                    textDailyGoalValue.text = "${formatter.format(newStepGoal)} steps"
                    Toast.makeText(this, "Daily goal set to ${items[which]}", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
        
        // Units of Measure Dropdown
        autoCompleteUnits.setOnItemClickListener { _, _, position, _ ->
            val selectedUnit = unitOptions[position]
            sharedPreferences.edit().putString(KEY_UNITS, selectedUnit).apply()
            
            val explanation = when(selectedUnit) {
                "Kilometers (km)" -> "Distances will be shown in kilometers"
                "Miles (mi)" -> "Distances will be shown in miles"
                else -> "Units changed to $selectedUnit"
            }
            
            Toast.makeText(this, explanation, Toast.LENGTH_SHORT).show()
            
            // Update home screen if needed to reflect the new unit
            val intent = Intent("com.android.stepsync.UNITS_CHANGED")
            intent.putExtra("unit_type", selectedUnit)
            
            // Use LocalBroadcastManager for safety
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            
            // Also send as a normal broadcast for components that might 
            // be registered with the system instead of LocalBroadcastManager
            intent.setPackage(packageName) // Restrict to our app only
            sendBroadcast(intent)
        }
        
        // Reset dropdown state when user cancels selection
        autoCompleteUnits.setOnDismissListener {
            // Force redraw of dropdown on next click
            autoCompleteUnits.setText(autoCompleteUnits.text.toString(), false)
        }
        
        // Theme Dropdown
        autoCompleteTheme.setOnItemClickListener { _, _, position, _ ->
            val selectedTheme = resources.getStringArray(R.array.theme_items)[position]
            sharedPreferences.edit().putString(KEY_THEME, selectedTheme).apply()
            
            // Apply theme change
            when (selectedTheme) {
                "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            
            Toast.makeText(this, "Theme set to $selectedTheme", Toast.LENGTH_SHORT).show()
        }
        
        // Reset dropdown state when user cancels selection
        autoCompleteTheme.setOnDismissListener {
            // Force redraw of dropdown on next click
            autoCompleteTheme.setText(autoCompleteTheme.text.toString(), false)
        }
        
        // Notifications Switch
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, isChecked).apply()
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
        
        // Data Sync Switch
        switchDataSync.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_DATA_SYNC, isChecked).apply()
            val message = if (isChecked) "Auto sync enabled" else "Auto sync disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // About Developers
        layoutAboutDevelopers.setOnClickListener {
            startActivity(Intent(this, DeveloperActivity::class.java))
        }

        // Back button
        buttonBack.setOnClickListener {
            finish()
        }

        // Logout
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

                    getSharedPreferences("login_prefs", MODE_PRIVATE).edit().clear().apply()

                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .show()
        }
        
        // Delete Account
        layoutDeleteAccount.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete Account")
                .setMessage("This will permanently delete your account and all associated data. This action cannot be undone. Are you sure?")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton("Delete Account") { _, _ ->
                    // Get current user
                    val app = application as MyApplication
                    val currentUser = app.firebaseAuth.currentUser
                    
                    if (currentUser != null) {
                        // Delete user data from database
                        val userId = currentUser.uid
                        val dbRef = app.database.getReference("user_activities/$userId")
                        
                        dbRef.removeValue().addOnCompleteListener { databaseTask ->
                            if (databaseTask.isSuccessful) {
                                // Then delete user authentication
                                currentUser.delete().addOnCompleteListener { authTask ->
                                    if (authTask.isSuccessful) {
                                        Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                        
                                        // Clear preferences
                                        getSharedPreferences("login_prefs", MODE_PRIVATE).edit().clear().apply()
                                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().clear().apply()
                                        
                                        // Go to login screen
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
    
    companion object {
        // Helper for converting between units
        fun convertDistance(distance: Float, fromUnit: String, toUnit: String): Float {
            return when {
                fromUnit == toUnit -> distance
                fromUnit == "Kilometers (km)" && toUnit == "Miles (mi)" -> distance * 0.621371f
                fromUnit == "Miles (mi)" && toUnit == "Kilometers (km)" -> distance * 1.60934f
                else -> distance // fallback
            }
        }
        
        // Get the appropriate distance unit string for display
        fun getDistanceUnitString(unitPreference: String): String {
            return when (unitPreference) {
                "Kilometers (km)" -> "km"
                "Miles (mi)" -> "mi"
                else -> "km" // Default fallback
            }
        }
        
        // Calculate pace from distance and time
        fun calculatePace(distanceKm: Float, timeSeconds: Long, unitPreference: String): String {
            if (distanceKm <= 0f || timeSeconds <= 0) return "-"
            
            // Calculate pace (time per unit of distance)
            return when (unitPreference) {
                "Miles (mi)" -> {
                    // Convert km to miles
                    val distanceMiles = distanceKm * 0.621371f
                    if (distanceMiles <= 0f) return "-"
                    
                    // Calculate minutes per mile
                    val minutesPerMile = (timeSeconds / 60f) / distanceMiles
                    val minutes = minutesPerMile.toInt()
                    val seconds = ((minutesPerMile - minutes) * 60).toInt()
                    
                    // Format as MM:SS
                    String.format("%d:%02d /mi", minutes, seconds)
                }
                else -> { // Kilometers
                    // Calculate minutes per km
                    val minutesPerKm = (timeSeconds / 60f) / distanceKm
                    val minutes = minutesPerKm.toInt()
                    val seconds = ((minutesPerKm - minutes) * 60).toInt()
                    
                    // Format as MM:SS
                    String.format("%d:%02d /km", minutes, seconds)
                }
            }
        }
    }
}