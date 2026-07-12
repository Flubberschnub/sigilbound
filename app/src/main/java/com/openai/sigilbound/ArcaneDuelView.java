package com.openai.sigilbound;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Sigilbound is intentionally built as one self-contained custom View.
 * Version 1.3 adds visible rival scribing, compositional clauses, configurable tempo, executable decks, and a legible mobile type system.
 * The game uses only Android Canvas primitives, making the APK small and the
 * source easy to study or extend without a third-party engine.
 */
public class ArcaneDuelView extends View {
    private static final int GOLD = Color.rgb(232, 199, 103);
    private static final int PALE_GOLD = Color.rgb(255, 235, 170);
    private static final int INK = Color.rgb(10, 9, 20);
    private static final int PANEL = Color.rgb(25, 21, 43);
    private static final int PANEL_LIGHT = Color.rgb(42, 35, 66);
    private static final int MUTED = Color.rgb(162, 153, 178);
    private static final int WHITE = Color.rgb(245, 241, 232);
    private static final int RED = Color.rgb(222, 69, 75);
    private static final int BLUE = Color.rgb(66, 165, 226);

    // Use Android's plain system sans family throughout. Medium weight replaces
    // synthetic bold so small mobile labels keep open counters and clean edges.
    private static final android.graphics.Typeface UI_REGULAR =
            android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL);
    private static final android.graphics.Typeface UI_MEDIUM =
            android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL);

    private enum Screen { TITLE, GRIMOIRE, TUTORIAL, DUEL, GAME_OVER }
    private enum Element {
        FIRE("Fire", Color.rgb(245, 83, 55)),
        WATER("Water", Color.rgb(70, 190, 220)),
        WIND("Wind", Color.rgb(132, 229, 170)),
        STONE("Stone", Color.rgb(219, 170, 84));
        final String label;
        final int color;
        Element(String label, int color) { this.label = label; this.color = color; }
    }
    private enum Rune {
        LANCE("LANCE", "swift projectile", 12),
        WARD("WARD", "blocking construct", 15),
        ORBIT("ORBIT", "repeating familiar", 18),
        BURST("BURST", "delayed field spell", 20);
        final String label;
        final String subtitle;
        final float baseCost;
        Rune(String label, String subtitle, float baseCost) {
            this.label = label; this.subtitle = subtitle; this.baseCost = baseCost;
        }
    }
    private enum Artifact {
        PRISM_LENS("PRISM LENS", "cheaper compound syntax"),
        ASHEN_QUILL("ASHEN QUILL", "perfect glyphs surge"),
        AEGIS_BELL("AEGIS BELL", "stronger constructs");
        final String label;
        final String subtitle;
        Artifact(String label, String subtitle) { this.label = label; this.subtitle = subtitle; }
    }
    private enum Difficulty {
        APPRENTICE("APPRENTICE", "slow • forgiving"),
        ADEPT("ADEPT", "balanced rival"),
        ARCHMAGE("ARCHMAGE", "fast • reactive");
        final String label;
        final String subtitle;
        Difficulty(String label, String subtitle) { this.label = label; this.subtitle = subtitle; }
    }

    private enum Clause {
        ECHO("ECHO", "repeat later"),
        FORK("FORK", "branch lanes"),
        ANCHOR("ANCHOR", "slow + persist");
        final String label;
        final String subtitle;
        Clause(String label, String subtitle) { this.label = label; this.subtitle = subtitle; }
    }
    private enum Tempo {
        RITUAL("RITUAL", "slow • readable", 0.42f, 1.32f),
        DUEL("DUEL", "measured", 0.62f, 1.08f),
        BLITZ("BLITZ", "fast field", 0.84f, 0.90f);
        final String label;
        final String subtitle;
        final float projectileScale;
        final float castTimeScale;
        Tempo(String label, String subtitle, float projectileScale, float castTimeScale) {
            this.label = label; this.subtitle = subtitle; this.projectileScale = projectileScale; this.castTimeScale = castTimeScale;
        }
    }
    private enum Kind { PROJECTILE, WARD, ORBIT, BURST, CLOUD, HAZARD }

    private static final int T_INTRO = 0;
    private static final int T_ELEMENTS = 1;
    private static final int T_RUNES = 2;
    private static final int T_FIRE_DRAW = 3;
    private static final int T_FIRE_CAST = 4;
    private static final int T_WATER_DRAW = 5;
    private static final int T_WATER_CAST = 6;
    private static final int T_WIND_DRAW = 7;
    private static final int T_WIND_CAST = 8;
    private static final int T_STONE_DRAW = 9;
    private static final int T_STONE_CAST = 10;
    private static final int T_COMPOUND_FIRE = 11;
    private static final int T_COMPOUND_WIND = 12;
    private static final int T_COMPOUND_CAST = 13;
    private static final int T_DEFENSE_INTRO = 14;
    private static final int T_DEFENSE_DRAW = 15;
    private static final int T_DEFENSE_CAST = 16;
    private static final int T_DEFENSE_WATCH = 17;
    private static final int T_FINAL_DUEL = 18;
    private static final int T_COMPLETE = 19;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final GlyphRecognizer recognizer = new GlyphRecognizer();
    private final ArrayList<PointF> currentStroke = new ArrayList<PointF>();
    private final ArrayList<Element> formula = new ArrayList<Element>();
    private final ArrayList<Clause> selectedClauses = new ArrayList<Clause>();
    private final SpellProgram[] deck = new SpellProgram[3];
    private final float[] executableCooldown = new float[]{0f, 0f, 0f};
    private final RectF[] clauseRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final RectF[] executableRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final ArrayList<SpellEntity> entities = new ArrayList<SpellEntity>();
    private final ArrayList<FloatingText> floatingTexts = new ArrayList<FloatingText>();
    private final ArrayList<PendingCast> pendingCasts = new ArrayList<PendingCast>();
    private final RectF[] runeRects = new RectF[]{new RectF(), new RectF(), new RectF(), new RectF()};
    private final RectF[] laneRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final RectF drawingPad = new RectF();
    private final RectF clearButton = new RectF();
    private final RectF[] artifactRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final RectF[] difficultyRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final RectF[] tempoRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final RectF grimoireButton = new RectF();
    private final RectF[] grimoireSlotRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final RectF[] editorElementRects = new RectF[]{new RectF(), new RectF(), new RectF(), new RectF()};
    private final RectF[] editorRuneRects = new RectF[]{new RectF(), new RectF(), new RectF(), new RectF()};
    private final RectF[] editorClauseRects = new RectF[]{new RectF(), new RectF(), new RectF()};
    private final RectF editorClearButton = new RectF();
    private final RectF editorSaveButton = new RectF();
    private final RectF editorBackButton = new RectF();
    private final RectF mainButton = new RectF();
    private final RectF secondaryButton = new RectF();
    private final SharedPreferences preferences;

    private Screen screen = Screen.TITLE;
    private boolean tutorialComplete;
    private boolean running = true;
    private boolean drawing;
    private long lastFrameNanos;
    private long formulaStartedAt;
    private float scale = 1f;
    private float density = 1f;
    private float scaledDensity = 1f;
    private float fieldTop;
    private float fieldBottom;
    private float castTop;
    private float[] laneX = new float[3];
    private Shader backgroundShader;

    private float playerHealth;
    private float enemyHealth;
    private float playerMana;
    private float enemyMana;
    private float playerResonance;
    private float enemyResonance;
    private float playerInk;
    private float playerSlow;
    private float enemySlow;
    private float playerBurn;
    private float enemyBurn;
    private float aiCooldown;
    private float duelClock;
    private int lastStrokeLane = 1;
    private int selectedLane = -1;
    private float lastGlyphQuality = 0.8f;
    private String banner = "";
    private float bannerTimer;
    private String resultTitle = "";
    private String resultSubtitle = "";
    private int tutorialStep;
    private float tutorialDelay;
    private int sigilsCast;
    private int reactionsWon;
    private int damageDealt;
    private Artifact selectedArtifact;
    private Difficulty selectedDifficulty;
    private Tempo selectedTempo;
    private SpellEntity tutorialThreat;
    private EnemyCastIntent enemyIntent;
    private int armedExecutable = -1;
    private int editingSlot = 0;
    private SpellProgram editorProgram;

    public ArcaneDuelView(Context context) {
        super(context);
        setFocusable(true);
        setKeepScreenOn(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        preferences = context.getSharedPreferences("sigilbound", Context.MODE_PRIVATE);
        tutorialComplete = preferences.getBoolean("tutorial_complete", false);
        int artifactIndex = preferences.getInt("selected_artifact", 0);
        selectedArtifact = Artifact.values()[Math.max(0, Math.min(Artifact.values().length - 1, artifactIndex))];
        int difficultyIndex = preferences.getInt("selected_difficulty", 0);
        selectedDifficulty = Difficulty.values()[Math.max(0, Math.min(Difficulty.values().length - 1, difficultyIndex))];
        int tempoIndex = preferences.getInt("selected_tempo", 0);
        selectedTempo = Tempo.values()[Math.max(0, Math.min(Tempo.values().length - 1, tempoIndex))];
        loadDeck();
        editorProgram = deck[0].copy();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        resetCombat();
    }

    private void loadDeck() {
        SpellProgram[] defaults = new SpellProgram[]{
                program(new Element[]{Element.FIRE, Element.WIND}, Rune.LANCE, new Clause[]{Clause.ECHO}),
                program(new Element[]{Element.WATER, Element.STONE}, Rune.WARD, new Clause[]{Clause.FORK, Clause.ANCHOR}),
                program(new Element[]{Element.STONE, Element.WIND, Element.FIRE}, Rune.BURST, new Clause[]{Clause.FORK})
        };
        for (int i = 0; i < deck.length; i++) {
            String encoded = preferences.getString("deck_" + i, "");
            deck[i] = encoded == null || encoded.length() == 0 ? defaults[i] : parseProgram(encoded, defaults[i]);
        }
    }

    private SpellProgram program(Element[] elements, Rune rune, Clause[] clauses) {
        SpellProgram p = new SpellProgram();
        Collections.addAll(p.elements, elements); p.rune = rune; Collections.addAll(p.clauses, clauses);
        return p;
    }

    private SpellProgram parseProgram(String encoded, SpellProgram fallback) {
        try {
            String[] parts = encoded.split("\\|", -1);
            SpellProgram p = new SpellProgram();
            if (parts.length > 0 && parts[0].length() > 0)
                for (String token : parts[0].split(",")) if (p.elements.size() < 3) p.elements.add(Element.valueOf(token));
            p.rune = parts.length > 1 ? Rune.valueOf(parts[1]) : Rune.LANCE;
            if (parts.length > 2 && parts[2].length() > 0)
                for (String token : parts[2].split(",")) if (p.clauses.size() < 2) p.clauses.add(Clause.valueOf(token));
            if (p.elements.isEmpty()) return fallback.copy();
            return p;
        } catch (Exception ignored) { return fallback.copy(); }
    }

    private String serializeProgram(SpellProgram p) {
        StringBuilder e = new StringBuilder();
        for (Element element : p.elements) { if (e.length() > 0) e.append(','); e.append(element.name()); }
        StringBuilder c = new StringBuilder();
        for (Clause clause : p.clauses) { if (c.length() > 0) c.append(','); c.append(clause.name()); }
        return e.toString() + "|" + p.rune.name() + "|" + c.toString();
    }

    private void saveDeckSlot(int slot) {
        if (editorProgram.elements.isEmpty()) { showBanner("A program needs at least one element.", 1.3f); return; }
        deck[slot] = editorProgram.copy();
        preferences.edit().putString("deck_" + slot, serializeProgram(deck[slot])).apply();
        showBanner("EXECUTABLE " + (slot + 1) + " COMPILED", 1.2f);
    }

    public void resume() {
        running = true;
        lastFrameNanos = 0L;
        postInvalidateOnAnimation();
    }

    public void pause() {
        running = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        scale = w / 1080f;

        // The battlefield remains proportional, while the control surface uses
        // density-independent measurements so it stays usable on real phones.
        fieldTop = Math.max(185f * scale, dp(102f));
        float castingHeight = clamp(h * 0.62f, dp(470f), dp(610f));
        castTop = h - castingHeight;
        fieldBottom = castTop - dp(64f);
        if (fieldBottom < fieldTop + dp(150f)) fieldBottom = fieldTop + dp(150f);

        laneX[0] = w * 0.22f;
        laneX[1] = w * 0.50f;
        laneX[2] = w * 0.78f;

        float side = dp(14f);
        float gap = dp(8f);
        float executableTop = castTop + dp(50f);
        float executableBottom = executableTop + dp(48f);
        float runeBottom = h - dp(12f);
        float runeTop = runeBottom - dp(74f);
        float laneBottom = runeTop - dp(8f);
        float laneTop = laneBottom - dp(48f);
        float clauseBottom = laneTop - dp(8f);
        float clauseTop = clauseBottom - dp(42f);
        float padTop = executableBottom + dp(8f);
        float padBottom = clauseTop - dp(8f);
        drawingPad.set(side, padTop, w - side, padBottom);

        float execW = (w - side * 2f - gap * 2f) / 3f;
        for (int i = 0; i < 3; i++) {
            executableRects[i].set(side + i * (execW + gap), executableTop,
                    side + i * (execW + gap) + execW, executableBottom);
        }

        float clauseW = (w - side * 2f - gap * 2f) / 3f;
        for (int i = 0; i < 3; i++) {
            clauseRects[i].set(side + i * (clauseW + gap), clauseTop,
                    side + i * (clauseW + gap) + clauseW, clauseBottom);
        }

        float laneW = (w - side * 2f - gap * 2f) / 3f;
        for (int i = 0; i < 3; i++) {
            laneRects[i].set(side + i * (laneW + gap), laneTop,
                    side + i * (laneW + gap) + laneW, laneBottom);
        }

        float buttonW = (w - side * 2f - gap * 3f) / 4f;
        for (int i = 0; i < 4; i++) {
            runeRects[i].set(side + i * (buttonW + gap), runeTop,
                    side + i * (buttonW + gap) + buttonW, runeBottom);
        }
        clearButton.set(w - dp(88f), castTop + dp(7f), w - dp(12f), castTop + dp(45f));

        backgroundShader = new LinearGradient(0, 0, 0, h,
                new int[]{Color.rgb(9, 8, 20), Color.rgb(24, 18, 44), Color.rgb(9, 9, 20)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0f : Math.min(0.035f, (now - lastFrameNanos) / 1000000000f);
        lastFrameNanos = now;
        if (running) update(dt);

        drawBackground(canvas);
        if (screen == Screen.TITLE) {
            drawTitle(canvas);
        } else if (screen == Screen.GRIMOIRE) {
            drawGrimoire(canvas);
        } else {
            drawArena(canvas);
            drawHud(canvas);
            drawCastingInterface(canvas);
            drawFloatingText(canvas);
            if (screen == Screen.TUTORIAL) drawTutorial(canvas);
            if (screen == Screen.GAME_OVER) drawGameOver(canvas);
        }
        if (running) postInvalidateOnAnimation();
    }

    private void update(float dt) {
        if (bannerTimer > 0f) bannerTimer -= dt;
        if (tutorialDelay > 0f) tutorialDelay -= dt;
        for (Iterator<FloatingText> it = floatingTexts.iterator(); it.hasNext();) {
            FloatingText f = it.next();
            f.life -= dt;
            f.y -= 38f * scale * dt;
            if (f.life <= 0f) it.remove();
        }
        boolean activeTutorial = screen == Screen.TUTORIAL
                && tutorialStep >= T_FIRE_DRAW && tutorialStep <= T_FINAL_DUEL;
        if (screen != Screen.DUEL && !activeTutorial) return;

        duelClock += dt;
        for (int i = 0; i < executableCooldown.length; i++) executableCooldown[i] = Math.max(0f, executableCooldown[i] - dt);
        updatePendingCasts(dt);
        playerMana = Math.min(100f, playerMana + 11.5f * dt);
        enemyMana = Math.min(100f, enemyMana + aiManaRegen() * dt);
        playerResonance = Math.max(0f, playerResonance - 1.0f * dt);
        enemyResonance = Math.max(0f, enemyResonance - 0.7f * dt);
        playerSlow = Math.max(0f, playerSlow - dt);
        enemySlow = Math.max(0f, enemySlow - dt);
        if (playerBurn > 0f) {
            playerBurn -= dt;
            playerHealth -= 2.2f * dt;
        }
        if (enemyBurn > 0f) {
            enemyBurn -= dt;
            enemyHealth -= 2.2f * dt;
        }

        // Prepared manual syntax remains stable until cast or cleared. Readability
        // and strategic lane juggling are more important than a hidden fizzle timer.

        updateEntities(dt);

        if (screen == Screen.TUTORIAL && tutorialStep == T_DEFENSE_WATCH) {
            boolean threatGone = tutorialThreat == null || tutorialThreat.dead || !entities.contains(tutorialThreat);
            if (threatGone) {
                tutorialStep = T_FINAL_DUEL;
                tutorialThreat = null;
                beginTutorialFight();
                return;
            }
        }

        boolean aiEnabled = screen == Screen.DUEL
                || (screen == Screen.TUTORIAL && tutorialStep == T_FINAL_DUEL);
        if (aiEnabled) {
            if (enemyIntent != null) {
                updateEnemyIntent(dt);
            } else {
                aiCooldown -= dt;
                if (aiCooldown <= 0f) castAiSpell();
            }
        }

        if (playerHealth <= 0f || enemyHealth <= 0f) {
            playerHealth = Math.max(0f, playerHealth);
            enemyHealth = Math.max(0f, enemyHealth);
            if (screen == Screen.TUTORIAL && tutorialStep == T_FINAL_DUEL && enemyHealth <= 0f) {
                tutorialStep = T_COMPLETE;
                entities.clear();
                tutorialThreat = null;
                preferences.edit().putBoolean("tutorial_complete", true).apply();
                tutorialComplete = true;
            } else if (screen == Screen.TUTORIAL && tutorialStep == T_FINAL_DUEL && playerHealth <= 0f) {
                showBanner("The master restores the practice circle.", 2f);
                beginTutorialFight();
            } else if (screen == Screen.DUEL) {
                finishDuel(enemyHealth <= 0f);
            }
        }
    }

    private void updatePendingCasts(float dt) {
        ArrayList<PendingCast> ready = new ArrayList<PendingCast>();
        for (Iterator<PendingCast> it = pendingCasts.iterator(); it.hasNext();) {
            PendingCast p = it.next();
            p.delay -= dt;
            if (p.delay <= 0f) { ready.add(p); it.remove(); }
        }
        for (PendingCast p : ready) {
            deploySpell(p.owner, p.program.elements, p.program.rune, p.lane, p.quality,
                    p.empowered, p.program.clauses, p.powerScale);
        }
    }

    private Difficulty effectiveDifficulty() {
        return screen == Screen.TUTORIAL ? Difficulty.APPRENTICE : selectedDifficulty;
    }

    private float aiManaRegen() {
        Difficulty d = effectiveDifficulty();
        if (d == Difficulty.APPRENTICE) return 8.2f;
        if (d == Difficulty.ADEPT) return 10.2f;
        return 12.2f;
    }

    private float initialAiDelay() {
        Difficulty d = effectiveDifficulty();
        if (screen == Screen.TUTORIAL) return 7.5f;
        if (d == Difficulty.APPRENTICE) return 8.0f;
        if (d == Difficulty.ADEPT) return 5.0f;
        return 3.2f;
    }

    private float nextAiCooldown() {
        Difficulty d = effectiveDifficulty();
        if (d == Difficulty.APPRENTICE) return 4.8f + random.nextFloat() * 1.8f;
        if (d == Difficulty.ADEPT) return 3.0f + random.nextFloat() * 1.3f;
        return 1.9f + random.nextFloat() * 0.9f;
    }

    private void updateEntities(float dt) {
        ArrayList<SpellEntity> additions = new ArrayList<SpellEntity>();
        for (SpellEntity e : entities) {
            if (e.dead) continue;
            e.age += dt;
            e.timer -= dt;
            if (e.kind == Kind.PROJECTILE) {
                float slowFactor = e.owner == 0 ? (playerSlow > 0f ? 0.82f : 1f) : (enemySlow > 0f ? 0.82f : 1f);
                float zoneSlow = 1f;
                for (SpellEntity z : entities) {
                    if (!z.dead && (z.kind == Kind.CLOUD || z.kind == Kind.HAZARD)
                            && z.lane == e.lane && Math.abs(z.y - e.y) < z.radius) {
                        if (z.kind == Kind.CLOUD) {
                            zoneSlow *= Math.max(0.55f, 0.86f - z.moisture * 0.10f);
                            e.damage = Math.max(3f, e.damage - dt * (1.0f + z.moisture * 0.55f));
                            e.heat = Math.max(0f, e.heat - dt * z.moisture * 0.18f);
                        } else {
                            zoneSlow *= Math.max(0.48f, 0.82f - z.mass * 0.10f);
                            if (z.heat > 0.5f && e.heat > 0.3f) e.damage += dt * 0.45f;
                        }
                    }
                }
                e.y += e.velocity * slowFactor * zoneSlow * dt;
                if (e.fracture > 0.25f && !e.split &&
                        ((e.owner == 0 && e.y < (fieldTop + fieldBottom) * 0.5f)
                                || (e.owner == 1 && e.y > (fieldTop + fieldBottom) * 0.5f))) {
                    e.split = true;
                    for (int direction : new int[]{-1, 1}) {
                        int newLane = e.lane + direction;
                        if (newLane >= 0 && newLane < 3) {
                            SpellEntity shard = e.copyProjectile();
                            shard.lane = newLane;
                            shard.x = laneX[newLane];
                            shard.damage *= 0.58f;
                            shard.radius *= 0.72f;
                            shard.split = true;
                            additions.add(shard);
                        }
                    }
                    e.damage *= 0.72f;
                }
            } else if (e.kind == Kind.ORBIT) {
                if (e.timer <= 0f) {
                    e.timer = 1.42f;
                    SpellEntity bolt = createProjectile(e.owner, e.lane, e.primary, e.secondary,
                            Math.max(4f, e.damage * 0.52f), 0.88f, e.empowered);
                    copySpellProfile(e, bolt);
                    bolt.velocity *= (0.82f + e.impulse * 0.22f) * selectedTempo.projectileScale;
                    bolt.damage *= 0.90f + e.heat * 0.15f + e.mass * 0.08f;
                    bolt.y = e.y + (e.owner == 0 ? -e.radius : e.radius);
                    additions.add(bolt);
                }
                e.life -= dt;
                if (e.life <= 0f || e.hp <= 0f) e.dead = true;
            } else if (e.kind == Kind.WARD) {
                e.life -= dt;
                if (e.life <= 0f || e.hp <= 0f) e.dead = true;
            } else if (e.kind == Kind.BURST) {
                if (e.timer <= 0f) detonateBurst(e, additions);
            } else if (e.kind == Kind.CLOUD || e.kind == Kind.HAZARD) {
                e.life -= dt;
                if (e.kind == Kind.HAZARD) {
                    for (SpellEntity target : entities) {
                        if (!target.dead && target.owner != e.owner && target.lane == e.lane
                                && (target.kind == Kind.WARD || target.kind == Kind.ORBIT)
                                && Math.abs(target.y - e.y) < e.radius) {
                            target.hp -= 3.8f * dt;
                        }
                    }
                }
                if (e.life <= 0f) e.dead = true;
            }
        }

        // Projectile versus construct and caster collisions.
        for (SpellEntity p : entities) {
            if (p.dead || p.kind != Kind.PROJECTILE) continue;
            SpellEntity hit = null;
            for (SpellEntity c : entities) {
                if (!c.dead && c.owner != p.owner && c.lane == p.lane
                        && (c.kind == Kind.WARD || c.kind == Kind.ORBIT)
                        && Math.abs(c.y - p.y) < c.radius + p.radius) {
                    hit = c;
                    break;
                }
            }
            if (hit != null) {
                float multiplier = interactionMultiplier(p.primary, hit.primary);
                hit.hp -= p.damage * multiplier;
                if (p.owner == 0) playerResonance = Math.min(100f, playerResonance + (multiplier > 1f ? 7f : 3f));
                else enemyResonance = Math.min(100f, enemyResonance + (multiplier > 1f ? 7f : 3f));
                if (p.chill > 0.18f) hit.life = Math.min(hit.life, Math.max(0.8f, 1.8f - p.chill));
                if (p.molten > 0.18f) additions.add(createProfileZone(Kind.HAZARD, p, p.y, 2.8f));
                if (p.vapor > 0.18f) additions.add(createProfileZone(Kind.CLOUD, p, p.y, 2.5f));
                p.dead = true;
                floatingTexts.add(new FloatingText((multiplier > 1f ? "FRACTURE" : "BLOCK"), p.x, p.y,
                        multiplier > 1f ? PALE_GOLD : MUTED, 0.8f));
            }
            if (!p.dead && p.owner == 0 && p.y <= fieldTop + 14f * scale) {
                applyCasterDamage(1, p, additions);
                p.dead = true;
            } else if (!p.dead && p.owner == 1 && p.y >= fieldBottom - 14f * scale) {
                applyCasterDamage(0, p, additions);
                p.dead = true;
            }
        }

        // Projectile versus projectile reactions.
        for (int i = 0; i < entities.size(); i++) {
            SpellEntity a = entities.get(i);
            if (a.dead || a.kind != Kind.PROJECTILE) continue;
            for (int j = i + 1; j < entities.size(); j++) {
                SpellEntity b = entities.get(j);
                if (b.dead || b.kind != Kind.PROJECTILE || a.owner == b.owner || a.lane != b.lane) continue;
                if (Math.abs(a.y - b.y) <= a.radius + b.radius + 8f * scale) {
                    resolveProjectileReaction(a, b, additions);
                    break;
                }
            }
        }

        entities.addAll(additions);
        for (Iterator<SpellEntity> it = entities.iterator(); it.hasNext();) {
            SpellEntity e = it.next();
            if (e.dead || e.y < fieldTop - 220f * scale || e.y > fieldBottom + 220f * scale) it.remove();
        }
    }

    private void resolveProjectileReaction(SpellEntity a, SpellEntity b, ArrayList<SpellEntity> additions) {
        float y = (a.y + b.y) * 0.5f;
        float vapor = Math.max(a.heat * b.moisture, b.heat * a.moisture);
        if (vapor > 0.20f) {
            SpellEntity source = a.vapor >= b.vapor ? a : b;
            SpellEntity cloud = createProfileZone(Kind.CLOUD, source, y, 2.5f + vapor * 0.7f);
            cloud.radius *= 0.9f + Math.min(0.7f, vapor * 0.25f);
            additions.add(cloud);
            a.damage *= Math.max(0.36f, 1f - vapor * 0.24f);
            b.damage *= Math.max(0.36f, 1f - vapor * 0.24f);
            floatingTexts.add(new FloatingText("VAPOR", laneX[a.lane], y, Color.rgb(180, 230, 234), 1.0f));
        }

        float powerA = a.damage * (1f + a.mass * 0.20f + a.impulse * 0.12f + a.volatility * 0.08f);
        float powerB = b.damage * (1f + b.mass * 0.20f + b.impulse * 0.12f + b.volatility * 0.08f);
        int advantage = elementAdvantage(a.primary, b.primary);
        if (advantage > 0) powerA *= 1.34f;
        else if (advantage < 0) powerB *= 1.34f;

        float gap = Math.abs(powerA - powerB) / Math.max(1f, Math.max(powerA, powerB));
        if (gap < 0.20f) {
            a.dead = true; b.dead = true;
            damageNearbyConstructs(a.lane, y, a.owner, b.damage * 0.22f, dp(44f));
            damageNearbyConstructs(a.lane, y, b.owner, a.damage * 0.22f, dp(44f));
            if (Math.max(a.fracture, b.fracture) > 0.24f) {
                SpellEntity source = a.fracture >= b.fracture ? a : b;
                spawnFractureShards(source, y, additions);
            }
            floatingTexts.add(new FloatingText("RESONANT BREAK", laneX[a.lane], y, PALE_GOLD, 1.1f));
            gainReaction(a.owner, 5f); gainReaction(b.owner, 5f);
        } else {
            SpellEntity winner = powerA > powerB ? a : b;
            SpellEntity loser = winner == a ? b : a;
            loser.dead = true;
            winner.damage *= 0.56f + gap * 0.24f;
            winner.radius *= 0.92f;
            if (loser.molten > 0.22f || winner.molten > 0.28f)
                additions.add(createProfileZone(Kind.HAZARD, winner.molten >= loser.molten ? winner : loser, y, 2.8f));
            floatingTexts.add(new FloatingText(advantage != 0 ? "COUNTER" : "OVERPOWER", winner.x, y, winner.primary.color, 1.0f));
            gainReaction(winner.owner, 11f);
            if (winner.owner == 0) reactionsWon++;
        }
    }

    private void spawnFractureShards(SpellEntity source, float y, ArrayList<SpellEntity> additions) {
        for (int direction : new int[]{-1, 1}) {
            int lane = source.lane + direction;
            if (lane < 0 || lane >= 3) continue;
            SpellEntity shard = source.copyProjectile();
            shard.lane = lane; shard.x = laneX[lane]; shard.y = y;
            shard.damage *= 0.34f; shard.radius *= 0.62f; shard.fracture = 0f; shard.split = true;
            additions.add(shard);
        }
    }

    private void gainReaction(int owner, float amount) {
        if (owner == 0) playerResonance = Math.min(100f, playerResonance + amount);
        else if (owner == 1) enemyResonance = Math.min(100f, enemyResonance + amount);
    }

    private void damageNearbyConstructs(int lane, float y, int attackerOwner, float damage, float radius) {
        for (SpellEntity e : entities) {
            if (!e.dead && e.owner != attackerOwner && e.lane == lane
                    && (e.kind == Kind.WARD || e.kind == Kind.ORBIT)
                    && Math.abs(e.y - y) < radius) e.hp -= damage;
        }
    }

    private void detonateBurst(SpellEntity e, ArrayList<SpellEntity> additions) {
        e.dead = true;
        float radius = e.radius;
        for (SpellEntity target : entities) {
            if (!target.dead && target.owner != e.owner && target.lane == e.lane
                    && Math.abs(target.y - e.y) < radius
                    && target.kind != Kind.CLOUD && target.kind != Kind.HAZARD) {
                if (target.kind == Kind.PROJECTILE) target.dead = true;
                else target.hp -= e.damage * interactionMultiplier(e.primary, target.primary);
            }
        }
        boolean guarded = false;
        for (SpellEntity target : entities) {
            if (!target.dead && target.owner != e.owner && target.lane == e.lane && target.kind == Kind.WARD) {
                guarded = true;
                break;
            }
        }
        if (!guarded) {
            float casterDamage = e.damage * 0.72f;
            if (e.owner == 0) {
                enemyHealth -= casterDamage;
                damageDealt += (int)casterDamage;
            } else playerHealth -= casterDamage;
        }
        if (e.vapor > 0.16f) additions.add(createProfileZone(Kind.CLOUD, e, e.y, 2.8f + e.vapor * 0.4f));
        if (e.molten > 0.16f || e.anchored) additions.add(createProfileZone(Kind.HAZARD, e, e.y, 2.7f + e.mass * 0.25f));
        if (e.chill > 0.14f) {
            float slow = 1.8f + e.chill * 0.9f;
            if (e.owner == 0) enemySlow = Math.max(enemySlow, slow);
            else playerSlow = Math.max(playerSlow, slow);
        }
        if (e.fracture > 0.28f) spawnFractureShards(e, e.y, additions);
        floatingTexts.add(new FloatingText(e.combo + " BURST", e.x, e.y, e.primary.color, 1.25f));
    }

    private void applyCasterDamage(int targetOwner, SpellEntity p, ArrayList<SpellEntity> additions) {
        float damage = p.damage;
        if (targetOwner == 1) {
            enemyHealth -= damage;
            damageDealt += (int)damage;
            playerResonance = Math.min(100f, playerResonance + damage * 0.42f);
            if (p.heat > 0.72f) enemyBurn = Math.max(enemyBurn, 1.5f + p.heat * 0.55f);
            if (p.chill > 0.12f) enemySlow = Math.max(enemySlow, 1.6f + p.chill * 0.7f);
        } else {
            playerHealth -= damage;
            enemyResonance = Math.min(100f, enemyResonance + damage * 0.42f);
            if (p.heat > 0.72f) playerBurn = Math.max(playerBurn, 1.5f + p.heat * 0.55f);
            if (p.chill > 0.12f) playerSlow = Math.max(playerSlow, 1.6f + p.chill * 0.7f);
        }
        floatingTexts.add(new FloatingText("-" + Math.round(damage), p.x,
                targetOwner == 1 ? fieldTop + 28f * scale : fieldBottom - 28f * scale,
                p.primary.color, 1f));
        if (p.molten > 0.18f || p.anchored) additions.add(createProfileZone(Kind.HAZARD, p,
                targetOwner == 1 ? fieldTop + dp(34f) : fieldBottom - dp(34f), 2.5f));
        if (p.vapor > 0.22f) additions.add(createProfileZone(Kind.CLOUD, p,
                targetOwner == 1 ? fieldTop + dp(38f) : fieldBottom - dp(38f), 2.3f));
    }

    private void castAiSpell() {
        Difficulty difficulty = effectiveDifficulty();
        SpellProgram program = new SpellProgram();
        program.rune = Rune.values()[random.nextInt(Rune.values().length)];
        int lane = random.nextInt(3);

        float reactionChance = difficulty == Difficulty.APPRENTICE ? 0.18f
                : difficulty == Difficulty.ADEPT ? 0.48f : 0.72f;
        SpellEntity threat = nearestPlayerThreat();
        if (threat != null && random.nextFloat() < reactionChance) {
            lane = threat.lane;
            program.elements.add(counterTo(threat.primary));
            program.rune = random.nextFloat() < 0.55f ? Rune.LANCE : Rune.WARD;
        } else {
            program.elements.add(Element.values()[random.nextInt(4)]);
        }

        float secondChance = difficulty == Difficulty.APPRENTICE ? 0.18f
                : difficulty == Difficulty.ADEPT ? 0.52f : 0.78f;
        float thirdChance = difficulty == Difficulty.APPRENTICE ? 0.03f
                : difficulty == Difficulty.ADEPT ? 0.20f : 0.52f;
        if (duelClock > 10f && random.nextFloat() < secondChance)
            program.elements.add(Element.values()[random.nextInt(4)]);
        if (duelClock > 20f && random.nextFloat() < thirdChance)
            program.elements.add(Element.values()[random.nextInt(4)]);

        float clauseChance = difficulty == Difficulty.APPRENTICE ? 0.08f
                : difficulty == Difficulty.ADEPT ? 0.34f : 0.64f;
        if (random.nextFloat() < clauseChance)
            program.clauses.add(Clause.values()[random.nextInt(Clause.values().length)]);
        if (difficulty == Difficulty.ARCHMAGE && random.nextFloat() < 0.28f) {
            Clause c = Clause.values()[random.nextInt(Clause.values().length)];
            if (!program.clauses.contains(c)) program.clauses.add(c);
        }

        float qualityMin = difficulty == Difficulty.APPRENTICE ? 0.56f
                : difficulty == Difficulty.ADEPT ? 0.70f : 0.82f;
        float qualityRange = difficulty == Difficulty.APPRENTICE ? 0.20f
                : difficulty == Difficulty.ADEPT ? 0.22f : 0.18f;
        float quality = qualityMin + random.nextFloat() * qualityRange;
        float cost = spellCost(program.rune, program.elements.size(), program.clauses.size());
        boolean empowered = enemyResonance >= 100f;
        if (!empowered && enemyMana < cost) {
            aiCooldown = difficulty == Difficulty.APPRENTICE ? 1.2f : 0.65f;
            return;
        }
        if (empowered) enemyResonance = 0f; else enemyMana -= cost;

        float channel = difficulty == Difficulty.APPRENTICE ? 7.2f + random.nextFloat() * 1.8f
                : difficulty == Difficulty.ADEPT ? 4.8f + random.nextFloat() * 1.2f
                : 3.25f + random.nextFloat() * 0.85f;
        channel += Math.max(0, program.elements.size() - 1) * 0.55f;
        channel += program.clauses.size() * 0.42f;
        channel *= selectedTempo.castTimeScale;
        enemyIntent = new EnemyCastIntent(program, lane, quality, empowered, channel);
        showBanner("RIVAL SCRIBING — READ THE " + laneName(lane).toUpperCase(Locale.US) + " LANE", 1.2f);
    }

    private SpellEntity nearestPlayerThreat() {
        SpellEntity threat = null;
        for (SpellEntity e : entities) {
            if (!e.dead && e.owner == 0 && e.kind == Kind.PROJECTILE && e.y < fieldBottom - dp(50f)) {
                if (threat == null || e.y < threat.y) threat = e;
            }
        }
        return threat;
    }

    private void updateEnemyIntent(float dt) {
        if (enemyIntent == null) return;
        enemyIntent.elapsed += dt;
        if (enemyIntent.elapsed >= enemyIntent.totalTime) {
            SpellProgram p = enemyIntent.program;
            deploySpell(1, p.elements, p.rune, enemyIntent.lane, enemyIntent.quality,
                    enemyIntent.empowered, p.clauses, 1f);
            enemyIntent = null;
            aiCooldown = nextAiCooldown();
        }
    }

    private Element counterTo(Element e) {
        switch (e) {
            case FIRE: return Element.WATER;
            case WATER: return Element.WIND;
            case WIND: return Element.STONE;
            case STONE: default: return Element.FIRE;
        }
    }

    private float interactionMultiplier(Element attack, Element defense) {
        int adv = elementAdvantage(attack, defense);
        return adv > 0 ? 1.65f : adv < 0 ? 0.58f : 1f;
    }

    /** Water > Fire > Stone > Wind > Water. */
    private int elementAdvantage(Element a, Element b) {
        if (a == b) return 0;
        if ((a == Element.WATER && b == Element.FIRE)
                || (a == Element.FIRE && b == Element.STONE)
                || (a == Element.STONE && b == Element.WIND)
                || (a == Element.WIND && b == Element.WATER)) return 1;
        if ((b == Element.WATER && a == Element.FIRE)
                || (b == Element.FIRE && a == Element.STONE)
                || (b == Element.STONE && a == Element.WIND)
                || (b == Element.WIND && a == Element.WATER)) return -1;
        return 0;
    }

    private void deploySpell(int owner, List<Element> elements, Rune rune, int lane, float quality, boolean empowered) {
        deploySpell(owner, elements, rune, lane, quality, empowered, Collections.<Clause>emptyList(), 1f);
    }

    private void deploySpell(int owner, List<Element> elements, Rune rune, int lane, float quality,
                             boolean empowered, List<Clause> clauses, float powerScale) {
        if (elements == null || elements.isEmpty()) return;
        Element primary = elements.get(0);
        Element secondary = elements.size() > 1 ? elements.get(1) : null;
        Element tertiary = elements.size() > 2 ? elements.get(2) : null;
        CompiledProfile profile = compileProfile(elements);
        float q = clamp(quality, 0.55f, 1.08f);
        if (owner == 0 && selectedArtifact == Artifact.ASHEN_QUILL && quality > 0.86f && powerScale > 0.9f) {
            q = clamp(q + 0.12f, 0.55f, 1.18f);
            playerResonance = Math.min(100f, playerResonance + 7f);
            floatingTexts.add(new FloatingText("QUILL SURGE", laneX[lane], fieldBottom - dp(28f), PALE_GOLD, 1f));
        }
        boolean anchored = clauses.contains(Clause.ANCHOR);
        SpellEntity entity;
        if (rune == Rune.LANCE) {
            float damage = 10.8f * (0.72f + q * 0.48f);
            entity = createProjectile(owner, lane, primary, secondary, damage, q, empowered);
            applyProfile(entity, profile, tertiary);
            entity.damage *= powerScale;
            entity.radius *= 0.92f + profile.mass * 0.12f + profile.moisture * 0.06f;
            entity.velocity *= (0.82f + profile.impulse * 0.22f) * selectedTempo.projectileScale;
            entity.damage *= 0.88f + profile.heat * 0.20f + profile.mass * 0.12f + profile.volatility * 0.12f;
            if (anchored) { entity.velocity *= 0.48f; entity.damage *= 1.18f; entity.radius *= 1.22f; entity.anchored = true; }
            entities.add(entity);
        } else if (rune == Rune.WARD) {
            entity = new SpellEntity(Kind.WARD, owner, lane, primary, secondary);
            applyProfile(entity, profile, tertiary);
            entity.x = laneX[lane];
            entity.y = owner == 0 ? fieldBottom - dp(38f) : fieldTop + dp(38f);
            entity.radius = dp(19f) * (1f + profile.moisture * 0.10f + profile.mass * 0.12f);
            entity.hp = 20f * (0.82f + q * 0.52f) * (1f + profile.cohesion * 0.42f + profile.mass * 0.20f) * Math.max(0.72f, 1f - profile.volatility * 0.10f) * powerScale;
            entity.life = 6.5f * (1f + profile.cohesion * 0.22f);
            if (owner == 0 && selectedArtifact == Artifact.AEGIS_BELL) { entity.hp *= 1.30f; entity.life *= 1.12f; }
            if (anchored) { entity.hp *= 1.55f; entity.life *= 1.42f; entity.radius *= 1.12f; entity.anchored = true; }
            if (empowered) { entity.hp *= 1.50f; entity.life *= 1.22f; entity.empowered = true; }
            entities.add(entity);
        } else if (rune == Rune.ORBIT) {
            entity = new SpellEntity(Kind.ORBIT, owner, lane, primary, secondary);
            applyProfile(entity, profile, tertiary);
            entity.x = laneX[lane];
            entity.y = owner == 0 ? fieldBottom - dp(48f) : fieldTop + dp(48f);
            entity.radius = dp(15f) * (1f + profile.mass * 0.10f);
            entity.hp = 15f * (0.85f + q * 0.42f) * (1f + profile.cohesion * 0.30f) * powerScale;
            entity.damage = 8.8f * (0.82f + q * 0.38f) * (1f + profile.heat * 0.16f + profile.volatility * 0.10f) * powerScale;
            entity.life = 7.6f * (1f + profile.cohesion * 0.15f) * Math.max(0.76f, 1f - profile.volatility * 0.08f);
            entity.timer = Math.max(0.52f, 1.35f - profile.impulse * 0.22f);
            entity.empowered = empowered;
            if (owner == 0 && selectedArtifact == Artifact.AEGIS_BELL) { entity.hp *= 1.22f; entity.life *= 1.12f; }
            if (anchored) { entity.hp *= 1.42f; entity.life *= 1.38f; entity.timer *= 0.88f; entity.anchored = true; }
            if (empowered) { entity.hp *= 1.40f; entity.damage *= 1.42f; entity.life *= 1.18f; }
            entities.add(entity);
        } else {
            entity = new SpellEntity(Kind.BURST, owner, lane, primary, secondary);
            applyProfile(entity, profile, tertiary);
            entity.x = laneX[lane];
            entity.y = owner == 0 ? fieldTop + (fieldBottom - fieldTop) * 0.32f
                    : fieldBottom - (fieldBottom - fieldTop) * 0.32f;
            entity.radius = dp(48f) * (1f + profile.moisture * 0.18f + profile.impulse * 0.08f);
            entity.damage = 14.5f * (0.76f + q * 0.46f) * (0.92f + profile.heat * 0.18f + profile.mass * 0.12f + profile.volatility * 0.14f) * powerScale;
            entity.timer = Math.max(0.62f, 1.18f - profile.impulse * 0.12f - profile.volatility * 0.08f);
            if (owner == 0 && selectedArtifact == Artifact.PRISM_LENS && elements.size() > 1) entity.radius *= 1.14f;
            if (anchored) { entity.timer *= 1.30f; entity.radius *= 1.30f; entity.damage *= 1.20f; entity.anchored = true; }
            if (empowered) { entity.damage *= 1.48f; entity.radius *= 1.18f; entity.timer *= 0.76f; entity.empowered = true; }
            entities.add(entity);
        }

        if (clauses.contains(Clause.FORK) && powerScale > 0.30f) {
            ArrayList<Clause> branchClauses = new ArrayList<Clause>(clauses);
            branchClauses.remove(Clause.FORK);
            for (int direction : new int[]{-1, 1}) {
                int branchLane = lane + direction;
                if (branchLane >= 0 && branchLane < 3)
                    deploySpell(owner, elements, rune, branchLane, quality, empowered, branchClauses, powerScale * 0.46f);
            }
        }
        if (clauses.contains(Clause.ECHO) && powerScale > 0.30f) {
            SpellProgram echo = new SpellProgram(elements, rune, clauses);
            echo.clauses.remove(Clause.ECHO);
            pendingCasts.add(new PendingCast(owner, echo, lane, quality, empowered,
                    0.92f + elements.size() * 0.12f, powerScale * 0.58f));
        }

        if (powerScale > 0.90f) {
            if (owner == 0) {
                sigilsCast++;
                playerResonance = Math.min(100f, playerResonance + 3.2f + quality * 4.5f
                        + (elements.size() - 1) * 2.2f + clauses.size() * 1.4f);
            } else enemyResonance = Math.min(100f, enemyResonance + 3.2f + quality * 4f);
        }
    }

    private SpellEntity createProjectile(int owner, int lane, Element primary, Element secondary,
                                         float damage, float quality, boolean empowered) {
        SpellEntity e = new SpellEntity(Kind.PROJECTILE, owner, lane, primary, secondary);
        e.x = laneX[lane];
        e.y = owner == 0 ? fieldBottom - dp(18f) : fieldTop + dp(18f);
        e.radius = dp(8f);
        e.damage = damage;
        float speed = dp(112f);
        if (empowered) { e.damage *= 1.48f; e.radius *= 1.28f; speed *= 1.10f; e.empowered = true; }
        e.velocity = owner == 0 ? -speed : speed;
        return e;
    }

    private CompiledProfile compileProfile(List<Element> elements) {
        CompiledProfile p = new CompiledProfile();
        float[] weights = new float[]{1f, 0.72f, 0.52f};
        for (int i = 0; i < elements.size() && i < 3; i++) {
            float w = weights[i];
            Element e = elements.get(i);
            if (e == Element.FIRE) { p.heat += 1.00f * w; p.impulse += 0.22f * w; p.volatility += 0.48f * w; }
            else if (e == Element.WATER) { p.moisture += 1.00f * w; p.cohesion += 0.28f * w; p.heat -= 0.20f * w; }
            else if (e == Element.WIND) { p.impulse += 1.00f * w; p.volatility += 0.25f * w; p.heat -= 0.08f * w; }
            else { p.mass += 1.00f * w; p.cohesion += 0.72f * w; p.impulse -= 0.08f * w; }
        }
        p.heat = Math.max(0f, p.heat);
        p.impulse = Math.max(0.05f, p.impulse);
        p.vapor = Math.max(0f, p.heat * p.moisture - 0.28f);
        p.molten = Math.max(0f, p.heat * p.mass - 0.42f);
        p.chill = Math.max(0f, p.moisture * p.impulse - p.heat * 0.30f - 0.34f);
        p.fracture = Math.max(0f, p.mass * p.impulse - 0.38f);
        StringBuilder name = new StringBuilder();
        for (Element e : elements) { if (name.length() > 0) name.append('·'); name.append(e.label.toUpperCase(Locale.US)); }
        p.label = name.toString();
        return p;
    }

    private void applyProfile(SpellEntity e, CompiledProfile p, Element tertiary) {
        e.tertiary = tertiary; e.heat = p.heat; e.moisture = p.moisture; e.impulse = p.impulse;
        e.mass = p.mass; e.cohesion = p.cohesion; e.volatility = p.volatility;
        e.vapor = p.vapor; e.molten = p.molten; e.chill = p.chill; e.fracture = p.fracture;
        e.combo = p.label;
    }

    private void copySpellProfile(SpellEntity source, SpellEntity target) {
        target.tertiary = source.tertiary; target.heat = source.heat; target.moisture = source.moisture;
        target.impulse = source.impulse; target.mass = source.mass; target.cohesion = source.cohesion;
        target.volatility = source.volatility; target.vapor = source.vapor; target.molten = source.molten;
        target.chill = source.chill; target.fracture = source.fracture; target.combo = source.combo;
        target.anchored = source.anchored;
    }

    private SpellEntity createZone(Kind kind, int owner, int lane, float y, Element element, float life) {
        SpellEntity z = new SpellEntity(kind, owner, lane, element, null);
        z.x = laneX[lane];
        z.y = y;
        z.radius = (kind == Kind.CLOUD ? 112f : 95f) * scale;
        z.life = life;
        return z;
    }

    private SpellEntity createProfileZone(Kind kind, SpellEntity source, float y, float life) {
        SpellEntity z = createZone(kind, source.owner, source.lane, y, source.primary, life);
        z.tertiary = source.tertiary; z.heat = source.heat; z.moisture = source.moisture;
        z.impulse = source.impulse; z.mass = source.mass; z.cohesion = source.cohesion;
        z.volatility = source.volatility; z.vapor = source.vapor; z.molten = source.molten;
        z.chill = source.chill; z.fracture = source.fracture; z.combo = source.combo;
        z.radius *= source.anchored ? 1.28f : 1f;
        return z;
    }

    private float spellCost(Rune rune, int elementCount) {
        return spellCost(rune, elementCount, 0);
    }

    private float spellCost(Rune rune, int elementCount, int clauseCount) {
        return rune.baseCost + Math.max(0, elementCount - 1) * 6f + clauseCount * 5f;
    }

    private float playerSpellCost(Rune rune, int elementCount) {
        return playerSpellCost(rune, elementCount, selectedClauses.size());
    }

    private float playerSpellCost(Rune rune, int elementCount, int clauseCount) {
        float cost = spellCost(rune, elementCount, clauseCount);
        if (selectedArtifact == Artifact.PRISM_LENS && elementCount > 1) cost -= 4f;
        if (selectedArtifact == Artifact.AEGIS_BELL && (rune == Rune.WARD || rune == Rune.ORBIT)) cost += 2f;
        return Math.max(8f, cost);
    }

    private void resetCombat() {
        playerHealth = 100f;
        enemyHealth = 100f;
        playerMana = 100f;
        enemyMana = 100f;
        playerResonance = 0f;
        enemyResonance = 0f;
        playerInk = 3f;
        playerSlow = enemySlow = playerBurn = enemyBurn = 0f;
        aiCooldown = initialAiDelay();
        duelClock = 0f;
        formula.clear();
        selectedClauses.clear();
        armedExecutable = -1;
        enemyIntent = null;
        pendingCasts.clear();
        for (int i = 0; i < executableCooldown.length; i++) executableCooldown[i] = 0f;
        entities.clear();
        floatingTexts.clear();
        currentStroke.clear();
        selectedLane = -1;
        tutorialThreat = null;
        sigilsCast = 0;
        reactionsWon = 0;
        damageDealt = 0;
    }

    private void startDuel() {
        screen = Screen.DUEL;
        resetCombat();
        aiCooldown = initialAiDelay();
        showBanner(selectedDifficulty.label + " DUEL — PREPARE YOUR FIRST FORMULA", 2.2f);
    }

    private void startTutorial() {
        screen = Screen.TUTORIAL;
        resetCombat();
        tutorialStep = T_INTRO;
        tutorialDelay = 0f;
    }

    private void beginDefenseLesson() {
        screen = Screen.TUTORIAL;
        formula.clear();
        selectedLane = -1;
        currentStroke.clear();
        entities.clear();
        floatingTexts.clear();
        playerMana = 100f;
        enemyMana = 100f;
        tutorialStep = T_DEFENSE_DRAW;
        tutorialThreat = createProjectile(1, 1, Element.FIRE, null, 9f, 0.72f, false);
        tutorialThreat.y = (fieldTop + fieldBottom) * 0.42f;
        tutorialThreat.velocity = 0f;
        tutorialThreat.radius *= 1.12f;
        entities.add(tutorialThreat);
        showBanner("THE FIRE LANCE IS SUSPENDED UNTIL YOUR WARD IS READY", 2.4f);
    }

    private void beginTutorialFight() {
        screen = Screen.TUTORIAL;
        resetCombat();
        tutorialStep = T_FINAL_DUEL;
        enemyHealth = 64f;
        playerHealth = 100f;
        enemyMana = 45f;
        aiCooldown = 3.8f;
        showBanner("FINAL PRACTICE — THE RIVAL WILL CAST SLOWLY", 2.4f);
    }

    private void finishDuel(boolean victory) {
        screen = Screen.GAME_OVER;
        resultTitle = victory ? "VICTORY" : "SIGIL BROKEN";
        resultSubtitle = victory
                ? "The rival circle collapses under your syntax."
                : "Your circle failed. Read the counters and cast again.";
        entities.clear();
    }

    private void showBanner(String text, float duration) {
        banner = text;
        bannerTimer = duration;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (screen == Screen.TITLE) return handleTitleTouch(event, x, y);
        if (screen == Screen.GRIMOIRE) return handleGrimoireTouch(event, x, y);
        if (screen == Screen.GAME_OVER) return handleGameOverTouch(event, x, y);
        if (screen == Screen.TUTORIAL && handleTutorialOverlayTouch(event, x, y)) return true;

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (drawingPad.contains(x, y)) {
                drawing = true;
                currentStroke.clear();
                currentStroke.add(new PointF(x, y));
                return true;
            }
            return true;
        } else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && drawing) {
            float clampedX = clamp(x, drawingPad.left, drawingPad.right);
            float clampedY = clamp(y, drawingPad.top, drawingPad.bottom);
            PointF last = currentStroke.get(currentStroke.size() - 1);
            if (distance(last.x, last.y, clampedX, clampedY) > dp(2f)) {
                currentStroke.add(new PointF(clampedX, clampedY));
            }
            return true;
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            if (drawing) {
                drawing = false;
                currentStroke.add(new PointF(clamp(x, drawingPad.left, drawingPad.right),
                        clamp(y, drawingPad.top, drawingPad.bottom)));
                recognizeStroke();
                return true;
            }
            if (clearButton.contains(x, y)) {
                formula.clear(); selectedClauses.clear(); armedExecutable = -1;
                selectedLane = -1;
                showBanner("Formula erased.", 0.8f);
                return true;
            }
            for (int i = 0; i < executableRects.length; i++) {
                if (executableRects[i].contains(x, y)) { armExecutable(i); return true; }
            }
            for (int i = 0; i < clauseRects.length; i++) {
                if (clauseRects[i].contains(x, y)) { toggleClause(Clause.values()[i]); return true; }
            }
            for (int i = 0; i < laneRects.length; i++) {
                if (laneRects[i].contains(x, y)) {
                    if (formula.isEmpty() && armedExecutable < 0) {
                        showBanner("Draw a sigil or arm an executable first.", 1.1f);
                    } else {
                        selectedLane = i; lastStrokeLane = i;
                        if (armedExecutable >= 0) castExecutable(armedExecutable, i);
                        else showBanner("LANE " + (i + 1) + " SELECTED  •  CHOOSE A FORM", 1.05f);
                    }
                    return true;
                }
            }
            for (int i = 0; i < runeRects.length; i++) {
                if (runeRects[i].contains(x, y)) {
                    attemptPlayerCast(Rune.values()[i]);
                    return true;
                }
            }
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            drawing = false;
            currentStroke.clear();
        }
        return true;
    }

    private boolean handleTitleTouch(MotionEvent event, float x, float y) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        for (int i = 0; i < artifactRects.length; i++) {
            if (artifactRects[i].contains(x, y)) {
                selectedArtifact = Artifact.values()[i];
                preferences.edit().putInt("selected_artifact", i).apply();
                return true;
            }
        }
        for (int i = 0; i < difficultyRects.length; i++) {
            if (difficultyRects[i].contains(x, y)) {
                selectedDifficulty = Difficulty.values()[i];
                preferences.edit().putInt("selected_difficulty", i).apply();
                showBanner(selectedDifficulty.label + " SELECTED", 0.9f);
                return true;
            }
        }
        for (int i = 0; i < tempoRects.length; i++) {
            if (tempoRects[i].contains(x, y)) {
                selectedTempo = Tempo.values()[i];
                preferences.edit().putInt("selected_tempo", i).apply();
                showBanner(selectedTempo.label + " TEMPO SELECTED", 0.9f);
                return true;
            }
        }
        if (grimoireButton.contains(x, y)) {
            editingSlot = 0; editorProgram = deck[0].copy(); screen = Screen.GRIMOIRE; return true;
        }
        if (mainButton.contains(x, y)) {
            if (tutorialComplete) startDuel(); else startTutorial();
        } else if (secondaryButton.contains(x, y)) {
            startTutorial();
        }
        return true;
    }

    private void toggleClause(Clause clause) {
        if (armedExecutable >= 0) { showBanner("Executable syntax is already compiled.", 1.0f); return; }
        if (formula.isEmpty()) { showBanner("Draw an element before adding clauses.", 1.0f); return; }
        if (selectedClauses.contains(clause)) selectedClauses.remove(clause);
        else if (selectedClauses.size() < 2) selectedClauses.add(clause);
        else { showBanner("A spell can carry two clauses.", 1.0f); return; }
        showBanner(clause.label + (selectedClauses.contains(clause) ? " ATTACHED" : " REMOVED"), 0.8f);
    }

    private void armExecutable(int slot) {
        if (screen == Screen.TUTORIAL && tutorialStep < T_FINAL_DUEL) { showBanner("Executables unlock after initiation.", 1.1f); return; }
        if (executableCooldown[slot] > 0f) { showBanner("EXECUTABLE COOLING: " + Math.round(executableCooldown[slot]) + "s", 1.0f); return; }
        SpellProgram p = deck[slot];
        float cost = playerSpellCost(p.rune, p.elements.size(), p.clauses.size()) * 1.25f;
        if (playerInk < 0.99f) { showBanner("NO INK — DRAW CLEAN GLYPHS TO REFILL", 1.2f); return; }
        if (playerMana < cost && playerResonance < 100f) { showBanner("NEED " + Math.round(cost) + " MANA", 1.0f); return; }
        formula.clear(); selectedClauses.clear(); currentStroke.clear(); selectedLane = -1;
        armedExecutable = slot;
        showBanner("EXECUTABLE " + (slot + 1) + " ARMED — CHOOSE A LANE", 1.3f);
    }

    private void castExecutable(int slot, int lane) {
        SpellProgram p = deck[slot];
        float cost = playerSpellCost(p.rune, p.elements.size(), p.clauses.size()) * 1.25f;
        boolean empowered = playerResonance >= 100f;
        if (empowered) playerResonance = 0f; else playerMana = Math.max(0f, playerMana - cost);
        playerInk = Math.max(0f, playerInk - 1f);
        executableCooldown[slot] = 8.5f + p.clauses.size() * 1.5f;
        deploySpell(0, p.elements, p.rune, lane, 0.92f, empowered, p.clauses, 1f);
        armedExecutable = -1; selectedLane = -1;
        showBanner("EXECUTED: " + p.shortLabel(), 1.1f);
    }

    private boolean handleGrimoireTouch(MotionEvent event, float x, float y) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        if (editorBackButton.contains(x, y)) { screen = Screen.TITLE; return true; }
        for (int i = 0; i < grimoireSlotRects.length; i++) {
            if (grimoireSlotRects[i].contains(x, y)) { editingSlot = i; editorProgram = deck[i].copy(); return true; }
        }
        for (int i = 0; i < editorElementRects.length; i++) {
            if (editorElementRects[i].contains(x, y)) {
                if (editorProgram.elements.size() < 3) editorProgram.elements.add(Element.values()[i]);
                else showBanner("THREE ELEMENTS MAXIMUM", 0.9f);
                return true;
            }
        }
        for (int i = 0; i < editorRuneRects.length; i++) {
            if (editorRuneRects[i].contains(x, y)) { editorProgram.rune = Rune.values()[i]; return true; }
        }
        for (int i = 0; i < editorClauseRects.length; i++) {
            if (editorClauseRects[i].contains(x, y)) {
                Clause c = Clause.values()[i];
                if (editorProgram.clauses.contains(c)) editorProgram.clauses.remove(c);
                else if (editorProgram.clauses.size() < 2) editorProgram.clauses.add(c);
                else showBanner("TWO CLAUSES MAXIMUM", 0.9f);
                return true;
            }
        }
        if (editorClearButton.contains(x, y)) { editorProgram.elements.clear(); editorProgram.clauses.clear(); return true; }
        if (editorSaveButton.contains(x, y)) { saveDeckSlot(editingSlot); return true; }
        return true;
    }

    private boolean handleGameOverTouch(MotionEvent event, float x, float y) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        if (mainButton.contains(x, y)) startDuel();
        else if (secondaryButton.contains(x, y)) screen = Screen.TITLE;
        return true;
    }

    private boolean handleTutorialOverlayTouch(MotionEvent event, float x, float y) {
        boolean blockingCard = tutorialStep == T_INTRO || tutorialStep == T_ELEMENTS
                || tutorialStep == T_RUNES || tutorialStep == T_DEFENSE_INTRO
                || tutorialStep == T_COMPLETE;

        // Full-screen lesson cards consume the entire gesture, including DOWN
        // and MOVE, so a tap cannot accidentally begin a hidden glyph beneath.
        if (blockingCard) {
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
            drawing = false;
            currentStroke.clear();
            if (tutorialStep == T_INTRO) {
                tutorialStep = T_ELEMENTS;
            } else if (tutorialStep == T_ELEMENTS) {
                tutorialStep = T_RUNES;
            } else if (tutorialStep == T_RUNES) {
                tutorialStep = T_FIRE_DRAW;
                showBanner("TRACE THE FIRE TRIANGLE — THERE IS NO TIME LIMIT", 2f);
            } else if (tutorialStep == T_DEFENSE_INTRO) {
                beginDefenseLesson();
            } else if (tutorialStep == T_COMPLETE) {
                screen = Screen.TITLE;
                entities.clear();
                formula.clear();
                selectedLane = -1;
            }
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP
                && x > getWidth() - dp(96f) && y < dp(58f)
                && tutorialStep < T_FINAL_DUEL) {
            drawing = false;
            currentStroke.clear();
            preferences.edit().putBoolean("tutorial_complete", true).apply();
            tutorialComplete = true;
            screen = Screen.TITLE;
            entities.clear();
            formula.clear();
            selectedLane = -1;
            return true;
        }
        return false;
    }

    private void recognizeStroke() {
        if (currentStroke.size() < 7 || pathLength(currentStroke) < 85f * scale) {
            showBanner("The mark was too faint. Try again — the lesson will wait.", 1.4f);
            currentStroke.clear();
            return;
        }
        GlyphResult result = recognizer.recognize(currentStroke);
        lastGlyphQuality = result.score;
        currentStroke.clear();
        if (result.score < 0.52f) {
            showBanner("Unstable glyph — trace a cleaner form. No rush.", 1.5f);
            return;
        }
        Element element = result.element;
        if (screen == Screen.TUTORIAL) {
            Element expected = expectedTutorialElement();
            if (expected != null && element != expected) {
                showBanner(tutorialGlyphHint(expected), 1.7f);
                return;
            }
        }
        if (formula.size() >= 3) {
            showBanner("THREE ELEMENTS BOUND — CAST OR CLEAR THE SENTENCE", 1.4f);
            return;
        }
        armedExecutable = -1;
        formula.add(element);
        selectedLane = -1;
        if (screen == Screen.DUEL || tutorialStep == T_FINAL_DUEL)
            playerInk = Math.min(3f, playerInk + 0.28f + Math.max(0f, result.score - 0.62f) * 0.72f);
        formulaStartedAt = SystemClock.uptimeMillis();
        playerResonance = Math.min(100f, playerResonance + Math.max(0f, result.score - 0.68f) * 18f);
        String quality = result.score > 0.88f ? "PERFECT" : result.score > 0.74f ? "CLEAN" : "BOUND";
        showBanner(quality + " " + element.label.toUpperCase(Locale.US)
                + (formula.size() < 3 ? "  •  ADD ELEMENT / CLAUSE OR CHOOSE LANE" : "  •  CHOOSE LANE"), 1.3f);

        if (screen == Screen.TUTORIAL) {
            if (tutorialStep == T_FIRE_DRAW) tutorialStep = T_FIRE_CAST;
            else if (tutorialStep == T_WATER_DRAW) tutorialStep = T_WATER_CAST;
            else if (tutorialStep == T_WIND_DRAW) tutorialStep = T_WIND_CAST;
            else if (tutorialStep == T_STONE_DRAW) tutorialStep = T_STONE_CAST;
            else if (tutorialStep == T_COMPOUND_FIRE) tutorialStep = T_COMPOUND_WIND;
            else if (tutorialStep == T_COMPOUND_WIND && formula.size() == 2) tutorialStep = T_COMPOUND_CAST;
            else if (tutorialStep == T_DEFENSE_DRAW) tutorialStep = T_DEFENSE_CAST;
        }
    }

    private Element expectedTutorialElement() {
        if (tutorialStep == T_FIRE_DRAW) return Element.FIRE;
        if (tutorialStep == T_WATER_DRAW || tutorialStep == T_DEFENSE_DRAW) return Element.WATER;
        if (tutorialStep == T_WIND_DRAW) return Element.WIND;
        if (tutorialStep == T_STONE_DRAW) return Element.STONE;
        if (tutorialStep == T_COMPOUND_FIRE) return Element.FIRE;
        if (tutorialStep == T_COMPOUND_WIND) return Element.WIND;
        return null;
    }

    private String tutorialGlyphHint(Element expected) {
        if (expected == Element.FIRE) return "Fire is one closed triangle. The lesson will wait for it.";
        if (expected == Element.WATER) return "Water is a continuous three-crested wave. Try again.";
        if (expected == Element.WIND) return "Wind is an inward spiral. Curl smoothly toward the center.";
        return "Stone is one closed square with four clear corners.";
    }

    private void attemptPlayerCast(Rune rune) {
        if (formula.isEmpty()) {
            showBanner("Draw an elemental sigil first.", 1.2f);
            return;
        }
        if (selectedLane < 0) {
            showBanner("Choose a lane before deploying the spell.", 1.2f);
            return;
        }
        if (screen == Screen.TUTORIAL) {
            Rune expectedRune = expectedTutorialRune();
            if (expectedRune != null && rune != expectedRune) {
                showBanner("This lesson calls for " + expectedRune.label + ". Your formula will remain ready.", 1.7f);
                return;
            }
            int expectedLane = expectedTutorialLane();
            if (expectedLane >= 0 && selectedLane != expectedLane) {
                showBanner("Choose the " + laneName(expectedLane) + " lane. Your formula will not expire.", 1.7f);
                return;
            }
            if (tutorialStep == T_COMPOUND_CAST && !selectedClauses.contains(Clause.FORK)) {
                showBanner("Attach the FORK clause first. Your core will remain ready.", 1.7f);
                return;
            }
            if (expectedRune == null && tutorialStep < T_FINAL_DUEL) {
                showBanner("Complete the current instruction first.", 1.2f);
                return;
            }
        }
        float cost = playerSpellCost(rune, formula.size());
        boolean empowered = playerResonance >= 100f;
        if (!empowered && playerMana < cost) {
            showBanner("Insufficient mana.", 1.1f);
            return;
        }
        ArrayList<Element> castFormula = new ArrayList<Element>(formula);
        ArrayList<Clause> castClauses = new ArrayList<Clause>(selectedClauses);
        if (empowered) playerResonance = 0f;
        else playerMana -= cost;
        int castLane = selectedLane;
        deploySpell(0, castFormula, rune, castLane, lastGlyphQuality, empowered, castClauses, 1f);
        String name = compileProfile(castFormula).label;
        String clauseText = castClauses.isEmpty() ? "" : " + " + clauseList(castClauses);
        showBanner((empowered ? "ASCENDANT " : "") + name + " " + rune.label + clauseText + "  •  " + laneName(castLane), 1.25f);
        formula.clear(); selectedClauses.clear();
        selectedLane = -1;

        if (screen == Screen.TUTORIAL) {
            if (tutorialStep == T_FIRE_CAST) {
                tutorialStep = T_WATER_DRAW;
            } else if (tutorialStep == T_WATER_CAST) {
                tutorialStep = T_WIND_DRAW;
            } else if (tutorialStep == T_WIND_CAST) {
                tutorialStep = T_STONE_DRAW;
            } else if (tutorialStep == T_STONE_CAST) {
                tutorialStep = T_COMPOUND_FIRE;
            } else if (tutorialStep == T_COMPOUND_CAST) {
                entities.clear();
                tutorialStep = T_DEFENSE_INTRO;
            } else if (tutorialStep == T_DEFENSE_CAST) {
                tutorialStep = T_DEFENSE_WATCH;
                if (tutorialThreat != null && !tutorialThreat.dead) {
                    tutorialThreat.velocity = 150f * scale;
                }
                showBanner("WARD DEPLOYED — WATCH WATER ABSORB THE FIRE LANCE", 2.2f);
            }
        }
    }

    private String clauseList(List<Clause> clauses) {
        StringBuilder out = new StringBuilder();
        for (Clause c : clauses) { if (out.length() > 0) out.append("+"); out.append(c.label); }
        return out.toString();
    }

    private Rune expectedTutorialRune() {
        if (tutorialStep == T_FIRE_CAST) return Rune.LANCE;
        if (tutorialStep == T_WATER_CAST || tutorialStep == T_DEFENSE_CAST) return Rune.WARD;
        if (tutorialStep == T_WIND_CAST) return Rune.ORBIT;
        if (tutorialStep == T_STONE_CAST || tutorialStep == T_COMPOUND_CAST) return Rune.BURST;
        return null;
    }

    private int expectedTutorialLane() {
        if (tutorialStep == T_FIRE_CAST) return 1;
        if (tutorialStep == T_WATER_CAST) return 0;
        if (tutorialStep == T_WIND_CAST) return 2;
        if (tutorialStep == T_STONE_CAST) return 1;
        if (tutorialStep == T_COMPOUND_CAST) return 1;
        if (tutorialStep == T_DEFENSE_CAST) return 1;
        return -1;
    }

    private String laneName(int lane) {
        return lane == 0 ? "LEFT" : lane == 1 ? "CENTER" : "RIGHT";
    }

    private int nearestLane(float x) {
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < laneX.length; i++) {
            float d = Math.abs(x - laneX[i]);
            if (d < bestDistance) { bestDistance = d; best = i; }
        }
        return best;
    }

    private void drawBackground(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(backgroundShader);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
        paint.setColor(Color.argb(30, 255, 255, 255));
        for (int i = 0; i < 45; i++) {
            float x = ((i * 173) % 1080) * scale;
            float y = ((i * 311) % Math.max(1, getHeight()));
            canvas.drawCircle(x, y, (i % 3 + 1) * 0.7f * scale, paint);
        }
    }

    private void drawGrimoire(Canvas canvas) {
        float w = getWidth(), h = getHeight(), margin = dp(14f), gap = dp(8f);
        drawDecorativeFrame(canvas, dp(9f), dp(10f), w - dp(9f), h - dp(10f));
        paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(27f)); paint.setColor(PALE_GOLD); canvas.drawText("EXECUTABLE GRIMOIRE", w / 2f, dp(48f), paint);
        paint.setTypeface(UI_REGULAR); paint.setTextSize(sp(12f)); paint.setColor(MUTED);
        canvas.drawText("Precompile three spell programs. Executing costs +25% mana and 1 Ink.", w / 2f, dp(70f), paint);

        editorBackButton.set(margin, dp(22f), margin + dp(66f), dp(58f));
        drawSmallButton(canvas, editorBackButton, "BACK", false);

        float slotTop = dp(88f), slotW = (w - margin * 2f - gap * 2f) / 3f;
        for (int i = 0; i < 3; i++) {
            RectF r = grimoireSlotRects[i];
            r.set(margin + i * (slotW + gap), slotTop, margin + i * (slotW + gap) + slotW, slotTop + dp(62f));
            paint.setStyle(Paint.Style.FILL); paint.setColor(i == editingSlot ? Color.rgb(65, 51, 82) : PANEL);
            canvas.drawRoundRect(r, dp(12f), dp(12f), paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(i == editingSlot ? dp(2f) : dp(1f));
            paint.setColor(i == editingSlot ? PALE_GOLD : Color.argb(110, 232, 199, 103)); canvas.drawRoundRect(r, dp(12f), dp(12f), paint);
            paint.setStyle(Paint.Style.FILL); paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(11f)); paint.setColor(WHITE); canvas.drawText("SLOT " + (i + 1), r.centerX(), r.top + dp(20f), paint);
            paint.setTypeface(UI_REGULAR); paint.setTextSize(sp(11f)); paint.setColor(MUTED);
            canvas.drawText(deck[i].shortLabel(), r.centerX(), r.top + dp(42f), paint);
        }

        float previewTop = slotTop + dp(76f);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(170, 28, 23, 46));
        RectF preview = new RectF(margin, previewTop, w - margin, previewTop + dp(64f));
        canvas.drawRoundRect(preview, dp(14f), dp(14f), paint);
        paint.setTextSize(sp(11.5f)); paint.setColor(GOLD); canvas.drawText("COMPILED SENTENCE", w / 2f, preview.top + dp(18f), paint);
        paint.setTextSize(sp(15f)); paint.setTypeface(UI_MEDIUM); paint.setColor(WHITE);
        String previewText = editorProgram.elements.isEmpty() ? "ADD AN ELEMENT" : compileProfile(editorProgram.elements).label + " → " + editorProgram.rune.label;
        canvas.drawText(previewText, w / 2f, preview.top + dp(39f), paint);
        paint.setTypeface(UI_REGULAR); paint.setTextSize(sp(11.5f)); paint.setColor(PALE_GOLD);
        canvas.drawText(editorProgram.clauses.isEmpty() ? "no clauses" : clauseList(editorProgram.clauses), w / 2f, preview.top + dp(56f), paint);

        float y = preview.bottom + dp(24f);
        paint.setTextSize(sp(11f)); paint.setColor(GOLD); canvas.drawText("1. ELEMENTS — TAP IN ORDER (MAX 3)", w / 2f, y, paint);
        float rowTop = y + dp(10f), chipW = (w - margin * 2f - gap * 3f) / 4f;
        for (int i = 0; i < 4; i++) {
            RectF r = editorElementRects[i]; r.set(margin + i * (chipW + gap), rowTop, margin + i * (chipW + gap) + chipW, rowTop + dp(48f));
            drawElementChip(canvas, r, Element.values()[i]);
        }

        y = rowTop + dp(72f); paint.setTextSize(sp(11f)); paint.setColor(GOLD); canvas.drawText("2. FORM", w / 2f, y, paint);
        rowTop = y + dp(10f);
        for (int i = 0; i < 4; i++) {
            RectF r = editorRuneRects[i]; r.set(margin + i * (chipW + gap), rowTop, margin + i * (chipW + gap) + chipW, rowTop + dp(64f));
            boolean selected = editorProgram.rune == Rune.values()[i];
            paint.setStyle(Paint.Style.FILL); paint.setColor(selected ? Color.rgb(64, 53, 80) : PANEL); canvas.drawRoundRect(r, dp(12f), dp(12f), paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(selected ? dp(2f) : dp(1f)); paint.setColor(selected ? PALE_GOLD : MUTED); canvas.drawRoundRect(r, dp(12f), dp(12f), paint);
            drawRuneIcon(canvas, Rune.values()[i], r.centerX(), r.top + dp(20f), selected ? PALE_GOLD : MUTED);
            paint.setStyle(Paint.Style.FILL); paint.setTextSize(sp(11f)); paint.setColor(WHITE); canvas.drawText(Rune.values()[i].label, r.centerX(), r.bottom - dp(9f), paint);
        }

        y = rowTop + dp(88f); paint.setTextSize(sp(11f)); paint.setColor(GOLD); canvas.drawText("3. CLAUSES — CHOOSE UP TO 2", w / 2f, y, paint);
        rowTop = y + dp(10f); float clauseW = (w - margin * 2f - gap * 2f) / 3f;
        for (int i = 0; i < 3; i++) {
            RectF r = editorClauseRects[i]; r.set(margin + i * (clauseW + gap), rowTop, margin + i * (clauseW + gap) + clauseW, rowTop + dp(50f));
            drawClauseChip(canvas, r, Clause.values()[i], editorProgram.clauses.contains(Clause.values()[i]));
        }

        float buttonTop = Math.min(h - dp(68f), rowTop + dp(72f));
        editorClearButton.set(margin, buttonTop, w * 0.43f, buttonTop + dp(48f));
        editorSaveButton.set(w * 0.46f, buttonTop, w - margin, buttonTop + dp(48f));
        drawButton(canvas, editorClearButton, "CLEAR SYNTAX", false); drawButton(canvas, editorSaveButton, "COMPILE SLOT " + (editingSlot + 1), true);
        if (bannerTimer > 0f) { paint.setTextSize(sp(12f)); paint.setColor(PALE_GOLD); canvas.drawText(banner, w / 2f, h - dp(12f), paint); }
    }

    private void drawSmallButton(Canvas canvas, RectF r, String label, boolean selected) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(selected ? Color.rgb(65, 51, 82) : PANEL); canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1f)); paint.setColor(selected ? PALE_GOLD : MUTED); canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(sp(11f)); paint.setColor(WHITE); canvas.drawText(label, r.centerX(), r.centerY() + dp(4f), paint);
    }

    private void drawElementChip(Canvas canvas, RectF r, Element e) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(55, Color.red(e.color), Color.green(e.color), Color.blue(e.color))); canvas.drawRoundRect(r, dp(12f), dp(12f), paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1.5f)); paint.setColor(e.color); canvas.drawRoundRect(r, dp(12f), dp(12f), paint);
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(11f)); paint.setColor(WHITE); canvas.drawText(e.label.toUpperCase(Locale.US), r.centerX(), r.centerY() + dp(4f), paint); paint.setTypeface(UI_REGULAR);
    }

    private void drawClauseChip(Canvas canvas, RectF r, Clause c, boolean selected) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(selected ? Color.rgb(67, 52, 84) : PANEL); canvas.drawRoundRect(r, dp(12f), dp(12f), paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(selected ? dp(2f) : dp(1f)); paint.setColor(selected ? PALE_GOLD : MUTED); canvas.drawRoundRect(r, dp(12f), dp(12f), paint);
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(UI_MEDIUM); paint.setTextSize(sp(11f)); paint.setColor(WHITE);
        canvas.drawText(c.label, r.centerX(), r.top + dp(20f), paint); paint.setTypeface(UI_REGULAR); paint.setTextSize(sp(10.8f)); paint.setColor(MUTED); canvas.drawText(c.subtitle, r.centerX(), r.top + dp(37f), paint);
    }

    private void drawTitle(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        drawDecorativeFrame(canvas, dp(9f), dp(10f), w - dp(9f), h - dp(10f));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setColor(PALE_GOLD); paint.setTextSize(Math.min(sp(36f), w * 0.105f));
        canvas.drawText("SIGILBOUND", w / 2f, dp(48f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setLetterSpacing(0.17f); paint.setTextSize(sp(13f)); paint.setColor(GOLD);
        canvas.drawText("READABLE SYNTAX", w / 2f, dp(72f), paint); paint.setLetterSpacing(0f);
        drawTitleSigil(canvas, w / 2f, dp(119f), dp(33f));
        paint.setTextSize(sp(14.5f)); paint.setColor(WHITE);
        canvas.drawText("Write spells. Read the rival. Control three lanes.", w / 2f, dp(170f), paint);

        float margin = dp(14f), gap = dp(7f);
        paint.setTextSize(sp(11f)); paint.setColor(GOLD);
        canvas.drawText("ARTIFACT", w / 2f, dp(197f), paint);
        float cardW = (w - margin * 2f - gap * 2f) / 3f;
        float artifactTop = dp(207f);
        for (int i = 0; i < artifactRects.length; i++) {
            artifactRects[i].set(margin + i * (cardW + gap), artifactTop,
                    margin + i * (cardW + gap) + cardW, artifactTop + dp(82f));
            drawArtifactCard(canvas, artifactRects[i], Artifact.values()[i], selectedArtifact == Artifact.values()[i]);
        }

        float difficultyLabelY = artifactTop + dp(100f);
        paint.setTextSize(sp(11f)); paint.setColor(GOLD);
        canvas.drawText("RIVAL", w / 2f, difficultyLabelY, paint);
        float difficultyTop = difficultyLabelY + dp(8f);
        for (int i = 0; i < difficultyRects.length; i++) {
            difficultyRects[i].set(margin + i * (cardW + gap), difficultyTop,
                    margin + i * (cardW + gap) + cardW, difficultyTop + dp(43f));
            drawDifficultyButton(canvas, difficultyRects[i], Difficulty.values()[i], selectedDifficulty == Difficulty.values()[i]);
        }

        float tempoLabelY = difficultyTop + dp(60f);
        paint.setTextSize(sp(11f)); paint.setColor(GOLD);
        canvas.drawText("BATTLEFIELD TEMPO", w / 2f, tempoLabelY, paint);
        float tempoTop = tempoLabelY + dp(8f);
        for (int i = 0; i < tempoRects.length; i++) {
            tempoRects[i].set(margin + i * (cardW + gap), tempoTop,
                    margin + i * (cardW + gap) + cardW, tempoTop + dp(43f));
            drawTempoButton(canvas, tempoRects[i], Tempo.values()[i], selectedTempo == Tempo.values()[i]);
        }

        float buttonW = Math.min(w - dp(34f), dp(340f));
        float buttonH = dp(48f);
        grimoireButton.set((w - buttonW) / 2f, tempoTop + dp(53f), (w + buttonW) / 2f, tempoTop + dp(53f) + buttonH);
        drawButton(canvas, grimoireButton, "EDIT EXECUTABLE GRIMOIRE", false);

        float buttonTop = Math.max(grimoireButton.bottom + dp(10f), h - dp(tutorialComplete ? 116f : 58f));
        mainButton.set((w - buttonW) / 2f, buttonTop, (w + buttonW) / 2f, buttonTop + buttonH);
        drawButton(canvas, mainButton, tutorialComplete ? "ENTER " + selectedDifficulty.label + " DUEL" : "BEGIN INITIATION", true);
        if (tutorialComplete) {
            secondaryButton.set((w - buttonW) / 2f, mainButton.bottom + dp(7f),
                    (w + buttonW) / 2f, mainButton.bottom + dp(7f) + buttonH);
            drawButton(canvas, secondaryButton, "REPLAY INITIATION", false);
        } else secondaryButton.set(0f, 0f, 0f, 0f);
    }

    private void drawTempoButton(Canvas canvas, RectF r, Tempo tempo, boolean selected) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(selected ? Color.rgb(55, 68, 88) : Color.rgb(28, 24, 42));
        canvas.drawRoundRect(r, dp(11f), dp(11f), paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(selected ? dp(2f) : dp(1f));
        paint.setColor(selected ? Element.WATER.color : Color.argb(105, 232, 199, 103));
        canvas.drawRoundRect(r, dp(11f), dp(11f), paint);
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(11.5f)); paint.setColor(selected ? WHITE : Color.rgb(214, 207, 218));
        canvas.drawText(tempo.label, r.centerX(), r.top + dp(17f), paint);
        paint.setTypeface(UI_REGULAR); paint.setTextSize(sp(10.8f));
        paint.setColor(selected ? Element.WATER.color : MUTED);
        canvas.drawText(tempo.subtitle, r.centerX(), r.top + dp(33f), paint);
    }

    private void drawArtifactCard(Canvas canvas, RectF r, Artifact artifact, boolean selected) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? Color.rgb(61, 48, 78) : Color.rgb(28, 24, 43));
        canvas.drawRoundRect(r, dp(13f), dp(13f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? dp(2f) : dp(1f));
        paint.setColor(selected ? PALE_GOLD : Color.argb(120, 232, 199, 103));
        canvas.drawRoundRect(r, dp(13f), dp(13f), paint);
        float cx = r.centerX();
        float iconY = r.top + dp(22f);
        paint.setColor(selected ? PALE_GOLD : GOLD);
        paint.setStrokeWidth(dp(2f));
        if (artifact == Artifact.PRISM_LENS) {
            Path prism = new Path();
            prism.moveTo(cx, iconY - dp(9f));
            prism.lineTo(cx + dp(10f), iconY + dp(8f));
            prism.lineTo(cx - dp(10f), iconY + dp(8f));
            prism.close();
            canvas.drawPath(prism, paint);
            canvas.drawCircle(cx, iconY + dp(1f), dp(14f), paint);
        } else if (artifact == Artifact.ASHEN_QUILL) {
            canvas.drawLine(cx - dp(11f), iconY + dp(10f), cx + dp(9f), iconY - dp(10f), paint);
            canvas.drawLine(cx - dp(6f), iconY + dp(5f), cx + dp(8f), iconY + dp(1f), paint);
            canvas.drawLine(cx + dp(1f), iconY - dp(2f), cx + dp(5f), iconY - dp(9f), paint);
        } else {
            RectF bell = new RectF(cx - dp(12f), iconY - dp(9f), cx + dp(12f), iconY + dp(10f));
            canvas.drawArc(bell, 190f, 160f, false, paint);
            canvas.drawLine(cx - dp(13f), iconY + dp(8f), cx + dp(13f), iconY + dp(8f), paint);
            canvas.drawCircle(cx, iconY + dp(12f), dp(2f), paint);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(11.5f));
        paint.setColor(selected ? WHITE : Color.rgb(225, 218, 224));
        canvas.drawText(artifact.label, cx, r.top + dp(53f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(11f));
        paint.setColor(MUTED);
        String subtitle = artifact == Artifact.PRISM_LENS ? "compound cost" :
                artifact == Artifact.ASHEN_QUILL ? "precision surge" : "tough constructs";
        canvas.drawText(subtitle, cx, r.top + dp(72f), paint);
    }

    private void drawDifficultyButton(Canvas canvas, RectF r, Difficulty difficulty, boolean selected) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? Color.rgb(70, 52, 82) : Color.rgb(28, 24, 42));
        canvas.drawRoundRect(r, dp(11f), dp(11f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? dp(2f) : dp(1f));
        paint.setColor(selected ? PALE_GOLD : Color.argb(105, 232, 199, 103));
        canvas.drawRoundRect(r, dp(11f), dp(11f), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(11.5f));
        paint.setColor(selected ? WHITE : Color.rgb(214, 207, 218));
        canvas.drawText(difficulty.label, r.centerX(), r.top + dp(18f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(10.8f));
        paint.setColor(selected ? PALE_GOLD : MUTED);
        canvas.drawText(difficulty.subtitle, r.centerX(), r.top + dp(34f), paint);
    }

    private void drawTitleSigil(Canvas canvas, float cx, float cy, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f * scale);
        paint.setColor(GOLD);
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.drawCircle(cx, cy, radius * 0.76f, paint);
        Path tri = new Path();
        tri.moveTo(cx, cy - radius * 0.72f);
        tri.lineTo(cx + radius * 0.64f, cy + radius * 0.48f);
        tri.lineTo(cx - radius * 0.64f, cy + radius * 0.48f);
        tri.close();
        canvas.drawPath(tri, paint);
        paint.setColor(Element.WATER.color);
        paint.setStrokeWidth(8f * scale);
        Path wave = new Path();
        wave.moveTo(cx - radius * 0.55f, cy + radius * 0.05f);
        wave.cubicTo(cx - radius * 0.28f, cy - radius * 0.2f, cx - radius * 0.12f, cy + radius * 0.25f, cx + radius * 0.05f, cy);
        wave.cubicTo(cx + radius * 0.25f, cy - radius * 0.2f, cx + radius * 0.4f, cy + radius * 0.2f, cx + radius * 0.56f, cy - radius * 0.02f);
        canvas.drawPath(wave, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PALE_GOLD);
        canvas.drawCircle(cx, cy, 12f * scale, paint);
    }

    private void drawArena(Canvas canvas) {
        // Field panel and lane architecture.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(175, 14, 12, 29));
        canvas.drawRoundRect(new RectF(24f * scale, fieldTop - 20f * scale,
                getWidth() - 24f * scale, fieldBottom + 18f * scale), 34f * scale, 34f * scale, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * scale);
        paint.setColor(Color.argb(90, 232, 199, 103));
        canvas.drawRoundRect(new RectF(24f * scale, fieldTop - 20f * scale,
                getWidth() - 24f * scale, fieldBottom + 18f * scale), 34f * scale, 34f * scale, paint);
        for (int i = 0; i < 3; i++) {
            boolean playerTarget = i == selectedLane;
            boolean rivalTarget = enemyIntent != null && i == enemyIntent.lane;
            if (rivalTarget) {
                float halfLane = i == 1 ? (laneX[2] - laneX[1]) * 0.38f : (laneX[1] - laneX[0]) * 0.38f;
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(34, 193, 109, 211));
                canvas.drawRoundRect(new RectF(laneX[i] - halfLane, fieldTop + dp(2f),
                        laneX[i] + halfLane, fieldBottom - dp(2f)), dp(18f), dp(18f), paint);
                paint.setStyle(Paint.Style.STROKE);
            }
            paint.setColor(rivalTarget ? Color.argb(180, 213, 128, 225)
                    : Color.argb(playerTarget ? 120 : 48, 232, 199, 103));
            paint.setStrokeWidth((playerTarget || rivalTarget ? 3f : 2f) * scale);
            canvas.drawLine(laneX[i], fieldTop + 20f * scale, laneX[i], fieldBottom - 12f * scale, paint);
            for (int k = 0; k < 4; k++) {
                float y = fieldTop + (fieldBottom - fieldTop) * (0.18f + k * 0.22f);
                canvas.drawCircle(laneX[i], y, (26f + k * 3f) * scale, paint);
            }
        }
        // Midline reaction band.
        float mid = (fieldTop + fieldBottom) * 0.5f;
        paint.setColor(Color.argb(45, 126, 211, 210));
        paint.setStrokeWidth(16f * scale);
        canvas.drawLine(48f * scale, mid, getWidth() - 48f * scale, mid, paint);
        paint.setColor(Color.argb(110, 126, 211, 210));
        paint.setStrokeWidth(1.5f * scale);
        canvas.drawLine(48f * scale, mid, getWidth() - 48f * scale, mid, paint);

        drawCaster(canvas, getWidth() * 0.08f, fieldTop + 42f * scale, true);
        drawCaster(canvas, getWidth() * 0.92f, fieldBottom - 42f * scale, false);

        if (enemyIntent != null) drawEnemyIntent(canvas);
        for (SpellEntity e : entities) drawEntity(canvas, e);
    }

    private void drawEnemyIntent(Canvas canvas) {
        if (enemyIntent == null) return;
        float progress = clamp(enemyIntent.elapsed / Math.max(0.01f, enemyIntent.totalTime), 0f, 1f);
        float panelLeft = dp(42f);
        float panelRight = getWidth() - dp(42f);
        float panelTop = fieldTop + dp(5f);
        float panelBottom = Math.min(fieldBottom - dp(18f), panelTop + dp(82f));
        RectF panel = new RectF(panelLeft, panelTop, panelRight, panelBottom);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(232, 20, 13, 31));
        canvas.drawRoundRect(panel, dp(16f), dp(16f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(Color.argb(190, 213, 128, 225));
        canvas.drawRoundRect(panel, dp(16f), dp(16f), paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(sp(12f));
        paint.setColor(Color.rgb(232, 190, 238));
        canvas.drawText("RIVAL WRITING • " + laneName(enemyIntent.lane).toUpperCase(Locale.US),
                panel.left + dp(12f), panel.top + dp(19f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(sp(11f));
        paint.setColor(MUTED);
        canvas.drawText(String.format(Locale.US, "%.1fs", Math.max(0f, enemyIntent.totalTime - enemyIntent.elapsed)),
                panel.right - dp(12f), panel.top + dp(19f), paint);

        SpellProgram program = enemyIntent.program;
        int totalTokens = program.elements.size() + 1 + program.clauses.size();
        float tokenGap = dp(6f);
        float usable = panel.width() - dp(24f) - tokenGap * Math.max(0, totalTokens - 1);
        float tokenW = Math.min(dp(58f), usable / Math.max(1, totalTokens));
        float startX = panel.centerX() - (tokenW * totalTokens + tokenGap * Math.max(0, totalTokens - 1)) * 0.5f;
        float tokenTop = panel.top + dp(28f);
        float tokenBottom = panel.bottom - dp(12f);
        int tokenIndex = 0;

        for (int i = 0; i < program.elements.size(); i++, tokenIndex++) {
            float revealStart = 0.06f + i * 0.16f;
            float local = clamp((progress - revealStart) / 0.18f, 0f, 1f);
            RectF r = new RectF(startX + tokenIndex * (tokenW + tokenGap), tokenTop,
                    startX + tokenIndex * (tokenW + tokenGap) + tokenW, tokenBottom);
            drawEnemyElementToken(canvas, r, program.elements.get(i), local);
        }

        float formReveal = clamp((progress - 0.50f) / 0.16f, 0f, 1f);
        RectF formRect = new RectF(startX + tokenIndex * (tokenW + tokenGap), tokenTop,
                startX + tokenIndex * (tokenW + tokenGap) + tokenW, tokenBottom);
        drawEnemyTextToken(canvas, formRect, program.rune.label, formReveal, GOLD);
        tokenIndex++;

        for (int i = 0; i < program.clauses.size(); i++, tokenIndex++) {
            float revealStart = 0.68f + i * 0.12f;
            float local = clamp((progress - revealStart) / 0.14f, 0f, 1f);
            RectF r = new RectF(startX + tokenIndex * (tokenW + tokenGap), tokenTop,
                    startX + tokenIndex * (tokenW + tokenGap) + tokenW, tokenBottom);
            drawEnemyTextToken(canvas, r, program.clauses.get(i).label, local, Color.rgb(213, 128, 225));
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(54, 37, 66));
        RectF track = new RectF(panel.left + dp(12f), panel.bottom - dp(5f), panel.right - dp(12f), panel.bottom - dp(2f));
        canvas.drawRoundRect(track, dp(2f), dp(2f), paint);
        paint.setColor(Color.rgb(213, 128, 225));
        canvas.drawRoundRect(new RectF(track.left, track.top, track.left + track.width() * progress, track.bottom),
                dp(2f), dp(2f), paint);
    }

    private void drawEnemyElementToken(Canvas canvas, RectF r, Element element, float progress) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(progress > 0f ? 210 : 110, 35, 26, 47));
        canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(progress >= 1f ? element.color : Color.argb(90, 213, 128, 225));
        canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        if (progress > 0f) drawMiniGlyph(canvas, element, r, progress);
    }

    private void drawEnemyTextToken(Canvas canvas, RectF r, String text, float progress, int accent) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(progress > 0f ? 210 : 100, 35, 26, 47));
        canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(progress >= 1f ? accent : Color.argb(90, 213, 128, 225));
        canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(10.5f));
        paint.setColor(progress > 0f ? WHITE : Color.rgb(88, 76, 96));
        canvas.drawText(progress > 0f ? text : "?", r.centerX(), r.centerY() + dp(4f), paint);
    }

    private void drawMiniGlyph(Canvas canvas, Element element, RectF r, float progress) {
        float cx = r.centerX();
        float cy = r.centerY();
        float radius = Math.min(r.width(), r.height()) * 0.28f;
        ArrayList<PointF> points = new ArrayList<PointF>();
        if (element == Element.FIRE) {
            points.add(new PointF(cx, cy - radius));
            points.add(new PointF(cx + radius * 0.88f, cy + radius * 0.70f));
            points.add(new PointF(cx - radius * 0.88f, cy + radius * 0.70f));
            points.add(new PointF(cx, cy - radius));
        } else if (element == Element.WATER) {
            for (int i = 0; i <= 24; i++) {
                float t = i / 24f;
                float x = cx - radius + t * radius * 2f;
                float y = cy + (float)Math.sin(t * Math.PI * 3f) * radius * 0.42f;
                points.add(new PointF(x, y));
            }
        } else if (element == Element.WIND) {
            for (int i = 0; i <= 30; i++) {
                float t = i / 30f;
                float a = t * (float)Math.PI * 3.7f;
                float rr = radius * (1f - t * 0.82f);
                points.add(new PointF(cx + (float)Math.cos(a) * rr, cy + (float)Math.sin(a) * rr));
            }
        } else {
            points.add(new PointF(cx - radius, cy - radius));
            points.add(new PointF(cx + radius, cy - radius));
            points.add(new PointF(cx + radius, cy + radius));
            points.add(new PointF(cx - radius, cy + radius));
            points.add(new PointF(cx - radius, cy - radius));
        }
        int last = Math.max(1, Math.min(points.size() - 1, Math.round((points.size() - 1) * progress)));
        Path path = new Path();
        path.moveTo(points.get(0).x, points.get(0).y);
        for (int i = 1; i <= last; i++) path.lineTo(points.get(i).x, points.get(i).y);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(dp(2.6f));
        paint.setColor(element.color);
        canvas.drawPath(path, paint);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawCaster(Canvas canvas, float x, float y, boolean enemy) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(enemy ? Color.rgb(93, 43, 92) : Color.rgb(36, 94, 106));
        Path p = new Path();
        float dir = enemy ? 1f : -1f;
        p.moveTo(x, y - 35f * scale * dir);
        p.lineTo(x + 28f * scale, y + 15f * scale * dir);
        p.lineTo(x + 5f * scale, y + 8f * scale * dir);
        p.lineTo(x - 18f * scale, y + 38f * scale * dir);
        p.lineTo(x - 30f * scale, y - 15f * scale * dir);
        p.close();
        canvas.drawPath(p, paint);
        paint.setColor(GOLD);
        canvas.drawCircle(x, y - 24f * scale * dir, 8f * scale, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f * scale);
        canvas.drawCircle(x, y, 42f * scale, paint);
    }

    private void drawEntity(Canvas canvas, SpellEntity e) {
        if (e.dead) return;
        int primaryColor = e.primary.color;
        int secondaryColor = e.secondary == null ? primaryColor : e.secondary.color;
        if (e.kind == Kind.PROJECTILE) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)));
            canvas.drawCircle(e.x, e.y, e.radius * 2.2f, paint);
            paint.setColor(primaryColor);
            canvas.drawCircle(e.x, e.y, e.radius, paint);
            if (e.secondary != null) {
                paint.setColor(secondaryColor);
                canvas.drawCircle(e.x + e.radius * 0.38f, e.y - e.radius * 0.28f, e.radius * 0.52f, paint);
            }
            paint.setColor(PALE_GOLD);
            canvas.drawCircle(e.x - e.radius * 0.25f, e.y - e.radius * 0.3f, e.radius * 0.22f, paint);
            if (e.empowered) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4f * scale);
                paint.setColor(PALE_GOLD);
                canvas.drawCircle(e.x, e.y, e.radius * 1.55f, paint);
            }
        } else if (e.kind == Kind.WARD) {
            float hpRatio = clamp(e.hp / 40f, 0.12f, 1f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((7f + 5f * hpRatio) * scale);
            paint.setColor(primaryColor);
            RectF r = new RectF(e.x - e.radius, e.y - e.radius * 0.38f,
                    e.x + e.radius, e.y + e.radius * 0.38f);
            canvas.drawArc(r, e.owner == 0 ? 190f : 10f, 160f, false, paint);
            paint.setStrokeWidth(2f * scale);
            paint.setColor(GOLD);
            canvas.drawCircle(e.x, e.y, e.radius * 0.35f, paint);
            if (e.secondary != null) {
                paint.setColor(secondaryColor);
                canvas.drawCircle(e.x, e.y, e.radius * 0.58f, paint);
            }
        } else if (e.kind == Kind.ORBIT) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f * scale);
            paint.setColor(primaryColor);
            canvas.drawCircle(e.x, e.y, e.radius, paint);
            float angle = e.age * 3.1f;
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(e.x + (float)Math.cos(angle) * e.radius,
                    e.y + (float)Math.sin(angle) * e.radius, 9f * scale, paint);
            paint.setColor(secondaryColor);
            canvas.drawCircle(e.x, e.y, 12f * scale, paint);
        } else if (e.kind == Kind.BURST) {
            float pulse = 0.78f + 0.22f * (float)Math.sin(e.age * 10f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4f * scale);
            paint.setColor(primaryColor);
            canvas.drawCircle(e.x, e.y, e.radius * pulse, paint);
            paint.setColor(Color.argb(120, Color.red(secondaryColor), Color.green(secondaryColor), Color.blue(secondaryColor)));
            canvas.drawCircle(e.x, e.y, e.radius * 0.58f * pulse, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(sp(22f));
            paint.setColor(WHITE);
            canvas.drawText(String.format(Locale.US, "%.1f", Math.max(0f, e.timer)), e.x, e.y + 7f * scale, paint);
        } else if (e.kind == Kind.CLOUD) {
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < 6; i++) {
                float a = i * 1.047f + e.age * 0.18f;
                paint.setColor(Color.argb(28, 188, 232, 235));
                canvas.drawCircle(e.x + (float)Math.cos(a) * e.radius * 0.42f,
                        e.y + (float)Math.sin(a) * e.radius * 0.22f,
                        e.radius * 0.48f, paint);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2f * scale);
            paint.setColor(Color.argb(120, 206, 244, 245));
            canvas.drawCircle(e.x, e.y, e.radius * 0.78f, paint);
        } else if (e.kind == Kind.HAZARD) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(45, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)));
            canvas.drawOval(new RectF(e.x - e.radius, e.y - e.radius * 0.34f,
                    e.x + e.radius, e.y + e.radius * 0.34f), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f * scale);
            paint.setColor(primaryColor);
            for (int i = 0; i < 3; i++) {
                float offset = (i - 1) * 27f * scale;
                canvas.drawLine(e.x + offset - 12f * scale, e.y + 16f * scale,
                        e.x + offset + 12f * scale, e.y - 16f * scale, paint);
            }
        }
    }

    private void drawHud(Canvas canvas) {
        drawCombatantHud(canvas, true);
        drawCombatantHud(canvas, false);
        if (bannerTimer > 0f && !banner.isEmpty()) {
            float alpha = clamp(bannerTimer * 2f, 0f, 1f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb((int)(205 * alpha), 9, 8, 19));
            RectF r = new RectF(dp(18f), fieldTop + dp(8f),
                    getWidth() - dp(18f), fieldTop + dp(48f));
            canvas.drawRoundRect(r, dp(18f), dp(18f), paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(14f));
            paint.setColor(Color.argb((int)(255 * alpha), 245, 236, 204));
            canvas.drawText(banner, getWidth() / 2f, r.centerY() + dp(5f), paint);
        }
    }

    private void drawCombatantHud(Canvas canvas, boolean enemy) {
        float left = dp(14f);
        float right = getWidth() - dp(14f);
        float top = enemy ? dp(16f) : castTop - dp(64f);
        float health = enemy ? enemyHealth : playerHealth;
        float mana = enemy ? enemyMana : playerMana;
        float resonance = enemy ? enemyResonance : playerResonance;
        String name = enemy ? "THE VIOLET WARDEN" : "YOU • GOLDEN CIRCLE";
        if (enemy && screen == Screen.DUEL) name += "  •  " + selectedDifficulty.label;

        paint.setTextAlign(enemy ? Paint.Align.LEFT : Paint.Align.RIGHT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(14f));
        paint.setColor(enemy ? Color.rgb(220, 159, 226) : PALE_GOLD);
        canvas.drawText(name, enemy ? left : right, top + dp(14f), paint);
        if (!enemy) {
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(UI_REGULAR);
            paint.setTextSize(sp(11f));
            paint.setColor(MUTED);
            canvas.drawText(selectedArtifact.label, left, top + dp(14f), paint);
        }

        float barTop = top + dp(20f);
        drawBar(canvas, left, barTop, right, barTop + dp(10f), health / 100f, RED, Color.rgb(58, 28, 36));
        drawBar(canvas, left, barTop + dp(15f), right, barTop + dp(23f), mana / 100f, BLUE, Color.rgb(24, 39, 64));
        drawBar(canvas, left, barTop + dp(28f), right, barTop + dp(34f), resonance / 100f, GOLD, Color.rgb(55, 47, 31));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(11f));
        paint.setColor(WHITE);
        canvas.drawText(Math.round(health) + " HP", getWidth() / 2f, barTop + dp(9f), paint);
        if (resonance >= 99.8f) {
            paint.setColor(PALE_GOLD);
            paint.setTextSize(sp(11f));
            canvas.drawText("ASCENDANT READY", getWidth() / 2f, barTop + dp(47f), paint);
        }
    }

    private void drawBar(Canvas canvas, float l, float t, float r, float b, float ratio, int color, int back) {
        ratio = clamp(ratio, 0f, 1f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(back);
        canvas.drawRoundRect(new RectF(l, t, r, b), (b - t) / 2f, (b - t) / 2f, paint);
        paint.setColor(color);
        canvas.drawRoundRect(new RectF(l, t, l + (r - l) * ratio, b), (b - t) / 2f, (b - t) / 2f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(Color.argb(95, 255, 255, 255));
        canvas.drawRoundRect(new RectF(l, t, r, b), (b - t) / 2f, (b - t) / 2f, paint);
    }

    private void drawCastingInterface(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(246, 12, 10, 24));
        canvas.drawRect(0, castTop, getWidth(), getHeight(), paint);
        paint.setColor(GOLD);
        canvas.drawRect(0, castTop, getWidth(), castTop + dp(1f), paint);

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(12f));
        paint.setColor(MUTED);
        canvas.drawText(armedExecutable >= 0 ? "ARMED PROGRAM" : "SPELL SENTENCE", dp(15f), castTop + dp(23f), paint);

        for (int i = 0; i < 3; i++) {
            float cx = dp(120f) + i * dp(34f);
            float cy = castTop + dp(19f);
            Element e = armedExecutable >= 0 && i < deck[armedExecutable].elements.size()
                    ? deck[armedExecutable].elements.get(i)
                    : i < formula.size() ? formula.get(i) : null;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(e == null ? PANEL_LIGHT : e.color);
            canvas.drawCircle(cx, cy, dp(9f), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.2f));
            paint.setColor(e == null ? Color.argb(95, 232, 199, 103) : PALE_GOLD);
            canvas.drawCircle(cx, cy, dp(11f), paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(70, 255, 255, 255));
        canvas.drawRoundRect(clearButton, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(Color.argb(130, 232, 199, 103));
        canvas.drawRoundRect(clearButton, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(11.5f));
        paint.setColor(WHITE);
        canvas.drawText("CLEAR", clearButton.centerX(), clearButton.centerY() + dp(4f), paint);

        for (int i = 0; i < executableRects.length; i++) drawExecutableButton(canvas, executableRects[i], i);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(160, 35, 28, 56));
        canvas.drawRoundRect(drawingPad, dp(22f), dp(22f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(drawing ? PALE_GOLD : Color.argb(115, 232, 199, 103));
        canvas.drawRoundRect(drawingPad, dp(22f), dp(22f), paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        if (currentStroke.isEmpty()) {
            if (armedExecutable >= 0) {
                paint.setTextSize(sp(17f));
                paint.setColor(PALE_GOLD);
                canvas.drawText("PROGRAM READY", drawingPad.centerX(), drawingPad.top + dp(38f), paint);
                paint.setTypeface(UI_REGULAR);
                paint.setTextSize(sp(12f));
                paint.setColor(MUTED);
                canvas.drawText(deck[armedExecutable].shortLabel(), drawingPad.centerX(), drawingPad.top + dp(61f), paint);
                canvas.drawText("Choose a lane to execute", drawingPad.centerX(), drawingPad.top + dp(82f), paint);
            } else if (formula.isEmpty()) {
                paint.setTextSize(sp(19f));
                paint.setColor(Color.argb(220, 245, 241, 232));
                canvas.drawText("DRAW AN ELEMENT", drawingPad.centerX(), drawingPad.top + dp(39f), paint);
                paint.setTypeface(UI_REGULAR);
                paint.setTextSize(sp(13f));
                paint.setColor(Color.argb(175, 220, 210, 222));
                canvas.drawText("Triangle • Wave • Spiral • Square", drawingPad.centerX(), drawingPad.top + dp(64f), paint);
            } else if (formula.size() < 3) {
                paint.setTextSize(sp(16f));
                paint.setColor(PALE_GOLD);
                canvas.drawText("ADD AN ELEMENT OR CONTINUE", drawingPad.centerX(), drawingPad.top + dp(38f), paint);
                paint.setTypeface(UI_REGULAR);
                paint.setTextSize(sp(12f));
                paint.setColor(MUTED);
                canvas.drawText("Attach clauses, then choose lane and form", drawingPad.centerX(), drawingPad.top + dp(61f), paint);
            } else {
                paint.setTextSize(sp(16f));
                paint.setColor(PALE_GOLD);
                canvas.drawText("THREE-ELEMENT CORE READY", drawingPad.centerX(), drawingPad.top + dp(40f), paint);
            }
        }

        if (screen == Screen.TUTORIAL) drawExpectedGlyph(canvas, drawingPad);
        if (!currentStroke.isEmpty()) {
            Path path = new Path();
            path.moveTo(currentStroke.get(0).x, currentStroke.get(0).y);
            for (int i = 1; i < currentStroke.size(); i++) path.lineTo(currentStroke.get(i).x, currentStroke.get(i).y);
            strokePaint.setStrokeWidth(dp(9f));
            strokePaint.setColor(Color.argb(70, 232, 199, 103));
            canvas.drawPath(path, strokePaint);
            strokePaint.setStrokeWidth(dp(4f));
            strokePaint.setColor(PALE_GOLD);
            canvas.drawPath(path, strokePaint);
        }

        for (int i = 0; i < clauseRects.length; i++) drawClauseButton(canvas, clauseRects[i], Clause.values()[i]);
        for (int i = 0; i < laneRects.length; i++) drawLaneButton(canvas, laneRects[i], i);
        for (int i = 0; i < runeRects.length; i++) drawRuneButton(canvas, runeRects[i], Rune.values()[i]);
    }

    private void drawExecutableButton(Canvas canvas, RectF r, int slot) {
        SpellProgram p = deck[slot];
        boolean selected = armedExecutable == slot;
        boolean ready = executableCooldown[slot] <= 0f && playerInk >= 0.99f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? Color.rgb(71, 50, 84) : ready ? Color.rgb(36, 30, 52) : Color.rgb(27, 25, 35));
        canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? dp(2f) : dp(1f));
        paint.setColor(selected ? PALE_GOLD : ready ? Color.argb(145, 232, 199, 103) : Color.rgb(75, 70, 82));
        canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(sp(11f));
        paint.setColor(ready ? WHITE : MUTED);
        canvas.drawText("EXE " + (slot + 1), r.left + dp(8f), r.top + dp(17f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(10.5f));
        paint.setColor(ready ? PALE_GOLD : Color.rgb(104, 98, 110));
        String status = executableCooldown[slot] > 0f ? Math.round(executableCooldown[slot]) + "s" : p.rune.label;
        canvas.drawText(status, r.left + dp(8f), r.top + dp(35f), paint);
        float pipX = r.right - dp(10f);
        for (int i = p.elements.size() - 1; i >= 0; i--) {
            paint.setColor(p.elements.get(i).color);
            canvas.drawCircle(pipX, r.top + dp(16f), dp(4f), paint);
            pipX -= dp(10f);
        }
    }

    private void drawClauseButton(Canvas canvas, RectF r, Clause clause) {
        boolean enabled = !formula.isEmpty() && armedExecutable < 0;
        boolean selected = selectedClauses.contains(clause);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? Color.rgb(68, 49, 79) : enabled ? Color.rgb(34, 29, 49) : Color.rgb(26, 24, 34));
        canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? dp(2f) : dp(1f));
        paint.setColor(selected ? Color.rgb(213, 128, 225) : enabled ? Color.argb(115, 213, 128, 225) : Color.rgb(72, 68, 78));
        canvas.drawRoundRect(r, dp(10f), dp(10f), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(11.5f));
        paint.setColor(selected ? WHITE : enabled ? Color.rgb(218, 210, 224) : Color.rgb(102, 97, 108));
        canvas.drawText(clause.label, r.centerX(), r.top + dp(17f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(10.5f));
        paint.setColor(selected ? Color.rgb(230, 174, 237) : enabled ? MUTED : Color.rgb(87, 82, 93));
        canvas.drawText(clause.subtitle, r.centerX(), r.top + dp(34f), paint);
    }

    private void drawLaneButton(Canvas canvas, RectF r, int lane) {
        boolean enabled = !formula.isEmpty() || armedExecutable >= 0;
        boolean selected = selectedLane == lane;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? Color.rgb(78, 60, 90) : enabled ? PANEL_LIGHT : Color.rgb(28, 26, 36));
        canvas.drawRoundRect(r, dp(13f), dp(13f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? dp(2f) : dp(1f));
        paint.setColor(selected ? PALE_GOLD : enabled ? Color.argb(150, 232, 199, 103) : Color.rgb(76, 70, 82));
        canvas.drawRoundRect(r, dp(13f), dp(13f), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(15f));
        paint.setColor(selected ? WHITE : enabled ? Color.rgb(226, 220, 230) : Color.rgb(105, 101, 111));
        canvas.drawText(lane == 0 ? "LEFT" : lane == 1 ? "CENTER" : "RIGHT",
                r.centerX(), r.centerY() + dp(5f), paint);
        paint.setTypeface(UI_REGULAR);
    }

    private void drawRuneButton(Canvas canvas, RectF r, Rune rune) {
        float cost = playerSpellCost(rune, Math.max(1, formula.size()));
        boolean formulaReady = armedExecutable < 0 && !formula.isEmpty() && selectedLane >= 0;
        boolean affordable = playerMana >= cost || playerResonance >= 100f;
        boolean available = formulaReady && affordable;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(available ? PANEL_LIGHT : Color.rgb(29, 27, 37));
        canvas.drawRoundRect(r, dp(14f), dp(14f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(available ? GOLD : Color.rgb(88, 80, 93));
        canvas.drawRoundRect(r, dp(14f), dp(14f), paint);
        float cx = r.centerX();
        float iconY = r.top + dp(20f);
        drawRuneIcon(canvas, rune, cx, iconY, available ? GOLD : MUTED);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(12.5f));
        paint.setColor(available ? WHITE : MUTED);
        canvas.drawText(rune.label, cx, r.top + dp(48f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(11.5f));
        paint.setColor(available ? MUTED : Color.rgb(100, 95, 108));
        canvas.drawText(Math.round(cost) + " M", cx, r.top + dp(65f), paint);
    }

    private void drawRuneIcon(Canvas canvas, Rune rune, float cx, float cy, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f * density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(color);
        if (rune == Rune.LANCE) {
            canvas.drawLine(cx - 24f * density, cy + 8f * density, cx + 24f * density, cy - 8f * density, paint);
            canvas.drawLine(cx + 24f * density, cy - 8f * density, cx + 10f * density, cy - 12f * density, paint);
            canvas.drawLine(cx + 24f * density, cy - 8f * density, cx + 14f * density, cy + 4f * density, paint);
        } else if (rune == Rune.WARD) {
            RectF a = new RectF(cx - 25f * density, cy - 15f * density, cx + 25f * density, cy + 20f * density);
            canvas.drawArc(a, 195f, 150f, false, paint);
            canvas.drawCircle(cx, cy + 2f * density, 5f * density, paint);
        } else if (rune == Rune.ORBIT) {
            canvas.drawCircle(cx, cy, 18f * density, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx + 18f * density, cy, 6f * density, paint);
            canvas.drawCircle(cx, cy, 5f * density, paint);
        } else {
            canvas.drawCircle(cx, cy, 21f * density, paint);
            canvas.drawLine(cx - 15f * density, cy - 15f * density, cx + 15f * density, cy + 15f * density, paint);
            canvas.drawLine(cx + 15f * density, cy - 15f * density, cx - 15f * density, cy + 15f * density, paint);
        }
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    private void drawExpectedGlyph(Canvas canvas, RectF pad) {
        Element expected = expectedTutorialElement();
        if (expected == null || !currentStroke.isEmpty()) return;
        float cx = pad.centerX();
        float cy = pad.centerY() + dp(10f);
        float size = Math.min(pad.width() * 0.38f, pad.height() * 0.48f);
        Path p = glyphPath(expected, cx, cy, size);
        strokePaint.setStrokeWidth(dp(3f));
        strokePaint.setColor(Color.argb(125, Color.red(expected.color), Color.green(expected.color), Color.blue(expected.color)));
        canvas.drawPath(p, strokePaint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(14f));
        paint.setColor(Color.argb(205, 255, 255, 255));
        canvas.drawText("TRACE " + expected.label.toUpperCase(Locale.US) + " • NO TIME LIMIT", cx,
                Math.min(pad.bottom - dp(16f), cy + size * 0.72f), paint);
    }

    private Path glyphPath(Element element, float cx, float cy, float size) {
        Path p = new Path();
        if (element == Element.FIRE) {
            p.moveTo(cx, cy - size * 0.52f);
            p.lineTo(cx + size * 0.52f, cy + size * 0.46f);
            p.lineTo(cx - size * 0.52f, cy + size * 0.46f);
            p.close();
        } else if (element == Element.STONE) {
            p.moveTo(cx - size * 0.48f, cy - size * 0.48f);
            p.lineTo(cx + size * 0.48f, cy - size * 0.48f);
            p.lineTo(cx + size * 0.48f, cy + size * 0.48f);
            p.lineTo(cx - size * 0.48f, cy + size * 0.48f);
            p.close();
        } else if (element == Element.WATER) {
            p.moveTo(cx - size * 0.55f, cy);
            p.cubicTo(cx - size * 0.42f, cy - size * 0.35f, cx - size * 0.25f, cy + size * 0.35f, cx - size * 0.12f, cy);
            p.cubicTo(cx, cy - size * 0.35f, cx + size * 0.18f, cy + size * 0.35f, cx + size * 0.3f, cy);
            p.cubicTo(cx + size * 0.39f, cy - size * 0.3f, cx + size * 0.48f, cy + size * 0.18f, cx + size * 0.56f, cy);
        } else {
            p.moveTo(cx - size * 0.5f, cy + size * 0.34f);
            p.cubicTo(cx - size * 0.36f, cy - size * 0.4f, cx + size * 0.25f, cy - size * 0.55f, cx + size * 0.48f, cy - size * 0.04f);
            p.cubicTo(cx + size * 0.62f, cy + size * 0.35f, cx + size * 0.1f, cy + size * 0.56f, cx - size * 0.12f, cy + size * 0.12f);
            p.cubicTo(cx - size * 0.24f, cy - size * 0.12f, cx + size * 0.18f, cy - size * 0.22f, cx + size * 0.2f, cy + size * 0.06f);
        }
        return p;
    }

    private void drawTutorial(Canvas canvas) {
        if (tutorialStep == T_FINAL_DUEL) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(PALE_GOLD);
            paint.setTextSize(sp(15f));
            canvas.drawText("FINAL LESSON: DEFEAT THE PATIENT WARDEN", getWidth() / 2f, fieldTop - dp(7f), paint);
            return;
        }
        paint.setStyle(Paint.Style.FILL);
        boolean fullOverlay = tutorialStep == T_INTRO || tutorialStep == T_ELEMENTS
                || tutorialStep == T_RUNES || tutorialStep == T_DEFENSE_INTRO
                || tutorialStep == T_COMPLETE;
        paint.setColor(Color.argb(fullOverlay ? 228 : 180, 7, 6, 15));
        if (fullOverlay) canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(sp(14f));
        paint.setColor(MUTED);
        if (tutorialStep < T_FINAL_DUEL) canvas.drawText("SKIP", getWidth() - dp(18f), dp(34f), paint);

        if (tutorialStep == T_INTRO) {
            drawTutorialCard(canvas, "INITIATION — SPELL SYNTAX",
                    "A spell sentence has an ordered core of up to three ELEMENT glyphs, optional CLAUSES, a target LANE, and a FORM rune. The first element has the strongest influence. Nothing in this initiation advances on a timer, and prepared syntax remains until you cast or clear it.",
                    "TAP TO STUDY THE ELEMENTS");
        } else if (tutorialStep == T_ELEMENTS) {
            drawElementGuideCard(canvas);
        } else if (tutorialStep == T_RUNES) {
            drawRuneGuideCard(canvas);
        } else if (tutorialStep == T_DEFENSE_INTRO) {
            drawTutorialCard(canvas, "DEFENSE — READ, COUNTER, DEPLOY",
                    "An enemy Fire Lance will be suspended in the center lane. Draw Water, choose CENTER, and deploy WARD. Water resists Fire, so the construct takes reduced damage. The projectile will not move until your correct ward is ready.",
                    "TAP TO SUMMON THE PRACTICE LANCE");
        } else if (tutorialStep == T_COMPLETE) {
            drawTutorialCard(canvas, "INITIATION COMPLETE",
                    "You practiced every element and form, attached a Fork clause, and defended a live lane. Outside combat, the Grimoire stores three executable spell programs. Executables cost extra mana and Ink; clean manual glyphs restore Ink. Rival speed and projectile tempo can be changed from the title screen.",
                    "RETURN TO THE DUEL MENU");
        } else {
            String title;
            String body;
            switch (tutorialStep) {
                case T_FIRE_DRAW:
                    title = "1. FIRE — TRACE THE TRIANGLE";
                    body = "Fire deals high damage. It fractures Stone, but Water quenches it. Trace one closed triangle; retry as many times as needed.";
                    break;
                case T_FIRE_CAST:
                    title = "2. LANCE — FAST FORWARD MOTION";
                    body = "Choose CENTER, then LANCE. Lance is the cheapest, fastest projectile and is useful for intercepting enemy spells.";
                    break;
                case T_WATER_DRAW:
                    title = "3. WATER — TRACE THREE CRESTS";
                    body = "Water resists and counters Fire. Fire meeting Water can also create a Steam Veil that slows and weakens projectiles.";
                    break;
                case T_WATER_CAST:
                    title = "4. WARD — A PERSISTENT BLOCKER";
                    body = "Choose LEFT, then WARD. Wards remain in their lane, absorb incoming attacks, and use their element when calculating damage.";
                    break;
                case T_WIND_DRAW:
                    title = "5. WIND — TRACE AN INWARD SPIRAL";
                    body = "Wind travels quickly and scatters Water. Stone grounds Wind. Trace a smooth spiral toward the center.";
                    break;
                case T_WIND_CAST:
                    title = "6. ORBIT — A REPEATING FAMILIAR";
                    body = "Choose RIGHT, then ORBIT. An Orbit is a destructible familiar that repeatedly fires small bolts from its lane.";
                    break;
                case T_STONE_DRAW:
                    title = "7. STONE — TRACE A CLOSED SQUARE";
                    body = "Stone is slow, heavy, and durable. It grounds Wind, but Fire fractures it. Make four clear corners and close the shape.";
                    break;
                case T_STONE_CAST:
                    title = "8. BURST — DELAYED AREA CONTROL";
                    body = "Choose CENTER, then BURST. A Burst marks enemy territory, waits briefly, and detonates across a wide area.";
                    break;
                case T_COMPOUND_FIRE:
                    title = "9. COMPOUND FORMULA — FIRST GLYPH";
                    body = "Draw Fire as the core. Do not choose a lane yet. Later elements modify continuous properties such as heat, impulse, mass, and cohesion.";
                    break;
                case T_COMPOUND_WIND:
                    title = "10. COMPOUND FORMULA — BIND WIND";
                    body = "Now draw Wind. As a modifier it adds impulse and volatility to the Fire core. The same properties affect every form rather than selecting a fixed recipe.";
                    break;
                case T_COMPOUND_CAST:
                    title = "11. ATTACH A CLAUSE";
                    body = "Tap FORK, choose CENTER, then BURST. Fork applies to any compiled spell and sends reduced branches into adjacent lanes.";
                    break;
                case T_DEFENSE_DRAW:
                    title = "12. DEFENSE — DRAW WATER";
                    body = "The Fire Lance is frozen in place. Draw Water. Nothing will advance until the correct glyph is recognized.";
                    break;
                case T_DEFENSE_CAST:
                    title = "13. DEFENSE — CENTER WATER WARD";
                    body = "Choose CENTER, then WARD. Your prepared Water formula cannot dissolve during this lesson.";
                    break;
                case T_DEFENSE_WATCH:
                default:
                    title = "14. WATCH THE ELEMENTAL BLOCK";
                    body = "The lance is moving now. Water resists Fire, so the ward should survive. After the collision, the final practice duel begins automatically.";
                    break;
            }
            drawTutorialStrip(canvas, title, body);
        }
    }

    private void drawElementGuideCard(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        RectF card = new RectF(dp(18f), dp(72f), w - dp(18f), h - dp(52f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PANEL);
        canvas.drawRoundRect(card, dp(20f), dp(20f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(GOLD);
        canvas.drawRoundRect(card, dp(20f), dp(20f), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(21f));
        paint.setColor(PALE_GOLD);
        canvas.drawText("THE FOUR ELEMENTS", w / 2f, card.top + dp(38f), paint);
        paint.setTypeface(UI_REGULAR);
        Element[] elements = Element.values();
        String[] notes = new String[]{
                "Triangle • high damage • beats Stone • loses to Water",
                "Three-crested wave • control • beats Fire • loses to Wind",
                "Inward spiral • high speed • beats Water • loses to Stone",
                "Closed square • heavy and durable • beats Wind • loses to Fire"
        };
        float rowTop = card.top + dp(62f);
        float rowH = (card.height() - dp(108f)) / 4f;
        for (int i = 0; i < elements.length; i++) {
            Element e = elements[i];
            float cy = rowTop + rowH * i + rowH * 0.5f;
            RectF row = new RectF(card.left + dp(12f), rowTop + rowH * i,
                    card.right - dp(12f), rowTop + rowH * (i + 1) - dp(5f));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(80, Color.red(e.color), Color.green(e.color), Color.blue(e.color)));
            canvas.drawRoundRect(row, dp(12f), dp(12f), paint);
            Path glyph = glyphPath(e, row.left + dp(39f), cy, dp(42f));
            strokePaint.setStrokeWidth(dp(3f));
            strokePaint.setColor(e.color);
            canvas.drawPath(glyph, strokePaint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(14f));
            paint.setColor(WHITE);
            canvas.drawText(e.label.toUpperCase(Locale.US), row.left + dp(78f), cy - dp(6f), paint);
            paint.setTypeface(UI_REGULAR);
            drawWrappedText(canvas, notes[i], row.left + dp(78f), cy + dp(10f),
                    row.right - row.left - dp(88f), sp(11.5f), dp(14f),
                    Color.rgb(220, 214, 224), Paint.Align.LEFT);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(sp(14f));
        paint.setColor(GOLD);
        canvas.drawText("TAP TO STUDY THE RUNES", w / 2f, card.bottom - dp(18f), paint);
    }

    private void drawRuneGuideCard(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        RectF card = new RectF(dp(18f), dp(72f), w - dp(18f), h - dp(52f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PANEL);
        canvas.drawRoundRect(card, dp(20f), dp(20f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(GOLD);
        canvas.drawRoundRect(card, dp(20f), dp(20f), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(21f));
        paint.setColor(PALE_GOLD);
        canvas.drawText("FORMS + CLAUSES", w / 2f, card.top + dp(38f), paint);
        paint.setTypeface(UI_REGULAR);
        Rune[] runes = Rune.values();
        String[] notes = new String[]{
                "Fast projectile • cheap • intercepts or pressures a lane",
                "Stationary blocker • absorbs attacks • inherits its element",
                "Destructible familiar • repeatedly fires elemental bolts",
                "Delayed field detonation • broad area control and reactions"
        };
        float rowTop = card.top + dp(62f);
        float rowH = (card.height() - dp(148f)) / 4f;
        for (int i = 0; i < runes.length; i++) {
            Rune rune = runes[i];
            float cy = rowTop + rowH * i + rowH * 0.5f;
            RectF row = new RectF(card.left + dp(12f), rowTop + rowH * i,
                    card.right - dp(12f), rowTop + rowH * (i + 1) - dp(5f));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(34, 29, 52));
            canvas.drawRoundRect(row, dp(12f), dp(12f), paint);
            drawRuneIcon(canvas, rune, row.left + dp(39f), cy - dp(2f), GOLD);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(14f));
            paint.setColor(WHITE);
            canvas.drawText(rune.label + "  •  " + Math.round(rune.baseCost) + " MANA", row.left + dp(78f), cy - dp(6f), paint);
            paint.setTypeface(UI_REGULAR);
            drawWrappedText(canvas, notes[i], row.left + dp(78f), cy + dp(10f),
                    row.right - row.left - dp(88f), sp(11.5f), dp(14f),
                    Color.rgb(220, 214, 224), Paint.Align.LEFT);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_REGULAR);
        drawWrappedText(canvas, "CLAUSES: Echo repeats • Fork branches lanes • Anchor slows and persists",
                w / 2f, card.bottom - dp(54f), card.width() - dp(32f), sp(11.5f), dp(15f),
                Color.rgb(224, 207, 230), Paint.Align.CENTER);
        paint.setTextSize(sp(14f));
        paint.setColor(GOLD);
        canvas.drawText("TAP TO BEGIN GUIDED CASTING", w / 2f, card.bottom - dp(18f), paint);
    }

    private void drawTutorialCard(Canvas canvas, String title, String body, String action) {
        float w = getWidth();
        float h = getHeight();
        float cardHeight = Math.min(dp(350f), h - dp(160f));
        float top = (h - cardHeight) * 0.48f;
        RectF card = new RectF(dp(20f), top, w - dp(20f), top + cardHeight);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PANEL);
        canvas.drawRoundRect(card, dp(22f), dp(22f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(GOLD);
        canvas.drawRoundRect(card, dp(22f), dp(22f), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(21f));
        paint.setColor(PALE_GOLD);
        drawWrappedText(canvas, title, w / 2f, top + dp(48f), card.width() - dp(38f),
                sp(21f), dp(25f), PALE_GOLD, Paint.Align.CENTER);
        paint.setTypeface(UI_REGULAR);
        drawWrappedText(canvas, body, w / 2f, top + dp(112f), card.width() - dp(42f),
                sp(15f), dp(22f), WHITE, Paint.Align.CENTER);
        paint.setTextSize(sp(16f));
        paint.setColor(GOLD);
        canvas.drawText(action, w / 2f, card.bottom - dp(28f), paint);
    }

    private void drawTutorialStrip(Canvas canvas, String title, String body) {
        float top = fieldTop + dp(55f);
        RectF r = new RectF(dp(12f), top, getWidth() - dp(12f), top + dp(116f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(235, 22, 18, 38));
        canvas.drawRoundRect(r, dp(16f), dp(16f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(GOLD);
        canvas.drawRoundRect(r, dp(16f), dp(16f), paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(16f));
        paint.setColor(PALE_GOLD);
        canvas.drawText(title, r.left + dp(15f), r.top + dp(26f), paint);
        paint.setTypeface(UI_REGULAR);
        drawWrappedText(canvas, body, r.left + dp(15f), r.top + dp(49f),
                r.width() - dp(30f), sp(13f), dp(18f), WHITE, Paint.Align.LEFT);
    }

    private void drawGameOver(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(225, 6, 5, 14));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        float w = getWidth();
        float h = getHeight();
        float top = dp(105f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(Math.min(sp(42f), w * 0.13f));
        paint.setColor(resultTitle.equals("VICTORY") ? PALE_GOLD : Color.rgb(235, 119, 132));
        canvas.drawText(resultTitle, w / 2f, top, paint);
        paint.setTypeface(UI_REGULAR);
        drawWrappedText(canvas, resultSubtitle, w / 2f, top + dp(38f), w - dp(48f),
                sp(15f), dp(21f), WHITE, Paint.Align.CENTER);

        RectF stats = new RectF(dp(18f), top + dp(105f), w - dp(18f), top + dp(205f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PANEL);
        canvas.drawRoundRect(stats, dp(18f), dp(18f), paint);
        String[] labels = new String[]{"SIGILS", "REACTIONS", "DAMAGE"};
        int[] values = new int[]{sigilsCast, reactionsWon, damageDealt};
        for (int i = 0; i < 3; i++) {
            float cx = stats.left + stats.width() * (i * 2f + 1f) / 6f;
            paint.setTextSize(sp(11f));
            paint.setColor(MUTED);
            canvas.drawText(labels[i], cx, stats.top + dp(29f), paint);
            paint.setTextSize(sp(28f));
            paint.setColor(PALE_GOLD);
            canvas.drawText(String.valueOf(values[i]), cx, stats.top + dp(72f), paint);
        }

        float buttonW = Math.min(w - dp(34f), dp(340f));
        float buttonH = dp(56f);
        float buttonTop = Math.max(stats.bottom + dp(35f), h - dp(154f));
        mainButton.set((w - buttonW) / 2f, buttonTop, (w + buttonW) / 2f, buttonTop + buttonH);
        secondaryButton.set((w - buttonW) / 2f, mainButton.bottom + dp(10f),
                (w + buttonW) / 2f, mainButton.bottom + dp(10f) + buttonH);
        drawButton(canvas, mainButton, "DUEL AGAIN", true);
        drawButton(canvas, secondaryButton, "RETURN TO TITLE", false);
    }

    private void drawButton(Canvas canvas, RectF r, String label, boolean primary) {
        if (label.isEmpty()) return;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(primary ? GOLD : PANEL_LIGHT);
        canvas.drawRoundRect(r, dp(16f), dp(16f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(primary ? PALE_GOLD : GOLD);
        canvas.drawRoundRect(r, dp(16f), dp(16f), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(17f));
        paint.setColor(primary ? INK : WHITE);
        canvas.drawText(label, r.centerX(), r.centerY() + dp(6f), paint);
    }

    private void drawDecorativeFrame(Canvas canvas, float l, float t, float r, float b) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(Color.argb(125, 232, 199, 103));
        canvas.drawRoundRect(new RectF(l, t, r, b), dp(18f), dp(18f), paint);
        float c = dp(22f);
        canvas.drawLine(l, t + c, l + c, t, paint);
        canvas.drawLine(r - c, t, r, t + c, paint);
        canvas.drawLine(l, b - c, l + c, b, paint);
        canvas.drawLine(r - c, b, r, b - c, paint);
    }

    private void drawFloatingText(Canvas canvas) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(16f));
        for (FloatingText f : floatingTexts) {
            float a = clamp(f.life / 0.45f, 0f, 1f);
            paint.setColor(Color.argb((int)(255 * a), Color.red(f.color), Color.green(f.color), Color.blue(f.color)));
            canvas.drawText(f.text, f.x, f.y, paint);
        }
    }

    private void drawWrappedText(Canvas canvas, String text, float x, float y, float maxWidth,
                                 float textSize, float lineHeight, int color, Paint.Align align) {
        paint.setTextSize(textSize);
        paint.setColor(color);
        paint.setTextAlign(align);
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float yy = y;
        for (String word : words) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(test) > maxWidth && line.length() > 0) {
                canvas.drawText(line.toString(), x, yy, paint);
                yy += lineHeight;
                line = new StringBuilder(word);
            } else line = new StringBuilder(test);
        }
        if (line.length() > 0) canvas.drawText(line.toString(), x, yy, paint);
    }

    private float pathLength(List<PointF> pts) {
        float len = 0f;
        for (int i = 1; i < pts.size(); i++) len += distance(pts.get(i - 1).x, pts.get(i - 1).y, pts.get(i).x, pts.get(i).y);
        return len;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1, dy = y2 - y1;
        return (float)Math.sqrt(dx * dx + dy * dy);
    }

    private float dp(float value) { return value * density; }

    private float sp(float value) { return value * scaledDensity; }

    private float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }

    private static final class FloatingText {
        final String text;
        final float x;
        float y;
        final int color;
        float life;
        FloatingText(String text, float x, float y, int color, float life) {
            this.text = text; this.x = x; this.y = y; this.color = color; this.life = life;
        }
    }

    private final class SpellEntity {
        final Kind kind;
        final int owner;
        int lane;
        final Element primary;
        final Element secondary;
        Element tertiary;
        float x, y, velocity, radius, damage, hp, life, timer, age;
        float heat, moisture, impulse, mass, cohesion, volatility, vapor, molten, chill, fracture;
        boolean dead, empowered, split, anchored;
        String combo;
        SpellEntity(Kind kind, int owner, int lane, Element primary, Element secondary) {
            this.kind = kind; this.owner = owner; this.lane = lane;
            this.primary = primary; this.secondary = secondary;
        }
        SpellEntity copyProjectile() {
            SpellEntity p = new SpellEntity(Kind.PROJECTILE, owner, lane, primary, secondary);
            p.x = x; p.y = y; p.velocity = velocity; p.radius = radius; p.damage = damage;
            p.empowered = empowered; p.combo = combo; p.split = split; p.anchored = anchored;
            p.tertiary = tertiary; p.heat = heat; p.moisture = moisture; p.impulse = impulse;
            p.mass = mass; p.cohesion = cohesion; p.volatility = volatility; p.vapor = vapor;
            p.molten = molten; p.chill = chill; p.fracture = fracture;
            return p;
        }
    }

    private static final class CompiledProfile {
        float heat, moisture, impulse, mass, cohesion, volatility, vapor, molten, chill, fracture;
        String label = "";
    }

    private static final class SpellProgram {
        final ArrayList<Element> elements = new ArrayList<Element>();
        Rune rune = Rune.LANCE;
        final ArrayList<Clause> clauses = new ArrayList<Clause>();
        SpellProgram() {}
        SpellProgram(List<Element> elements, Rune rune, List<Clause> clauses) {
            this.elements.addAll(elements); this.rune = rune; this.clauses.addAll(clauses);
        }
        SpellProgram copy() { return new SpellProgram(elements, rune, clauses); }
        String shortLabel() {
            StringBuilder s = new StringBuilder();
            for (Element e : elements) { if (s.length() > 0) s.append('·'); s.append(e.label.substring(0, 1)); }
            if (s.length() == 0) s.append('?');
            return s.toString() + " " + rune.label;
        }
    }

    private static final class PendingCast {
        final int owner, lane; final SpellProgram program; final float quality, powerScale; final boolean empowered; float delay;
        PendingCast(int owner, SpellProgram program, int lane, float quality, boolean empowered, float delay, float powerScale) {
            this.owner = owner; this.program = program; this.lane = lane; this.quality = quality;
            this.empowered = empowered; this.delay = delay; this.powerScale = powerScale;
        }
    }

    private static final class EnemyCastIntent {
        final SpellProgram program; final int lane; final float quality, totalTime; final boolean empowered; float elapsed;
        EnemyCastIntent(SpellProgram program, int lane, float quality, boolean empowered, float totalTime) {
            this.program = program; this.lane = lane; this.quality = quality; this.empowered = empowered; this.totalTime = totalTime;
        }
        float progress() { return Math.max(0f, Math.min(1f, elapsed / Math.max(0.01f, totalTime))); }
    }

    private static final class GlyphResult {
        final Element element;
        final float score;
        GlyphResult(Element element, float score) { this.element = element; this.score = score; }
    }

    /** Lightweight template recognizer inspired by the $1 unistroke recognizer. */
    private static final class GlyphRecognizer {
        private static final int SAMPLE_COUNT = 48;
        private final ArrayList<Template> templates = new ArrayList<Template>();

        GlyphRecognizer() {
            templates.add(new Template(Element.FIRE, polyline(new float[][]{
                    {0.5f, 0f}, {1f, 1f}, {0f, 1f}, {0.5f, 0f}})));
            templates.add(new Template(Element.STONE, polyline(new float[][]{
                    {0f, 0f}, {1f, 0f}, {1f, 1f}, {0f, 1f}, {0f, 0f}})));
            templates.add(new Template(Element.WATER, polyline(new float[][]{
                    {0f, 0.5f}, {0.05f, 0.32f}, {0.10f, 0.18f}, {0.16f, 0.22f},
                    {0.22f, 0.50f}, {0.28f, 0.75f}, {0.34f, 0.65f}, {0.38f, 0.40f},
                    {0.43f, 0.20f}, {0.49f, 0.25f}, {0.55f, 0.50f}, {0.61f, 0.75f},
                    {0.67f, 0.65f}, {0.72f, 0.40f}, {0.78f, 0.20f}, {0.84f, 0.30f},
                    {0.90f, 0.55f}, {0.96f, 0.58f}, {1f, 0.5f}})));
            templates.add(new Template(Element.WIND, polyline(new float[][]{
                    {0.02f, 0.78f}, {0.16f, 0.36f}, {0.38f, 0.08f}, {0.68f, 0.08f},
                    {0.93f, 0.35f}, {0.88f, 0.68f}, {0.62f, 0.88f}, {0.34f, 0.72f},
                    {0.31f, 0.43f}, {0.53f, 0.3f}, {0.69f, 0.46f}, {0.59f, 0.62f},
                    {0.45f, 0.54f}})));
        }

        GlyphResult recognize(List<PointF> raw) {
            List<PointF> candidate = normalize(resample(raw, SAMPLE_COUNT));
            List<PointF> reversedRaw = new ArrayList<PointF>(raw);
            Collections.reverse(reversedRaw);
            List<PointF> reversed = normalize(resample(reversedRaw, SAMPLE_COUNT));
            Template best = templates.get(0);
            float bestDistance = Float.MAX_VALUE;
            for (Template t : templates) {
                float d = Math.min(pathDistance(candidate, t.points), pathDistance(reversed, t.points));
                if (d < bestDistance) { bestDistance = d; best = t; }
            }
            float score = 1f - bestDistance / 0.72f;
            score = Math.max(0f, Math.min(1f, score));
            return new GlyphResult(best.element, score);
        }

        private static List<PointF> polyline(float[][] anchors) {
            ArrayList<PointF> points = new ArrayList<PointF>();
            for (float[] a : anchors) points.add(new PointF(a[0], a[1]));
            return normalize(resample(points, SAMPLE_COUNT));
        }

        private static List<PointF> resample(List<PointF> pts, int count) {
            ArrayList<PointF> source = new ArrayList<PointF>();
            for (PointF p : pts) source.add(new PointF(p.x, p.y));
            if (source.size() == 1) {
                ArrayList<PointF> repeated = new ArrayList<PointF>();
                for (int i = 0; i < count; i++) repeated.add(new PointF(source.get(0).x, source.get(0).y));
                return repeated;
            }
            float length = 0f;
            for (int i = 1; i < source.size(); i++) length += dist(source.get(i - 1), source.get(i));
            float interval = length / (count - 1);
            float accumulated = 0f;
            ArrayList<PointF> out = new ArrayList<PointF>();
            out.add(new PointF(source.get(0).x, source.get(0).y));
            int i = 1;
            while (i < source.size() && out.size() < count) {
                PointF prev = source.get(i - 1);
                PointF cur = source.get(i);
                float segment = dist(prev, cur);
                if (segment <= 0.0001f) { i++; continue; }
                if (accumulated + segment >= interval) {
                    float t = (interval - accumulated) / segment;
                    PointF q = new PointF(prev.x + t * (cur.x - prev.x), prev.y + t * (cur.y - prev.y));
                    out.add(q);
                    source.add(i, q);
                    accumulated = 0f;
                    i++;
                } else {
                    accumulated += segment;
                    i++;
                }
            }
            while (out.size() < count) {
                PointF last = source.get(source.size() - 1);
                out.add(new PointF(last.x, last.y));
            }
            return out;
        }

        private static List<PointF> normalize(List<PointF> pts) {
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            for (PointF p : pts) {
                minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
                minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
            }
            float width = Math.max(0.001f, maxX - minX);
            float height = Math.max(0.001f, maxY - minY);
            ArrayList<PointF> out = new ArrayList<PointF>();
            float cx = 0f, cy = 0f;
            for (PointF p : pts) {
                PointF q = new PointF((p.x - minX) / width, (p.y - minY) / height);
                out.add(q); cx += q.x; cy += q.y;
            }
            cx /= out.size(); cy /= out.size();
            for (PointF q : out) { q.x -= cx; q.y -= cy; }
            return out;
        }

        private static float pathDistance(List<PointF> a, List<PointF> b) {
            float total = 0f;
            int n = Math.min(a.size(), b.size());
            for (int i = 0; i < n; i++) total += dist(a.get(i), b.get(i));
            return total / n;
        }

        private static float dist(PointF a, PointF b) {
            float dx = b.x - a.x, dy = b.y - a.y;
            return (float)Math.sqrt(dx * dx + dy * dy);
        }

        private static final class Template {
            final Element element;
            final List<PointF> points;
            Template(Element element, List<PointF> points) { this.element = element; this.points = points; }
        }
    }
}
