package com.joshtalks.joshskills.common.core.pstn_states

//private const val TAG = "PSTNStateReceiver"
//internal class PSTNStateReceiver() : BroadcastReceiver() {
//    private val pstnFlow = MutableSharedFlow<PSTNState>()
//    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
//    fun observePstnReceiver() : SharedFlow<PSTNState> {
//        return pstnFlow
//    }
//
//    @SuppressLint("UnsafeProtectedBroadcastReceiver")
//    override fun onReceive(applicationContext: Context, intent: Intent) {
//        try {
//            voipLog?.log("On Receive")
//            getCallingState(applicationContext, intent)
//        } catch (ex: Exception) {
//            print(ex)
//        }
//    }
//
//    private fun getCallingState(applicationContext: Context, intent: Intent) {
//
//        val telephony =
//            applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//        val customPhoneListener = PhoneStateListener()
//        telephony.listen(customPhoneListener, PhoneStateListener.LISTEN_CALL_STATE)
//        val stateStr = intent.extras?.getString(TelephonyManager.EXTRA_STATE)
//        var state = 0
//
//        when (stateStr) {
//            TelephonyManager.EXTRA_STATE_IDLE -> {
//                state = TelephonyManager.CALL_STATE_IDLE
//                CoroutineScope(Dispatchers.IO).launch {
//                    try{
//                        pstnFlow.emit(PSTNState.Idle)
//                    }
//                    catch (e : Exception){
//                        if(e is CancellationException)
//                            throw e
//                        e.printStackTrace()
//                    }
//                }
//                Log.d(TAG, "getCallingState:Idle  $state")
//            }
//            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
//                state = TelephonyManager.CALL_STATE_OFFHOOK
//                scope.launch {
//                    try{
//                        pstnFlow.emit(PSTNState.OnCall)
//                    }
//                    catch (e : Exception){
//                        if(e is CancellationException)
//                            throw e
//                        e.printStackTrace()
//                    }
//                }
//                Log.d(TAG, "getCallingState:OffHook  $state")
//            }
//            TelephonyManager.EXTRA_STATE_RINGING -> {
//                state = TelephonyManager.CALL_STATE_RINGING
//                scope.launch {
//                    try{
//                        pstnFlow.emit(PSTNState.Ringing)
//                    }
//                    catch (e : Exception){
//                        if(e is CancellationException)
//                            throw e
//                        e.printStackTrace()
//                    }
//                }
//                Log.d(TAG, "getCallingState: Ringing $state")
//            }
//        }
//    }
//}