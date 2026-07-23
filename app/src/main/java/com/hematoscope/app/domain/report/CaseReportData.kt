package com.hematoscope.app.domain.report

/** One leukocyte line in the reported differential. */
data class DiffRow(val name: String, val count: Int, val percent: Float)

/** A graded red-cell finding (name + "1+/2+/3+"). */
data class GradedFinding(val name: String, val gradeLabel: String)

/**
 * Everything needed to render a case report, already resolved to display names
 * so the PDF generator stays free of catalog/DB lookups.
 */
data class CaseReportData(
    val patientCode: String,
    val description: String,
    val createdAtEpochMs: Long,
    val hasDifferential: Boolean,
    val differentialName: String,
    val differentialTarget: Int,
    val wbcTotal: Int,
    val differentialRows: List<DiffRow>,
    val nrbcCount: Int,
    val nrbcPer100Wbc: Float,
    val gradedFindings: List<GradedFinding>,
    val qualitativeFindings: List<String>,
    val capturePaths: List<String>
)
