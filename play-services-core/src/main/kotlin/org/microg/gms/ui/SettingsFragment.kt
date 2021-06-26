/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import com.google.android.gms.cast.media.CastMediaRouteProviderService
import com.mgoogle.android.gms.R
import org.microg.gms.checkin.CheckinClient
import org.microg.gms.checkin.getCheckinServiceInfo
import org.microg.gms.gcm.GcmDatabase
import org.microg.gms.gcm.McsConstants.ACTION_RECONNECT
import org.microg.gms.gcm.McsService
import org.microg.gms.gcm.TriggerReceiver
import org.microg.gms.gcm.getGcmServiceInfo
import org.microg.tools.ui.ResourceSettingsFragment

class SettingsFragment : ResourceSettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        val pm = requireActivity().packageManager

        findPreference<Preference>(PREF_CHECKIN)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openCheckinSettings)
            true
        }
        findPreference<Preference>(PREF_GCM)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openGcmSettings)
            true
        }
        findPreference<Preference>(PREF_ABOUT)?.setOnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAbout)
            true
        }
        findPreference<Preference>(PREF_ABOUT)?.summary = getString(R.string.about_version_str, AboutFragment.getSelfVersion(context))

        findPreference<SwitchPreferenceCompat>(PREF_CAST_DOUBLE_FIX_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
            pm?.setComponentEnabledSetting(
                    ComponentName(requireActivity().applicationContext, CastMediaRouteProviderService::class.java),
                    when (newValue) {
                        true -> PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        else -> PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    },
                    PackageManager.DONT_KILL_APP)
            true
        }

        findPreference<SwitchPreferenceCompat>(PREF_CAST_HIDE_LAUNCHER_ICON)?.apply {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                setOnPreferenceChangeListener { _, newValue ->
                    requireActivity().hideIcon(newValue as Boolean)
                    true
                }
            } else {
                preferenceScreen.removePreference(this)
            }

        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            updateDetails()
        }
    }

    private suspend fun updateDetails() {
        val context = requireContext()
        val gcmServiceInfo = getGcmServiceInfo(context)
        if (gcmServiceInfo.configuration.enabled) {
            val database = GcmDatabase(context)
            val regCount = database.registrationList.size
            // check if we are connected as we should be and re-connect if not
            if (!gcmServiceInfo.connected) {
                context.sendBroadcast(Intent(ACTION_RECONNECT, null, context, TriggerReceiver::class.java))
            }
            database.close()
            findPreference<Preference>(PREF_GCM)?.setSummary(context.getResources().getString(R.string.service_status_enabled_short) + " - " + resources.getQuantityString(R.plurals.gcm_registered_apps_counter, regCount, regCount))
        } else {
            findPreference<Preference>(PREF_GCM)?.setSummary(R.string.service_status_disabled_short)
        }

        findPreference<Preference>(PREF_CHECKIN)?.setSummary(if (getCheckinServiceInfo(context).configuration.enabled) R.string.service_status_enabled_short else R.string.service_status_disabled_short)
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_GCM = "pref_gcm"
        const val PREF_CHECKIN = "pref_checkin"
        const val PREF_CAST_DOUBLE_FIX_ENABLED = "pref_cast_double_fix_enabled"
        const val PREF_CAST_HIDE_LAUNCHER_ICON = "pref_hide_launcher_icon"
        const val BRAND_SPOOF_FIX_ENABLED = "brand_spoof_fix_enabled"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
