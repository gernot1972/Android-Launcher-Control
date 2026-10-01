package com.launcher_control_android.main.ui.program_settings.view

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.launcher_control_android.R
import com.launcher_control_android.data.model.response.ProgramItemModel
import com.launcher_control_android.data.model.response.ProgramProfileModel
import com.launcher_control_android.databinding.ActProgramSettingsBinding
import com.launcher_control_android.helper.util.PrefUtil
import com.launcher_control_android.helper.util.ToastUtil
import com.launcher_control_android.ui.adapter.ProgramStepAdapter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.view.View

@AndroidEntryPoint
class ProgramSettingsAct : AppCompatActivity(), View.OnClickListener {

    @Inject
    lateinit var prefs: PrefUtil

    private lateinit var binding: ActProgramSettingsBinding
    private lateinit var stepAdapter: ProgramStepAdapter
    private var sequenceItems: MutableList<ProgramItemModel> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<ActProgramSettingsBinding>(this, R.layout.act_program_settings)
        binding.click = this

        loadCurrentSequence()
        setupRecyclerView()
        setupListeners()
        updateProfileNameDisplay()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_back -> finish()
            R.id.btn_setting -> {
                startActivity(android.content.Intent(this, com.launcher_control_android.main.ui.main_configuration.view.MainConfigurationAct::class.java))
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.btnBack.setOnClickListener { finish() }
        binding.toolbar.btnSetting.setOnClickListener {
            startActivity(android.content.Intent(this, com.launcher_control_android.main.ui.main_configuration.view.MainConfigurationAct::class.java))
        }
    }

    private fun loadCurrentSequence() {
        sequenceItems.clear()
        val saved = prefs.programSequence
        if (saved.isNotEmpty()) {
            sequenceItems.addAll(saved)
        } else {
            sequenceItems.addAll(ProgramItemModel.getDefaultSequence())
        }
    }

    private fun setupRecyclerView() {
        stepAdapter = ProgramStepAdapter(sequenceItems) {
            // Auto-Save bei jeder Äderung in einem Schritt
            prefs.programSequence = sequenceItems
        }
        binding.rvProgramSteps.adapter = stepAdapter
    }

    private fun setupListeners() {
        binding.btnSaveProfileAs.setOnClickListener { showSaveProfileDialog() }
        binding.btnLoadProfile.setOnClickListener { showLoadProfileDialog() }
    }

    private fun updateProfileNameDisplay() {
        val currentName = prefs.activeProfileName
        binding.tvActiveProfileName.text = "Active Profile: $currentName"
    }

    private fun showSaveProfileDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Save Program Profile")
        builder.setMessage("Enter a name for this program profile:")

        val input = EditText(this)
        input.hint = "e.g. Field Trial 1"
        input.gravity = android.view.Gravity.CENTER
        input.textSize = 24f

        // Aktiven Profilnamen vorausfüllen und markieren
        val currentName = prefs.activeProfileName
        if (currentName.isNotBlank() && !currentName.equals("Default Profile", ignoreCase = true)) {
            input.setText(currentName)
            input.selectAll()
        }
        builder.setView(input)

        builder.setPositiveButton("Save") { dialog, _ ->
            val profileName = input.text.toString().trim()
            if (profileName.isNotEmpty()) {
                val profiles = prefs.programProfiles.toMutableList()
                val newProfile = ProgramProfileModel(
                    name = profileName,
                    items = sequenceItems.map { it.copy() }
                )

                // Prüfen, ob bereits ein Profil mit diesem Namen existiert -> Überschreiben
                val existingIndex = profiles.indexOfFirst { it.name.equals(profileName, ignoreCase = true) }
                if (existingIndex >= 0) {
                    profiles[existingIndex] = newProfile
                } else {
                    profiles.add(newProfile)
                }

                prefs.programProfiles = profiles
                prefs.activeProfileName = profileName
                updateProfileNameDisplay()
                ToastUtil.showToastMessage(this, "Profile '$profileName' saved")
            } else {
                ToastUtil.showToastMessage(this, "Name cannot be empty")
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showLoadProfileDialog() {
        val profiles = prefs.programProfiles
        if (profiles.isEmpty()) {
            ToastUtil.showToastMessage(this, "No saved profiles available")
            return
        }

        val profileNames = profiles.map { it.name }.toTypedArray()
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Load Program Profile")

        builder.setItems(profileNames) { dialog, which ->
            val selectedProfile = profiles[which]
            showProfileOptionsDialog(selectedProfile)
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showProfileOptionsDialog(profile: ProgramProfileModel) {
        val options = arrayOf("Load Profile", "Delete Profile")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Profile: ${profile.name}")

        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> loadProfile(profile)
                1 -> confirmDeleteProfile(profile)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun loadProfile(profile: ProgramProfileModel) {
        sequenceItems.clear()
        sequenceItems.addAll(profile.items.map { it.copy() })
        prefs.programSequence = sequenceItems
        prefs.activeProfileName = profile.name
        stepAdapter.notifyDataSetChanged()
        updateProfileNameDisplay()
        ToastUtil.showToastMessage(this, "Profile '${profile.name}' loaded")
    }

    private fun confirmDeleteProfile(profile: ProgramProfileModel) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete Profile")
        builder.setMessage("Are you sure you want to delete profile '${profile.name}'?")

        builder.setPositiveButton("Delete") { dialog, _ ->
            val profiles = prefs.programProfiles.toMutableList()
            profiles.removeAll { it.name.equals(profile.name, ignoreCase = true) }
            prefs.programProfiles = profiles

            if (prefs.activeProfileName.equals(profile.name, ignoreCase = true)) {
                prefs.activeProfileName = "Default Profile"
                updateProfileNameDisplay()
            }

            ToastUtil.showToastMessage(this, "Profile '${profile.name}' deleted")
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }
}