package com.example.myapplication

import java.util.Calendar
import kotlin.math.pow

object FeatureEngine {
    data class ChallengeConfig(
        val id: ChallengeId,
        val bossType: BossType,
        val health: Double,
        val durationMillis: Long,
        val rewardDebris: Double,
        val rewardPrestige: Int,
        val manualDamageMultiplier: Double,
        val fleetDamageMultiplier: Double,
        val abilityIntervalMillis: Long,
        val prerequisite: ChallengeId? = null
    )

    val challenges = listOf(
        ChallengeConfig(ChallengeId.VOID_LEVIATHAN, BossType.ASTEROID_TITAN, 100_000_000.0, 90_000L, 2_000_000.0, 2, 1.0, 1.0, 12_000L),
        ChallengeConfig(ChallengeId.SOLAR_DEVOURER, BossType.MECHANICAL_COLOSSUS, 500_000_000.0, 75_000L, 15_000_000.0, 5, 1.5, 0.35, 9_000L, ChallengeId.VOID_LEVIATHAN),
        ChallengeConfig(ChallengeId.DREADNOUGHT_EMPRESS, BossType.PIRATE_DREADNOUGHT, 1_000_000_000.0, 60_000L, 100_000_000.0, 10, 0.75, 1.5, 8_000L, ChallengeId.SOLAR_DEVOURER),
        ChallengeConfig(ChallengeId.NEBULA_DRAGON, BossType.MECHANICAL_COLOSSUS, 5_000_000_000.0, 120_000L, 500_000_000.0, 25, 1.0, 1.0, 10_000L, ChallengeId.DREADNOUGHT_EMPRESS)
    )

    fun challenge(id: ChallengeId): ChallengeConfig = challenges.first { it.id == id }

    fun isChallengeUnlocked(state: GameState, id: ChallengeId): Boolean =
        challenge(id).prerequisite?.let(state.completedChallengeIds::contains) ?: true

    fun processBossAbility(state: GameState, now: Long): GameState {
        val battle = state.titanBattle ?: return state
        if (now < battle.nextAbilityAt) return state
        val config = challenge(battle.challengeId)
        return when (battle.challengeId) {
            ChallengeId.VOID_LEVIATHAN -> state.copy(
                titanBattle = battle.copy(shieldCharges = 3, nextAbilityAt = now + config.abilityIntervalMillis, lastAbilityAt = now, abilityUses = battle.abilityUses + 1)
            )
            ChallengeId.SOLAR_DEVOURER -> state.copy(
                titanBattle = battle.copy(
                    health = (battle.health + battle.maxHealth * 0.02).coerceAtMost(battle.maxHealth),
                    shieldCharges = (battle.shieldCharges + 2).coerceAtMost(4),
                    nextAbilityAt = now + config.abilityIntervalMillis,
                    lastAbilityAt = now,
                    abilityUses = battle.abilityUses + 1
                )
            )
            ChallengeId.DREADNOUGHT_EMPRESS -> state.copy(
                titanBattle = summonMinions(battle, 2, 10).copy(nextAbilityAt = now + config.abilityIntervalMillis, lastAbilityAt = now, abilityUses = battle.abilityUses + 1)
            )
            ChallengeId.NEBULA_DRAGON -> {
                val disabledDrone = state.drones.firstOrNull { it.disabledUntil <= now }
                state.copy(
                    titanBattle = summonMinions(battle, 3, 15).copy(
                        health = (battle.health + battle.maxHealth * 0.01).coerceAtMost(battle.maxHealth),
                        nextAbilityAt = now + config.abilityIntervalMillis,
                        lastAbilityAt = now,
                        abilityUses = battle.abilityUses + 1
                    ),
                    drones = if (disabledDrone == null) state.drones else state.drones.map {
                        if (it.id == disabledDrone.id) it.copy(disabledUntil = now + 6_000L) else it
                    }
                )
            }
        }
    }

    private fun summonMinions(battle: TitanBattle, amount: Int, limit: Int): TitanBattle {
        val current = battle.bossMinions
        val toCreate = amount.coerceAtMost((limit - current.size).coerceAtLeast(0))
        val hp = battle.maxHealth * when (battle.challengeId) {
            ChallengeId.DREADNOUGHT_EMPRESS -> 0.004
            ChallengeId.NEBULA_DRAGON -> 0.002
            else -> 0.001
        }
        val firstId = (current.maxOfOrNull(BossMinion::id) ?: -1) + 1
        val summoned = List(toCreate) { index -> BossMinion(firstId + index, hp, hp) }
        return battle.copy(minions = current.size + summoned.size, bossMinions = current + summoned)
    }

    enum class WeeklyAction { CLICK, PASSIVE_INCOME, PURCHASE }
    fun weekKey(now: Long = System.currentTimeMillis()): Long = Calendar.getInstance().run {
        timeInMillis = now
        getWeekYear() * 100L + get(Calendar.WEEK_OF_YEAR)
    }

    fun weeklyFor(now: Long = System.currentTimeMillis()): WeeklyGalaxy {
        val key = weekKey(now)
        val rule = WeeklyRule.entries[(key % WeeklyRule.entries.size).toInt()]
        return WeeklyGalaxy(weekKey = key, rule = rule, target = when (rule) {
            WeeklyRule.CLICKS_ONLY -> 500.0
            WeeklyRule.FRAGILE_DRONES -> 75_000.0
            WeeklyRule.VOLATILE_MARKET -> 30.0
        })
    }

    fun refreshWeekly(state: GameState, now: Long = System.currentTimeMillis()): GameState =
        if (state.weeklyGalaxy.weekKey == weekKey(now)) state
        else state.copy(weeklyGalaxy = weeklyFor(now))

    fun stationCost(module: StationModule, level: Int): Double =
        when (module) {
            StationModule.HANGAR -> 25_000.0
            StationModule.LABORATORY -> 40_000.0
            StationModule.REACTOR -> 60_000.0
            StationModule.TRADE_HUB -> 90_000.0
        } * 3.0.pow(level.coerceAtLeast(0).toDouble())

    fun stationClickMultiplier(state: GameState) = 1.0 + 0.12 * (state.stationLevels[StationModule.LABORATORY] ?: 0)
    fun stationDpsMultiplier(state: GameState) = 1.0 + 0.15 * (state.stationLevels[StationModule.REACTOR] ?: 0)
    fun stationBossMultiplier(state: GameState) = 1.0 + 0.10 * (state.stationLevels[StationModule.HANGAR] ?: 0)
    fun stationRewardMultiplier(state: GameState) = 1.0 + 0.10 * (state.stationLevels[StationModule.TRADE_HUB] ?: 0)

    fun volatilePriceMultiplier(now: Long = System.currentTimeMillis()): Double =
        if ((now / 60_000L) % 2L == 0L) 0.65 else 1.35

    fun advanceWeekly(state: GameState, action: WeeklyAction, amount: Double = 1.0): GameState {
        val galaxy = state.weeklyGalaxy
        if (!galaxy.active || galaxy.rewardClaimed) return state
        val matches = when (galaxy.rule) {
            WeeklyRule.CLICKS_ONLY -> action == WeeklyAction.CLICK
            WeeklyRule.FRAGILE_DRONES -> action == WeeklyAction.PASSIVE_INCOME
            WeeklyRule.VOLATILE_MARKET -> action == WeeklyAction.PURCHASE
        }
        if (!matches || amount <= 0.0 || !amount.isFinite()) return state
        return state.copy(weeklyGalaxy = galaxy.copy(
            progress = (galaxy.progress + amount).coerceAtMost(galaxy.target)
        ))
    }

    fun damageTitan(state: GameState, rawDamage: Double): GameState {
        val battle = state.titanBattle ?: return state
        if (rawDamage <= 0.0 || !rawDamage.isFinite()) return state
        val remaining = battle.health - rawDamage * stationBossMultiplier(state)
        return if (remaining > 0.0) state.copy(titanBattle = battle.copy(health = remaining))
        else state.copy(
            titanBattle = null,
            titanWins = state.titanWins + 1,
            prestigePoints = state.prestigePoints + 1,
            totalDebris = state.totalDebris + battle.maxHealth * 2.0
        )
    }

    fun claimWeeklyReward(state: GameState): GameState {
        val galaxy = state.weeklyGalaxy
        if (galaxy.rewardClaimed || galaxy.progress < galaxy.target) return state
        return state.copy(
            weeklyGalaxy = galaxy.copy(rewardClaimed = true, active = false),
            prestigePoints = state.prestigePoints + 2,
            totalDebris = state.totalDebris + 250_000.0 * stationRewardMultiplier(state)
        )
    }

    fun upgradeStation(state: GameState, module: StationModule): GameState? {
        val level = state.stationLevels[module] ?: 0
        val cost = stationCost(module, level)
        if (level >= 5 || state.totalDebris < cost) return null
        return state.copy(
            totalDebris = state.totalDebris - cost,
            stationLevels = state.stationLevels + (module to level + 1)
        )
    }

    fun createBoss(state: GameState, challengeId: ChallengeId = ChallengeId.VOID_LEVIATHAN, now: Long = System.currentTimeMillis()): TitanBattle {
        val config = challenge(challengeId)
        val hp = config.health
        val battle = TitanBattle(
            type = config.bossType,
            health = hp,
            maxHealth = hp,
            expiresAt = now + config.durationMillis,
            challengeId = challengeId,
            shieldCharges = if (challengeId == ChallengeId.VOID_LEVIATHAN) 3 else 0,
            nextAbilityAt = if (config.abilityIntervalMillis == Long.MAX_VALUE) Long.MAX_VALUE else now + config.abilityIntervalMillis
        )
        return if (challengeId == ChallengeId.NEBULA_DRAGON) summonMinions(battle, 3, 15) else battle
    }

    fun createBoss(state: GameState, now: Long): TitanBattle {
        val type = BossType.entries[(weekKey(now) % BossType.entries.size).toInt()]
        val power = (state.lifetimeStats.clicks / 100L).coerceAtLeast(1L).toDouble()
        val hp = 4_000.0 + power * 1_500.0
        return TitanBattle(type, hp, hp, now + 60_000L)
    }
}
