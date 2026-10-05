package com.example.util

import com.example.core.model.CurrencyCode
import com.example.data.ledger.PurchaseItemSpec
import com.example.data.local.entity.PartyEntity
import com.example.data.network.DeviceStatus
import com.example.data.network.DeviceType
import com.example.data.network.NetworkDevice
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToLong

object DataJsonHelper {

    // ==========================================
    // 1. Parties (Customers / Grocery Agents) JSON
    // ==========================================

    fun exportPartiesToJson(parties: List<PartyEntity>, networkName: String = "شبكة سام ميكروتك"): String {
        val root = JSONObject()
        root.put("format", "SAM_CUSTOMER_BACKUP")
        root.put("version", 1)
        root.put("networkName", networkName)
        root.put("customerCount", parties.size)

        val array = JSONArray()
        for (p in parties) {
            val obj = JSONObject().apply {
                put("name", p.name)
                put("phone", p.phone)
                put("isCustomer", p.isCustomer)
                put("isVendor", p.isVendor)
                put("isPartner", p.isPartner)
                put("creditLimitMinor", p.creditLimitMinor)
                put("equityPercentageBasisPoints", p.equityPercentageBasisPoints)
            }
            array.put(obj)
        }
        root.put("customers", array)
        return root.toString(2)
    }

    fun parsePartiesFromJson(jsonString: String): List<PartyEntity> {
        val trimmed = jsonString.trim()
        val array: JSONArray = if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            when {
                root.has("customers") -> root.getJSONArray("customers")
                root.has("parties") -> root.getJSONArray("parties")
                root.has("retailers") -> root.getJSONArray("retailers")
                else -> JSONArray()
            }
        } else {
            JSONArray(trimmed)
        }

        val list = mutableListOf<PartyEntity>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.optString("name", "").trim()
            if (name.isNotEmpty()) {
                val phone = obj.optString("phone", "").trim()
                val isCustomer = obj.optBoolean("isCustomer", true)
                val isVendor = obj.optBoolean("isVendor", false)
                val isPartner = obj.optBoolean("isPartner", false)

                val creditLimit = if (obj.has("creditLimitMinor")) {
                    obj.optLong("creditLimitMinor", 0L)
                } else if (obj.has("balanceOwed")) {
                    (obj.optDouble("balanceOwed", 0.0) * 100.0).roundToLong().coerceAtLeast(0L)
                } else {
                    0L
                }

                list.add(
                    PartyEntity(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        phone = phone,
                        isCustomer = isCustomer,
                        isVendor = isVendor,
                        isPartner = isPartner,
                        creditLimitMinor = creditLimit,
                        equityPercentageBasisPoints = obj.optInt("equityPercentageBasisPoints", 0),
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }
        }
        return list
    }

    // ==========================================
    // 2. Network Devices JSON (Full Backward Compatibility)
    // ==========================================

    data class ParsedDevicesResult(
        val networkName: String?,
        val devices: List<NetworkDevice>
    )

    fun exportDevicesToJson(devices: List<NetworkDevice>, networkName: String = "شبكة سام ميكروتك"): String {
        val root = JSONObject()
        root.put("format", "MIKROTIK_DEVICE_BACKUP")
        root.put("version", 1)
        root.put("networkName", networkName)
        root.put("deviceCount", devices.size)

        val array = JSONArray()
        for (d in devices) {
            val obj = JSONObject().apply {
                put("name", d.name)
                put("ipAddress", d.ipAddress)
                put("deviceType", d.deviceType.name)
                put("macAddress", d.macAddress)
                put("locationArea", d.towerLocation)
                put("frequencyOrSsid", d.frequency)
                put("channelWidth", d.channelWidth)
                put("status", d.status.name)
                put("notes", d.notes)
            }
            array.put(obj)
        }
        root.put("devices", array)
        return root.toString(2)
    }

    fun parseDevicesFromJson(jsonString: String): ParsedDevicesResult {
        val trimmed = jsonString.trim()
        var extractedNetworkName: String? = null
        val array: JSONArray = if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            if (root.has("networkName")) {
                extractedNetworkName = root.optString("networkName").takeIf { it.isNotBlank() }
            }
            when {
                root.has("devices") -> root.getJSONArray("devices")
                root.has("deviceList") -> root.getJSONArray("deviceList")
                else -> JSONArray()
            }
        } else {
            JSONArray(trimmed)
        }

        val list = mutableListOf<NetworkDevice>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.optString("name", "جهاز ${i + 1}").trim()
            val ipAddress = obj.optString("ipAddress", "").trim()
            if (ipAddress.isEmpty()) continue

            val rawType = obj.optString("deviceType", "").trim().lowercase()
            val mappedType = when {
                rawType.contains("مرسل") || rawType.contains("access") || rawType.contains("ap") || rawType.contains("sector") -> DeviceType.ACCESS_POINT
                rawType.contains("لاقط") || rawType.contains("مستقبل") || rawType.contains("station") -> DeviceType.STATION
                rawType.contains("سويتش") || rawType.contains("switch") -> DeviceType.SWITCH
                rawType.contains("راوتر") || rawType.contains("router") || rawType.contains("mikrotik") || rawType.contains("ccr") -> DeviceType.ROUTER
                rawType.contains("هوائي") || rawType.contains("باوربيم") || rawType.contains("antenna") || rawType.contains("dish") -> DeviceType.ANTENNA
                else -> DeviceType.ACCESS_POINT
            }

            val rawStatus = obj.optString("status", "ONLINE").trim().uppercase()
            val mappedStatus = when {
                rawStatus.contains("OFFLINE") || rawStatus.contains("مفصول") || rawStatus.contains("معطل") -> DeviceStatus.OFFLINE
                rawStatus.contains("WARN") || rawStatus.contains("ضعيف") -> DeviceStatus.WARNING
                else -> DeviceStatus.ONLINE
            }

            val location = when {
                obj.has("locationArea") && obj.getString("locationArea").isNotBlank() -> obj.getString("locationArea").trim()
                obj.has("towerLocation") && obj.getString("towerLocation").isNotBlank() -> obj.getString("towerLocation").trim()
                obj.has("location") && obj.getString("location").isNotBlank() -> obj.getString("location").trim()
                else -> "البرج الرئيسي"
            }

            val freq = when {
                obj.has("frequencyOrSsid") && obj.getString("frequencyOrSsid").isNotBlank() -> obj.getString("frequencyOrSsid").trim()
                obj.has("frequency") && obj.getString("frequency").isNotBlank() -> obj.getString("frequency").trim()
                obj.has("model") && obj.getString("model").isNotBlank() -> obj.getString("model").trim()
                else -> "5500 MHz"
            }

            val model = obj.optString("model", "").trim()
            val port = obj.optString("portOrInterface", "").trim()
            val rawNotes = obj.optString("notes", "").trim()
            val combinedNotes = buildString {
                if (rawNotes.isNotEmpty()) append(rawNotes)
                if (model.isNotEmpty() && !freq.contains(model)) {
                    if (isNotEmpty()) append(" • ")
                    append("الموديل: $model")
                }
                if (port.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append("المنفذ: $port")
                }
            }

            list.add(
                NetworkDevice(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    ipAddress = ipAddress,
                    deviceType = mappedType,
                    macAddress = obj.optString("macAddress", ""),
                    towerLocation = location,
                    frequency = freq,
                    channelWidth = obj.optString("channelWidth", "20/40 MHz"),
                    status = mappedStatus,
                    notes = combinedNotes
                )
            )
        }
        return ParsedDevicesResult(extractedNetworkName, list)
    }

    // ==========================================
    // 3. Purchase Invoices JSON (Full Backward Compatibility)
    // ==========================================

    data class ImportedPurchaseDraft(
        val vendorName: String,
        val currencyCode: CurrencyCode,
        val exchangeRateMicros: Long,
        val items: List<PurchaseItemSpec>,
        val notes: String = ""
    )

    fun exportPurchasesToJson(
        purchases: List<ImportedPurchaseDraft>,
        networkName: String = "شبكة سام ميكروتك"
    ): String {
        val root = JSONObject()
        root.put("format", "SAM_PURCHASE_INVOICE_BACKUP")
        root.put("version", 1)
        root.put("networkName", networkName)
        root.put("invoiceCount", purchases.size)

        val array = JSONArray()
        for (p in purchases) {
            val obj = JSONObject().apply {
                put("supplierName", p.vendorName)
                put("currency", p.currencyCode.name)
                put("exchangeRateMicros", p.exchangeRateMicros)
                put("notes", p.notes)
                val itemsArr = JSONArray()
                for (item in p.items) {
                    val itemObj = JSONObject().apply {
                        put("description", item.description)
                        put("accountCode", item.accountCode)
                        put("quantity", item.quantity)
                        put("unitPriceMinor", item.unitPriceMinor)
                        put("isAsset", item.isAsset)
                        put("usefulLifeMonths", item.usefulLifeMonths ?: 36)
                    }
                    itemsArr.put(itemObj)
                }
                put("items", itemsArr)
            }
            array.put(obj)
        }
        root.put("invoices", array)
        return root.toString(2)
    }

    fun parsePurchasesFromJson(jsonString: String): List<ImportedPurchaseDraft> {
        val trimmed = jsonString.trim()
        val array: JSONArray = if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            when {
                root.has("invoices") -> root.getJSONArray("invoices")
                root.has("purchases") -> root.getJSONArray("purchases")
                root.has("purchaseInvoices") -> root.getJSONArray("purchaseInvoices")
                else -> JSONArray()
            }
        } else {
            JSONArray(trimmed)
        }

        val list = mutableListOf<ImportedPurchaseDraft>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val vendorName = when {
                obj.has("supplierName") && obj.getString("supplierName").isNotBlank() -> obj.getString("supplierName").trim()
                obj.has("vendorName") && obj.getString("vendorName").isNotBlank() -> obj.getString("vendorName").trim()
                else -> "مورد عام"
            }

            val rawCurr = obj.optString("currency", "YER").trim().uppercase()
            val currency = when {
                rawCurr.contains("USD") || rawCurr.contains("دولار") -> CurrencyCode.USD
                rawCurr.contains("SAR") || rawCurr.contains("سعودي") -> CurrencyCode.SAR
                else -> CurrencyCode.YER
            }

            val rateMicros = if (obj.has("exchangeRateMicros")) {
                obj.optLong("exchangeRateMicros", if (currency == CurrencyCode.USD) 530_000_000L else if (currency == CurrencyCode.SAR) 140_000_000L else 1_000_000L)
            } else {
                if (currency == CurrencyCode.USD) 530_000_000L else if (currency == CurrencyCode.SAR) 140_000_000L else 1_000_000L
            }

            val notes = when {
                obj.has("notes") && obj.getString("notes").isNotBlank() -> obj.getString("notes").trim()
                obj.has("invoiceNumber") -> "فاتورة رقم ${obj.getString("invoiceNumber")}"
                else -> "استيراد JSON"
            }

            val items = mutableListOf<PurchaseItemSpec>()
            if (obj.has("items")) {
                val itemsArr = obj.getJSONArray("items")
                for (j in 0 until itemsArr.length()) {
                    val itObj = itemsArr.getJSONObject(j)
                    items.add(
                        PurchaseItemSpec(
                            description = itObj.optString("description", "بند مشتريات"),
                            accountCode = itObj.optString("accountCode", "5101"),
                            quantity = itObj.optInt("quantity", 1).coerceAtLeast(1),
                            unitPriceMinor = itObj.optLong("unitPriceMinor", 10000L),
                            isAsset = itObj.optBoolean("isAsset", false),
                            usefulLifeMonths = if (itObj.has("usefulLifeMonths")) itObj.getInt("usefulLifeMonths") else null
                        )
                    )
                }
            } else {
                // Backward compatibility: If invoice was exported as a single flat total
                val amountDouble = if (obj.has("originalAmount") && obj.getDouble("originalAmount") > 0.0) {
                    obj.getDouble("originalAmount")
                } else if (obj.has("totalAmount")) {
                    obj.getDouble("totalAmount")
                } else {
                    100.0
                }
                val minorAmount = (amountDouble * 100.0).roundToLong().coerceAtLeast(100L)
                val targetType = obj.optString("targetType", "SERVICE").uppercase()
                val isFixedAsset = targetType.contains("ASSET") || targetType.contains("أصل")
                val accCode = if (isFixedAsset) "1501" else "5101"
                val summary = obj.optString("itemsSummary", "مشتريات/اشتراك شبكة")

                items.add(
                    PurchaseItemSpec(
                        description = summary,
                        accountCode = accCode,
                        quantity = 1,
                        unitPriceMinor = minorAmount,
                        isAsset = isFixedAsset,
                        usefulLifeMonths = if (isFixedAsset) 36 else null
                    )
                )
            }

            if (vendorName.isNotEmpty() && items.isNotEmpty()) {
                list.add(
                    ImportedPurchaseDraft(
                        vendorName = vendorName,
                        currencyCode = currency,
                        exchangeRateMicros = rateMicros,
                        items = items,
                        notes = notes
                    )
                )
            }
        }
        return list
    }
}
