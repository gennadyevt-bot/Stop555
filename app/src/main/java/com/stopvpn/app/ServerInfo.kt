package com.stopvpn.app

data class ServerInfo(
    val id: String,
    val name: String,
    val country: String,
    val flagEmoji: String,
    val interfaceAddress: String,
    val interfaceDns: String,
    val interfacePrivateKey: String,
    val peerPublicKey: String,
    val peerPresharedKey: String = "",
    val peerAllowedIPs: String = "0.0.0.0/0",
    val peerEndpoint: String,
    val peerPersistentKeepalive: String = "25"
)
