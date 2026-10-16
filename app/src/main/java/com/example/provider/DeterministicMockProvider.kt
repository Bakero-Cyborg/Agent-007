package com.example.provider

import com.example.model.AgentRole
import com.example.model.EvidenceCitation

/**
 * Deterministic Mock Provider for reproducible testing and instant offline execution.
 * Delivers verifiable domain outputs without external API reliance.
 */
class DeterministicMockProvider : ModelProvider {

    override val name: String = "Deterministic Verification Engine (MOCK-V1)"

    override suspend fun executeTask(
        role: AgentRole,
        taskTitle: String,
        objective: String,
        inputData: String,
        expectedOutput: String
    ): ProviderResult {
        return when (role) {
            AgentRole.SUPERVISOR -> {
                ProviderResult(
                    output = "تم تحليل الهدف بنجاح: '$objective'. تم إنشاء خطة تنفيذ معيارية DAG تتضمن توزيع المهام والتبعيات ومسارات التحقق المستقلة.",
                    tokensUsed = 240L
                )
            }
            AgentRole.RESEARCH -> {
                ProviderResult(
                    output = """
                        تقرير البحث والتقصي:
                        1. الحقائق المستخرجة: تم فحص البنية والمصادر المعتمدة وتحديد المتطلبات التقنية بدقة.
                        2. التمييز المنهجي: تم التحقق من المعايير المعمارية واستبعاد الفرضيات غير الموثقة.
                        3. التوصيات: اعتماد محرك تنسيق مبني على الرسوم البيانية الموجهة عديمة الحلقات (DAG) مع بوابات تدقيق مستقلة.
                    """.trimIndent(),
                    citations = listOf(
                        EvidenceCitation(
                            sourceTitle = "Enterprise Multi-Agent Architecture Standard",
                            uriOrRef = "https://standards.org/ai/multi-agent-spec-2026",
                            excerpt = "Decoupled supervisor and verification gates ensure robust task execution.",
                            confidence = 0.98f
                        ),
                        EvidenceCitation(
                            sourceTitle = "Zero-Trust Agent Security Protocol",
                            uriOrRef = "internal://docs/sec-gov-agent-v1",
                            excerpt = "Principle of least privilege must be applied to all tool integrations.",
                            confidence = 0.95f
                        )
                    ),
                    tokensUsed = 380L
                )
            }
            AgentRole.CYBERSECURITY -> {
                ProviderResult(
                    output = """
                        تقرير التدقيق الأمني والتحصين:
                        - نمذجة التهديدات: تقييم مخاطر Prompt Injection وحقن الأوامر غير المصرح بها.
                        - الصلاحيات: تطبيق نموذج RBAC مع بوابات موافقة بشرية (Human-in-the-Loop) للعمليات الحساسة.
                        - فحص الأسرار: لا توجد أسرار أو مفاتيح API مكشوفة في السجلات أو الذاكرة المشتركة.
                        - الحالة: النظام متوافق مع ضوابط الأمان الدفاعية المعتمدة (Level: High-Assurance).
                    """.trimIndent(),
                    citations = listOf(
                        EvidenceCitation(
                            sourceTitle = "OWASP Top 10 for LLM Applications",
                            uriOrRef = "https://owasp.org/www-project-top-10-for-large-language-model-applications/",
                            excerpt = "LLM01: Prompt Injection & LLM02: Sensitive Information Disclosure mitigated.",
                            confidence = 0.99f
                        )
                    ),
                    tokensUsed = 410L
                )
            }
            AgentRole.CODING -> {
                ProviderResult(
                    output = """
                        تنفيذ وكيل البرمجة:
                        - تحليل المتطلبات: بناء المكونات البرمجية وفق مبادئ Clean Architecture و MVVM.
                        - التنفيذ: كتابة كود Kotlin معياري خاضع لاختبارات التحقق.
                        - التحقق: جميع الاختبارات اجتازت بنجاح مع سلامة انتقالات حالات المهام وعدم وجود تسريبات.
                    """.trimIndent(),
                    tokensUsed = 460L
                )
            }
            AgentRole.DATA_ANALYSIS -> {
                ProviderResult(
                    output = """
                        تحليل البيانات والمؤشرات:
                        - العينة: 100% من المهام المسجلة في دورة التشغيل الحالية.
                        - معدل الإنجاز المتوقع: 98.4%، مع متوسط زمن استجابة 180ms للمهمة.
                        - مؤشرات الأداء: انخفاض احتمالية التراجع بنسبة 85% عبر التقييم الدوري.
                    """.trimIndent(),
                    tokensUsed = 310L
                )
            }
            AgentRole.AUTOMATION -> {
                ProviderResult(
                    output = """
                        تقرير وكيل الأتمتة:
                        - بناء تدفق العمل: تم تجهيز مشغلات الأحداث وخطوات إعادة المحاولة التلقائية (Retry Policy).
                        - خاصية Idempotency: مؤكدة لمنع تكرار العمليات عند إعادة الإرسال.
                    """.trimIndent(),
                    tokensUsed = 290L
                )
            }
            AgentRole.CRITIC_QA -> {
                ProviderResult(
                    output = "تم التدقيق والمصادقة: المخرجات متوافقة مع معايير القبول ولا تحتوي على تناقضات أو أخطاء غير معالجة.",
                    tokensUsed = 190L
                )
            }
            else -> {
                ProviderResult(
                    output = "أنجز وكيل ${role.titleAr} المطلوب بدقة وفق معايير الأداء المعتمدة والمدخلات المقدمة.",
                    tokensUsed = 220L
                )
            }
        }
    }
}
