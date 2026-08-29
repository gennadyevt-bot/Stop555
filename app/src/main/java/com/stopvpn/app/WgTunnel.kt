package com.stopvpn.app

import com.wireguard.android.backend.Tunnel

class WgTunnel(
    private val tunnelName: String,
    private val onStateChanged: ((Tunnel.State) -> Unit)? = null
) : Tunnel {

    private var currentState: Tunnel.State = Tunnel.State.DOWN

    override fun getName(): String = tunnelName

    override fun onStateChange(newState: Tunnel.State) {
        currentState = newState
        onStateChanged?.invoke(newState)
    }

    fun getState(): Tunnel.State = currentState
}
