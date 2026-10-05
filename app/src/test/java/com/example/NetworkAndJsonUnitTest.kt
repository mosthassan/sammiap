package com.example

import com.example.core.model.CurrencyCode
import com.example.data.ledger.PurchaseItemSpec
import com.example.data.local.entity.PartyEntity
import com.example.data.network.NetworkConfig
import com.example.util.DataJsonHelper
import com.example.util.NetworkSecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NetworkAndJsonUnitTest {

    @Test
    fun testPartiesJsonExportAndParse() {
        val parties = listOf(
            PartyEntity(
                id = "p1",
                name = "بقالة الأمل",
                phone = "777123456",
                isCustomer = true,
                isVendor = false,
                creditLimitMinor = 5000000L
            ),
            PartyEntity(
                id = "p2",
                name = "شركة ستارلينك اليمن",
                phone = "770999888",
                isCustomer = false,
                isVendor = true,
                creditLimitMinor = 0L
            )
        )

        val json = DataJsonHelper.exportPartiesToJson(parties)
        assertNotNull(json)
        assertTrue(json.contains("بقالة الأمل"))
        assertTrue(json.contains("شركة ستارلينك اليمن"))

        val parsed = DataJsonHelper.parsePartiesFromJson(json)
        assertEquals(2, parsed.size)
        assertEquals("بقالة الأمل", parsed[0].name)
        assertEquals("777123456", parsed[0].phone)
        assertTrue(parsed[0].isCustomer)
        assertEquals("شركة ستارلينك اليمن", parsed[1].name)
        assertTrue(parsed[1].isVendor)
    }

    @Test
    fun testPurchasesJsonExportAndParse() {
        val drafts = listOf(
            DataJsonHelper.ImportedPurchaseDraft(
                vendorName = "مؤسسة الأبراج للاتصالات",
                currencyCode = CurrencyCode.USD,
                exchangeRateMicros = 530_000_000L,
                items = listOf(
                    PurchaseItemSpec(
                        description = "راوتر CCR2004",
                        accountCode = "1501",
                        quantity = 1,
                        unitPriceMinor = 45000L,
                        isAsset = true,
                        usefulLifeMonths = 36
                    )
                ),
                notes = "توريد معدات سنترال"
            )
        )

        val json = DataJsonHelper.exportPurchasesToJson(drafts)
        assertTrue(json.contains("مؤسسة الأبراج للاتصالات"))
        assertTrue(json.contains("CCR2004"))

        val parsed = DataJsonHelper.parsePurchasesFromJson(json)
        assertEquals(1, parsed.size)
        assertEquals("مؤسسة الأبراج للاتصالات", parsed[0].vendorName)
        assertEquals(CurrencyCode.USD, parsed[0].currencyCode)
        assertEquals(1, parsed[0].items.size)
        assertEquals("راوتر CCR2004", parsed[0].items[0].description)
        assertTrue(parsed[0].items[0].isAsset)
    }

    @Test
    fun testRouterOsHardeningScriptGeneration() {
        val config = NetworkConfig(
            networkName = "شبكة سام",
            mainRouterModel = "CCR2004",
            approvedSubnet = "10.10.0.0/16",
            primaryDns = "8.8.8.8",
            secondaryDns = "1.1.1.1"
        )
        val options = NetworkSecurityHelper.SecurityOptions(
            disableInsecureServices = true,
            blockPortScan = true,
            blockBruteForceWinbox = true,
            preventDnsPoisoning = true,
            dropInvalidPackets = true,
            enableClientIsolation = true,
            customWinboxPort = 8291
        )

        val script = NetworkSecurityHelper.generateRouterOsHardeningScript(config, options)
        assertNotNull(script)
        assertTrue(script.contains("/ip service set telnet disabled=yes"))
        assertTrue(script.contains("Port_Scanners"))
        assertTrue(script.contains("Winbox_Blacklist"))
        assertTrue(script.contains("allow-remote-requests=no"))
        assertTrue(script.contains("horizon=1"))
    }

    @Test
    fun testUserExact36DevicesBackupImport() {
        val userJson = """
        {
          "format": "MIKROTIK_DEVICE_BACKUP",
          "version": 1,
          "networkName": "شبكة طلقة نت",
          "exportedAt": "2026-10-02 05:56:16",
          "deviceCount": 36,
          "devices": [
            {
              "name": "talqaap",
              "deviceType": "مرسل",
              "ipAddress": "10.0.0.102",
              "locationArea": "فوق محل الدوحة",
              "model": "nano station",
              "status": "ONLINE"
            },
            {
              "name": "AP30",
              "deviceType": "Access Point",
              "ipAddress": "10.0.0.30",
              "locationArea": "فالح مرزوق حفرين",
              "model": "kt708",
              "status": "ONLINE"
            },
            {
              "name": "AP15",
              "deviceType": "لاقط",
              "ipAddress": "10.0.0.15",
              "locationArea": "لاقط علي فاضل",
              "model": "kt708",
              "status": "ONLINE"
            },
            {
              "name": "باوربيم",
              "deviceType": "مستقبل",
              "ipAddress": "10.0.0.103",
              "locationArea": "مستقبل فوق بيتي",
              "model": "nano station",
              "status": "ONLINE"
            }
          ]
        }
        """.trimIndent()

        val result = DataJsonHelper.parseDevicesFromJson(userJson)
        assertEquals("شبكة طلقة نت", result.networkName)
        assertEquals(4, result.devices.size)
        assertEquals("talqaap", result.devices[0].name)
        assertEquals("10.0.0.102", result.devices[0].ipAddress)
        assertEquals("فوق محل الدوحة", result.devices[0].towerLocation)
        assertEquals(com.example.data.network.DeviceType.ACCESS_POINT, result.devices[0].deviceType)
        assertEquals(com.example.data.network.DeviceType.STATION, result.devices[2].deviceType)
        assertEquals(com.example.data.network.DeviceType.STATION, result.devices[3].deviceType)
    }

    @Test
    fun testLegacyCustomerBackupImport() {
        val legacyJson = """
        {
          "format": "SAM_CUSTOMER_BACKUP",
          "version": 1,
          "networkName": "شبكة طلقة نت",
          "exportedAt": "2026-10-02 05:56:16",
          "customerCount": 2,
          "customers": [
            {
              "name": "بقالة البركة",
              "ownerName": "محمد صالح",
              "phone": "771234567",
              "location": "الشارع العام",
              "balanceOwed": 25000.0
            },
            {
              "name": "مركز النجم للاتصالات",
              "ownerName": "عادل أحمد",
              "phone": "779888777",
              "location": "السوق",
              "balanceOwed": 50000.0
            }
          ]
        }
        """.trimIndent()

        val parsed = DataJsonHelper.parsePartiesFromJson(legacyJson)
        assertEquals(2, parsed.size)
        assertEquals("بقالة البركة", parsed[0].name)
        assertEquals("771234567", parsed[0].phone)
        assertTrue(parsed[0].isCustomer)
        assertEquals(2500000L, parsed[0].creditLimitMinor)
        assertEquals("مركز النجم للاتصالات", parsed[1].name)
        assertEquals(5000000L, parsed[1].creditLimitMinor)
    }

    @Test
    fun testLegacyPurchaseInvoiceBackupImport() {
        val legacyJson = """
        {
          "format": "SAM_PURCHASE_INVOICE_BACKUP",
          "version": 1,
          "networkName": "شبكة طلقة نت",
          "invoiceCount": 1,
          "invoices": [
            {
              "invoiceNumber": "PINV-2026-0001",
              "supplierName": "شركة يمن فايبر",
              "targetType": "SERVICE_FIBER",
              "currency": "USD",
              "originalAmount": 200.0,
              "totalAmount": 106000.0,
              "notes": "اشتراك خط الفايبر الرئيسي"
            }
          ]
        }
        """.trimIndent()

        val parsed = DataJsonHelper.parsePurchasesFromJson(legacyJson)
        assertEquals(1, parsed.size)
        assertEquals("شركة يمن فايبر", parsed[0].vendorName)
        assertEquals(CurrencyCode.USD, parsed[0].currencyCode)
        assertEquals(1, parsed[0].items.size)
        assertEquals("5101", parsed[0].items[0].accountCode)
        assertEquals(20000L, parsed[0].items[0].unitPriceMinor)
    }
}
