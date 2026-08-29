package com.stopvpn.app

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var vpnManager: VpnManager
    private lateinit var serverAdapter: ServerAdapter
    private lateinit var serverStorage: ServerStorage
    private lateinit var btnPower: MaterialButton
    private lateinit var tvStatus: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var rvServers: RecyclerView
    private lateinit var tvCurrentServer: TextView
    private lateinit var fabAddServer: FloatingActionButton

    private var selectedServer: ServerInfo? = null
    private val servers = mutableListOf<ServerInfo>()

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedServer?.let { connectToServer(it) }
        } else {
            Toast.makeText(this, "Разрешение VPN отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Уведомления отключены — VPN может работать нестабильно", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vpnManager = VpnManager(this)
        serverStorage = ServerStorage(this)

        btnPower = findViewById(R.id.btnPower)
        tvStatus = findViewById(R.id.tvStatus)
        ivLogo = findViewById(R.id.ivLogo)
        rvServers = findViewById(R.id.rvServers)
        tvCurrentServer = findViewById(R.id.tvCurrentServer)
        fabAddServer = findViewById(R.id.fabAddServer)

        requestNotificationPermission()
        loadServers()
        setupRecyclerView()
        setupVpnCallbacks()
        updateUiState(VpnStatus.DISCONNECTED)

        btnPower.setOnClickListener {
            when (vpnManager.getStatus()) {
                VpnStatus.CONNECTED, VpnStatus.CONNECTING -> vpnManager.disconnect()
                else -> {
                    selectedServer?.let { requestVpnPermissionAndConnect(it) }
                        ?: Toast.makeText(this, "Сначала выберите сервер из списка", Toast.LENGTH_SHORT).show()
                }
            }
        }

        fabAddServer.setOnClickListener {
            showAddServerDialog()
        }
    }

    private fun loadServers() {
        servers.clear()
        val saved = serverStorage.loadServers()
        if (saved.isEmpty()) {
            servers.addAll(getDefaultServers())
            serverStorage.saveServers(servers)
        } else {
            servers.addAll(saved)
        }
    }

    private fun getDefaultServers(): List<ServerInfo> {
        return listOf(
            ServerInfo(
                id = "nl-ams-01",
                name = "NL-AMS-01",
                country = "Нидерланды, Амстердам",
                flagEmoji = "🇳🇱",
                interfaceAddress = "192.168.6.54/32",
                interfaceDns = "1.1.1.1, 8.8.8.8",
                interfacePrivateKey = "YOUR_PRIVATE_KEY_HERE",
                peerPublicKey = "YOUR_SERVER_PUBLIC_KEY_HERE",
                peerEndpoint = "nl-ams-01.stopvpn.example:51820"
            ),
            ServerInfo(
                id = "de-fra-01",
                name = "DE-FRA-01",
                country = "Германия, Франкфурт",
                flagEmoji = "🇩🇪",
                interfaceAddress = "192.168.6.54/32",
                interfaceDns = "1.1.1.1, 8.8.8.8",
                interfacePrivateKey = "YOUR_PRIVATE_KEY_HERE",
                peerPublicKey = "YOUR_SERVER_PUBLIC_KEY_HERE",
                peerEndpoint = "de-fra-01.stopvpn.example:51820"
            ),
            ServerInfo(
                id = "us-nyc-01",
                name = "US-NYC-01",
                country = "США, Нью-Йорк",
                flagEmoji = "🇺🇸",
                interfaceAddress = "192.168.6.54/32",
                interfaceDns = "1.1.1.1, 8.8.8.8",
                interfacePrivateKey = "YOUR_PRIVATE_KEY_HERE",
                peerPublicKey = "YOUR_SERVER_PUBLIC_KEY_HERE",
                peerEndpoint = "us-nyc-01.stopvpn.example:51820"
            )
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED -> { }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(this, "Уведомления нужны для стабильной работы VPN в фоне", Toast.LENGTH_LONG).show()
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        serverAdapter = ServerAdapter(
            servers,
            onServerClick = { server ->
                when (vpnManager.getStatus()) {
                    VpnStatus.CONNECTED -> {
                        if (vpnManager.getCurrentServer()?.id != server.id) {
                            vpnManager.switchServer(server)
                        }
                    }
                    VpnStatus.CONNECTING, VpnStatus.SWITCHING -> { }
                    else -> {
                        selectedServer = server
                        serverAdapter.setSelectedServer(server.id)
                        requestVpnPermissionAndConnect(server)
                    }
                }
            },
            onEditClick = { server ->
                showEditServerDialog(server)
            }
        )
        rvServers.layoutManager = LinearLayoutManager(this)
        rvServers.adapter = serverAdapter
    }

    private fun setupVpnCallbacks() {
        vpnManager.onStatusChanged = { status ->
            updateUiState(status)
            serverAdapter.setStatus(status)
        }
        vpnManager.onServerChanged = { server ->
            server?.let {
                tvCurrentServer.text = "Сервер: ${it.flagEmoji} ${it.name}"
                serverAdapter.setSelectedServer(it.id)
            } ?: run {
                tvCurrentServer.text = "Сервер: не выбран"
                serverAdapter.setSelectedServer(null)
            }
        }
    }

    private fun requestVpnPermissionAndConnect(server: ServerInfo) {
        val intent = vpnManager.getPrepareIntent(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            connectToServer(server)
        }
    }

    private fun connectToServer(server: ServerInfo) {
        if (server.interfacePrivateKey == "YOUR_PRIVATE_KEY_HERE" ||
            server.peerPublicKey == "YOUR_SERVER_PUBLIC_KEY_HERE") {
            Toast.makeText(this, "Сначала добавь конфиг через плюсик →", Toast.LENGTH_LONG).show()
            return
        }
        vpnManager.connect(server)
    }

    private fun showEditServerDialog(server: ServerInfo) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_server, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvDialogSubtitle)
        val etEndpoint = view.findViewById<EditText>(R.id.etEndpoint)
        val etPrivateKey = view.findViewById<EditText>(R.id.etPrivateKey)
        val etPublicKey = view.findViewById<EditText>(R.id.etPublicKey)
        val etPresharedKey = view.findViewById<EditText>(R.id.etPresharedKey)

        tvTitle.text = "Конфиг сервера"
        tvSubtitle.text = "${server.flagEmoji} ${server.name} — ${server.country}"

        if (!server.peerEndpoint.contains(".stopvpn.example")) {
            etEndpoint.setText(server.peerEndpoint)
        }
        if (server.interfacePrivateKey != "YOUR_PRIVATE_KEY_HERE") {
            etPrivateKey.setText(server.interfacePrivateKey)
        }
        if (server.peerPublicKey != "YOUR_SERVER_PUBLIC_KEY_HERE") {
            etPublicKey.setText(server.peerPublicKey)
        }
        if (server.peerPresharedKey.isNotEmpty()) {
            etPresharedKey.setText(server.peerPresharedKey)
        }

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Сохранить") { _, _ ->
                val endpoint = etEndpoint.text.toString().trim()
                val privateKey = etPrivateKey.text.toString().trim()
                val publicKey = etPublicKey.text.toString().trim()
                val presharedKey = etPresharedKey.text.toString().trim()

                if (endpoint.isEmpty() || privateKey.isEmpty() || publicKey.isEmpty()) {
                    Toast.makeText(this, "Заполни обязательные поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val idx = servers.indexOfFirst { it.id == server.id }
                if (idx >= 0) {
                    val updated = server.copy(
                        peerEndpoint = endpoint,
                        interfacePrivateKey = privateKey,
                        peerPublicKey = publicKey,
                        peerPresharedKey = presharedKey
                    )
                    servers[idx] = updated
                    serverAdapter.notifyItemChanged(idx)
                    serverStorage.saveServers(servers)
                    Toast.makeText(this, "Конфиг ${server.name} сохранён", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showAddServerDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_add_server, null)
        val etName = view.findViewById<EditText>(R.id.etName)
        val etCountry = view.findViewById<EditText>(R.id.etCountry)
        val etEndpoint = view.findViewById<EditText>(R.id.etEndpoint)
        val etPrivateKey = view.findViewById<EditText>(R.id.etPrivateKey)
        val etPublicKey = view.findViewById<EditText>(R.id.etPublicKey)

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Добавить") { _, _ ->
                val name = etName.text.toString().trim()
                val country = etCountry.text.toString().trim()
                val endpoint = etEndpoint.text.toString().trim()
                val privateKey = etPrivateKey.text.toString().trim()
                val publicKey = etPublicKey.text.toString().trim()

                if (name.isEmpty() || endpoint.isEmpty() || privateKey.isEmpty() || publicKey.isEmpty()) {
                    Toast.makeText(this, "Заполни все поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newServer = ServerInfo(
                    id = "custom_${System.currentTimeMillis()}",
                    name = name,
                    country = country.ifEmpty { "Custom" },
                    flagEmoji = "🌍",
                    interfaceAddress = "192.168.6.54/32",
                    interfaceDns = "1.1.1.1, 8.8.8.8",
                    interfacePrivateKey = privateKey,
                    peerPublicKey = publicKey,
                    peerEndpoint = endpoint
                )
                servers.add(newServer)
                serverAdapter.notifyItemInserted(servers.size - 1)
                rvServers.scrollToPosition(servers.size - 1)
                serverStorage.saveServers(servers)
                Toast.makeText(this, "Сервер добавлен и сохранён", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateUiState(status: VpnStatus) {
        when (status) {
            VpnStatus.CONNECTED -> {
                btnPower.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_green_dark)
                tvStatus.text = "VPN активен"
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
            VpnStatus.CONNECTING -> {
                btnPower.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark)
                tvStatus.text = "Подключение..."
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            VpnStatus.SWITCHING -> {
                btnPower.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark)
                tvStatus.text = "Смена сервера..."
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            VpnStatus.DISCONNECTING -> {
                btnPower.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_orange_dark)
                tvStatus.text = "Отключение..."
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            }
            else -> {
                btnPower.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_dark)
                tvStatus.text = "VPN отключен"
                tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (vpnManager.getStatus() == VpnStatus.CONNECTED) {
            vpnManager.disconnect()
        }
    }
}
