package com.drdisagree.pixellauncherenhanced.ui.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.drdisagree.pixellauncherenhanced.BuildConfig
import com.drdisagree.pixellauncherenhanced.R
import com.drdisagree.pixellauncherenhanced.databinding.FragmentAboutBinding
import com.drdisagree.pixellauncherenhanced.utils.MiscUtils.setToolbarTitle

class About : Fragment() {

    private lateinit var binding: FragmentAboutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutBinding.inflate(inflater, container, false)

        setToolbarTitle(
            requireContext(),
            R.string.fragment_about_title,
            true,
            binding.header.toolbar,
            binding.header.collapsingToolbar
        )

        try {
            binding.appIcon.setImageDrawable(
                requireContext().packageManager.getApplicationIcon(
                    BuildConfig.APPLICATION_ID
                )
            )
        } catch (_: PackageManager.NameNotFoundException) {
            // Unlikely to happen, but just in case
            binding.appIcon.setImageResource(R.mipmap.ic_launcher)
        }
        binding.versionCode.text = getString(
            R.string.version_codes,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )
        binding.btnNews.setOnClickListener { openUrl("https://t.me/DrDsProjects") }
        binding.btnSupport.setOnClickListener { openUrl("https://t.me/DrDsProjectsChat") }
        binding.btnGithub.setOnClickListener { openUrl("https://github.com/Mahmud0808/ColorBlendr") }
        binding.developer.setOnClickListener { openUrl("https://github.com/Mahmud0808") }
        binding.buymeacoffee.setOnClickListener { openUrl("https://buymeacoffee.com/drdisagree") }

        return binding.root
    }

    private fun openUrl(url: String) {
        try {
            requireContext().startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    url.toUri()
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            parentFragmentManager.popBackStackImmediate()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}