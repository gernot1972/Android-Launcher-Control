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

    // Launcher zum Öffnen des Datei-Pickers für den Profil-Import
    private val importProfileLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        uri?.let { importProfileFromUri(it) }
    }

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
        binding.btnLoadProfile.setOnClickListener { showManageProfilesDialog() }
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

    private fun showManageProfilesDialog() {
        val profiles = prefs.programProfiles
        val optionsList = mutableListOf<String>()

        // Vorhandene Profile zur Liste hinzufügen
        profiles.forEach { optionsList.add(it.name) }
        // Die Import-Option steht IMMER an letzter Stelle (auch bei leerer Liste!)
        optionsList.add("📂 Import Profile from File")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Manage Program Profiles")

        builder.setItems(optionsList.toTypedArray()) { dialog, which ->
            if (which < profiles.size) {
                // Ein vorhandenes Profil wurde gewählt
                val selectedProfile = profiles[which]
                showProfileOptionsDialog(selectedProfile)
            } else {
                // "Import Profile from File" wurde gewählt
                importProfileLauncher.launch("*/*")
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun showProfileOptionsDialog(profile: ProgramProfileModel) {
        val options = arrayOf("Load Profile", "Share / Export Profile", "Save to Downloads", "Delete Profile")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Profile: ${profile.name}")

        builder.setItems(options) { dialog, which ->
            when (which) {
                0 -> loadProfile(profile)
                1 -> shareProfile(profile)
                2 -> saveProfileToDownloads(profile)
                3 -> confirmDeleteProfile(profile)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun shareProfile(profile: ProgramProfileModel) {
        try {
            val gson = com.google.gson.Gson()
            val jsonString = gson.toJson(profile)
            val fileName = "${profile.name.replace(" ", "_")}.json"
            val file = java.io.File(cacheDir, fileName)
            file.writeText(jsonString)

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "$packageName.provider",
                file
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share Profile"))
        } catch (e: Exception) {
            ToastUtil.showToastMessage(this, "Export failed: ${e.message}")
        }
    }

    private fun saveProfileToDownloads(profile: ProgramProfileModel) {
        try {
            val gson = com.google.gson.Gson()
            val jsonString = gson.toJson(profile)
            val fileName = "${profile.name.replace(" ", "_")}.json"

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonString.toByteArray())
                    }
                    ToastUtil.showToastMessage(this, "Profile saved to Downloads folder")
                } else {
                    ToastUtil.showToastMessage(this, "Save to Downloads failed")
                }
            } else {
                @Suppress("DEPRECATION")
                val targetDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(targetDir, fileName)
                file.writeText(jsonString)
                ToastUtil.showToastMessage(this, "Profile saved to Downloads folder")
            }
        } catch (e: Exception) {
            ToastUtil.showToastMessage(this, "Save failed: ${e.message}")
        }
    }

    private fun importProfileFromUri(uri: android.net.Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
            if (!jsonString.isNullOrEmpty()) {
                val gson = com.google.gson.Gson()
                val importedProfile = gson.fromJson(jsonString, ProgramProfileModel::class.java)

                if (importedProfile != null && !importedProfile.name.isNullOrEmpty()) {
                    val profiles = prefs.programProfiles.toMutableList()
                    val existingIndex = profiles.indexOfFirst { it.name.equals(importedProfile.name, ignoreCase = true) }
                    if (existingIndex >= 0) {
                        profiles[existingIndex] = importedProfile
                    } else {
                        profiles.add(importedProfile)
                    }
                    prefs.programProfiles = profiles
                    loadProfile(importedProfile)
                    ToastUtil.showToastMessage(this, "Profile '${importedProfile.name}' imported and loaded")
                } else {
                    ToastUtil.showToastMessage(this, "Invalid profile file format")
                }
            }
        } catch (e: Exception) {
            ToastUtil.showToastMessage(this, "Import failed: ${e.message}")
        }
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