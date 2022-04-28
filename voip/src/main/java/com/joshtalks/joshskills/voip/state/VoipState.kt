//package com.joshtalks.joshskills.voip.state
//
//import android.util.Log
//import com.joshtalks.joshskills.voip.constant.*
//import com.joshtalks.joshskills.voip.mediator.CallingMediator
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.flow.collect
//import kotlinx.coroutines.launch
//
//interface VoipState {
//    fun connect()
//    fun disconnect()
//    fun backPress()
//}
//
//
//// ISSUE ---> Because of SharedFlow we might miss some events
//
//// Can make Calls
//class IdleState(val mediator: CallingMediator) : VoipState {
//    private val TAG = "IdleState"
//
//    override fun connect() {
//        mediator.voipState = SearchingState(mediator)
//    }
//
//    override fun disconnect() {
//        Log.d(TAG, "disconnect: Illegal Request")
//    }
//
//    override fun backPress() {
//        Log.d(TAG, "backPress: ")
//    }
//}
//
//// Fired an API So We have to make sure how to cancel
//class SearchingState(val mediator: CallingMediator) : VoipState {
//    private val TAG = "SearchingState"
//    private val scope = CoroutineScope(Dispatchers.IO)
//
//    init {
//        observe()
//        /**
//         * 1. Call API
//         * 2. Stay in this state
//         * 3. Once receive Channel Switch to Joining State
//         */
//    }
//
//    override fun connect() {
//        Log.d(TAG, "connect: Illegal Request")
//    }
//
//    override fun disconnect() {
//        Log.d(TAG, "disconnect: Illegal Request")
//    }
//
//    override fun backPress() {
//        Log.d(TAG, "backPress: ")
//    }
//
//    private fun observe() {
//        scope.launch {
//            mediator.observeEvents().collect {
//                if(it.what == RECEIVED_CHANNEL_DATA) {
//                    Log.d(TAG, "observe: Received Channel Data")
//                    mediator.voipState = JoiningState(mediator)
//                }
//            }
//        }
//    }
//}
//
//// Got a Channel and Joining Agora State
//class JoiningState(val mediator: CallingMediator) : VoipState {
//    private val TAG = "JoiningState"
//    private val scope = CoroutineScope(Dispatchers.IO)
//
//    init {
//        observe()
//    }
//
//    override fun connect() {
//        Log.d(TAG, "connect: Illegal Request")
//    }
//
//    override fun disconnect() {
//        Log.d(TAG, "disconnect: Illegal Request")
//    }
//
//    override fun backPress() {
//        Log.d(TAG, "backPress: ")
//    }
//
//    private fun observe() {
//        scope.launch {
//            mediator.observeEvents().collect {
//                if(it.what == CALL_INITIATED_EVENT) {
//                    Log.d(TAG, "observe: Joined Received Channel")
//                    mediator.voipState = JoinedState(mediator)
//                }
//            }
//        }
//    }
//}
//
//// User Joined the Agora Channel
//class JoinedState(val mediator: CallingMediator) : VoipState {
//    private val TAG = "JoinedState"
//    private val scope = CoroutineScope(Dispatchers.IO)
//
//    init {
//        observe()
//    }
//
//    override fun connect() {
//        Log.d(TAG, "connect: Illegal Request")
//    }
//
//    override fun disconnect() {
//        Log.d(TAG, "disconnect: Illegal Request")
//    }
//
//    override fun backPress() {
//        Log.d(TAG, "backPress: ")
//    }
//
//    private fun observe() {
//        scope.launch {
//            mediator.observeEvents().collect {
//                if(it.what == CALL_CONNECTED_EVENT) {
//                    Log.d(TAG, "observe: Joined Received Channel")
//                    mediator.voipState = ConnectedState(mediator)
//                    mediator.voipState.connect()
//                }
//            }
//        }
//    }
//}
//
//// Remote User Joined the Channel and can talk
//class ConnectedState(val mediator: CallingMediator) : VoipState {
//    private val TAG = "ConnectedState"
//    private val scope = CoroutineScope(Dispatchers.IO)
//
//    init {
//        observe()
//    }
//
//    override fun connect() {
//        Log.d(TAG, "connect: Illegal Request")
//    }
//
//    override fun disconnect() {
//        scope.cancel()
//        mediator.voipState = LeavingState(mediator)
//    }
//
//    override fun backPress() {
//        Log.d(TAG, "backPress: ")
//    }
//
//    // Handle Events related to Connected State
//    private fun observe() {
//        scope.launch {
//            mediator.observeEvents().collect {
//                when(it.what) {
//                    MUTE -> Log.d(TAG, "observe: Mute")
//                    UNMUTE -> Log.d(TAG, "observe: Unmute")
//                    HOLD -> Log.d(TAG, "observe: Hold")
//                    UNHOLD -> Log.d(TAG, "observe: UnHold")
//                    RECONNECTING -> {
//                        mediator.voipState = ReconnectingState(mediator)
//                    }
//                    // From Backend
//                    CALL_DISCONNECT_REQUEST -> {
//
//                    }
//                }
//            }
//        }
//    }
//}
//
//// Some Temp. Network Problem
//class ReconnectingState(val mediator: CallingMediator) : VoipState {
//    private val TAG = "ReconnectingState"
//    private val scope = CoroutineScope(Dispatchers.IO)
//    private val switchingScope = CoroutineScope(Dispatchers.IO)
//
//    init {
//        observe()
//    }
//
//    override fun connect() {
//        Log.d(TAG, "connect: Illegal Request")
//    }
//
//    override fun disconnect() {
//        scope.cancel()
//        mediator.voipState = LeavingState(mediator)
//    }
//
//    override fun backPress() {
//        Log.d(TAG, "backPress: ")
//    }
//
//    // Handle Events related to Connected State
//    private fun observe() {
//        scope.launch {
//            mediator.observeEvents().collect {
//                when(it.what) {
//                    RECONNECTED -> {
//                        mediator.voipState = ConnectedState(mediator)
//                    }
//                }
//            }
//        }
//    }
//}
//
//// Fired Leave Channel and waiting for Leave Channel Callback
//class LeavingState(val mediator: CallingMediator) : VoipState {
//    private val TAG = "LeavingState"
//
//    private val scope = CoroutineScope(Dispatchers.IO)
//    private val switchingScope = CoroutineScope(Dispatchers.IO)
//
//    init {
//        observe()
//    }
//
//    override fun connect() {
//        Log.d(TAG, "connect: Illegal Request")
//    }
//
//    override fun disconnect() {
//        Log.d(TAG, "connect: Illegal Request")
//    }
//
//    override fun backPress() {
//        Log.d(TAG, "backPress: ")
//    }
//
//    // Handle Events related to Connected State
//    private fun observe() {
//        scope.launch {
//            mediator.observeEvents().collect {
//                when(it.what) {
//                    CALL_DISCONNECTED -> {
//                        mediator.voipState = IdleState(mediator)
//                    }
//                }
//            }
//        }
//    }
//}