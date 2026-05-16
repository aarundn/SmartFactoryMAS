package com.example.smartfactorymas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import kotlinx.serialization.Serializable


// ==============================================================================
// Tableau 4.6: Reactivity (Computation Time)
// ==============================================================================
@Composable
fun FullTableau46(rows: List<Table46RowModel>) {
    val borderColor = Color(0xFFCCCCCC)
    val headerBg = Color(0xFFF0F0F0)

    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).border(1.dp, borderColor)) {
        Text("Tableau 4.6. Influence du nombre de jobs, machines et ressources sur la résolution", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))

        Row(modifier = Modifier.background(headerBg)) {
            Column { Row { HeaderCell("m0", 40.dp, 90.dp); HeaderCell("m1", 50.dp, 90.dp); HeaderCell("m4", 40.dp, 90.dp) } }
            Column {
                HeaderCell("Niveau une-machine (ms)", 360.dp, 30.dp)
                Row { HeaderCell("Début", 120.dp); HeaderCell("Milieu", 120.dp); HeaderCell("Fin", 120.dp) }
                Row { repeat(3) { HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp) } }
            }
            Column {
                HeaderCell("Niveau Multi-machines (ms)", 360.dp, 30.dp)
                Row { HeaderCell("Début", 120.dp); HeaderCell("Milieu", 120.dp); HeaderCell("Fin", 120.dp) }
                Row { repeat(3) { HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp) } }
            }
        }

        // Variables to track previous row values for grouping
        var lastM0 = ""
        var lastM1 = ""
        var lastSingleTime = Double.MAX_VALUE // To track when to bold a cell

        rows.forEachIndexed { i, r ->
            val isEven = i % 2 == 0

            // 🌟 FIX 1: Grouping (Blank out repeated m0 and m1 values to match paper)
            val displayM0 = if (r.m0 == lastM0) "" else r.m0
            val displayM1 = if (r.m0 == lastM0 && r.m1 == lastM1) "" else r.m1

            lastM0 = r.m0
            lastM1 = r.m1

            // 🌟 FIX 2: "Les cases grasses" (Bold if this row is unexpectedly faster than the previous row)
            val currentSingleTime = r.s_debut_som.toDoubleOrNull() ?: 0.0
            val isAnomaly = (r.m4 == "8" || r.m4 == "4") && (currentSingleTime < lastSingleTime)
            if (r.m4 == "2" || r.m4 == "--") lastSingleTime = currentSingleTime // reset baseline for new block
            else lastSingleTime = currentSingleTime

            Row(modifier = Modifier.background(if (isEven) Color.White else Color(0xFFFAFAFA))) {

                // Print the left columns (Using the grouped display variables)
                DataCell(displayM0, 40.dp, isBold = true)
                DataCell(displayM1, 50.dp, isBold = true)
                DataCell(r.m4, 40.dp, isBold = true)

                // Print Single Machine (Apply bolding if it's a "case grasse")
                DataCell(r.s_debut_som, 60.dp, isBold = isAnomaly)
                DataCell(r.s_debut_som, 60.dp, isBold = isAnomaly)
                DataCell(r.s_milieu_som, 60.dp)
                DataCell(r.s_milieu_sop, 60.dp)
                DataCell(r.s_fin_som, 60.dp)
                DataCell(r.s_fin_sop, 60.dp)

                // Print Multi Machine (Visually hide data if m4 is 2 or 8, matching the paper)
                if (r.m4 == "4" || r.m4 == "--") {
                    DataCell(r.m_debut_som, 60.dp); DataCell(r.m_debut_sop, 60.dp)
                    DataCell(r.m_milieu_som, 60.dp); DataCell(r.m_milieu_sop, 60.dp)
                    DataCell(r.m_fin_som, 60.dp); DataCell(r.m_fin_sop, 60.dp)
                } else {
                    repeat(6) { DataCell("", 60.dp) }
                }
            }
        }
    }
}

// ==============================================================================
// Tableau 4.7: Stability (Capacité d'absorption)
// ==============================================================================
@Composable
fun FullTableau47(rows: List<Table47RowModel>) {
    val borderColor = Color(0xFFCCCCCC)
    val headerBg = Color(0xFFF0F0F0)

    Column(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).border(1.dp, borderColor)) {
        Text(
            "Tableau 4.7. Le retard résultant des résolutions une-machine et multi-machines selon la stratégie",
            fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(16.dp)
        )

        // --- THE NESTED HEADERS ---
        Row(modifier = Modifier.background(headerBg)) {
            HeaderCell("m4", width = 50.dp, height = 90.dp)

            Column {
                HeaderCell("Le retard une-machine", width = 360.dp, height = 30.dp)
                Row {
                    HeaderCell("Stable", 120.dp, 30.dp); HeaderCell("Amélioré", 120.dp, 30.dp); HeaderCell("Détérioré", 120.dp, 30.dp)
                }
                Row {
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                }
            }

            Column {
                HeaderCell("Le retard multi-machines", width = 360.dp, height = 30.dp)
                Row {
                    HeaderCell("Stable", 120.dp, 30.dp); HeaderCell("Amélioré", 120.dp, 30.dp); HeaderCell("Détérioré", 120.dp, 30.dp)
                }
                Row {
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                    HeaderCell("SOM", 60.dp); HeaderCell("SOP", 60.dp)
                }
            }
        }

        // --- DATA ROWS ---
        rows.forEachIndexed { index, row ->
            val isEven = index % 2 == 0
            Row(modifier = Modifier.background(if (isEven) Color.White else Color(0xFFFAFAFA))) {
                DataCell(row.m4, 50.dp, isBold = true)

                DataCell(row.s_stable_som, 60.dp); DataCell(row.s_stable_sop, 60.dp)
                DataCell(row.s_ameliore_som, 60.dp); DataCell(row.s_ameliore_sop, 60.dp)
                DataCell(row.s_deteriore_som, 60.dp); DataCell(row.s_deteriore_sop, 60.dp)

                DataCell(row.m_stable_som, 60.dp); DataCell(row.m_stable_sop, 60.dp)
                DataCell(row.m_ameliore_som, 60.dp); DataCell(row.m_ameliore_sop, 60.dp)
                DataCell(row.m_deteriore_som, 60.dp); DataCell(row.m_deteriore_sop, 60.dp)
            }
        }
    }
}

// --- Cell Helpers ---
@Composable
fun HeaderCell(text: String, width: Dp, height: Dp = 30.dp) {
    Box(modifier = Modifier.width(width).height(height).border(0.5.dp, Color(0xFFE0E0E0)).padding(2.dp), contentAlignment = Alignment.Center) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, textAlign = TextAlign.Center, color = Color(0xFF333333))
    }
}

@Composable
fun DataCell(text: String, width: Dp, isBold: Boolean = false) {
    Box(modifier = Modifier.width(width).height(30.dp).border(0.5.dp, Color(0xFFEEEEEE)).padding(2.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 11.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, color = Color(0xFF111111))
    }
}

// Data class for a single row in Tableau 4.6

data class Table46RowModel(
    val m0: String, val m1: String, val m4: String,
    val s_debut_som: String, val s_debut_sop: String,
    val s_milieu_som: String, val s_milieu_sop: String,
    val s_fin_som: String, val s_fin_sop: String,
    val m_debut_som: String, val m_debut_sop: String,
    val m_milieu_som: String, val m_milieu_sop: String,
    val m_fin_som: String, val m_fin_sop: String
)

// Data class for a single row in Tableau 4.7



data class Table47RowModel(
    val m4: String,
    val s_stable_som: String, val s_stable_sop: String,
    val s_ameliore_som: String, val s_ameliore_sop: String,
    val s_deteriore_som: String, val s_deteriore_sop: String,
    val m_stable_som: String, val m_stable_sop: String,
    val m_ameliore_som: String, val m_ameliore_sop: String,
    val m_deteriore_som: String, val m_deteriore_sop: String
)


fun Table46Row.toModel(): Table46RowModel {
    return Table46RowModel(
        m0 = this.m0,
        m1 = this.m1,
        m4 = this.m4,
        s_debut_som = this.s_debut_som,
        s_debut_sop = this.s_debut_sop,
        s_milieu_som = this.s_milieu_som,
        s_milieu_sop = this.s_milieu_sop,
        s_fin_som = this.s_fin_som,
        s_fin_sop = this.s_fin_sop,
        m_debut_som = this.m_debut_som,
        m_debut_sop = this.m_debut_sop,
        m_milieu_som = this.m_milieu_som,
        m_milieu_sop = this.m_milieu_sop,
        m_fin_som = this.m_fin_som,
        m_fin_sop = this.m_fin_sop
    )
}

// List mapper for convenience
fun List<Table46Row>.toModelList1(): List<Table46RowModel> {
    return this.map { it.toModel() }
}

// ==========================================
// Mappers for Tableau 4.7
// ==========================================

fun Table47Row.toModel(): Table47RowModel {
    return Table47RowModel(
        m4 = this.m4,
        // Mapping the shortened JSON keys to the descriptive UI Model keys
        s_stable_som = this.s_s_som,
        s_stable_sop = this.s_s_sop,
        s_ameliore_som = this.s_a_som,
        s_ameliore_sop = this.s_a_sop,
        s_deteriore_som = this.s_d_som,
        s_deteriore_sop = this.s_d_sop,

        m_stable_som = this.m_s_som,
        m_stable_sop = this.m_s_sop,
        m_ameliore_som = this.m_a_som,
        m_ameliore_sop = this.m_a_sop,
        m_deteriore_som = this.m_d_som,
        m_deteriore_sop = this.m_d_sop
    )
}

// List mapper for convenience
fun List<Table47Row>.toModelList(): List<Table47RowModel> {
    return this.map { it.toModel() }
}
// ============================================================
//  Table Data Generators (Empty States & Helpers)
// ============================================================
fun getEmptyTable46Data(): List<Table46Row> {
    val rows = mutableListOf<Table46Row>()
    val m0List = listOf(5, 10, 20)
    val m1List = listOf(20, 50, 100)
    val m4List = listOf(2, 4, 8)

    for (m0 in m0List) {
        for (m1 in m1List) {
            for (m4 in m4List) {
                rows.add(
                    Table46Row(
                        m0.toString(), m1.toString(), m4.toString(),
                        "--", "--", "--", "--", "--", "--",
                        "--", "--", "--", "--", "--", "--"
                    )
                )
            }
        }
    }
    return rows
}

fun getEmptyTable47Data(): List<Table47Row> {
    return listOf("2", "4", "8").map { m4 ->
        Table47Row(m4, "--", "--", "--", "--", "--", "--", "--", "--", "--", "--", "--", "--")
    }
}


fun formatMs(value: Double?): String = if (value != null && value > 0.0) "%.2f".format(value) else "--"
fun formatPct(value: Double?): String = if (value != null) "%.2f".format(value) else "--"