package com.joshtalks.joshskills.voip.state

interface VoipState {
    fun connect()
    fun disconnect()
    fun backPress()
}

// Can make Calls
class IdleState : VoipState {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }
}

// Fired an API So We have to make sure how to cancel
class SearchingState : VoipState {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }
}

// Got a Channel and Joining Agora State
class JoiningState : VoipState {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }
}

// User Joined the Agora Channel
class JoinedState : VoipState {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }
}

// Remote User Joined the Channel and can talk
class ConnectedState : VoipState {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }
}

// Some Temp. Network Problem
class ReconnectingState : VoipState {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }
}

// Fired Leave Channel and waiting for Leave Channel Callback
class LeavingState : VoipState {
    override fun connect() {
        TODO("Not yet implemented")
    }

    override fun disconnect() {
        TODO("Not yet implemented")
    }

    override fun backPress() {
        TODO("Not yet implemented")
    }
}


class Mediator {
    var state : VoipState =  IdleState()

    fun connect() {

    }
}