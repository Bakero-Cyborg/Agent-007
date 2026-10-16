package com.example.model

/**
 * Specialist Agent Roles as specified in the platform architecture.
 */
enum class AgentRole(
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String,
    val defaultTools: List<String>,
    val requiresApprovalForDestructive: Boolean = true
) {
    SUPERVISOR(
        titleAr = "الوكيل المشرف المركزي",
        titleEn = "Supervisor Agent",
        descriptionAr = "تنسيق المهام وتحليل الأهداف وتفكيكها وإدارة دورة حياة التنفيذ",
        defaultTools = listOf("GoalDecomposer", "TaskAssigner", "DAGResolver", "ResultSynthesizer"),
        requiresApprovalForDestructive = false
    ),
    RESEARCH(
        titleAr = "وكيل البحث والتقصي",
        titleEn = "Research Agent",
        descriptionAr = "جمع المعلومات، تدقيق المصادر، استخراج الحقائق والتمييز بين الأدلة والافتراضات",
        defaultTools = listOf("WebSearch", "DocumentReader", "CitationExtractor", "FactChecker")
    ),
    CODING(
        titleAr = "وكيل البرمجة والأنظمة",
        titleEn = "Coding Agent",
        descriptionAr = "تحليل المستودعات، كتابة الشيفرات واختبارها وإصلاح الأعطال بأمان",
        defaultTools = listOf("RepoExplorer", "CodeGenerator", "TestRunner", "AstLinter")
    ),
    CYBERSECURITY(
        titleAr = "وكيل الأمن السيبراني",
        titleEn = "Cybersecurity Agent",
        descriptionAr = "نمذجة التهديدات، فحص الثغرات، كشف تسريب الأسرار وحماية Prompt Injection",
        defaultTools = listOf("VulnerabilityScanner", "SecretLeakDetector", "RbacAuditor", "PromptShield")
    ),
    DATA_ANALYSIS(
        titleAr = "وكيل تحليل البيانات",
        titleEn = "Data Analysis Agent",
        descriptionAr = "التحليل الإحصائي، اختبار الفرضيات، اكتشاف القيم الشاذة وإنتاج المؤشرات",
        defaultTools = listOf("StatisticalCalculator", "TrendDetector", "ChartGenerator")
    ),
    DATA_ENGINEERING(
        titleAr = "وكيل هندسة البيانات",
        titleEn = "Data Engineering Agent",
        descriptionAr = "إدارة دورة حياة البيانات، التحقق من المخططات وبناء خطوط ETL/ELT",
        defaultTools = listOf("SchemaValidator", "EtlPipeline", "Deduplicator", "DataLineage")
    ),
    AUTOMATION(
        titleAr = "وكيل الأتمتة والعمليات",
        titleEn = "Automation Agent",
        descriptionAr = "تنفيذ تدفقات العمل المجدولة، معالجة الأخطاء ودعم Idempotency",
        defaultTools = listOf("WorkflowRunner", "ApiConnector", "CronScheduler", "RetryHandler")
    ),
    CREATIVITY(
        titleAr = "وكيل الإبداع والمحتوى",
        titleEn = "Creativity Agent",
        descriptionAr = "توليد الأفكار، صياغة المحتوى التسويقي والسيناريوهات وموجزات التصميم",
        defaultTools = listOf("ConceptGenerator", "ContentFormatter", "PromptDrafter")
    ),
    BUSINESS(
        titleAr = "وكيل الأعمال والاستراتيجية",
        titleEn = "Business Agent",
        descriptionAr = "تحليل نماذج الأعمال، تقدير التكاليف والهوامش ووضع خطط النمو",
        defaultTools = listOf("UnitEconomicsCalc", "MarketAnalyzer", "KpiPlanner")
    ),
    PROJECT(
        titleAr = "وكيل إدارة المشاريع",
        titleEn = "Project Agent",
        descriptionAr = "تخطيط WBS، إدارة الأولويات والعوائق وتتبع الإنجاز الفعلي",
        defaultTools = listOf("WbsBuilder", "GanttPlanner", "RiskTracker")
    ),
    CRITIC_QA(
        titleAr = "وكيل المراجعة وضبط الجودة",
        titleEn = "Critic & QA Agent",
        descriptionAr = "مراجعة مستقلة لمخرجات الوكلاء والتحقق من الأدلة وصحة المعايير",
        defaultTools = listOf("OutputValidator", "HallucinationDetector", "ComplianceVerifier")
    );

    val identifier: String get() = name.lowercase()
}
