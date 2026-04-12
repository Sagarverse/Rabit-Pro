package com.example.rabit.data.repository

import android.content.Context
import com.example.rabit.domain.model.Workstation
import com.example.rabit.domain.repository.DeviceRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceRepositoryImpl(context: Context) : DeviceRepository {
    private val prefs = context.getSharedPreferences("rabit_pro_devices", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _knownWorkstations = MutableStateFlow<List<Workstation>>(emptyList())
    override val knownWorkstations: StateFlow<List<Workstation>> = _knownWorkstations.asStateFlow()

    init {
        loadWorkstations()
    }

    private fun loadWorkstations() {
        val json = prefs.getString("known_workstations", "[]")
        val type = object : TypeToken<List<Workstation>>() {}.type
        val workstations: List<Workstation> = gson.fromJson(json, type) ?: emptyList()
        _knownWorkstations.value = workstations.sortedByDescending { it.lastConnected }
    }

    override fun saveWorkstation(address: String, name: String) {
        val current = _knownWorkstations.value.toMutableList()
        val existing = current.find { it.address == address }
        
        if (existing != null) {
            current.remove(existing)
            current.add(0, existing.copy(lastConnected = System.currentTimeMillis()))
        } else {
            current.add(0, Workstation(address, name))
        }
        
        persist(current)
    }

    override fun removeWorkstation(address: String) {
        val current = _knownWorkstations.value.filter { it.address != address }
        persist(current)
    }

    override fun updateNickname(address: String, nickname: String) {
        val current = _knownWorkstations.value.map {
            if (it.address == address) it.copy(name = nickname) else it
        }
        persist(current)
    }

    private fun persist(list: List<Workstation>) {
        val sorted = list.sortedByDescending { it.lastConnected }
        _knownWorkstations.value = sorted
        prefs.edit().putString("known_workstations", gson.toJson(sorted)).apply()
    }
}
