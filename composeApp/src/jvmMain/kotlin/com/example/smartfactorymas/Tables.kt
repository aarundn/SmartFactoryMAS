package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════
//  BUG FIXED IN THIS FILE
//
//  OLD code (broken):
//      DataCell(r.s_debut_som, 60.dp, isBold = isAnomaly)
//      DataCell(r.s_debut_som, 60.dp, isBold = isAnomaly)  ← copy-paste bug!
//      DataCell(r.s_milieu_som, 60.dp)
//      DataCell(r.s_milieu_som, 60.dp)                     ← same bug
//      DataCell(r.s_fin_som, 60.dp)
//      DataCell(r.s_fin_som, 60.dp)                        ← same bug
//
//  This is why the SOM and SOP columns showed IDENTICAL values in Table 4.6.
//  The SOP cells were all rendering the SOM field instead of s_debut_sop,
//  s_milieu_sop, s_fin_sop, m_debut_sop, etc.
//
//  NEW (fixed): every SOP cell uses its correct field.
// ═══════════════════════════════════════════════════════════════════════════

// ── Data classes (must match CliInterop/Models.kt) ────────────────────────



// ── Empty-state initializers ──────────────────────────────────────────────

fun getEmptyTable46Data(): List<Table46Row> {
    val rows = mutableListOf<Table46Row>()
    for (m0 in listOf(5, 10, 20))
        for (m1 in listOf(20, 50, 100))
            for (m4 in listOf(2, 4, 8))
                rows.add(Table46Row(
                    m0.toString(), m1.toString(), m4.toString(),
                    "--","--","--","--","--","--",
                    "--","--","--","--","--","--"
                ))
    return rows
}

fun getEmptyTable47Data(): List<Table47Row> =
    listOf("2","4","8").map { m4 ->
        Table47Row(m4, "--","--","--","--","--","--","--","--","--","--","--","--")
    }

// ── Format helpers ────────────────────────────────────────────────────────

fun formatMs(value: Double?): String =
    if (value != null && value > 0.001) "%.2f".format(value) else "--"

fun formatPct(value: Double?): String =
    if (value != null) "%.2f%%".format(value) else "--"

// ═══════════════════════════════════════════════════════════════════════════
//  Tableau 4.6 – Réactivité (Computation Time)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun FullTableau46(rows: List<Table46Row>) {
    val border      = Color(0xFFCCCCCC)
    val headerBg    = Color(0xFFF0F0F0)
    val altRowBg    = Color(0xFFFAFAFA)
    val boldCellBg  = Color(0xFFFFF3CD)  // highlight "cases grasses"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(1.dp, border)
    ) {
        // ── Title ────────────────────────────────────────────────────────
        Text(
            text = "Tableau 4.6. Influence du nombre de jobs, de machines et de " +
                    "ressources humaines sur le temps de résolution",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
        )

        // ── Header rows ──────────────────────────────────────────────────
        Row(modifier = Modifier.background(headerBg)) {

            // Left identifiers (span 3 sub-header rows)
            Column {
                Row {
                    HeaderCell("m₀",  40.dp, 90.dp)
                    HeaderCell("m₁",  50.dp, 90.dp)
                    HeaderCell("m₄",  40.dp, 90.dp)
                }
            }

            // ── Single-machine section ────────────────────────────────
            Column {
                HeaderCell("Niveau une-machine (ms)", 360.dp, 30.dp)
                Row {
                    HeaderCell("Début",  120.dp, 30.dp)
                    HeaderCell("Milieu", 120.dp, 30.dp)
                    HeaderCell("Fin",    120.dp, 30.dp)
                }
                Row {
                    HeaderCell("SOM", 60.dp)
                    HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp)
                    HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp)
                    HeaderCell("SOP", 60.dp)
                }
            }

            // ── Multi-machine section ─────────────────────────────────
            Column {
                HeaderCell("Niveau Multi-machines (ms)", 360.dp, 30.dp)
                Row {
                    HeaderCell("Début",  120.dp, 30.dp)
                    HeaderCell("Milieu", 120.dp, 30.dp)
                    HeaderCell("Fin",    120.dp, 30.dp)
                }
                Row {
                    HeaderCell("SOM", 60.dp)
                    HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp)
                    HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp)
                    HeaderCell("SOP", 60.dp)
                }
            }
        }

        // ── Data rows ────────────────────────────────────────────────────
        //    "Cases grasses" (bold in paper): rows where m4=8 has a LOWER
        //    single-machine time than m4=4 in the same (m0, m1) group.
        //    We detect this by tracking the m4=4 value for each group.
        val boldSet = detectBoldRows(rows)

        var lastM0 = ""; var lastM1 = ""

        rows.forEachIndexed { idx, row ->
            val isBold   = idx in boldSet
            val isEven   = idx % 2 == 0
            val rowBg    = when {
                isBold  -> boldCellBg
                isEven  -> Color.White
                else    -> altRowBg
            }

            // Suppress repeated m0/m1 values (paper groups them visually)
            val displayM0 = if (row.m0 == lastM0) "" else row.m0
            val displayM1 = if (row.m0 == lastM0 && row.m1 == lastM1) "" else row.m1
            lastM0 = row.m0; lastM1 = row.m1

            // Multi-machine data: paper only shows it for m4=4 rows
            val showMulti = row.m4 == "4"

            Row(modifier = Modifier.background(rowBg)) {
                // Identifiers
                DataCell(displayM0, 40.dp, isBold = true)
                DataCell(displayM1, 50.dp, isBold = true)
                DataCell(row.m4,   40.dp, isBold = true)

                // ── FIXED: each SOP cell now uses its OWN field ──────────
                // Single-machine Début
                DataCell(row.s_debut_som,  60.dp, isBold = isBold)
                DataCell(row.s_debut_sop,  60.dp, isBold = isBold)  // ← was s_debut_som (BUG)
                // Single-machine Milieu
                DataCell(row.s_milieu_som, 60.dp)
                DataCell(row.s_milieu_sop, 60.dp)                   // ← was s_milieu_som (BUG)
                // Single-machine Fin
                DataCell(row.s_fin_som,    60.dp)
                DataCell(row.s_fin_sop,    60.dp)                   // ← was s_fin_som (BUG)

                // Multi-machine (blank for m4=2 and m4=8, per paper layout)
                if (showMulti) {
                    DataCell(row.m_debut_som,  60.dp)
                    DataCell(row.m_debut_sop,  60.dp)               // ← was m_debut_som (BUG)
                    DataCell(row.m_milieu_som, 60.dp)
                    DataCell(row.m_milieu_sop, 60.dp)               // ← was m_milieu_som (BUG)
                    DataCell(row.m_fin_som,    60.dp)
                    DataCell(row.m_fin_sop,    60.dp)               // ← was m_fin_som (BUG)
                } else {
                    repeat(6) { DataCell("", 60.dp) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Tableau 4.7 – Capacité d'absorption (Stability)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun FullTableau47(rows: List<Table47Row>) {
    val border   = Color(0xFFCCCCCC)
    val headerBg = Color(0xFFF0F0F0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .border(1.dp, border)
    ) {
        Text(
            text = "Tableau 4.7. Le retard résultant des résolutions une-machine et " +
                    "multi-machines selon la stratégie de maintenance adoptée",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(12.dp)
        )

        // ── Headers ──────────────────────────────────────────────────────
        Row(modifier = Modifier.background(headerBg)) {
            HeaderCell("m₄", 50.dp, 90.dp)

            Column {
                HeaderCell("Le retard une-machine",   360.dp, 30.dp)
                Row {
                    HeaderCell("Stable",    120.dp, 30.dp)
                    HeaderCell("Amélioré",  120.dp, 30.dp)
                    HeaderCell("Détérioré", 120.dp, 30.dp)
                }
                Row {
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                }
            }

            Column {
                HeaderCell("Le retard multi-machines", 360.dp, 30.dp)
                Row {
                    HeaderCell("Stable",    120.dp, 30.dp)
                    HeaderCell("Amélioré",  120.dp, 30.dp)
                    HeaderCell("Détérioré", 120.dp, 30.dp)
                }
                Row {
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                }
            }
        }

        // ── Data rows ────────────────────────────────────────────────────
        rows.forEachIndexed { idx, row ->
            val bg = if (idx % 2 == 0) Color.White else Color(0xFFFAFAFA)
            Row(modifier = Modifier.background(bg)) {
                DataCell(row.m4, 50.dp, isBold = true)
                // Single Stable
                DataCell(row.s_s_som, 60.dp); DataCell(row.s_s_sop, 60.dp)
                // Single Amélioré
                DataCell(row.s_a_som, 60.dp); DataCell(row.s_a_sop, 60.dp)
                // Single Détérioré
                DataCell(row.s_d_som, 60.dp); DataCell(row.s_d_sop, 60.dp)
                // Multi Stable
                DataCell(row.m_s_som, 60.dp); DataCell(row.m_s_sop, 60.dp)
                // Multi Amélioré
                DataCell(row.m_a_som, 60.dp); DataCell(row.m_a_sop, 60.dp)
                // Multi Détérioré
                DataCell(row.m_d_som, 60.dp); DataCell(row.m_d_sop, 60.dp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Cell Helpers
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun HeaderCell(text: String, width: Dp, height: Dp = 30.dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(0.5.dp, Color(0xFFDDDDDD))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text        = text,
            fontWeight  = FontWeight.SemiBold,
            fontSize    = 11.sp,
            textAlign   = TextAlign.Center,
            color       = Color(0xFF333333)
        )
    }
}

@Composable
fun DataCell(text: String, width: Dp, isBold: Boolean = false) {
    Box(
        modifier = Modifier
            .width(width)
            .height(30.dp)
            .border(0.5.dp, Color(0xFFEEEEEE))
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            fontSize   = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            textAlign  = TextAlign.Center,
            color      = Color(0xFF111111)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  "Cases grasses" detection
//  Bold row = when m4=8 has a SMALLER single-machine time than the m4=4 row
//  in the same (m0, m1) group (the paper highlights these as unexpected).
// ═══════════════════════════════════════════════════════════════════════════
private fun detectBoldRows(rows: List<Table46Row>): Set<Int> {
    val boldIndices = mutableSetOf<Int>()
    // Group by (m0, m1), find m4=4 reference time
    data class Key(val m0: String, val m1: String)
    val ref4 = mutableMapOf<Key, Double>()

    rows.forEachIndexed { idx, row ->
        val key = Key(row.m0, row.m1)
        if (row.m4 == "4") {
            ref4[key] = row.s_debut_som.replace(",", ".").toDoubleOrNull() ?: Double.MAX_VALUE
        }
    }
    rows.forEachIndexed { idx, row ->
        val key = Key(row.m0, row.m1)
        if (row.m4 == "8") {
            val t8  = row.s_debut_som.replace(",", ".").toDoubleOrNull() ?: Double.MAX_VALUE
            val t4  = ref4[key] ?: Double.MAX_VALUE
            if (t8 < t4) boldIndices.add(idx)
        }
    }
    return boldIndices
}

// ═══════════════════════════════════════════════════════════════════════════
//  Mapper helpers (Table46Row ↔ Table46RowModel if you still use both)
//  These allow the SimulationDomain to return Table46Row and the UI to use it
//  directly without a separate "model" class.
// ═══════════════════════════════════════════════════════════════════════════

// Convenience: build a Table46Row directly from BatchResultEvent splits
fun buildTable46Row(
    m0: Int, m1: Int, m4: Int,
    somResult: com.example.smartfactorymas.BatchResultEvent?,
    sopResult: com.example.smartfactorymas.BatchResultEvent?
): Table46Row = Table46Row(
    m0 = m0.toString(), m1 = m1.toString(), m4 = m4.toString(),
    s_debut_som  = formatMs(somResult?.splits?.debut?.singleMs),
    s_debut_sop  = formatMs(sopResult?.splits?.debut?.singleMs),
    s_milieu_som = formatMs(somResult?.splits?.milieu?.singleMs),
    s_milieu_sop = formatMs(sopResult?.splits?.milieu?.singleMs),
    s_fin_som    = formatMs(somResult?.splits?.fin?.singleMs),
    s_fin_sop    = formatMs(sopResult?.splits?.fin?.singleMs),
    m_debut_som  = if (m4 == 4) formatMs(somResult?.splits?.debut?.multiMs)  else "--",
    m_debut_sop  = if (m4 == 4) formatMs(sopResult?.splits?.debut?.multiMs)  else "--",
    m_milieu_som = if (m4 == 4) formatMs(somResult?.splits?.milieu?.multiMs) else "--",
    m_milieu_sop = if (m4 == 4) formatMs(sopResult?.splits?.milieu?.multiMs) else "--",
    m_fin_som    = if (m4 == 4) formatMs(somResult?.splits?.fin?.multiMs)    else "--",
    m_fin_sop    = if (m4 == 4) formatMs(sopResult?.splits?.fin?.multiMs)    else "--"
)

// Convenience: build a Table47Row directly from BatchResultEvent stability
fun buildTable47Row(
    m4: Int,
    somResult: com.example.smartfactorymas.BatchResultEvent?,
    sopResult: com.example.smartfactorymas.BatchResultEvent?
): Table47Row = Table47Row(
    m4     = m4.toString(),
    s_s_som = formatPct(somResult?.stability?.single?.stable),
    s_s_sop = formatPct(sopResult?.stability?.single?.stable),
    s_a_som = formatPct(somResult?.stability?.single?.improved),
    s_a_sop = formatPct(sopResult?.stability?.single?.improved),
    s_d_som = formatPct(somResult?.stability?.single?.deteriorated),
    s_d_sop = formatPct(sopResult?.stability?.single?.deteriorated),
    m_s_som = formatPct(somResult?.stability?.multi?.stable),
    m_s_sop = formatPct(sopResult?.stability?.multi?.stable),
    m_a_som = formatPct(somResult?.stability?.multi?.improved),
    m_a_sop = formatPct(sopResult?.stability?.multi?.improved),
    m_d_som = formatPct(somResult?.stability?.multi?.deteriorated),
    m_d_sop = formatPct(sopResult?.stability?.multi?.deteriorated)
)