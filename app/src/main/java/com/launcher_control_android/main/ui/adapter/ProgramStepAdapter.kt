package com.launcher_control_android.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.launcher_control_android.data.model.response.ProgramItemModel
import com.launcher_control_android.databinding.ItemProgramStepBinding

class ProgramStepAdapter(
    private val items: MutableList<ProgramItemModel>,
    private val onItemChanged: () -> Unit
) : RecyclerView.Adapter<ProgramStepAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemProgramStepBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProgramStepBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        val context = holder.itemView.context

        binding.tvStepIndex.text = "#${item.index}"

        // Zerstöre alte Listener vor Adapter-Setzung, um Endlosschleifen zu vermeiden
        binding.spinnerDelay.onItemSelectedListener = null
        binding.spinnerUnit.onItemSelectedListener = null
        binding.spinnerAction.onItemSelectedListener = null
        binding.spinnerVolume.onItemSelectedListener = null

        // 1. Delay Spinner Setup (weißer Text)
        val delayValues = listOf(1, 2, 3, 4, 5, 7, 10, 15, 20, 30, 45, 60, 90, 120, 180, 240, 300)
        binding.spinnerDelay.adapter = createWhiteTextAdapter(context, delayValues.map { "${it}s" })
        val delayIdx = delayValues.indexOf(item.delay).let { if (it >= 0) it else 2 }
        binding.spinnerDelay.setSelection(delayIdx)

        // 2. Unit Spinner Setup (exakte Werte: "0", "1", "2", "3", "4")
        val unitOptions = listOf("0", "1", "2", "3", "4")
        binding.spinnerUnit.adapter = createWhiteTextAdapter(context, unitOptions)
        binding.spinnerUnit.setSelection(item.fireUnit.coerceIn(0, 4))

        // 3. Action Spinner Setup (weißer Text)
        binding.spinnerAction.adapter = createWhiteTextAdapter(context, ProgramItemModel.actionTitles)
        binding.spinnerAction.setSelection(item.action)

        // 4. Dynamischer Spinner Setup für Volume / Channel (weißer Text)
        val tvLabel = binding.layoutVolume.getChildAt(0) as? TextView

        fun updateFourthSpinner() {
            if (item.action in 2..7) { // Sound-Aktion (Duck, Pheasant, etc.)
                binding.layoutVolume.visibility = View.VISIBLE
                tvLabel?.text = "Volume"
                val volumeOptions = listOf("15%", "30%", "50%", "100%")
                binding.spinnerVolume.adapter = createWhiteTextAdapter(context, volumeOptions)
                binding.spinnerVolume.setSelection((item.volumeStep - 1).coerceIn(0, 3))
            } else if (item.action == 1) { // Launch Dummy
                binding.layoutVolume.visibility = View.VISIBLE
                tvLabel?.text = "Channel"
                val channelOptions = listOf("-") + (1..12).map { "Ch $it" }
                binding.spinnerVolume.adapter = createWhiteTextAdapter(context, channelOptions)
                binding.spinnerVolume.setSelection(item.targetChannel.coerceIn(0, 12))
            } else {
                binding.layoutVolume.visibility = View.GONE
            }
        }

        updateFourthSpinner()
        updateSummary(binding, item)

        // Listener für Änderungen
        binding.spinnerDelay.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                item.delay = delayValues[pos]
                onItemChanged()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                item.fireUnit = pos
                binding.layoutVolume.visibility = if (item.isUseSound) View.VISIBLE else View.GONE
                updateSummary(binding, item)
                onItemChanged()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerAction.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                item.action = pos

                // Wenn "No Action" (Index 0) gewählt wird -> Zeile auf Standardwerte zurücksetzen
                if (pos == 0) {
                    item.delay = 3
                    item.fireUnit = 0
                    item.volumeStep = 4
                    item.targetChannel = 0

                    // UI-Spinner auf Standard-Auswahl setzen
                    val defaultDelayIdx = delayValues.indexOf(3).let { if (it >= 0) it else 2 }
                    binding.spinnerDelay.setSelection(defaultDelayIdx)
                    binding.spinnerUnit.setSelection(0)
                }

                updateFourthSpinner()
                updateSummary(binding, item)
                onItemChanged()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.spinnerVolume.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                if (item.action in 2..7) {
                    item.volumeStep = pos + 1
                } else if (item.action == 1) {
                    item.targetChannel = pos // 0 = "-", 1..12 = Ch 1..12
                }
                updateSummary(binding, item)
                onItemChanged()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

    private fun updateSummary(binding: ItemProgramStepBinding, item: ProgramItemModel) {
        if (!item.isValidSequence) {
            binding.tvStepSummary.text = "No Action"
            binding.tvStepSummary.setTextColor(0xFF888888.toInt())
        } else {
            val unitText = if (item.fireUnit > 0) "Unit ${item.fireUnit}" else ""
            val extraText = when {
                item.action in 2..7 -> {
                    val volPct = listOf("15%", "30%", "50%", "100%").getOrElse((item.volumeStep - 1).coerceIn(0, 3)) { "100%" }
                    " (Volume $volPct)"
                }
                item.action == 1 -> {
                    if (item.targetChannel > 0) " (Channel ${item.targetChannel})" else " (next available Ch)"
                }
                else -> ""
            }
            binding.tvStepSummary.text = "${item.shortActionName} -> $unitText$extraText"
            binding.tvStepSummary.setTextColor(0xFF00FF00.toInt())
        }
    }

    private fun <T> createCustomSpinnerAdapter(context: android.content.Context, items: List<T>): ArrayAdapter<T> {
        return object : ArrayAdapter<T>(context, android.R.layout.simple_spinner_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? android.widget.TextView)?.setTextColor(android.graphics.Color.WHITE)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? android.widget.TextView)?.setTextColor(android.graphics.Color.BLACK)
                return view
            }
        }.apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    /**
     * Erzeugt einen ArrayAdapter mit explizit weißem Text und dunklem Dropdown-Hintergrund
     */
    private fun <T> createWhiteTextAdapter(context: android.content.Context, items: List<T>): ArrayAdapter<T> {
        return object : ArrayAdapter<T>(context, android.R.layout.simple_spinner_dropdown_item, items) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(Color.WHITE)
                    textSize = 16f
                }
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.apply {
                    setTextColor(Color.BLACK)
                    setBackgroundColor(Color.WHITE)
                    textSize = 16f
                }
                return view
            }
        }
    }
}