package com.example.myapplication

object GameResourceRegistry {
    private val droneDrawables = intArrayOf(
        R.drawable.drone_01, R.drawable.drone_02, R.drawable.drone_03,
        R.drawable.drone_04, R.drawable.drone_05, R.drawable.drone_06,
        R.drawable.drone_07, R.drawable.drone_08, R.drawable.drone_09,
        R.drawable.drone_10, R.drawable.drone_11, R.drawable.drone_12,
        R.drawable.drone_13, R.drawable.drone_14, R.drawable.drone_15,
        R.drawable.drone_16, R.drawable.drone_17, R.drawable.drone_18,
        R.drawable.drone_19, R.drawable.drone_20, R.drawable.drone_21,
        R.drawable.drone_22, R.drawable.drone_23, R.drawable.drone_24,
        R.drawable.drone_25, R.drawable.drone_26, R.drawable.drone_27,
        R.drawable.drone_28, R.drawable.drone_29
    )

    private val caseDrawables = intArrayOf(
        R.drawable.case_01, R.drawable.case_02, R.drawable.case_03, R.drawable.case_04,
        R.drawable.case_05, R.drawable.case_06, R.drawable.case_08, R.drawable.case_08
    )

    fun drone(number: Int): Int = droneDrawables.getOrElse(number - 1) {
        R.drawable.upgrade_magnet
    }

    fun caseFrame(frame: Int): Int = caseDrawables.getOrElse(frame - 1) {
        R.drawable.case_08
    }
}
