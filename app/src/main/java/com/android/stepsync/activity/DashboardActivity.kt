package com.android.stepsync.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.android.stepsync.R
import com.android.stepsync.fragment.HomeFragment
import com.android.stepsync.fragment.ProfileFragment
import com.android.stepsync.fragment.RecordFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DashboardActivity : AppCompatActivity() {
    
    private var homeFragment: HomeFragment? = null
    private var profileFragment: ProfileFragment? = null
    private var recordFragment: RecordFragment? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        if (savedInstanceState == null) {
            homeFragment = HomeFragment()
            replaceFragment(homeFragment!!)
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    if (homeFragment == null) {
                        homeFragment = HomeFragment()
                    }
                    replaceFragment(homeFragment!!)
                    true
                }
                R.id.navigation_profile -> {
                    if (profileFragment == null) {
                        profileFragment = ProfileFragment()
                    }
                    replaceFragment(profileFragment!!)
                    true
                }
                R.id.navigation_record -> {
                    if (recordFragment == null) {
                        recordFragment = RecordFragment()
                    }
                    replaceFragment(recordFragment!!)
                    true
                }
                else -> false
            }
        }

        bottomNavigationView.selectedItemId = R.id.navigation_dashboard
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayoutFragment, fragment)
            .commit()
    }

    fun updateHomeFragmentStats() {
        homeFragment?.loadWeeklyStats()
    }

    fun switchToHomeAndUpdateStats() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.selectedItemId = R.id.navigation_dashboard

        homeFragment?.view?.post {
            homeFragment?.loadWeeklyStats()
        }
    }
}