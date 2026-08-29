package com.stopvpn.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ServerAdapter(
    private val servers: MutableList<ServerInfo>,
    private val onServerClick: (ServerInfo) -> Unit,
    private val onEditClick: (ServerInfo) -> Unit
) : RecyclerView.Adapter<ServerAdapter.ServerViewHolder>() {

    private var selectedServerId: String? = null
    private var currentStatus: VpnStatus = VpnStatus.DISCONNECTED

    fun setSelectedServer(id: String?) {
        selectedServerId = id
        notifyDataSetChanged()
    }

    fun setStatus(status: VpnStatus) {
        currentStatus = status
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_server, parent, false)
        return ServerViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = servers[position]
        holder.bind(server, selectedServerId == server.id, currentStatus)
        holder.itemView.setOnClickListener { onServerClick(server) }
        holder.ivEdit.setOnClickListener { onEditClick(server) }
    }

    override fun getItemCount(): Int = servers.size

    class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvFlag: TextView = itemView.findViewById(R.id.tvFlag)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvCountry: TextView = itemView.findViewById(R.id.tvCountry)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val viewIndicator: View = itemView.findViewById(R.id.viewIndicator)
        val ivEdit: ImageView = itemView.findViewById(R.id.ivEdit)

        fun bind(server: ServerInfo, isSelected: Boolean, status: VpnStatus) {
            tvFlag.text = server.flagEmoji
            tvName.text = server.name
            tvCountry.text = server.country
            when {
                isSelected && status == VpnStatus.CONNECTED -> {
                    tvStatus.text = "Подключено"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                    viewIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                }
                isSelected && (status == VpnStatus.CONNECTING || status == VpnStatus.SWITCHING) -> {
                    tvStatus.text = if (status == VpnStatus.SWITCHING) "Смена сервера..." else "Подключение..."
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark))
                    viewIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark))
                }
                else -> {
                    tvStatus.text = "Нажмите для подключения"
                    tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                    viewIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                }
            }
        }
    }
}
