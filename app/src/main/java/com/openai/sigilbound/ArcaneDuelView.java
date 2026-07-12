package com.openai.sigilbound;

import static com.openai.sigilbound.SpellSystem.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Sigilbound v2.0: The Living Weave.
 *
 * A compact three-lane real-time duel where every lane owns a contestable ley
 * node, a rolled lane trait, and persistent field state, and where the arena
 * itself periodically intervenes. Spell sentences support four ordered sigils,
 * nine forms, and three ordered clauses; persistent objects (wards, glyphs,
 * rifts, auras, traps, fields) keep modifying each other after they resolve.
 * The UI is a guided five-stage composer: Sigils -> Form -> Clauses -> Lane -> Cast.
 */
public final class ArcaneDuelView extends View {
    /** Entity owner index used for neutral environmental hazards (hostile to both duelists). */
    private static final int OWNER_ENVIRONMENT = 2;
    private static final int VOID_BLACK = Color.rgb(7, 7, 14);
    private static final int DEEP_INK = Color.rgb(13, 12, 24);
    private static final int PANEL = Color.rgb(24, 22, 38);
    private static final int PANEL_HIGH = Color.rgb(39, 35, 56);
    private static final int PANEL_SOFT = Color.rgb(31, 28, 46);
    private static final int GOLD = Color.rgb(218, 176, 86);
    private static final int BRIGHT_GOLD = Color.rgb(255, 225, 146);
    private static final int OLD_GOLD = Color.rgb(116, 82, 35);
    private static final int IVORY = Color.rgb(246, 242, 231);
    private static final int MUTED = Color.rgb(168, 160, 181);
    private static final int SUCCESS = Color.rgb(112, 224, 169);
    private static final int DANGER = Color.rgb(238, 82, 93);
    private static final int INK_BLUE = Color.rgb(83, 153, 231);

    private static final Typeface UI_REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Typeface UI_MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);

    private enum Screen { TITLE, GRIMOIRE, CODEX, DUEL, GAME_OVER }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private final GlyphRecognizer recognizer = new GlyphRecognizer();
    private final SharedPreferences preferences;

    private final ArrayList<PointF> currentStroke = new ArrayList<PointF>();
    private final ArrayList<Program> library = new ArrayList<Program>();
    private final ArrayList<Float> executableCooldowns = new ArrayList<Float>();
    private final ArrayList<Entity> entities = new ArrayList<Entity>();
    private final ArrayList<PendingCast> pendingCasts = new ArrayList<PendingCast>();
    private final ArrayList<Particle> particles = new ArrayList<Particle>();
    private final ArrayList<FloatingText> floatingTexts = new ArrayList<FloatingText>();
    private final ArrayList<Shockwave> shockwaves = new ArrayList<Shockwave>();
    private final LeyNode[] nodes = {new LeyNode(), new LeyNode(), new LeyNode()};
    private final LaneTrait[] laneTraits = {LaneTrait.LEY_CURRENT, LaneTrait.BEDROCK, LaneTrait.WELLSPRING};

    private final RectF mainButton = new RectF();
    private final RectF secondaryButton = new RectF();
    private final RectF grimoireButton = new RectF();
    private final RectF codexButton = new RectF();
    private final RectF[] artifactRects = createRects(7);
    private final RectF[] difficultyRects = createRects(3);
    private final RectF[] tempoRects = createRects(3);
    private final RectF[] arenaRects = createRects(6);

    private final RectF composerPanel = new RectF();
    private final RectF[] stepRects = createRects(5);
    private final RectF executableRibbon = new RectF();
    private final RectF[] executableCardRects = createRects(4);
    private final RectF executablePrev = new RectF();
    private final RectF executableNext = new RectF();
    private final RectF drawingPad = new RectF();
    private final RectF undoButton = new RectF();
    private final RectF clearButton = new RectF();
    private final RectF continueButton = new RectF();
    private final RectF backStepButton = new RectF();
    private final RectF[] formRects = createRects(9);
    private final RectF[] clauseRects = createRects(12);
    private final RectF[] laneRects = createRects(3);
    private final RectF castButton = new RectF();

    private final RectF grimoireList = new RectF();
    private final RectF[] grimoireCards = createRects(4);
    private final RectF grimoireUp = new RectF();
    private final RectF grimoireDown = new RectF();
    private final RectF grimoireAdd = new RectF();
    private final RectF grimoireDuplicate = new RectF();
    private final RectF grimoireDelete = new RectF();
    private final RectF grimoireSave = new RectF();
    private final RectF grimoireBack = new RectF();
    private final RectF grimoireUndo = new RectF();
    private final RectF[] editorElementRects = createRects(10);
    private final RectF[] editorFormRects = createRects(9);
    private final RectF[] editorClauseRects = createRects(12);

    private final RectF codexBack = new RectF();
    private final RectF codexPrev = new RectF();
    private final RectF codexNext = new RectF();
    private final RectF codexPractice = new RectF();

    private Screen screen = Screen.TITLE;
    private Shader backgroundShader;
    private float density = 1f;
    private float scaledDensity = 1f;
    private float scale = 1f;
    private float fieldTop;
    private float fieldBottom;
    private float composerTop;
    private final float[] laneX = new float[3];
    private float nodeY;
    private boolean running = true;
    private long lastFrameNanos;

    private Artifact selectedArtifact;
    private Difficulty selectedDifficulty;
    private Tempo selectedTempo;
    private ArenaType selectedArena;

    private Program composer = new Program();
    private int composerStep;
    private int selectedLane = -1;
    private int loadedExecutable = -1;
    private int executablePage;
    private boolean drawing;
    private float lastGlyphQuality = 0.75f;

    private int grimoireScroll;
    private int editingProgram = -1;
    private Program editorProgram = new Program();
    private int codexPage;

    private float playerHealth;
    private float enemyHealth;
    private float playerMana;
    private float enemyMana;
    private float playerResonance;
    private float enemyResonance;
    private float playerInk;
    private float aiCooldown;
    private float duelClock;
    private float uiClock;
    private float playerBurn;
    private float enemyBurn;
    private float playerSlow;
    private float enemySlow;
    private EnemyIntent enemyIntent;
    private boolean practiceMode;

    private float arenaEventTimer;
    private float tideSurgeTimer;
    private int lightShaftLane = -1;
    private float lightShaftTimer;
    private float shakeTimer;
    private float shakeMagnitude;

    private String banner = "";
    private float bannerTimer;
    private String resultTitle = "";
    private String resultSubtitle = "";
    private int damageDealt;
    private int reactionsWon;
    private int manualCasts;

    public ArcaneDuelView(Context context) {
        super(context);
        setFocusable(true);
        setKeepScreenOn(true);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
        preferences = context.getSharedPreferences("sigilbound_v14", Context.MODE_PRIVATE);

        selectedArtifact = Artifact.values()[safeIndex(preferences.getInt("artifact", 0), Artifact.values().length)];
        selectedDifficulty = Difficulty.values()[safeIndex(preferences.getInt("difficulty", 0), Difficulty.values().length)];
        selectedTempo = Tempo.values()[safeIndex(preferences.getInt("tempo", 0), Tempo.values().length)];
        selectedArena = ArenaType.values()[safeIndex(preferences.getInt("arena", 0), ArenaType.values().length)];
        loadLibrary();

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        resetComposer();
        resetCombat();
    }

    private static RectF[] createRects(int count) {
        RectF[] result = new RectF[count];
        for (int i = 0; i < count; i++) result[i] = new RectF();
        return result;
    }

    private static int safeIndex(int index, int length) {
        return Math.max(0, Math.min(length - 1, index));
    }

    private void loadLibrary() {
        library.clear();
        executableCooldowns.clear();
        Program[] defaults = {
                program(new Element[]{Element.FIRE, Element.WIND, Element.AETHER}, Form.LANCE,
                        new Clause[]{Clause.SEEK, Clause.RELAY}),
                program(new Element[]{Element.WATER, Element.FROST, Element.STONE}, Form.WARD,
                        new Clause[]{Clause.ANCHOR, Clause.BIND}),
                program(new Element[]{Element.LIGHTNING, Element.WATER}, Form.BURST,
                        new Clause[]{Clause.TRIGGER, Clause.FORK}),
                program(new Element[]{Element.VOID, Element.AETHER}, Form.GLYPH,
                        new Clause[]{Clause.CONSUME, Clause.ECHO}),
                program(new Element[]{Element.STONE, Element.WIND, Element.FROST}, Form.ORBIT,
                        new Clause[]{Clause.FORK, Clause.ANCHOR}),
                program(new Element[]{Element.RADIANCE, Element.AETHER}, Form.AURA,
                        new Clause[]{Clause.ANCHOR}),
                program(new Element[]{Element.VERDANCE, Element.WATER}, Form.SURGE,
                        new Clause[]{Clause.SWIFT}),
                program(new Element[]{Element.VOID, Element.LIGHTNING}, Form.LANCE,
                        new Clause[]{Clause.DISPEL, Clause.SEEK})
        };
        int count = preferences.getInt("program_count", -1);
        if (count < 0) {
            Collections.addAll(library, defaults);
            saveLibrary();
        } else {
            for (int i = 0; i < count; i++) {
                Program fallback = defaults[i % defaults.length];
                library.add(decode(preferences.getString("program_" + i, ""), fallback));
            }
            if (library.isEmpty()) Collections.addAll(library, defaults);
        }
        for (int i = 0; i < library.size(); i++) executableCooldowns.add(0f);
    }

    private void saveLibrary() {
        SharedPreferences.Editor editor = preferences.edit();
        int oldCount = preferences.getInt("program_count", 0);
        editor.putInt("program_count", library.size());
        for (int i = 0; i < library.size(); i++) editor.putString("program_" + i, SpellSystem.encode(library.get(i)));
        for (int i = library.size(); i < oldCount; i++) editor.remove("program_" + i);
        editor.apply();
        syncCooldownList();
    }

    private void syncCooldownList() {
        while (executableCooldowns.size() < library.size()) executableCooldowns.add(0f);
        while (executableCooldowns.size() > library.size()) executableCooldowns.remove(executableCooldowns.size() - 1);
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
        float margin = dp(12f);

        float composerMinimum = dp(250f);
        float composerMaximum = Math.max(composerMinimum, h - dp(390f));
        composerTop = clamp(h * 0.45f, composerMinimum, composerMaximum);
        fieldTop = dp(88f);
        fieldBottom = composerTop - dp(8f);
        nodeY = (fieldTop + fieldBottom) * 0.52f;
        laneX[0] = w * 0.20f;
        laneX[1] = w * 0.50f;
        laneX[2] = w * 0.80f;
        composerPanel.set(margin, composerTop, w - margin, h - margin);

        layoutTitle(w, h, margin);
        layoutComposer(w, h, margin);
        layoutGrimoire(w, h, margin);
        layoutCodex(w, h, margin);

        backgroundShader = new LinearGradient(0, 0, 0, h,
                new int[]{VOID_BLACK, selectedArena.shadow, DEEP_INK},
                new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP);
    }

    private void layoutTitle(int w, int h, float margin) {
        float y = dp(148f);
        layoutGrid(artifactRects, margin, y, w - margin, y + dp(94f), 4, 2, dp(6f));
        y += dp(110f);
        layoutRow(difficultyRects, margin, y, w - margin, dp(42f), dp(8f));
        y += dp(56f);
        layoutRow(tempoRects, margin, y, w - margin, dp(42f), dp(8f));
        y += dp(56f);
        layoutGrid(arenaRects, margin, y, w - margin, y + dp(94f), 3, 2, dp(6f));

        float buttonY = h - dp(154f);
        mainButton.set(margin, buttonY, w - margin, buttonY + dp(58f));
        secondaryButton.set(margin, buttonY + dp(66f), w * 0.50f - dp(4f), buttonY + dp(116f));
        grimoireButton.set(w * 0.50f + dp(4f), buttonY + dp(66f), w - margin, buttonY + dp(116f));
        codexButton.set(w - dp(70f), dp(12f), w - margin, dp(54f));
    }

    private void layoutComposer(int w, int h, float margin) {
        float left = composerPanel.left + dp(10f);
        float right = composerPanel.right - dp(10f);
        float top = composerPanel.top + dp(9f);
        layoutRow(stepRects, left, top, right, dp(34f), dp(5f));
        top += dp(40f);

        executableRibbon.set(left, top, right, top + dp(50f));
        executablePrev.set(left, top, left + dp(34f), top + dp(50f));
        executableNext.set(right - dp(34f), top, right, top + dp(50f));
        float cardLeft = executablePrev.right + dp(5f);
        float cardRight = executableNext.left - dp(5f);
        layoutRow(executableCardRects, cardLeft, top, cardRight, dp(50f), dp(5f));
        top += dp(58f);

        float bottom = composerPanel.bottom - dp(10f);
        backStepButton.set(left, bottom - dp(48f), left + dp(76f), bottom);
        continueButton.set(right - dp(118f), bottom - dp(48f), right, bottom);
        castButton.set(left + dp(84f), bottom - dp(48f), right, bottom);
        clearButton.set(right - dp(78f), top, right, top + dp(38f));
        undoButton.set(left, top, left + dp(72f), top + dp(38f));

        drawingPad.set(left, top + dp(44f), right, bottom - dp(56f));
        layoutGrid(formRects, left, top + dp(12f), right, bottom - dp(56f), 3, 3, dp(7f));
        layoutGrid(clauseRects, left, top + dp(8f), right, bottom - dp(56f), 3, 4, dp(6f));
        layoutRow(laneRects, left, top + dp(38f), right, dp(92f), dp(9f));
    }

    private void layoutGrimoire(int w, int h, float margin) {
        grimoireBack.set(margin, dp(16f), margin + dp(72f), dp(58f));
        grimoireList.set(margin, dp(78f), w - margin, dp(248f));
        layoutGrid(grimoireCards, margin + dp(4f), dp(86f), w - margin - dp(4f), dp(206f), 2, 2, dp(7f));

        float actionTop = dp(212f);
        float usable = w - margin * 2f;
        float gap = dp(5f);
        float small = dp(42f);
        float add = dp(70f);
        float delete = dp(70f);
        float duplicate = Math.max(dp(82f), usable - small * 2f - add - delete - gap * 4f);
        float x = margin;
        grimoireUp.set(x, actionTop, x + small, actionTop + dp(30f)); x += small + gap;
        grimoireDown.set(x, actionTop, x + small, actionTop + dp(30f)); x += small + gap;
        grimoireAdd.set(x, actionTop, x + add, actionTop + dp(30f)); x += add + gap;
        grimoireDuplicate.set(x, actionTop, x + duplicate, actionTop + dp(30f)); x += duplicate + gap;
        grimoireDelete.set(x, actionTop, w - margin, actionTop + dp(30f));

        float editorTop = dp(258f);
        layoutGrid(editorElementRects, margin, editorTop + dp(48f), w - margin, editorTop + dp(122f), 5, 2, dp(5f));
        layoutGrid(editorFormRects, margin, editorTop + dp(148f), w - margin, editorTop + dp(258f), 3, 3, dp(5f));
        layoutGrid(editorClauseRects, margin, editorTop + dp(284f), w - margin, editorTop + dp(394f), 4, 3, dp(5f));
        float actionY = h - dp(64f);
        grimoireUndo.set(margin, actionY, margin + dp(86f), actionY + dp(48f));
        grimoireSave.set(margin + dp(94f), actionY, w - margin, actionY + dp(48f));
    }

    private void layoutCodex(int w, int h, float margin) {
        codexBack.set(margin, dp(16f), margin + dp(72f), dp(58f));
        codexPrev.set(margin, h - dp(66f), margin + dp(88f), h - dp(18f));
        codexNext.set(w - margin - dp(88f), h - dp(66f), w - margin, h - dp(18f));
        codexPractice.set(margin + dp(98f), h - dp(66f), w - margin - dp(98f), h - dp(18f));
    }

    private void layoutRow(RectF[] rects, float left, float top, float right, float height, float gap) {
        float width = (right - left - gap * (rects.length - 1)) / rects.length;
        for (int i = 0; i < rects.length; i++) {
            float x = left + i * (width + gap);
            rects[i].set(x, top, x + width, top + height);
        }
    }

    private void layoutGrid(RectF[] rects, float left, float top, float right, float bottom,
                            int columns, int rows, float gap) {
        float width = (right - left - gap * (columns - 1)) / columns;
        float height = (bottom - top - gap * (rows - 1)) / rows;
        for (int i = 0; i < rects.length; i++) {
            int column = i % columns;
            int row = i / columns;
            float x = left + column * (width + gap);
            float y = top + row * (height + gap);
            rects[i].set(x, y, x + width, y + height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0L ? 0f : Math.min(0.035f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        if (running) update(dt);

        drawBackground(canvas);
        if (screen == Screen.TITLE) drawTitle(canvas);
        else if (screen == Screen.GRIMOIRE) drawGrimoire(canvas);
        else if (screen == Screen.CODEX) drawCodex(canvas);
        else {
            boolean shaking = shakeTimer > 0f && screen == Screen.DUEL;
            if (shaking) {
                canvas.save();
                float falloff = shakeTimer / 0.28f;
                canvas.translate((random.nextFloat() - 0.5f) * 2f * shakeMagnitude * falloff,
                        (random.nextFloat() - 0.5f) * 2f * shakeMagnitude * falloff);
            }
            drawArena(canvas);
            drawShockwaves(canvas);
            if (shaking) canvas.restore();
            drawHud(canvas);
            drawEnemyIntent(canvas);
            drawComposer(canvas);
            drawParticles(canvas);
            drawFloatingText(canvas);
            if (screen == Screen.GAME_OVER) drawGameOver(canvas);
        }
        if (running) postInvalidateOnAnimation();
    }

    private void update(float dt) {
        uiClock += dt;
        if (bannerTimer > 0f) bannerTimer -= dt;
        for (Iterator<Particle> iterator = particles.iterator(); iterator.hasNext();) {
            Particle particle = iterator.next();
            particle.life -= dt;
            particle.x += particle.vx * dt;
            particle.y += particle.vy * dt;
            particle.vx *= 0.985f;
            particle.vy *= 0.985f;
            if (particle.life <= 0f) iterator.remove();
        }
        for (Iterator<FloatingText> iterator = floatingTexts.iterator(); iterator.hasNext();) {
            FloatingText text = iterator.next();
            text.life -= dt;
            text.y -= dp(19f) * dt;
            if (text.life <= 0f) iterator.remove();
        }
        for (Iterator<Shockwave> iterator = shockwaves.iterator(); iterator.hasNext();) {
            Shockwave wave = iterator.next();
            wave.life -= dt;
            wave.radius += wave.speed * dt;
            if (wave.life <= 0f) iterator.remove();
        }
        if (shakeTimer > 0f) shakeTimer = Math.max(0f, shakeTimer - dt);
        for (int i = 0; i < executableCooldowns.size(); i++) {
            if (executableCooldowns.get(i) > 0f) executableCooldowns.set(i, Math.max(0f, executableCooldowns.get(i) - dt));
        }
        updatePendingCasts(dt);

        if (screen != Screen.DUEL) return;
        duelClock += dt;
        playerMana = Math.min(100f, playerMana + 9.5f * dt);
        enemyMana = Math.min(100f, enemyMana + (8.4f + selectedDifficulty.pressureScale * 1.4f) * dt);
        if (playerBurn > 0f) { playerBurn -= dt; playerHealth -= 2.3f * dt; }
        if (enemyBurn > 0f) { enemyBurn -= dt; enemyHealth -= 2.3f * dt; }
        playerSlow = Math.max(0f, playerSlow - dt);
        enemySlow = Math.max(0f, enemySlow - dt);

        for (int lane = 0; lane < 3; lane++) {
            LeyNode node = nodes[lane];
            node.charge = Math.max(0f, node.charge - dt * (selectedArena == ArenaType.ASTRAL_COURT ? 0.5f : 0.9f));
            if (node.charge <= 0.01f) { node.owner = -1; node.profile = baseNodeProfile(); }
            if (laneTraits[lane] == LaneTrait.WELLSPRING && node.owner >= 0 && node.charge > 4f) {
                if (node.owner == 0) playerMana = Math.min(100f, playerMana + 1.7f * dt);
                else enemyMana = Math.min(100f, enemyMana + 1.7f * dt);
            }
        }

        tideSurgeTimer = Math.max(0f, tideSurgeTimer - dt);
        lightShaftTimer = Math.max(0f, lightShaftTimer - dt);
        if (lightShaftTimer <= 0f) lightShaftLane = -1;
        if (!practiceMode || duelClock > 10f) {
            arenaEventTimer -= dt;
            if (arenaEventTimer <= 0f) fireArenaEvent();
        }

        updateEntities(dt);
        if (!practiceMode || duelClock > 10f) {
            if (enemyIntent != null) updateEnemyIntent(dt);
            else {
                aiCooldown -= dt;
                if (aiCooldown <= 0f) prepareAiSpell();
            }
        }

        if (playerHealth <= 0f || enemyHealth <= 0f) finishDuel(enemyHealth <= 0f);
    }

    private void updatePendingCasts(float dt) {
        ArrayList<PendingCast> ready = new ArrayList<PendingCast>();
        for (Iterator<PendingCast> iterator = pendingCasts.iterator(); iterator.hasNext();) {
            PendingCast pending = iterator.next();
            pending.delay -= dt;
            if (pending.delay <= 0f) { ready.add(pending); iterator.remove(); }
        }
        for (PendingCast pending : ready) deploySpell(pending.owner, pending.program, pending.lane,
                pending.quality, pending.powerScale, pending.empowered, false);
    }

    private void updateEntities(float dt) {
        ArrayList<Entity> additions = new ArrayList<Entity>();
        for (Entity entity : entities) {
            if (entity.dead) continue;
            entity.age += dt;
            entity.timer -= dt;
            if (!entity.trail.isEmpty() || entity.kind == Kind.PROJECTILE || entity.kind == Kind.SHARD
                    || entity.kind == Kind.SURGE) {
                entity.trailTimer -= dt;
                if (entity.trailTimer <= 0f) {
                    entity.trailTimer = 0.045f;
                    entity.trail.add(new PointF(entity.x, entity.y));
                    while (entity.trail.size() > 12) entity.trail.remove(0);
                }
            }

            switch (entity.kind) {
                case PROJECTILE:
                case SHARD:
                    updateProjectile(entity, dt, additions);
                    break;
                case SURGE:
                    updateSurge(entity, dt, additions);
                    break;
                case ORBIT:
                    updateOrbit(entity, dt, additions);
                    break;
                case WARD:
                case GLYPH:
                case FIELD:
                case ENCHANTMENT:
                case AURA:
                    updatePersistent(entity, dt, additions);
                    break;
                case RIFT:
                    updateRift(entity, dt, additions);
                    break;
                case BURST:
                    if (entity.timer <= 0f) detonateBurst(entity, additions);
                    break;
                case BEAM:
                    updateBeam(entity, dt, additions);
                    break;
                case TRAP:
                    updateTrap(entity, dt, additions);
                    break;
            }
        }

        resolveProjectileCollisions(additions);
        resolveWaitingEnchantments();
        entities.addAll(additions);
        for (Iterator<Entity> iterator = entities.iterator(); iterator.hasNext();) {
            Entity entity = iterator.next();
            if (entity.dead || entity.y < fieldTop - dp(180f) || entity.y > fieldBottom + dp(180f)) iterator.remove();
        }
    }

    private void updateProjectile(Entity entity, float dt, ArrayList<Entity> additions) {
        float statusScale = entity.owner == 0 ? (playerSlow > 0f ? 0.78f : 1f) : (enemySlow > 0f ? 0.78f : 1f);
        float fieldScale = 1f;
        for (Entity zone : entities) {
            if (zone.dead || zone.lane != entity.lane || zone == entity) continue;
            if ((zone.kind == Kind.FIELD || zone.kind == Kind.GLYPH) && Math.abs(zone.y - entity.y) < zone.radius) {
                fieldScale *= applyFieldInfluence(entity, zone, dt);
            }
            if (zone.kind == Kind.RIFT && Math.abs(zone.y - entity.y) < zone.radius) {
                applyRiftInfluence(entity, zone);
            }
        }
        if (tideSurgeTimer > 0f) {
            fieldScale *= 0.74f;
            entity.profile.blend(tideProfile(), dt * 0.30f);
        }
        if (lightShaftLane == entity.lane && lightShaftTimer > 0f) {
            entity.damage += dt * 1.4f;
            entity.profile.blend(lightProfile(), dt * 0.30f);
        }
        float oldY = entity.y;
        entity.y += entity.velocity * statusScale * fieldScale * dt;

        if (!entity.passedNode && crossed(oldY, entity.y, nodeY)) {
            entity.passedNode = true;
            interactWithNode(entity, additions);
        }
        if (entity.program != null && entity.program.has(Clause.SEEK) && !entity.seekingDone
                && crossed(oldY, entity.y, (fieldTop + fieldBottom) * 0.48f)) {
            entity.seekingDone = true;
            int lane = chooseSeekLane(entity.owner, entity.lane);
            if (lane != entity.lane) {
                entity.lane = lane;
                entity.x = laneX[lane];
                spawnArc(entity.x, entity.y, entity.profileColor(), 12);
                floatingTexts.add(new FloatingText("SEEK", entity.x, entity.y, BRIGHT_GOLD, 0.8f));
            }
        }
        if (random.nextFloat() < 0.32f) spawnTrailParticle(entity);
    }

    private float applyFieldInfluence(Entity projectile, Entity zone, float dt) {
        if (zone.owner == projectile.owner) {
            projectile.profile.blend(zone.profile, dt * 0.045f);
            projectile.damage += dt * (0.22f + zone.profile.aether * 0.18f + zone.profile.heat * 0.12f);
            return 1f + Math.min(0.18f, zone.profile.impulse * 0.035f);
        }
        boolean hexwright = projectile.owner == 0 && selectedArtifact == Artifact.HEXWRIGHT_RING;
        if (hasClause(projectile, Clause.DISPEL) && zone.kind == Kind.FIELD) {
            zone.dead = true;
            floatingTexts.add(new FloatingText("DISPELLED", zone.x, zone.y, IVORY, 0.9f));
            spawnShockwave(zone.x, zone.y, IVORY, zone.radius);
            return 1f;
        }
        if (hasClause(projectile, Clause.SIPHON)) {
            float rate = hexwright ? 1.5f : 1f;
            projectile.profile.blend(zone.profile, dt * 0.06f * rate);
            projectile.damage += dt * 0.85f * rate;
            zone.life -= dt * 1.7f * rate;
            if (random.nextFloat() < 0.25f) spawnArc(projectile.x, projectile.y, zone.profileColor(), 3);
            return 1f;
        }
        float slow = 1f;
        if (zone.profile.moisture > 0.2f || zone.profile.cold > 0.2f) {
            slow -= Math.min(0.35f, zone.profile.moisture * 0.06f + zone.profile.cold * 0.08f);
        }
        if (zone.profile.overgrowth > 0.15f) slow -= Math.min(0.20f, zone.profile.growth * 0.08f);
        projectile.damage = Math.max(2f, projectile.damage - dt * (0.55f + zone.profile.entropy * 0.65f));
        if (zone.profile.charge > 0.25f && projectile.profile.moisture > 0.25f) projectile.damage += dt * 0.8f;
        if (zone.profile.magma > 0.15f) projectile.damage = Math.max(2f, projectile.damage - dt * zone.profile.magma * 1.1f);
        return Math.max(0.50f, slow);
    }

    /**
     * A hostile rift with remaining charges throws the projectile into the lane
     * where the rift owner is strongest; allied projectiles are accelerated and
     * tinted by the rift instead.
     */
    private void applyRiftInfluence(Entity projectile, Entity rift) {
        if (rift.owner == projectile.owner) {
            if (!projectile.riftBoosted) {
                projectile.riftBoosted = true;
                projectile.velocity *= 1.12f;
                projectile.profile.blend(rift.profile, 0.15f);
            }
            return;
        }
        if (rift.timer <= 0f || projectile.riftedBy == rift) return;
        int target = rift.lane;
        float best = -Float.MAX_VALUE;
        for (int direction : new int[]{-1, 1}) {
            int lane = rift.lane + direction;
            if (lane < 0 || lane > 2) continue;
            float score = lanePressure(rift.owner, lane) - lanePressure(projectile.owner, lane) * 0.5f
                    + random.nextFloat() * 2f;
            if (score > best) { best = score; target = lane; }
        }
        if (target == rift.lane) return;
        rift.timer -= 1f;
        projectile.riftedBy = rift;
        spawnArc(projectile.x, projectile.y, rift.profileColor(), laneX[target], projectile.y, 5);
        projectile.lane = target;
        projectile.x = laneX[target];
        projectile.passedNode = false;
        floatingTexts.add(new FloatingText("RIFTED", projectile.x, projectile.y, rift.profileColor(), 0.9f));
        spawnShockwave(rift.x, rift.y, rift.profileColor(), rift.radius * 1.2f);
        if (rift.timer <= 0f) {
            rift.dead = true;
            floatingTexts.add(new FloatingText("RIFT COLLAPSES", rift.x, rift.y, MUTED, 0.9f));
        }
    }

    private boolean hasClause(Entity entity, Clause clause) {
        return entity.program != null && entity.program.has(clause);
    }

    private Profile tideProfile() {
        Profile p = new Profile();
        p.moisture = 1f;
        p.cohesion = 0.3f;
        p.recalculate();
        return p;
    }

    private Profile lightProfile() {
        Profile p = new Profile();
        p.radiance = 1f;
        p.heat = 0.2f;
        p.recalculate();
        return p;
    }

    private void updateOrbit(Entity entity, float dt, ArrayList<Entity> additions) {
        entity.life -= dt;
        if (entity.life <= 0f || entity.hp <= 0f) { entity.dead = true; return; }
        if (entity.timer <= 0f) {
            entity.timer = Math.max(0.64f, 1.45f - entity.profile.impulse * 0.18f - entity.profile.charge * 0.10f)
                    * (entity.swift ? 0.75f : 1f);
            Entity bolt = createProjectile(entity.owner, entity.lane, entity.profile.copy(),
                    entity.damage * 0.48f, entity.program, entity.powerScale * 0.78f, entity.empowered);
            bolt.y = entity.y + (entity.owner == 0 ? -entity.radius : entity.radius);
            additions.add(bolt);
            spawnArc(entity.x, entity.y, entity.profileColor(), 8);
        }
    }

    /**
     * A surge is a wide wavefront: it shoves hostile spell bodies backward,
     * damps hostile fields, and washes over constructs instead of trading.
     */
    private void updateSurge(Entity surge, float dt, ArrayList<Entity> additions) {
        float fieldScale = 1f;
        for (Entity other : entities) {
            if (other.dead || other == surge || other.lane != surge.lane) continue;
            if (other.owner == surge.owner) continue;
            float gap = Math.abs(other.y - surge.y);
            if ((other.kind == Kind.PROJECTILE || other.kind == Kind.SHARD) && gap < surge.radius + other.radius) {
                float push = dp(150f) * (1f + surge.profile.impulse * 0.20f) * dt;
                other.y -= Math.signum(other.velocity) * push;
                other.damage = Math.max(2f, other.damage - dt * 1.6f);
                if (random.nextFloat() < 0.3f) spawnArc(other.x, other.y, surge.profileColor(), 3);
            } else if (other.kind == Kind.FIELD && gap < surge.radius + other.radius) {
                other.life -= dt * 2.6f;
            } else if (isConstruct(other) && gap < surge.radius + other.radius) {
                applyHitToEntity(surge, other, surge.damage, additions);
                surge.dead = true;
                return;
            }
        }
        if (tideSurgeTimer > 0f) fieldScale *= 0.80f;
        surge.y += surge.velocity * fieldScale * dt;
        if (surge.owner == 0 && surge.y <= fieldTop + dp(10f)) {
            applyCasterDamage(1, surge, additions, surge.damage * 0.75f);
            surge.dead = true;
        } else if (surge.owner == 1 && surge.y >= fieldBottom - dp(10f)) {
            applyCasterDamage(0, surge, additions, surge.damage * 0.75f);
            surge.dead = true;
        }
        if (random.nextFloat() < 0.45f) spawnTrailParticle(surge);
    }

    /**
     * A rift holds position and decays; its redirect work happens in
     * applyRiftInfluence when projectiles cross it.
     */
    private void updateRift(Entity rift, float dt, ArrayList<Entity> additions) {
        rift.life -= dt;
        if (rift.life <= 0f || rift.hp <= 0f || rift.timer <= 0f) {
            if (!rift.dead) {
                rift.dead = true;
                spawnShockwave(rift.x, rift.y, rift.profileColor(), rift.radius * 1.4f);
            }
            return;
        }
        if (random.nextFloat() < 0.2f) {
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            particles.add(new Particle(rift.x + (float) Math.cos(angle) * rift.radius,
                    rift.y + (float) Math.sin(angle) * rift.radius * 0.45f,
                    -(float) Math.cos(angle) * dp(24f), -(float) Math.sin(angle) * dp(12f),
                    dp(1.4f), rift.profileColor(), 0.5f, true));
        }
    }

    private void updatePersistent(Entity entity, float dt, ArrayList<Entity> additions) {
        entity.life -= dt;
        if ((entity.kind == Kind.WARD || entity.kind == Kind.ORBIT || entity.kind == Kind.GLYPH
                || entity.kind == Kind.AURA) && entity.hp <= 0f) entity.dead = true;
        if (entity.life <= 0f) entity.dead = true;
        if (entity.dead) return;

        if (entity.kind == Kind.GLYPH && Math.abs(entity.y - nodeY) < entity.radius + dp(18f)) {
            LeyNode node = nodes[entity.lane];
            float speed = selectedArtifact == Artifact.LEY_KEY && entity.owner == 0 ? 9f : 6f;
            attuneNode(node, entity.owner, entity.profile, speed * dt);
        }
        if (entity.kind == Kind.FIELD) {
            float corrode = entity.profile.corrosion > 0.2f ? 2.2f + entity.profile.corrosion * 1.4f : 0f;
            float burn = entity.profile.magma > 0.15f ? 1.6f + entity.profile.magma * 1.2f : 0f;
            float shatter = entity.profile.shatter > 0.15f ? 1.2f + entity.profile.shatter * 1.0f : 0f;
            float mend = entity.profile.overgrowth > 0.15f ? 1.8f + entity.profile.overgrowth * 1.5f : 0f;
            if (entity.owner == 0 && selectedArtifact == Artifact.VERDANT_CHALICE) mend *= 1.5f;
            for (Entity other : entities) {
                if (other.dead || other.lane != entity.lane || !isConstruct(other)
                        || Math.abs(other.y - entity.y) >= entity.radius) continue;
                if (other.owner != entity.owner) other.hp -= dt * (corrode + burn + shatter);
                // Environment blooms mend everyone; owned regrowth mends allies only.
                if (mend > 0f && (entity.owner == OWNER_ENVIRONMENT || other.owner == entity.owner)) {
                    other.hp += dt * mend;
                }
            }
            if (mend > 0f && entity.owner == 0 && selectedArtifact == Artifact.VERDANT_CHALICE) {
                playerHealth = Math.min(100f, playerHealth + dt * 0.6f);
            }
        }
        if (entity.kind == Kind.WARD && selectedArtifact == Artifact.AEGIS_BELL && entity.owner == 0) {
            entity.pulseTimer -= dt;
            if (entity.pulseTimer <= 0f) {
                entity.pulseTimer = 2.4f;
                for (Entity ally : entities) {
                    if (!ally.dead && ally.owner == entity.owner && ally.lane == entity.lane && isConstruct(ally)) {
                        ally.hp += 1.8f;
                    }
                }
                spawnRing(entity.x, entity.y, GOLD, entity.radius * 0.9f);
            }
        }
    }

    private void updateBeam(Entity entity, float dt, ArrayList<Entity> additions) {
        entity.life -= dt;
        if (!entity.beamResolved) {
            entity.beamResolved = true;
            Entity target = nearestHostileInLane(entity.owner, entity.lane, true);
            float damage = entity.damage;
            if (target != null) {
                applyHitToEntity(entity, target, damage, additions);
            } else {
                applyCasterDamage(1 - entity.owner, entity, additions, damage * 0.80f);
            }
            interactBeamWithNode(entity, additions);
        }
        if (entity.life <= 0f) entity.dead = true;
    }

    private void updateTrap(Entity trap, float dt, ArrayList<Entity> additions) {
        trap.life -= dt;
        if (trap.life <= 0f) { trap.dead = true; return; }
        for (Entity hostile : entities) {
            if (hostile.dead || hostile.owner == trap.owner || hostile.lane != trap.lane) continue;
            if ((hostile.kind == Kind.PROJECTILE || hostile.kind == Kind.BEAM || hostile.kind == Kind.SHARD)
                    && Math.abs(hostile.y - trap.y) < trap.radius) {
                if (hasClause(hostile, Clause.DISPEL)) {
                    trap.dead = true;
                    hostile.program.clauses.remove(Clause.DISPEL);
                    floatingTexts.add(new FloatingText("TRAP DISPELLED", trap.x, trap.y, IVORY, 1f));
                    spawnShockwave(trap.x, trap.y, IVORY, trap.radius);
                    break;
                }
                trap.dead = true;
                Program released = trap.storedProgram == null ? trap.program : trap.storedProgram;
                deploySpell(trap.owner, released, trap.lane, 0.88f, trap.powerScale * 0.88f, trap.empowered, true);
                spawnImpact(trap.x, trap.y, trap.profileColor(), 22);
                floatingTexts.add(new FloatingText("TRIGGER", trap.x, trap.y, BRIGHT_GOLD, 1f));
                break;
            }
        }
    }

    private void resolveProjectileCollisions(ArrayList<Entity> additions) {
        for (Entity projectile : entities) {
            if (projectile.dead || (projectile.kind != Kind.PROJECTILE && projectile.kind != Kind.SHARD)) continue;
            Entity target = null;
            for (Entity candidate : entities) {
                if (candidate.dead || candidate.owner == projectile.owner || candidate.lane != projectile.lane) continue;
                if (isConstruct(candidate) && Math.abs(candidate.y - projectile.y) < candidate.radius + projectile.radius) {
                    target = candidate;
                    break;
                }
            }
            if (target != null) {
                applyHitToEntity(projectile, target, projectile.damage, additions);
                projectile.dead = true;
            }
            if (!projectile.dead && projectile.owner == 0 && projectile.y <= fieldTop + dp(10f)) {
                applyCasterDamage(1, projectile, additions, projectile.damage);
                projectile.dead = true;
            } else if (!projectile.dead && projectile.owner == 1 && projectile.y >= fieldBottom - dp(10f)) {
                applyCasterDamage(0, projectile, additions, projectile.damage);
                projectile.dead = true;
            }
        }

        for (int i = 0; i < entities.size(); i++) {
            Entity a = entities.get(i);
            if (a.dead || (a.kind != Kind.PROJECTILE && a.kind != Kind.SHARD)) continue;
            for (int j = i + 1; j < entities.size(); j++) {
                Entity b = entities.get(j);
                if (b.dead || a.owner == b.owner || a.lane != b.lane
                        || (b.kind != Kind.PROJECTILE && b.kind != Kind.SHARD)) continue;
                if (Math.abs(a.y - b.y) <= a.radius + b.radius + dp(6f)) {
                    resolveSpellReaction(a, b, additions);
                    break;
                }
            }
        }
    }

    private void resolveWaitingEnchantments() {
        for (Entity enchantment : entities) {
            if (enchantment.dead || enchantment.kind != Kind.ENCHANTMENT) continue;
            if (enchantment.hexWaiting) {
                for (Entity hostile : entities) {
                    if (hostile.dead || hostile.owner == enchantment.owner || hostile.owner == OWNER_ENVIRONMENT
                            || hostile.lane != enchantment.lane) continue;
                    if (!isConstruct(hostile) && hostile.kind != Kind.PROJECTILE && hostile.kind != Kind.RIFT) continue;
                    if (Math.abs(hostile.y - enchantment.y) < enchantment.radius * 1.3f) {
                        applyHex(hostile, enchantment.profile, enchantment.powerScale, enchantment.owner);
                        enchantment.dead = true;
                        break;
                    }
                }
                continue;
            }
            Entity target = nearestAlliedBindable(enchantment.owner, enchantment.lane, enchantment.y);
            if (target != null && Math.abs(target.y - enchantment.y) < enchantment.radius * 1.2f) {
                applyBinding(target, enchantment.profile, enchantment.powerScale);
                enchantment.dead = true;
                floatingTexts.add(new FloatingText("BOUND", target.x, target.y, BRIGHT_GOLD, 0.9f));
                spawnRing(target.x, target.y, enchantment.profileColor(), target.radius * 1.2f);
            }
        }
    }

    /**
     * Hex is the hostile mirror of Bind: it merges entropy pressure into the
     * target and erodes its damage, durability, and remaining lifetime.
     */
    private void applyHex(Entity target, Profile hexProfile, float powerScale, int hexOwner) {
        boolean hexwright = hexOwner == 0 && selectedArtifact == Artifact.HEXWRIGHT_RING;
        float strength = powerScale * (hexwright ? 1.4f : 1f);
        Profile curse = hexProfile.copy();
        curse.entropy += 0.5f;
        curse.recalculate();
        target.profile.blend(curse, 0.30f * strength);
        target.damage *= Math.max(0.45f, 1f - 0.22f * strength - hexProfile.entropy * 0.08f);
        target.hp *= Math.max(0.45f, 1f - 0.20f * strength);
        target.life *= Math.max(0.40f, 1f - 0.25f * strength);
        target.hexed = true;
        floatingTexts.add(new FloatingText("HEXED", target.x, target.y, Element.VOID.color, 1f));
        spawnRing(target.x, target.y, Element.VOID.color, target.radius * 1.3f);
    }

    private void applyHitToEntity(Entity source, Entity target, float damage, ArrayList<Entity> additions) {
        float response = reactionStrength(source.profile, target.profile);
        float multiplier = 1f + Math.min(0.65f, response * 0.18f);
        if (target.kind == Kind.WARD && target.profile.mass > source.profile.impulse) multiplier *= 0.82f;
        if (source.profile.nullFlux > 0.2f) multiplier += 0.18f;
        if (source.profile.eclipse > 0.15f) multiplier += 0.24f;
        if (target.profile.sanctify > 0.15f) multiplier *= 0.82f;
        if (hasClause(source, Clause.DISPEL)) multiplier += 0.30f;
        if (hasClause(source, Clause.SIPHON) && source.owner <= 1) {
            boolean hexwright = source.owner == 0 && selectedArtifact == Artifact.HEXWRIGHT_RING;
            float leech = damage * (hexwright ? 0.22f : 0.14f);
            if (source.owner == 0) playerMana = Math.min(100f, playerMana + leech);
            else enemyMana = Math.min(100f, enemyMana + leech);
        }
        if (laneTraits[target.lane] == LaneTrait.WILD_MAGIC) multiplier *= 1.20f;
        target.hp -= damage * multiplier;
        if (source.profile.entropy > 0.3f) target.life -= source.profile.entropy * 0.35f;
        if (source.profile.charge > 0.3f && target.profile.moisture > 0.25f) target.hp -= damage * 0.22f;
        spawnImpact(source.x, source.y, source.profileColor(), 14);
        floatingTexts.add(new FloatingText(multiplier > 1.25f ? "ARCANE BREAK" : "BLOCK",
                source.x, source.y, multiplier > 1.25f ? BRIGHT_GOLD : MUTED, 0.85f));
        if (target.hp <= 0f) {
            target.dead = true;
            if (source.owner == 0) playerResonance = Math.min(100f, playerResonance + 9f);
            else enemyResonance = Math.min(100f, enemyResonance + 9f);
            createReactionFieldFromProfiles(source.owner, source.lane, target.y, source.profile, target.profile, additions);
        }
    }

    private void applyCasterDamage(int targetOwner, Entity source, ArrayList<Entity> additions, float damage) {
        float finalDamage = damage * (0.82f + source.profile.force() * 0.16f);
        if (targetOwner == 0) {
            playerHealth -= finalDamage;
            if (source.profile.heat > 0.55f) playerBurn = Math.max(playerBurn, 2.2f + source.profile.heat * 0.5f);
            if (source.profile.cold > 0.45f || source.profile.moisture > 0.65f) playerSlow = Math.max(playerSlow, 2.0f);
        } else {
            enemyHealth -= finalDamage;
            damageDealt += Math.round(finalDamage);
            if (source.profile.heat > 0.55f) enemyBurn = Math.max(enemyBurn, 2.2f + source.profile.heat * 0.5f);
            if (source.profile.cold > 0.45f || source.profile.moisture > 0.65f) enemySlow = Math.max(enemySlow, 2.0f);
        }
        if (source.profile.corrosion > 0.25f) {
            if (source.owner == 0) playerMana = Math.min(100f, playerMana + 3f + source.profile.entropy);
            else enemyMana = Math.min(100f, enemyMana + 3f + source.profile.entropy);
        }
        spawnImpact(source.x, targetOwner == 0 ? fieldBottom : fieldTop, source.profileColor(), 24);
        spawnShockwave(source.x, targetOwner == 0 ? fieldBottom : fieldTop, source.profileColor(), dp(54f));
        addShake(dp(Math.min(6f, 2f + finalDamage * 0.14f)));
        floatingTexts.add(new FloatingText("-" + Math.round(finalDamage), source.x,
                targetOwner == 0 ? fieldBottom - dp(10f) : fieldTop + dp(20f), DANGER, 1f));
        createReactionFieldFromProfiles(source.owner, source.lane,
                targetOwner == 0 ? fieldBottom - dp(42f) : fieldTop + dp(42f), source.profile, null, additions);
    }

    private void detonateBurst(Entity burst, ArrayList<Entity> additions) {
        burst.dead = true;
        float damage = burst.damage;
        boolean hitSomething = false;
        for (Entity target : entities) {
            if (target.dead || target.owner == burst.owner || target.lane != burst.lane) continue;
            if (Math.abs(target.y - burst.y) < burst.radius + target.radius) {
                hitSomething = true;
                if (isConstruct(target)) applyHitToEntity(burst, target, damage, additions);
            }
        }
        float casterY = burst.owner == 0 ? fieldTop : fieldBottom;
        if (Math.abs(casterY - burst.y) < burst.radius * 1.7f) {
            applyCasterDamage(1 - burst.owner, burst, additions, damage * 0.88f);
            hitSomething = true;
        }
        createReactionFieldFromProfiles(burst.owner, burst.lane, burst.y, burst.profile, null, additions);
        spawnImpact(burst.x, burst.y, burst.profileColor(), hitSomething ? 34 : 24);
        spawnShockwave(burst.x, burst.y, burst.profileColor(), burst.radius * 1.5f);
        addShake(dp(hitSomething ? 5f : 3f));
        floatingTexts.add(new FloatingText(burst.profile.dominantReaction().toUpperCase(Locale.US),
                burst.x, burst.y, BRIGHT_GOLD, 1.2f));
    }

    private void resolveSpellReaction(Entity a, Entity b, ArrayList<Entity> additions) {
        float y = (a.y + b.y) * 0.5f;
        Reaction reaction = analyzeReaction(a.profile, b.profile);
        float powerA = a.damage * a.profile.force() * a.powerScale;
        float powerB = b.damage * b.profile.force() * b.powerScale;
        if (laneTraits[a.lane] == LaneTrait.WILD_MAGIC) {
            reaction = new Reaction(reaction.type, reaction.strength * 1.35f, reaction.label, reaction.color);
        }

        // Dispel is the removal instant: it erases the opposing body outright,
        // consuming the dispel charge before any elemental reaction can occur.
        boolean dispelA = hasClause(a, Clause.DISPEL);
        boolean dispelB = hasClause(b, Clause.DISPEL);
        if (dispelA || dispelB) {
            if (dispelA && dispelB) {
                a.dead = true;
                b.dead = true;
            } else {
                Entity dispeller = dispelA ? a : b;
                Entity dispelled = dispelA ? b : a;
                dispelled.dead = true;
                dispeller.program.clauses.remove(Clause.DISPEL);
                dispeller.damage *= 0.82f;
            }
            floatingTexts.add(new FloatingText("DISPELLED", laneX[a.lane], y, IVORY, 1f));
            spawnShockwave(laneX[a.lane], y, IVORY, dp(46f));
            reactionsWon += a.owner == 0 || b.owner == 0 ? 1 : 0;
            return;
        }

        if (reaction.type == ReactionType.NULL_FLUX) {
            a.dead = true;
            b.dead = true;
            LeyNode node = nodes[a.lane];
            node.charge = Math.max(0f, node.charge - 35f * reaction.strength);
            spawnImpact(laneX[a.lane], y, Element.VOID.color, 30);
        } else if (reaction.type == ReactionType.THERMAL_SHOCK) {
            a.dead = true;
            b.dead = true;
            damageNearby(a.lane, y, -1, (powerA + powerB) * 0.22f, dp(72f));
            spawnImpact(laneX[a.lane], y, IVORY, 34);
            spawnShockwave(laneX[a.lane], y, IVORY, dp(72f));
            addShake(dp(4f));
        } else if (reaction.type == ReactionType.CONDUCTION) {
            Entity winner = powerA >= powerB ? a : b;
            Entity loser = winner == a ? b : a;
            loser.dead = true;
            winner.damage *= 0.68f;
            chainLightning(winner.owner, winner.lane, y, winner.damage * 0.24f, additions);
        } else if (reaction.type == ReactionType.STEAM || reaction.type == ReactionType.BLIZZARD
                || reaction.type == ReactionType.MAGMA || reaction.type == ReactionType.CORROSION
                || reaction.type == ReactionType.WILDFIRE || reaction.type == ReactionType.OVERGROWTH) {
            a.dead = true;
            b.dead = true;
            Entity field = createField(powerA >= powerB ? a.owner : b.owner, a.lane, y,
                    mergeProfiles(a.profile, b.profile, 0.55f), 4.0f + reaction.strength * 1.4f);
            additions.add(field);
        } else if (reaction.type == ReactionType.ECLIPSE) {
            a.dead = true;
            b.dead = true;
            damageNearby(a.lane, y, -1, (powerA + powerB) * 0.26f, dp(80f));
            spawnShockwave(laneX[a.lane], y, Element.RADIANCE.color, dp(80f));
            spawnImpact(laneX[a.lane], y, Element.VOID.color, 30);
            addShake(dp(5f));
        } else if (reaction.type == ReactionType.SANCTIFY) {
            // Cleansing light: the stronger body survives purified — entropy
            // stripped, hex lifted, and a little durability restored.
            Entity winner = powerA >= powerB ? a : b;
            Entity loser = winner == a ? b : a;
            loser.dead = true;
            winner.profile.entropy = 0f;
            winner.profile.recalculate();
            winner.hexed = false;
            winner.hp += 4f;
            winner.damage *= 0.88f;
        } else if (reaction.type == ReactionType.SHATTER) {
            a.dead = true;
            b.dead = true;
            Entity source = powerA >= powerB ? a : b;
            spawnShards(source, y, additions);
        } else {
            float gap = Math.abs(powerA - powerB) / Math.max(1f, Math.max(powerA, powerB));
            if (gap < 0.16f) {
                a.dead = true;
                b.dead = true;
                spawnImpact(laneX[a.lane], y, BRIGHT_GOLD, 22);
            } else {
                Entity winner = powerA > powerB ? a : b;
                Entity loser = winner == a ? b : a;
                loser.dead = true;
                winner.damage *= 0.56f + gap * 0.24f;
                winner.radius *= 0.92f;
            }
        }
        reactionsWon += a.owner == 0 || b.owner == 0 ? 1 : 0;
        floatingTexts.add(new FloatingText(reaction.label, laneX[a.lane], y,
                reaction.type == ReactionType.RESONANCE ? MUTED : BRIGHT_GOLD, 1.0f));
        spawnArc(laneX[a.lane], y, reaction.color, 18);
    }

    private void createReactionFieldFromProfiles(int owner, int lane, float y, Profile a, Profile b,
                                                 ArrayList<Entity> additions) {
        Profile merged = b == null ? a.copy() : mergeProfiles(a, b, 0.50f);
        String reaction = merged.dominantReaction();
        if (!"Resonance".equals(reaction) || merged.entropy > 0.35f || merged.aether > 0.45f) {
            additions.add(createField(owner, lane, y, merged, 3.2f + merged.stability() * 0.8f));
        }
    }

    private Entity createField(int owner, int lane, float y, Profile profile, float life) {
        Entity field = new Entity(Kind.FIELD, owner, lane, profile.copy());
        field.x = laneX[lane];
        field.y = y;
        field.radius = dp(48f) * (1f + profile.moisture * 0.10f + profile.aether * 0.08f);
        if (laneTraits[lane] == LaneTrait.ASHEN_GROUND) life *= 1.40f;
        if (laneTraits[lane] == LaneTrait.WILD_MAGIC) life *= 1.20f;
        field.life = life;
        field.hp = 1f;
        field.damage = 2f + profile.heat + profile.entropy;
        field.label = profile.dominantReaction();
        return field;
    }

    private void spawnShards(Entity source, float y, ArrayList<Entity> additions) {
        for (int direction : new int[]{-1, 0, 1}) {
            int lane = source.lane + direction;
            if (lane < 0 || lane > 2) continue;
            Entity shard = createProjectile(source.owner, lane, source.profile.copy(),
                    source.damage * (direction == 0 ? 0.42f : 0.30f), source.program,
                    source.powerScale * 0.50f, false);
            shard.kind = Kind.SHARD;
            shard.y = y;
            shard.radius *= 0.66f;
            additions.add(shard);
        }
    }

    private void chainLightning(int owner, int lane, float y, float damage, ArrayList<Entity> additions) {
        for (int direction : new int[]{-1, 1}) {
            int targetLane = lane + direction;
            if (targetLane < 0 || targetLane > 2) continue;
            Entity target = nearestHostileInLane(owner, targetLane, true);
            if (target != null) {
                target.hp -= damage;
                spawnArc(laneX[lane], y, Element.LIGHTNING.color, laneX[targetLane], target.y, 4);
            } else {
                LeyNode node = nodes[targetLane];
                if (node.owner != owner) node.charge = Math.max(0f, node.charge - damage * 0.7f);
            }
        }
    }

    private void damageNearby(int lane, float y, int excludedOwner, float damage, float radius) {
        for (Entity entity : entities) {
            if (entity.dead || entity.lane != lane || !isConstruct(entity)) continue;
            if (excludedOwner >= 0 && entity.owner == excludedOwner) continue;
            if (Math.abs(entity.y - y) < radius) entity.hp -= damage;
        }
    }

    private void deploySpell(int owner, Program sourceProgram, int lane, float quality,
                             float powerScale, boolean empowered, boolean releasedFromTrigger) {
        if (sourceProgram == null || !sourceProgram.valid()) return;
        Program program = sourceProgram.copy();
        Profile profile = compile(program.elements, owner == 0 ? selectedArtifact : Artifact.PRISM_LENS, selectedArena);
        float localPower = powerScale;

        // A standing aura is a global enchantment: every allied cast inherits a
        // share of its profile while it survives.
        if (program.form != Form.AURA) {
            Entity aura = findAura(owner);
            if (aura != null) {
                profile.blend(aura.profile, 0.35f);
                spawnArc(aura.x, aura.y, aura.profileColor(), 4);
            }
        }

        if (program.has(Clause.CONSUME)) {
            Entity sacrifice = findConsumable(owner, lane);
            if (sacrifice != null) {
                profile.blend(sacrifice.profile, 0.58f);
                localPower *= 1.24f + Math.min(0.26f, sacrifice.profile.force() * 0.05f);
                sacrifice.dead = true;
                spawnImpact(sacrifice.x, sacrifice.y, Element.VOID.color, 18);
                floatingTexts.add(new FloatingText("CONSUMED", sacrifice.x, sacrifice.y, Element.VOID.color, 0.9f));
            }
        }

        if (program.has(Clause.HEX)) {
            Entity target = strongestHostilePersistent(owner, lane);
            if (target != null) {
                applyHex(target, profile, localPower, owner);
            } else {
                Entity waiting = new Entity(Kind.ENCHANTMENT, owner, lane, profile.copy());
                waiting.x = laneX[lane];
                waiting.y = nodeY + (owner == 0 ? -dp(30f) : dp(30f));
                waiting.radius = dp(44f);
                waiting.life = 8f;
                waiting.powerScale = localPower;
                waiting.hexWaiting = true;
                waiting.program = program.copy();
                entities.add(waiting);
                floatingTexts.add(new FloatingText("HEX WAITS", waiting.x, waiting.y, Element.VOID.color, 0.9f));
            }
            rewardCast(owner, quality, program, localPower);
            return;
        }

        if (program.has(Clause.BIND)) {
            Entity target = nearestAlliedBindable(owner, lane, owner == 0 ? fieldBottom : fieldTop);
            if (target != null) {
                applyBinding(target, profile, localPower);
                spawnRing(target.x, target.y, profileColor(profile), target.radius * 1.25f);
                floatingTexts.add(new FloatingText("BOUND", target.x, target.y, BRIGHT_GOLD, 0.9f));
                rewardCast(owner, quality, program, localPower);
                return;
            }
            Entity waiting = new Entity(Kind.ENCHANTMENT, owner, lane, profile.copy());
            waiting.x = laneX[lane];
            waiting.y = owner == 0 ? fieldBottom - dp(74f) : fieldTop + dp(74f);
            waiting.radius = dp(44f);
            waiting.life = 8f;
            waiting.powerScale = localPower;
            waiting.program = program.copy();
            entities.add(waiting);
            rewardCast(owner, quality, program, localPower);
            return;
        }

        if (program.has(Clause.TRIGGER) && !releasedFromTrigger) {
            Program stored = program.copy();
            stored.clauses.remove(Clause.TRIGGER);
            Entity trap = new Entity(Kind.TRAP, owner, lane, profile.copy());
            trap.x = laneX[lane];
            trap.y = nodeY + (owner == 0 ? dp(42f) : -dp(42f));
            trap.radius = dp(50f) * (1f + profile.aether * 0.08f);
            trap.life = 10f * profile.stability();
            trap.powerScale = localPower;
            trap.storedProgram = stored;
            trap.program = program.copy();
            trap.empowered = empowered;
            entities.add(trap);
            attuneNode(nodes[lane], owner, profile, 10f);
            spawnRing(trap.x, trap.y, profileColor(profile), trap.radius);
            rewardCast(owner, quality, program, localPower);
            return;
        }

        Entity entity;
        boolean anchored = program.has(Clause.ANCHOR);
        boolean swift = program.has(Clause.SWIFT);
        switch (program.form) {
            case LANCE:
                entity = createProjectile(owner, lane, profile, 12.8f * (0.72f + quality * 0.52f),
                        program, localPower, empowered);
                if (anchored) {
                    entity.velocity *= 0.66f;
                    entity.radius *= 1.28f;
                    entity.damage *= 1.20f;
                    entity.anchored = true;
                }
                if (swift) {
                    entity.velocity *= 1.35f;
                    entity.radius *= 0.85f;
                    entity.damage *= 0.92f;
                }
                entities.add(entity);
                break;
            case SURGE:
                entity = new Entity(Kind.SURGE, owner, lane, profile.copy());
                entity.x = laneX[lane];
                entity.y = owner == 0 ? fieldBottom - dp(20f) : fieldTop + dp(20f);
                entity.radius = dp(34f) * (1f + profile.mass * 0.06f + profile.moisture * 0.05f);
                float surgeSpeed = dp(72f) * selectedTempo.projectileScale
                        * (0.82f + profile.impulse * 0.12f);
                if (laneTraits[lane] == LaneTrait.LEY_CURRENT) surgeSpeed *= 1.18f;
                entity.velocity = owner == 0 ? -surgeSpeed : surgeSpeed;
                entity.damage = 8.5f * profile.force() * localPower;
                entity.program = program.copy();
                entity.powerScale = localPower;
                if (anchored) { entity.radius *= 1.25f; entity.velocity *= 0.75f; entity.damage *= 1.15f; entity.anchored = true; }
                if (swift) { entity.velocity *= 1.30f; entity.radius *= 0.88f; }
                entities.add(entity);
                break;
            case RIFT:
                entity = new Entity(Kind.RIFT, owner, lane, profile.copy());
                entity.x = laneX[lane];
                entity.y = owner == 0 ? fieldTop + (fieldBottom - fieldTop) * 0.34f
                        : fieldBottom - (fieldBottom - fieldTop) * 0.34f;
                entity.radius = dp(30f) * (1f + profile.aether * 0.10f);
                entity.hp = 12f * profile.stability() * localPower;
                entity.life = 12f * profile.stability();
                entity.timer = 3f;
                entity.damage = 2f;
                entity.program = program.copy();
                entity.powerScale = localPower;
                if (laneTraits[lane] == LaneTrait.BEDROCK) entity.hp *= 1.30f;
                if (anchored) { entity.life *= 1.35f; entity.hp *= 1.30f; entity.timer += 1f; entity.anchored = true; }
                entities.add(entity);
                break;
            case AURA: {
                Entity previous = findAura(owner);
                if (previous != null) previous.dead = true;
                entity = new Entity(Kind.AURA, owner, lane, profile.copy());
                entity.x = laneX[lane];
                entity.y = owner == 0 ? fieldBottom - dp(34f) : fieldTop + dp(34f);
                entity.radius = dp(22f) * (1f + profile.aether * 0.08f + profile.radiance * 0.06f);
                entity.hp = 13f * profile.stability() * localPower;
                entity.life = 15f * profile.stability();
                entity.damage = 1f;
                entity.program = program.copy();
                entity.powerScale = localPower;
                if (laneTraits[lane] == LaneTrait.BEDROCK) entity.hp *= 1.30f;
                if (anchored) { entity.life *= 1.40f; entity.hp *= 1.30f; entity.anchored = true; }
                entities.add(entity);
                floatingTexts.add(new FloatingText("AURA STANDS", entity.x, entity.y, BRIGHT_GOLD, 1f));
                break;
            }
            case WARD:
                entity = new Entity(Kind.WARD, owner, lane, profile.copy());
                entity.x = laneX[lane];
                entity.y = owner == 0 ? fieldBottom - dp(54f) : fieldTop + dp(54f);
                entity.radius = dp(30f) * (1f + profile.mass * 0.10f + profile.cohesion * 0.08f);
                entity.hp = 30f * profile.stability() * localPower;
                entity.life = 8.2f * profile.stability();
                entity.damage = 4f + profile.heat * 1.5f + profile.charge;
                entity.program = program.copy();
                entity.powerScale = localPower;
                if (owner == 0 && selectedArtifact == Artifact.AEGIS_BELL) { entity.hp *= 1.30f; entity.life *= 1.18f; }
                if (laneTraits[lane] == LaneTrait.BEDROCK) entity.hp *= 1.30f;
                if (anchored) { entity.hp *= 1.42f; entity.life *= 1.38f; entity.radius *= 1.12f; entity.anchored = true; }
                entities.add(entity);
                break;
            case ORBIT:
                entity = new Entity(Kind.ORBIT, owner, lane, profile.copy());
                entity.x = laneX[lane];
                entity.y = owner == 0 ? fieldBottom - dp(84f) : fieldTop + dp(84f);
                entity.radius = dp(19f) * (1f + profile.aether * 0.10f);
                entity.hp = 18f * profile.stability() * localPower;
                entity.damage = 9f * profile.force() * localPower;
                entity.life = 8f * profile.stability();
                entity.timer = 0.45f;
                entity.program = program.copy();
                entity.powerScale = localPower;
                if (owner == 0 && selectedArtifact == Artifact.AEGIS_BELL) { entity.hp *= 1.24f; entity.life *= 1.15f; }
                if (laneTraits[lane] == LaneTrait.BEDROCK) entity.hp *= 1.30f;
                if (anchored) { entity.hp *= 1.34f; entity.life *= 1.34f; entity.anchored = true; }
                if (swift) { entity.swift = true; entity.damage *= 0.92f; }
                entities.add(entity);
                break;
            case BURST:
                entity = new Entity(Kind.BURST, owner, lane, profile.copy());
                entity.x = laneX[lane];
                entity.y = owner == 0 ? fieldTop + (fieldBottom - fieldTop) * 0.28f
                        : fieldBottom - (fieldBottom - fieldTop) * 0.28f;
                entity.radius = dp(55f) * (1f + profile.moisture * 0.10f + profile.aether * 0.08f);
                entity.damage = 16f * profile.force() * localPower;
                entity.timer = Math.max(0.60f, 1.25f - profile.impulse * 0.10f - profile.charge * 0.08f);
                entity.life = entity.timer + 0.1f;
                entity.program = program.copy();
                entity.powerScale = localPower;
                if (anchored) { entity.radius *= 1.30f; entity.damage *= 1.18f; entity.timer *= 1.18f; entity.anchored = true; }
                if (swift) { entity.timer *= 0.70f; entity.damage *= 0.94f; }
                entities.add(entity);
                break;
            case BEAM:
                entity = new Entity(Kind.BEAM, owner, lane, profile.copy());
                entity.x = laneX[lane];
                entity.y = owner == 0 ? fieldBottom : fieldTop;
                entity.radius = dp(15f) * (1f + profile.charge * 0.10f + profile.aether * 0.08f);
                entity.damage = 14f * profile.force() * localPower;
                entity.life = anchored ? 0.72f : 0.42f;
                if (lightShaftLane == lane && lightShaftTimer > 0f) entity.damage *= 1.25f;
                if (swift) { entity.life *= 0.80f; entity.damage *= 0.94f; }
                entity.program = program.copy();
                entity.powerScale = localPower;
                entity.anchored = anchored;
                entities.add(entity);
                break;
            case GLYPH:
            default:
                entity = new Entity(Kind.GLYPH, owner, lane, profile.copy());
                entity.x = laneX[lane];
                entity.y = nodeY;
                entity.radius = dp(54f) * (1f + profile.aether * 0.12f + profile.cohesion * 0.06f);
                entity.hp = 18f * profile.stability() * localPower;
                entity.life = 10f * profile.stability();
                entity.damage = 3f + profile.heat + profile.entropy;
                entity.program = program.copy();
                entity.powerScale = localPower;
                if (laneTraits[lane] == LaneTrait.BEDROCK) entity.hp *= 1.30f;
                if (anchored) { entity.life *= 1.45f; entity.hp *= 1.35f; entity.radius *= 1.18f; entity.anchored = true; }
                entities.add(entity);
                attuneNode(nodes[lane], owner, profile, 28f * localPower);
                break;
        }

        if (empowered) {
            entity.empowered = true;
            entity.damage *= 1.38f;
            entity.hp *= 1.22f;
            entity.radius *= 1.12f;
        }
        spawnCastParticles(entity.x, entity.y, profileColor(profile), program.elements.size() * 5 + 12);

        if (program.has(Clause.FORK) && localPower > 0.34f) {
            Program branch = program.copy();
            branch.clauses.remove(Clause.FORK);
            float branchScale = selectedArena == ArenaType.SHATTERED_CROWN ? 0.56f : 0.46f;
            for (int direction : new int[]{-1, 1}) {
                int branchLane = lane + direction;
                if (branchLane >= 0 && branchLane < 3) {
                    pendingCasts.add(new PendingCast(owner, branch, branchLane, quality,
                            localPower * branchScale, empowered, 0.20f));
                }
            }
        }
        if (program.has(Clause.ECHO) && localPower > 0.34f) {
            Program echo = program.copy();
            echo.clauses.remove(Clause.ECHO);
            pendingCasts.add(new PendingCast(owner, echo, lane, quality,
                    localPower * 0.58f, empowered, 0.92f + program.elements.size() * 0.10f));
        }
        rewardCast(owner, quality, program, localPower);
    }

    private Entity createProjectile(int owner, int lane, Profile profile, float baseDamage,
                                    Program program, float powerScale, boolean empowered) {
        Entity entity = new Entity(Kind.PROJECTILE, owner, lane, profile.copy());
        entity.x = laneX[lane];
        entity.y = owner == 0 ? fieldBottom - dp(16f) : fieldTop + dp(16f);
        entity.radius = dp(10f) * (1f + profile.mass * 0.08f + profile.aether * 0.04f);
        entity.damage = baseDamage * profile.force() * powerScale;
        float speed = dp(120f) * selectedTempo.projectileScale
                * (0.82f + profile.impulse * 0.16f + profile.charge * 0.10f);
        if (selectedArena == ArenaType.TIDAL_ARCHIVE) speed *= 0.88f;
        if (laneTraits[lane] == LaneTrait.LEY_CURRENT) speed *= 1.18f;
        entity.velocity = owner == 0 ? -speed : speed;
        entity.program = program == null ? null : program.copy();
        entity.powerScale = powerScale;
        entity.empowered = empowered;
        if (empowered) { entity.damage *= 1.35f; entity.radius *= 1.18f; entity.velocity *= 1.08f; }
        return entity;
    }

    private void rewardCast(int owner, float quality, Program program, float powerScale) {
        if (powerScale < 0.85f) return;
        float resonance = 3f + quality * 4f + Math.max(0, program.elements.size() - 1) * 1.5f
                + program.clauses.size() * 1.1f;
        if (owner == 0) {
            manualCasts++;
            playerResonance = Math.min(100f, playerResonance + resonance);
        } else enemyResonance = Math.min(100f, enemyResonance + resonance);
    }

    private Entity findConsumable(int owner, int lane) {
        Entity best = null;
        float score = -1f;
        for (Entity entity : entities) {
            if (entity.dead || entity.owner != owner || entity.lane != lane) continue;
            if (entity.kind != Kind.GLYPH && entity.kind != Kind.FIELD
                    && entity.kind != Kind.WARD && entity.kind != Kind.ORBIT
                    && entity.kind != Kind.RIFT && entity.kind != Kind.AURA) continue;
            float candidate = entity.life + entity.hp * 0.1f + entity.profile.force();
            if (candidate > score) { score = candidate; best = entity; }
        }
        return best;
    }

    private Entity findAura(int owner) {
        for (Entity entity : entities) {
            if (!entity.dead && entity.kind == Kind.AURA && entity.owner == owner) return entity;
        }
        return null;
    }

    private Entity strongestHostilePersistent(int owner, int lane) {
        Entity best = null;
        float score = -1f;
        for (Entity entity : entities) {
            if (entity.dead || entity.owner == owner || entity.owner == OWNER_ENVIRONMENT
                    || entity.lane != lane) continue;
            if (!isConstruct(entity) && entity.kind != Kind.RIFT) continue;
            float candidate = entity.hp + entity.damage * 0.6f + entity.life * 0.4f;
            if (candidate > score) { score = candidate; best = entity; }
        }
        return best;
    }

    private Entity nearestAlliedBindable(int owner, int lane, float fromY) {
        Entity best = null;
        float distance = Float.MAX_VALUE;
        for (Entity entity : entities) {
            if (entity.dead || entity.owner != owner || entity.lane != lane || entity.kind == Kind.ENCHANTMENT) continue;
            if (entity.kind != Kind.PROJECTILE && entity.kind != Kind.SHARD && !isConstruct(entity)) continue;
            float d = Math.abs(entity.y - fromY);
            if (d < distance) { distance = d; best = entity; }
        }
        return best;
    }

    private void applyBinding(Entity target, Profile binding, float powerScale) {
        target.profile.blend(binding, 0.44f * powerScale);
        target.damage *= 1f + binding.heat * 0.08f + binding.charge * 0.08f + binding.entropy * 0.05f;
        target.hp *= 1f + binding.mass * 0.08f + binding.cohesion * 0.08f;
        target.life *= 1f + binding.aether * 0.08f + binding.cohesion * 0.05f;
        target.velocity *= 1f + binding.impulse * 0.06f;
        target.bound = true;
    }

    private boolean isConstruct(Entity entity) {
        return entity.kind == Kind.WARD || entity.kind == Kind.ORBIT || entity.kind == Kind.GLYPH
                || entity.kind == Kind.AURA;
    }

    private Entity nearestHostileInLane(int owner, int lane, boolean includeProjectiles) {
        Entity best = null;
        float sourceY = owner == 0 ? fieldBottom : fieldTop;
        float distance = Float.MAX_VALUE;
        for (Entity entity : entities) {
            if (entity.dead || entity.owner == owner || entity.lane != lane) continue;
            if (!includeProjectiles && !isConstruct(entity)) continue;
            if (entity.kind == Kind.FIELD || entity.kind == Kind.ENCHANTMENT || entity.kind == Kind.TRAP) continue;
            float d = Math.abs(entity.y - sourceY);
            if (d < distance) { distance = d; best = entity; }
        }
        return best;
    }

    private int chooseSeekLane(int owner, int currentLane) {
        int bestLane = currentLane;
        float bestPressure = lanePressure(1 - owner, currentLane);
        for (int direction : new int[]{-1, 1}) {
            int lane = currentLane + direction;
            if (lane < 0 || lane > 2) continue;
            float pressure = lanePressure(1 - owner, lane);
            if (pressure > bestPressure + 1f) { bestPressure = pressure; bestLane = lane; }
        }
        return bestLane;
    }

    private float lanePressure(int owner, int lane) {
        float pressure = nodes[lane].owner == owner ? nodes[lane].charge * 0.06f : 0f;
        for (Entity entity : entities) {
            if (entity.dead || entity.owner != owner || entity.lane != lane) continue;
            pressure += entity.damage * 0.18f + entity.hp * 0.08f + entity.life * 0.3f;
        }
        return pressure;
    }

    private void interactWithNode(Entity projectile, ArrayList<Entity> additions) {
        LeyNode node = nodes[projectile.lane];
        if (node.owner < 0) {
            attuneNode(node, projectile.owner, projectile.profile, 18f * projectile.powerScale);
            projectile.damage *= 1.05f;
            floatingTexts.add(new FloatingText("ATTUNE", projectile.x, nodeY, BRIGHT_GOLD, 0.8f));
        } else if (node.owner == projectile.owner) {
            projectile.profile.blend(node.profile, 0.12f + node.charge / 700f);
            projectile.damage *= 1f + Math.min(0.22f, node.charge / 420f);
            node.charge = Math.max(0f, node.charge - 4f);
            if (projectile.program != null && projectile.program.has(Clause.RELAY) && !projectile.relayed) {
                int relayLane = findRelayLane(projectile.owner, projectile.lane);
                if (relayLane != projectile.lane) {
                    projectile.relayed = true;
                    spawnArc(projectile.x, projectile.y, profileColor(projectile.profile), laneX[relayLane], nodeY, 5);
                    projectile.lane = relayLane;
                    projectile.x = laneX[relayLane];
                    projectile.damage *= 1.10f;
                    floatingTexts.add(new FloatingText("RELAY", projectile.x, projectile.y, BRIGHT_GOLD, 0.8f));
                }
            }
        } else {
            Reaction reaction = analyzeReaction(projectile.profile, node.profile);
            float attack = projectile.damage * projectile.profile.force();
            if (hasClause(projectile, Clause.SIPHON)) {
                boolean hexwright = projectile.owner == 0 && selectedArtifact == Artifact.HEXWRIGHT_RING;
                float drain = attack * (hexwright ? 0.55f : 0.42f);
                node.charge -= drain;
                projectile.profile.blend(node.profile, 0.15f);
                projectile.damage *= 1.08f;
                floatingTexts.add(new FloatingText("SIPHON", projectile.x, nodeY, Element.VOID.color, 0.9f));
                spawnArc(projectile.x, nodeY, projectile.profileColor(), 10);
                if (node.charge <= 0f) attuneNode(node, projectile.owner, projectile.profile, 12f);
                return;
            }
            node.charge -= attack * (0.30f + reaction.strength * 0.05f);
            projectile.damage *= Math.max(0.55f, 0.88f - node.profile.stability() * 0.05f);
            if (reaction.type == ReactionType.NULL_FLUX) {
                node.charge -= 22f;
                projectile.damage *= 1.20f;
            }
            floatingTexts.add(new FloatingText(reaction.label, projectile.x, nodeY, reaction.color, 0.9f));
            spawnArc(projectile.x, nodeY, reaction.color, 12);
            if (node.charge <= 0f) {
                attuneNode(node, projectile.owner, projectile.profile, 14f);
                playerResonance += projectile.owner == 0 ? 5f : 0f;
            }
        }
    }

    private void interactBeamWithNode(Entity beam, ArrayList<Entity> additions) {
        LeyNode node = nodes[beam.lane];
        if (node.owner == beam.owner) {
            beam.damage *= 1f + node.charge / 500f;
            node.charge = Math.max(0f, node.charge - 6f);
        } else {
            node.charge -= beam.damage * 0.20f;
            if (node.charge <= 0f) attuneNode(node, beam.owner, beam.profile, 12f);
        }
        if (beam.program != null && beam.program.has(Clause.RELAY)) {
            int relayLane = findRelayLane(beam.owner, beam.lane);
            if (relayLane != beam.lane) {
                Entity echo = new Entity(Kind.BEAM, beam.owner, relayLane, beam.profile.copy());
                echo.x = laneX[relayLane];
                echo.y = beam.y;
                echo.radius = beam.radius * 0.75f;
                echo.damage = beam.damage * 0.55f;
                echo.life = beam.life;
                echo.program = beam.program.copy();
                echo.program.clauses.remove(Clause.RELAY);
                additions.add(echo);
            }
        }
    }

    private int findRelayLane(int owner, int currentLane) {
        int best = currentLane;
        float bestScore = -1f;
        for (int direction : new int[]{-1, 1}) {
            int lane = currentLane + direction;
            if (lane < 0 || lane > 2) continue;
            LeyNode node = nodes[lane];
            if (node.owner == owner) {
                float score = node.charge + lanePressure(1 - owner, lane) * 0.8f;
                if (score > bestScore) { bestScore = score; best = lane; }
            }
        }
        return best;
    }

    private void attuneNode(LeyNode node, int owner, Profile profile, float amount) {
        if (node.profile == null || node.owner != owner) node.profile = profile.copy();
        else node.profile.blend(profile, 0.10f + amount / 500f);
        node.owner = owner;
        float multiplier = selectedArtifact == Artifact.LEY_KEY && owner == 0 ? 1.30f : 1f;
        if (selectedArena == ArenaType.ASTRAL_COURT) multiplier *= 1.15f;
        for (int lane = 0; lane < 3; lane++) {
            if (nodes[lane] == node && laneTraits[lane] == LaneTrait.THIN_VEIL) multiplier *= 1.40f;
        }
        node.charge = Math.min(100f, Math.max(0f, node.charge) + amount * multiplier);
    }

    /**
     * The arena periodically intervenes in the duel. Hazard fields are owned by
     * OWNER_ENVIRONMENT so they treat both duelists as hostile.
     */
    private void fireArenaEvent() {
        arenaEventTimer = 11f + random.nextFloat() * 5f;
        int lane = random.nextInt(3);
        float y = nodeY + (random.nextFloat() - 0.5f) * (fieldBottom - fieldTop) * 0.32f;
        switch (selectedArena) {
            case EMBER_VAULT: {
                Profile molten = new Profile();
                molten.heat = 1.25f;
                molten.mass = 0.8f;
                molten.volatility = 0.4f;
                molten.recalculate();
                molten.label = "Molten Vent";
                Entity vent = createField(OWNER_ENVIRONMENT, lane, y, molten, 5.5f);
                entities.add(vent);
                spawnShockwave(vent.x, vent.y, Element.FIRE.color, vent.radius * 1.6f);
                addShake(dp(4f));
                showBanner("VENT ERUPTION — " + laneName(lane).toUpperCase(Locale.US) + " LANE", 1.4f);
                break;
            }
            case TIDAL_ARCHIVE:
                tideSurgeTimer = 3.5f;
                for (int i = 0; i < 3; i++) spawnShockwave(laneX[i], nodeY, Element.WATER.color, dp(70f));
                showBanner("TIDE SURGE — SPELLS SLOWED AND SOAKED", 1.4f);
                break;
            case SHATTERED_CROWN: {
                Profile fracture = new Profile();
                fracture.mass = 1.0f;
                fracture.impulse = 0.9f;
                fracture.cold = 0.3f;
                fracture.recalculate();
                fracture.label = "Fracture";
                Entity scar = createField(OWNER_ENVIRONMENT, lane, y, fracture, 5f);
                entities.add(scar);
                spawnShockwave(scar.x, scar.y, Element.STONE.color, scar.radius * 1.5f);
                addShake(dp(5f));
                showBanner("FRACTURE — " + laneName(lane).toUpperCase(Locale.US) + " LANE TEARS OPEN", 1.4f);
                break;
            }
            case VERDANT_REACH: {
                Profile bloom = new Profile();
                bloom.growth = 1.3f;
                bloom.moisture = 0.6f;
                bloom.cohesion = 0.4f;
                bloom.recalculate();
                bloom.label = "Bloom";
                Entity garden = createField(OWNER_ENVIRONMENT, lane, y, bloom, 6f);
                entities.add(garden);
                spawnShockwave(garden.x, garden.y, Element.VERDANCE.color, garden.radius * 1.5f);
                showBanner("BLOOM — CONSTRUCTS MEND IN THE " + laneName(lane).toUpperCase(Locale.US) + " LANE", 1.4f);
                break;
            }
            case RADIANT_BASILICA:
                lightShaftLane = lane;
                lightShaftTimer = 4.5f;
                spawnShockwave(laneX[lane], nodeY, Element.RADIANCE.color, dp(90f));
                showBanner("LIGHT SHAFT — " + laneName(lane).toUpperCase(Locale.US) + " LANE CONSECRATED", 1.4f);
                break;
            case ASTRAL_COURT:
            default: {
                LeyNode node = nodes[lane];
                node.charge = Math.min(100f, node.charge + (node.owner >= 0 ? 30f : 16f));
                spawnShockwave(laneX[lane], nodeY, BRIGHT_GOLD, dp(60f));
                showBanner("LEY SURGE — " + laneName(lane).toUpperCase(Locale.US) + " NODE FLARES", 1.4f);
                break;
            }
        }
    }

    private Profile baseNodeProfile() {
        Profile profile = new Profile();
        if (selectedArena == ArenaType.EMBER_VAULT) profile.heat = 0.35f;
        else if (selectedArena == ArenaType.TIDAL_ARCHIVE) profile.moisture = 0.35f;
        else if (selectedArena == ArenaType.SHATTERED_CROWN) { profile.mass = 0.18f; profile.volatility = 0.22f; }
        else if (selectedArena == ArenaType.VERDANT_REACH) { profile.growth = 0.35f; profile.moisture = 0.10f; }
        else if (selectedArena == ArenaType.RADIANT_BASILICA) { profile.radiance = 0.35f; profile.cohesion = 0.10f; }
        else profile.aether = 0.30f;
        profile.recalculate();
        profile.label = selectedArena.label;
        return profile;
    }

    private enum ReactionType {
        RESONANCE, STEAM, THERMAL_SHOCK, CONDUCTION, MAGMA, BLIZZARD, SHATTER, NULL_FLUX, CORROSION,
        WILDFIRE, OVERGROWTH, SANCTIFY, ECLIPSE
    }

    private static final class Reaction {
        final ReactionType type;
        final float strength;
        final String label;
        final int color;

        Reaction(ReactionType type, float strength, String label, int color) {
            this.type = type;
            this.strength = strength;
            this.label = label;
            this.color = color;
        }
    }

    private Reaction analyzeReaction(Profile a, Profile b) {
        float steam = a.heat * b.moisture + b.heat * a.moisture;
        float thermal = a.heat * b.cold + b.heat * a.cold;
        float conduction = a.charge * b.moisture + b.charge * a.moisture;
        float magma = a.heat * b.mass + b.heat * a.mass;
        float blizzard = a.cold * b.impulse + b.cold * a.impulse;
        float shatter = a.mass * b.impulse + b.mass * a.impulse + a.cold * b.mass + b.cold * a.mass;
        float nullFlux = a.aether * b.entropy + b.aether * a.entropy;
        float corrosion = a.entropy * (b.moisture + b.cohesion) + b.entropy * (a.moisture + a.cohesion);
        float wildfire = a.heat * b.growth + b.heat * a.growth;
        float overgrowth = a.growth * b.moisture + b.growth * a.moisture;
        float sanctify = a.radiance * b.cohesion + b.radiance * a.cohesion;
        float eclipse = a.radiance * b.entropy + b.radiance * a.entropy;

        float best = 0.28f;
        Reaction result = new Reaction(ReactionType.RESONANCE, reactionStrength(a, b), "RESONANCE", BRIGHT_GOLD);
        if (steam > best) { best = steam; result = new Reaction(ReactionType.STEAM, steam, "STEAM VEIL", Color.rgb(175, 229, 236)); }
        if (thermal > best) { best = thermal; result = new Reaction(ReactionType.THERMAL_SHOCK, thermal, "THERMAL SHOCK", IVORY); }
        if (conduction > best) { best = conduction; result = new Reaction(ReactionType.CONDUCTION, conduction, "CONDUCTION", Element.LIGHTNING.color); }
        if (magma > best) { best = magma; result = new Reaction(ReactionType.MAGMA, magma, "MOLTEN FIELD", Color.rgb(255, 112, 53)); }
        if (blizzard > best) { best = blizzard; result = new Reaction(ReactionType.BLIZZARD, blizzard, "BLIZZARD", Element.FROST.color); }
        if (shatter > best) { best = shatter; result = new Reaction(ReactionType.SHATTER, shatter, "SHATTER", Element.STONE.color); }
        if (nullFlux > best) { best = nullFlux; result = new Reaction(ReactionType.NULL_FLUX, nullFlux, "NULL FLUX", Element.VOID.color); }
        if (wildfire > best) { best = wildfire; result = new Reaction(ReactionType.WILDFIRE, wildfire, "WILDFIRE", Color.rgb(255, 150, 40)); }
        if (overgrowth > best) { best = overgrowth; result = new Reaction(ReactionType.OVERGROWTH, overgrowth, "OVERGROWTH", Element.VERDANCE.color); }
        if (sanctify > best) { best = sanctify; result = new Reaction(ReactionType.SANCTIFY, sanctify, "SANCTIFY", Element.RADIANCE.color); }
        if (eclipse > best) { best = eclipse; result = new Reaction(ReactionType.ECLIPSE, eclipse, "ECLIPSE", Color.rgb(240, 200, 255)); }
        if (corrosion > best) result = new Reaction(ReactionType.CORROSION, corrosion, "CORROSION", Color.rgb(210, 86, 151));
        return result;
    }

    private float reactionStrength(Profile a, Profile b) {
        if (a == null || b == null) return 0f;
        return Math.max(0f,
                Math.abs(a.heat - b.cold) * 0.12f
                        + Math.abs(a.moisture - b.charge) * 0.08f
                        + Math.abs(a.mass - b.impulse) * 0.09f
                        + (a.aether * b.entropy + b.aether * a.entropy) * 0.18f);
    }

    private Profile mergeProfiles(Profile a, Profile b, float bWeight) {
        Profile merged = a.copy();
        merged.blend(b, bWeight);
        merged.label = a.label + "+" + b.label;
        return merged;
    }

    private boolean crossed(float from, float to, float point) {
        return (from <= point && to >= point) || (from >= point && to <= point);
    }

    private void prepareAiSpell() {
        Program program = new Program();
        int lane = chooseAiLane();
        Entity threat = nearestPlayerThreat();
        Element first = threat == null ? Element.values()[random.nextInt(Element.values().length)]
                : counterElement(dominantElement(threat.profile));
        program.elements.add(first);

        int maxElements = selectedDifficulty == Difficulty.APPRENTICE ? 2
                : selectedDifficulty == Difficulty.ADEPT ? 3 : 4;
        int count = 1 + random.nextInt(maxElements);
        while (program.elements.size() < count) {
            Element choice = Element.values()[random.nextInt(Element.values().length)];
            program.elements.add(choice);
        }

        float hostilePressure = lanePressure(0, lane);
        if (threat != null && isConstruct(threat)) program.form = random.nextBoolean() ? Form.LANCE : Form.BEAM;
        else if (hostilePressure > 12f) program.form = random.nextBoolean() ? Form.BURST : Form.GLYPH;
        else program.form = Form.values()[random.nextInt(Form.values().length)];
        // The rival keeps at most one aura; recasting one immediately would waste it.
        if (program.form == Form.AURA && findAura(1) != null) program.form = Form.LANCE;

        int clauseCount = selectedDifficulty == Difficulty.APPRENTICE ? (random.nextFloat() < 0.35f ? 1 : 0)
                : selectedDifficulty == Difficulty.ADEPT ? random.nextInt(3) : 1 + random.nextInt(3);
        ArrayList<Clause> pool = new ArrayList<Clause>();
        Collections.addAll(pool, Clause.values());
        Collections.shuffle(pool, random);
        for (Clause clause : pool) {
            if (program.clauses.size() >= clauseCount) break;
            if (clause == Clause.BIND && nearestAlliedBindable(1, lane, fieldTop) == null && random.nextBoolean()) continue;
            program.clauses.add(clause);
        }
        program.name = autoName(program);

        float base = selectedDifficulty == Difficulty.APPRENTICE ? 6.3f
                : selectedDifficulty == Difficulty.ADEPT ? 4.7f : 3.4f;
        float channel = base * selectedDifficulty.channelScale * selectedTempo.channelScale
                + program.elements.size() * 0.30f + program.clauses.size() * 0.22f;
        boolean empowered = enemyResonance >= 100f && random.nextFloat() < 0.65f;
        enemyIntent = new EnemyIntent(program, lane,
                selectedDifficulty == Difficulty.APPRENTICE ? 0.68f : selectedDifficulty == Difficulty.ADEPT ? 0.80f : 0.91f,
                channel, empowered);
    }

    private void updateEnemyIntent(float dt) {
        if (enemyIntent == null) return;
        enemyIntent.elapsed += dt;
        if (enemyIntent.elapsed < enemyIntent.totalTime) return;

        float cost = manaCost(enemyIntent.program, Artifact.PRISM_LENS);
        if (enemyMana >= cost || enemyIntent.empowered) {
            if (enemyIntent.empowered) enemyResonance = 0f;
            else enemyMana -= cost;
            deploySpell(1, enemyIntent.program, enemyIntent.lane, enemyIntent.quality, 1f, enemyIntent.empowered, false);
        }
        enemyIntent = null;
        aiCooldown = nextAiCooldown();
    }

    private float nextAiCooldown() {
        float base = selectedDifficulty == Difficulty.APPRENTICE ? 5.2f
                : selectedDifficulty == Difficulty.ADEPT ? 3.5f : 2.3f;
        return base + random.nextFloat() * (selectedDifficulty == Difficulty.APPRENTICE ? 2.0f : 1.2f);
    }

    private int chooseAiLane() {
        int bestLane = random.nextInt(3);
        float best = -Float.MAX_VALUE;
        for (int lane = 0; lane < 3; lane++) {
            float pressure = lanePressure(0, lane) + (nodes[lane].owner == 0 ? nodes[lane].charge * 0.08f : 0f);
            pressure += random.nextFloat() * 4f;
            if (pressure > best) { best = pressure; bestLane = lane; }
        }
        return bestLane;
    }

    private Entity nearestPlayerThreat() {
        Entity best = null;
        float bestY = Float.MAX_VALUE;
        for (Entity entity : entities) {
            if (entity.dead || entity.owner != 0) continue;
            float distance = Math.abs(entity.y - fieldTop);
            if (distance < bestY) { bestY = distance; best = entity; }
        }
        return best;
    }

    private Element dominantElement(Profile profile) {
        float best = profile.heat;
        Element result = Element.FIRE;
        if (profile.moisture > best) { best = profile.moisture; result = Element.WATER; }
        if (profile.impulse > best) { best = profile.impulse; result = Element.WIND; }
        if (profile.mass > best) { best = profile.mass; result = Element.STONE; }
        if (profile.cold > best) { best = profile.cold; result = Element.FROST; }
        if (profile.charge > best) { best = profile.charge; result = Element.LIGHTNING; }
        if (profile.aether > best) { best = profile.aether; result = Element.AETHER; }
        if (profile.entropy > best) { best = profile.entropy; result = Element.VOID; }
        if (profile.radiance > best) { best = profile.radiance; result = Element.RADIANCE; }
        if (profile.growth > best) result = Element.VERDANCE;
        return result;
    }

    private Element counterElement(Element element) {
        switch (element) {
            case FIRE: return Element.WATER;
            case WATER: return Element.LIGHTNING;
            case WIND: return Element.STONE;
            case STONE: return Element.FROST;
            case FROST: return Element.FIRE;
            case LIGHTNING: return Element.STONE;
            case AETHER: return Element.VOID;
            case VOID: return Element.RADIANCE;
            case RADIANCE: return Element.VOID;
            case VERDANCE: return Element.FIRE;
            default: return Element.WATER;
        }
    }

    private void resetCombat() {
        playerHealth = 100f;
        enemyHealth = 100f;
        playerMana = 100f;
        enemyMana = 100f;
        playerResonance = 0f;
        enemyResonance = 0f;
        playerInk = 3f;
        playerBurn = enemyBurn = playerSlow = enemySlow = 0f;
        duelClock = 0f;
        aiCooldown = practiceMode ? 12f : (selectedDifficulty == Difficulty.APPRENTICE ? 8f : 5f);
        enemyIntent = null;
        entities.clear();
        pendingCasts.clear();
        particles.clear();
        floatingTexts.clear();
        shockwaves.clear();
        damageDealt = reactionsWon = manualCasts = 0;
        arenaEventTimer = 9f + random.nextFloat() * 4f;
        tideSurgeTimer = 0f;
        lightShaftLane = -1;
        lightShaftTimer = 0f;
        shakeTimer = 0f;
        shakeMagnitude = 0f;
        ArrayList<LaneTrait> traitPool = new ArrayList<LaneTrait>();
        Collections.addAll(traitPool, LaneTrait.values());
        Collections.shuffle(traitPool, random);
        for (int lane = 0; lane < 3; lane++) laneTraits[lane] = traitPool.get(lane);
        for (LeyNode node : nodes) {
            node.owner = -1;
            node.charge = selectedArena == ArenaType.ASTRAL_COURT ? 18f : 10f;
            node.profile = baseNodeProfile();
        }
        syncCooldownList();
        for (int i = 0; i < executableCooldowns.size(); i++) executableCooldowns.set(i, 0f);
        resetComposer();
    }

    private void resetComposer() {
        composer = new Program();
        composer.form = Form.LANCE;
        composer.name = "Uncompiled Formula";
        composerStep = 0;
        selectedLane = -1;
        loadedExecutable = -1;
        currentStroke.clear();
        drawing = false;
    }

    private void startDuel(boolean practice) {
        practiceMode = practice;
        screen = Screen.DUEL;
        resetCombat();
        showBanner(practice ? "PRACTICE CIRCLE — THE RIVAL WAITS WHILE YOU COMPOSE"
                : selectedArena.label.toUpperCase(Locale.US) + " — LEY NODES AWAKEN", 2.4f);
    }

    private void finishDuel(boolean victory) {
        playerHealth = Math.max(0f, playerHealth);
        enemyHealth = Math.max(0f, enemyHealth);
        screen = Screen.GAME_OVER;
        resultTitle = victory ? "CIRCLE ASCENDANT" : "SIGIL FRACTURED";
        resultSubtitle = victory ? "Your syntax rewrote the arena." : "The rival seized the ley architecture.";
        enemyIntent = null;
    }

    private void showBanner(String text, float duration) {
        banner = text;
        bannerTimer = duration;
    }

    private float currentManaCost() {
        if (!composer.valid()) return 0f;
        float cost = manaCost(composer, selectedArtifact);
        if (loadedExecutable >= 0) cost *= executableSurcharge(selectedArtifact);
        return cost;
    }

    private boolean canAdvanceTo(int step) {
        if (step <= 0) return true;
        if (composer.elements.isEmpty()) return false;
        if (step <= 1) return true;
        if (composer.form == null) return false;
        if (step <= 3) return true;
        return selectedLane >= 0;
    }

    private void advanceComposer() {
        if (composerStep == 0 && composer.elements.isEmpty()) {
            showBanner("DRAW AT LEAST ONE SIGIL", 1.0f);
            return;
        }
        if (composerStep == 3 && selectedLane < 0) {
            showBanner("SELECT A LANE", 1.0f);
            return;
        }
        composerStep = Math.min(4, composerStep + 1);
    }

    private void castComposer() {
        if (!composer.valid() || selectedLane < 0) {
            showBanner("COMPLETE SIGILS, FORM, AND LANE", 1.1f);
            return;
        }
        float cost = currentManaCost();
        boolean empowered = playerResonance >= 100f;
        if (!empowered && playerMana < cost) {
            showBanner("NEED " + Math.round(cost) + " MANA", 1.0f);
            return;
        }
        if (loadedExecutable >= 0) {
            if (loadedExecutable >= executableCooldowns.size()) { resetComposer(); return; }
            if (executableCooldowns.get(loadedExecutable) > 0f) {
                showBanner("EXECUTABLE COOLING", 1.0f);
                return;
            }
            if (playerInk < 0.99f) {
                showBanner("NO INK — WRITE CLEAN MANUAL SIGILS", 1.2f);
                return;
            }
            playerInk -= 1f;
            executableCooldowns.set(loadedExecutable, 5.5f + composer.clauses.size() * 0.7f);
        }
        if (empowered) playerResonance = 0f;
        else playerMana -= cost;

        deploySpell(0, composer, selectedLane, lastGlyphQuality, 1f, empowered, false);
        if (loadedExecutable < 0) {
            float qualityGain = Math.max(0f, lastGlyphQuality - 0.55f) * 1.2f;
            if (selectedArtifact == Artifact.ASHEN_QUILL) qualityGain *= 1.55f;
            if (selectedArtifact == Artifact.MNEMONIC_CROWN) qualityGain *= 0.55f;
            playerInk = Math.min(6f, playerInk + qualityGain);
        }
        showBanner((empowered ? "ASCENDANT " : "") + composer.name.toUpperCase(Locale.US), 1.0f);
        resetComposer();
    }

    private void loadExecutable(int index) {
        if (index < 0 || index >= library.size()) return;
        if (executableCooldowns.get(index) > 0f) {
            showBanner("EXECUTABLE COOLING: " + Math.round(executableCooldowns.get(index)) + "s", 1.0f);
            return;
        }
        composer = library.get(index).copy();
        loadedExecutable = index;
        selectedLane = -1;
        composerStep = 3;
        showBanner("EXECUTABLE LOADED — CHOOSE A LANE", 1.0f);
    }

    private void recognizeStroke() {
        if (currentStroke.size() < 6 || pathLength(currentStroke) < dp(42f)) {
            showBanner("SIGIL TOO SHORT — TRY AGAIN", 1.0f);
            currentStroke.clear();
            return;
        }
        GlyphResult result = recognizer.recognize(currentStroke);
        currentStroke.clear();
        if (result.score < 0.37f) {
            showBanner("UNSTABLE GLYPH — TRACE A CLEARER SHAPE", 1.1f);
            return;
        }
        if (composer.elements.size() >= 4) {
            showBanner("THE CORE HOLDS FOUR SIGILS", 1.0f);
            return;
        }
        loadedExecutable = -1;
        composer.elements.add(result.element);
        composer.name = autoName(composer);
        lastGlyphQuality = result.score;
        showBanner(result.element.label.toUpperCase(Locale.US) + " • " + Math.round(result.score * 100f) + "%", 0.9f);
        spawnCastParticles(drawingPad.centerX(), drawingPad.centerY(), result.element.color, 18);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (screen == Screen.TITLE) return handleTitleTouch(event, x, y);
        if (screen == Screen.GRIMOIRE) return handleGrimoireTouch(event, x, y);
        if (screen == Screen.CODEX) return handleCodexTouch(event, x, y);
        if (screen == Screen.GAME_OVER) return handleGameOverTouch(event, x, y);
        return handleDuelTouch(event, x, y);
    }

    private boolean handleTitleTouch(MotionEvent event, float x, float y) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        for (int i = 0; i < artifactRects.length; i++) {
            if (artifactRects[i].contains(x, y)) {
                selectedArtifact = Artifact.values()[i];
                preferences.edit().putInt("artifact", i).apply();
                showBanner(selectedArtifact.label.toUpperCase(Locale.US), 0.8f);
                return true;
            }
        }
        for (int i = 0; i < difficultyRects.length; i++) {
            if (difficultyRects[i].contains(x, y)) {
                selectedDifficulty = Difficulty.values()[i];
                preferences.edit().putInt("difficulty", i).apply();
                showBanner(selectedDifficulty.label.toUpperCase(Locale.US), 0.8f);
                return true;
            }
        }
        for (int i = 0; i < tempoRects.length; i++) {
            if (tempoRects[i].contains(x, y)) {
                selectedTempo = Tempo.values()[i];
                preferences.edit().putInt("tempo", i).apply();
                showBanner(selectedTempo.label.toUpperCase(Locale.US), 0.8f);
                return true;
            }
        }
        for (int i = 0; i < arenaRects.length; i++) {
            if (arenaRects[i].contains(x, y)) {
                selectedArena = ArenaType.values()[i];
                preferences.edit().putInt("arena", i).apply();
                backgroundShader = null;
                showBanner(selectedArena.label.toUpperCase(Locale.US), 0.9f);
                return true;
            }
        }
        if (mainButton.contains(x, y)) startDuel(false);
        else if (secondaryButton.contains(x, y)) startDuel(true);
        else if (grimoireButton.contains(x, y)) {
            screen = Screen.GRIMOIRE;
            editingProgram = library.isEmpty() ? -1 : 0;
            editorProgram = library.isEmpty() ? new Program() : library.get(0).copy();
            grimoireScroll = 0;
        } else if (codexButton.contains(x, y)) {
            codexPage = 0;
            screen = Screen.CODEX;
        }
        return true;
    }

    private boolean handleDuelTouch(MotionEvent event, float x, float y) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (composerStep == 0 && drawingPad.contains(x, y)) {
                drawing = true;
                currentStroke.clear();
                currentStroke.add(new PointF(x, y));
                return true;
            }
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && drawing) {
            float px = clamp(x, drawingPad.left, drawingPad.right);
            float py = clamp(y, drawingPad.top, drawingPad.bottom);
            PointF last = currentStroke.get(currentStroke.size() - 1);
            if (distance(last.x, last.y, px, py) > dp(2f)) currentStroke.add(new PointF(px, py));
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            drawing = false;
            currentStroke.clear();
            return true;
        }
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;

        if (drawing) {
            drawing = false;
            currentStroke.add(new PointF(clamp(x, drawingPad.left, drawingPad.right),
                    clamp(y, drawingPad.top, drawingPad.bottom)));
            recognizeStroke();
            return true;
        }

        if (executablePrev.contains(x, y)) {
            executablePage = Math.max(0, executablePage - 1);
            return true;
        }
        if (executableNext.contains(x, y)) {
            executablePage = Math.min(Math.max(0, library.size() - 4), executablePage + 1);
            return true;
        }
        for (int i = 0; i < executableCardRects.length; i++) {
            if (executableCardRects[i].contains(x, y)) {
                int index = executablePage + i;
                if (index < library.size()) loadExecutable(index);
                else {
                    screen = Screen.GRIMOIRE;
                    editingProgram = -1;
                    editorProgram = new Program();
                }
                return true;
            }
        }
        for (int i = 0; i < stepRects.length; i++) {
            if (stepRects[i].contains(x, y)) {
                if (canAdvanceTo(i)) composerStep = i;
                else showBanner("COMPLETE THE EARLIER CLAUSE OF THE SENTENCE", 1.0f);
                return true;
            }
        }
        if (backStepButton.contains(x, y)) {
            if (composerStep > 0) composerStep--;
            else resetComposer();
            return true;
        }

        if (composerStep == 0) {
            if (undoButton.contains(x, y)) {
                if (!composer.elements.isEmpty()) composer.elements.remove(composer.elements.size() - 1);
                loadedExecutable = -1;
                composer.name = autoName(composer);
                return true;
            }
            if (clearButton.contains(x, y)) {
                resetComposer();
                showBanner("FORMULA CLEARED", 0.8f);
                return true;
            }
            if (continueButton.contains(x, y)) { advanceComposer(); return true; }
        } else if (composerStep == 1) {
            for (int i = 0; i < formRects.length; i++) {
                if (formRects[i].contains(x, y)) {
                    composer.form = Form.values()[i];
                    composer.name = autoName(composer);
                    loadedExecutable = -1;
                    showBanner(composer.form.label.toUpperCase(Locale.US) + " FORM", 0.8f);
                    return true;
                }
            }
            if (continueButton.contains(x, y)) { advanceComposer(); return true; }
        } else if (composerStep == 2) {
            for (int i = 0; i < clauseRects.length; i++) {
                if (clauseRects[i].contains(x, y)) {
                    Clause clause = Clause.values()[i];
                    loadedExecutable = -1;
                    if (composer.clauses.contains(clause)) composer.clauses.remove(clause);
                    else if (composer.clauses.size() < 3) composer.clauses.add(clause);
                    else showBanner("A SENTENCE HOLDS THREE ORDERED CLAUSES", 1.0f);
                    composer.name = autoName(composer);
                    return true;
                }
            }
            if (continueButton.contains(x, y)) { advanceComposer(); return true; }
        } else if (composerStep == 3) {
            for (int i = 0; i < laneRects.length; i++) {
                if (laneRects[i].contains(x, y)) {
                    selectedLane = i;
                    composerStep = 4;
                    return true;
                }
            }
        } else if (composerStep == 4) {
            if (castButton.contains(x, y)) { castComposer(); return true; }
            if (clearButton.contains(x, y)) { resetComposer(); return true; }
        }
        return true;
    }

    private boolean handleGrimoireTouch(MotionEvent event, float x, float y) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        if (grimoireBack.contains(x, y)) { screen = Screen.TITLE; return true; }
        if (grimoireUp.contains(x, y)) { grimoireScroll = Math.max(0, grimoireScroll - 1); return true; }
        if (grimoireDown.contains(x, y)) {
            grimoireScroll = Math.min(Math.max(0, library.size() - 4), grimoireScroll + 1);
            return true;
        }
        for (int i = 0; i < grimoireCards.length; i++) {
            if (grimoireCards[i].contains(x, y)) {
                int index = grimoireScroll + i;
                if (index < library.size()) {
                    editingProgram = index;
                    editorProgram = library.get(index).copy();
                }
                return true;
            }
        }
        if (grimoireAdd.contains(x, y)) {
            editingProgram = -1;
            editorProgram = new Program();
            editorProgram.form = Form.LANCE;
            return true;
        }
        if (grimoireDuplicate.contains(x, y)) {
            if (editingProgram >= 0 && editingProgram < library.size()) {
                Program copy = library.get(editingProgram).copy();
                copy.name = copy.name + " Copy";
                library.add(copy);
                saveLibrary();
                editingProgram = library.size() - 1;
                editorProgram = copy.copy();
                grimoireScroll = Math.max(0, library.size() - 4);
            }
            return true;
        }
        if (grimoireDelete.contains(x, y)) {
            if (editingProgram >= 0 && editingProgram < library.size()) {
                library.remove(editingProgram);
                saveLibrary();
                editingProgram = library.isEmpty() ? -1 : Math.min(editingProgram, library.size() - 1);
                editorProgram = editingProgram < 0 ? new Program() : library.get(editingProgram).copy();
                grimoireScroll = Math.min(grimoireScroll, Math.max(0, library.size() - 4));
            }
            return true;
        }
        if (grimoireUndo.contains(x, y)) {
            if (!editorProgram.clauses.isEmpty()) editorProgram.clauses.remove(editorProgram.clauses.size() - 1);
            else if (!editorProgram.elements.isEmpty()) editorProgram.elements.remove(editorProgram.elements.size() - 1);
            editorProgram.name = autoName(editorProgram);
            return true;
        }
        for (int i = 0; i < editorElementRects.length; i++) {
            if (editorElementRects[i].contains(x, y)) {
                if (editorProgram.elements.size() < 4) editorProgram.elements.add(Element.values()[i]);
                else showBanner("FOUR SIGILS MAXIMUM", 0.9f);
                editorProgram.name = autoName(editorProgram);
                return true;
            }
        }
        for (int i = 0; i < editorFormRects.length; i++) {
            if (editorFormRects[i].contains(x, y)) {
                editorProgram.form = Form.values()[i];
                editorProgram.name = autoName(editorProgram);
                return true;
            }
        }
        for (int i = 0; i < editorClauseRects.length; i++) {
            if (editorClauseRects[i].contains(x, y)) {
                Clause clause = Clause.values()[i];
                if (editorProgram.clauses.contains(clause)) editorProgram.clauses.remove(clause);
                else if (editorProgram.clauses.size() < 3) editorProgram.clauses.add(clause);
                else showBanner("THREE CLAUSES MAXIMUM", 0.9f);
                editorProgram.name = autoName(editorProgram);
                return true;
            }
        }
        if (grimoireSave.contains(x, y)) {
            if (!editorProgram.valid()) { showBanner("ADD AT LEAST ONE ELEMENT", 1.0f); return true; }
            editorProgram.name = autoName(editorProgram);
            if (editingProgram >= 0 && editingProgram < library.size()) library.set(editingProgram, editorProgram.copy());
            else {
                library.add(editorProgram.copy());
                editingProgram = library.size() - 1;
                grimoireScroll = Math.max(0, library.size() - 4);
            }
            saveLibrary();
            showBanner("EXECUTABLE SAVED", 0.9f);
            return true;
        }
        return true;
    }

    private boolean handleCodexTouch(MotionEvent event, float x, float y) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        if (codexBack.contains(x, y)) { screen = Screen.TITLE; return true; }
        if (codexPrev.contains(x, y)) { codexPage = Math.max(0, codexPage - 1); return true; }
        if (codexNext.contains(x, y)) { codexPage = Math.min(4, codexPage + 1); return true; }
        if (codexPractice.contains(x, y)) { startDuel(true); return true; }
        return true;
    }

    private boolean handleGameOverTouch(MotionEvent event, float x, float y) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        if (mainButton.contains(x, y)) startDuel(practiceMode);
        else if (secondaryButton.contains(x, y)) screen = Screen.TITLE;
        return true;
    }

    private void drawBackground(Canvas canvas) {
        if (backgroundShader == null) {
            backgroundShader = new LinearGradient(0, 0, 0, getHeight(),
                    new int[]{VOID_BLACK, selectedArena.shadow, DEEP_INK},
                    new float[]{0f, 0.48f, 1f}, Shader.TileMode.CLAMP);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(backgroundShader);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(Color.argb(22, 255, 225, 146));
        float spacing = dp(52f);
        for (float x = -getHeight(); x < getWidth() + getHeight(); x += spacing) {
            canvas.drawLine(x, 0, x - getHeight() * 0.22f, getHeight(), paint);
        }

        // Giant ley circle watermark rotating slowly behind the play field.
        float wx = getWidth() * 0.5f;
        float wy = (fieldTop + fieldBottom) * 0.5f;
        float wr = getWidth() * 0.58f;
        canvas.save();
        canvas.rotate(uiClock * 2.1f, wx, wy);
        paint.setColor(Color.argb(16, 255, 225, 146));
        paint.setStrokeWidth(dp(1.2f));
        canvas.drawCircle(wx, wy, wr, paint);
        canvas.drawCircle(wx, wy, wr * 0.84f, paint);
        drawArcanePolygonAlpha(canvas, wx, wy, wr * 0.92f, 8, 16);
        drawArcanePolygonAlpha(canvas, wx, wy, wr * 0.66f, 6, 12);
        for (int i = 0; i < 8; i++) {
            float a = (float) (Math.PI * 2.0 * i / 8.0);
            canvas.drawLine(wx + (float) Math.cos(a) * wr * 0.84f, wy + (float) Math.sin(a) * wr * 0.84f,
                    wx + (float) Math.cos(a) * wr, wy + (float) Math.sin(a) * wr, paint);
        }
        canvas.restore();

        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 40; i++) {
            float x = ((i * 83) % Math.max(1, getWidth())) + (float) Math.sin(i * 2.7 + uiClock * 0.14f) * dp(8f);
            float y = ((i * 137) % Math.max(1, getHeight())) - uiClock * dp(1.4f + (i % 3)) % Math.max(1, getHeight());
            if (y < 0) y += getHeight();
            paint.setColor(Color.argb(16 + (i % 3) * 9, 255, 225, 146));
            canvas.drawCircle(x, y, dp(0.7f + (i % 2) * 0.5f), paint);
        }

        // Vignette pulls the eye toward the play field.
        paint.setShader(new RadialGradient(getWidth() * 0.5f, getHeight() * 0.44f,
                Math.max(getWidth(), getHeight()) * 0.78f,
                new int[]{Color.TRANSPARENT, Color.TRANSPARENT, Color.argb(150, 3, 3, 8)},
                new float[]{0f, 0.62f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        paint.setShader(null);
    }

    private void drawArcanePolygonAlpha(Canvas canvas, float cx, float cy, float radius, int sides, int alpha) {
        Path path = new Path();
        for (int i = 0; i <= sides; i++) {
            float angle = (float) (-Math.PI / 2.0 + Math.PI * 2.0 * i / sides);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.argb(alpha, 255, 225, 146));
        canvas.drawPath(path, paint);
    }

    private void drawTitle(Canvas canvas) {
        float cx = getWidth() * 0.5f;
        drawArcaneCrest(canvas, cx, dp(74f), dp(48f), uiClock * 8f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(29f));
        paint.setColor(IVORY);
        canvas.drawText("SIGILBOUND", cx, dp(91f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(12.5f));
        paint.setColor(MUTED);
        canvas.drawText("ARCANE ARCHITECTURE", cx, dp(113f), paint);
        drawSmallGoldButton(canvas, codexButton, "?", false);

        drawSectionLabel(canvas, "ARTIFACT", artifactRects[0].left, artifactRects[0].top - dp(8f));
        for (int i = 0; i < artifactRects.length; i++) {
            drawOptionCard(canvas, artifactRects[i], Artifact.values()[i].label,
                    shortArtifactLabel(Artifact.values()[i]), selectedArtifact == Artifact.values()[i], GOLD);
        }
        drawSectionLabel(canvas, "RIVAL", difficultyRects[0].left, difficultyRects[0].top - dp(8f));
        for (int i = 0; i < difficultyRects.length; i++) {
            drawOptionCard(canvas, difficultyRects[i], Difficulty.values()[i].label,
                    Difficulty.values()[i].subtitle, selectedDifficulty == Difficulty.values()[i], selectedArena.accent);
        }
        drawSectionLabel(canvas, "TEMPO", tempoRects[0].left, tempoRects[0].top - dp(8f));
        for (int i = 0; i < tempoRects.length; i++) {
            drawOptionCard(canvas, tempoRects[i], Tempo.values()[i].label,
                    Tempo.values()[i].subtitle, selectedTempo == Tempo.values()[i], INK_BLUE);
        }
        drawSectionLabel(canvas, "ARENA", arenaRects[0].left, arenaRects[0].top - dp(8f));
        for (int i = 0; i < arenaRects.length; i++) {
            ArenaType arena = ArenaType.values()[i];
            drawOptionCard(canvas, arenaRects[i], arena.label,
                    shortArenaLabel(arena), selectedArena == arena, arena.accent);
        }

        drawGoldButton(canvas, mainButton, "ENTER DUEL", "contest the ley nodes", true);
        drawGoldButton(canvas, secondaryButton, "PRACTICE", "slow rival", false);
        drawGoldButton(canvas, grimoireButton, "GRIMOIRE", library.size() + " executables", false);
        drawBanner(canvas);
    }

    private String shortArtifactLabel(Artifact artifact) {
        switch (artifact) {
            case PRISM_LENS: return "weight";
            case ASHEN_QUILL: return "precision";
            case AEGIS_BELL: return "constructs";
            case LEY_KEY: return "nodes";
            case HEXWRIGHT_RING: return "curses";
            case VERDANT_CHALICE: return "growth";
            default: return "executables";
        }
    }

    private String shortArenaLabel(ArenaType arena) {
        switch (arena) {
            case ASTRAL_COURT: return "resonance";
            case EMBER_VAULT: return "heat";
            case TIDAL_ARCHIVE: return "conduction";
            case VERDANT_REACH: return "growth";
            case RADIANT_BASILICA: return "light";
            default: return "fracture";
        }
    }

    private void drawOptionCard(Canvas canvas, RectF rect, String title, String subtitle,
                                boolean selected, int accent) {
        drawMetalPanel(canvas, rect, selected, accent);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(rect.width() < dp(74f) ? 9.8f : 11f));
        paint.setColor(selected ? IVORY : Color.rgb(221, 216, 226));
        canvas.drawText(ellipsize(title.toUpperCase(Locale.US), rect.width() - dp(8f), paint),
                rect.centerX(), rect.top + dp(22f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(9.5f));
        paint.setColor(selected ? BRIGHT_GOLD : MUTED);
        canvas.drawText(ellipsize(subtitle, rect.width() - dp(8f), paint),
                rect.centerX(), rect.bottom - dp(12f), paint);
    }

    private void drawGrimoire(Canvas canvas) {
        drawSmallGoldButton(canvas, grimoireBack, "‹", false);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(24f));
        paint.setColor(IVORY);
        canvas.drawText("THE LIVING GRIMOIRE", getWidth() * 0.5f, dp(54f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(11.5f));
        paint.setColor(MUTED);
        canvas.drawText("No slot limit. Prepare as many executable sentences as your playstyle needs.",
                getWidth() * 0.5f, dp(73f), paint);

        drawMetalPanel(canvas, grimoireList, false, GOLD);
        drawSmallGoldButton(canvas, grimoireUp, "‹", false);
        drawSmallGoldButton(canvas, grimoireDown, "›", false);
        for (int i = 0; i < grimoireCards.length; i++) {
            int index = grimoireScroll + i;
            RectF rect = grimoireCards[i];
            if (index < library.size()) drawLibraryCard(canvas, rect, library.get(index), index, index == editingProgram);
            else drawEmptyLibraryCard(canvas, rect);
        }
        drawSmallGoldButton(canvas, grimoireAdd, "+ NEW", false);
        drawSmallGoldButton(canvas, grimoireDuplicate, "DUPLICATE", false);
        drawSmallGoldButton(canvas, grimoireDelete, "DELETE", false);

        float titleY = editorElementRects[0].top - dp(47f);
        drawSectionLabel(canvas, editingProgram < 0 ? "NEW EXECUTABLE" : "EDIT EXECUTABLE " + (editingProgram + 1),
                dp(14f), titleY);
        drawProgramString(canvas, editorProgram, dp(14f), titleY + dp(19f), getWidth() - dp(28f));

        drawSectionLabel(canvas, "SIGILS — TAP TO APPEND, REPEATS ALLOWED", dp(14f), editorElementRects[0].top - dp(8f));
        for (int i = 0; i < editorElementRects.length; i++) {
            drawElementEditorButton(canvas, editorElementRects[i], Element.values()[i]);
        }
        drawSectionLabel(canvas, "FORM RUNE", dp(14f), editorFormRects[0].top - dp(8f));
        for (int i = 0; i < editorFormRects.length; i++) {
            drawFormCard(canvas, editorFormRects[i], Form.values()[i], editorProgram.form == Form.values()[i], false);
        }
        drawSectionLabel(canvas, "ORDERED CLAUSES", dp(14f), editorClauseRects[0].top - dp(8f));
        for (int i = 0; i < editorClauseRects.length; i++) {
            Clause clause = Clause.values()[i];
            int order = editorProgram.clauses.indexOf(clause);
            drawClauseCard(canvas, editorClauseRects[i], clause, order, false);
        }
        drawSmallGoldButton(canvas, grimoireUndo, "UNDO", false);
        drawGoldButton(canvas, grimoireSave, "SAVE EXECUTABLE", autoName(editorProgram), true);
        drawBanner(canvas);
    }

    private void drawLibraryCard(Canvas canvas, RectF rect, Program program, int index, boolean selected) {
        drawMetalPanel(canvas, rect, selected, selected ? BRIGHT_GOLD : program.elements.get(0).color);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(8.5f));
        paint.setColor(selected ? BRIGHT_GOLD : MUTED);
        canvas.drawText(String.format(Locale.US, "%02d", index + 1), rect.left + dp(7f), rect.top + dp(13f), paint);

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(9.2f));
        paint.setColor(IVORY);
        canvas.drawText(ellipsize(program.name, rect.width() - dp(42f), paint), rect.centerX() + dp(8f), rect.top + dp(14f), paint);

        float glyphY = rect.centerY() + dp(1f);
        float startX = rect.centerX() - (program.elements.size() - 1) * dp(9f);
        for (int i = 0; i < program.elements.size(); i++) {
            drawMiniGlyph(canvas, program.elements.get(i), startX + i * dp(18f), glyphY, dp(8f), 1f);
        }
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(8.2f));
        paint.setColor(BRIGHT_GOLD);
        String footer = program.form.label + (program.clauses.isEmpty() ? "" : " · " + program.clauses.size() + " clauses");
        canvas.drawText(ellipsize(footer, rect.width() - dp(10f), paint), rect.centerX(), rect.bottom - dp(7f), paint);
    }

    private void drawEmptyLibraryCard(Canvas canvas, RectF rect) {
        drawMetalPanel(canvas, rect, false, OLD_GOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(11f));
        paint.setColor(Color.rgb(100, 94, 111));
        canvas.drawText("EMPTY", rect.centerX(), rect.centerY(), paint);
    }

    private void drawElementEditorButton(Canvas canvas, RectF rect, Element element) {
        drawMetalPanel(canvas, rect, false, element.color);
        drawMiniGlyph(canvas, element, rect.left + dp(20f), rect.centerY(), dp(14f), 1f);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(11f));
        paint.setColor(IVORY);
        canvas.drawText(element.label.toUpperCase(Locale.US), rect.left + dp(38f), rect.centerY() - dp(2f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(9.5f));
        paint.setColor(MUTED);
        canvas.drawText(element.glyphHint, rect.left + dp(38f), rect.centerY() + dp(12f), paint);
    }

    private void drawCodex(Canvas canvas) {
        drawSmallGoldButton(canvas, codexBack, "‹", false);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(25f));
        paint.setColor(IVORY);
        String title = codexPage == 0 ? "SIGIL CODEX" : codexPage == 1 ? "FORM RUNES"
                : codexPage == 2 ? "CLAUSE SYNTAX" : codexPage == 3 ? "LEY ARCHITECTURE" : "THE LIVING WEAVE";
        canvas.drawText(title, getWidth() * 0.5f, dp(55f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(12f));
        paint.setColor(MUTED);
        canvas.drawText((codexPage + 1) + " / 5", getWidth() * 0.5f, dp(76f), paint);

        RectF page = new RectF(dp(14f), dp(94f), getWidth() - dp(14f), getHeight() - dp(82f));
        drawMetalPanel(canvas, page, false, selectedArena.accent);
        if (codexPage == 0) drawElementCodex(canvas, page);
        else if (codexPage == 1) drawFormCodex(canvas, page);
        else if (codexPage == 2) drawClauseCodex(canvas, page);
        else if (codexPage == 3) drawArenaCodex(canvas, page);
        else drawInteractionCodex(canvas, page);

        drawSmallGoldButton(canvas, codexPrev, "PREV", false);
        drawSmallGoldButton(canvas, codexNext, "NEXT", false);
        drawGoldButton(canvas, codexPractice, "PRACTICE CIRCLE", "rival waits 10 seconds", true);
    }

    private void drawElementCodex(Canvas canvas, RectF page) {
        float margin = dp(12f);
        float gap = dp(7f);
        float cardW = (page.width() - margin * 2f - gap) / 2f;
        float cardH = (page.height() - margin * 2f - gap * 4f) / 5f;
        for (int i = 0; i < Element.values().length; i++) {
            int column = i % 2;
            int row = i / 2;
            RectF rect = new RectF(page.left + margin + column * (cardW + gap),
                    page.top + margin + row * (cardH + gap),
                    page.left + margin + column * (cardW + gap) + cardW,
                    page.top + margin + row * (cardH + gap) + cardH);
            Element element = Element.values()[i];
            drawMetalPanel(canvas, rect, false, element.color);
            drawMiniGlyph(canvas, element, rect.left + dp(26f), rect.centerY(), dp(17f), 1f);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(12f));
            paint.setColor(IVORY);
            canvas.drawText(element.label, rect.left + dp(52f), rect.centerY() - dp(7f), paint);
            paint.setTypeface(UI_REGULAR);
            paint.setTextSize(sp(9.5f));
            paint.setColor(element.color);
            canvas.drawText(element.glyphHint, rect.left + dp(52f), rect.centerY() + dp(8f), paint);
            paint.setColor(MUTED);
            canvas.drawText(ellipsize(element.subtitle, rect.width() - dp(60f), paint),
                    rect.left + dp(52f), rect.centerY() + dp(22f), paint);
        }
    }

    private void drawFormCodex(Canvas canvas, RectF page) {
        float margin = dp(12f), gap = dp(8f);
        float cardW = (page.width() - margin * 2f - gap * 2f) / 3f;
        float cardH = (page.height() - margin * 2f - gap * 2f) / 3f;
        for (int i = 0; i < Form.values().length; i++) {
            int column = i % 3;
            int row = i / 3;
            RectF rect = new RectF(page.left + margin + column * (cardW + gap),
                    page.top + margin + row * (cardH + gap),
                    page.left + margin + column * (cardW + gap) + cardW,
                    page.top + margin + row * (cardH + gap) + cardH);
            drawFormCard(canvas, rect, Form.values()[i], false, true);
        }
    }

    private void drawClauseCodex(Canvas canvas, RectF page) {
        float margin = dp(12f), gap = dp(7f);
        float cardW = (page.width() - margin * 2f - gap) / 2f;
        float cardH = (page.height() - margin * 2f - gap * 5f) / 6f;
        for (int i = 0; i < Clause.values().length; i++) {
            int column = i % 2;
            int row = i / 2;
            RectF rect = new RectF(page.left + margin + column * (cardW + gap),
                    page.top + margin + row * (cardH + gap),
                    page.left + margin + column * (cardW + gap) + cardW,
                    page.top + margin + row * (cardH + gap) + cardH);
            drawClauseCard(canvas, rect, Clause.values()[i], -1, true);
        }
    }

    private void drawArenaCodex(Canvas canvas, RectF page) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(15f));
        paint.setColor(BRIGHT_GOLD);
        canvas.drawText("THE ARENA IS PART OF THE SPELL", page.left + dp(18f), page.top + dp(36f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(12f));
        paint.setColor(IVORY);
        String body = "Each lane contains a contestable ley node and a lane trait rolled at the start of every duel. "
                + "Passing allied spells inherit part of a node's profile; hostile spells fight its charge. "
                + "Every arena also intervenes: vents erupt, tides surge, lanes fracture, blooms mend, light shafts consecrate, and ley nodes flare. "
                + "Environmental hazards belong to no one — position around them or Consume, Siphon, and Surge them away.";
        drawWrappedText(canvas, body, page.left + dp(18f), page.top + dp(62f), page.width() - dp(36f), sp(11f), dp(16f), 8, Paint.Align.LEFT);

        float y = page.top + dp(206f);
        for (ArenaType arena : ArenaType.values()) {
            RectF rect = new RectF(page.left + dp(18f), y, page.right - dp(18f), y + dp(56f));
            if (rect.bottom > page.bottom - dp(8f)) break;
            drawMetalPanel(canvas, rect, arena == selectedArena, arena.accent);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(12f));
            paint.setColor(IVORY);
            canvas.drawText(arena.label, rect.left + dp(12f), rect.top + dp(22f), paint);
            paint.setTypeface(UI_REGULAR);
            paint.setTextSize(sp(9.8f));
            paint.setColor(MUTED);
            canvas.drawText(ellipsize(arena.subtitle, rect.width() - dp(24f), paint),
                    rect.left + dp(12f), rect.top + dp(41f), paint);
            y += dp(62f);
        }
    }

    private void drawInteractionCodex(Canvas canvas, RectF page) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(15f));
        paint.setColor(BRIGHT_GOLD);
        canvas.drawText("EVERY SPELL CAN MODIFY EVERY SPELL", page.left + dp(18f), page.top + dp(36f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(11f));
        paint.setColor(IVORY);
        String body = "The mid-board is a stack of interacting objects, not a bullet corridor.\n\n"
                + "AURA — a global enchantment beside your caster. Every allied cast inherits part of its profile while it stands. One per duelist; it can be beamed, hexed, consumed, or dispelled.\n\n"
                + "BIND / HEX — Bind enchants an allied object with your profile; Hex is its dark mirror, cursing the strongest hostile persistent object to sap damage, durability, and lifetime.\n\n"
                + "DISPEL — the removal instant. On contact the hostile spell body is erased outright, and hostile traps, fields, and enchantments are torn up in passing.\n\n"
                + "SIPHON — inverts friction: hostile fields and nodes are drained into the spell, and construct hits leech mana back to the caster.\n\n"
                + "CONSUME / TRIGGER / RELAY — sacrifice your own permanents for power, store whole sentences as traps, and route spells through owned nodes.\n\n"
                + "RIFT — a portal that throws hostile projectiles into another lane; allied spells pass through faster.";
        drawWrappedText(canvas, body, page.left + dp(18f), page.top + dp(64f), page.width() - dp(36f),
                sp(11f), dp(15.5f), 40, Paint.Align.LEFT);
    }

    private void drawArena(Canvas canvas) {
        RectF arena = new RectF(dp(8f), fieldTop, getWidth() - dp(8f), fieldBottom);
        paint.setStyle(Paint.Style.FILL);
        LinearGradient arenaGradient = new LinearGradient(0, arena.top, 0, arena.bottom,
                new int[]{Color.argb(125, Color.red(selectedArena.accent), Color.green(selectedArena.accent), Color.blue(selectedArena.accent)),
                        Color.argb(50, 12, 10, 22),
                        Color.argb(110, Color.red(selectedArena.shadow), Color.green(selectedArena.shadow), Color.blue(selectedArena.shadow))},
                null, Shader.TileMode.CLAMP);
        paint.setShader(arenaGradient);
        canvas.drawRoundRect(arena, dp(20f), dp(20f), paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.5f));
        paint.setColor(Color.argb(150, 218, 176, 86));
        canvas.drawRoundRect(arena, dp(20f), dp(20f), paint);

        for (int lane = 0; lane < 3; lane++) {
            float half = getWidth() * 0.122f;
            RectF laneRect = new RectF(laneX[lane] - half, fieldTop + dp(8f), laneX[lane] + half, fieldBottom - dp(8f));
            int alpha = enemyIntent != null && enemyIntent.lane == lane ? 46 : 24;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(alpha, 255, 225, 146));
            canvas.drawRoundRect(laneRect, dp(16f), dp(16f), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1f));
            paint.setColor(Color.argb(52, 255, 225, 146));
            canvas.drawRoundRect(laneRect, dp(16f), dp(16f), paint);

            for (int segment = 1; segment < 4; segment++) {
                float y = fieldTop + (fieldBottom - fieldTop) * segment / 4f;
                canvas.drawLine(laneRect.left + dp(8f), y, laneRect.right - dp(8f), y, paint);
            }

            if (lightShaftLane == lane && lightShaftTimer > 0f) {
                float glow = clamp(lightShaftTimer / 1.2f, 0f, 1f);
                paint.setStyle(Paint.Style.FILL);
                paint.setShader(new LinearGradient(laneRect.left, 0, laneRect.right, 0,
                        new int[]{Color.TRANSPARENT,
                                Color.argb(Math.round(70 * glow), 255, 243, 196),
                                Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
                canvas.drawRect(laneRect, paint);
                paint.setShader(null);
            }

            // Lane trait etched at the top of each lane.
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(7.6f));
            paint.setLetterSpacing(0.10f);
            paint.setColor(Color.argb(165, 255, 225, 146));
            canvas.drawText(laneTraits[lane].label.toUpperCase(Locale.US), laneX[lane], fieldTop + dp(21f), paint);
            paint.setLetterSpacing(0f);

            drawLeyNode(canvas, lane, nodes[lane]);
        }

        if (tideSurgeTimer > 0f) {
            float wave = clamp(tideSurgeTimer / 3.5f, 0f, 1f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2f));
            for (int band = 0; band < 3; band++) {
                float y = fieldTop + (fieldBottom - fieldTop) * ((duelClock * 0.16f + band * 0.33f) % 1f);
                paint.setColor(Color.argb(Math.round(64 * wave), 71, 194, 228));
                canvas.drawLine(arena.left + dp(14f), y, arena.right - dp(14f), y, paint);
            }
        }

        drawCaster(canvas, getWidth() * 0.5f, fieldTop + dp(8f), true);
        drawCaster(canvas, getWidth() * 0.5f, fieldBottom - dp(8f), false);

        for (Entity entity : entities) drawEntity(canvas, entity);
        if (practiceMode && duelClock < 10f) drawPracticePrompt(canvas);
        drawBanner(canvas);
    }

    private void drawLeyNode(Canvas canvas, int lane, LeyNode node) {
        float cx = laneX[lane];
        float radius = dp(22f);
        float pulse = 1f + (float) Math.sin(duelClock * 2.2f + lane) * 0.06f;
        int color = node.owner < 0 ? selectedArena.accent : node.owner == 0 ? BRIGHT_GOLD : DANGER;
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx, nodeY, radius * 1.9f,
                new int[]{Color.argb(105, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT},
                null, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, nodeY, radius * 1.9f, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);

        // Charge gauge: a bright arc sweeping with stored ley charge.
        paint.setStrokeWidth(dp(2.6f));
        paint.setColor(Color.argb(70, Color.red(color), Color.green(color), Color.blue(color)));
        RectF gauge = new RectF(cx - radius * 1.28f, nodeY - radius * 1.28f, cx + radius * 1.28f, nodeY + radius * 1.28f);
        canvas.drawArc(gauge, -90f, 360f, false, paint);
        paint.setColor(color);
        canvas.drawArc(gauge, -90f, 360f * clamp(node.charge / 100f, 0f, 1f), false, paint);

        paint.setStrokeWidth(dp(2f));
        canvas.drawCircle(cx, nodeY, radius * pulse, paint);
        canvas.save();
        canvas.rotate(duelClock * (node.owner == 1 ? -24f : 24f), cx, nodeY);
        for (int i = 0; i < 6; i++) {
            float angle = (float) (Math.PI * 2.0 * i / 6.0);
            float x1 = cx + (float) Math.cos(angle) * radius * 0.55f;
            float y1 = nodeY + (float) Math.sin(angle) * radius * 0.55f;
            float x2 = cx + (float) Math.cos(angle) * radius * 0.95f;
            float y2 = nodeY + (float) Math.sin(angle) * radius * 0.95f;
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
        canvas.restore();
        canvas.save();
        canvas.rotate(-duelClock * 15f, cx, nodeY);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(Color.argb(120, Color.red(color), Color.green(color), Color.blue(color)));
        drawArcanePolygon(canvas, cx, nodeY, radius * 0.72f, 6, paint.getColor());
        canvas.restore();
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(9.5f));
        paint.setColor(IVORY);
        canvas.drawText(Math.round(node.charge) + "%", cx, nodeY + dp(4f), paint);
    }

    private void drawCaster(Canvas canvas, float cx, float cy, boolean enemy) {
        int color = enemy ? DANGER : BRIGHT_GOLD;
        float direction = enemy ? 1f : -1f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2f));
        paint.setColor(Color.argb(145, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawArc(new RectF(cx - dp(32f), cy - dp(14f), cx + dp(32f), cy + dp(14f)),
                enemy ? 190f : 10f, 160f, false, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        Path crown = new Path();
        crown.moveTo(cx - dp(12f), cy);
        crown.lineTo(cx, cy + direction * dp(18f));
        crown.lineTo(cx + dp(12f), cy);
        crown.close();
        canvas.drawPath(crown, paint);
    }

    private void drawEntity(Canvas canvas, Entity entity) {
        if (entity.dead) return;
        int color = entity.profileColor();
        // Comet trail: tapering connected segments fading toward the tail.
        int trailSize = entity.trail.size();
        if (trailSize > 1) {
            strokePaint.setStrokeCap(Paint.Cap.ROUND);
            for (int i = 1; i < trailSize; i++) {
                PointF previous = entity.trail.get(i - 1);
                PointF point = entity.trail.get(i);
                float t = i / (float) trailSize;
                strokePaint.setStrokeWidth(entity.radius * (0.25f + 0.85f * t));
                strokePaint.setColor(Color.argb(Math.round(18f + 110f * t),
                        Color.red(color), Color.green(color), Color.blue(color)));
                canvas.drawLine(previous.x, previous.y, point.x, point.y, strokePaint);
            }
            PointF last = entity.trail.get(trailSize - 1);
            strokePaint.setStrokeWidth(entity.radius * 0.55f);
            strokePaint.setColor(Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawLine(last.x, last.y, entity.x, entity.y, strokePaint);
        }

        if (entity.kind == Kind.PROJECTILE || entity.kind == Kind.SHARD) {
            paint.setStyle(Paint.Style.FILL);
            paint.setMaskFilter(new BlurMaskFilter(dp(10f), BlurMaskFilter.Blur.NORMAL));
            paint.setColor(Color.argb(130, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawCircle(entity.x, entity.y, entity.radius * 1.65f, paint);
            paint.setMaskFilter(null);
            paint.setShader(new RadialGradient(entity.x, entity.y, Math.max(1f, entity.radius),
                    new int[]{IVORY, color, darken(color, 0.7f)},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(entity.x, entity.y, entity.radius, paint);
            paint.setShader(null);
            drawProfilePips(canvas, entity.profile, entity.x, entity.y + entity.radius + dp(8f), dp(3.2f));
        } else if (entity.kind == Kind.WARD) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(4f));
            paint.setColor(color);
            RectF arc = new RectF(entity.x - entity.radius, entity.y - entity.radius * 0.65f,
                    entity.x + entity.radius, entity.y + entity.radius * 0.65f);
            canvas.drawArc(arc, entity.owner == 0 ? 190f : 10f, 160f, false, paint);
            paint.setStrokeWidth(dp(1.5f));
            paint.setColor(BRIGHT_GOLD);
            canvas.drawCircle(entity.x, entity.y, entity.radius * 0.42f, paint);
        } else if (entity.kind == Kind.ORBIT) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2f));
            paint.setColor(color);
            canvas.drawCircle(entity.x, entity.y, entity.radius, paint);
            float angle = entity.age * 3.2f;
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(entity.x + (float) Math.cos(angle) * entity.radius,
                    entity.y + (float) Math.sin(angle) * entity.radius, dp(6f), paint);
            drawMiniGlyph(canvas, dominantElement(entity.profile), entity.x, entity.y, entity.radius * 0.55f, 1f);
        } else if (entity.kind == Kind.BURST) {
            float progress = 1f - clamp(entity.timer / 1.4f, 0f, 1f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2f));
            paint.setColor(Color.argb(190, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawCircle(entity.x, entity.y, entity.radius * (0.55f + progress * 0.45f), paint);
            canvas.save();
            canvas.rotate(entity.age * 70f, entity.x, entity.y);
            drawArcanePolygon(canvas, entity.x, entity.y, entity.radius * 0.70f, 6, color);
            canvas.restore();
        } else if (entity.kind == Kind.BEAM) {
            float targetY = entity.owner == 0 ? fieldTop : fieldBottom;
            float flicker = 0.86f + 0.24f * (float) Math.sin(entity.age * 42f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(entity.radius * 2.1f * flicker);
            paint.setMaskFilter(new BlurMaskFilter(dp(11f), BlurMaskFilter.Blur.NORMAL));
            paint.setColor(Color.argb(80, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawLine(entity.x, entity.y, entity.x, targetY, paint);
            paint.setMaskFilter(null);
            paint.setStrokeWidth(entity.radius * 0.85f * flicker);
            paint.setColor(color);
            canvas.drawLine(entity.x, entity.y, entity.x, targetY, paint);
            paint.setStrokeWidth(entity.radius * 0.30f);
            paint.setColor(IVORY);
            canvas.drawLine(entity.x, entity.y, entity.x, targetY, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(190, 255, 255, 250));
            canvas.drawCircle(entity.x, targetY, entity.radius * 0.9f * flicker, paint);
        } else if (entity.kind == Kind.SURGE) {
            paint.setStyle(Paint.Style.STROKE);
            float direction = entity.owner == 0 ? -1f : 1f;
            for (int band = 0; band < 3; band++) {
                float offset = direction * band * entity.radius * 0.30f;
                float sweepTop = entity.y - entity.radius * 0.42f + offset;
                RectF crest = new RectF(entity.x - entity.radius, sweepTop,
                        entity.x + entity.radius, sweepTop + entity.radius * 0.84f);
                paint.setStrokeWidth(dp(3.4f - band * 0.9f));
                paint.setColor(Color.argb(210 - band * 60, Color.red(color), Color.green(color), Color.blue(color)));
                canvas.drawArc(crest, direction < 0f ? 200f : 20f, 140f, false, paint);
            }
            paint.setMaskFilter(new BlurMaskFilter(dp(8f), BlurMaskFilter.Blur.NORMAL));
            paint.setStrokeWidth(dp(5f));
            paint.setColor(Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)));
            RectF glowCrest = new RectF(entity.x - entity.radius, entity.y - entity.radius * 0.42f,
                    entity.x + entity.radius, entity.y + entity.radius * 0.42f);
            canvas.drawArc(glowCrest, direction < 0f ? 200f : 20f, 140f, false, paint);
            paint.setMaskFilter(null);
        } else if (entity.kind == Kind.RIFT) {
            RectF portal = new RectF(entity.x - entity.radius, entity.y - entity.radius * 0.45f,
                    entity.x + entity.radius, entity.y + entity.radius * 0.45f);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(entity.x, entity.y, entity.radius,
                    new int[]{Color.argb(120, Color.red(color), Color.green(color), Color.blue(color)),
                            Color.argb(30, 8, 6, 16), Color.TRANSPARENT}, null, Shader.TileMode.CLAMP));
            canvas.drawOval(portal, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            for (int ring = 0; ring < 2; ring++) {
                float squeeze = 1f - ring * 0.28f;
                RectF orbit = new RectF(entity.x - entity.radius * squeeze,
                        entity.y - entity.radius * 0.45f * squeeze,
                        entity.x + entity.radius * squeeze,
                        entity.y + entity.radius * 0.45f * squeeze);
                paint.setStrokeWidth(dp(1.8f - ring * 0.5f));
                paint.setColor(Color.argb(220 - ring * 90, Color.red(color), Color.green(color), Color.blue(color)));
                canvas.save();
                canvas.rotate((ring == 0 ? 1f : -1.6f) * entity.age * 40f, entity.x, entity.y);
                canvas.drawOval(orbit, paint);
                canvas.restore();
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(BRIGHT_GOLD);
            for (int charge = 0; charge < Math.round(entity.timer); charge++) {
                canvas.drawCircle(entity.x - dp(10f) + charge * dp(10f), entity.y + entity.radius * 0.62f, dp(2.4f), paint);
            }
        } else if (entity.kind == Kind.AURA) {
            float pulse = 1f + (float) Math.sin(entity.age * 2.4f) * 0.10f;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(entity.x, entity.y, entity.radius * 1.7f,
                    new int[]{Color.argb(70, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(entity.x, entity.y, entity.radius * 1.7f, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2f));
            paint.setColor(BRIGHT_GOLD);
            canvas.drawCircle(entity.x, entity.y, entity.radius * pulse, paint);
            canvas.save();
            canvas.rotate(entity.age * 30f, entity.x, entity.y);
            drawArcanePolygon(canvas, entity.x, entity.y, entity.radius * 0.78f, 6, color);
            canvas.restore();
            canvas.save();
            canvas.rotate(-entity.age * 18f, entity.x, entity.y);
            drawArcanePolygon(canvas, entity.x, entity.y, entity.radius * 0.52f, 3, BRIGHT_GOLD);
            canvas.restore();
            drawMiniGlyph(canvas, dominantElement(entity.profile), entity.x, entity.y, entity.radius * 0.30f, 1f);
        } else if (entity.kind == Kind.GLYPH || entity.kind == Kind.FIELD
                || entity.kind == Kind.TRAP || entity.kind == Kind.ENCHANTMENT) {
            float pulse = 1f + (float) Math.sin(entity.age * 3f) * 0.08f;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new RadialGradient(entity.x, entity.y, entity.radius,
                    new int[]{Color.argb(entity.kind == Kind.FIELD ? 66 : 92,
                            Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            canvas.drawCircle(entity.x, entity.y, entity.radius * pulse, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(entity.kind == Kind.TRAP ? 1.5f : 2.2f));
            paint.setColor(color);
            canvas.save();
            canvas.rotate(entity.age * (entity.owner == 0 ? 24f : -24f), entity.x, entity.y);
            drawArcanePolygon(canvas, entity.x, entity.y, entity.radius * 0.72f, entity.kind == Kind.TRAP ? 3 : 7, color);
            canvas.restore();
            if (entity.kind == Kind.FIELD) {
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(UI_MEDIUM);
                paint.setTextSize(sp(8.5f));
                paint.setColor(Color.argb(190, 245, 242, 231));
                canvas.drawText(entity.label == null ? entity.profile.dominantReaction().toUpperCase(Locale.US)
                        : entity.label.toUpperCase(Locale.US), entity.x, entity.y + dp(4f), paint);
            }
        }

        if (entity.bound) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.3f));
            paint.setColor(BRIGHT_GOLD);
            canvas.drawCircle(entity.x, entity.y, entity.radius * 1.35f, paint);
        }
        if (entity.hexed) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.6f));
            paint.setColor(Element.VOID.color);
            canvas.save();
            canvas.rotate(entity.age * -46f, entity.x, entity.y);
            drawArcanePolygon(canvas, entity.x, entity.y, entity.radius * 1.30f, 5, Element.VOID.color);
            canvas.restore();
        }
        if (entity.empowered) {
            float sparkAngle = entity.age * 5.2f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(BRIGHT_GOLD);
            for (int i = 0; i < 2; i++) {
                float a = sparkAngle + i * (float) Math.PI;
                canvas.drawCircle(entity.x + (float) Math.cos(a) * entity.radius * 1.5f,
                        entity.y + (float) Math.sin(a) * entity.radius * 1.5f, dp(2.2f), paint);
            }
        }
    }

    private void drawHud(Canvas canvas) {
        drawCombatantHud(canvas, true);
        drawCombatantHud(canvas, false);
    }

    private void drawCombatantHud(Canvas canvas, boolean enemy) {
        float top = enemy ? dp(10f) : composerTop - dp(54f);
        float left = dp(14f);
        float right = getWidth() - dp(14f);
        RectF panel = new RectF(left, top, right, top + dp(44f));
        drawMetalPanel(canvas, panel, false, enemy ? DANGER : GOLD);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(10.5f));
        paint.setColor(enemy ? DANGER : BRIGHT_GOLD);
        canvas.drawText(enemy ? "RIVAL CIRCLE" : "YOUR CIRCLE", left + dp(9f), top + dp(15f), paint);
        float health = enemy ? enemyHealth : playerHealth;
        float mana = enemy ? enemyMana : playerMana;
        float resonance = enemy ? enemyResonance : playerResonance;
        drawBar(canvas, left + dp(9f), top + dp(21f), right - dp(134f), top + dp(28f), health / 100f, DANGER);
        drawBar(canvas, left + dp(9f), top + dp(32f), right - dp(134f), top + dp(39f), mana / 100f, INK_BLUE);
        drawBar(canvas, right - dp(125f), top + dp(21f), right - dp(9f), top + dp(28f), resonance / 100f, BRIGHT_GOLD);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(9.5f));
        paint.setColor(IVORY);
        canvas.drawText(Math.round(health) + " HP  " + Math.round(mana) + " M", right - dp(9f), top + dp(40f), paint);
        if (resonance >= 100f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.4f));
            float glow = 0.5f + 0.5f * (float) Math.sin(duelClock * 6f);
            paint.setColor(Color.argb(Math.round(120 + 120 * glow), 255, 225, 146));
            canvas.drawRoundRect(new RectF(right - dp(127f), top + dp(19f), right - dp(7f), top + dp(30f)), dp(4f), dp(4f), paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (!enemy) {
            // Ink pips: filled diamonds for whole ink, hollow for the fraction ceiling.
            float pipX = right - dp(12f);
            for (int pip = 0; pip < 6; pip++) {
                float fill = clamp(playerInk - pip, 0f, 1f);
                if (fill <= 0.02f && pip > 0 && playerInk < pip) break;
                Path diamond = new Path();
                float px = pipX - pip * dp(13f);
                float py = top + dp(11f);
                float r = dp(4f);
                diamond.moveTo(px, py - r);
                diamond.lineTo(px + r, py);
                diamond.lineTo(px, py + r);
                diamond.lineTo(px - r, py);
                diamond.close();
                paint.setStyle(fill >= 0.98f ? Paint.Style.FILL : Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1f));
                paint.setColor(fill >= 0.98f ? BRIGHT_GOLD : Color.argb(140, 218, 176, 86));
                canvas.drawPath(diamond, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawEnemyIntent(Canvas canvas) {
        if (enemyIntent == null || screen != Screen.DUEL) return;
        RectF rect = new RectF(dp(20f), dp(59f), getWidth() - dp(20f), fieldTop - dp(5f));
        drawMetalPanel(canvas, rect, true, DANGER);
        float progress = enemyIntent.progress();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(10f));
        paint.setColor(DANGER);
        canvas.drawText("RIVAL WRITING · " + laneName(enemyIntent.lane).toUpperCase(Locale.US),
                rect.left + dp(9f), rect.top + dp(14f), paint);
        float x = rect.left + dp(104f);
        int revealed = Math.min(enemyIntent.program.elements.size(), (int) Math.floor(progress * (enemyIntent.program.elements.size() + 2.5f)));
        for (int i = 0; i < enemyIntent.program.elements.size(); i++) {
            float p = i < revealed ? 1f : clamp(progress * (enemyIntent.program.elements.size() + 2f) - i, 0f, 1f);
            drawMiniGlyph(canvas, enemyIntent.program.elements.get(i), x + i * dp(24f), rect.centerY() + dp(2f), dp(10f), p);
        }
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(9.5f));
        paint.setColor(progress > 0.68f ? IVORY : MUTED);
        String syntax = progress > 0.58f ? enemyIntent.program.form.label.toUpperCase(Locale.US) : "FORM HIDDEN";
        if (progress > 0.75f && !enemyIntent.program.clauses.isEmpty()) syntax += " · " + clauseList(enemyIntent.program.clauses);
        canvas.drawText(ellipsize(syntax, rect.width() * 0.35f, paint), rect.right - dp(9f), rect.top + dp(16f), paint);
        drawBar(canvas, rect.left + dp(9f), rect.bottom - dp(7f), rect.right - dp(9f), rect.bottom - dp(3f), progress, DANGER);
    }

    private void drawPracticePrompt(Canvas canvas) {
        RectF rect = new RectF(dp(24f), nodeY - dp(44f), getWidth() - dp(24f), nodeY + dp(44f));
        drawMetalPanel(canvas, rect, true, BRIGHT_GOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(13f));
        paint.setColor(IVORY);
        canvas.drawText("COMPOSE WITHOUT PRESSURE", rect.centerX(), rect.top + dp(26f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(11f));
        paint.setColor(MUTED);
        canvas.drawText("Draw sigils, choose a form, attach clauses, select a lane, then Cast.",
                rect.centerX(), rect.top + dp(48f), paint);
        paint.setColor(BRIGHT_GOLD);
        canvas.drawText("The rival begins in " + Math.max(0, Math.round(10f - duelClock)) + "s", rect.centerX(), rect.top + dp(68f), paint);
    }

    private void drawComposer(Canvas canvas) {
        drawMetalPanel(canvas, composerPanel, true, selectedArena.accent);
        drawStepRail(canvas);
        drawExecutableRibbon(canvas);

        if (composerStep < 4 && !composer.elements.isEmpty()) drawSentencePreview(canvas);

        if (composerStep == 0) drawSigilStep(canvas);
        else if (composerStep == 1) {
            drawSectionLabel(canvas, "CHOOSE A FORM RUNE", drawingPad.left, drawingPad.top - dp(4f));
            for (int i = 0; i < formRects.length; i++) {
                drawFormCard(canvas, formRects[i], Form.values()[i], composer.form == Form.values()[i], true);
            }
            drawSmallGoldButton(canvas, backStepButton, "BACK", false);
            drawGoldButton(canvas, continueButton, "3 · CLAUSES", "shape behavior", true);
        } else if (composerStep == 2) {
            drawSectionLabel(canvas, "ATTACH UP TO THREE CLAUSES", drawingPad.left, drawingPad.top - dp(4f));
            for (int i = 0; i < clauseRects.length; i++) {
                Clause clause = Clause.values()[i];
                drawClauseCard(canvas, clauseRects[i], clause, composer.clauses.indexOf(clause), true);
            }
            drawSmallGoldButton(canvas, backStepButton, "BACK", false);
            drawGoldButton(canvas, continueButton, "4 · LANE", "commit position", true);
        } else if (composerStep == 3) {
            drawLaneStep(canvas);
            drawSmallGoldButton(canvas, backStepButton, "BACK", false);
        } else {
            drawCastStep(canvas);
        }
        drawBanner(canvas);
    }

    /** Live sentence readout: the spell as written so far, right-aligned over the stage area. */
    private void drawSentencePreview(Canvas canvas) {
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(8.6f));
        paint.setColor(BRIGHT_GOLD);
        String sentence = composer.shortLabel()
                + (composer.clauses.isEmpty() ? "" : " · " + clauseList(composer.clauses));
        canvas.drawText(ellipsize(sentence, drawingPad.width() * 0.52f, paint),
                drawingPad.right, drawingPad.top - dp(4f), paint);
    }

    private void drawStepRail(Canvas canvas) {
        String[] labels = {"SIGILS", "FORM", "CLAUSES", "LANE", "CAST"};
        // Golden rail line behind the diamond stations.
        float railY = stepRects[0].centerY();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        paint.setColor(Color.argb(110, 218, 176, 86));
        canvas.drawLine(stepRects[0].centerX(), railY, stepRects[4].centerX(), railY, paint);

        for (int i = 0; i < stepRects.length; i++) {
            boolean active = composerStep == i;
            boolean complete = i < composerStep || (i == 0 && !composer.elements.isEmpty())
                    || (i == 3 && selectedLane >= 0);
            float cx = stepRects[i].centerX();
            float r = active ? dp(9f) : dp(7f);
            Path diamond = new Path();
            diamond.moveTo(cx, railY - r);
            diamond.lineTo(cx + r, railY);
            diamond.lineTo(cx, railY + r);
            diamond.lineTo(cx - r, railY);
            diamond.close();
            if (active) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2f));
                paint.setMaskFilter(new BlurMaskFilter(dp(4f), BlurMaskFilter.Blur.NORMAL));
                paint.setColor(Color.argb(150, 255, 225, 146));
                canvas.drawPath(diamond, paint);
                paint.setMaskFilter(null);
            }
            paint.setStyle(complete || active ? Paint.Style.FILL : Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.4f));
            paint.setColor(active ? BRIGHT_GOLD : (complete ? GOLD : Color.argb(150, 116, 82, 35)));
            canvas.drawPath(diamond, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(active ? UI_MEDIUM : UI_REGULAR);
            paint.setTextSize(sp(7.8f));
            paint.setLetterSpacing(0.08f);
            paint.setColor(active ? IVORY : (complete ? BRIGHT_GOLD : MUTED));
            canvas.drawText(labels[i], cx, stepRects[i].bottom + dp(1f), paint);
            paint.setLetterSpacing(0f);
        }
    }

    private void drawExecutableRibbon(Canvas canvas) {
        drawMetalPanel(canvas, executableRibbon, false, GOLD);
        drawSmallGoldButton(canvas, executablePrev, "‹", false);
        drawSmallGoldButton(canvas, executableNext, "›", false);
        for (int i = 0; i < executableCardRects.length; i++) {
            int index = executablePage + i;
            RectF rect = executableCardRects[i];
            if (index >= library.size()) {
                drawMetalPanel(canvas, rect, false, OLD_GOLD);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(UI_REGULAR);
                paint.setTextSize(sp(8.5f));
                paint.setColor(MUTED);
                canvas.drawText("+", rect.centerX(), rect.centerY() + dp(3f), paint);
                continue;
            }
            Program p = library.get(index);
            boolean selected = loadedExecutable == index;
            int accent = p.elements.isEmpty() ? GOLD : p.elements.get(0).color;
            drawMetalPanel(canvas, rect, selected, accent);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(8.2f));
            paint.setColor(selected ? BRIGHT_GOLD : IVORY);
            canvas.drawText(String.format(Locale.US, "%02d", index + 1), rect.left + dp(5f), rect.top + dp(11f), paint);
            float x = rect.left + dp(10f);
            for (int g = 0; g < Math.min(4, p.elements.size()); g++) {
                drawMiniGlyph(canvas, p.elements.get(g), x + g * dp(12f), rect.centerY() + dp(3f), dp(5f), 1f);
            }
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTypeface(UI_REGULAR);
            paint.setTextSize(sp(7.7f));
            paint.setColor(MUTED);
            canvas.drawText(ellipsize(p.form.label, rect.width() * 0.45f, paint), rect.right - dp(5f), rect.bottom - dp(7f), paint);
            if (index < executableCooldowns.size() && executableCooldowns.get(index) > 0f) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(Color.argb(170, 8, 8, 14));
                canvas.drawRoundRect(rect, dp(5f), dp(5f), paint);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTypeface(UI_MEDIUM);
                paint.setTextSize(sp(9f));
                paint.setColor(DANGER);
                canvas.drawText(Math.round(executableCooldowns.get(index)) + "s", rect.centerX(), rect.centerY() + dp(3f), paint);
            }
        }
    }

    private void drawSigilStep(Canvas canvas) {
        drawSectionLabel(canvas, "DRAW SIGILS — ORDER MATTERS", drawingPad.left, drawingPad.top - dp(4f));
        drawSmallGoldButton(canvas, undoButton, "UNDO", false);
        drawSmallGoldButton(canvas, clearButton, "CLEAR", false);
        drawMetalPanel(canvas, drawingPad, true, composer.elements.isEmpty() ? GOLD : composer.elements.get(composer.elements.size() - 1).color);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(Color.argb(36, 255, 225, 146));
        float ring = Math.min(drawingPad.width(), drawingPad.height()) * 0.23f;
        canvas.drawCircle(drawingPad.centerX(), drawingPad.centerY(), ring, paint);
        canvas.drawCircle(drawingPad.centerX(), drawingPad.centerY(), ring * 0.64f, paint);
        for (int i = 0; i < 8; i++) {
            float a = (float) (Math.PI * 2.0 * i / 8.0);
            canvas.drawLine(drawingPad.centerX() + (float) Math.cos(a) * ring * 0.72f,
                    drawingPad.centerY() + (float) Math.sin(a) * ring * 0.72f,
                    drawingPad.centerX() + (float) Math.cos(a) * ring,
                    drawingPad.centerY() + (float) Math.sin(a) * ring, paint);
        }
        if (currentStroke.size() > 1) {
            Path path = new Path();
            path.moveTo(currentStroke.get(0).x, currentStroke.get(0).y);
            for (int i = 1; i < currentStroke.size(); i++) path.lineTo(currentStroke.get(i).x, currentStroke.get(i).y);
            strokePaint.setStrokeWidth(dp(5.5f));
            strokePaint.setMaskFilter(new BlurMaskFilter(dp(7f), BlurMaskFilter.Blur.NORMAL));
            int glow = composer.elements.isEmpty() ? BRIGHT_GOLD : composer.elements.get(composer.elements.size() - 1).color;
            strokePaint.setColor(Color.argb(125, Color.red(glow), Color.green(glow), Color.blue(glow)));
            canvas.drawPath(path, strokePaint);
            strokePaint.setMaskFilter(null);
            strokePaint.setStrokeWidth(dp(2.4f));
            strokePaint.setColor(IVORY);
            canvas.drawPath(path, strokePaint);
        } else if (composer.elements.isEmpty()) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(13f));
            paint.setColor(Color.rgb(205, 195, 211));
            canvas.drawText("TRACE A SIGIL", drawingPad.centerX(), drawingPad.centerY() - dp(4f), paint);
            paint.setTypeface(UI_REGULAR);
            paint.setTextSize(sp(10f));
            paint.setColor(MUTED);
            canvas.drawText("triangle · wave · spiral · square · diamond · bolt · circle · cross",
                    drawingPad.centerX(), drawingPad.centerY() + dp(17f), paint);
        }

        float glyphX = drawingPad.left + dp(22f);
        float glyphY = drawingPad.top + dp(26f);
        for (int i = 0; i < composer.elements.size(); i++) {
            drawMiniGlyph(canvas, composer.elements.get(i), glyphX + i * dp(39f), glyphY, dp(14f), 1f);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(8f));
            paint.setColor(BRIGHT_GOLD);
            canvas.drawText(String.valueOf(i + 1), glyphX + i * dp(39f), glyphY + dp(23f), paint);
        }
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(9f));
        paint.setColor(MUTED);
        canvas.drawText(composer.elements.size() + "/4", drawingPad.right - dp(8f), drawingPad.bottom - dp(8f), paint);
        drawSmallGoldButton(canvas, backStepButton, "RESET", false);
        drawGoldButton(canvas, continueButton, "2 · FORM", composer.elements.isEmpty() ? "draw first" : autoName(composer), !composer.elements.isEmpty());
    }

    private void drawFormCard(Canvas canvas, RectF rect, Form form, boolean selected, boolean detailed) {
        drawMetalPanel(canvas, rect, selected, selected ? BRIGHT_GOLD : GOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(detailed ? 12f : 10f));
        paint.setColor(selected ? IVORY : Color.rgb(224, 219, 228));
        canvas.drawText(form.label.toUpperCase(Locale.US), rect.centerX(), rect.top + rect.height() * 0.36f, paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(detailed ? 9.5f : 8.5f));
        paint.setColor(selected ? BRIGHT_GOLD : MUTED);
        drawWrappedText(canvas, form.subtitle, rect.centerX(), rect.top + rect.height() * 0.58f,
                rect.width() - dp(10f), sp(detailed ? 9.5f : 8.5f), dp(12f), 2);
        paint.setTextSize(sp(8.5f));
        paint.setColor(INK_BLUE);
        canvas.drawText(Math.round(form.baseCost) + " base", rect.centerX(), rect.bottom - dp(8f), paint);
    }

    private void drawClauseCard(Canvas canvas, RectF rect, Clause clause, int order, boolean detailed) {
        boolean selected = order >= 0;
        drawMetalPanel(canvas, rect, selected, selected ? BRIGHT_GOLD : OLD_GOLD);
        if (selected) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(BRIGHT_GOLD);
            canvas.drawCircle(rect.left + dp(13f), rect.top + dp(13f), dp(9f), paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(8.5f));
            paint.setColor(DEEP_INK);
            canvas.drawText(String.valueOf(order + 1), rect.left + dp(13f), rect.top + dp(16f), paint);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(detailed ? 11f : 9.5f));
        paint.setColor(selected ? IVORY : Color.rgb(214, 208, 218));
        canvas.drawText(clause.label.toUpperCase(Locale.US), rect.centerX(), rect.top + rect.height() * 0.42f, paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(detailed ? 9f : 8f));
        paint.setColor(selected ? BRIGHT_GOLD : MUTED);
        canvas.drawText(ellipsize(clause.subtitle, rect.width() - dp(10f), paint), rect.centerX(), rect.bottom - dp(12f), paint);
    }

    private void drawLaneStep(Canvas canvas) {
        drawSectionLabel(canvas, "COMMIT TO A LANE", drawingPad.left, drawingPad.top - dp(4f));
        for (int i = 0; i < laneRects.length; i++) {
            boolean selected = selectedLane == i;
            int accent = nodes[i].owner == 0 ? SUCCESS : nodes[i].owner == 1 ? DANGER : selectedArena.accent;
            drawMetalPanel(canvas, laneRects[i], selected, accent);
            drawArcaneCrest(canvas, laneRects[i].centerX(), laneRects[i].top + dp(32f), dp(18f), i * 31f);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(UI_MEDIUM);
            paint.setTextSize(sp(12f));
            paint.setColor(selected ? IVORY : Color.rgb(220, 214, 225));
            canvas.drawText(laneName(i).toUpperCase(Locale.US), laneRects[i].centerX(), laneRects[i].bottom - dp(35f), paint);
            paint.setTypeface(UI_REGULAR);
            paint.setTextSize(sp(9f));
            paint.setColor(nodes[i].owner < 0 ? MUTED : (nodes[i].owner == 0 ? SUCCESS : DANGER));
            String status = nodes[i].owner < 0 ? "neutral node" : (nodes[i].owner == 0 ? "your ley " : "rival ley ") + Math.round(nodes[i].charge);
            canvas.drawText(status, laneRects[i].centerX(), laneRects[i].bottom - dp(21f), paint);
            paint.setTextSize(sp(8.2f));
            paint.setColor(BRIGHT_GOLD);
            canvas.drawText(ellipsize(laneTraits[i].label + " · " + laneTraits[i].subtitle,
                    laneRects[i].width() - dp(10f), paint), laneRects[i].centerX(), laneRects[i].bottom - dp(9f), paint);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(10f));
        paint.setColor(MUTED);
        canvas.drawText("Position matters: persistent fields, traps, nodes, and forks remain lane-local.",
                drawingPad.centerX(), laneRects[0].bottom + dp(28f), paint);
    }

    private void drawCastStep(Canvas canvas) {
        drawSectionLabel(canvas, "REVIEW THE COMPILED SENTENCE", drawingPad.left, drawingPad.top - dp(4f));
        RectF review = new RectF(drawingPad.left, drawingPad.top + dp(4f), drawingPad.right, drawingPad.bottom - dp(6f));
        drawMetalPanel(canvas, review, true, composer.elements.isEmpty() ? GOLD : composer.elements.get(0).color);
        drawProgramSummary(canvas, composer, review, selectedLane);
        float cost = currentManaCost();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(10.5f));
        paint.setColor(INK_BLUE);
        canvas.drawText(Math.round(cost) + " MANA" + (loadedExecutable >= 0 ? " + 1 INK" : ""), review.left + dp(12f), review.bottom - dp(14f), paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(playerResonance >= 100f ? BRIGHT_GOLD : MUTED);
        canvas.drawText(playerResonance >= 100f ? "ASCENDANT READY" : Math.round(playerResonance) + "% RESONANCE",
                review.right - dp(12f), review.bottom - dp(14f), paint);
        drawSmallGoldButton(canvas, backStepButton, "BACK", false);
        drawGoldButton(canvas, castButton, "CAST", laneName(selectedLane) + " · " + autoName(composer), true);
        clearButton.set(composerPanel.right - dp(78f), composerPanel.bottom - dp(105f), composerPanel.right - dp(10f), composerPanel.bottom - dp(63f));
        drawSmallGoldButton(canvas, clearButton, "CLEAR", false);
    }

    private void drawProgramString(Canvas canvas, Program program, float left, float baseline, float maxWidth) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(12f));
        paint.setColor(IVORY);
        canvas.drawText(ellipsize(autoName(program), maxWidth, paint), left, baseline, paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(9.5f));
        paint.setColor(MUTED);
        String syntax = program.shortLabel() + (program.clauses.isEmpty() ? "" : " · " + clauseList(program.clauses));
        canvas.drawText(ellipsize(syntax, maxWidth, paint), left, baseline + dp(17f), paint);
    }

    private void drawProgramSummary(Canvas canvas, Program program, RectF rect, int lane) {
        float glyphY = rect.top + dp(42f);
        float startX = rect.centerX() - (program.elements.size() - 1) * dp(22f);
        for (int i = 0; i < program.elements.size(); i++) {
            drawMiniGlyph(canvas, program.elements.get(i), startX + i * dp(44f), glyphY, dp(18f), 1f);
        }
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(16f));
        paint.setColor(IVORY);
        canvas.drawText(autoName(program).toUpperCase(Locale.US), rect.centerX(), rect.top + dp(84f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(11f));
        paint.setColor(BRIGHT_GOLD);
        canvas.drawText(program.form.label.toUpperCase(Locale.US) + " · " + (lane < 0 ? "NO LANE" : laneName(lane).toUpperCase(Locale.US)),
                rect.centerX(), rect.top + dp(106f), paint);
        paint.setColor(MUTED);
        canvas.drawText(program.clauses.isEmpty() ? "No behavioral clauses" : clauseList(program.clauses),
                rect.centerX(), rect.top + dp(126f), paint);
        Profile p = compile(program.elements, selectedArtifact, selectedArena);
        drawProfilePips(canvas, p, rect.centerX(), rect.top + dp(151f), dp(5f));
        paint.setTextSize(sp(9.5f));
        paint.setColor(profileColor(p));
        canvas.drawText(p.dominantReaction().toUpperCase(Locale.US) + " PROFILE", rect.centerX(), rect.top + dp(174f), paint);
        paint.setColor(MUTED);
        drawWrappedText(canvas, "Fields and nodes can add, damp, bind, consume, or redirect these channels after deployment.",
                rect.centerX(), rect.top + dp(194f), rect.width() - dp(28f), sp(9.5f), dp(13f), 2);
    }

    private void drawMiniGlyph(Canvas canvas, Element element, float cx, float cy, float radius, float progress) {
        progress = clamp(progress, 0f, 1f);
        Path path = glyphPath(element, cx, cy, radius);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(Math.max(dp(1.2f), radius * 0.16f));
        paint.setMaskFilter(new BlurMaskFilter(Math.max(dp(2f), radius * 0.25f), BlurMaskFilter.Blur.NORMAL));
        paint.setColor(Color.argb(Math.round(100 * progress), Color.red(element.color), Color.green(element.color), Color.blue(element.color)));
        canvas.drawPath(path, paint);
        paint.setMaskFilter(null);
        paint.setStrokeWidth(Math.max(dp(1f), radius * 0.08f));
        paint.setColor(Color.argb(Math.round(255 * progress), Color.red(element.color), Color.green(element.color), Color.blue(element.color)));
        canvas.drawPath(path, paint);
    }

    private Path glyphPath(Element element, float cx, float cy, float r) {
        Path p = new Path();
        switch (element) {
            case FIRE:
                p.moveTo(cx, cy - r); p.lineTo(cx + r * 0.86f, cy + r); p.lineTo(cx - r * 0.86f, cy + r); p.close();
                break;
            case WATER:
                p.moveTo(cx - r, cy); p.cubicTo(cx - r * 0.7f, cy - r, cx - r * 0.35f, cy + r, cx, cy);
                p.cubicTo(cx + r * 0.35f, cy - r, cx + r * 0.7f, cy + r, cx + r, cy);
                break;
            case WIND:
                p.moveTo(cx - r, cy + r * 0.5f); p.cubicTo(cx - r, cy - r, cx + r * 0.8f, cy - r, cx + r * 0.65f, cy + r * 0.1f);
                p.cubicTo(cx + r * 0.5f, cy + r, cx - r * 0.55f, cy + r * 0.65f, cx - r * 0.25f, cy - r * 0.1f);
                p.cubicTo(cx, cy - r * 0.6f, cx + r * 0.45f, cy - r * 0.25f, cx + r * 0.18f, cy + r * 0.2f);
                break;
            case STONE:
                p.addRect(cx - r * 0.82f, cy - r * 0.82f, cx + r * 0.82f, cy + r * 0.82f, Path.Direction.CW);
                break;
            case FROST:
                p.moveTo(cx, cy - r); p.lineTo(cx + r, cy); p.lineTo(cx, cy + r); p.lineTo(cx - r, cy); p.close();
                p.moveTo(cx, cy - r); p.lineTo(cx, cy + r);
                break;
            case LIGHTNING:
                p.moveTo(cx + r * 0.45f, cy - r); p.lineTo(cx - r * 0.25f, cy - r * 0.05f);
                p.lineTo(cx + r * 0.25f, cy - r * 0.05f); p.lineTo(cx - r * 0.45f, cy + r);
                break;
            case AETHER:
                p.addCircle(cx, cy, r * 0.82f, Path.Direction.CW);
                break;
            case VOID:
                p.moveTo(cx - r * 0.75f, cy - r * 0.75f); p.lineTo(cx + r * 0.75f, cy + r * 0.75f);
                p.moveTo(cx + r * 0.75f, cy - r * 0.75f); p.lineTo(cx - r * 0.75f, cy + r * 0.75f);
                break;
            case RADIANCE:
                p.moveTo(cx - r, cy + r * 0.85f);
                p.cubicTo(cx - r, cy - r * 0.75f, cx + r, cy - r * 0.75f, cx + r, cy + r * 0.85f);
                break;
            case VERDANCE:
                p.moveTo(cx - r * 0.85f, cy - r * 0.05f);
                p.lineTo(cx - r * 0.28f, cy + r * 0.85f);
                p.lineTo(cx + r * 0.85f, cy - r * 0.85f);
                break;
        }
        return p;
    }

    private void drawArcaneCrest(Canvas canvas, float cx, float cy, float radius, float rotation) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        paint.setColor(Color.argb(115, 255, 225, 146));
        canvas.save();
        canvas.rotate(rotation, cx, cy);
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.drawCircle(cx, cy, radius * 0.68f, paint);
        drawArcanePolygon(canvas, cx, cy, radius * 0.88f, 8, BRIGHT_GOLD);
        drawArcanePolygon(canvas, cx, cy, radius * 0.52f, 4, selectedArena.accent);
        canvas.restore();
    }

    private void drawArcanePolygon(Canvas canvas, float cx, float cy, float radius, int sides, int color) {
        Path path = new Path();
        for (int i = 0; i <= sides; i++) {
            float angle = (float) (-Math.PI / 2.0 + Math.PI * 2.0 * i / sides);
            float x = cx + (float) Math.cos(angle) * radius;
            float y = cy + (float) Math.sin(angle) * radius;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.2f));
        paint.setColor(color);
        canvas.drawPath(path, paint);
    }

    private void drawMetalPanel(Canvas canvas, RectF rect, boolean selected, int accent) {
        float radius = dp(7f);
        // Brushed-metal body: three-stop vertical gradient with a top sheen.
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                selected ? new int[]{Color.rgb(62, 54, 76), Color.rgb(34, 30, 48), Color.rgb(23, 21, 34)}
                        : new int[]{PANEL_HIGH, PANEL, Color.rgb(19, 18, 30)},
                new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);
        if (selected) {
            paint.setShader(new RadialGradient(rect.centerX(), rect.top, Math.max(dp(8f), rect.width() * 0.7f),
                    new int[]{Color.argb(46, Color.red(accent), Color.green(accent), Color.blue(accent)), Color.TRANSPARENT},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRoundRect(rect, radius, radius, paint);
            paint.setShader(null);
        }

        // Engraved border: gradient gold outer line plus a dark inner seam.
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(selected ? dp(1.8f) : dp(1f));
        paint.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                selected ? new int[]{lighten(accent, 1.25f), accent, darken(accent, 0.6f)}
                        : new int[]{Color.argb(160, Color.red(accent), Color.green(accent), Color.blue(accent)),
                                Color.argb(95, Color.red(accent), Color.green(accent), Color.blue(accent)),
                                Color.argb(140, Color.red(accent), Color.green(accent), Color.blue(accent))},
                null, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(rect, radius, radius, paint);
        paint.setShader(null);
        paint.setStrokeWidth(dp(0.7f));
        paint.setColor(Color.argb(46, 255, 255, 255));
        RectF inset = new RectF(rect.left + dp(3f), rect.top + dp(3f), rect.right - dp(3f), rect.bottom - dp(3f));
        canvas.drawRoundRect(inset, radius * 0.7f, radius * 0.7f, paint);

        // Filigree corner ticks on larger panels.
        if (rect.width() > dp(64f) && rect.height() > dp(36f)) {
            float tick = Math.min(dp(9f), rect.height() * 0.22f);
            paint.setStrokeWidth(dp(1.1f));
            paint.setColor(selected ? BRIGHT_GOLD
                    : Color.argb(150, Color.red(GOLD), Color.green(GOLD), Color.blue(GOLD)));
            float pad = dp(5f);
            canvas.drawLine(rect.left + pad, rect.top + pad + tick, rect.left + pad, rect.top + pad, paint);
            canvas.drawLine(rect.left + pad, rect.top + pad, rect.left + pad + tick, rect.top + pad, paint);
            canvas.drawLine(rect.right - pad - tick, rect.top + pad, rect.right - pad, rect.top + pad, paint);
            canvas.drawLine(rect.right - pad, rect.top + pad, rect.right - pad, rect.top + pad + tick, paint);
            canvas.drawLine(rect.left + pad, rect.bottom - pad - tick, rect.left + pad, rect.bottom - pad, paint);
            canvas.drawLine(rect.left + pad, rect.bottom - pad, rect.left + pad + tick, rect.bottom - pad, paint);
            canvas.drawLine(rect.right - pad - tick, rect.bottom - pad, rect.right - pad, rect.bottom - pad, paint);
            canvas.drawLine(rect.right - pad, rect.bottom - pad, rect.right - pad, rect.bottom - pad - tick, paint);
        }

        if (selected) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(accent);
            canvas.drawRect(rect.left, rect.top + dp(7f), rect.left + dp(2.5f), rect.bottom - dp(7f), paint);
        }
    }

    private static int lighten(int color, float factor) {
        return Color.rgb(Math.min(255, Math.round(Color.red(color) * factor)),
                Math.min(255, Math.round(Color.green(color) * factor)),
                Math.min(255, Math.round(Color.blue(color) * factor)));
    }

    private static int darken(int color, float factor) {
        return Color.rgb(Math.round(Color.red(color) * factor),
                Math.round(Color.green(color) * factor),
                Math.round(Color.blue(color) * factor));
    }

    private void drawGoldButton(Canvas canvas, RectF rect, String title, String subtitle, boolean primary) {
        if (primary) {
            // Breathing glow so the primary action always reads as "press me".
            float glow = 0.5f + 0.5f * (float) Math.sin(uiClock * 3.4f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2.6f));
            paint.setMaskFilter(new BlurMaskFilter(dp(6f), BlurMaskFilter.Blur.NORMAL));
            paint.setColor(Color.argb(Math.round(60 + 90 * glow), 255, 225, 146));
            canvas.drawRoundRect(rect, dp(8f), dp(8f), paint);
            paint.setMaskFilter(null);
        }
        drawMetalPanel(canvas, rect, primary, primary ? BRIGHT_GOLD : GOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(primary ? 12.5f : 11f));
        paint.setColor(primary ? IVORY : Color.rgb(226, 220, 228));
        float y = subtitle == null || subtitle.length() == 0 ? rect.centerY() + dp(4f) : rect.top + rect.height() * 0.43f;
        canvas.drawText(title, rect.centerX(), y, paint);
        if (subtitle != null && subtitle.length() > 0) {
            paint.setTypeface(UI_REGULAR);
            paint.setTextSize(sp(9f));
            paint.setColor(primary ? BRIGHT_GOLD : MUTED);
            canvas.drawText(ellipsize(subtitle, rect.width() - dp(12f), paint), rect.centerX(), rect.bottom - dp(9f), paint);
        }
    }

    private void drawSmallGoldButton(Canvas canvas, RectF rect, String label, boolean active) {
        drawMetalPanel(canvas, rect, active, active ? BRIGHT_GOLD : GOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(9.5f));
        paint.setColor(active ? IVORY : Color.rgb(220, 214, 224));
        canvas.drawText(label, rect.centerX(), rect.centerY() + dp(3.5f), paint);
    }

    private void drawSectionLabel(Canvas canvas, String label, float x, float y) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(9f));
        paint.setLetterSpacing(0.08f);
        paint.setColor(BRIGHT_GOLD);
        canvas.drawText(label, x, y, paint);
        paint.setLetterSpacing(0f);
    }

    private void drawBar(Canvas canvas, float left, float top, float right, float bottom, float value, int color) {
        value = clamp(value, 0f, 1f);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(105, 3, 3, 8));
        canvas.drawRoundRect(new RectF(left, top, right, bottom), dp(3f), dp(3f), paint);
        if (value > 0f) {
            paint.setColor(color);
            canvas.drawRoundRect(new RectF(left, top, left + (right - left) * value, bottom), dp(3f), dp(3f), paint);
        }
    }

    private void drawBanner(Canvas canvas) {
        if (bannerTimer <= 0f || banner == null || banner.length() == 0) return;
        float alpha = clamp(bannerTimer * 2f, 0f, 1f);
        RectF rect = new RectF(dp(24f), getHeight() * 0.46f - dp(24f), getWidth() - dp(24f), getHeight() * 0.46f + dp(24f));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(Math.round(210 * alpha), 11, 10, 18));
        canvas.drawRoundRect(rect, dp(8f), dp(8f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1f));
        paint.setColor(Color.argb(Math.round(210 * alpha), 255, 225, 146));
        canvas.drawRoundRect(rect, dp(8f), dp(8f), paint);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(11f));
        paint.setColor(Color.argb(Math.round(255 * alpha), 246, 242, 231));
        canvas.drawText(ellipsize(banner, rect.width() - dp(20f), paint), rect.centerX(), rect.centerY() + dp(4f), paint);
    }

    private void drawGameOver(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(215, 5, 5, 10));
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        RectF panel = new RectF(dp(28f), getHeight() * 0.24f, getWidth() - dp(28f), getHeight() * 0.68f);
        drawMetalPanel(canvas, panel, true, resultTitle.contains("ASCENDANT") ? BRIGHT_GOLD : DANGER);
        drawArcaneCrest(canvas, panel.centerX(), panel.top + dp(70f), dp(45f), uiClock * 13f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(UI_MEDIUM);
        paint.setTextSize(sp(24f));
        paint.setColor(IVORY);
        canvas.drawText(resultTitle, panel.centerX(), panel.top + dp(132f), paint);
        paint.setTypeface(UI_REGULAR);
        paint.setTextSize(sp(11f));
        paint.setColor(MUTED);
        drawWrappedText(canvas, resultSubtitle, panel.centerX(), panel.top + dp(160f), panel.width() - dp(34f), sp(11f), dp(16f), 2);
        paint.setTextSize(sp(10f));
        paint.setColor(BRIGHT_GOLD);
        canvas.drawText("DAMAGE " + damageDealt + "   ·   REACTIONS " + reactionsWon + "   ·   MANUAL CASTS " + manualCasts,
                panel.centerX(), panel.bottom - dp(35f), paint);
        float y = getHeight() - dp(154f);
        mainButton.set(dp(18f), y, getWidth() - dp(18f), y + dp(58f));
        secondaryButton.set(dp(18f), y + dp(66f), getWidth() - dp(18f), y + dp(116f));
        drawGoldButton(canvas, mainButton, "DUEL AGAIN", selectedArena.label, true);
        drawGoldButton(canvas, secondaryButton, "RETURN TO SANCTUM", "change loadout", false);
    }

    private void drawShockwaves(Canvas canvas) {
        for (Shockwave wave : shockwaves) {
            float a = clamp(wave.life / wave.maxLife, 0f, 1f);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1.2f) + dp(2.6f) * a);
            paint.setColor(Color.argb(Math.round(190 * a), Color.red(wave.color), Color.green(wave.color), Color.blue(wave.color)));
            canvas.drawCircle(wave.x, wave.y, wave.radius, paint);
            paint.setStrokeWidth(dp(0.8f));
            paint.setColor(Color.argb(Math.round(90 * a), 255, 255, 250));
            canvas.drawCircle(wave.x, wave.y, wave.radius * 0.82f, paint);
        }
    }

    private void drawParticles(Canvas canvas) {
        for (Particle particle : particles) {
            float a = clamp(particle.life / Math.max(0.001f, particle.maxLife), 0f, 1f);
            paint.setStyle(Paint.Style.FILL);
            paint.setMaskFilter(particle.glow ? new BlurMaskFilter(dp(3f), BlurMaskFilter.Blur.NORMAL) : null);
            paint.setColor(Color.argb(Math.round(220 * a), Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color)));
            canvas.drawCircle(particle.x, particle.y, particle.radius * (0.5f + a * 0.5f), paint);
            paint.setMaskFilter(null);
        }
    }

    private void drawFloatingText(Canvas canvas) {
        for (FloatingText text : floatingTexts) {
            float a = clamp(text.life / Math.max(0.001f, text.maxLife), 0f, 1f);
            // Pop-in: oversized on arrival, settling to normal size.
            float pop = 1f + Math.max(0f, a - 0.72f) * 1.6f;
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(UI_MEDIUM);
            paint.setLetterSpacing(0.06f);
            paint.setTextSize(sp(9.5f) * pop);
            paint.setColor(Color.argb(Math.round(150 * a), 5, 5, 10));
            canvas.drawText(text.text, text.x + dp(1f), text.y + dp(1f), paint);
            paint.setColor(Color.argb(Math.round(255 * a), Color.red(text.color), Color.green(text.color), Color.blue(text.color)));
            canvas.drawText(text.text, text.x, text.y, paint);
            paint.setLetterSpacing(0f);
        }
    }

    private int profileColor(Profile profile) {
        if (profile == null) return BRIGHT_GOLD;
        float[] values = {profile.heat, profile.moisture, profile.impulse, profile.mass,
                profile.cold, profile.charge, profile.aether, profile.entropy,
                profile.radiance, profile.growth};
        Element[] elements = Element.values();
        float total = 0f;
        float r = 0f, g = 0f, b = 0f;
        for (int i = 0; i < values.length; i++) {
            float v = Math.max(0f, values[i]);
            total += v;
            r += Color.red(elements[i].color) * v;
            g += Color.green(elements[i].color) * v;
            b += Color.blue(elements[i].color) * v;
        }
        if (total <= 0.001f) return BRIGHT_GOLD;
        return Color.rgb((int) clamp(r / total, 0f, 255f), (int) clamp(g / total, 0f, 255f), (int) clamp(b / total, 0f, 255f));
    }

    private void drawProfilePips(Canvas canvas, Profile p, float cx, float cy, float radius) {
        float[] values = {p.heat, p.moisture, p.impulse, p.mass, p.cold, p.charge, p.aether, p.entropy,
                p.radiance, p.growth};
        Element[] elements = Element.values();
        int shown = 0;
        for (int i = 0; i < values.length; i++) if (values[i] > 0.08f) shown++;
        if (shown == 0) return;
        float gap = radius * 2.7f;
        float x = cx - (shown - 1) * gap * 0.5f;
        for (int i = 0; i < values.length; i++) {
            if (values[i] <= 0.08f) continue;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(elements[i].color);
            canvas.drawCircle(x, cy, radius * clamp(0.65f + values[i] * 0.18f, 0.65f, 1.25f), paint);
            x += gap;
        }
    }

    private void spawnCastParticles(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float) Math.PI * 2f;
            float speed = dp(22f + random.nextFloat() * 58f);
            particles.add(new Particle(x, y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                    dp(1.2f + random.nextFloat() * 2.4f), color, 0.35f + random.nextFloat() * 0.55f, true));
        }
    }

    private void spawnImpact(float x, float y, int color, int count) {
        spawnCastParticles(x, y, color, count);
        spawnRing(x, y, color, dp(28f));
    }

    private void spawnShockwave(float x, float y, int color, float targetRadius) {
        shockwaves.add(new Shockwave(x, y, color, targetRadius));
    }

    private void addShake(float magnitude) {
        shakeTimer = Math.max(shakeTimer, 0.28f);
        shakeMagnitude = Math.max(shakeMagnitude * (shakeTimer > 0f ? 1f : 0f), magnitude);
    }

    private void spawnRing(float x, float y, int color, float radius) {
        int points = 18;
        for (int i = 0; i < points; i++) {
            float angle = (float) (Math.PI * 2.0 * i / points);
            float px = x + (float) Math.cos(angle) * radius;
            float py = y + (float) Math.sin(angle) * radius;
            particles.add(new Particle(px, py, (float) Math.cos(angle) * dp(8f), (float) Math.sin(angle) * dp(8f),
                    dp(1.4f), color, 0.55f, true));
        }
    }

    private void spawnArc(float x, float y, int color, int count) {
        for (int i = 0; i < count; i++) {
            float a = random.nextFloat() * (float) Math.PI * 2f;
            particles.add(new Particle(x, y, (float) Math.cos(a) * dp(45f), (float) Math.sin(a) * dp(45f),
                    dp(1.3f), color, 0.38f, true));
        }
    }

    private void spawnArc(float x1, float y1, int color, float x2, float y2, int segments) {
        for (int i = 0; i <= segments * 3; i++) {
            float t = i / (float) Math.max(1, segments * 3);
            float x = x1 + (x2 - x1) * t + (random.nextFloat() - 0.5f) * dp(8f);
            float y = y1 + (y2 - y1) * t + (random.nextFloat() - 0.5f) * dp(8f);
            particles.add(new Particle(x, y, 0f, 0f, dp(1.5f), color, 0.32f, true));
        }
    }

    private void spawnTrailParticle(Entity entity) {
        particles.add(new Particle(entity.x + (random.nextFloat() - 0.5f) * entity.radius,
                entity.y + (random.nextFloat() - 0.5f) * entity.radius,
                (random.nextFloat() - 0.5f) * dp(8f), -entity.velocity * 0.05f,
                dp(1.2f + random.nextFloat() * 1.8f), entity.profileColor(), 0.35f, true));
    }

    private String clauseList(List<Clause> clauses) {
        if (clauses == null || clauses.isEmpty()) return "No clauses";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) b.append(" → ");
            b.append(clauses.get(i).label);
        }
        return b.toString();
    }

    private String laneName(int lane) {
        if (lane == 0) return "Left";
        if (lane == 1) return "Center";
        if (lane == 2) return "Right";
        return "Unbound";
    }

    private String ellipsize(String text, float maxWidth, Paint p) {
        if (text == null) return "";
        if (p.measureText(text) <= maxWidth) return text;
        String suffix = "…";
        int end = text.length();
        while (end > 0 && p.measureText(text.substring(0, end) + suffix) > maxWidth) end--;
        return text.substring(0, Math.max(0, end)) + suffix;
    }

    private void drawWrappedText(Canvas canvas, String text, float anchorX, float firstBaseline,
                                 float maxWidth, float textSize, float lineHeight, int maxLines, Paint.Align align) {
        Paint.Align previous = paint.getTextAlign();
        paint.setTextAlign(align);
        paint.setTextSize(textSize);
        if (text == null) { paint.setTextAlign(previous); return; }
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(candidate) > maxWidth && line.length() > 0) {
                canvas.drawText(line.toString(), anchorX, firstBaseline + lineIndex * lineHeight, paint);
                lineIndex++;
                if (lineIndex >= maxLines) { paint.setTextAlign(previous); return; }
                line.setLength(0);
                line.append(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (lineIndex < maxLines && line.length() > 0) canvas.drawText(ellipsize(line.toString(), maxWidth, paint), anchorX, firstBaseline + lineIndex * lineHeight, paint);
        paint.setTextAlign(previous);
    }

    private void drawWrappedText(Canvas canvas, String text, float cx, float firstBaseline,
                                 float maxWidth, float textSize, float lineHeight, int maxLines) {
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(textSize);
        if (text == null) return;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        int lineIndex = 0;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(candidate) > maxWidth && line.length() > 0) {
                canvas.drawText(line.toString(), cx, firstBaseline + lineIndex * lineHeight, paint);
                lineIndex++;
                if (lineIndex >= maxLines) return;
                line.setLength(0);
                line.append(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (lineIndex < maxLines && line.length() > 0) canvas.drawText(ellipsize(line.toString(), maxWidth, paint), cx, firstBaseline + lineIndex * lineHeight, paint);
    }

    private float pathLength(List<PointF> points) {
        float length = 0f;
        for (int i = 1; i < points.size(); i++) length += distance(points.get(i - 1).x, points.get(i - 1).y, points.get(i).x, points.get(i).y);
        return length;
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float dp(float value) { return value * density; }
    private float sp(float value) { return value * scaledDensity; }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private static final class LeyNode {
        int owner = -1;
        float charge;
        Profile profile = new Profile();
    }

    /** Per-duel lane modifiers rolled at combat start; lane choice is positional strategy. */
    private enum LaneTrait {
        LEY_CURRENT("Ley Current", "spells travel faster"),
        BEDROCK("Bedrock", "constructs are sturdier"),
        WELLSPRING("Wellspring", "owned node grants mana"),
        WILD_MAGIC("Wild Magic", "reactions run hotter"),
        ASHEN_GROUND("Ashen Ground", "fields linger longer"),
        THIN_VEIL("Thin Veil", "node charges faster");

        final String label;
        final String subtitle;

        LaneTrait(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
        }
    }

    private static final class Shockwave {
        final float x;
        final float y;
        final int color;
        float radius;
        final float speed;
        float life;
        final float maxLife;

        Shockwave(float x, float y, int color, float targetRadius) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.radius = targetRadius * 0.18f;
            this.maxLife = 0.45f;
            this.life = maxLife;
            this.speed = (targetRadius - radius) / maxLife;
        }
    }

    private final class Entity {
        Kind kind;
        final int owner;
        int lane;
        Profile profile;
        Program program;
        Program storedProgram;
        float x;
        float y;
        float velocity;
        float radius = dp(8f);
        float damage;
        float hp;
        float life = 5f;
        float timer;
        float age;
        float powerScale = 1f;
        float trailTimer;
        float pulseTimer;
        boolean dead;
        boolean empowered;
        boolean anchored;
        boolean bound;
        boolean hexed;
        boolean swift;
        boolean hexWaiting;
        boolean riftBoosted;
        boolean passedNode;
        boolean relayed;
        boolean seekingDone;
        boolean beamResolved;
        Entity riftedBy;
        String label;
        final ArrayList<PointF> trail = new ArrayList<PointF>();

        Entity(Kind kind, int owner, int lane, Profile profile) {
            this.kind = kind;
            this.owner = owner;
            this.lane = lane;
            this.profile = profile == null ? new Profile() : profile;
        }

        int profileColor() { return ArcaneDuelView.this.profileColor(profile); }
    }

    private static final class PendingCast {
        final int owner;
        final Program program;
        final int lane;
        final float quality;
        final float powerScale;
        final boolean empowered;
        float delay;

        PendingCast(int owner, Program program, int lane, float quality, float powerScale, boolean empowered, float delay) {
            this.owner = owner;
            this.program = program.copy();
            this.lane = lane;
            this.quality = quality;
            this.powerScale = powerScale;
            this.empowered = empowered;
            this.delay = delay;
        }
    }

    private static final class EnemyIntent {
        final Program program;
        final int lane;
        final float quality;
        final float totalTime;
        final boolean empowered;
        float elapsed;

        EnemyIntent(Program program, int lane, float quality, float totalTime, boolean empowered) {
            this.program = program.copy();
            this.lane = lane;
            this.quality = quality;
            this.totalTime = Math.max(0.1f, totalTime);
            this.empowered = empowered;
        }

        float progress() { return Math.max(0f, Math.min(1f, elapsed / totalTime)); }
    }

    private static final class Particle {
        float x;
        float y;
        float vx;
        float vy;
        final float radius;
        final int color;
        float life;
        final float maxLife;
        final boolean glow;

        Particle(float x, float y, float vx, float vy, float radius, int color, float life, boolean glow) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy; this.radius = radius;
            this.color = color; this.life = life; this.maxLife = life; this.glow = glow;
        }
    }

    private static final class FloatingText {
        final String text;
        final float x;
        float y;
        final int color;
        float life;
        final float maxLife;

        FloatingText(String text, float x, float y, int color, float life) {
            this.text = text; this.x = x; this.y = y; this.color = color; this.life = life; this.maxLife = life;
        }
    }
}
