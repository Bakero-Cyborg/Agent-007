package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.AgentProfileDao
import com.example.data.database.AgentDatabase
import com.example.data.entity.AgentProfileEntity
import com.example.data.repository.AgentProfileRepository
import com.example.model.AgentRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AgentDatabaseTest {

    private lateinit var database: AgentDatabase
    private lateinit var dao: AgentProfileDao
    private lateinit var repository: AgentProfileRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AgentDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.agentProfileDao()
        repository = AgentProfileRepository(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPrepopulateAndQueryAllAgentProfiles() = runBlocking {
        // Initially empty
        assertEquals(0, dao.getProfilesCount())

        // Seed default profiles
        repository.ensureDefaultProfilesLoaded()
        val count = dao.getProfilesCount()
        assertEquals(11, count)

        val all = repository.allProfiles.first()
        assertEquals(11, all.size)

        // Verify Supervisor profile
        val supervisor = repository.getProfileByIdSync("supervisor")
        assertNotNull(supervisor)
        assertEquals("الوكيل المشرف المركزي", supervisor!!.nameAr)
        assertEquals(AgentRole.SUPERVISOR, supervisor.role)
        assertTrue(supervisor.capabilities.contains("DAGDependencyResolution"))
        assertTrue(supervisor.systemPrompt.contains("الوكيل المشرف المركزي"))
    }

    @Test
    fun testUpdateSystemPrompt() = runBlocking {
        repository.ensureDefaultProfilesLoaded()

        val updatedPrompt = "توجيه جديد مخصص: التركيز على معايير OWASP LLM 2026 والتحصين الشامل."
        repository.updateSystemPrompt("cybersecurity", updatedPrompt)

        val cyber = repository.getProfileByIdSync("cybersecurity")
        assertNotNull(cyber)
        assertEquals(updatedPrompt, cyber!!.systemPrompt)
    }

    @Test
    fun testToggleAgentEnabledState() = runBlocking {
        repository.ensureDefaultProfilesLoaded()

        // Disable creativity agent
        repository.setAgentEnabled("creativity", false)
        val creativity = repository.getProfileByIdSync("creativity")
        assertNotNull(creativity)
        assertFalse(creativity!!.isEnabled)

        val enabledList = repository.enabledProfiles.first()
        assertEquals(10, enabledList.size)
        assertFalse(enabledList.any { it.id == "creativity" })

        // Re-enable
        repository.setAgentEnabled("creativity", true)
        val reEnabled = repository.getProfileByIdSync("creativity")
        assertTrue(reEnabled!!.isEnabled)
    }

    @Test
    fun testInsertAndManageCustomAgentProfile() = runBlocking {
        val customAgent = AgentProfileEntity(
            id = "custom_legal_compliance",
            nameAr = "وكيل الامتثال القانوني واللوائح",
            nameEn = "Legal & Compliance Agent",
            role = AgentRole.CRITIC_QA,
            descriptionAr = "مراجعة شروط الخدمة وحماية خصوصية بيانات المستخدمين.",
            descriptionEn = "Terms of service and privacy compliance auditor.",
            systemPrompt = "أنت وكيل الامتثال القانوني. راجع جميع المخرجات للتأكد من موافقتها للوائح التنظيمية.",
            capabilities = listOf("PrivacyCheck", "GdprAudit", "TermsReview"),
            temperature = 0.1f,
            isBuiltIn = false
        )

        repository.saveProfile(customAgent)

        val retrieved = repository.getProfileByIdSync("custom_legal_compliance")
        assertNotNull(retrieved)
        assertEquals(customAgent.nameAr, retrieved!!.nameAr)
        assertEquals(3, retrieved.capabilities.size)
        assertFalse(retrieved.isBuiltIn)

        // Delete custom profile
        repository.deleteProfile(customAgent)
        val afterDelete = repository.getProfileByIdSync("custom_legal_compliance")
        assertNull(afterDelete)
    }

    @Test
    fun testSearchProfilesByCapability() = runBlocking {
        repository.ensureDefaultProfilesLoaded()

        val searchResults = repository.searchProfiles("VulnerabilityScanning").first()
        assertEquals(1, searchResults.size)
        assertEquals("cybersecurity", searchResults[0].id)
    }
}
