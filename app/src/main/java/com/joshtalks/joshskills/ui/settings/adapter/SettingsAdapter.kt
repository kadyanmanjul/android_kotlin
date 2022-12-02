package com.joshtalks.joshskills.ui.settings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MIN
import com.joshtalks.joshskills.core.textColorSet
import com.joshtalks.joshskills.databinding.FragmentSettingsBinding
import com.joshtalks.joshskills.databinding.ItemSettingBinding
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible
import com.joshtalks.joshskills.ui.settings.model.Setting

class SettingsAdapter(
    val settings: List<Setting>
): RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val layout = ItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingsViewHolder(layout)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        holder.bind(settings[position])
    }

    override fun getItemCount(): Int {
        return settings.size
    }

    inner class SettingsViewHolder(private val binding: ItemSettingBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(setting: Setting) {
            with(binding) {
                if (!setting.isVisible){
                    settingRoot.gone()
                    return
                }
                settingIcon.setImageResource(setting.icon)
                settingTitle.text = setting.title
                manageSubTitle(setting)
                manageSwitch(setting)
                if (!setting.isDisabled) {
                    manageClick(setting)
                } else {
                    settingRoot.isEnabled = false
//                    settingRoot.alpha = ALPHA_MIN
                    settingIcon.imageTintList = ContextCompat.getColorStateList(binding.root.context, R.color.disabled)
                    settingTitle.textColorSet(R.color.disabled)
                    settingSubTitle.textColorSet(R.color.disabled)
                    settingsGoIcon.imageTintList = ContextCompat.getColorStateList(binding.root.context, R.color.disabled)
                }

            }
        }

        private fun manageClick(setting: Setting) {
            binding.settingRoot.setOnClickListener {
                if (setting.showSwitch) {
                    binding.settingSwitch.isChecked = !binding.settingSwitch.isChecked
                } else {
                    setting.onClick?.invoke()
                }
            }

            if (setting.showSwitch) {
                binding.settingSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                    setting.onSwitch?.invoke(isChecked)
                }
            }
        }

        private fun manageSwitch(setting: Setting) {
            if (setting.showSwitch) {
                binding.settingsGoIcon.gone()
                binding.settingSwitch.visible()
                binding.settingSwitch.isChecked = setting.isSwitchChecked
            } else {
                binding.settingsGoIcon.visible()
                binding.settingSwitch.gone()
            }
        }

        private fun manageSubTitle(setting: Setting) {
            if (setting.subheading != null && setting.subheading != ""){
                binding.settingSubTitle.apply {
                    visible()
                    text = setting.subheading
                }
            } else {
                binding.settingSubTitle.gone()
            }
        }
    }
}