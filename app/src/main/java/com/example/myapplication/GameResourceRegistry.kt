package com.example.myapplication

object GameResourceRegistry {
    private val droneDrawables = intArrayOf(
        R.drawable.drone_01_v2, R.drawable.drone_02_v2, R.drawable.drone_03_v2,
        R.drawable.drone_04_v2, R.drawable.drone_05_v2, R.drawable.drone_06_v2,
        R.drawable.drone_07_v2, R.drawable.drone_08_v2, R.drawable.drone_09_v2,
        R.drawable.drone_10_v2, R.drawable.drone_11_v2, R.drawable.drone_12_v2,
        R.drawable.drone_13_v2, R.drawable.drone_14_v2, R.drawable.drone_15_v2,
        R.drawable.drone_16_v2, R.drawable.drone_17_v2, R.drawable.drone_18_v2,
        R.drawable.drone_19_v2, R.drawable.drone_20_v2, R.drawable.drone_21_v2,
        R.drawable.drone_22_v2, R.drawable.drone_23_v2, R.drawable.drone_24_v2,
        R.drawable.drone_25_v2, R.drawable.drone_26_v2, R.drawable.drone_27_v2,
        R.drawable.drone_28_v2, R.drawable.drone_29_v2
    )

    private val caseDrawables = intArrayOf(
        R.drawable.case_01, R.drawable.case_02, R.drawable.case_03, R.drawable.case_04,
        R.drawable.case_05, R.drawable.case_06, R.drawable.case_08, R.drawable.case_08
    )
    private val commonCaseDrawables = intArrayOf(
        R.drawable.case_common_1, R.drawable.case_common_2,
        R.drawable.case_common_3, R.drawable.case_common_4,
        R.drawable.case_common_5, R.drawable.case_common_6,
        R.drawable.case_common_7, R.drawable.case_common_8
    )
    private val rareCaseDrawables = intArrayOf(
        R.drawable.case_rare_1, R.drawable.case_rare_2,
        R.drawable.case_rare_3, R.drawable.case_rare_4,
        R.drawable.case_rare_5, R.drawable.case_rare_6,
        R.drawable.case_rare_7, R.drawable.case_rare_8
    )
    private val legendaryCaseDrawables = intArrayOf(
        R.drawable.case_legendary_1, R.drawable.case_legendary_2,
        R.drawable.case_legendary_3, R.drawable.case_legendary_4,
        R.drawable.case_legendary_5, R.drawable.case_legendary_6,
        R.drawable.case_legendary_7, R.drawable.case_legendary_8
    )

    fun drone(number: Int): Int = droneDrawables.getOrElse(number - 1) {
        R.drawable.upgrade_magnet
    }

    fun caseFrame(frame: Int): Int = caseDrawables.getOrElse(frame - 1) {
        R.drawable.case_08
    }

    fun caseFrame(type: CaseType, frame: Int): Int {
        val frames = when (type) {
            CaseType.COMMON -> commonCaseDrawables
            CaseType.RARE -> rareCaseDrawables
            CaseType.LEGENDARY -> legendaryCaseDrawables
        }
        return frames.getOrElse(frame.coerceIn(1, frames.size) - 1) { frames.last() }
    }
}
