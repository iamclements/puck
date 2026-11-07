package dev.danielclements.puck

import android.content.Context
import dev.atvremote.protocol.discovery.AppleTvDevice
import dev.atvremote.protocol.hap.Credentials

/**
 * Persists pairing credentials per device.
 *
 * Values are encrypted with a key held in the Android Keystore (see
 * [SecureStore]) before being written to app-private storage. Credentials
 * grant complete control of an Apple TV, so app-private storage alone is not
 * treated as sufficient: on a rooted device, or from a backup of the data
 * directory, plaintext would be trivially recoverable.
 *
 * Backups are additionally disabled in the manifest, since Keystore-wrapped
 * ciphertext cannot be decrypted after a restore onto different hardware.
 */

class CredentialStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("pairings", Context.MODE_PRIVATE)

    fun save(key: String, credentials: Credentials) {
        prefs.edit().putString(key, SecureStore.encrypt(credentials.serialize())).apply()
    }

    fun load(key: String): Credentials? {
        val stored = prefs.getString(key, null) ?: return null

        SecureStore.decrypt(stored)?.let { plaintext ->
            return runCatching { Credentials.parse(plaintext) }.getOrNull()
        }

        // Written by a build that predates encryption: parse it, then rewrite
        // it encrypted so the plaintext does not survive.
        val legacy = runCatching { Credentials.parse(stored) }.getOrNull() ?: return null
        save(key, legacy)
        return legacy
    }

    // The last device connected to, so the list shows something the instant
    // the app opens instead of waiting on discovery.
    fun saveLastDevice(device: AppleTvDevice) {
        prefs.edit()
            .putString("last-name", device.name)
            .putString("last-address", device.address)
            .putInt("last-port", device.port)
            .putString("last-model", device.model)
            .putString("last-identifier", device.identifier)
            .apply()
    }

    fun loadLastDevice(): AppleTvDevice? {
        val name = prefs.getString("last-name", null) ?: return null
        val address = prefs.getString("last-address", null) ?: return null
        val device = AppleTvDevice(
            name = name,
            address = address,
            port = prefs.getInt("last-port", 0),
            model = prefs.getString("last-model", null),
            identifier = prefs.getString("last-identifier", null),
        )
        return applyDisplayName(device)
    }

    fun forget(key: String) {
        prefs.edit().remove(key).apply()
        prefs.edit()
            .remove("meta-$key-name")
            .remove("meta-$key-address")
            .remove("meta-$key-port")
            .remove("meta-$key-model")
            .remove("meta-$key-identifier")
            .remove("meta-$key-usedAt")
            .remove("meta-$key-displayName")
            .apply()
    }

    fun isPaired(key: String): Boolean = prefs.contains(key)

    /**
     * Every device that has a companion pairing, excluding the AirPlay
     * second-pairing entries, the last-device note, and the per-device
     * metadata keys saveDevice() writes alongside the real credential key.
     */
    fun pairedKeys(): Set<String> = prefs.all.keys
        .filter { !it.endsWith("-airplay") && !it.startsWith("last-") && !it.startsWith("meta-") }
        .toSet()

    /**
     * Remembers every paired device's connection details (not just the most
     * recent one), stamped with the time it was last connected to.
     *
     * This is what lets a home-screen shortcut jump straight to a specific
     * Apple TV — including one that isn't the device the app happened to
     * connect to last — without first waiting on mDNS discovery.
     */
    fun saveDevice(device: AppleTvDevice) {
        val key = device.credentialKey
        prefs.edit()
            .putString("meta-$key-name", device.name)
            .putString("meta-$key-address", device.address)
            .putInt("meta-$key-port", device.port)
            .putString("meta-$key-model", device.model)
            .putString("meta-$key-identifier", device.identifier)
            .putLong("meta-$key-usedAt", System.currentTimeMillis())
            .apply()
    }

    fun loadDevice(key: String): AppleTvDevice? {
        val name = prefs.getString("meta-$key-name", null) ?: return null
        val address = prefs.getString("meta-$key-address", null) ?: return null
        return AppleTvDevice(
            name = displayName(key) ?: name,
            address = address,
            port = prefs.getInt("meta-$key-port", 0),
            model = prefs.getString("meta-$key-model", null),
            identifier = prefs.getString("meta-$key-identifier", null),
        )
    }

    /**
     * A name the user picked from the Manage Devices screen, overriding
     * whatever the Apple TV itself advertises — useful when a TV's mDNS name
     * is something like "Living Room" already taken by another room's TV, or
     * just not what the user wants to see.
     */
    fun displayName(key: String): String? = prefs.getString("meta-$key-displayName", null)

    fun setDisplayName(key: String, name: String) {
        prefs.edit().putString("meta-$key-displayName", name).apply()
    }

    /** Applies [displayName]'s override, if one is set, to an otherwise fresh device. */
    fun applyDisplayName(device: AppleTvDevice): AppleTvDevice {
        val override = displayName(device.credentialKey) ?: return device
        return if (override == device.name) device else device.copy(name = override)
    }

    /** When this TV was last connected to, or 0 if it never has been. */
    fun lastUsedAt(key: String): Long = prefs.getLong("meta-$key-usedAt", 0L)

    /**
     * Every paired TV, most recently used first, then by name. Unlike
     * [recentDevices] this includes TVs that were paired but never connected to.
     */
    fun pairedDevicesByRecency(): List<AppleTvDevice> = pairedKeys()
        .mapNotNull { key -> loadDevice(key)?.let { it to lastUsedAt(key) } }
        .sortedWith(compareByDescending<Pair<AppleTvDevice, Long>> { it.second }.thenBy { it.first.name })
        .map { it.first }

    /** Paired devices ordered by most recently connected to, for shortcuts. */
    fun recentDevices(limit: Int): List<AppleTvDevice> = pairedKeys()
        .mapNotNull { key -> prefs.getLong("meta-$key-usedAt", 0L).takeIf { it > 0 }?.let { key to it } }
        .sortedByDescending { it.second }
        .take(limit)
        .mapNotNull { (key, _) -> loadDevice(key) }

    // Now-playing needs a second, independent AirPlay pairing with its own PIN.
    fun airplayKey(key: String): String = "$key-airplay"
    fun saveAirPlay(key: String, credentials: Credentials) = save(airplayKey(key), credentials)
    fun loadAirPlay(key: String): Credentials? = load(airplayKey(key))
    fun isAirPlayPaired(key: String): Boolean = isPaired(airplayKey(key))
    fun forgetAirPlay(key: String) = forget(airplayKey(key))
}
