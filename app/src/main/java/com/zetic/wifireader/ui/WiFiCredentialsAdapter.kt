package com.zetic.wifireader.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zetic.wifireader.R
import com.zetic.wifireader.model.WiFiCredentials

class WiFiCredentialsAdapter(
    private val context: Context
) : ListAdapter<WiFiCredentials, WiFiCredentialsAdapter.WiFiCredentialsViewHolder>(DiffCallback()) {

    class WiFiCredentialsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ssidText: TextView = itemView.findViewById(R.id.ssidText)
        val passwordText: TextView = itemView.findViewById(R.id.passwordText)
        val copySSIDButton: ImageButton = itemView.findViewById(R.id.copySSIDButton)
        val copyPasswordButton: ImageButton = itemView.findViewById(R.id.copyPasswordButton)
        val confidenceProgress: ProgressBar = itemView.findViewById(R.id.confidenceProgress)
        val confidenceText: TextView = itemView.findViewById(R.id.confidenceText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WiFiCredentialsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wifi_credential, parent, false)
        return WiFiCredentialsViewHolder(view)
    }

    override fun onBindViewHolder(holder: WiFiCredentialsViewHolder, position: Int) {
        val credentials = getItem(position)

        holder.ssidText.text = credentials.ssid
        holder.passwordText.text = credentials.password

        // Set confidence
        val confidencePercentage = (credentials.confidence * 100).toInt()
        holder.confidenceProgress.progress = confidencePercentage
        holder.confidenceText.text = "$confidencePercentage%"

        // Set up copy buttons
        holder.copySSIDButton.setOnClickListener {
            copyToClipboard("WiFi SSID", credentials.ssid)
        }

        holder.copyPasswordButton.setOnClickListener {
            copyToClipboard("WiFi Password", credentials.password)
        }
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    class DiffCallback : DiffUtil.ItemCallback<WiFiCredentials>() {
        override fun areItemsTheSame(oldItem: WiFiCredentials, newItem: WiFiCredentials): Boolean {
            return oldItem.ssid == newItem.ssid
        }

        override fun areContentsTheSame(oldItem: WiFiCredentials, newItem: WiFiCredentials): Boolean {
            return oldItem == newItem
        }
    }
}