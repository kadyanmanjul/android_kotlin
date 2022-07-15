package com.joshtalks.joshskills.ui.invite_call

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.provider.ContactsContract
import androidx.databinding.ObservableBoolean
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.base.local.entity.PhonebookContact
import com.joshtalks.joshskills.base.local.model.Mentor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class InviteFriendViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context by lazy { application.applicationContext }
    private val phonebookDao = AppObjectController.appDatabase.phonebookDao()
    private val apiService by lazy { AppObjectController.commonNetworkService }
    val isLoading = ObservableBoolean(false)
    val isContactsPermissionEnabled = ObservableBoolean(true)
    val query = MutableStateFlow("")
    val scrollToTop = ObservableBoolean(false)
    val adapter = ContactsAdapter()
    var contacts: List<PhonebookContact> = emptyList()
    val isListEmpty = ObservableBoolean(true)
    var adapterList = emptyList<PhonebookContact>()
        set(value) {
            field = value
            adapter.submitList(value)
        }

    init {
        setQueryListener()
    }

    @SuppressLint("Range")
    fun readContacts() {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )
        val temp = mutableListOf<PhonebookContact>()
        if (cursor != null) {
            while (cursor.moveToNext()) {
                val id =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val name =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                var phoneNumber =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        .replace("-", "")
                        .replace("(", "")
                        .replace(")", "")
                        .replace(" ", "").trim()
                if (phoneNumber.contains("+") && phoneNumber.contains("+91").not())
                    continue
                if (phoneNumber.first() == '0')
                    phoneNumber = phoneNumber.substring(1)
                if (phoneNumber.contains("+91").not())
                    phoneNumber = "+91$phoneNumber"
                temp.add(PhonebookContact(id, name, phoneNumber))
            }
            cursor.close()
        }
        temp.sortWith { o1, o2 ->
            o1.name.compareTo(o2.name)
        }
        val result = temp.distinctBy { it.phoneNumber }
        if (result.isNotEmpty()) {
            isListEmpty.set(false)
            contacts = result
            adapterList = result
            addContactsToDatabase(result)
        } else {
            isListEmpty.set(true)
        }
    }

    fun inviteFriend(
        contact: PhonebookContact,
        deepLink: String,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        viewModelScope.launch {
            isLoading.set(true)
            try {
                val res = apiService.inviteFriend(
                    hashMapOf(
                        "mentor_id" to Mentor.getInstance().getId(),
                        "name" to contact.name,
                        "mobile" to contact.phoneNumber,
                        "deep_link" to deepLink
                    )
                )
                if (res.isSuccessful) {
                    onSuccess()
                } else {
                    onError(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message)
            } finally {
                isLoading.set(false)
            }
        }
    }


    fun addContactsToDatabase(contacts: List<PhonebookContact>) {
        viewModelScope.launch {
            phonebookDao.insertAll(contacts).also {
                uploadUnsynchronizedContacts()
            }
        }
    }

    fun uploadUnsynchronizedContacts() {
        viewModelScope.launch {
            phonebookDao.getAllUnsynchronized().also { list ->
                if (list.isNotEmpty()) {
                    try {
                        isLoading.set(true)
                        val res = apiService.uploadContacts(
                            hashMapOf(
                                "mentor_id" to Mentor.getInstance().getId(),
                                "contacts" to list.map {
                                    mapOf(
                                        "name" to it.name,
                                        "phoneNumber" to it.phoneNumber
                                    )
                                }
                            )
                        )
                        if (res.isSuccessful) {
                            phonebookDao.updateSyncStatus(list.map { contact -> contact.id })
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        isLoading.set(false)
                    }
                }
            }
        }
    }

    private fun setQueryListener() {
        viewModelScope.launch {
            query.debounce(300)
                .distinctUntilChanged()
                .flowOn(Dispatchers.Main)
                .collect {
                    adapterList =
                        if (it.isEmpty()) {
                            contacts
                        } else it.lowercase(Locale.getDefault()).let {
                            contacts.filter { contact ->
                                contact.name.lowercase()
                                    .contains(it) || contact.phoneNumber.contains(it)
                            }.sortedBy { contact ->
                                contact.name
                            }
                        }
                    if (adapterList.isEmpty())
                        isListEmpty.set(true)
                    else
                        isListEmpty.set(false)
                    scrollToTop.set(it.isEmpty())
                }
        }
    }
}